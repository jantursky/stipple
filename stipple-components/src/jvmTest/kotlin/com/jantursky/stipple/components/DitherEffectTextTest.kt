package com.jantursky.stipple.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.jantursky.stipple.StippleMotion
import com.jantursky.stipple.StippleTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DitherEffectTextTest {
    private val theme = StippleTheme.Light
    private val style = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold)

    // ---- scrambleText -------------------------------------------------------------------------------------------

    @Test
    fun aFinishedScrambleIsTheText() {
        assertEquals("Hello world", scrambleText("Hello world", 1f, 3, DEFAULT_SCRAMBLE_CHARSET))
    }

    @Test
    fun halfWayTheFirstHalfOfTheLettersIsReal() {
        val shown = scrambleText("abcdefghij", 0.5f, 7, "#")
        assertEquals("abcde#####", shown)
    }

    @Test
    fun spacesAndLineBreaksAreNeverScrambled() {
        val shown = scrambleText("ab cd\nef", 0f, 4, "#")
        assertEquals("## ##\n##", shown)
    }

    @Test
    fun aTickAndAProgressAlwaysGiveTheSameString() {
        val a = scrambleText("Stipple dithers", 0.2f, 5, DEFAULT_SCRAMBLE_CHARSET)
        assertEquals(a, scrambleText("Stipple dithers", 0.2f, 5, DEFAULT_SCRAMBLE_CHARSET))
        assertNotEquals(a, scrambleText("Stipple dithers", 0.2f, 6, DEFAULT_SCRAMBLE_CHARSET))
    }

    @Test
    fun theLengthNeverChanges() {
        for (p in listOf(0f, 0.3f, 0.99f, 1f)) {
            assertEquals(15, scrambleText("Stipple dithers", p, 9, "WWWWW@@@").length)
        }
    }

    // ---- layout ------------------------------------------------------------------------------------------------

    private fun sizeAt(
        effect: DitherTextEffect,
        text: String,
        p: Float,
        charset: String = DEFAULT_SCRAMBLE_CHARSET,
    ): IntSize {
        var size = IntSize.Zero
        runComponentTest {
            setComponent { DitherEffectText(text, effect, style = style, progress = { p }, charset = charset) }
            mainClock.advanceTimeByFrame()
            size = onNodeWithText(text).fetchSemanticsNode().size
        }
        return size
    }

    @Test
    fun theSizeIsTheFinalTextsInEveryFrame() {
        val text = "Stipple dithers everything"
        for (effect in DitherTextEffect.entries) {
            val sizes = listOf(0f, 0.4f, 1f).map { sizeAt(effect, text, it) }
            assertEquals(1, sizes.toSet().size, "$effect changed size: $sizes")
        }
    }

    @Test
    fun wideRandomCharactersDoNotChangeTheSize() {
        val text = "iiiiiiiiii iiiiiiiiii"
        val narrow = sizeAt(DitherTextEffect.Scramble, text, 1f)
        assertEquals(narrow, sizeAt(DitherTextEffect.Scramble, text, 0f, charset = "WWWWWWWW"))
    }

    // ---- accessibility and reduced motion ----------------------------------------------------------------------

    @Test
    fun aScreenReaderReadsTheFinalTextOnly() =
        runComponentTest {
            setComponent {
                DitherEffectText("Stipple", DitherTextEffect.Scramble, style = style, progress = { 0f })
            }
            mainClock.advanceTimeByFrame()
            val texts =
                onAllNodesWithTextProperty().map { it.config[SemanticsProperties.Text].joinToString { t -> t.text } }
            assertEquals(listOf("Stipple"), texts, "unmerged tree has scrambled text: $texts")
        }

    private fun androidx.compose.ui.test.ComposeUiTest.onAllNodesWithTextProperty() =
        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
            .fetchSemanticsNodes()

    private fun picture(
        effect: DitherTextEffect,
        motion: StippleMotion,
        p: Float,
    ): androidx.compose.ui.graphics.ImageBitmap {
        var image: androidx.compose.ui.graphics.ImageBitmap? = null
        runComponentTest {
            setComponent(motion, theme) {
                DitherEffectText("Stipple", effect, style = style, progress = { p })
            }
            mainClock.advanceTimeByFrame()
            image = capture()
        }
        return image!!
    }

    @Test
    fun reducedMotionShowsTheFinishedText() {
        for (effect in DitherTextEffect.entries) {
            val finished = picture(effect, StippleMotion.Full, 1f)
            val reduced = picture(effect, StippleMotion.Reduced, 0f)
            assertEquals(0, finished.pixelsDifferentFrom(reduced), "$effect")
        }
    }

    // ---- dissolve and the timeline -----------------------------------------------------------------------------

    @Test
    fun aDissolveStartsEmptyAndEndsWithTheWholeText() {
        val empty = picture(DitherTextEffect.Dissolve, StippleMotion.Full, 0f)
        val half = picture(DitherTextEffect.Dissolve, StippleMotion.Full, 0.5f)
        val full = picture(DitherTextEffect.Dissolve, StippleMotion.Full, 1f)
        val ink = theme.ink
        assertEquals(0f, empty.shareOf(ink), "something is lit at 0")
        assertTrue(full.shareOf(ink) > 0.02f, "nothing is drawn at 1")
        assertTrue(
            half.shareOf(ink) in 0.001f..full.shareOf(ink),
            "half is ${half.shareOf(ink)} of ${full.shareOf(ink)}",
        )
    }

    @Test
    fun theTimelinePlaysToTheFinishedText() {
        var played: androidx.compose.ui.graphics.ImageBitmap? = null
        runComponentTest {
            setComponent(StippleMotion.Full, theme) {
                DitherEffectText("Stipple", DitherTextEffect.Dissolve, style = style, durationMillis = 400)
            }
            mainClock.advanceTimeBy(1000)
            played = capture()
        }
        assertEquals(0, played!!.pixelsDifferentFrom(picture(DitherTextEffect.Dissolve, StippleMotion.Full, 1f)))
    }

    @Test
    fun theTimelineStartsFromNothingAfterItsDelay() {
        var early: androidx.compose.ui.graphics.ImageBitmap? = null
        runComponentTest {
            setComponent(StippleMotion.Full, theme) {
                DitherEffectText("Stipple", DitherTextEffect.Dissolve, style = style, delayMillis = 5000)
            }
            mainClock.advanceTimeBy(500)
            early = capture()
        }
        assertEquals(0f, early!!.shareOf(theme.ink))
    }

    @Test
    fun aTriggerPlaysTheTimelineAgain() =
        runComponentTest {
            var trigger by androidx.compose.runtime.mutableIntStateOf(0)
            setComponent(StippleMotion.Full, theme) {
                DitherEffectText(
                    "Stipple",
                    DitherTextEffect.Dissolve,
                    style = style,
                    durationMillis = 400,
                    trigger = trigger,
                )
            }
            mainClock.advanceTimeBy(1000)
            val finished = capture().shareOf(theme.ink)
            assertTrue(finished > 0.02f)
            trigger = 1
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            assertEquals(0f, capture().shareOf(theme.ink), "the timeline did not restart")
        }

    // ---- recomposition -----------------------------------------------------------------------------------------

    /**
     * A palette that counts how often a theme holding it is compared with another. Compose compares a modifier with
     * the one it replaces, theme included, only when the text recomposes and builds its modifiers again; drawing
     * never does. (Counting `size` would not do: the theme's ramp is this list, so every draw reads it.)
     */
    private class CountingPalette(
        private val colors: List<Color>,
    ) : AbstractList<Color>() {
        var comparisons = 0
        override val size: Int get() = colors.size

        override fun get(index: Int): Color = colors[index]

        override fun equals(other: Any?): Boolean {
            comparisons++
            return super.equals(other)
        }

        override fun hashCode(): Int = super.hashCode()
    }

    private fun readsAfterProgressChanges(effect: DitherTextEffect): Pair<Int, Int> {
        val palette = CountingPalette(listOf(theme.paper, theme.ink))
        var warm = 0
        var after = 0
        runComponentTest {
            var p by mutableFloatStateOf(0f)
            setComponent(StippleMotion.Full, StippleTheme.Light.copy(palette = palette)) {
                DitherEffectText("Stipple", effect, style = style, progress = { p })
            }
            repeat(3) { mainClock.advanceTimeByFrame() }
            warm = palette.comparisons
            for (value in listOf(0.2f, 0.5f, 0.8f, 1f)) {
                p = value
                repeat(2) { mainClock.advanceTimeByFrame() }
            }
            after = palette.comparisons
        }
        return warm to after
    }

    @Test
    fun aDissolveRedrawsWithoutRecomposing() {
        val (warm, after) = readsAfterProgressChanges(DitherTextEffect.Dissolve)
        assertEquals(warm, after, "the dissolve recomposed when only its progress changed")
    }

    @Test
    fun theCountingProbeSeesARecompositionWhenThereIsOne() {
        // A scramble reads its progress while composing, so the probe must register it: this keeps the test above honest.
        val (warm, after) = readsAfterProgressChanges(DitherTextEffect.Scramble)
        assertTrue(after > warm, "the probe saw no recomposition from a scramble: $warm then $after")
    }
}
