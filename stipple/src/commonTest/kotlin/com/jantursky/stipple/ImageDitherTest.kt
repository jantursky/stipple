package com.jantursky.stipple

import com.jantursky.stipple.internal.CellGrid
import com.jantursky.stipple.internal.TRANSPARENT_CELL
import com.jantursky.stipple.internal.boxDownscale
import com.jantursky.stipple.internal.cellGrid
import com.jantursky.stipple.internal.ditherCells
import com.jantursky.stipple.internal.kernelOf
import com.jantursky.stipple.internal.readbackSize
import com.jantursky.stipple.internal.sampleCells
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
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

    // The 1.1.0 implementation, kept here so the kernel table is pinned to what shipped.
    private class Tap(val dx: Int, val dy: Int, val weight: Float)

    private fun reference(
        tones: FloatArray,
        columns: Int,
        rows: Int,
        levels: Int,
        taps: List<Tap>,
    ): IntArray {
        val out = IntArray(columns * rows)
        val work = tones.copyOf()
        val top = levels - 1
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val i = row * columns + column
                val wanted = work[i].coerceIn(0f, 1f)
                val level = kotlin.math.floor(wanted * top + 0.5f).toInt().coerceIn(0, top)
                out[i] = level
                val error = wanted - level.toFloat() / top
                for (tap in taps) {
                    val x = column + tap.dx
                    val y = row + tap.dy
                    if (x in 0 until columns && y < rows) work[y * columns + x] += error * tap.weight
                }
            }
        }
        return out
    }

    private val gradient = FloatArray(40 * 30) { (it % 40) / 39f * 0.8f + (it / 40) / 29f * 0.2f }

    private fun gradientRun(
        algorithm: DitherAlgorithm,
        options: ImageDitherOptions = ImageDitherOptions.Default,
    ) = ditherCells(gradient, FloatArray(gradient.size) { 1f }, 40, 30, algorithm, DitherPatterns.Bayer8, 2, options)

    @Test
    fun floydSteinbergAndAtkinsonMatchTheFirstRelease() {
        val fs = listOf(Tap(1, 0, 7f / 16f), Tap(-1, 1, 3f / 16f), Tap(0, 1, 5f / 16f), Tap(1, 1, 1f / 16f))
        val atkinson =
            listOf(Tap(1, 0, 1f / 8f), Tap(2, 0, 1f / 8f), Tap(-1, 1, 1f / 8f), Tap(0, 1, 1f / 8f), Tap(1, 1, 1f / 8f), Tap(0, 2, 1f / 8f))
        assertTrue(reference(gradient, 40, 30, 2, fs).contentEquals(gradientRun(DitherAlgorithm.FloydSteinberg)))
        assertTrue(reference(gradient, 40, 30, 2, atkinson).contentEquals(gradientRun(DitherAlgorithm.Atkinson)))
        assertTrue(reference(gradient, 40, 30, 3, fs).contentEquals(
            ditherCells(gradient, FloatArray(gradient.size) { 1f }, 40, 30, DitherAlgorithm.FloydSteinberg, DitherPatterns.Bayer8, 3),
        ))
    }

    @Test
    fun everyNewKernelKeepsAFlatMidGreyAtHalfInk() {
        for (algorithm in DitherAlgorithm.entries.filter { it != DitherAlgorithm.Ordered }) {
            val out = run(0.5f, algorithm, size = 64)
            val share = out.sum() / out.size.toFloat()
            assertTrue(share in 0.47f..0.53f || algorithm == DitherAlgorithm.Atkinson, "$algorithm gave $share")
        }
    }

    @Test
    fun kernelWeightsSumToOneExceptAtkinson() {
        for (algorithm in DitherAlgorithm.entries.filter { it != DitherAlgorithm.Ordered }) {
            val kernel = kernelOf(algorithm)!!
            val sum = kernel.weight.sum()
            val expected = if (algorithm == DitherAlgorithm.Atkinson) 0.75f else 1f
            assertEquals(expected, sum, 1e-5f, "$algorithm")
        }
    }

    @Test
    fun newKernelsDiffuseOnlyForwardAndDown() {
        for (algorithm in DitherAlgorithm.entries.filter { it != DitherAlgorithm.Ordered }) {
            val kernel = kernelOf(algorithm)!!
            for (i in kernel.weight.indices) {
                assertTrue(kernel.dy[i] > 0 || (kernel.dy[i] == 0 && kernel.dx[i] > 0), "$algorithm tap $i")
            }
        }
    }

    @Test
    fun serpentineMirrorsOddRows() {
        val tones = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.35f)
        val alpha = FloatArray(12) { 1f }
        val plain = ditherCells(tones, alpha, 6, 2, DitherAlgorithm.FloydSteinberg, DitherPatterns.Bayer8, 2)
        val serpentine =
            ditherCells(
                tones, alpha, 6, 2, DitherAlgorithm.FloydSteinberg, DitherPatterns.Bayer8, 2,
                ImageDitherOptions(serpentine = true),
            )
        // Row 0 is untouched. Row 1 scanned right to left gives what a left-to-right scan of the reversed row gives.
        assertEquals(plain.toList().subList(0, 6), serpentine.toList().subList(0, 6))
        val reversedRow = floatArrayOf(0.35f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f)
        val expected = ditherCells(reversedRow, FloatArray(6) { 1f }, 6, 1, DitherAlgorithm.FloydSteinberg, DitherPatterns.Bayer8, 2)
        assertEquals(expected.reversed(), serpentine.toList().subList(6, 12))
    }

    @Test
    fun serpentineOffLeavesTheOutputUnchangedAndOnChangesIt() {
        for (algorithm in DitherAlgorithm.entries.filter { it != DitherAlgorithm.Ordered }) {
            val off = gradientRun(algorithm, ImageDitherOptions(serpentine = false))
            assertTrue(off.contentEquals(gradientRun(algorithm)), "$algorithm default")
            assertFalse(off.contentEquals(gradientRun(algorithm, ImageDitherOptions(serpentine = true))), "$algorithm on")
        }
    }

    @Test
    fun neutralOptionsEqualNoOptions() {
        assertEquals(ImageDitherOptions.Default, ImageDitherOptions(0f, 1f, 1f, 0f, 1f))
        assertEquals(ImageDitherOptions.Default.hashCode(), ImageDitherOptions().hashCode())
        assertNotEquals(ImageDitherOptions.Default, ImageDitherOptions(brightness = 0.1f))
        val neutral = ImageDitherOptions(0f, 1f, 1f, 0f, 1f)
        for (algorithm in DitherAlgorithm.entries) {
            assertTrue(gradientRun(algorithm).contentEquals(gradientRun(algorithm, neutral)), "$algorithm")
        }
        val gray = IntArray(16) { 0xFF7F7F7F.toInt() }
        val grid = CellGrid(cell = 2f, columns = 2, rows = 2)
        assertEquals(
            sampleCells(gray, 4, 4, grid).luminance.toList(),
            sampleCells(gray, 4, 4, grid, neutral).luminance.toList(),
        )
    }

    @Test
    fun everySettingCountsForEquality() {
        val base = ImageDitherOptions()
        val changed =
            listOf(
                base.copy(brightness = 0.1f),
                base.copy(contrast = 2f),
                base.copy(gamma = 2f),
                base.copy(threshold = 0.1f),
                base.copy(strength = 0.5f),
                base.copy(serpentine = true),
                base.copy(linearLuminance = true),
                base.copy(sampling = 3),
            )
        for (other in changed) {
            assertNotEquals(base, other)
            assertNotEquals<Any?>(base, "options")
        }
        assertEquals(changed.size, changed.map { it.hashCode() }.toSet().size)
        // A near-black cell takes the linear segment of the sRGB curve on its way back.
        val dark = IntArray(4) { 0xFF030303.toInt() }
        val grid = CellGrid(cell = 2f, columns = 1, rows = 1)
        assertEquals(
            3f / 255f,
            sampleCells(dark, 2, 2, grid, ImageDitherOptions(linearLuminance = true)).luminance[0],
            0.002f,
        )
    }

    @Test
    fun optionsAreClampedAndCopied() {
        val options = ImageDitherOptions(brightness = 5f, contrast = -1f, gamma = 0f, strength = 2f, sampling = 0)
        assertEquals(1f, options.brightness)
        assertEquals(0f, options.contrast)
        assertEquals(0.1f, options.gamma)
        assertEquals(1f, options.strength)
        assertEquals(1, options.sampling)
        assertEquals(options.copy(), options)
        assertEquals(0.5f, options.copy(threshold = 0.5f).threshold)
        assertTrue(options.toString().contains("sampling=1"))
    }

    private fun shareAt(options: ImageDitherOptions): Float {
        val gray = IntArray(16) { 0xFF808080.toInt() }
        val grid = CellGrid(cell = 2f, columns = 4, rows = 4)
        val samples = sampleCells(gray, 4, 4, grid, options)
        val out = ditherCells(samples.luminance, samples.alpha, 4, 4, DitherAlgorithm.Ordered, DitherPatterns.Bayer4, 2, options)
        return out.sum() / out.size.toFloat()
    }

    @Test
    fun brightnessContrastGammaAndThresholdMoveTheInk() {
        val base = shareAt(ImageDitherOptions.Default)
        assertTrue(shareAt(ImageDitherOptions(brightness = 0.3f)) > base)
        assertTrue(shareAt(ImageDitherOptions(brightness = -0.3f)) < base)
        assertTrue(shareAt(ImageDitherOptions(gamma = 3f)) > base)
        assertTrue(shareAt(ImageDitherOptions(gamma = 0.3f)) < base)
        assertTrue(shareAt(ImageDitherOptions(threshold = -0.3f)) > base)
        assertTrue(shareAt(ImageDitherOptions(threshold = 0.3f)) < base)
        val dark = IntArray(4) { 0xFF404040.toInt() }
        val grid = CellGrid(cell = 2f, columns = 1, rows = 1)
        val plain = sampleCells(dark, 2, 2, grid).luminance[0]
        assertTrue(sampleCells(dark, 2, 2, grid, ImageDitherOptions(contrast = 2f)).luminance[0] < plain)
    }

    @Test
    fun strengthZeroRoundsEveryCellOnItsOwn() {
        val out = run(0.4f, DitherAlgorithm.FloydSteinberg)
        val rounded = ditherCells(
            FloatArray(32 * 32) { 0.4f }, FloatArray(32 * 32) { 1f }, 32, 32,
            DitherAlgorithm.FloydSteinberg, DitherPatterns.Bayer8, 2, ImageDitherOptions(strength = 0f),
        )
        assertTrue(rounded.all { it == 0 }, "0.4 rounds to paper with no error pushed on")
        assertTrue(out.any { it == 1 })
        val high = ditherCells(
            FloatArray(4) { 0.6f }, FloatArray(4) { 1f }, 2, 2,
            DitherAlgorithm.Stucki, DitherPatterns.Bayer8, 2, ImageDitherOptions(strength = 0f),
        )
        assertTrue(high.all { it == 1 })
    }

    @Test
    fun linearLuminanceKeepsGreyAndBrightensMixedCells() {
        val grid = CellGrid(cell = 2f, columns = 1, rows = 1)
        val gray = IntArray(4) { 0xFF808080.toInt() }
        val linearGray = sampleCells(gray, 2, 2, grid, ImageDitherOptions(linearLuminance = true)).luminance[0]
        assertEquals(0.5f, linearGray, 0.02f)
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        val mixed = intArrayOf(black, white, white, black)
        val encoded = sampleCells(mixed, 2, 2, grid).luminance[0]
        val linear = sampleCells(mixed, 2, 2, grid, ImageDitherOptions(linearLuminance = true)).luminance[0]
        assertEquals(0.5f, encoded, 1e-3f)
        assertTrue(linear > encoded + 0.1f, "linear $linear")
    }

    @Test
    fun readbackIsBoundedByTheGridTimesSamplingSquared() {
        val grid = cellGrid(1200f, 800f, 3f)
        for (sampling in 1..4) {
            val (w, h) = readbackSize(3600, 2400, grid, sampling)
            assertTrue(w * h <= grid.columns * grid.rows * sampling * sampling, "sampling $sampling gave ${w}x$h")
        }
        assertEquals(10 to 8, readbackSize(10, 8, grid, 2))
    }

    @Test
    fun boxDownscaleAveragesBlocksAndIgnoresSeeThroughPixels() {
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        val clear = 0x00FFFFFF
        val src = intArrayOf(black, white, clear, clear, white, black, clear, clear)
        val out = boxDownscale(src, 4, 2, 2, 1)
        assertEquals(0xFF7F7F7F.toInt(), out[0])
        assertEquals(0, out[1])
        val red = 0xFFFF0000.toInt()
        assertEquals(listOf(red, red), boxDownscale(intArrayOf(red, red), 2, 1, 2, 1).toList())
        assertEquals(0x7FFF0000, boxDownscale(intArrayOf(red, clear), 2, 1, 1, 1)[0])
    }

    @Test
    fun aCancelledCheckStopsTheWork() {
        assertFailsWith<kotlin.coroutines.cancellation.CancellationException> {
            gradientRun0 { throw kotlin.coroutines.cancellation.CancellationException("stop") }
        }
    }

    private fun gradientRun0(check: () -> Unit) =
        ditherCells(
            gradient, FloatArray(gradient.size) { 1f }, 40, 30, DitherAlgorithm.Stucki, DitherPatterns.Bayer8, 2,
            checkCancelled = check,
        )
}
