package com.jantursky.stipple

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DitherFillMaskTest {
    private val theme = StippleTheme.Light.copy(cell = 1.dp)
    private val red = Color(0xFFFF0000)

    @Test
    fun fillRecolorsTheContentWithInk() =
        runDitherTest {
            setDitherContent {
                Square(Modifier.ditherFill(DitherFields.solid(1f), theme = theme).background(red))
            }
            assertEquals(theme.ink, capture().toPixelMap()[50, 50])
        }

    @Test
    fun fillWithLowToneTurnsTheContentToPaper() =
        runDitherTest {
            setDitherContent {
                Square(Modifier.ditherFill(DitherFields.solid(0f), theme = theme).background(red))
            }
            assertEquals(theme.paper, capture().toPixelMap()[50, 50])
        }

    @Test
    fun fillLeavesTransparentContentAlone() =
        runDitherTest {
            setDitherContent {
                Square(Modifier.ditherFill(DitherFields.solid(1f), theme = theme))
            }
            // No content drawn: nothing is painted, not even ink.
            assertEquals(Color.Transparent, capture().toPixelMap()[50, 50])
        }

    @Test
    fun maskKeepsTheContentWhereTheToneIsHigh() =
        runDitherTest {
            setDitherContent {
                Square(Modifier.ditherMask(DitherFields.solid(1f), theme = theme).background(red))
            }
            assertEquals(red, capture().toPixelMap()[50, 50])
        }

    @Test
    fun maskHidesTheContentWhereTheToneIsLow() =
        runDitherTest {
            setDitherContent {
                Square(Modifier.ditherMask(DitherFields.solid(0f), theme = theme).background(red))
            }
            assertEquals(Color.Transparent, capture().toPixelMap()[50, 50])
        }

    @Test
    fun aSweepRevealsTheContentFromTheLeft() =
        runDitherTest {
            setDitherContent {
                Square(Modifier.ditherMask(DitherFields.sweep(0.5f, softness = 0.1f), theme = theme).background(red))
            }
            val map = capture().toPixelMap()
            assertEquals(red, map[10, 50])
            assertEquals(Color.Transparent, map[90, 50])
        }

    @Test
    fun maskShowsAShareOfContentThatFollowsTheTone() =
        runDitherTest {
            setDitherContent {
                Square(Modifier.ditherMask(DitherFields.solid(0.5f), DitherPatterns.Bayer8, theme).background(red))
            }
            val share = capture().shareOf(red)
            assertEquals(0.5f, share, 0.01f)
        }

    @Test
    fun contrastRatioMatchesWcagReferenceValues() {
        assertEquals(21f, contrastRatio(Color.Black, Color.White), 0.01f)
        assertEquals(1f, contrastRatio(Color.White, Color.White), 0.001f)
        assertEquals(4.48f, contrastRatio(Color(0xFF777777), Color.White), 0.05f)
        assertEquals(StippleTheme.Light.contrastRatio > 15f, true)
    }
}
