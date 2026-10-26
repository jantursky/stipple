package com.jantursky.stipple

import com.jantursky.stipple.internal.cellGrid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FieldsTest {
    private fun DitherField.at(
        x: Float,
        y: Float,
        t: Float = 0f,
        aspect: Float = 1f,
    ) = tone(x, y, t, aspect)

    @Test
    fun solidReturnsItsToneEverywhere() {
        val f = DitherFields.solid(0.3f)
        assertEquals(0.3f, f.at(0f, 0f))
        assertEquals(0.3f, f.at(1f, 1f, t = 9f))
    }

    @Test
    fun linearRunsTopToBottomByDefault() {
        val f = DitherFields.linear()
        assertEquals(0f, f.at(0.5f, 0f), 1e-4f)
        assertEquals(0.5f, f.at(0.5f, 0.5f), 1e-4f)
        assertEquals(1f, f.at(0.5f, 1f), 1e-4f)
        assertEquals(f.at(0f, 0.25f), f.at(1f, 0.25f), 1e-3f)
    }

    @Test
    fun linearRunsLeftToRightAtZeroDegrees() {
        val f = DitherFields.linear(angleDegrees = 0f)
        assertEquals(0f, f.at(0f, 0.5f), 1e-4f)
        assertEquals(1f, f.at(1f, 0.5f), 1e-4f)
        assertEquals(0.5f, f.at(0.5f, 0.5f, aspect = 3f), 1e-4f)
    }

    @Test
    fun radialIsRoundOnAWideElement() {
        val f = DitherFields.radial(radius = 0.4f)
        // With aspect 2, moving 0.1 across is as far as moving 0.2 down.
        assertEquals(f.at(0.6f, 0.5f, aspect = 2f), f.at(0.5f, 0.7f, aspect = 2f), 1e-4f)
        assertEquals(1f, f.at(0.5f, 0.5f), 1e-4f)
        assertEquals(0f, f.at(0f, 0f), 1e-4f)
    }

    @Test
    fun noiseStaysNearItsLevelAndMovesWithTime() {
        val f = DitherFields.noise(level = 0.5f, contrast = 1f)
        for (i in 0 until 50) {
            val v = f.at(i / 50f, (i * 7 % 50) / 50f, t = i * 0.3f)
            assertTrue(v in 0f..1f, "tone $v")
        }
        assertNotEquals(f.at(0.3f, 0.3f, t = 0f), f.at(0.3f, 0.3f, t = 20f))
    }

    @Test
    fun noiseIsDeterministic() {
        assertEquals(DitherFields.noise().at(0.2f, 0.9f, 3f), DitherFields.noise().at(0.2f, 0.9f, 3f))
    }

    @Test
    fun wavesStayBetweenTheirTwoTonesAndTravel() {
        val f = DitherFields.waves(low = 0.2f, high = 0.8f)
        var min = 1f
        var max = 0f
        for (i in 0 until 200) {
            val v = f.at(i / 200f, 0.4f)
            min = minOf(min, v)
            max = maxOf(max, v)
        }
        assertTrue(min >= 0.2f - 1e-4f && max <= 0.8f + 1e-4f)
        assertNotEquals(f.at(0.3f, 0.3f, t = 0f), f.at(0.3f, 0.3f, t = 0.7f))
    }

    @Test
    fun ripplesFadeToNothingAtTheirRadius() {
        val f = DitherFields.ripples(radius = 0.4f)
        assertEquals(0f, f.at(0.5f, 0.95f), 1e-4f)
    }

    @Test
    fun sweepCoversTheElementFromTheLeft() {
        assertEquals(0f, DitherFields.sweep(0f).at(0.5f, 0.5f), 1e-4f)
        assertEquals(1f, DitherFields.sweep(1f).at(0.5f, 0.5f), 1e-4f)
        val half = DitherFields.sweep(0.5f, softness = 0.1f)
        assertEquals(1f, half.at(0.1f, 0.5f))
        assertEquals(0f, half.at(0.9f, 0.5f))
    }

    @Test
    fun theGridUsesWholePixelCells() {
        val g = cellGrid(100f, 50f, 3.4f)
        assertEquals(3f, g.cell)
        assertEquals(34, g.columns)
        assertEquals(17, g.rows)
    }

    @Test
    fun aHugeAreaCoarsensTheGrid() {
        val g = cellGrid(4000f, 3000f, 2f)
        assertTrue(g.columns.toLong() * g.rows <= 90_000, "${g.columns} x ${g.rows}")
        assertTrue(g.cell > 2f)
    }

    @Test
    fun cellsAreNeverSmallerThanOnePixel() {
        assertEquals(1f, cellGrid(10f, 10f, 0.2f).cell)
    }
}
