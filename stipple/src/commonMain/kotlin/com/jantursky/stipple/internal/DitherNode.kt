package com.jantursky.stipple.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import com.jantursky.stipple.CellShape
import com.jantursky.stipple.DitherField
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.DitherRenderer
import com.jantursky.stipple.LocalStippleMotion
import com.jantursky.stipple.LocalStippleTheme
import com.jantursky.stipple.ShaderField
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.durationScale
import com.jantursky.stipple.resolveReducedMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Where [DitherRenderer.Auto] switches to the shader: more cells than a frame of rectangles can reasonably cost. */
internal const val AUTO_SHADER_CELLS = 20_000

/** Where a dither goes relative to the element's own content. */
internal enum class DitherMode {
    /** Behind the content. */
    Behind,

    /** Over the content's shape: the content's pixels take the dither's colors. */
    Fill,

    /** The content shows only where the dither is lit. */
    Mask,
}

internal class DitherElement(
    val mode: DitherMode,
    val field: DitherField,
    val pattern: DitherPattern,
    val theme: StippleTheme?,
    val shape: Shape,
    val animated: Boolean,
    val speed: Float,
    val stillTime: Float,
    val renderer: DitherRenderer = DitherRenderer.Cells,
) : ModifierNodeElement<DitherNode>() {
    override fun create(): DitherNode =
        DitherNode(mode, field, pattern, theme, shape, animated, speed, stillTime, renderer)

    override fun update(node: DitherNode) =
        node.update(mode, field, pattern, theme, shape, animated, speed, stillTime, renderer)

    override fun equals(other: Any?): Boolean =
        other is DitherElement &&
            mode == other.mode &&
            field == other.field &&
            pattern == other.pattern &&
            theme == other.theme &&
            shape == other.shape &&
            animated == other.animated &&
            speed == other.speed &&
            stillTime == other.stillTime &&
            renderer == other.renderer

    override fun hashCode(): Int {
        var result = mode.hashCode()
        result = 31 * result + field.hashCode()
        result = 31 * result + pattern.hashCode()
        result = 31 * result + (theme?.hashCode() ?: 0)
        result = 31 * result + shape.hashCode()
        result = 31 * result + animated.hashCode()
        result = 31 * result + speed.hashCode()
        result = 31 * result + stillTime.hashCode()
        result = 31 * result + renderer.hashCode()
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "dither"
        properties["pattern"] = pattern
        properties["animated"] = animated
    }
}

/**
 * Draws a dither behind the element. The clock lives here as snapshot state that only the draw block reads, so a
 * frame redraws the element and never recomposes anything.
 */
internal class DitherNode(
    private var mode: DitherMode,
    private var field: DitherField,
    private var pattern: DitherPattern,
    private var themeOverride: StippleTheme?,
    private var shape: Shape,
    private var animated: Boolean,
    private var speed: Float,
    private var stillTime: Float,
    private var renderer: DitherRenderer,
) : Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode,
    ObserverModifierNode {
    private var time by mutableFloatStateOf(stillTime)
    private var inheritedTheme: StippleTheme = StippleTheme.Light
    private var reducedMotion = false
    private var clock: Job? = null

    override val shouldAutoInvalidate: Boolean get() = false

    fun update(
        mode: DitherMode,
        field: DitherField,
        pattern: DitherPattern,
        theme: StippleTheme?,
        shape: Shape,
        animated: Boolean,
        speed: Float,
        stillTime: Float,
        renderer: DitherRenderer,
    ) {
        val clockChanged = this.animated != animated || this.speed != speed || this.stillTime != stillTime
        this.mode = mode
        this.field = field
        this.pattern = pattern
        this.themeOverride = theme
        this.shape = shape
        this.animated = animated
        this.speed = speed
        this.stillTime = stillTime
        this.renderer = renderer
        if (clockChanged) restartClock()
        invalidateDraw()
    }

    override fun onAttach() {
        readLocals()
        restartClock()
    }

    override fun onDetach() {
        clock?.cancel()
        clock = null
    }

    override fun onReset() {
        time = stillTime
    }

    override fun onObservedReadsChanged() {
        readLocals()
        restartClock()
        invalidateDraw()
    }

    private fun readLocals() {
        observeReads {
            inheritedTheme = currentValueOf(LocalStippleTheme)
            reducedMotion =
                resolveReducedMotion(currentValueOf(LocalStippleMotion), coroutineScope.coroutineContext.durationScale)
        }
    }

    private fun restartClock() {
        clock?.cancel()
        clock = null
        if (!animated || reducedMotion) {
            time = stillTime
            return
        }
        clock =
            coroutineScope.launch {
                var last = -1L
                while (true) {
                    androidx.compose.runtime.withFrameNanos { now ->
                        if (last >= 0) time += (now - last) / 1_000_000_000f * speed
                        last = now
                    }
                }
            }
    }

    private val layerPaint = Paint()
    private val maskPaint = Paint().apply { blendMode = BlendMode.DstIn }

    override fun ContentDrawScope.draw() {
        val theme = themeOverride ?: inheritedTheme
        val t = time
        val bounds = Rect(Offset.Zero, size)
        when (mode) {
            DitherMode.Behind -> {
                dither(theme, t)
                drawContent()
            }

            DitherMode.Fill -> {
                drawIntoCanvas { it.saveLayer(bounds, layerPaint) }
                drawContent()
                dither(theme, t, blendMode = BlendMode.SrcIn)
                drawIntoCanvas { it.restore() }
            }

            DitherMode.Mask -> {
                drawIntoCanvas { it.saveLayer(bounds, layerPaint) }
                drawContent()
                drawIntoCanvas { it.saveLayer(bounds, maskPaint) }
                dither(
                    theme,
                    t,
                    ramp =
                        theme.ramp.mapIndexed { i, _ ->
                            if (i ==
                                0
                            ) {
                                Color.Transparent
                            } else {
                                Color.Black
                            }
                        },
                )
                drawIntoCanvas { it.restore() }
                drawIntoCanvas { it.restore() }
            }
        }
    }

    private var shaderPainter: ShaderPainter? = null
    private var shaderPainterCreated = false
    private var brokenProgram: String? = null

    /** The shader renderer when the dither allows it and this device has one, otherwise the cells. */
    private fun DrawScope.render(
        theme: StippleTheme,
        t: Float,
        ramp: List<Color>,
        blendMode: BlendMode,
    ) {
        val params = (field as? ShaderParams)?.shaderParams()
        val program = (field as? ShaderField)?.shaderSource?.let { shaderProgram(it, params?.size ?: 0) }
        val bayer = shaderBayerSide(pattern)
        val usable =
            renderer != DitherRenderer.Cells &&
                program != null &&
                program != brokenProgram &&
                bayer > 0 &&
                theme.shape == CellShape.Square &&
                ramp.size == 2 &&
                (
                    renderer == DitherRenderer.Shader ||
                        cellGrid(
                            size.width,
                            size.height,
                            theme.cell.toPx(),
                        ).let { it.columns * it.rows > AUTO_SHADER_CELLS }
                )
        if (usable && program != null) {
            if (!shaderPainterCreated) {
                shaderPainter = createShaderPainter()
                shaderPainterCreated = true
            }
            val painter = shaderPainter
            if (painter != null) {
                val cell =
                    maxOf(
                        1f,
                        theme.cell
                            .toPx()
                            .roundToInt()
                            .toFloat(),
                    )
                try {
                    painter.draw(this, program, ShaderUniforms(t, cell, bayer, ramp[1], ramp[0], params), blendMode)
                    return
                } catch (_: Exception) {
                    brokenProgram = program
                }
            }
        }
        CellPainter.draw(this, field, pattern, theme, t, ramp, blendMode)
    }

    private fun DrawScope.dither(
        theme: StippleTheme,
        t: Float,
        ramp: List<Color> = theme.ramp,
        blendMode: BlendMode = BlendMode.SrcOver,
    ) {
        if (shape === RectangleShape) {
            render(theme, t, ramp, blendMode)
            return
        }
        when (val outline = shape.createOutline(size, layoutDirection, this)) {
            is Outline.Rectangle -> {
                clipRect(outline.rect.left, outline.rect.top, outline.rect.right, outline.rect.bottom) {
                    render(theme, t, ramp, blendMode)
                }
            }

            is Outline.Rounded -> {
                clipPath(Path().apply { addRoundRect(outline.roundRect) }) {
                    render(theme, t, ramp, blendMode)
                }
            }

            is Outline.Generic -> {
                clipPath(outline.path) { render(theme, t, ramp, blendMode) }
            }
        }
    }
}
