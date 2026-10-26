package com.jantursky.stipple.internal

import com.jantursky.stipple.DitherAlgorithm
import com.jantursky.stipple.DitherPattern
import kotlin.math.floor

/** Cells that are fully see-through are not drawn. */
internal const val TRANSPARENT_CELL = -1

/** One tone and one opacity per cell, row by row. */
internal class CellSamples(
    val columns: Int,
    val rows: Int,
    val luminance: FloatArray,
    val alpha: FloatArray,
)

/**
 * Averages the ARGB [pixels] of an image into [grid]-sized cells: the luminance weighted by opacity, and the mean
 * opacity. The image is stretched over the grid, so any size of image fills it.
 */
internal fun sampleCells(
    pixels: IntArray,
    width: Int,
    height: Int,
    grid: CellGrid,
): CellSamples {
    val count = grid.columns * grid.rows
    val luminance = FloatArray(count)
    val alpha = FloatArray(count)
    for (row in 0 until grid.rows) {
        val y0 = row * height / grid.rows
        val y1 = maxOf(y0 + 1, (row + 1) * height / grid.rows).coerceAtMost(height)
        for (column in 0 until grid.columns) {
            val x0 = column * width / grid.columns
            val x1 = maxOf(x0 + 1, (column + 1) * width / grid.columns).coerceAtMost(width)
            var sum = 0f
            var opacity = 0f
            for (y in y0 until y1) {
                for (x in x0 until x1) {
                    val p = pixels[y * width + x]
                    val a = (p ushr 24) / 255f
                    val r = ((p shr 16) and 0xFF) / 255f
                    val g = ((p shr 8) and 0xFF) / 255f
                    val b = (p and 0xFF) / 255f
                    sum += (0.2126f * r + 0.7152f * g + 0.0722f * b) * a
                    opacity += a
                }
            }
            val n = (x1 - x0) * (y1 - y0)
            luminance[row * grid.columns + column] = if (opacity > 0f) sum / opacity else 0f
            alpha[row * grid.columns + column] = opacity / n
        }
    }
    return CellSamples(grid.columns, grid.rows, luminance, alpha)
}

/**
 * The ramp index of every cell. [tones] holds 0 for paper and 1 for ink; cells whose opacity is under a half become
 * [TRANSPARENT_CELL]. Error diffusion runs once over the whole grid, so call this off the main thread and keep the
 * result.
 */
internal fun ditherCells(
    tones: FloatArray,
    alpha: FloatArray,
    columns: Int,
    rows: Int,
    algorithm: DitherAlgorithm,
    pattern: DitherPattern,
    levels: Int,
): IntArray {
    val out = IntArray(columns * rows)
    when (algorithm) {
        DitherAlgorithm.Ordered -> {
            for (row in 0 until rows) {
                for (column in 0 until columns) {
                    out[row * columns + column] =
                        quantize(tones[row * columns + column], pattern.threshold(column, row), levels)
                }
            }
        }

        DitherAlgorithm.FloydSteinberg -> {
            diffuse(tones, out, columns, rows, levels, FLOYD_STEINBERG)
        }

        DitherAlgorithm.Atkinson -> {
            diffuse(tones, out, columns, rows, levels, ATKINSON)
        }
    }
    for (i in out.indices) if (alpha[i] < 0.5f) out[i] = TRANSPARENT_CELL
    return out
}

/** A neighbour that receives [weight] of the error, [dx] cells right and [dy] rows down. */
private class Tap(
    val dx: Int,
    val dy: Int,
    val weight: Float,
)

private val FLOYD_STEINBERG =
    listOf(Tap(1, 0, 7f / 16f), Tap(-1, 1, 3f / 16f), Tap(0, 1, 5f / 16f), Tap(1, 1, 1f / 16f))

// Atkinson spreads 6/8 of the error, so large flat areas drift to pure paper or pure ink.
private val ATKINSON =
    listOf(
        Tap(1, 0, 1f / 8f),
        Tap(2, 0, 1f / 8f),
        Tap(-1, 1, 1f / 8f),
        Tap(0, 1, 1f / 8f),
        Tap(1, 1, 1f / 8f),
        Tap(0, 2, 1f / 8f),
    )

private fun diffuse(
    tones: FloatArray,
    out: IntArray,
    columns: Int,
    rows: Int,
    levels: Int,
    taps: List<Tap>,
) {
    val work = tones.copyOf()
    val top = levels - 1
    for (row in 0 until rows) {
        for (column in 0 until columns) {
            val i = row * columns + column
            val wanted = work[i].coerceIn(0f, 1f)
            val level = floor(wanted * top + 0.5f).toInt().coerceIn(0, top)
            out[i] = level
            val error = wanted - level.toFloat() / top
            for (tap in taps) {
                val x = column + tap.dx
                val y = row + tap.dy
                if (x in 0 until columns && y < rows) work[y * columns + x] += error * tap.weight
            }
        }
    }
}
