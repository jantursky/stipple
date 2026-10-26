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
import com.jantursky.stipple.DitherField
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.LocalStippleMotion
import com.jantursky.stipple.LocalStippleTheme
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.durationScale
import com.jantursky.stipple.resolveReducedMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
) : ModifierNodeElement<DitherNode>() {
    override fun create(): DitherNode = DitherNode(mode, field, pattern, theme, shape, animated, speed, stillTime)

    override fun update(node: DitherNode) = node.update(mode, field, pattern, theme, shape, animated, speed, stillTime)

    override fun equals(other: Any?): Boolean =
        other is DitherElement &&
            mode == other.mode &&
            field == other.field &&
            pattern == other.pattern &&
            theme == other.theme &&
            shape == other.shape &&
            animated == other.animated &&
            speed == other.speed &&
            stillTime == other.stillTime

    override fun hashCode(): Int {
        var result = mode.hashCode()
        result = 31 * result + field.hashCode()
        result = 31 * result + pattern.hashCode()
        result = 31 * result + (theme?.hashCode() ?: 0)
        result = 31 * result + shape.hashCode()
        result = 31 * result + animated.hashCode()
        result = 31 * result + speed.hashCode()
        result = 31 * result + stillTime.hashCode()
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

    private fun DrawScope.dither(
        theme: StippleTheme,
        t: Float,
        ramp: List<Color> = theme.ramp,
        blendMode: BlendMode = BlendMode.SrcOver,
    ) {
        if (shape === RectangleShape) {
            CellPainter.draw(this, field, pattern, theme, t, ramp, blendMode)
            return
        }
        when (val outline = shape.createOutline(size, layoutDirection, this)) {
            is Outline.Rectangle -> {
                clipRect { CellPainter.draw(this, field, pattern, theme, t, ramp, blendMode) }
            }

            is Outline.Rounded -> {
                clipPath(Path().apply { addRoundRect(outline.roundRect) }) {
                    CellPainter.draw(this, field, pattern, theme, t, ramp, blendMode)
                }
            }

            is Outline.Generic -> {
                clipPath(outline.path) { CellPainter.draw(this, field, pattern, theme, t, ramp, blendMode) }
            }
        }
    }
}
