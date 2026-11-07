package com.jantursky.stipple

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DitherImageTest {
    private val light = StippleTheme.Light.copy(cell = 2.dp)
    private val dark = StippleTheme.Dark.copy(cell = 2.dp)

    private fun androidx.compose.ui.test.ComposeUiTest.shareWhenReady(
        color: Color,
        timeoutMillis: Long = 5_000,
    ): Float {
        waitUntil(timeoutMillis = timeoutMillis) { capture().shareOf(color) > 0f }
        return capture().shareOf(color)
    }

    @Test
    fun darkContentBecomesInkOnLightPaper() =
        runDesktopComposeUiTest(200, 200) {
            setDitherContent {
                Square(Modifier.ditherImage(theme = light).background(Color.Black))
            }
            assertEquals(1f, shareWhenReady(light.ink))
        }

    @Test
    fun brightContentBecomesPaperOnLightPaper() =
        runDesktopComposeUiTest(200, 200) {
            setDitherContent {
                Square(Modifier.ditherImage(theme = light).background(Color.White))
            }
            assertEquals(1f, shareWhenReady(light.paper))
        }

    @Test
    fun brightContentBecomesInkOnDarkPaper() =
        runDesktopComposeUiTest(200, 200) {
            setDitherContent(theme = dark) {
                Square(Modifier.ditherImage(theme = dark).background(Color.White))
            }
            assertEquals(1f, shareWhenReady(dark.ink))
        }

    @Test
    fun invertFlipsTheTones() =
        runDesktopComposeUiTest(200, 200) {
            setDitherContent {
                Square(Modifier.ditherImage(theme = light, invert = true).background(Color.Black))
            }
            assertEquals(1f, shareWhenReady(light.paper))
        }

    @Test
    fun midGrayGivesHalfInkWithErrorDiffusion() =
        runDesktopComposeUiTest(200, 200) {
            setDitherContent {
                Square(
                    Modifier
                        .ditherImage(theme = light.copy(cell = 1.dp), algorithm = DitherAlgorithm.FloydSteinberg)
                        .background(Color(0xFF808080)),
                )
            }
            val share = shareWhenReady(light.ink)
            assertTrue(share in 0.4f..0.6f, "ink share $share")
        }

    @Test
    fun transparentContentStaysTransparent() =
        runDesktopComposeUiTest(200, 200) {
            setDitherContent {
                Square(Modifier.ditherImage(theme = light))
            }
            mainClock.advanceTimeBy(500)
            waitForIdle()
            // Nothing drawn is paper or nothing yet, depending on whether the result has landed; never ink.
            assertNotEquals(light.ink, capture().toPixelMap()[50, 50])
        }

    private fun gradientContent() =
        Modifier.background(
            androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color.Black, Color.White)),
        )

    @Test
    fun equalOptionsDoNotRebuildAndChangedOptionsDo() =
        runDesktopComposeUiTest(200, 200) {
            var options by androidx.compose.runtime.mutableStateOf(ImageDitherOptions.Default)
            setDitherContent {
                Square(
                    Modifier.ditherImage(theme = light, options = options).background(Color(0xFF808080)),
                )
            }
            waitUntil(timeoutMillis = 5_000) { capture().shareOf(light.ink) > 0f }
            val before = com.jantursky.stipple.internal.ImageDitherStats.completed
            val share = capture().shareOf(light.ink)
            options = ImageDitherOptions(0f, 1f, 1f, 0f, 1f)
            waitForIdle()
            Thread.sleep(300)
            assertEquals(before, com.jantursky.stipple.internal.ImageDitherStats.completed, "equal options")
            options = ImageDitherOptions(brightness = 0.3f)
            waitUntil(timeoutMillis = 5_000) { com.jantursky.stipple.internal.ImageDitherStats.completed > before }
            waitForIdle()
            assertTrue(capture().shareOf(light.ink) < share, "brighter content has less ink on light paper")
        }

    @Test
    fun quickSizeChangesDitherOnlyTheLastSize() =
        runDesktopComposeUiTest(200, 200) {
            var side by androidx.compose.runtime.mutableStateOf(100)
            setDitherContent {
                Box(Modifier.size(side.dp).ditherImage(theme = light).background(Color.Black))
            }
            waitUntil(timeoutMillis = 5_000) { capture().shareOf(light.ink) > 0f }
            waitForIdle()
            val before = com.jantursky.stipple.internal.ImageDitherStats.completed
            for (next in listOf(120, 140, 160, 180)) {
                side = next
                waitForIdle()
            }
            waitUntil(timeoutMillis = 5_000) { com.jantursky.stipple.internal.ImageDitherStats.completed > before }
            Thread.sleep(400)
            assertEquals(before + 1, com.jantursky.stipple.internal.ImageDitherStats.completed)
            assertEquals(1f, capture().shareOf(light.ink))
        }

    @Test
    fun defaultSamplingStaysCloseToFullSizeSampling() =
        runDesktopComposeUiTest(200, 200) {
            var options by androidx.compose.runtime.mutableStateOf(ImageDitherOptions(sampling = 1000))
            val cell4 = light.copy(cell = 4.dp)
            setDitherContent {
                Square(
                    Modifier
                        .ditherImage(theme = cell4, algorithm = DitherAlgorithm.Ordered, options = options)
                        .then(gradientContent()),
                )
            }
            waitUntil(timeoutMillis = 5_000) { capture().shareOf(cell4.ink) > 0f }
            waitForIdle()
            val full = capture()
            val before = com.jantursky.stipple.internal.ImageDitherStats.completed
            options = ImageDitherOptions()
            waitUntil(timeoutMillis = 5_000) { com.jantursky.stipple.internal.ImageDitherStats.completed > before }
            waitForIdle()
            val different = full.pixelsDifferentFrom(capture())
            assertTrue(different < 400, "$different of 40000 pixels differ from full-size sampling")
        }
}
