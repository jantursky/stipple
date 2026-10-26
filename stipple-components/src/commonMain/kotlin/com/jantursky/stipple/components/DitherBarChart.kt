package com.jantursky.stipple.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.DitherFields
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.DitherPatterns
import com.jantursky.stipple.ProgressDirection
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.components.internal.Label
import com.jantursky.stipple.components.internal.rememberEased
import com.jantursky.stipple.components.internal.resolveTheme
import com.jantursky.stipple.ditherProgress

/** One column of a [DitherBarChart]. */
@Immutable
public class BarEntry(
    public val label: String,
    public val value: Float,
)

/**
 * A column chart whose columns are filled with a dither that is dense at the foot and lighter towards the top.
 *
 * With [animated] the columns grow in one after another and ease to new values. The chart is one accessibility node
 * that reads out every label and value, or [contentDescription] when given.
 *
 * @param maxValue The value of a full-height column. Defaults to the largest entry.
 * @param barGap The space between columns.
 */
@Composable
public fun DitherBarChart(
    entries: List<BarEntry>,
    modifier: Modifier = Modifier,
    maxValue: Float = entries.maxOfOrNull { it.value }?.takeIf { it > 0f } ?: 1f,
    theme: StippleTheme? = null,
    pattern: DitherPattern = DitherPatterns.Bayer4,
    animated: Boolean = false,
    barGap: Dp = 8.dp,
    showLabels: Boolean = true,
    contentDescription: String? = null,
) {
    val resolved = resolveTheme(theme)
    val description = contentDescription ?: entries.joinToString(", ") { "${it.label} ${it.value.cleanNumber()}" }
    Row(
        modifier.semantics(mergeDescendants = true) { this.contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(barGap),
    ) {
        entries.forEachIndexed { index, entry ->
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                val level =
                    rememberEased(
                        (entry.value / maxValue).coerceIn(0f, 1f),
                        animated,
                        initial = 0f,
                        delayMillis = index * 70,
                    )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .ditherProgress(
                            progress = { level.value },
                            pattern = pattern,
                            theme = resolved,
                            direction = ProgressDirection.Up,
                            softness = 0.03f,
                            fill = ColumnFill,
                        ),
                )
                if (showLabels) {
                    Label(entry.label, resolved.ink, weight = FontWeight.Medium, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

// Dense at the foot, lighter near the tip: the element is the full column, so the tone is read against its height.
private val ColumnFill = DitherFields.linear(from = 0.55f, to = 1f, angleDegrees = 90f)

private fun Float.cleanNumber(): String = if (this == toInt().toFloat()) toInt().toString() else toString()
