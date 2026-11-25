package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jantursky.stipple.DitherFields
import com.jantursky.stipple.DitherPatterns
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.dither

// The same gradient through every built-in pattern. Bayer matrices leave a regular grid, blue and white noise have none, and halftone, lines and crosshatch
// read as print screens.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PatternsExample(theme: StippleTheme) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DitherPatterns.All.forEach { pattern ->
            Column(Modifier.width(112.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth().aspectRatio(1f).dither(DitherFields.linear(), pattern, theme))
                BasicText(DitherPatterns.nameOf(pattern), style = TextStyle(color = theme.ink, fontSize = 12.sp))
            }
        }
    }
}
