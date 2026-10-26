package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.components.BarEntry
import com.jantursky.stipple.components.DitherBarChart

// One accessibility node reads out every label and value. With animated the columns grow in one after another
// and ease to new values.
@Composable
fun BarChartExample(
    values: List<Float>,
    theme: StippleTheme,
    pattern: DitherPattern,
    animated: Boolean,
) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    DitherBarChart(
        entries = values.mapIndexed { i, v -> BarEntry(days[i % days.size], v) },
        modifier = Modifier.fillMaxWidth().height(240.dp),
        theme = theme,
        pattern = pattern,
        animated = animated,
    )
}
