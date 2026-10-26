package com.jantursky.stipple

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.internal.DitherElement
import com.jantursky.stipple.internal.DitherImageElement
import com.jantursky.stipple.internal.DitherMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ElementsAndShapesTest {
    private val cell4 = StippleTheme.Light.copy(cell = 4.dp)
    private val ink = StippleTheme.Light.ink

    private fun element(pattern: DitherPattern = DitherPatterns.Bayer4) =
        DitherElement(
            DitherMode.Behind,
            DitherFields.solid(1f),
            pattern,
            null,
            RectangleShape,
            animated = false,
            speed = 1f,
            stillTime = 0f,
        )

    @Test
    fun equalElementsAreEqualAndDifferentOnesAreNot() {
        val field = DitherFields.solid(1f)

        fun make(
            mode: DitherMode = DitherMode.Behind,
            renderer: DitherRenderer = DitherRenderer.Cells,
            speed: Float = 1f,
        ) = DitherElement(mode, field, DitherPatterns.Bayer4, cell4, RectangleShape, true, speed, 0f, renderer)
        assertEquals(make(), make())
        assertEquals(make().hashCode(), make().hashCode())
        assertNotEquals(make(), make(mode = DitherMode.Mask))
        assertNotEquals(make(), make(renderer = DitherRenderer.Shader))
        assertNotEquals(make(), make(speed = 2f))
        assertNotEquals(element(), element(DitherPatterns.Bayer8))
        assertNotEquals<Any>(element(), "element")
    }

    @Test
    fun theInspectorShowsTheSettings() {
        val info = InspectorInfo()
        with(element()) { info.inspectableProperties() }
        assertEquals("dither", info.name)
        assertEquals(DitherPatterns.Bayer4, info.properties.first { it.name == "pattern" }.value)
        val image = DitherImageElement(DitherPatterns.Bayer4, null, DitherAlgorithm.Atkinson, false, "k")
        val imageInfo = InspectorInfo()
        with(image) { imageInfo.inspectableProperties() }
        assertEquals("ditherImage", imageInfo.name)
        assertEquals(image, DitherImageElement(DitherPatterns.Bayer4, null, DitherAlgorithm.Atkinson, false, "k"))
        assertEquals(
            image.hashCode(),
            DitherImageElement(DitherPatterns.Bayer4, null, DitherAlgorithm.Atkinson, false, "k").hashCode(),
        )
        assertNotEquals(image, DitherImageElement(DitherPatterns.Bayer4, null, DitherAlgorithm.Atkinson, true, "k"))
        assertNotEquals<Any>(image, "image")
    }

    @Test
    fun aCircleLeavesTheCornersEmpty() =
        runDitherTest {
            setDitherContent { Square(Modifier.dither(DitherFields.solid(1f), theme = cell4, shape = CircleShape)) }
            val map = capture().toPixelMap()
            assertEquals(ink, map[50, 50])
            assertEquals(Color.Transparent, map[1, 1])
        }

    @Test
    fun roundedAndCustomShapesClipToo() {
        val shapes =
            listOf(
                RoundedCornerShape(40.dp),
                GenericShape {
                    size,
                    _,
                    ->
                    addOval(
                        androidx.compose.ui.geometry
                            .Rect(0f, 0f, size.width, size.height),
                    )
                },
            )
        for (shape in shapes) {
            runDitherTest {
                setDitherContent { Square(Modifier.dither(DitherFields.solid(1f), theme = cell4, shape = shape)) }
                val map = capture().toPixelMap()
                assertEquals(ink, map[50, 50])
                assertEquals(Color.Transparent, map[1, 1])
            }
        }
    }

    @Test
    fun aRectangularOutlineThatIsNotTheDefaultStillClips() =
        runDitherTest {
            val inset =
                object : androidx.compose.ui.graphics.Shape {
                    override fun createOutline(
                        size: androidx.compose.ui.geometry.Size,
                        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                        density: androidx.compose.ui.unit.Density,
                    ) = androidx.compose.ui.graphics.Outline.Rectangle(
                        androidx.compose.ui.geometry
                            .Rect(20f, 20f, 80f, 80f),
                    )
                }
            setDitherContent { Square(Modifier.dither(DitherFields.solid(1f), theme = cell4, shape = inset)) }
            val map = capture().toPixelMap()
            assertEquals(ink, map[50, 50])
            assertEquals(Color.Transparent, map[5, 5])
        }

    @Test
    fun dotCellsLeaveGapsAroundTheCircles() =
        runDitherTest {
            val dots = cell4.copy(shape = CellShape.Dot)
            setDitherContent { Square(Modifier.dither(DitherFields.solid(1f), theme = dots)) }
            val share = capture().shareOf(ink)
            // Round cells cover less than square ones, and only their middles are exactly ink.
            assertTrue(share in 0.1f..0.6f, "ink share $share")
        }

    @Test
    fun aPaletteGivesMoreThanTwoColours() =
        runDitherTest {
            val mid = Color(0xFF8080FF)
            val themed = cell4.copy(palette = listOf(Color.White, mid, Color.Black))
            setDitherContent { Square(Modifier.dither(DitherFields.solid(0.5f), theme = themed)) }
            assertEquals(1f, capture().shareOf(mid))
        }

    @Test
    fun changingTheSettingsRedraws() =
        runDitherTest {
            var tone by mutableStateOf(0f)
            var algorithm by mutableStateOf(DitherAlgorithm.Ordered)
            var pattern by mutableStateOf<DitherPattern>(DitherPatterns.Bayer4)
            setDitherContent {
                Box(
                    Modifier
                        .size(
                            100.dp,
                        ).dither(DitherFields.solid(tone), pattern, cell4, animated = false, stillTime = tone),
                )
                Box(Modifier.size(1.dp).ditherImage(pattern, cell4, algorithm, key = algorithm))
            }
            assertTrue(capture().shareOf(StippleTheme.Light.paper) > 0.999f)
            tone = 1f
            algorithm = DitherAlgorithm.Atkinson
            pattern = DitherPatterns.Bayer8
            mainClock.advanceTimeBy(100)
            waitForIdle()
            assertTrue(capture().shareOf(ink) > 0.999f)
        }

    @Test
    fun providedThemesReachTheDithers() =
        runDitherTest {
            setContent {
                ProvideStippleTheme(StippleTheme.Dark) {
                    Square(Modifier.dither(DitherFields.solid(1f)))
                }
            }
            assertEquals(1f, capture().shareOf(StippleTheme.Dark.ink))
        }

    @Test
    fun theSystemThemeIsLightWhenTheSystemIsLight() =
        runDitherTest {
            setContent { ProvideStippleTheme { Square(Modifier.dither(DitherFields.solid(1f))) } }
            assertEquals(1f, capture().shareOf(StippleTheme.Light.ink))
        }

    @Test
    fun systemMotionIsFullOnDesktop() =
        runDitherTest {
            var reduced = true
            setContent {
                androidx.compose.runtime.CompositionLocalProvider(LocalStippleMotion provides StippleMotion.System) {
                    reduced = isStippleMotionReduced()
                }
            }
            assertEquals(false, reduced)
        }
}
