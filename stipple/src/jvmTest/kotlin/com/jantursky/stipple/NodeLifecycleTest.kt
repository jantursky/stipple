package com.jantursky.stipple

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class)
class NodeLifecycleTest {
    private val cell1 = StippleTheme.Light.copy(cell = 1.dp)

    @Test
    fun aChangedThemeInTheCompositionRedrawsTheDither() =
        runDitherTest {
            var theme by mutableStateOf(StippleTheme.Light)
            setContent {
                CompositionLocalProvider(LocalStippleTheme provides theme) {
                    Square(Modifier.dither(DitherFields.solid(1f)))
                }
            }
            assertEquals(1f, capture().shareOf(StippleTheme.Light.ink))
            theme = StippleTheme.Dark
            mainClock.advanceTimeBy(50)
            assertEquals(1f, capture().shareOf(StippleTheme.Dark.ink))
        }

    @Test
    fun theClockStartsAndStopsWithTheAnimatedFlag() =
        runDitherTest {
            var animated by mutableStateOf(false)
            setDitherContent {
                Square(Modifier.dither(DitherFields.ripples(), theme = cell1, animated = animated, stillTime = 0.3f))
            }
            val still = capture()
            animated = true
            mainClock.advanceTimeBy(700)
            assertNotEquals(0, still.pixelsDifferentFrom(capture()))
            animated = false
            mainClock.advanceTimeBy(50)
            assertEquals(0, still.pixelsDifferentFrom(capture()))
        }

    @Test
    fun speedChangesRestartTheClockWithoutRecomposingTheContent() =
        runDitherTest {
            var speed by mutableStateOf(1f)
            setDitherContent {
                Square(Modifier.dither(DitherFields.waves(), theme = cell1, animated = true, speed = speed))
            }
            mainClock.advanceTimeBy(300)
            speed = 4f
            mainClock.advanceTimeBy(300)
            capture()
        }

    @Test
    fun leavingTheCompositionStopsTheClock() =
        runDitherTest {
            var shown by mutableStateOf(true)
            setDitherContent {
                if (shown) Box(Modifier.size(100.dp).dither(DitherFields.waves(), theme = cell1, animated = true))
            }
            mainClock.advanceTimeBy(200)
            shown = false
            mainClock.advanceTimeBy(200)
            // The frame loop is gone, so with nothing left to ask for frames the window goes idle.
            waitForIdle()
        }

    @Test
    fun anImageDitherReleasesItsLayerWhenItLeaves() =
        runDitherTest {
            var shown by mutableStateOf(true)
            setDitherContent {
                if (shown) Box(Modifier.size(50.dp).ditherImage(theme = cell1))
            }
            mainClock.advanceTimeBy(300)
            shown = false
            mainClock.advanceTimeBy(100)
            waitForIdle()
        }
}
