package com.jantursky.stipple.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.StippleMotion
import com.jantursky.stipple.StippleTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ComponentsTest {
    private val light = StippleTheme.Light
    private val dark = StippleTheme.Dark

    @Test
    fun buttonClicks() =
        runComponentTest {
            var clicks = 0
            setComponent { DitherButton("Save", onClick = { clicks++ }) }
            onNodeWithText("Save").assertHasClickAction().performClick()
            assertEquals(1, clicks)
        }

    @Test
    fun aDisabledButtonDoesNotClick() =
        runComponentTest {
            var clicks = 0
            setComponent { DitherButton("Save", onClick = { clicks++ }, enabled = false) }
            onNodeWithText("Save").assertIsNotEnabled()
            assertEquals(0, clicks)
        }

    @Test
    fun theButtonShadowIsDitheredAndTheFaceIsPaper() =
        runComponentTest {
            setComponent { DitherButton("Go", onClick = {}, modifier = Modifier.width(120.dp), theme = light) }
            val image = capture()
            // The face is flat paper inside its outline, so the label keeps full contrast.
            assertTrue(image.shareOf(light.paper, 10, 10, 50, 20) > 0.7f)
            // Bottom-right of the button is its shadow: ink dots between paper.
            val shadow = image.shareOf(light.ink, 8, 36, 100, 5)
            assertTrue(shadow > 0.1f, "shadow ink share $shadow")
        }

    @Test
    fun barChartDescribesItself() =
        runComponentTest {
            setComponent {
                DitherBarChart(listOf(BarEntry("Mon", 3f), BarEntry("Tue", 7.5f)), Modifier.size(200.dp))
            }
            onNodeWithContentDescription("Mon 3, Tue 7.5").assertExists()
        }

    @Test
    fun barChartColumnsGrowWithTheirValue() =
        runComponentTest {
            setComponent {
                DitherBarChart(
                    listOf(BarEntry("A", 10f), BarEntry("B", 5f)),
                    Modifier.size(200.dp),
                    showLabels = false,
                    theme = light.copy(cell = 1.dp),
                    barGap = 20.dp,
                )
            }
            val image = capture()
            // The first column spans x 0..90, the second 110..200. The tall one is inked near the top, the short one is not.
            assertTrue(image.shareOf(light.ink, 0, 10, 90, 20) > 0.3f)
            assertTrue(image.shareOf(light.ink, 110, 10, 90, 20) < 0.01f)
            assertTrue(image.shareOf(light.ink, 110, 150, 90, 40) > 0.3f)
        }

    @Test
    fun barChartGrowsInWhenAnimated() =
        runComponentTest {
            setComponent {
                DitherBarChart(
                    listOf(BarEntry("A", 10f)),
                    Modifier.size(100.dp),
                    showLabels = false,
                    animated = true,
                    theme = light.copy(cell = 1.dp),
                )
            }
            mainClock.advanceTimeByFrame()
            val start = capture().shareOf(light.ink)
            mainClock.advanceTimeBy(2000)
            assertTrue(capture().shareOf(light.ink) > start + 0.3f)
        }

    @Test
    fun reducedMotionSkipsTheGrowIn() =
        runComponentTest {
            setComponent(StippleMotion.Reduced) {
                DitherBarChart(
                    listOf(BarEntry("A", 10f)),
                    Modifier.size(100.dp),
                    showLabels = false,
                    animated = true,
                    theme = light.copy(cell = 1.dp),
                )
            }
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            assertTrue(capture().shareOf(light.ink) > 0.3f)
        }

    @Test
    fun gridDrawsSquaresWithGaps() =
        runComponentTest {
            setComponent {
                DitherGrid(
                    listOf(1f, 0f, 0f, 1f),
                    columns = 2,
                    Modifier.size(100.dp),
                    theme = light,
                    gap = 0.2f,
                    cell = 1.dp,
                )
            }
            val image = capture()
            assertEquals(1f, image.shareOf(light.ink, 10, 10, 30, 30))
            assertEquals(1f, image.shareOf(light.paper, 60, 10, 30, 30))
            assertEquals(1f, image.shareOf(light.paper, 48, 0, 4, 100), "the gap between columns is paper")
        }

    @Test
    fun gridKeepsItsSquaresSquare() =
        runComponentTest {
            setComponent {
                DitherGrid(List(6) { 1f }, columns = 3, Modifier.width(150.dp), theme = light, cell = 1.dp)
            }
            val node = onRoot().fetchSemanticsNode()
            // 3 columns, 2 rows: 150 x 100.
            assertEquals(150, node.size.width.coerceAtMost(150))
            assertEquals(100, onNodeRoot().height)
        }

    private fun androidx.compose.ui.test.ComposeUiTest.onNodeRoot() =
        onRoot()
            .fetchSemanticsNode()
            .children
            .first()
            .size

    @Test
    fun gaugeReportsItsValue() =
        runComponentTest {
            setComponent { DitherGauge(0.4f, Modifier.size(100.dp), contentDescription = "Battery") }
            val info =
                onNodeWithContentDescription(
                    "Battery",
                ).fetchSemanticsNode().config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)
            assertEquals(ProgressBarRangeInfo(0.4f, 0f..1f), info)
        }

    @Test
    fun gaugeFillsToItsValueAndKeepsTheTrackFaint() =
        runComponentTest {
            setComponent { DitherGauge(0.5f, Modifier.size(200.dp), theme = light.copy(cell = 1.dp)) }
            val image = capture()
            // The ring runs clockwise from the bottom left over the top. At half way the fill reaches the top; the
            // left side of the ring is filled and the right side is only track.
            val left = image.shareOf(light.ink, 5, 70, 30, 60)
            val right = image.shareOf(light.ink, 165, 70, 30, 60)
            assertTrue(left > right + 0.2f, "left $left right $right")
        }

    @Test
    fun gaugeShowsItsContent() =
        runComponentTest {
            setComponent {
                DitherGauge(
                    0.5f,
                    Modifier.size(100.dp),
                ) {
                    androidx.compose.foundation.text
                        .BasicText("50%")
                }
            }
            onNodeWithText("50%").assertExists()
        }

    @Test
    fun statCardReadsOutLabelValueAndDelta() =
        runComponentTest {
            setComponent { DitherStatCard("Revenue", "$12.4k", delta = "+8%", trend = listOf(1f, 3f, 2f, 5f)) }
            onNodeWithContentDescription("Revenue: $12.4k, +8%").assertExists()
        }

    @Test
    fun statCardDrawsItsTrendBelowTheText() =
        runComponentTest {
            setComponent {
                DitherStatCard(
                    "Visits",
                    "9",
                    Modifier.width(200.dp),
                    trend = listOf(1f, 2f, 3f, 4f),
                    theme = light,
                    trendHeight = 80.dp,
                )
            }
            val image = capture()
            assertTrue(image.shareOf(light.ink, 10, image.height - 60, 180, 50) > 0.1f)
            assertEquals(1f, image.shareOf(light.paper, 100, 5, 90, 60), "nothing is dithered behind the value")
        }

    @Test
    fun progressBarReportsItsProgress() =
        runComponentTest {
            setComponent { DitherProgressBar(0.25f, contentDescription = "Upload") }
            val info =
                onNodeWithContentDescription(
                    "Upload",
                ).fetchSemanticsNode().config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)
            assertEquals(ProgressBarRangeInfo(0.25f, 0f..1f), info)
        }

    @Test
    fun anIndeterminateProgressBarSaysSo() =
        runComponentTest {
            setComponent { DitherProgressBar(null, contentDescription = "Loading") }
            val info =
                onNodeWithContentDescription(
                    "Loading",
                ).fetchSemanticsNode().config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)
            assertEquals(ProgressBarRangeInfo.Indeterminate, info)
        }

    @Test
    fun anIndeterminateBarMovesOnlyWhenAnimated() =
        runComponentTest {
            setComponent {
                DitherProgressBar(
                    null,
                    Modifier.width(200.dp),
                    animated = true,
                    theme = light.copy(cell = 1.dp),
                )
            }
            mainClock.advanceTimeByFrame()
            val first = capture()
            mainClock.advanceTimeBy(600)
            assertNotEquals(0, first.pixelsDifferentFrom(capture()))
        }

    @Test
    fun anIndeterminateBarRestsUnderReducedMotion() =
        runComponentTest {
            setComponent(StippleMotion.Reduced) {
                DitherProgressBar(null, Modifier.width(200.dp), animated = true, theme = light.copy(cell = 1.dp))
            }
            mainClock.advanceTimeByFrame()
            val first = capture()
            mainClock.advanceTimeBy(600)
            assertEquals(0, first.pixelsDifferentFrom(capture()))
        }

    @Test
    fun theProgressFillMatchesTheProgress() =
        runComponentTest {
            setComponent {
                DitherProgressBar(
                    0.5f,
                    Modifier.width(200.dp),
                    theme = light.copy(cell = 1.dp),
                    track = 0f,
                )
            }
            val image = capture()
            assertTrue(image.shareOf(light.ink, 4, 4, 80, 6) > 0.9f)
            assertTrue(image.shareOf(light.ink, 120, 4, 70, 6) < 0.01f)
        }

    @Test
    fun textKeepsItsMeaning() =
        runComponentTest {
            setComponent { DitherText("Stipple") }
            onNodeWithText("Stipple").assertExists()
        }

    @Test
    fun textIsInkedWhereItIsDrawn() =
        runComponentTest {
            setComponent {
                DitherText("MMM", theme = light, fadeTo = 1f, cell = 1.dp)
            }
            val image = capture()
            assertTrue(image.shareOf(light.ink) > 0.1f)
            assertEquals(0f, image.shareOf(Color.Red))
        }

    @Test
    fun anAvatarNamesItself() =
        runComponentTest {
            setComponent { DitherAvatar("Ada Lovelace") }
            onNodeWithContentDescription("Ada Lovelace").assertContentDescriptionEquals("Ada Lovelace")
            onNodeWithText("AL").assertExists()
        }

    @Test
    fun theSameNameMakesTheSameAvatar() =
        runComponentTest {
            setComponent { DitherAvatar("Grace Hopper", size = 100.dp) }
            val first = capture()
            setComponent { DitherAvatar("Grace Hopper", size = 100.dp) }
            assertEquals(0, first.pixelsDifferentFrom(capture()))
        }

    @Test
    fun differentNamesMakeDifferentAvatars() =
        runComponentTest {
            setComponent { DitherAvatar("Grace Hopper", size = 100.dp) }
            val first = capture()
            setComponent { DitherAvatar("Alan Turing", size = 100.dp) }
            assertNotEquals(0, first.pixelsDifferentFrom(capture()))
        }

    @Test
    fun aPhotoAvatarIsDithered() =
        runComponentTest {
            setComponent {
                DitherAvatar("Ada", size = 100.dp, painter = ColorPainter(Color.Black), theme = light.copy(cell = 1.dp))
            }
            waitUntil(timeoutMillis = 5_000) { capture().shareOf(light.ink) > 0.5f }
        }

    @Test
    fun anImageIsDitheredAndRevealedFromTheLeft() =
        runComponentTest {
            val red = Color(0xFFFF0000)
            setComponent {
                DitherImage(
                    ColorPainter(red),
                    "A red field",
                    Modifier.size(100.dp),
                    theme = light.copy(cell = 1.dp),
                    reveal = 0.5f,
                )
            }
            waitUntil(timeoutMillis = 5_000) { capture().shareOf(red, 0, 0, 40, 100) > 0.5f }
            val image = capture()
            assertTrue(image.shareOf(red, 0, 0, 40, 100) > 0.9f, "the left shows the original")
            assertEquals(0f, image.shareOf(red, 60, 0, 40, 100), "the right does not")
        }

    @Test
    fun anImageHasADescription() =
        runComponentTest {
            setComponent { DitherImage(ColorPainter(Color.Gray), "Gray square", Modifier.size(50.dp)) }
            onNodeWithContentDescription("Gray square").assertExists()
        }

    @Test
    fun aBackgroundShowsItsContentOverTheDither() =
        runComponentTest {
            setComponent {
                DitherBackground(Modifier.fillMaxSize(), theme = light) {
                    Box(Modifier.size(40.dp).background(Color.Red))
                }
            }
            val image = capture()
            assertTrue(image.shareOf(light.ink) > 0.01f, "dither drawn")
            assertTrue(image.shareOf(Color.Red, 0, 0, 40, 40) == 1f, "content over it")
        }

    @Test
    fun animatedBackgroundMovesUnlessMotionIsReduced() =
        runComponentTest {
            setComponent { DitherBackground(Modifier.size(100.dp), animated = true, theme = light.copy(cell = 1.dp)) }
            mainClock.advanceTimeByFrame()
            val first = capture()
            mainClock.advanceTimeBy(1500)
            assertNotEquals(0, first.pixelsDifferentFrom(capture()))
        }

    @Test
    fun framesDoNotRecomposeAnyAnimatedComponent() =
        runComponentTest {
            val counter = Counter()
            setComponent {
                counter.Count()
                Box(Modifier.size(300.dp)) {
                    DitherButton("Go", onClick = {}, animated = true)
                    DitherBarChart(listOf(BarEntry("A", 1f)), Modifier.size(50.dp), animated = true)
                    DitherGauge(0.5f, Modifier.size(50.dp), animated = true)
                    DitherProgressBar(0.5f, animated = true)
                    DitherProgressBar(null, animated = true)
                    DitherGrid(listOf(0.5f), 1, Modifier.size(50.dp), animated = true)
                    DitherText("Hi", animated = true)
                    DitherAvatar("A B", animated = true)
                    DitherBackground(Modifier.size(50.dp), animated = true)
                    DitherStatCard("a", "b", trend = listOf(1f, 2f), animated = true)
                }
            }
            mainClock.advanceTimeBy(3000)
            val before = counter.count
            repeat(120) { mainClock.advanceTimeByFrame() }
            assertEquals(before, counter.count)
        }

    @Test
    fun darkThemeFlipsTheColors() =
        runComponentTest {
            setComponent(theme = dark) { DitherProgressBar(1f, Modifier.width(200.dp), theme = dark.copy(cell = 1.dp)) }
            assertTrue(capture().shareOf(dark.ink, 4, 4, 190, 6) > 0.9f)
        }
}
