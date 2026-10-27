package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jantursky.stipple.DitherFields
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.DitherRenderer
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.dither
import com.jantursky.stipple.ditherFill
import com.jantursky.stipple.ditherMask

// The same field drawn twice: one rectangle per cell on the left, one fragment shader on the right. The picture is
// the same; the cost is not. Shrink the cell size and watch the frame meter. The shader renderer needs a Bayer
// pattern, square cells and a field that carries a shader (the built-in ones do); otherwise it draws cells. The
// second row fills text with a moving dither and the third reveals a block through a sweep, both under each renderer.
@Composable
fun RendererExample(
    theme: StippleTheme,
    pattern: DitherPattern,
    animated: Boolean,
) {
    val field = DitherFields.ripples(rings = 7f, speed = 0.3f, radius = 0.9f)
    val renderers = listOf(DitherRenderer.Cells, DitherRenderer.Shader)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            renderers.forEach { renderer ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(240.dp)
                        .dither(field, pattern, theme, animated = animated, stillTime = 1f, renderer = renderer),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            renderers.forEach { renderer ->
                BasicText(
                    "Stipple",
                    Modifier
                        .weight(1f)
                        .ditherFill(
                            DitherFields.waves(count = 3f, speed = 0.35f, low = 0.5f, high = 1f),
                            pattern,
                            theme.copy(cell = 1.dp),
                            animated = animated,
                            stillTime = 1f,
                            renderer = renderer,
                        ),
                    TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Bold),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            renderers.forEach { renderer ->
                val sweep = remember(animated) { DitherFields.noise(scale = 5f, speed = 0.3f, level = 0.55f) }
                Box(
                    Modifier
                        .weight(1f)
                        .height(96.dp)
                        .ditherMask(sweep, pattern, theme, animated = animated, stillTime = 1f, renderer = renderer)
                        .background(theme.ink),
                )
            }
        }
    }
}
