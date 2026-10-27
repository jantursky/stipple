package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.CellShape
import com.jantursky.stipple.DitherFields
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.DitherRenderer
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.dither

// The load scripts/frame-bench.sh measures: three large animated surfaces at a one-dp cell, all drawn by one
// renderer. Every field here carries a shader, so the shader renderer draws all of it. The page exists for
// measuring; the other pages show what the renderers look like.
@Composable
fun BenchExample(
    theme: StippleTheme,
    pattern: DitherPattern,
    renderer: DitherRenderer,
    dots: Boolean = false,
) {
    val fine = theme.copy(cell = 1.dp, shape = if (dots) CellShape.Dot else theme.shape)
    val fields =
        listOf(
            DitherFields.ripples(rings = 9f, speed = 0.5f, radius = 0.9f),
            DitherFields.waves(count = 6f, speed = 0.6f),
            DitherFields.radial(),
        )
    Column {
        fields.forEach { field ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .dither(field, pattern, fine, animated = true, renderer = renderer),
            )
        }
    }
}
