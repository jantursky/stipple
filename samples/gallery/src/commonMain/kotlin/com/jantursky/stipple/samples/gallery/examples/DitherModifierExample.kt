package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.jantursky.stipple.dither

// Modifier.dither fills the element's shape behind its content, so text and children sit on top of the grain.
@Composable
fun DitherModifierExample(
    theme: StippleTheme,
    pattern: DitherPattern,
    animated: Boolean,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .dither(
                field = DitherFields.radial(inner = 0.9f, outer = 0.1f, centerX = 0.3f, centerY = 0.35f, radius = 0.9f),
                pattern = pattern,
                theme = theme,
                shape = RoundedCornerShape(24.dp),
                animated = animated,
            ),
        contentAlignment = Alignment.BottomStart,
    ) {
        BasicText(
            "Behind the content",
            Modifier.padding(20.dp),
            style = TextStyle(color = theme.paper, fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}
