package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.DitherField
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.dither
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin

enum class InterfaceKind { Divider, Skeleton, Loader, Sparkline }

// Small interface pieces use the same two ingredients as everything else: a clipped box and a field.
private val divider =
    DitherField { x, _, _, _ -> (minOf(x, 1f - x) * 6f).coerceIn(0f, 1f) }

// A band of light sweeps across the placeholder.
private val skeleton =
    DitherField { x, _, t, _ ->
        val band = 0.5f + 0.5f * cos((x - (t * 0.4f) % 1.4f + 0.2f) * 2f * PI.toFloat() / 0.5f)
        0.18f + 0.22f * band
    }

// A comet: the tone fades behind the head as it goes round.
private val spinner =
    DitherField { x, y, t, _ ->
        val r = hypot(x - 0.5f, y - 0.5f)
        if (r > 0.5f || r < 0.25f) {
            0f
        } else {
            val turns = (t * 3f - atan2(y - 0.5f, x - 0.5f)) / (2f * PI.toFloat())
            1f - (turns - floor(turns))
        }
    }

private val pulse =
    DitherField { x, y, t, _ ->
        val r = hypot(x - 0.5f, y - 0.5f) * 2f
        (1f - r).coerceIn(0f, 1f) * (0.6f + 0.4f * sin(t * 4f))
    }

// Dense under the line, sparse further down.
private val sparkline =
    DitherField { x, y, _, _ ->
        val line = 0.55f - 0.25f * sin(x * 7f) * (0.4f + 0.6f * x) - 0.2f * x
        if (y < line) 0f else (1f - (y - line) * 1.6f).coerceIn(0f, 1f)
    }

@Composable
fun InterfaceExample(
    kind: InterfaceKind,
    theme: StippleTheme,
    pattern: DitherPattern,
    animated: Boolean,
) {
    fun Modifier.fill(field: DitherField) = dither(field, pattern, theme, animated = animated, stillTime = 1f)
    when (kind) {
        InterfaceKind.Divider -> {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Box(Modifier.fillMaxWidth().height(6.dp).fill(divider))
                Box(Modifier.fillMaxWidth().height(2.dp).fill(divider))
            }
        }

        InterfaceKind.Skeleton -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(56.dp).clip(CircleShape).fill(skeleton))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .fill(skeleton),
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.7f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .fill(skeleton),
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.4f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .fill(skeleton),
                )
            }
        }

        InterfaceKind.Loader -> {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(64.dp).fill(spinner))
                Box(Modifier.size(64.dp).fill(pulse))
            }
        }

        InterfaceKind.Sparkline -> {
            Box(Modifier.fillMaxWidth().height(96.dp).fill(sparkline))
        }
    }
}
