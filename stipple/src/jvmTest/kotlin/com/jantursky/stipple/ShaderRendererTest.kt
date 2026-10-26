package com.jantursky.stipple

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun blueNoiseFallsBackToCells() =
        assertEquals("cells", drawnBy(DitherRenderer.Shader, pattern = DitherPatterns.BlueNoise))

    @Test
    fun dotsFallBackToCells() =
        assertEquals("cells", drawnBy(DitherRenderer.Shader, theme = cell2.copy(shape = CellShape.Dot)))

    @Test
    fun aPaletteFallsBackToCells() =
        assertEquals(
            "cells",
            drawnBy(DitherRenderer.Shader, theme = cell2.copy(palette = listOf(paper, ink, ink))),
        )

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
