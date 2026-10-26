package com.jantursky.stipple

import androidx.compose.foundation.background
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
            mainClock.advanceTimeBy(500)
            waitForIdle()
            assertEquals(1f, capture().shareOf(light.paper))
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
            mainClock.advanceTimeBy(500)
            waitForIdle()
            assertEquals(1f, capture().shareOf(light.paper))
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
}
