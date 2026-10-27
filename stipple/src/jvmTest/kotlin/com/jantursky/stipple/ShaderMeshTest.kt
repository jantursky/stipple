package com.jantursky.stipple

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.internal.ShaderParams
import com.jantursky.stipple.internal.shaderProgram
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ShaderMeshTest {
    private val theme = StippleTheme.Light.copy(cell = 2.dp)
    private val tones =
        listOf(
            listOf(0f, 0.5f, 1f, 0.3f),
            listOf(0.25f, 0.9f, 0.1f, 0.7f),
            listOf(1f, 0f, 0.6f, 0.2f),
        )

    private fun both(
        field: DitherField,
        time: Float,
    ): Pair<ImageBitmap, ImageBitmap> {
        var cells: ImageBitmap? = null
        var shader: ImageBitmap? = null
        runDitherTest {
            setDitherContent { Square(Modifier.dither(field, theme = theme, stillTime = time)) }
            cells = capture()
        }
        runDitherTest {
            setDitherContent {
                Square(Modifier.dither(field, theme = theme, stillTime = time, renderer = DitherRenderer.Shader))
            }
            shader = capture()
        }
        return cells!! to shader!!
    }

    @Test
    fun aStaticMeshAgreesWithCells() {
        val (cells, shader) = both(DitherFields.mesh(tones), 0f)
        val different = cells.pixelsDifferentFrom(shader)
        assertTrue(different < 100, "$different of 40000 pixels differ")
    }

    @Test
    fun aDriftingMeshAgreesWithCellsAtSeveralTimes() {
        for (time in listOf(0f, 0.9f, 4.2f)) {
            val (cells, shader) = both(DitherFields.mesh(tones, drift = 0.12f, speed = 0.3f), time)
            val different = cells.pixelsDifferentFrom(shader)
            assertTrue(different < 100, "at $time s: $different of 40000 pixels differ")
        }
    }

    @Test
    fun changingTheTonesDoesNotChangeTheProgram() {
        val a = DitherFields.mesh(tones, drift = 0.1f) as ShaderField
        val other = tones.map { row -> row.map { 1f - it } }
        val b = DitherFields.mesh(other, drift = 0.2f) as ShaderField
        val pa = (a as ShaderParams).shaderParams().copyOf()
        val pb = (b as ShaderParams).shaderParams().copyOf()
        assertEquals(shaderProgram(a.shaderSource, pa.size), shaderProgram(b.shaderSource, pb.size))
        assertNotEquals(pa.toList(), pb.toList())
    }

    @Test
    fun aDifferentGridSizeIsADifferentProgram() {
        val a = DitherFields.mesh(tones) as ShaderField
        val b = DitherFields.mesh(List(2) { List(2) { 0.5f } }) as ShaderField
        assertNotEquals(a.shaderSource, b.shaderSource)
    }

    @Test
    fun aDissolveAgreesWithCells() {
        for (progress in listOf(0.2f, 0.5f, 0.8f)) {
            val (cells, shader) = both(DitherFields.dissolve({ progress }), 0f)
            val different = cells.pixelsDifferentFrom(shader)
            assertTrue(different < 100, "dissolve at $progress: $different of 40000 pixels differ")
        }
    }

    @Test
    fun aMovingDissolveKeepsOneProgram() {
        var progress = 0.1f
        val field = DitherFields.dissolve({ progress }) as ShaderField
        val before = shaderProgram(field.shaderSource, (field as ShaderParams).shaderParams().size)
        val first = field.shaderParams().copyOf()
        progress = 0.9f
        assertEquals(before, shaderProgram(field.shaderSource, field.shaderParams().size))
        assertNotEquals(first.toList(), field.shaderParams().toList())
    }
}
