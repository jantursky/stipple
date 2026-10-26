package com.jantursky.stipple.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jantursky.stipple.DitherAlgorithm
import com.jantursky.stipple.DitherField
import com.jantursky.stipple.DitherFields
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.DitherPatterns
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.components.internal.resolveTheme
import com.jantursky.stipple.dither
import com.jantursky.stipple.ditherImage

/**
 * An avatar. With a [painter] the photo is dithered; without one the avatar is the initials of [name] on a generated
 * dither, the same for the same name every time.
 *
 * With [animated] the generated background drifts. A photo is static content, so [animated] has no effect on it.
 *
 * @param name Who it is. Read by screen readers, and the source of the initials and the generated pattern.
 * @param size The width and height.
 */
@Composable
public fun DitherAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    painter: Painter? = null,
    shape: Shape = CircleShape,
    theme: StippleTheme? = null,
    pattern: DitherPattern = DitherPatterns.Bayer4,
    algorithm: DitherAlgorithm = DitherAlgorithm.FloydSteinberg,
    animated: Boolean = false,
) {
    val resolved = resolveTheme(theme).let { it.copy(cell = (size / 24).coerceAtLeast(1.dp)) }
    val frame = modifier.size(size).clip(shape).semantics { contentDescription = name }
    if (painter != null) {
        Image(
            painter,
            null,
            frame.ditherImage(pattern, resolved, algorithm, key = painter),
            contentScale = ContentScale.Crop,
        )
        return
    }
    val field = remember(name, animated) { generatedField(name, animated) }
    Box(frame.dither(field, pattern, resolved, animated = animated), contentAlignment = Alignment.Center) {
        Box(Modifier.size(size * 0.62f).clip(shape).background(resolved.paper))
        BasicText(
            initials(name),
            style = TextStyle(color = resolved.ink, fontSize = (size.value * 0.26f).sp, fontWeight = FontWeight.Bold),
        )
    }
}

/** At most two letters: the first of the first two words, or the first two letters of a single word. */
internal fun initials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(2).uppercase()
        else -> "${words[0].first()}${words[1].first()}".uppercase()
    }
}

/** A gradient and noise that depend only on [name]. */
private fun generatedField(
    name: String,
    animated: Boolean,
): DitherField {
    var hash = 17
    for (c in name) hash = hash * 31 + c.code
    fun unit(shift: Int): Float = (((hash ushr shift) and 0xFF) / 255f)
    val angle = unit(0) * 360f
    val ramp = DitherFields.linear(from = 0.15f, to = 0.95f, angleDegrees = angle)
    val grain = DitherFields.noise(scale = 2f + unit(8) * 3f, speed = if (animated) 0.4f else 0f, level = 0f, contrast = 0.5f)
    return DitherField { x, y, t, aspect -> ramp.tone(x, y, t, aspect) + grain.tone(x, y, t, aspect) }
}
