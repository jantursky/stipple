package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.components.DitherText

// Letters keep their shape and take the dither as their fill. Contrast is enforced: if ink and paper are too
// close the ink is swapped for black or white. Use it for headings, not body text.
@Composable
fun TextExample(
    text: String,
    fadeTo: Float,
    theme: StippleTheme,
    pattern: DitherPattern,
    animated: Boolean,
) {
    DitherText(
        text = text,
        style = TextStyle(fontSize = 96.sp, fontWeight = FontWeight.Bold, lineHeight = 100.sp),
        theme = theme,
        pattern = pattern,
        animated = animated,
        fadeTo = fadeTo,
    )
}
