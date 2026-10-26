package com.jantursky.stipple

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin

/** Ready-made [DitherField]s. All of them are pure functions of position and time. */
public object DitherFields {
    /** The same [tone] everywhere. */
    public fun solid(tone: Float = 1f): DitherField = DitherField { _, _, _, _ -> tone }

    /**
     * A ramp from [from] to [to] along [angleDegrees]: 0 runs left to right, 90 runs top to bottom. The ramp spans the
     * element corner to corner, so it reaches both tones whatever the angle.
     */
    public fun linear(
        from: Float = 0f,
        to: Float = 1f,
        angleDegrees: Float = 90f,
    ): DitherField {
        val radians = angleDegrees * PI.toFloat() / 180f
        val dx = cos(radians)
        val dy = sin(radians)
        return DitherField { x, y, _, aspect ->
            val px = (x - 0.5f) * aspect
            val py = y - 0.5f
            val reach = (abs(dx) * aspect + abs(dy)) / 2f
            val along = (px * dx + py * dy) / reach / 2f + 0.5f
            from + (to - from) * along.coerceIn(0f, 1f)
        }
    }

    /**
     * A round gradient from [inner] at ([centerX], [centerY]) to [outer] at [radius]. The radius is a fraction of the
     * element's height, so the gradient stays round on wide elements.
     */
    public fun radial(
        inner: Float = 1f,
        outer: Float = 0f,
        centerX: Float = 0.5f,
        centerY: Float = 0.5f,
        radius: Float = 0.5f,
    ): DitherField =
        DitherField { x, y, _, aspect ->
            val d = hypot((x - centerX) * aspect, y - centerY) / radius
            inner + (outer - inner) * d.coerceIn(0f, 1f)
        }

    /**
     * Soft, cloudy noise around [level] that drifts over time at [speed]. [scale] is the number of blobs across the
     * element's height and [contrast] how far the tone swings around [level].
     */
    public fun noise(
        scale: Float = 3f,
        speed: Float = 0.15f,
        level: Float = 0.5f,
        contrast: Float = 1f,
    ): DitherField =
        DitherField { x, y, t, aspect ->
            val n = valueNoise(x * aspect * scale + t * speed * 3f, y * scale - t * speed * 2f, t * speed)
            level + (n - 0.5f) * contrast
        }

    /** Diagonal bands that travel across the element: [count] of them per element height, moving at [speed] cycles per second. */
    public fun waves(
        count: Float = 4f,
        speed: Float = 0.25f,
        angleDegrees: Float = 25f,
        low: Float = 0.1f,
        high: Float = 0.9f,
    ): DitherField {
        val radians = angleDegrees * PI.toFloat() / 180f
        val dx = cos(radians)
        val dy = sin(radians)
        return DitherField { x, y, t, aspect ->
            val phase = (x * aspect * dx + y * dy) * count - t * speed
            low + (high - low) * (0.5f + 0.5f * sin(phase * 2f * PI.toFloat()))
        }
    }

    /**
     * Rings that spread from ([centerX], [centerY]) and fade with distance: [rings] of them across [radius] (a
     * fraction of the element's height), travelling at [speed] cycles per second.
     */
    public fun ripples(
        rings: Float = 5f,
        speed: Float = 0.4f,
        centerX: Float = 0.5f,
        centerY: Float = 0.5f,
        radius: Float = 0.8f,
    ): DitherField =
        DitherField { x, y, t, aspect ->
            val d = hypot((x - centerX) * aspect, y - centerY) / radius
            val wave = 0.5f + 0.5f * cos((d * rings - t * speed) * 2f * PI.toFloat())
            wave * (1f - d.coerceIn(0f, 1f))
        }

    /**
     * A soft edge that sweeps from the left to the right as [progress] goes from 0 to 1: tone 1 behind the edge, 0
     * ahead of it. [softness] is the width of the edge as a fraction of the element's width.
     */
    public fun sweep(
        progress: Float,
        softness: Float = 0.15f,
    ): DitherField =
        DitherField { x, _, _, _ ->
            val edge = progress * (1f + softness) - softness
            1f - ((x - edge) / softness).coerceIn(0f, 1f)
        }
}

/** Smooth value noise in 0..1 with a time axis, made from a hash of the integer lattice. */
internal fun valueNoise(
    x: Float,
    y: Float,
    z: Float,
): Float {
    val x0 = floor(x)
    val y0 = floor(y)
    val z0 = floor(z)
    val fx = smooth(x - x0)
    val fy = smooth(y - y0)
    val fz = smooth(z - z0)
    val ix = x0.toInt()
    val iy = y0.toInt()
    val iz = z0.toInt()

    fun corner(
        dx: Int,
        dy: Int,
        dz: Int,
    ): Float = hash01(ix + dx, iy + dy, iz + dz)

    fun lerp(
        a: Float,
        b: Float,
        k: Float,
    ): Float = a + (b - a) * k
    val front =
        lerp(
            lerp(corner(0, 0, 0), corner(1, 0, 0), fx),
            lerp(corner(0, 1, 0), corner(1, 1, 0), fx),
            fy,
        )
    val back =
        lerp(
            lerp(corner(0, 0, 1), corner(1, 0, 1), fx),
            lerp(corner(0, 1, 1), corner(1, 1, 1), fx),
            fy,
        )
    return lerp(front, back, fz)
}

private fun smooth(k: Float): Float = k * k * (3f - 2f * k)

/** A hash of three integers, in 0..1. */
internal fun hash01(
    x: Int,
    y: Int,
    z: Int,
): Float {
    var h = x * 374761393 + y * 668265263 + z * 1274126177
    h = (h xor (h ushr 13)) * 1274126177
    h = h xor (h ushr 16)
    return (h and 0xFFFFFF) / 16777216f
}
