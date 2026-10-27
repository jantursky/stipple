package com.jantursky.stipple

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.internal.ShaderParams
import com.jantursky.stipple.internal.shaderProgram
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ShaderFillMaskTest {
    private val theme = StippleTheme.Light.copy(cell = 2.dp)
    private val red = Color(0xFFFF0000)

    /** Looks like paper to the cell renderer and like ink to the shader, so the picture says who drew it. */
    private class Probe : ShaderField {
        override val shaderSource = "float tone(float2 uv, float t, float aspect) { return 1.0; }"

        override fun tone(
            x: Float,
            y: Float,
            t: Float,
            aspect: Float,
        ) = 0f
    }

    private fun pixel(
        renderer: DitherRenderer,
        modifier: (DitherRenderer) -> Modifier,
    ): Color {
        var color = Color.Unspecified
        runDitherTest {
            setDitherContent { Square(modifier(renderer).background(red)) }
            color = capture().toPixelMap()[50, 50]
        }
        return color
    }

    private fun render(block: (DitherRenderer) -> Modifier): Pair<ImageBitmap, ImageBitmap> {
        var cells: ImageBitmap? = null
        var shader: ImageBitmap? = null
        runDitherTest {
            setDitherContent { Square(block(DitherRenderer.Cells).background(red)) }
            cells = capture()
        }
        runDitherTest {
            setDitherContent { Square(block(DitherRenderer.Shader).background(red)) }
            shader = capture()
        }
        return cells!! to shader!!
    }

    @Test
    fun fillIsDrawnByTheShader() {
        val fill = { r: DitherRenderer -> Modifier.ditherFill(Probe(), theme = theme, renderer = r) }
        assertEquals(theme.ink, pixel(DitherRenderer.Shader, fill))
        assertEquals(theme.paper, pixel(DitherRenderer.Cells, fill))
    }

    @Test
    fun maskIsDrawnByTheShader() {
        val mask = { r: DitherRenderer -> Modifier.ditherMask(Probe(), theme = theme, renderer = r) }
        assertEquals(red, pixel(DitherRenderer.Shader, mask))
        assertEquals(Color.Transparent, pixel(DitherRenderer.Cells, mask))
    }

    @Test
    fun theDefaultRendererIsStillCells() {
        assertEquals(theme.paper, pixel(DitherRenderer.Cells) { _ -> Modifier.ditherFill(Probe(), theme = theme) })
        assertEquals(
            Color.Transparent,
            pixel(DitherRenderer.Cells) { _ -> Modifier.ditherMask(Probe(), theme = theme) },
        )
    }

    @Test
    fun anIneligiblePatternFallsBackToCellsWithoutThrowing() {
        // Blue noise has no shader yet, so the shader renderer hands the fill to the cell renderer.
        val fill = { r: DitherRenderer ->
            Modifier.ditherFill(Probe(), DitherPatterns.BlueNoise, theme, renderer = r)
        }
        assertEquals(theme.paper, pixel(DitherRenderer.Shader, fill))
    }

    @Test
    fun fillAgreesWithCells() {
        val (cells, shader) =
            render { r ->
                Modifier.ditherFill(DitherFields.linear(0.2f, 0.9f, 30f), theme = theme, stillTime = 0.3f, renderer = r)
            }
        val different = cells.pixelsDifferentFrom(shader)
        assertTrue(different < 100, "$different of 40000 pixels differ")
    }

    @Test
    fun maskAgreesWithCells() {
        val (cells, shader) =
            render { r ->
                Modifier.ditherMask(DitherFields.radial(), DitherPatterns.Bayer8, theme, renderer = r)
            }
        val different = cells.pixelsDifferentFrom(shader)
        assertTrue(different < 100, "$different of 40000 pixels differ")
    }

    @Test
    fun noiseAgreesWithCells() {
        for (time in listOf(0f, 1.7f, 12f)) {
            val (cells, shader) =
                render { r ->
                    Modifier.dither(DitherFields.noise(), theme = theme, stillTime = time, renderer = r)
                }
            val different = cells.pixelsDifferentFrom(shader)
            // The hash is float arithmetic on both sides; rounding may flip a cell that sits on a threshold.
            assertTrue(different < 100, "noise at $time s: $different of 40000 pixels differ")
        }
    }

    @Test
    fun aSweepAgreesWithCells() {
        for (progress in listOf(0f, 0.3f, 0.5f, 1f)) {
            val (cells, shader) =
                render { r ->
                    Modifier.dither(DitherFields.sweep(progress, 0.2f), theme = theme, renderer = r)
                }
            val different = cells.pixelsDifferentFrom(shader)
            assertTrue(different < 100, "sweep at $progress: $different of 40000 pixels differ")
        }
    }

    @Test
    fun aLiveSweepAgreesWithAFixedOne() {
        val (fixed, live) =
            render { r ->
                Modifier.dither(DitherFields.sweep({ 0.4f }, 0.2f), theme = theme, renderer = r)
            }
        assertTrue(fixed.pixelsDifferentFrom(live) < 100)
    }

    @Test
    fun sweepProgressDoesNotChangeTheProgram() {
        val a = DitherFields.sweep(0.1f) as ShaderField
        val b = DitherFields.sweep(0.9f) as ShaderField
        val paramsA = (a as ShaderParams).shaderParams().copyOf()
        val paramsB = (b as ShaderParams).shaderParams().copyOf()
        // One program text means one compiled shader, whatever the progress.
        assertEquals(shaderProgram(a.shaderSource, paramsA.size), shaderProgram(b.shaderSource, paramsB.size))
        assertNotEquals(paramsA.toList(), paramsB.toList())
        assertContentEquals(floatArrayOf(0.1f, 0.15f), paramsA)
    }
}
