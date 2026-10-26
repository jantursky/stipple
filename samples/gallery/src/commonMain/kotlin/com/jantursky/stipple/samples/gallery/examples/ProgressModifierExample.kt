package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.DitherFields
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.ProgressDirection
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.ditherProgress

// ditherProgress fills an element up to a value. Read your state inside the lambda and a change redraws without
// recomposing.
@Composable
fun ProgressModifierExample(
    progress: Float,
    theme: StippleTheme,
    pattern: DitherPattern,
    animated: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .border(1.dp, theme.ink)
                .ditherProgress({ progress }, pattern, theme, track = 0.1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ProgressDirection.entries.forEach { direction ->
                Box(
                    Modifier
                        .size(88.dp)
                        .border(1.dp, theme.ink)
                        .ditherProgress(
                            progress = { progress },
                            pattern = pattern,
                            theme = theme,
                            direction = direction,
                            fill = DitherFields.waves(count = 3f, low = 0.55f, high = 1f),
                            animated = animated,
                        ),
                )
            }
        }
    }
}
