package com.jantursky.stipple

import com.jantursky.stipple.internal.quantize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuantizeTest {
    @Test
    fun twoLevelsMakeInkWhenToneIsAboveTheThreshold() {
        assertEquals(1, quantize(0.6f, 0.5f, 2))
        assertEquals(0, quantize(0.4f, 0.5f, 2))
    }

    @Test
    fun toneZeroIsAlwaysPaperAndToneOneAlwaysInk() {
        for (pattern in DitherPatterns.All) {
            for (y in 0 until pattern.size) {
                for (x in 0 until pattern.size) {
                    assertEquals(0, quantize(0f, pattern.threshold(x, y), 2))
                    assertEquals(1, quantize(1f, pattern.threshold(x, y), 2))
                }
            }
        }
    }

    @Test
    fun theShareOfInkFollowsTheTone() {
        val p = DitherPatterns.Bayer8
        for (tone in listOf(0.125f, 0.25f, 0.5f, 0.75f)) {
            var ink = 0
            for (y in 0 until 8) for (x in 0 until 8) ink += quantize(tone, p.threshold(x, y), 2)
            assertEquals((tone * 64).toInt(), ink, "tone $tone")
        }
    }

    @Test
    fun outOfRangeTonesAreClamped() {
        assertEquals(1, quantize(7f, 0.9f, 2))
        assertEquals(0, quantize(-3f, 0.1f, 2))
    }

    @Test
    fun aPaletteChoosesBetweenTheTwoNeighbouringColors() {
        // Three levels: tone 0.25 sits halfway between level 0 and 1.
        assertEquals(1, quantize(0.25f, 0.4f, 3))
        assertEquals(0, quantize(0.25f, 0.6f, 3))
        // Tone 0.75 sits halfway between level 1 and 2.
        assertEquals(2, quantize(0.75f, 0.4f, 3))
        assertEquals(1, quantize(0.75f, 0.6f, 3))
        assertTrue(quantize(1f, 0.99f, 3) == 2)
    }
}
