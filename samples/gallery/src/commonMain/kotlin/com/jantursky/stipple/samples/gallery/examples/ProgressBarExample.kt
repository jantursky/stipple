package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.components.DitherProgressBar

// A determinate bar eases to each new value; a null progress is indeterminate and rests mid-track under reduced
// motion.
@Composable
fun ProgressBarExample(
    progress: Float,
    theme: StippleTheme,
    pattern: DitherPattern,
    animated: Boolean,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        DitherProgressBar(progress, theme = theme, pattern = pattern, animated = animated, contentDescription = "Upload")
        DitherProgressBar(null, theme = theme, pattern = pattern, animated = animated, contentDescription = "Loading")
    }
}
