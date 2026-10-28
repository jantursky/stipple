package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.DitherFields
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.ditherMask
import com.jantursky.stipple.samples.gallery.Landscape

enum class MaskReveal { Sweep, Dissolve }

// ditherMask shows the content only where the dither is lit. A sweep reveals it from the left through the pattern, a
// dissolve in an irregular order. Both read the reveal while drawing, so dragging it recomposes nothing.
@Composable
fun MaskExample(
    reveal: Float,
    kind: MaskReveal,
    theme: StippleTheme,
    pattern: DitherPattern,
) {
    Landscape(
        Modifier
            .fillMaxWidth()
            .aspectRatio(2f)
            .ditherMask(
                when (kind) {
                    MaskReveal.Sweep -> DitherFields.sweep({ reveal }, softness = 0.25f)
                    MaskReveal.Dissolve -> DitherFields.dissolve({ reveal })
                },
                pattern,
                theme.copy(cell = 4.dp),
            ),
    )
}
