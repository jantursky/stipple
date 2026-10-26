package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.layout.Box
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class CustomKind { Spiral, Plasma, Checker }

// A field is one function of position and time. Return a smooth tone from 0 to 1 and scale x by `aspect` before
// measuring a distance, so circles stay round on a wide element.
private val spiral =
    DitherField { x, y, t, aspect ->
        val dx = (x - 0.5f) * aspect
        val dy = y - 0.5f
        val angle = atan2(dy, dx)
        val ring = hypot(dx, dy) * 14f
        0.5f + 0.5f * sin(ring - angle * 2f - t * 2f)
    }

private val plasma =
    DitherField { x, y, t, aspect ->
        val u = x * aspect * 6f
        val v = y * 6f
        (sin(u + t) + sin(v * 1.3f - t * 0.7f) + sin((u + v) * 0.7f + t * 0.5f) + 3f) / 6f
    }

private val checker =
    DitherField { x, y, t, aspect ->
        val cx = (x * aspect * 8f).toInt()
        val cy = (y * 8f).toInt()
        // Squares breathe in turn: the phase depends on which square, the tone on the phase.
        0.5f + 0.5f * cos(t * 2f + (cx + cy) * PI.toFloat() / 2f) * if ((cx + cy) % 2 == 0) 1f else 0.6f
    }

@Composable
fun CustomFieldExample(
    kind: CustomKind,
    theme: StippleTheme,
    pattern: DitherPattern,
    animated: Boolean,
) {
    val field =
        when (kind) {
            CustomKind.Spiral -> spiral
            CustomKind.Plasma -> plasma
            CustomKind.Checker -> checker
        }
    Box(Modifier.fillMaxWidth().height(240.dp).dither(field, pattern, theme, animated = animated, stillTime = 1f))
}
