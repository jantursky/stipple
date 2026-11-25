package com.jantursky.stipple

import com.jantursky.stipple.internal.bayerRanks
import com.jantursky.stipple.internal.crosshatchRanks
import com.jantursky.stipple.internal.halftoneRanks
import com.jantursky.stipple.internal.hexGridRanks
import com.jantursky.stipple.internal.lineRanks
import com.jantursky.stipple.internal.shuffledRanks
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
                0,
                8,
                2,
                10,
                12,
                4,
                14,
                6,
                3,
                11,
                1,
                9,
                15,
                7,
                13,
                5,
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

    private val newPresets =
        listOf(
            DitherPatterns.Bayer16 to "Bayer 16x16",
            DitherPatterns.Halftone to "Halftone",
            DitherPatterns.HexGrid to "Hex grid",
            DitherPatterns.Lines to "Lines",
            DitherPatterns.Crosshatch to "Crosshatch",
            DitherPatterns.WhiteNoise to "White noise",
        )

    @Test
    fun everyGeneratedMapIsAPermutationOfItsRanks() {
        val maps =
            listOf(
                8 to halftoneRanks(8, true),
                8 to halftoneRanks(8, false),
                12 to halftoneRanks(12, false),
                8 to hexGridRanks(8),
                8 to lineRanks(8, true),
                8 to lineRanks(8, false),
                8 to crosshatchRanks(8),
                32 to shuffledRanks(32, 7L),
            )
        for ((size, ranks) in maps) {
            assertContentEquals((0 until size * size).toList().toIntArray(), ranks.sortedArray())
        }
    }

    @Test
    fun newPatternsRepeatEverySize() {
        for ((pattern, name) in newPresets) {
            val n = pattern.size
            for (y in 0 until n) {
                for (x in 0 until n) {
                    assertEquals(pattern.threshold(x, y), pattern.threshold(x + n, y + n), "$name at $x,$y")
                }
            }
        }
    }

    @Test
    fun whiteNoiseIsFixed() {
        val first = (0 until 8).map { (DitherPatterns.WhiteNoise.threshold(it, 0) * 1024f - 0.5f).toInt() }
        assertEquals(PINNED_NOISE, first)
    }

    @Test
    fun coverageFollowsTheToneWithinOneCell() {
        val patterns = newPresets.map { it.first } + DitherPatterns.halftone(12, false) + DitherPatterns.lines(6, false)
        for (pattern in patterns) {
            val cells = pattern.size * pattern.size
            for (tone in listOf(0.25f, 0.5f, 0.75f)) {
                var lit = 0
                for (y in 0 until pattern.size) {
                    for (x in 0 until pattern.size) {
                        if (pattern.threshold(x, y) <
                            tone
                        ) {
                            lit++
                        }
                    }
                }
                assertTrue(kotlin.math.abs(lit - tone * cells) <= 1f, "${pattern.size} at $tone lit $lit of $cells")
            }
        }
    }

    @Test
    fun halftoneStartsAsOneDotAtTheTileCentre() {
        val p = DitherPatterns.halftone(8, diagonal = false)
        val lit = (0 until 64).filter { p.threshold(it % 8, it / 8) < 0.1f }.toSet()
        assertTrue(lit.isNotEmpty())
        assertTrue((4 * 8 + 4) in lit, "centre cell is lit first")
        // Flood fill from the centre reaches every lit cell: one cluster, not scatter.
        val reached = mutableSetOf(4 * 8 + 4)
        val queue = ArrayDeque(listOf(4 * 8 + 4))
        while (queue.isNotEmpty()) {
            val c = queue.removeFirst()
            for ((dx, dy) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                val n = (c / 8 + dy).mod(8) * 8 + (c % 8 + dx).mod(8)
                if (n in lit && reached.add(n)) queue.addLast(n)
            }
        }
        assertEquals(lit, reached)
    }

    @Test
    fun theDiagonalHalftoneGrowsTwoDotsPerTile() {
        val p = DitherPatterns.Halftone
        val lit = (0 until 64).filter { p.threshold(it % 8, it / 8) < 0.1f }
        assertTrue((4 * 8 + 4) in lit)
        assertTrue(0 in lit, "second dot sits on the tile corner")
    }

    @Test
    fun allListsTheOldFourFirstThenTheNewPresets() {
        assertEquals(
            listOf(DitherPatterns.Bayer2, DitherPatterns.Bayer4, DitherPatterns.Bayer8, DitherPatterns.BlueNoise),
            DitherPatterns.All.take(4),
        )
        assertEquals(newPresets.map { it.first }, DitherPatterns.All.drop(4))
    }

    @Test
    fun everyPresetHasANameAndFactoriesAreCustom() {
        for ((pattern, name) in newPresets) assertEquals(name, DitherPatterns.nameOf(pattern))
        assertEquals("Custom", DitherPatterns.nameOf(DitherPatterns.halftone(8)))
        assertEquals("Custom", DitherPatterns.nameOf(DitherPatterns.lines(8)))
    }

    @Test
    fun factoriesRejectBadSizes() {
        assertFailsWith<IllegalArgumentException> { DitherPatterns.halftone(1) }
        assertFailsWith<IllegalArgumentException> { DitherPatterns.halftone(257) }
        assertFailsWith<IllegalArgumentException> { DitherPatterns.lines(1) }
        assertFailsWith<IllegalArgumentException> { DitherPatterns.lines(257) }
        assertEquals(256, DitherPatterns.lines(256).size)
        assertEquals(12, DitherPatterns.halftone(size = 12, diagonal = false).size)
    }

    private object CustomPattern : DitherPattern {
        override val size = 1

        override fun threshold(
            x: Int,
            y: Int,
        ) = 0.5f
    }

    private companion object {
        val PINNED_NOISE = listOf(262, 923, 50, 233, 97, 976, 277, 1019)
    }
}
