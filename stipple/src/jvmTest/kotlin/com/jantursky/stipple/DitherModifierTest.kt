package com.jantursky.stipple

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DitherModifierTest {
    private val ink = StippleTheme.Light.ink
    private val paper = StippleTheme.Light.paper
    private val bayer8 = DitherPatterns.Bayer8
    private val cell1 = StippleTheme.Light.copy(cell = 1.dp)

    @Test
    fun toneZeroIsAllPaperAndToneOneAllInk() =
        runDitherTest {
            setDitherContent { Square(Modifier.dither(DitherFields.solid(0f), theme = cell1)) }
            assertEquals(1f, capture().shareOf(paper))
            setDitherContent { Square(Modifier.dither(DitherFields.solid(1f), theme = cell1)) }
            assertEquals(1f, capture().shareOf(ink))
        }

    @Test
    fun theShareOfInkFollowsTheTone() =
        runDitherTest {
            setDitherContent {
                Square(Modifier.dither(DitherFields.solid(0.25f), bayer8, cell1))
            }
            val share = capture().shareOf(ink)
            assertTrue(share in 0.24f..0.26f, "ink share $share")
        }

    @Test
    fun cellsAreAsBigAsTheThemeSays() =
        runDitherTest {
            val theme = StippleTheme.Light.copy(cell = 10.dp)
            setDitherContent { Square(Modifier.dither(DitherFields.solid(0.5f), DitherPatterns.Bayer2, theme)) }
            val map = capture().toPixelMap()
            // Within one 10 x 10 cell every pixel has the same color.
            for (y in 0 until 10) for (x in 0 until 10) assertEquals(map[0, 0], map[x, y])
        }

    @Test
    fun theDitherSitsBehindTheContent() =
        runDitherTest {
            setDitherContent {
                Box(Modifier.dither(DitherFields.solid(1f)).fillMaxSize()) {
                    Box(Modifier.size(20.dp).dither(DitherFields.solid(0f), theme = cell1))
                }
            }
            assertEquals(paper, capture().toPixelMap()[5, 5])
            assertEquals(ink, capture().toPixelMap()[50, 50])
        }

    @Test
    fun theThemeComesFromTheCompositionLocal() =
        runDitherTest {
            val dark = StippleTheme.Dark.copy(cell = 1.dp)
            setDitherContent(theme = dark) { Square(Modifier.dither(DitherFields.solid(0f))) }
            assertEquals(1f, capture().shareOf(dark.paper))
        }

    @Test
    fun aThemeChangeRedrawsTheDither() =
        runDitherTest {
            var theme by mutableStateOf(StippleTheme.Light.copy(cell = 1.dp))
            setDitherContent { Square(Modifier.dither(DitherFields.solid(0f), theme = theme)) }
            assertEquals(1f, capture().shareOf(paper))
            theme = StippleTheme.Dark.copy(cell = 1.dp)
            mainClock.advanceTimeByFrame()
            assertEquals(1f, capture().shareOf(StippleTheme.Dark.paper))
        }

    @Test
    fun aPaletteMixesItsColors() =
        runDitherTest {
            val mid = Color(0xFFFF0000)
            val theme = StippleTheme.Light.copy(palette = listOf(paper, mid, ink), cell = 1.dp)
            setDitherContent { Square(Modifier.dither(DitherFields.solid(0.5f), bayer8, theme)) }
            assertEquals(1f, capture().shareOf(mid))
        }

    @Test
    fun dotsLeavePaperBetweenThem() =
        runDitherTest {
            val theme = StippleTheme.Light.copy(cell = 10.dp, shape = CellShape.Dot)
            setDitherContent { Square(Modifier.dither(DitherFields.solid(1f), theme = theme)) }
            val map = capture().toPixelMap()
            assertEquals(ink, map[5, 5])
            assertEquals(paper, map[0, 0], "the corner of a cell is outside the dot")
        }

    @Test
    fun aShapeClipsTheDither() =
        runDitherTest {
            setDitherContent { Square(Modifier.dither(DitherFields.solid(1f), theme = cell1, shape = CircleShape)) }
            val map = capture().toPixelMap()
            assertEquals(ink, map[50, 50])
            assertNotEquals(ink, map[1, 1])
        }

    @Test
    fun anAnimatedDitherMovesAndAStillOneDoesNot() =
        runDitherTest {
            val field = DitherFields.waves(speed = 1f)
            setDitherContent { Square(Modifier.dither(field, animated = true, theme = cell1)) }
            mainClock.advanceTimeByFrame()
            val first = capture()
            mainClock.advanceTimeBy(300)
            assertTrue(first.pixelsDifferentFrom(capture()) > 0, "animated dither stood still")
        }

    @Test
    fun aStillDitherStaysPut() =
        runDitherTest {
            val field = DitherFields.waves(speed = 1f)
            setDitherContent { Square(Modifier.dither(field, theme = cell1)) }
            mainClock.advanceTimeByFrame()
            val first = capture()
            mainClock.advanceTimeBy(300)
            assertEquals(0, first.pixelsDifferentFrom(capture()))
        }

    @Test
    fun reducedMotionFreezesAtTheStillFrame() =
        runDitherTest {
            val field = DitherFields.waves(speed = 1f)
            setDitherContent(StippleMotion.Reduced) {
                Square(Modifier.dither(field, animated = true, stillTime = 0.5f, theme = cell1))
            }
            mainClock.advanceTimeByFrame()
            val first = capture()
            mainClock.advanceTimeBy(500)
            assertEquals(0, first.pixelsDifferentFrom(capture()))
        }

    @Test
    fun reducedMotionShowsTheFrameAtStillTime() =
        runDitherTest {
            val field = DitherFields.waves(speed = 1f)
            setDitherContent(StippleMotion.Reduced) {
                Square(Modifier.dither(field, animated = true, stillTime = 0.5f, theme = cell1))
            }
            val reduced = capture()
            setDitherContent { Square(Modifier.dither(field, stillTime = 0.5f, theme = cell1)) }
            assertEquals(0, reduced.pixelsDifferentFrom(capture()))
        }

    @Test
    fun switchingReducedMotionOnStopsTheClock() =
        runDitherTest {
            var motion by mutableStateOf(StippleMotion.Full)
            val field = DitherFields.waves(speed = 1f)
            setContent {
                CompositionLocalProvider(
                    LocalDensity provides Density(1f),
                    LocalStippleMotion provides motion,
                ) { Square(Modifier.dither(field, animated = true, theme = cell1)) }
            }
            mainClock.advanceTimeBy(200)
            motion = StippleMotion.Reduced
            mainClock.advanceTimeByFrame()
            val frozen = capture()
            mainClock.advanceTimeBy(500)
            assertEquals(0, frozen.pixelsDifferentFrom(capture()))
        }

    @Test
    fun framesDoNotRecompose() =
        runDitherTest {
            val counter = CompositionCounter()
            val field = DitherFields.noise()
            setDitherContent {
                counter.Count()
                Square(Modifier.dither(field, animated = true, theme = cell1))
            }
            mainClock.advanceTimeByFrame()
            val before = counter.count
            repeat(60) { mainClock.advanceTimeByFrame() }
            assertEquals(before, counter.count)
        }

    @Test
    fun keyFramesLookRight() =
        runDitherTest {
            val theme = StippleTheme.Light.copy(cell = 4.dp)
            setDitherContent {
                Box(Modifier.fillMaxSize()) {
                    Square(Modifier.dither(DitherFields.linear(), DitherPatterns.Bayer4, theme))
                }
            }
            onRoot().captureRoboImage("src/jvmTest/screenshots/linear_bayer4.png")
        }
}
