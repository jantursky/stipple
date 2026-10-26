package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.DitherField
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.dither
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin

enum class ChartKind { Area, Heatmap, Donut, Uptime }

// A chart is a field too: the data goes into the function, the pattern turns the tones into texture. Tones above
// the highest threshold of the pattern come out solid, so 1 gives a solid ink block.
private fun hash(
    x: Int,
    y: Int,
): Float {
    var h = x * 374761393 + y * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    return ((h xor (h ushr 16)) and 0xFFFF) / 65535f
}

private fun ramp(
    edge0: Float,
    edge1: Float,
    x: Float,
): Float = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)

// The line is solid, the fill thins out the further it falls below the line.
private val area =
    DitherField { x, y, _, _ ->
        val line = 0.5f - 0.22f * sin(x * 6f) - 0.18f * x
        val below = y - line
        when {
            abs(below) < 0.012f -> 1f
            below > 0f -> 0.75f * (1f - ramp(0f, 0.6f, below))
            else -> 0f
        }
    }

// Twenty weeks by seven days; the value of a day sets how dense its square is.
private val heatmap =
    DitherField { x, y, _, aspect ->
        val columns = 24f
        val rows = 7f
        val gx = x * columns
        val gy = y * rows
        val inside = abs(gx - floor(gx) - 0.5f) < 0.42f && abs(gy - floor(gy) - 0.5f) < 0.42f
        if (inside) 0.08f + 0.92f * hash(gx.toInt(), gy.toInt()) else 0f
    }

// Four slices, each with its own density, and a gap between them.
private val donut =
    DitherField { x, y, _, aspect ->
        val dx = (x - 0.5f) * aspect
        val dy = y - 0.5f
        val r = hypot(dx, dy)
        if (r < 0.28f || r > 0.46f) {
            0f
        } else {
            val turn = (atan2(dy, dx) / (2f * PI.toFloat()) + 1.25f) % 1f
            val edges = floatArrayOf(0.38f, 0.65f, 0.85f, 1f)
            val tones = floatArrayOf(1f, 0.62f, 0.38f, 0.2f)
            val slice = edges.indexOfFirst { turn < it }
            val gap = edges.any { abs(turn - it) < 0.008f } || turn < 0.008f
            if (gap) 0f else tones[slice]
        }
    }

// Forty days: solid when up, lighter on a degraded day, empty on an outage.
private val uptime =
    DitherField { x, _, _, _ ->
        val bars = 40f
        val gx = x * bars
        val day = gx.toInt()
        if (gx - floor(gx) > 0.72f) {
            0f
        } else {
            when {
                day == 9 || day == 27 -> 0f
                day == 17 || day == 31 || day == 32 -> 0.5f
                else -> 1f
            }
        }
    }

@Composable
fun ChartsExample(
    kind: ChartKind,
    theme: StippleTheme,
    pattern: DitherPattern,
) {
    val look =
        when (kind) {
            ChartKind.Area -> Modifier.fillMaxWidth().height(200.dp)
            ChartKind.Heatmap -> Modifier.fillMaxWidth().aspectRatio(24f / 7f)
            ChartKind.Donut -> Modifier.fillMaxWidth().height(260.dp)
            ChartKind.Uptime -> Modifier.fillMaxWidth().height(48.dp)
        }
    val field =
        when (kind) {
            ChartKind.Area -> area
            ChartKind.Heatmap -> heatmap
            ChartKind.Donut -> donut
            ChartKind.Uptime -> uptime
        }
    Box(look.dither(field, pattern, theme))
}
