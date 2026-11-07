package com.jantursky.stipple

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.jantursky.stipple.internal.cellGrid
import com.jantursky.stipple.internal.ditherPixels
import com.jantursky.stipple.internal.paintCells
import com.jantursky.stipple.internal.readbackSize
import com.jantursky.stipple.internal.readPixelsScaled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Dithers this image and returns the result as a new [ImageBitmap] of the same size, drawn from the same cells as
 * [ditherImage]: paper-colored where the image is see-through, ink where the theme says so. Save or share the bitmap
 * the way your app already does; the library doesn't encode files.
 *
 * The work runs on a background dispatcher and stops with `CancellationException` when the caller is cancelled.
 *
 * @param cell The side of one cell in pixels of this bitmap. Defaults to the theme's cell size as plain pixels.
 * @param theme The colors and cell shape. [StippleTheme.Light] by default.
 */
public suspend fun ImageBitmap.dithered(
    cell: Float = StippleTheme.Light.cell.value,
    theme: StippleTheme = StippleTheme.Light,
    pattern: DitherPattern = DitherPatterns.Bayer4,
    algorithm: DitherAlgorithm = DitherAlgorithm.FloydSteinberg,
    invert: Boolean = false,
    options: ImageDitherOptions = ImageDitherOptions.Default,
): ImageBitmap {
    currentCoroutineContext().ensureActive()
    val width = this.width
    val height = this.height
    val grid = cellGrid(width.toFloat(), height.toFloat(), cell)
    val (readWidth, readHeight) = readbackSize(width, height, grid, options.sampling)
    val pixels = readPixelsScaled(readWidth, readHeight)
    val levels =
        withContext(Dispatchers.Default) {
            ditherPixels(pixels, readWidth, readHeight, grid, theme, pattern, algorithm, invert, options) {
                ensureActive()
            }
        }
    currentCoroutineContext().ensureActive()
    val out = ImageBitmap(width, height)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(out), Size(width.toFloat(), height.toFloat())) {
        paintCells(grid, theme) { column, row -> levels[row * grid.columns + column].coerceAtLeast(0) }
    }
    return out
}

/**
 * Dithers [image] in the background and returns the result once it is ready, or null before that. A new result is
 * made when any argument changes; the previous one stays until then.
 *
 * @param theme The colors and cell shape; the current [LocalStippleTheme] when null.
 * @param cell The side of one cell in pixels of [image].
 */
@Composable
public fun rememberDitheredImage(
    image: ImageBitmap,
    cell: Float = StippleTheme.Light.cell.value,
    theme: StippleTheme? = null,
    pattern: DitherPattern = DitherPatterns.Bayer4,
    algorithm: DitherAlgorithm = DitherAlgorithm.FloydSteinberg,
    invert: Boolean = false,
    options: ImageDitherOptions = ImageDitherOptions.Default,
): State<ImageBitmap?> {
    val resolved = theme ?: LocalStippleTheme.current
    return produceState<ImageBitmap?>(null, image, cell, resolved, pattern, algorithm, invert, options) {
        value = image.dithered(cell, resolved, pattern, algorithm, invert, options)
    }
}
