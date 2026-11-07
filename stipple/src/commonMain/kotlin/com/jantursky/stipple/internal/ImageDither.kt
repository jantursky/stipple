package com.jantursky.stipple.internal

import androidx.compose.ui.graphics.Color
import com.jantursky.stipple.DitherAlgorithm
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.ImageDitherOptions
import com.jantursky.stipple.StippleTheme
import kotlin.math.floor
import kotlin.math.pow

/** Cells that are fully see-through are not drawn. */
internal const val TRANSPARENT_CELL = -1

/** One tone and one opacity per cell, row by row. */
internal class CellSamples(
    val columns: Int,
    val rows: Int,
    val luminance: FloatArray,
    val alpha: FloatArray,
)

/** Linear-light value of every sRGB byte, so linear luminance costs a lookup per channel. */
private val LINEAR = FloatArray(256) { i ->
    val c = i / 255f
    if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
}

private fun encodeSrgb(linear: Float): Float {
    val l = linear.coerceIn(0f, 1f)
    return if (l <= 0.0031308f) l * 12.92f else 1.055f * l.pow(1f / 2.4f) - 0.055f
}

/** The pixel size read back for a grid: the cell grid times [sampling], never more than the source itself. */
internal fun readbackSize(
    width: Int,
    height: Int,
    grid: CellGrid,
    sampling: Int,
): Pair<Int, Int> =
    minOf(width, grid.columns * sampling).coerceAtLeast(1) to minOf(height, grid.rows * sampling).coerceAtLeast(1)

/**
 * Averages the ARGB [pixels] of an image into [grid]-sized cells: the luminance weighted by opacity, and the mean
 * opacity. The image is stretched over the grid, so any size of image fills it.
 */
internal fun sampleCells(
    pixels: IntArray,
    width: Int,
    height: Int,
    grid: CellGrid,
    options: ImageDitherOptions = ImageDitherOptions.Default,
): CellSamples {
    val linear = options.linearLuminance
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
                    val r: Float
                    val g: Float
                    val b: Float
                    if (linear) {
                        r = LINEAR[(p shr 16) and 0xFF]
                        g = LINEAR[(p shr 8) and 0xFF]
                        b = LINEAR[p and 0xFF]
                    } else {
                        r = ((p shr 16) and 0xFF) / 255f
                        g = ((p shr 8) and 0xFF) / 255f
                        b = (p and 0xFF) / 255f
                    }
                    sum += (0.2126f * r + 0.7152f * g + 0.0722f * b) * a
                    opacity += a
                }
            }
            val n = (x1 - x0) * (y1 - y0)
            val mean = if (opacity > 0f) sum / opacity else 0f
            luminance[row * grid.columns + column] = shapeTone(if (linear) encodeSrgb(mean) else mean, options)
            alpha[row * grid.columns + column] = opacity / n
        }
    }
    return CellSamples(grid.columns, grid.rows, luminance, alpha)
}

/** Contrast around the middle, then brightness, then gamma. The neutral settings return [tone] untouched. */
private fun shapeTone(
    tone: Float,
    options: ImageDitherOptions,
): Float {
    var t = tone
    if (options.contrast != 1f) t = (t - 0.5f) * options.contrast + 0.5f
    if (options.brightness != 0f) t += options.brightness
    if (options.gamma != 1f) t = t.coerceIn(0f, 1f).pow(1f / options.gamma)
    return t.coerceIn(0f, 1f)
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
    options: ImageDitherOptions = ImageDitherOptions.Default,
    checkCancelled: () -> Unit = {},
): IntArray {
    val out = IntArray(columns * rows)
    val kernel = kernelOf(algorithm)
    if (kernel == null) {
        for (row in 0 until rows) {
            checkCancelled()
            for (column in 0 until columns) {
                out[row * columns + column] =
                    quantize(
                        tones[row * columns + column] - options.threshold,
                        pattern.threshold(column, row),
                        levels,
                    )
            }
        }
    } else {
        diffuse(tones, out, columns, rows, levels, kernel, options, checkCancelled)
    }
    for (i in out.indices) if (alpha[i] < 0.5f) out[i] = TRANSPARENT_CELL
    return out
}

/**
 * Reads [pixels] into cells and dithers them: sample, flip to the theme's ink, diffuse or compare. Call it off the
 * main thread; [checkCancelled] runs once per row so a stale job stops early.
 */
internal fun ditherPixels(
    pixels: IntArray,
    width: Int,
    height: Int,
    grid: CellGrid,
    theme: StippleTheme,
    pattern: DitherPattern,
    algorithm: DitherAlgorithm,
    invert: Boolean,
    options: ImageDitherOptions,
    checkCancelled: () -> Unit = {},
): IntArray {
    val samples = sampleCells(pixels, width, height, grid, options)
    val ramp = theme.ramp
    val inkIsLight = ramp.last().luminance() > ramp.first().luminance()
    val flip = inkIsLight == invert
    val tones = FloatArray(samples.luminance.size) { if (flip) 1f - samples.luminance[it] else samples.luminance[it] }
    checkCancelled()
    return ditherCells(
        tones,
        samples.alpha,
        grid.columns,
        grid.rows,
        algorithm,
        pattern,
        ramp.size,
        options,
        checkCancelled,
    )
}

internal fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

/** The neighbours that receive a share of the error: [dx] cells right and [dy] rows down, each with its [weight]. */
internal class Kernel(
    val dx: IntArray,
    val dy: IntArray,
    val weight: FloatArray,
)

/** Builds a kernel from (dx, dy, share) triples, each share being a part of [divisor]. */
private fun kernel(
    divisor: Int,
    vararg taps: IntArray,
): Kernel =
    Kernel(
        IntArray(taps.size) { taps[it][0] },
        IntArray(taps.size) { taps[it][1] },
        FloatArray(taps.size) { taps[it][2].toFloat() / divisor },
    )

private fun tap(
    dx: Int,
    dy: Int,
    share: Int,
) = intArrayOf(dx, dy, share)

private val FLOYD_STEINBERG = kernel(16, tap(1, 0, 7), tap(-1, 1, 3), tap(0, 1, 5), tap(1, 1, 1))

// Atkinson spreads 6/8 of the error, so large flat areas drift to pure paper or pure ink.
private val ATKINSON =
    kernel(8, tap(1, 0, 1), tap(2, 0, 1), tap(-1, 1, 1), tap(0, 1, 1), tap(1, 1, 1), tap(0, 2, 1))

private val SIERRA =
    kernel(
        32,
        tap(1, 0, 5), tap(2, 0, 3),
        tap(-2, 1, 2), tap(-1, 1, 4), tap(0, 1, 5), tap(1, 1, 4), tap(2, 1, 2),
        tap(-1, 2, 2), tap(0, 2, 3), tap(1, 2, 2),
    )

private val SIERRA_LITE = kernel(4, tap(1, 0, 2), tap(-1, 1, 1), tap(0, 1, 1))

private val BURKES =
    kernel(
        32,
        tap(1, 0, 8), tap(2, 0, 4),
        tap(-2, 1, 2), tap(-1, 1, 4), tap(0, 1, 8), tap(1, 1, 4), tap(2, 1, 2),
    )

private val STUCKI =
    kernel(
        42,
        tap(1, 0, 8), tap(2, 0, 4),
        tap(-2, 1, 2), tap(-1, 1, 4), tap(0, 1, 8), tap(1, 1, 4), tap(2, 1, 2),
        tap(-2, 2, 1), tap(-1, 2, 2), tap(0, 2, 4), tap(1, 2, 2), tap(2, 2, 1),
    )

private val JARVIS_JUDICE_NINKE =
    kernel(
        48,
        tap(1, 0, 7), tap(2, 0, 5),
        tap(-2, 1, 3), tap(-1, 1, 5), tap(0, 1, 7), tap(1, 1, 5), tap(2, 1, 3),
        tap(-2, 2, 1), tap(-1, 2, 3), tap(0, 2, 5), tap(1, 2, 3), tap(2, 2, 1),
    )

/** The kernel of an error-diffusion algorithm, or null for ordered dithering. */
internal fun kernelOf(algorithm: DitherAlgorithm): Kernel? =
    when (algorithm) {
        DitherAlgorithm.Ordered -> null
        DitherAlgorithm.FloydSteinberg -> FLOYD_STEINBERG
        DitherAlgorithm.Atkinson -> ATKINSON
        DitherAlgorithm.Sierra -> SIERRA
        DitherAlgorithm.SierraLite -> SIERRA_LITE
        DitherAlgorithm.Burkes -> BURKES
        DitherAlgorithm.Stucki -> STUCKI
        DitherAlgorithm.JarvisJudiceNinke -> JARVIS_JUDICE_NINKE
    }

private fun diffuse(
    tones: FloatArray,
    out: IntArray,
    columns: Int,
    rows: Int,
    levels: Int,
    kernel: Kernel,
    options: ImageDitherOptions,
    checkCancelled: () -> Unit,
) {
    val work = tones.copyOf()
    val top = levels - 1
    val strength = options.strength
    val shift = options.threshold
    val taps = kernel.weight.size
    for (row in 0 until rows) {
        checkCancelled()
        val mirrored = options.serpentine && row % 2 == 1
        for (step in 0 until columns) {
            val column = if (mirrored) columns - 1 - step else step
            val i = row * columns + column
            val wanted = work[i].coerceIn(0f, 1f)
            val level = floor((wanted - shift) * top + 0.5f).toInt().coerceIn(0, top)
            out[i] = level
            val error = (wanted - level.toFloat() / top) * strength
            for (t in 0 until taps) {
                val x = column + if (mirrored) -kernel.dx[t] else kernel.dx[t]
                val y = row + kernel.dy[t]
                if (x in 0 until columns && y < rows) work[y * columns + x] += error * kernel.weight[t]
            }
        }
    }
}
