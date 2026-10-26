package com.jantursky.stipple.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.DitherField
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.DitherPatterns
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.components.internal.resolveTheme
import com.jantursky.stipple.dither
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A grid of squares, each dithered to its own tone: a heatmap, a contribution calendar, a level meter. [values] fill
 * the grid row by row, [columns] to a row, and each runs from 0 (paper) to 1 (ink). Missing values are empty.
 *
 * The height follows the width so the squares stay square. Each square is made of many dither cells, so the grain is
 * finer than the theme's default: see [cell], and keep it below a sixth of a square.
 *
 * With [animated] a band of light travels across the grid.
 *
 * @param gap The space between squares as a fraction of a square.
 * @param cell The size of one dither cell. Replaces the theme's.
 * @param contentDescription Read by screen readers; the grid is otherwise skipped, as it only decorates numbers.
 */
@Composable
public fun DitherGrid(
    values: List<Float>,
    columns: Int,
    modifier: Modifier = Modifier,
    theme: StippleTheme? = null,
    pattern: DitherPattern = DitherPatterns.Bayer4,
    animated: Boolean = false,
    gap: Float = 0.16f,
    cell: Dp = 1.dp,
    contentDescription: String? = null,
) {
    require(columns > 0) { "columns must be positive" }
    val rows = ceil(values.size / columns.toFloat()).toInt().coerceAtLeast(1)
    val resolved = resolveTheme(theme).let { it.copy(cell = cell) }
    val field = remember(values, columns, rows, gap, animated) { GridField(values, columns, rows, gap, animated) }
    Box(
        modifier
            .aspectRatio(columns / rows.toFloat())
            .semantics { if (contentDescription != null) this.contentDescription = contentDescription }
            .dither(field, pattern, resolved, animated = animated, stillTime = 0f),
    )
}

private class GridField(
    private val values: List<Float>,
    private val columns: Int,
    private val rows: Int,
    gap: Float,
    private val animated: Boolean,
) : DitherField {
    private val margin = gap.coerceIn(0f, 0.9f) / 2f

    override fun tone(
        x: Float,
        y: Float,
        t: Float,
        aspect: Float,
    ): Float {
        val gx = x * columns
        val gy = y * rows
        val u = gx - floor(gx)
        val v = gy - floor(gy)
        if (u < margin || u > 1f - margin || v < margin || v > 1f - margin) return 0f
        val index = floor(gy).toInt().coerceIn(0, rows - 1) * columns + floor(gx).toInt().coerceIn(0, columns - 1)
        val value = values.getOrNull(index) ?: return 0f
        if (!animated) return value
        // A diagonal band of light travels over the squares and wraps around.
        val band = ((x + y) / 2f - (t * 0.25f) % 1.4f + 0.2f)
        return value + 0.35f * (1f - (abs(band) / 0.12f).coerceIn(0f, 1f)) * (if (value > 0f) 1f else 0f)
    }
}
