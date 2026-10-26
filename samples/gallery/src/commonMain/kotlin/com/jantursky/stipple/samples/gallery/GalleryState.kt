package com.jantursky.stipple.samples.gallery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.CellShape
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.DitherPatterns
import com.jantursky.stipple.StippleMotion
import com.jantursky.stipple.StippleTheme

/** The settings every page shares: what the bar at the top of the gallery controls. */
class GalleryState(
    dark: Boolean,
) {
    var dark by mutableStateOf(dark)
    var ink by mutableStateOf<Color?>(null)
    var paper by mutableStateOf<Color?>(null)
    var cell by mutableFloatStateOf(3f)
    var shape by mutableStateOf(CellShape.Square)
    var pattern by mutableStateOf<DitherPattern>(DitherPatterns.Bayer4)
    var animated by mutableStateOf(false)
    var motion by mutableStateOf(StippleMotion.System)

    /** The theme on the stage: the chosen colors over the light or dark base. */
    val theme: StippleTheme
        get() {
            val base = if (dark) StippleTheme.Dark else StippleTheme.Light
            return base.copy(ink = ink ?: base.ink, paper = paper ?: base.paper, cell = cell.dp, shape = shape)
        }

    companion object {
        /** Ink colors on offer; null follows the theme. */
        val Inks = listOf<Color?>(null, Color(0xFF615FFF), Color(0xFF0D542B), Color(0xFFE17100), Color(0xFFEC003F))

        /** Paper colors on offer; null follows the theme. */
        val Papers = listOf<Color?>(null, Color(0xFFFEF3C6), Color(0xFFDFDFFF), Color(0xFF111827), Color(0xFF000000))
    }
}
