package com.jantursky.stipple.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.jantursky.stipple.CellShape
import com.jantursky.stipple.DitherField
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.StippleTheme
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Past this many cells the grid is coarsened, because drawing more than this each frame is not realistic. */
internal const val MAX_CELLS = 90_000

/** The cell grid for an area: whole-pixel cells so neighbours never leave seams. */
internal class CellGrid(
    val cell: Float,
    val columns: Int,
    val rows: Int,
)

internal fun cellGrid(
    width: Float,
    height: Float,
    requestedCell: Float,
): CellGrid {
    var cell = max(1f, requestedCell.roundToInt().toFloat())
    var columns = ceil(width / cell).toInt()
    var rows = ceil(height / cell).toInt()
    if (columns.toLong() * rows > MAX_CELLS) {
        cell = ceil(cell * sqrt(columns.toDouble() * rows / MAX_CELLS)).toFloat()
        columns = ceil(width / cell).toInt()
        rows = ceil(height / cell).toInt()
    }
    return CellGrid(cell, max(1, columns), max(1, rows))
}

/**
 * Draws a grid of cells: [levelAt] gives the ramp index of each cell, 0 being paper. Paper is drawn once as the
 * background, and neighbours of one level join into a single rectangle, so a still area costs few draw calls.
 */
internal inline fun DrawScope.paintCells(
    grid: CellGrid,
    theme: StippleTheme,
    levelAt: (column: Int, row: Int) -> Int,
) {
    val width = size.width
    val height = size.height
    val ramp = theme.ramp
    val cell = grid.cell
    drawRect(ramp[0])
    val dots = theme.shape == CellShape.Dot
    for (row in 0 until grid.rows) {
        val top = row * cell
        var runLevel = 0
        var runStart = 0
        for (column in 0..grid.columns) {
            val level = if (column == grid.columns) 0 else levelAt(column, row)
            if (dots) {
                if (level > 0) {
                    drawCircle(
                        color = ramp[level],
                        radius = min(cell, min(width - column * cell, height - top)) / 2f,
                        center = Offset(column * cell + cell / 2f, top + cell / 2f),
                    )
                }
                continue
            }
            if (level == runLevel) continue
            if (runLevel > 0) {
                val left = runStart * cell
                drawRect(
                    color = ramp[runLevel],
                    topLeft = Offset(left, top),
                    size = Size(min(column * cell, width) - left, min(cell, height - top)),
                )
            }
            runLevel = level
            runStart = column
        }
    }
}

/** Implemented by fields that read their inputs once per frame instead of once per cell. */
internal interface FrameAware {
    fun beginFrame()
}

/**
 * The cell renderer: samples the field once per cell, quantizes it against the pattern and draws the result as
 * squares or dots. It uses nothing but the canvas, so it behaves the same on every target and on Android 6.
 */
internal object CellPainter {
    fun draw(
        scope: DrawScope,
        field: DitherField,
        pattern: DitherPattern,
        theme: StippleTheme,
        t: Float,
    ) {
        with(scope) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return
            (field as? FrameAware)?.beginFrame()
            val grid = cellGrid(width, height, theme.cell.toPx())
            val cell = grid.cell
            val aspect = width / height
            val levels = theme.ramp.size
            paintCells(grid, theme) { column, row ->
                val x = min((column * cell + cell / 2f) / width, 1f)
                val y = min((row * cell + cell / 2f) / height, 1f)
                quantize(field.tone(x, y, t, aspect), pattern.threshold(column, row), levels)
            }
        }
    }
}
