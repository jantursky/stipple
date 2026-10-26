package com.jantursky.stipple

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DitherProgressTest {
    private val theme = StippleTheme.Light.copy(cell = 1.dp)
    private val ink = theme.ink
    private val paper = theme.paper

    @Test
    fun theFillStopsAtTheProgress() =
        runDitherTest {
            setDitherContent { Square(Modifier.ditherProgress({ 0.5f }, theme = theme, softness = 0.1f)) }
            val map = capture().toPixelMap()
            assertEquals(ink, map[10, 50])
            assertEquals(ink, map[40, 50])
            assertEquals(paper, map[60, 50])
            assertEquals(paper, map[95, 50])
        }

    @Test
    fun zeroIsEmptyAndOneIsFull() =
        runDitherTest {
            setDitherContent { Square(Modifier.ditherProgress({ 0f }, theme = theme)) }
            assertEquals(1f, capture().shareOf(paper))
            setDitherContent { Square(Modifier.ditherProgress({ 1f }, theme = theme)) }
            assertEquals(1f, capture().shareOf(ink))
        }

    @Test
    fun theTrackShowsBehindTheFill() =
        runDitherTest {
            setDitherContent {
                Square(Modifier.ditherProgress({ 0f }, DitherPatterns.Bayer8, theme, track = 0.25f))
            }
            assertTrue(capture().shareOf(ink) in 0.24f..0.26f)
        }

    @Test
    fun theDirectionChangesTheEdge() =
        runDitherTest {
            setDitherContent {
                Square(
                    Modifier.ditherProgress({ 0.5f }, theme = theme, direction = ProgressDirection.Up, softness = 0.1f),
                )
            }
            val map = capture().toPixelMap()
            assertEquals(ink, map[50, 90])
            assertEquals(paper, map[50, 10])
        }

    @Test
    fun aChangingValueRedrawsWithoutRecomposing() =
        runDitherTest {
            var value by mutableFloatStateOf(0f)
            val counter = CompositionCounter()
            setDitherContent {
                counter.Count()
                Square(Modifier.ditherProgress({ value }, theme = theme))
            }
            mainClock.advanceTimeByFrame()
            val before = counter.count
            assertEquals(1f, capture().shareOf(paper))
            value = 1f
            mainClock.advanceTimeByFrame()
            assertEquals(1f, capture().shareOf(ink))
            assertEquals(before, counter.count)
        }

    @Test
    fun reducedMotionDoesNotFreezeProgress() =
        runDitherTest {
            setDitherContent(StippleMotion.Reduced) { Square(Modifier.ditherProgress({ 1f }, theme = theme)) }
            assertEquals(1f, capture().shareOf(ink))
        }
}
