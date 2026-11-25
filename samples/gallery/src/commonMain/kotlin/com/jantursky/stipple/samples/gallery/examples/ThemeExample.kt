package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.CellShape
import com.jantursky.stipple.DitherFields
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.ProvideStippleTheme
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.dither
import com.jantursky.stipple.samples.gallery.Segmented

// A StippleTheme is ink, paper and a cell size. Provide one and every dither below it follows. A palette adds
// colors between paper and ink, StippleTheme.presets lists ready-made looks, and gradient builds a ramp.
@Composable
fun ThemeExample(
    theme: StippleTheme,
    pattern: DitherPattern,
) {
    val sunset =
        theme.copy(
            palette =
                listOf(
                    Color(0xFF14122B),
                    Color(0xFF6B5B95),
                    Color(0xFFEC003F),
                    Color(0xFFFFB900),
                    Color(0xFFFFF8E5),
                ),
        )
    val names = listOf("Gallery") + StippleTheme.presets.keys
    var chosen by remember { mutableStateOf(names.first()) }
    val picked = StippleTheme.presets[chosen] ?: theme
    val ramp = StippleTheme.gradient(theme.paper, theme.ink, steps = 5, cell = theme.cell)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Segmented(names, chosen, { chosen = it }, { it }, wrap = true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(picked, theme.copy(shape = CellShape.Dot), sunset).forEach { themed ->
                ProvideStippleTheme(themed) {
                    // No theme argument: the dither reads it from the composition.
                    Box(Modifier.weight(1f).aspectRatio(1f).dither(DitherFields.radial(radius = 0.7f), pattern))
                }
            }
        }
        // StippleTheme.gradient: five evenly spaced colors from paper to ink.
        ProvideStippleTheme(ramp) {
            Box(Modifier.fillMaxWidth().height(48.dp).dither(DitherFields.linear(0f, 1f, 0f), pattern))
        }
    }
}
