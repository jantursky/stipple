package com.jantursky.stipple.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jantursky.stipple.DitherField
import com.jantursky.stipple.DitherFields
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.DitherPatterns
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.components.internal.resolveTheme
import com.jantursky.stipple.contrastRatio
import com.jantursky.stipple.ditherFill

/**
 * Text whose letters are filled with a dither. The glyphs keep their shape, and the fill fades from full ink to
 * [fadeTo] across [field], so a headline can dissolve into grain without losing its first letters.
 *
 * Readability is enforced, not left to the caller. If the theme's ink and paper have less than [minContrast] between
 * them, the ink is swapped for black or white, whichever contrasts more with the paper. Keep [fadeTo] at 0.6 or
 * above for anything people have to read, and avoid body-sized text: dither cells cut into thin strokes, and below
 * about 20sp the letters lose their edges. Use it for headings, numbers and labels.
 *
 * With [animated] the fill drifts, so the grain seems to move through the letters.
 *
 * @param cell The size of one dither cell. Text needs a finer grain than the theme's default.
 * @param fadeTo The tone at the far end of the default [field].
 * @param minContrast The least contrast ratio between ink and paper; 4.5 is the WCAG level for text.
 */
@Composable
public fun DitherText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold),
    theme: StippleTheme? = null,
    pattern: DitherPattern = DitherPatterns.Bayer4,
    animated: Boolean = false,
    cell: Dp = 1.dp,
    fadeTo: Float = 0.7f,
    field: DitherField? = null,
    minContrast: Float = 4.5f,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val resolved = readable(resolveTheme(theme), minContrast).copy(cell = cell)
    val fill =
        field
            ?: remember(fadeTo, animated) {
                if (animated) {
                    DitherFields.waves(count = 3f, speed = 0.35f, angleDegrees = 20f, low = fadeTo, high = 1f)
                } else {
                    DitherFields.linear(from = 1f, to = fadeTo, angleDegrees = 0f)
                }
            }
    BasicText(
        text,
        modifier.ditherFill(fill, pattern, resolved, animated = animated),
        style.copy(color = Color.Black),
        maxLines = maxLines,
        overflow = overflow,
    )
}

/** [theme], or the same paper with black or white ink when its own ink falls short of [minContrast]. */
internal fun readable(
    theme: StippleTheme,
    minContrast: Float,
): StippleTheme {
    if (theme.contrastRatio >= minContrast) return theme
    val black = Color.Black
    val white = Color.White
    val ink = if (contrastRatio(black, theme.paper) >= contrastRatio(white, theme.paper)) black else white
    return theme.copy(ink = ink, palette = null)
}
