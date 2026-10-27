package com.jantursky.stipple

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.internal.AUTO_SHADER_CELLS
import com.jantursky.stipple.internal.patternTexture
import com.jantursky.stipple.internal.shaderEligibility
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ShaderRendererTest {
    private val cell2 = StippleTheme.Light.copy(cell = 2.dp)
    private val ink = StippleTheme.Light.ink
    private val paper = StippleTheme.Light.paper

    /** Looks like paper to the cell renderer and like ink to the shader, so the picture says who drew it. */
    private class Probe(
        source: String = "float tone(float2 uv, float t, float aspect) { return 1.0; }",
    ) : ShaderField {
        override val shaderSource = source

        override fun tone(
            x: Float,
            y: Float,
            t: Float,
            aspect: Float,
        ) = 0f
    }

    private fun drawnBy(
        renderer: DitherRenderer,
        field: DitherField = Probe(),
        pattern: DitherPattern = DitherPatterns.Bayer4,
        theme: StippleTheme = cell2,
    ): String {
        var share = 0f
        runDitherTest {
            setDitherContent { Square(Modifier.dither(field, pattern, theme, renderer = renderer)) }
            share = capture().shareOf(ink)
        }
        return if (share > 0.99f) {
            "shader"
        } else if (share < 0.01f) {
            "cells"
        } else {
            "mixed $share"
        }
    }

    @Test
    fun theShaderRendererMatchesTheCellRenderer() {
        val fields =
            mapOf(
                "solid" to DitherFields.solid(0.4f),
                "linear" to DitherFields.linear(),
                "angled" to DitherFields.linear(0.1f, 0.9f, 30f),
                "radial" to DitherFields.radial(),
                "waves" to DitherFields.waves(),
                "ripples" to DitherFields.ripples(),
            )
        for ((name, field) in fields) {
            for (pattern in listOf(DitherPatterns.Bayer2, DitherPatterns.Bayer4, DitherPatterns.Bayer8)) {
                var cells: androidx.compose.ui.graphics.ImageBitmap? = null
                var shader: androidx.compose.ui.graphics.ImageBitmap? = null
                runDitherTest {
                    setDitherContent { Square(Modifier.dither(field, pattern, cell2, stillTime = 0.3f)) }
                    cells = capture()
                }
                runDitherTest {
                    setDitherContent {
                        Square(
                            Modifier.dither(field, pattern, cell2, stillTime = 0.3f, renderer = DitherRenderer.Shader),
                        )
                    }
                    shader = capture()
                }
                val different = cells!!.pixelsDifferentFrom(shader!!)
                // Float rounding may flip a cell that sits exactly on a threshold, nothing more.
                assertTrue(different < 100, "$name on $pattern: $different of 40000 pixels differ")
            }
        }
    }

    @Test
    fun cellsNeverUseTheShader() = assertEquals("cells", drawnBy(DitherRenderer.Cells))

    @Test
    fun shaderUsesTheShaderForAShaderField() = assertEquals("shader", drawnBy(DitherRenderer.Shader))

    @Test
    fun aPlainFieldFallsBackToCells() =
        assertEquals("cells", drawnBy(DitherRenderer.Shader, DitherField { _, _, _, _ -> 0f }))

    private class FlatPattern(
        override val size: Int,
    ) : DitherPattern {
        override fun threshold(
            x: Int,
            y: Int,
        ) = ((x.mod(size) + y.mod(size) * size) % 7 + 0.5f) / 7f
    }

    /** How light the picture is on average, so antialiased edges count for their coverage. */
    private fun androidx.compose.ui.graphics.ImageBitmap.meanRed(): Float {
        val pixels = toPixelMap()
        var sum = 0f
        for (y in 0 until height) for (x in 0 until width) sum += pixels[x, y].red
        return sum / (width * height)
    }

    private fun picture(
        renderer: DitherRenderer,
        field: DitherField,
        pattern: DitherPattern,
        theme: StippleTheme,
    ): androidx.compose.ui.graphics.ImageBitmap {
        var bitmap: androidx.compose.ui.graphics.ImageBitmap? = null
        runDitherTest {
            setDitherContent {
                Square(Modifier.dither(field, pattern, theme, stillTime = 0.3f, renderer = renderer))
            }
            bitmap = capture()
        }
        return bitmap!!
    }

    @Test
    fun blueNoiseIsDrawnByTheShader() =
        assertEquals("shader", drawnBy(DitherRenderer.Shader, pattern = DitherPatterns.BlueNoise))

    @Test
    fun blueNoiseMatchesTheCellRenderer() {
        for (field in listOf(DitherFields.linear(0.1f, 0.9f, 30f), DitherFields.radial(), DitherFields.waves())) {
            val cells = picture(DitherRenderer.Cells, field, DitherPatterns.BlueNoise, cell2)
            val shader = picture(DitherRenderer.Shader, field, DitherPatterns.BlueNoise, cell2)
            val different = cells.pixelsDifferentFrom(shader)
            assertTrue(different < 100, "$different of 40000 pixels differ")
        }
    }

    @Test
    fun aCustomPatternUpTo256IsDrawnByTheShader() {
        assertEquals("shader", drawnBy(DitherRenderer.Shader, pattern = FlatPattern(16)))
        assertEquals("shader", drawnBy(DitherRenderer.Shader, pattern = FlatPattern(256)))
    }

    @Test
    fun aCustomPatternMatchesTheCellRenderer() {
        val field = DitherFields.linear(0.1f, 0.9f, 30f)
        val cells = picture(DitherRenderer.Cells, field, FlatPattern(16), cell2)
        val shader = picture(DitherRenderer.Shader, field, FlatPattern(16), cell2)
        val different = cells.pixelsDifferentFrom(shader)
        assertTrue(different < 100, "$different of 40000 pixels differ")
    }

    @Test
    fun aPatternWiderThan256FallsBackToCells() =
        assertEquals("cells", drawnBy(DitherRenderer.Shader, pattern = FlatPattern(512)))

    @Test
    fun dotsAreDrawnByTheShader() {
        // The probe is paper to the cell renderer, so any ink at all comes from the shader. Dots are antialiased.
        val theme = StippleTheme.Light.copy(cell = 8.dp, shape = CellShape.Dot)
        assertTrue(picture(DitherRenderer.Shader, Probe(), DitherPatterns.Bayer4, theme).shareOf(ink) > 0.3f)
        assertEquals(0f, picture(DitherRenderer.Cells, Probe(), DitherPatterns.Bayer4, theme).shareOf(ink))
    }

    @Test
    fun dotsLightTheSameAreaAsTheCellRenderer() {
        // 7 dp cells leave clipped cells along the right and bottom edge of the 100 dp square.
        val theme = StippleTheme.Light.copy(cell = 7.dp, shape = CellShape.Dot)
        for (tone in listOf(0.2f, 0.5f, 0.9f)) {
            val field = DitherFields.solid(tone)
            val cells = picture(DitherRenderer.Cells, field, DitherPatterns.Bayer4, theme).meanRed()
            val shader = picture(DitherRenderer.Shader, field, DitherPatterns.Bayer4, theme).meanRed()
            assertTrue(kotlin.math.abs(cells - shader) < 0.02f, "tone $tone: cells $cells, shader $shader")
        }
    }

    @Test
    fun aPaletteOfUpToEightColoursIsDrawnByTheShader() {
        assertEquals("shader", drawnBy(DitherRenderer.Shader, theme = cell2.copy(palette = listOf(paper, ink, ink))))
        assertEquals("shader", drawnBy(DitherRenderer.Shader, theme = cell2.copy(palette = List(8) { ink })))
    }

    @Test
    fun aPaletteMatchesTheCellRenderer() {
        val palette = listOf(paper, Color(0xFFE53935), Color(0xFFFFB300), ink)
        val theme = cell2.copy(palette = palette)
        for (pattern in listOf(DitherPatterns.Bayer4, DitherPatterns.BlueNoise)) {
            val field = DitherFields.linear(0f, 1f, 20f)
            val cells = picture(DitherRenderer.Cells, field, pattern, theme)
            val shader = picture(DitherRenderer.Shader, field, pattern, theme)
            val different = cells.pixelsDifferentFrom(shader)
            assertTrue(different < 100, "$different of 40000 pixels differ")
        }
    }

    @Test
    fun aPaletteOfNineColoursFallsBackToCells() =
        assertEquals(
            "cells",
            drawnBy(
                DitherRenderer.Shader,
                theme =
                    cell2.copy(
                        palette =
                            listOf(paper) + List(8) { ink },
                    ),
            ),
        )

    @Test
    fun eligibilityFollowsEachRule() {
        fun eligible(
            renderer: DitherRenderer = DitherRenderer.Shader,
            hasProgram: Boolean = true,
            broken: Boolean = false,
            pattern: DitherPattern = DitherPatterns.Bayer4,
            colors: Int = 2,
            cells: Int = 0,
        ) = shaderEligibility(renderer, hasProgram, broken, pattern, colors) { cells }

        assertTrue(eligible())
        assertTrue(eligible(pattern = DitherPatterns.BlueNoise, colors = 8))
        assertFalse(eligible(renderer = DitherRenderer.Cells))
        assertFalse(eligible(hasProgram = false))
        assertFalse(eligible(broken = true))
        assertFalse(eligible(pattern = FlatPattern(257)))
        assertFalse(eligible(colors = 9))
        assertFalse(eligible(renderer = DitherRenderer.Auto, cells = AUTO_SHADER_CELLS))
        assertTrue(eligible(renderer = DitherRenderer.Auto, cells = AUTO_SHADER_CELLS + 1))
    }

    @Test
    fun theTextureHoldsEveryThreshold() {
        for (pattern in DitherPatterns.All + FlatPattern(16)) {
            val bytes = patternTexture(pattern)
            val side = pattern.size
            for (y in 0 until side) {
                for (x in 0 until side) {
                    val i = (y * side + x) * 4
                    val decoded = ((bytes[i].toInt() and 255) * 256 + (bytes[i + 1].toInt() and 255)) / 65535f
                    val error = kotlin.math.abs(decoded - pattern.threshold(x, y))
                    assertTrue(error <= 1f / 65535f, "$pattern ($x, $y): off by $error")
                }
            }
        }
    }

    @Test
    fun aProgramThePlatformRejectsFallsBackToCells() =
        assertEquals("cells", drawnBy(DitherRenderer.Shader, Probe("this is not a shader")))

    @Test
    fun autoStaysOnCellsForASmallElement() = assertEquals("cells", drawnBy(DitherRenderer.Auto))

    @Test
    fun autoSwitchesToTheShaderWhenCellsGetMany() =
        runDitherTest {
            // The 200 x 200 window at 1 px cells is 40,000 cells, past the 20,000 where Auto changes over.
            setDitherContent {
                Box(
                    Modifier.fillMaxSize().dither(
                        Probe(),
                        theme = StippleTheme.Light.copy(cell = 1.dp),
                        renderer = DitherRenderer.Auto,
                    ),
                )
            }
            assertTrue(capture().toPixelMap()[10, 10] == ink)
        }
}
