package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jantursky.stipple.DitherFields
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.components.DitherBackground

// A backdrop with content on top. Keep the field's tone low where text sits; a screen-sized background wants a
// larger cell, because every cell is drawn on every frame.
@Composable
fun BackgroundExample(
    theme: StippleTheme,
    pattern: DitherPattern,
    animated: Boolean,
) {
    DitherBackground(
        modifier = Modifier.fillMaxWidth().height(260.dp),
        field = DitherFields.ripples(rings = 4f, radius = 1f, centerX = 0.2f, centerY = 0.9f),
        theme = theme,
        pattern = pattern,
        animated = animated,
    ) {
        Column(Modifier.fillMaxWidth().height(260.dp), Arrangement.Center, Alignment.CenterHorizontally) {
            BasicText("Content on top", style = TextStyle(color = theme.ink, fontSize = 28.sp, fontWeight = FontWeight.SemiBold))
        }
    }
}
