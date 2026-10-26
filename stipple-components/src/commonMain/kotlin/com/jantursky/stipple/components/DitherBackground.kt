package com.jantursky.stipple.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jantursky.stipple.DitherField
import com.jantursky.stipple.DitherFields
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.DitherPatterns
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.components.internal.resolveTheme
import com.jantursky.stipple.dither

/**
 * A dithered backdrop with [content] on top. The default field is soft noise at a low tone, so text and controls
 * above it stay readable; pass your own [field] for rings, waves or a gradient.
 *
 * With [animated] the default noise drifts. The cell renderer draws every cell on every frame, so a backdrop that
 * covers a whole screen should use a larger theme cell: about 100,000 cells is the practical ceiling, and Stipple
 * coarsens the grid beyond 90,000.
 *
 * @param field The tone behind the content. Keep it under about 0.5 where text will sit.
 */
@Composable
public fun DitherBackground(
    modifier: Modifier = Modifier,
    field: DitherField? = null,
    theme: StippleTheme? = null,
    pattern: DitherPattern = DitherPatterns.Bayer4,
    animated: Boolean = false,
    content: @Composable () -> Unit = {},
) {
    val resolved = resolveTheme(theme)
    val tone = field ?: remember { DitherFields.noise(scale = 2.5f, speed = 0.12f, level = 0.2f, contrast = 0.8f) }
    Box(modifier.dither(tone, pattern, resolved, animated = animated, stillTime = 4f)) { content() }
}
