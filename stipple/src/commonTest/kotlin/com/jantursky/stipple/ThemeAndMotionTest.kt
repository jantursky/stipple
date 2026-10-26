package com.jantursky.stipple

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.internal.ProgressField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ThemeAndMotionTest {
    @Test
    fun copyChangesOnlyWhatItIsTold() {
        val themed = StippleTheme.Light.copy(cell = 5.dp, shape = CellShape.Dot)
        assertEquals(StippleTheme.Light.ink, themed.ink)
        assertEquals(StippleTheme.Light.paper, themed.paper)
        assertEquals(5.dp, themed.cell)
        assertEquals(CellShape.Dot, themed.shape)
    }

    @Test
    fun equalThemesAreEqualAndHashTheSame() {
        val a = StippleTheme.Light.copy(palette = listOf(Color.White, Color.Gray, Color.Black))
        val b = StippleTheme.Light.copy(palette = listOf(Color.White, Color.Gray, Color.Black))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, StippleTheme.Light)
        assertNotEquals(a.hashCode(), StippleTheme.Light.hashCode())
        assertFalse(a.equals("theme"))
    }

    @Test
    fun theRampIsThePaletteWhenThereAreTwoOrMoreColours() {
        assertEquals(listOf(Color.White, Color.Black), StippleTheme(Color.Black, Color.White).ramp)
        val palette = listOf(Color.White, Color.Red, Color.Black)
        assertEquals(palette, StippleTheme(Color.Black, Color.White, palette).ramp)
        // One colour is no ramp: fall back to paper and ink.
        assertEquals(2, StippleTheme(Color.Black, Color.White, listOf(Color.Red)).ramp.size)
    }

    @Test
    fun theDefaultThemesHaveStrongContrast() {
        assertTrue(StippleTheme.Light.contrastRatio > 15f)
        assertTrue(StippleTheme.Dark.contrastRatio > 15f)
        assertEquals(21f, contrastRatio(Color.Black, Color.White), 0.01f)
        assertEquals(1f, contrastRatio(Color.Gray, Color.Gray), 0.001f)
    }

    @Test
    fun fullMotionIsNeverReducedAndReducedAlwaysIs() {
        assertFalse(resolveReducedMotion(StippleMotion.Full, 0f))
        assertTrue(resolveReducedMotion(StippleMotion.Reduced, 1f))
    }

    @Test
    fun systemMotionIsReducedWhenAnimationsAreScaledToZero() {
        assertTrue(resolveReducedMotion(StippleMotion.System, 0f))
    }

    @Test
    fun progressFillsFromTheDirectionItIsGiven() {
        fun tone(
            direction: ProgressDirection,
            x: Float,
            y: Float,
        ): Float {
            val field = ProgressField({ 0.5f }, direction, 0.001f, 0f, DitherFields.solid(1f))
            field.beginFrame()
            return field.tone(x, y, 0f, 1f)
        }
        assertEquals(1f, tone(ProgressDirection.Right, 0.1f, 0.5f))
        assertEquals(0f, tone(ProgressDirection.Right, 0.9f, 0.5f))
        assertEquals(1f, tone(ProgressDirection.Left, 0.9f, 0.5f))
        assertEquals(0f, tone(ProgressDirection.Left, 0.1f, 0.5f))
        assertEquals(1f, tone(ProgressDirection.Down, 0.5f, 0.1f))
        assertEquals(0f, tone(ProgressDirection.Down, 0.5f, 0.9f))
        assertEquals(1f, tone(ProgressDirection.Up, 0.5f, 0.9f))
        assertEquals(0f, tone(ProgressDirection.Up, 0.5f, 0.1f))
    }
}
