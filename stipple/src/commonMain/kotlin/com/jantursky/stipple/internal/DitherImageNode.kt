package com.jantursky.stipple.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toIntSize
import com.jantursky.stipple.DitherAlgorithm
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.LocalStippleTheme
import com.jantursky.stipple.StippleTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class DitherImageElement(
    val pattern: DitherPattern,
    val theme: StippleTheme?,
    val algorithm: DitherAlgorithm,
    val invert: Boolean,
    val key: Any?,
) : ModifierNodeElement<DitherImageNode>() {
    override fun create(): DitherImageNode = DitherImageNode(pattern, theme, algorithm, invert, key)

    override fun update(node: DitherImageNode) = node.update(pattern, theme, algorithm, invert, key)

    override fun equals(other: Any?): Boolean =
        other is DitherImageElement &&
            pattern == other.pattern &&
            theme == other.theme &&
            algorithm == other.algorithm &&
            invert == other.invert &&
            key == other.key

    override fun hashCode(): Int {
        var result = pattern.hashCode()
        result = 31 * result + (theme?.hashCode() ?: 0)
        result = 31 * result + algorithm.hashCode()
        result = 31 * result + invert.hashCode()
        result = 31 * result + (key?.hashCode() ?: 0)
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "ditherImage"
        properties["algorithm"] = algorithm
    }
}

/** What a finished dither was made from, so a draw can tell whether it is still current. */
private data class Source(
    val size: IntSize,
    val cell: Float,
    val pattern: DitherPattern,
    val theme: StippleTheme,
    val algorithm: DitherAlgorithm,
    val invert: Boolean,
    val key: Any?,
)

private class Result(
    val source: Source,
    val grid: CellGrid,
    val levels: IntArray,
)

/**
 * Dithers what the element draws. The content is recorded into a layer, read back once, reduced to one tone per cell
 * and dithered on a background dispatcher; the result is cached until the size, the theme or a setting changes. A
 * draw never waits for it: the previous result stays on screen, and nothing shows before the first one.
 */
internal class DitherImageNode(
    private var pattern: DitherPattern,
    private var themeOverride: StippleTheme?,
    private var algorithm: DitherAlgorithm,
    private var invert: Boolean,
    private var key: Any?,
) : Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode,
    ObserverModifierNode {
    private var inheritedTheme: StippleTheme = StippleTheme.Light
    private var result by mutableStateOf<Result?>(null)
    private var layer: GraphicsLayer? = null
    private var pending: Source? = null
    private var job: Job? = null

    override val shouldAutoInvalidate: Boolean get() = false

    fun update(
        pattern: DitherPattern,
        theme: StippleTheme?,
        algorithm: DitherAlgorithm,
        invert: Boolean,
        key: Any?,
    ) {
        this.pattern = pattern
        this.themeOverride = theme
        this.algorithm = algorithm
        this.invert = invert
        this.key = key
        invalidateDraw()
    }

    override fun onAttach() {
        readLocals()
    }

    override fun onDetach() {
        job?.cancel()
        job = null
        pending = null
        layer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        layer = null
    }

    override fun onReset() {
        result = null
        pending = null
    }

    override fun onObservedReadsChanged() {
        readLocals()
        invalidateDraw()
    }

    private fun readLocals() {
        observeReads { inheritedTheme = currentValueOf(LocalStippleTheme) }
    }

    override fun ContentDrawScope.draw() {
        if (size.width <= 0f || size.height <= 0f) return
        val theme = themeOverride ?: inheritedTheme
        val cell = cellGrid(size.width, size.height, theme.cell.toPx()).cell
        val source = Source(size.toIntSize(), cell, pattern, theme, algorithm, invert, key)
        val done = result
        if ((done == null || done.source != source) && pending != source) {
            pending = source
            val layer = layer ?: requireGraphicsContext().createGraphicsLayer().also { layer = it }
            val outer = this
            layer.record(outer.density.let { androidx.compose.ui.unit.Density(it, outer.fontScale) }, layoutDirection, source.size) {
                val context = outer.drawContext
                val canvas = context.canvas
                context.canvas = drawContext.canvas
                try {
                    outer.drawContent()
                } finally {
                    context.canvas = canvas
                }
            }
            start(layer, source)
        }
        if (done != null) {
            val grid = done.grid
            val levels = done.levels
            val width = grid.columns
            paintCells(grid, done.source.theme) { column, row -> levels[row * width + column].coerceAtLeast(0) }
        }
    }

    private fun start(
        layer: GraphicsLayer,
        source: Source,
    ) {
        job?.cancel()
        job =
            coroutineScope.launch {
                val bitmap = layer.toImageBitmap()
                val width = bitmap.width
                val height = bitmap.height
                val pixels = IntArray(width * height)
                bitmap.readPixels(pixels, 0, 0, width, height)
                val made =
                    withContext(Dispatchers.Default) {
                        val grid = cellGrid(source.size.width.toFloat(), source.size.height.toFloat(), source.cell)
                        val samples = sampleCells(pixels, width, height, grid)
                        val ramp = source.theme.ramp
                        val inkIsLight = ramp.last().luminance() > ramp.first().luminance()
                        val flip = inkIsLight == source.invert
                        val tones = FloatArray(samples.luminance.size) { if (flip) 1f - samples.luminance[it] else samples.luminance[it] }
                        val levels =
                            ditherCells(tones, samples.alpha, grid.columns, grid.rows, source.algorithm, source.pattern, ramp.size)
                        Result(source, grid, levels)
                    }
                result = made
                pending = null
            }
    }
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
