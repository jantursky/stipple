package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.DitherFields
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.DitherRenderer
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.dither

enum class MeshPreset(
    val tones: List<List<Float>>,
) {
    Aurora(
        listOf(
            listOf(0.1f, 0.5f, 0.9f, 0.4f),
            listOf(0.6f, 1f, 0.3f, 0.8f),
            listOf(0.2f, 0.4f, 0.7f, 0.1f),
        ),
    ),
    Dusk(
        listOf(
            listOf(0f, 0.15f, 0.3f),
            listOf(0.3f, 0.5f, 0.7f),
            listOf(0.7f, 0.85f, 1f),
        ),
    ),
    Spots(
        listOf(
            listOf(1f, 0f, 0f, 0f, 1f),
            listOf(0f, 0f, 0.8f, 0f, 0f),
            listOf(0f, 0.6f, 0f, 0.9f, 0f),
            listOf(1f, 0f, 0f, 0f, 0.3f),
        ),
    ),
}

// A mesh gradient is a grid of tones blended smoothly. Each point holds its tone exactly; with drift the interior
// points wander while the edges stay where they are. It is drawn by the shader renderer where the device has one.
@Composable
fun MeshExample(
    preset: MeshPreset,
    drift: Float,
    theme: StippleTheme,
    pattern: DitherPattern,
    animated: Boolean,
) {
    val field = remember(preset, drift) { DitherFields.mesh(preset.tones, drift = drift, speed = 0.25f) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(280.dp)
            .dither(
                field,
                pattern,
                theme,
                animated = animated && drift > 0f,
                stillTime = 0.5f,
                renderer = DitherRenderer.Shader,
            ),
    )
}
