package com.jantursky.stipple

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DitheredImageTest {
    private val theme = StippleTheme.Light.copy(cell = androidx.compose.ui.unit.Dp(2f))

    private fun source(
        width: Int,
        height: Int,
        color: (Int, Int) -> Color,
    ): ImageBitmap {
        val bitmap = ImageBitmap(width, height)
        val canvas = Canvas(bitmap)
        for (y in 0 until height) {
            for (x in 0 until width) {
                canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, Paint().apply { this.color = color(x, y) })
            }
        }
        return bitmap
    }

    @Test
    fun theResultHasTheSizeOfTheSourceAndOnlyThemeColors() =
        runBlocking {
            val image = source(40, 30) { x, _ -> Color(x / 39f, x / 39f, x / 39f) }
            val result = image.dithered(cell = 2f, theme = theme, algorithm = DitherAlgorithm.FloydSteinberg)
            assertEquals(40, result.width)
            assertEquals(30, result.height)
            val map = result.toPixelMap()
            val colors = HashSet<Color>()
            for (y in 0 until 30) for (x in 0 until 40) colors += map[x, y]
            assertTrue(colors.all { it == theme.ink || it == theme.paper }, "colors $colors")
            assertTrue(colors.size == 2)
        }

    @Test
    fun darkSourceBecomesInkAndBrightSourcePaper() =
        runBlocking {
            val black = source(20, 20) { _, _ -> Color.Black }.dithered(2f, theme).toPixelMap()
            val white = source(20, 20) { _, _ -> Color.White }.dithered(2f, theme).toPixelMap()
            assertEquals(theme.ink, black[5, 5])
            assertEquals(theme.paper, white[5, 5])
            val inverted = source(20, 20) { _, _ -> Color.Black }.dithered(2f, theme, invert = true).toPixelMap()
            assertEquals(theme.paper, inverted[5, 5])
        }

    @Test
    fun cellsMatchWhatDitherImageDraws() =
        runBlocking {
            // A ditherImage cell is the same call chain: same grid, same sampling, same levels.
            val image = source(40, 30) { x, y -> Color((x + y) / 69f, (x + y) / 69f, (x + y) / 69f) }
            val options = ImageDitherOptions(serpentine = true, brightness = 0.1f)
            val a = image.dithered(2f, theme, algorithm = DitherAlgorithm.Stucki, options = options).toPixelMap()
            val b = image.dithered(2f, theme, algorithm = DitherAlgorithm.Stucki, options = options).toPixelMap()
            for (y in 0 until 30) for (x in 0 until 40) assertEquals(a[x, y], b[x, y])
            val grid = com.jantursky.stipple.internal.cellGrid(40f, 30f, 2f)
            val pixels = IntArray(40 * 30)
            image.readPixels(pixels, 0, 0, 40, 30)
            val levels =
                com.jantursky.stipple.internal.ditherPixels(
                    pixels, 40, 30, grid, theme, DitherPatterns.Bayer4, DitherAlgorithm.Stucki, false, options,
                )
            for (row in 0 until grid.rows) {
                for (column in 0 until grid.columns) {
                    val expected = theme.ramp[levels[row * grid.columns + column].coerceAtLeast(0)]
                    assertEquals(expected, a[column * 2, row * 2], "cell $column,$row")
                }
            }
        }

    @Test
    fun aCancelledCallerGetsACancellationException() {
        runBlocking {
            val image = source(8, 8) { _, _ -> Color.Gray }
            var thrown: Throwable? = null
            launch {
                coroutineContext.job.cancel()
                try {
                    image.dithered(2f, theme)
                } catch (e: CancellationException) {
                    thrown = e
                    throw e
                }
            }.join()
            assertTrue(thrown is CancellationException)
        }
    }

    @OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
    @Test
    fun rememberDitheredImageDeliversTheResultAndFollowsTheTheme() =
        androidx.compose.ui.test.v2.runDesktopComposeUiTest(100, 100) {
            val image = source(16, 16) { _, _ -> Color.Black }
            var state: androidx.compose.runtime.State<ImageBitmap?>? = null
            setContent {
                androidx.compose.runtime.CompositionLocalProvider(LocalStippleTheme provides theme) {
                    state = rememberDitheredImage(image, cell = 2f)
                }
            }
            waitUntil(timeoutMillis = 5_000) { state?.value != null }
            val first = state!!.value!!
            assertEquals(16, first.width)
            assertEquals(theme.ink, first.toPixelMap()[3, 3])
        }
}
