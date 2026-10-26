package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.components.DitherGrid

// Each square is dithered to its own tone, 0 to 1. The grid keeps its squares square, and every square is many
// dither cells, so the grain is finer than the theme's.
@Composable
fun GridExample(
    values: List<Float>,
    columns: Int,
    gap: Float,
    theme: StippleTheme,
    pattern: DitherPattern,
    animated: Boolean,
) {
    DitherGrid(
        values = values,
        columns = columns,
        modifier = Modifier.fillMaxWidth(),
        theme = theme,
        pattern = pattern,
        animated = animated,
        gap = gap,
        contentDescription = "Activity over ${values.size} days",
    )
}
