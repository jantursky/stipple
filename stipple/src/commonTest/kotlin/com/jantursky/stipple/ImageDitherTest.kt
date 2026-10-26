package com.jantursky.stipple

import com.jantursky.stipple.internal.CellGrid
import com.jantursky.stipple.internal.TRANSPARENT_CELL
import com.jantursky.stipple.internal.ditherCells
import com.jantursky.stipple.internal.sampleCells
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImageDitherTest {
    private fun run(
        tone: Float,
        algorithm: DitherAlgorithm,
        levels: Int = 2,
        size: Int = 32,
    ): IntArray =
        ditherCells(
            FloatArray(size * size) { tone },
            FloatArray(size * size) { 1f },
            size,
            size,
            algorithm,
            DitherPatterns.Bayer8,
            levels,
        )

    @Test
    fun everyAlgorithmKeepsPaperAndInkExact() {
        for (algorithm in DitherAlgorithm.entries) {
            assertTrue(run(0f, algorithm).all { it == 0 }, "$algorithm paper")
            // Atkinson drops part of the error, so a fully inked area stays ink all the same.
            assertTrue(run(1f, algorithm).all { it == 1 }, "$algorithm ink")
        }
    }

    @Test
    fun theShareOfInkFollowsTheToneForEveryAlgorithm() {
        for (algorithm in listOf(DitherAlgorithm.Ordered, DitherAlgorithm.FloydSteinberg)) {
            for (tone in listOf(0.25f, 0.5f, 0.75f)) {
                val out = run(tone, algorithm)
                val share = out.sum() / out.size.toFloat()
                assertTrue(share in (tone - 0.03f)..(tone + 0.03f), "$algorithm at $tone gave $share")
            }
        }
    }

    @Test
    fun atkinsonLosesSomeMidtoneBecauseItDiffusesThreeQuartersOfTheError() {
        val out = run(0.5f, DitherAlgorithm.Atkinson)
        val share = out.sum() / out.size.toFloat()
        assertTrue(share in 0.35f..0.65f, "gave $share")
    }

    @Test
    fun diffusionIsDeterministic() {
        assertTrue(run(0.37f, DitherAlgorithm.FloydSteinberg).contentEquals(run(0.37f, DitherAlgorithm.FloydSteinberg)))
    }

    @Test
    fun diffusionMakesMoreThanTwoLevelsFromAPalette() {
        val out = run(0.5f, DitherAlgorithm.FloydSteinberg, levels = 3)
        assertTrue(out.all { it in 0..2 })
        assertTrue(out.sum() / out.size.toFloat() in 0.9f..1.1f)
    }

    @Test
    fun transparentCellsAreMarked() {
        val tones = FloatArray(4) { 1f }
        val alpha = floatArrayOf(1f, 0.2f, 1f, 0.9f)
        val out = ditherCells(tones, alpha, 2, 2, DitherAlgorithm.Ordered, DitherPatterns.Bayer2, 2)
        assertEquals(listOf(1, TRANSPARENT_CELL, 1, 1), out.toList())
    }

    @Test
    fun cellsAverageThePixelsUnderThem() {
        // A 4 x 2 image: black on the left half, white on the right, sampled into 2 x 1 cells.
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        val pixels = intArrayOf(black, black, white, white, black, black, white, white)
        val samples = sampleCells(pixels, 4, 2, CellGrid(cell = 2f, columns = 2, rows = 1))
        assertEquals(0f, samples.luminance[0], 1e-4f)
        assertEquals(1f, samples.luminance[1], 1e-4f)
        assertEquals(1f, samples.alpha[0])
    }

    @Test
    fun luminanceIgnoresTransparentPixels() {
        val clear = 0x00FFFFFF
        val red = 0xFFFF0000.toInt()
        val samples = sampleCells(intArrayOf(clear, red), 2, 1, CellGrid(cell = 2f, columns = 1, rows = 1))
        assertEquals(0.2126f, samples.luminance[0], 1e-3f)
        assertEquals(0.5f, samples.alpha[0], 1e-4f)
    }

    @Test
    fun anImageIsStretchedOverTheGrid() {
        val gray = 0xFF808080.toInt()
        val samples = sampleCells(IntArray(3 * 3) { gray }, 3, 3, CellGrid(cell = 1f, columns = 5, rows = 4))
        assertEquals(20, samples.luminance.size)
        assertTrue(samples.luminance.all { it in 0.49f..0.51f })
    }
}
