package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.DitherField
import com.jantursky.stipple.DitherFields
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.dither

enum class FieldKind { Solid, Linear, Radial, Noise, Waves, Ripples, Sweep, Dissolve }

// A field says how dark each cell wants to be, from 0 (paper) to 1 (ink). The pattern turns that into dots.
@Composable
fun FieldsExample(
    kind: FieldKind,
    theme: StippleTheme,
    pattern: DitherPattern,
    animated: Boolean,
    progress: Float,
) {
    val field: DitherField =
        when (kind) {
            FieldKind.Solid -> DitherFields.solid(0.4f)
            FieldKind.Linear -> DitherFields.linear(from = 0f, to = 1f, angleDegrees = 45f)
            FieldKind.Radial -> DitherFields.radial(inner = 1f, outer = 0f, radius = 0.6f)
            FieldKind.Noise -> DitherFields.noise(scale = 3f)
            FieldKind.Waves -> DitherFields.waves(count = 4f)
            FieldKind.Ripples -> DitherFields.ripples(rings = 5f)
            FieldKind.Sweep -> DitherFields.sweep(progress)
            FieldKind.Dissolve -> DitherFields.dissolve({ progress })
        }
    // stillTime is the frame shown when nothing moves: a moment that looks finished.
    Box(Modifier.fillMaxWidth().height(240.dp).dither(field, pattern, theme, animated = animated, stillTime = 1.5f))
}
