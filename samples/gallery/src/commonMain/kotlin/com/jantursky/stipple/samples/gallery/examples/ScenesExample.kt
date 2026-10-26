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
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class Scene {
    Glow,
    Aurora,
    Waves,
    Ripples,
    Metaballs,
    Tunnel,
    RetroGrid,
    Eclipse,
    Starfield,
    Contours,
    Halftone,
    Beams,
}

// Every scene is one DitherField: a pure function of position and time that returns a tone from 0 (paper) to 1 (ink).
// None of them know about colours or cells; the pattern and the theme turn the tone into dots.
private fun smooth(
    edge0: Float,
    edge1: Float,
    x: Float,
): Float {
    val k = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return k * k * (3f - 2f * k)
}

private fun fract(v: Float): Float = v - floor(v)

private fun hash(
    x: Int,
    y: Int,
): Float {
    var h = x * 374761393 + y * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    return ((h xor (h ushr 16)) and 0xFFFF) / 65535f
}

private val glow =
    DitherField { x, y, t, aspect ->
        val d = hypot((x - 0.5f) * aspect, y * 1.1f)
        (1f - smooth(0f, 0.9f + 0.08f * sin(t), d)).let { it * it }
    }

private val aurora =
    DitherField { x, y, t, aspect ->
        val u = x * aspect
        var tone = 0f
        // Two curtains at different heights and speeds; each is a soft band with fine vertical rays along it.
        for (k in 0 until 2) {
            val sway = sin(u * (1.6f + k) + t * (0.35f + 0.2f * k) + k * 2f) * 0.14f
            val band = 1f - smooth(0f, 0.34f, abs(y - (0.34f + 0.2f * k + sway)))
            val rays = 0.6f + 0.4f * sin(u * 22f + sin(u * 3f + t * 0.5f) * 4f + k * 4f)
            tone += band * band * rays * (0.9f - 0.3f * k)
        }
        tone.coerceIn(0f, 1f)
    }

private val waves =
    DitherField { x, y, t, aspect ->
        val u = x * aspect
        var tone = 0f
        for (layer in 0 until 4) {
            val line = 0.45f + layer * 0.14f + sin(u * (2f + layer) + t * (0.5f + layer * 0.2f)) * 0.05f
            if (y > line) tone = 0.25f + layer * 0.2f
        }
        tone
    }

private val ripples =
    DitherField { x, y, t, aspect ->
        var sum = 0f
        for (i in 0 until 3) {
            val cx = (0.2f + 0.3f * i) * aspect
            val cy = 0.3f + 0.4f * ((i + 1) % 2)
            val d = hypot(x * aspect - cx, y - cy)
            sum += sin(d * 28f - t * 3f) * (1f / (1f + d * 4f))
        }
        0.5f + sum * 0.4f
    }

private val metaballs =
    DitherField { x, y, t, aspect ->
        var energy = 0f
        for (i in 0 until 4) {
            val bx = (0.5f + 0.3f * sin(t * (0.5f + i * 0.17f) + i * 1.7f)) * aspect
            val by = 0.5f + 0.3f * cos(t * (0.4f + i * 0.13f) + i * 2.3f)
            val d2 = (x * aspect - bx) * (x * aspect - bx) + (y - by) * (y - by)
            energy += 0.03f / (d2 + 0.004f)
        }
        // A soft rim around the merged shape reads as a glow before the solid core.
        smooth(0.6f, 2.2f, energy)
    }

private val tunnel =
    DitherField { x, y, t, aspect ->
        val r = max(hypot((x - 0.5f) * aspect, y - 0.5f), 0.02f)
        val rings = 0.5f + 0.5f * sin(6f / r - t * 3f)
        rings * smooth(0.02f, 0.35f, r)
    }

private val retroGrid =
    DitherField { x, y, t, aspect ->
        val horizon = 0.55f
        if (y < horizon) {
            val d = hypot((x - 0.5f) * aspect, y - 0.3f)
            val sun = 1f - smooth(0.18f, 0.2f, d)
            // Dark stripes cut through the lower half of the sun.
            val cut = if (y > 0.3f && fract(y * 38f) < (y - 0.3f) * 3f) 0f else 1f
            sun * cut * 0.9f
        } else {
            val depth = 1f / (y - horizon + 0.02f)
            val lineX = abs(fract((x - 0.5f) * aspect * depth * 0.5f) - 0.5f)
            val lineZ = abs(fract(depth * 0.35f + t * 0.4f) - 0.5f)
            val line = max(1f - smooth(0f, 0.12f * depth * 0.5f, lineX), 1f - smooth(0f, 0.16f, lineZ))
            line * (1f - smooth(0f, 1f, 1f - (y - horizon) * 2.2f))
        }
    }

private val eclipse =
    DitherField { x, y, t, aspect ->
        val d = hypot((x - 0.5f) * aspect, y - 0.5f)
        val radius = 0.22f
        if (d < radius) {
            0f
        } else {
            val corona = (1f - smooth(radius, radius + 0.3f + 0.02f * sin(t * 1.5f), d))
            corona * corona * (0.5f + 0.5f * smooth(radius, radius + 0.02f, d).let { 1f - it })
        }
    }

private val starfield =
    DitherField { x, y, t, aspect ->
        var tone = 0f
        for (layer in 1..3) {
            val scale = 14f * layer
            val gx = x * aspect * scale
            val gy = y * scale + t * 0.3f * layer
            val cx = floor(gx).toInt()
            val cy = floor(gy).toInt()
            if (hash(cx, cy + layer * 97) > 0.9f) {
                val d = hypot(fract(gx) - 0.5f, fract(gy) - 0.5f)
                val twinkle = 0.6f + 0.4f * sin(t * 3f + hash(cx, cy) * 40f)
                tone = max(tone, (1f - smooth(0f, 0.5f, d)) * twinkle)
            }
        }
        tone
    }

private val contours =
    DitherField { x, y, t, aspect ->
        val u = x * aspect * 3f
        val v = y * 3f
        val height = sin(u + t * 0.2f) * cos(v * 1.3f - t * 0.15f) + 0.5f * sin(u * 2.1f + v * 1.7f)
        val band = abs(fract(height * 3f) - 0.5f)
        1f - smooth(0f, 0.08f, band)
    }

private val halftone =
    DitherField { x, y, t, aspect ->
        val cell = 16f
        val gx = x * aspect * cell
        val gy = y * cell
        val d = hypot(fract(gx) - 0.5f, fract(gy) - 0.5f)
        val cx = (floor(gx) + 0.5f) / (cell * aspect)
        val cy = (floor(gy) + 0.5f) / cell
        val level = 0.5f + 0.5f * sin((cx * aspect + cy) * 2.2f + t * 0.6f)
        1f - smooth(level * 0.55f - 0.04f, level * 0.55f + 0.04f, d)
    }

private val beams =
    DitherField { x, y, t, aspect ->
        val angle = atan2(y + 0.2f, (x - 0.5f) * aspect)
        val rays = 0.5f + 0.5f * sin(angle * 9f + t * 0.8f)
        val fade = 1f - min(1f, hypot((x - 0.5f) * aspect, y + 0.2f) * 1.1f)
        rays * rays * fade
    }

private fun fieldOf(scene: Scene): DitherField =
    when (scene) {
        Scene.Glow -> glow
        Scene.Aurora -> aurora
        Scene.Waves -> waves
        Scene.Ripples -> ripples
        Scene.Metaballs -> metaballs
        Scene.Tunnel -> tunnel
        Scene.RetroGrid -> retroGrid
        Scene.Eclipse -> eclipse
        Scene.Starfield -> starfield
        Scene.Contours -> contours
        Scene.Halftone -> halftone
        Scene.Beams -> beams
    }

@Composable
fun ScenesExample(
    scene: Scene,
    theme: StippleTheme,
    pattern: DitherPattern,
    animated: Boolean,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(320.dp)
            .dither(fieldOf(scene), pattern, theme, animated = animated, stillTime = 2f),
    )
}
