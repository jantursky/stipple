package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.CellShape
import com.jantursky.stipple.DitherFields
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.ProvideStippleTheme
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.dither

// A StippleTheme is ink, paper and a cell size. Provide one and every dither below it follows. A palette adds
// colors between paper and ink.
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(theme, theme.copy(shape = CellShape.Dot), sunset).forEach { themed ->
            ProvideStippleTheme(themed) {
                // No theme argument: the dither reads it from the composition.
                Box(Modifier.weight(1f).aspectRatio(1f).dither(DitherFields.radial(radius = 0.7f), pattern))
            }
        }
    }
}
