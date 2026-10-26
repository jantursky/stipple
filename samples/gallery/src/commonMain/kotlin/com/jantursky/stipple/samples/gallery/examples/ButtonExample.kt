package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.components.DitherButton

// The face stays plain paper so the label keeps its contrast; the shadow is the dither. Hover, focus and press
// change its tone, and pressing sinks the face into it.
@Composable
fun ButtonExample(
    theme: StippleTheme,
    pattern: DitherPattern,
    animated: Boolean,
    onClick: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        DitherButton("Save changes", onClick, theme = theme, pattern = pattern, animated = animated)
        DitherButton("Disabled", onClick, enabled = false, theme = theme, pattern = pattern)
    }
}
