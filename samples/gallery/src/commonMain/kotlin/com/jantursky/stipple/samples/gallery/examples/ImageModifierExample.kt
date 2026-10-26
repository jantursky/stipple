package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.DitherAlgorithm
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.ditherImage
import com.jantursky.stipple.samples.gallery.Landscape

// ditherImage dithers whatever the element draws, once, off the main thread. Pass a key that changes with the
// content. The same picture three ways: ordered, Floyd-Steinberg and Atkinson.
@Composable
fun ImageModifierExample(
    theme: StippleTheme,
    pattern: DitherPattern,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DitherAlgorithm.entries.forEach { algorithm ->
            Landscape(
                Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .ditherImage(pattern, theme, algorithm, key = "landscape"),
            )
        }
    }
}
