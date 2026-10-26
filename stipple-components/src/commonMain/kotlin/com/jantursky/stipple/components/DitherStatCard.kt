package com.jantursky.stipple.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jantursky.stipple.DitherField
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.DitherPatterns
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.components.internal.Label
import com.jantursky.stipple.components.internal.rememberEased
import com.jantursky.stipple.components.internal.resolveTheme
import com.jantursky.stipple.dither
import kotlin.math.abs
import kotlin.math.floor

/**
 * A metric on a card: a [label], a large [value], an optional [delta] such as "+12%", and an optional [trend] drawn
 * along the foot as a dithered area. Text sits on plain paper, so it stays readable; only the trend is dithered.
 *
 * With [animated] the trend draws in from the left and a soft band of light travels along its top.
 *
 * @param trend Values in any range; they are scaled to fill the strip.
 * @param trendHeight The height of the trend strip.
 */
@Composable
public fun DitherStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    delta: String? = null,
    trend: List<Float> = emptyList(),
    theme: StippleTheme? = null,
    pattern: DitherPattern = DitherPatterns.Bayer4,
    animated: Boolean = false,
    trendHeight: Dp = 56.dp,
) {
    val resolved = resolveTheme(theme)
    Column(
        modifier
            .semantics(mergeDescendants = true) {
                contentDescription =
                    buildString {
                        append(label).append(": ").append(value)
                        if (delta != null) append(", ").append(delta)
                    }
            }.background(resolved.paper)
            .border(1.dp, resolved.ink),
    ) {
        Column(Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 10.dp)) {
            Label(label, resolved.ink, sizeSp = 12)
            Spacer(Modifier.height(6.dp))
            BasicText(
                value,
                style =
                    TextStyle(
                        color = resolved.ink,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                    ),
            )
            if (delta != null) {
                Spacer(Modifier.height(2.dp))
                Label(delta, resolved.ink, sizeSp = 12, weight = FontWeight.Normal)
            }
        }
        if (trend.size >= 2) {
            val drawn = rememberEased(1f, animated, initial = 0f, durationMillis = 900)
            val area = remember(trend) { TrendArea(trend) }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(trendHeight)
                    .dither(
                        area.drawing { drawn.value },
                        pattern,
                        resolved.copy(cell = 2.dp),
                        animated = animated,
                        stillTime = 0f,
                    ),
            )
        } else {
            Box(Modifier.height(6.dp))
        }
    }
}

/** The area under a polyline, dense under the line and fading downwards. */
private class TrendArea(
    values: List<Float>,
) {
    private val low = values.min()
    private val high = values.max()
    private val points = values.map { if (high > low) (it - low) / (high - low) else 0.5f }

    fun drawing(progress: () -> Float): DitherField =
        DitherField { x, y, t, _ ->
            val reveal = progress()
            if (x > reveal) return@DitherField 0f
            val position = x * (points.size - 1)
            val i = floor(position).toInt().coerceIn(0, points.size - 2)
            val line = 1f - (points[i] + (points[i + 1] - points[i]) * (position - i)) * 0.8f - 0.1f
            when {
                y < line -> {
                    0f
                }

                else -> {
                    val depth = (y - line) / (1f - line).coerceAtLeast(0.01f)
                    // The line itself is solid; below it the tone falls away. A light band rides along it while animating.
                    val body = if (y - line < 0.06f) 1f else 0.78f - 0.6f * depth
                    body + 0.2f * (1f - (abs(x - (t * 0.3f) % 1.4f + 0.2f) / 0.1f).coerceIn(0f, 1f)) * depth
                }
            }
        }
}
