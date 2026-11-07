package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jantursky.stipple.DitherAlgorithm
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.ImageDitherOptions
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.components.DitherImage
import com.jantursky.stipple.samples.gallery.LandscapePainter

// Error diffusion runs once on a background dispatcher and is cached. reveal lets the original through a dither
// pattern from the left; animated sweeps it back and forth. ImageDitherOptions adds tone controls, serpentine
// scanning and linear-light luminance.
@Composable
fun ImageExample(
    algorithm: DitherAlgorithm,
    reveal: Float,
    theme: StippleTheme,
    pattern: DitherPattern,
    animated: Boolean,
    options: ImageDitherOptions = ImageDitherOptions(),
) {
    // Remember the painter: the cached result is keyed by it.
    val painter = remember { LandscapePainter() }
    DitherImage(
        painter = painter,
        contentDescription = "A sunset over three ridges",
        options = options,
        modifier = Modifier.fillMaxWidth().aspectRatio(1.8f),
        theme = theme,
        pattern = pattern,
        algorithm = algorithm,
        reveal = reveal,
        animated = animated,
    )
}
