package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.components.DitherStatCard

// Text sits on plain paper; only the trend along the foot is dithered.
@Composable
fun StatCardExample(
    trend: List<Float>,
    theme: StippleTheme,
    pattern: DitherPattern,
    animated: Boolean,
) {
    DitherStatCard(
        label = "Monthly revenue",
        value = "$48,210",
        delta = "+12.4% from last month",
        trend = trend,
        modifier = Modifier.width(300.dp),
        theme = theme,
        pattern = pattern,
        animated = animated,
    )
}
