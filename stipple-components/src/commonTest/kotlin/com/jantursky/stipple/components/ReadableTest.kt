package com.jantursky.stipple.components

import androidx.compose.ui.graphics.Color
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.contrastRatio
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ReadableTest {
    @Test
    fun aThemeWithEnoughContrastIsLeftAlone() {
        assertSame(StippleTheme.Light, readable(StippleTheme.Light, 4.5f))
    }

    @Test
    fun weakInkIsReplacedByBlackOnLightPaper() {
        val weak = StippleTheme(ink = Color(0xFFCCCCCC), paper = Color.White)
        val fixed = readable(weak, 4.5f)
        assertEquals(Color.Black, fixed.ink)
        assertEquals(Color.White, fixed.paper)
        assertTrue(fixed.contrastRatio >= 4.5f)
    }

    @Test
    fun weakInkIsReplacedByWhiteOnDarkPaper() {
        val fixed = readable(StippleTheme(ink = Color(0xFF222222), paper = Color(0xFF111111)), 4.5f)
        assertEquals(Color.White, fixed.ink)
    }

    @Test
    fun aPaletteIsDroppedWhenInkIsReplaced() {
        val weak =
            StippleTheme(
                ink = Color.LightGray,
                paper = Color.White,
                palette = listOf(Color.White, Color.Yellow, Color.LightGray),
            )
        assertEquals(null, readable(weak, 4.5f).palette)
    }

    @Test
    fun theLimitCanBeRaised() {
        val gray = StippleTheme(ink = Color(0xFF767676), paper = Color.White)
        assertSame(gray, readable(gray, 4.5f))
        assertTrue(contrastRatio(readable(gray, 7f).ink, Color.White) >= 7f)
    }

    @Test
    fun initialsTakeTheFirstLettersOfTwoWords() {
        assertEquals("AL", initials("Ada Lovelace"))
        assertEquals("GB", initials("  grace   brewster hopper "))
        assertEquals("AL", initials("Alan"))
        assertEquals("?", initials("   "))
    }
}
