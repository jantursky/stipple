package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.DitherFields
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.DitherRenderer
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.dither

// The same field drawn twice: one rectangle per cell on the left, one fragment shader on the right. The picture is
// the same; the cost is not. Shrink the cell size and watch the frame meter. The shader renderer needs a Bayer
// pattern, square cells and a field that carries a shader (the built-in ones do); otherwise it draws cells.
@Composable
fun RendererExample(
    theme: StippleTheme,
    pattern: DitherPattern,
    animated: Boolean,
) {
    val field = DitherFields.ripples(rings = 7f, speed = 0.3f, radius = 0.9f)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(DitherRenderer.Cells, DitherRenderer.Shader).forEach { renderer ->
            Box(
                Modifier
                    .weight(1f)
                    .height(320.dp)
                    .dither(field, pattern, theme, animated = animated, stillTime = 1f, renderer = renderer),
            )
        }
    }
}
