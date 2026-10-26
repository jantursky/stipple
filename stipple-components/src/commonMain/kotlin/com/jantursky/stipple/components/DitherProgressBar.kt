package com.jantursky.stipple.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.DitherField
import com.jantursky.stipple.DitherFields
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.DitherPatterns
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.components.internal.rememberEased
import com.jantursky.stipple.components.internal.resolveTheme
import com.jantursky.stipple.dither
import com.jantursky.stipple.ditherProgress
import kotlin.math.abs

/**
 * A progress bar filled with a dither. [progress] runs from 0 to 1.
 *
 * With [animated] the bar eases to each new value and bands of lighter dots travel along the fill. Pass a null
 * [progress] for an indeterminate bar: a band of ink sweeps across the track, or rests mid-track under reduced motion.
 *
 * @param track The tone of the unfilled part, 0 for plain paper.
 * @param contentDescription Read by screen readers, together with the progress value.
 */
@Composable
public fun DitherProgressBar(
    progress: Float?,
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    theme: StippleTheme? = null,
    pattern: DitherPattern = DitherPatterns.Bayer4,
    animated: Boolean = false,
    track: Float = 0.12f,
    contentDescription: String? = null,
) {
    val resolved = resolveTheme(theme)
    val bar =
        modifier
            .fillMaxWidth()
            .height(height)
            .semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
                progressBarRangeInfo =
                    if (progress == null) {
                        ProgressBarRangeInfo.Indeterminate
                    } else {
                        ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f)
                    }
            }.border(1.dp, resolved.ink)
    if (progress == null) {
        Box(
            bar.dither(
                remember(track) { IndeterminateBand(track) },
                pattern,
                resolved,
                animated = animated,
                stillTime = 0.6f,
            ),
        )
        return
    }
    val eased = rememberEased(progress.coerceIn(0f, 1f), animated)
    Box(
        bar.ditherProgress(
            progress = { eased.value },
            pattern = pattern,
            theme = resolved,
            track = track,
            fill = if (animated) MovingBands else DitherFields.solid(1f),
            animated = animated,
        ),
    )
}

private val MovingBands = DitherFields.waves(count = 6f, speed = 0.6f, angleDegrees = 0f, low = 0.62f, high = 1f)

/** A band of full ink travelling across a faint track; the field's time is its position. */
private class IndeterminateBand(
    private val track: Float,
) : DitherField {
    override fun tone(
        x: Float,
        y: Float,
        t: Float,
        aspect: Float,
    ): Float {
        val center = (t * 0.45f) % 1.5f - 0.25f
        return track + (1f - track) * (1f - (abs(x - center) / 0.2f).coerceIn(0f, 1f))
    }
}
