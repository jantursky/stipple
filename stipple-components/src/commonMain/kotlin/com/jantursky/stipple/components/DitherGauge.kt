package com.jantursky.stipple.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.jantursky.stipple.DitherField
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.DitherPatterns
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.components.internal.rememberEased
import com.jantursky.stipple.components.internal.resolveTheme
import com.jantursky.stipple.dither
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * A circular gauge: a ring that fills with a dither from the start of its arc up to [value], with a faint track for
 * the rest. [content] sits in the middle, for the number.
 *
 * With [animated] the gauge sweeps to each new value, and the fill breathes slightly. The gauge announces [value] as
 * a range, in percent of [range].
 *
 * @param value A value within [range].
 * @param startAngle Where the arc starts, in degrees clockwise from the right: 135 starts at the bottom left.
 * @param sweepAngle How far the arc runs: 270 leaves a gap at the bottom, 360 closes the ring.
 * @param thickness The width of the ring as a fraction of the gauge's radius, from 0 to 1.
 */
@Composable
public fun DitherGauge(
    value: Float,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    theme: StippleTheme? = null,
    pattern: DitherPattern = DitherPatterns.Bayer4,
    animated: Boolean = false,
    startAngle: Float = 135f,
    sweepAngle: Float = 270f,
    thickness: Float = 0.28f,
    contentDescription: String? = null,
    content: @Composable () -> Unit = {},
) {
    val resolved = resolveTheme(theme)
    val span = (range.endInclusive - range.start).takeIf { it > 0f } ?: 1f
    val fraction = ((value - range.start) / span).coerceIn(0f, 1f)
    val eased = rememberEased(fraction, animated, initial = 0f, durationMillis = 900)
    val field = remember(startAngle, sweepAngle, thickness) { GaugeField(startAngle, sweepAngle, thickness) }
    Box(
        modifier
            .aspectRatio(1f)
            .semantics(mergeDescendants = true) {
                if (contentDescription != null) this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(range), range)
            }.dither(field.reading { eased.value }, pattern, resolved, animated = animated),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
    }
}

/** The ring's geometry; [reading] binds the value, read per cell in the draw phase. */
private class GaugeField(
    private val startAngle: Float,
    private val sweepAngle: Float,
    private val thickness: Float,
) {
    fun reading(value: () -> Float): DitherField =
        DitherField { x, y, t, aspect ->
            val dx = (x - 0.5f) * aspect
            val dy = y - 0.5f
            val r = hypot(dx, dy) * 2f
            if (r > 1f || r < 1f - thickness) return@DitherField 0f
            var degrees = atan2(dy, dx) * 180f / PI.toFloat() - startAngle
            degrees = ((degrees % 360f) + 360f) % 360f
            if (degrees > sweepAngle) return@DitherField 0f
            val along = degrees / sweepAngle
            if (along <= value()) 0.62f + 0.38f * along else TRACK_TONE
        }

    companion object {
        const val TRACK_TONE = 0.16f
    }
}
