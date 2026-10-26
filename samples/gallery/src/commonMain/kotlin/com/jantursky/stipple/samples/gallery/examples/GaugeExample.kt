package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.components.DitherGauge
import kotlin.math.roundToInt

// The ring fills from the start of its arc. The content slot holds the number, on plain paper. The gauge
// announces its value as a range to screen readers.
@Composable
fun GaugeExample(
    value: Float,
    theme: StippleTheme,
    pattern: DitherPattern,
    animated: Boolean,
) {
    DitherGauge(
        value = value,
        modifier = Modifier.size(240.dp),
        theme = theme,
        pattern = pattern,
        animated = animated,
        contentDescription = "Battery level",
    ) {
        BasicText(
            "${(value * 100).roundToInt()}%",
            style =
                TextStyle(
                    color = theme.ink,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                ),
        )
    }
}
