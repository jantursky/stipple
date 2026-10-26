package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.DitherAlgorithm
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.ditherImage
import com.jantursky.stipple.samples.gallery.Landscape

// A before and after slider: the picture underneath is untouched, the same picture on top is dithered and clipped
// to the left of the handle. Drag anywhere on it.
@Composable
fun ImageCompareExample(
    theme: StippleTheme,
    pattern: DitherPattern,
    algorithm: DitherAlgorithm,
) {
    var split by remember { mutableFloatStateOf(0.5f) }
    var width by remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current
    val left =
        object : Shape {
            override fun createOutline(
                size: androidx.compose.ui.geometry.Size,
                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                density: androidx.compose.ui.unit.Density,
            ) = Outline.Rectangle(Rect(0f, 0f, size.width * split, size.height))
        }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clipToBounds()
            .onSizeChanged { width = it.width.toFloat() }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { split = (it.x / width).coerceIn(0f, 1f) },
                ) { change, _ -> split = (change.position.x / width).coerceIn(0f, 1f) }
            },
    ) {
        Landscape(Modifier.fillMaxWidth().fillMaxHeight())
        Landscape(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clip(left)
                .ditherImage(pattern, theme, algorithm, key = "compare"),
        )
        Box(
            Modifier
                .offset(x = with(density) { (width * split).toDp() } - 1.dp)
                .width(2.dp)
                .fillMaxHeight()
                .background(theme.ink),
        )
    }
}
