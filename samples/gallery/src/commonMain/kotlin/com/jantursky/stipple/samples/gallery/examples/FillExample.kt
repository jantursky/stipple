package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jantursky.stipple.DitherFields
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.ditherFill

// ditherFill keeps the shape of what the element draws and swaps its colors for the dither. Keep the tone high
// where the shape has to stay readable.
@Composable
fun FillExample(
    theme: StippleTheme,
    pattern: DitherPattern,
    animated: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText(
            "Stipple",
            Modifier.ditherFill(DitherFields.linear(1f, 0.35f, 0f), pattern, theme.copy(cell = 1.5.dp)),
            style = TextStyle(color = Color.Black, fontSize = 88.sp, fontWeight = FontWeight.Bold),
        )
        BasicText(
            "dither",
            Modifier.ditherFill(
                DitherFields.waves(count = 3f, low = 0.45f, high = 1f),
                pattern,
                theme.copy(cell = 1.5.dp),
                animated = animated,
            ),
            style = TextStyle(color = Color.Black, fontSize = 88.sp, fontWeight = FontWeight.Bold),
        )
    }
}
