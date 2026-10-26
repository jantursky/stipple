package com.jantursky.stipple

import com.jantursky.stipple.internal.bayerRanks
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PatternsTest {
    @Test
    fun bayer2MatchesTheClassicMatrix() {
        assertContentEquals(intArrayOf(0, 2, 3, 1), bayerRanks(2))
    }

    @Test
    fun bayer4MatchesTheClassicMatrix() {
        val expected =
            intArrayOf(
                0, 8, 2, 10,
                12, 4, 14, 6,
                3, 11, 1, 9,
                15, 7, 13, 5,
            )
        assertContentEquals(expected, bayerRanks(4))
    }

    @Test
    fun everyTileHoldsEachRankOnce() {
        for (pattern in DitherPatterns.All) {
            val seen = HashSet<Int>()
            for (y in 0 until pattern.size) {
                for (x in 0 until pattern.size) {
                    seen += (pattern.threshold(x, y) * pattern.size * pattern.size).toInt()
                }
            }
            assertEquals(pattern.size * pattern.size, seen.size, DitherPatterns.nameOf(pattern))
        }
    }

    @Test
    fun thresholdsStayInsideTheOpenInterval() {
        for (pattern in DitherPatterns.All) {
            for (y in 0 until pattern.size) {
                for (x in 0 until pattern.size) {
                    val v = pattern.threshold(x, y)
                    assertTrue(v > 0f && v < 1f, "${DitherPatterns.nameOf(pattern)} at $x,$y was $v")
                }
            }
        }
    }

    @Test
    fun thresholdsRepeatAndAcceptNegativeCoordinates() {
        val p = DitherPatterns.Bayer4
        assertEquals(p.threshold(1, 2), p.threshold(5, 6))
        assertEquals(p.threshold(3, 3), p.threshold(-1, -1))
    }

    @Test
    fun bayerRejectsSizesThatAreNotPowersOfTwo() {
        assertFailsWith<IllegalArgumentException> { bayerRanks(6) }
    }

    @Test
    fun theTileSizesAreAsNamed() {
        assertEquals(2, DitherPatterns.Bayer2.size)
        assertEquals(4, DitherPatterns.Bayer4.size)
        assertEquals(8, DitherPatterns.Bayer8.size)
        assertEquals(64, DitherPatterns.BlueNoise.size)
    }

    @Test
    fun blueNoiseHasNoLongRunsOfNeighboursOnTheSameSide() {
        // Blue noise pushes similar ranks apart: the cells below one half should not form big clumps.
        val p = DitherPatterns.BlueNoise
        var clumped = 0
        for (y in 0 until 64) {
            for (x in 0 until 63) {
                if (p.threshold(x, y) < 0.25f && p.threshold(x + 1, y) < 0.25f) clumped++
            }
        }
        // Random noise would give about 4032 * 1/16 = 252 pairs.
        assertTrue(clumped < 150, "found $clumped neighbouring dark pairs")
    }

    @Test
    fun namesOfBuiltInPatternsAreReadable() {
        assertEquals("Bayer 4x4", DitherPatterns.nameOf(DitherPatterns.Bayer4))
        assertEquals("Custom", DitherPatterns.nameOf(CustomPattern))
    }

    private object CustomPattern : DitherPattern {
        override val size = 1

        override fun threshold(
            x: Int,
            y: Int,
        ) = 0.5f
    }
}
