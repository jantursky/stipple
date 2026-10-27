package com.jantursky.stipple

import com.jantursky.stipple.internal.ShaderBacked
import com.jantursky.stipple.internal.ShaderParams
import com.jantursky.stipple.internal.glsl
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin

/** Ready-made [DitherField]s. All of them are pure functions of position and time. */
public object DitherFields {
    /** The same [tone] everywhere. */
    public fun solid(tone: Float = 1f): DitherField =
        ShaderBacked(
            { _, _, _, _ -> tone },
            "float tone(float2 uv, float t, float aspect) { return ${glsl(tone)}; }",
        )

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
        return ShaderBacked(
            { x, y, _, aspect ->
                val px = (x - 0.5f) * aspect
                val py = y - 0.5f
                val reach = (abs(dx) * aspect + abs(dy)) / 2f
                val along = (px * dx + py * dy) / reach / 2f + 0.5f
                from + (to - from) * along.coerceIn(0f, 1f)
            },
            """
            float tone(float2 uv, float t, float aspect) {
                float2 d = float2(${glsl(dx)}, ${glsl(dy)});
                float2 p = float2((uv.x - 0.5) * aspect, uv.y - 0.5);
                float reach = (abs(d.x) * aspect + abs(d.y)) / 2.0;
                float along = dot(p, d) / reach / 2.0 + 0.5;
                return ${glsl(from)} + (${glsl(to)} - ${glsl(from)}) * clamp(along, 0.0, 1.0);
            }
            """.trimIndent(),
        )
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
        ShaderBacked(
            { x, y, _, aspect ->
                val d = hypot((x - centerX) * aspect, y - centerY) / radius
                inner + (outer - inner) * d.coerceIn(0f, 1f)
            },
            """
            float tone(float2 uv, float t, float aspect) {
                float d = length(float2((uv.x - ${glsl(centerX)}) * aspect, uv.y - ${glsl(centerY)})) / ${glsl(radius)};
                return ${glsl(inner)} + (${glsl(outer)} - ${glsl(inner)}) * clamp(d, 0.0, 1.0);
            }
            """.trimIndent(),
        )

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
        ShaderBacked(
            { x, y, t, aspect ->
                val n = valueNoise(x * aspect * scale + t * speed * 3f, y * scale - t * speed * 2f, t * speed)
                level + (n - 0.5f) * contrast
            },
            """
            $NOISE_SHADER

            float tone(float2 uv, float t, float aspect) {
                float n = valueNoise(float3(
                    uv.x * aspect * ${glsl(scale)} + t * ${glsl(speed)} * 3.0,
                    uv.y * ${glsl(scale)} - t * ${glsl(speed)} * 2.0,
                    t * ${glsl(speed)}));
                return ${glsl(level)} + (n - 0.5) * ${glsl(contrast)};
            }
            """.trimIndent(),
        )

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
        return ShaderBacked(
            { x, y, t, aspect ->
                val phase = (x * aspect * dx + y * dy) * count - t * speed
                low + (high - low) * (0.5f + 0.5f * sin(phase * 2f * PI.toFloat()))
            },
            """
            float tone(float2 uv, float t, float aspect) {
                float phase = (uv.x * aspect * ${glsl(dx)} + uv.y * ${glsl(dy)}) * ${glsl(count)} - t * ${glsl(speed)};
                return ${glsl(low)} + (${glsl(high)} - ${glsl(low)}) * (0.5 + 0.5 * sin(phase * 6.2831855));
            }
            """.trimIndent(),
        )
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
        ShaderBacked(
            { x, y, t, aspect ->
                val d = hypot((x - centerX) * aspect, y - centerY) / radius
                val wave = 0.5f + 0.5f * cos((d * rings - t * speed) * 2f * PI.toFloat())
                wave * (1f - d.coerceIn(0f, 1f))
            },
            """
            float tone(float2 uv, float t, float aspect) {
                float d = length(float2((uv.x - ${glsl(centerX)}) * aspect, uv.y - ${glsl(centerY)})) / ${glsl(radius)};
                float wave = 0.5 + 0.5 * cos((d * ${glsl(rings)} - t * ${glsl(speed)}) * 6.2831855);
                return wave * (1.0 - clamp(d, 0.0, 1.0));
            }
            """.trimIndent(),
        )

    /**
     * A reveal in an irregular order instead of a straight edge: tone 0 everywhere at a [progress] of 0, tone 1
     * everywhere at 1, and in between each part of the element turns on when the progress passes its own random
     * threshold, so cells light up in patches that grow together. Use it with [ditherMask] to make content appear or
     * vanish like grain, and run [progress] backwards to dissolve it away.
     *
     * [progress] is read every time the field is drawn: pass `{ animated.value }` and a change redraws the element
     * without recomposing it. A shader draws it from one compiled program however the progress moves.
     *
     * @param scale How many patches of the noise fit across the element's height; more is finer grain.
     * @param softness How wide the band is, in progress, over which a patch goes from paper to ink; the pattern
     *   dithers inside it.
     */
    public fun dissolve(
        progress: () -> Float,
        scale: Float = 24f,
        softness: Float = 0.25f,
    ): DitherField = DissolveField(progress, scale, softness.coerceAtLeast(0.01f))

    /**
     * A mesh gradient: a grid of tones blended smoothly, for a soft, organic surface with several highlights. [tones]
     * has one list per row, top to bottom, and each row has one tone per column, left to right; 2 to 8 rows and
     * columns. The tone at a control point is exactly its value and nothing goes outside the range of [tones].
     *
     * A [drift] above 0 (a fraction of the element, 0.05 to 0.15 reads well) lets the interior points wander at
     * [speed] cycles per second while the edges stay put; it needs `animated` on the modifier to move. The field is a
     * shader field, and changing [tones] or [drift] does not compile a new program.
     *
     * @throws IllegalArgumentException when the grid is smaller than 2x2, larger than 8x8 or ragged.
     */
    public fun mesh(
        tones: List<List<Float>>,
        drift: Float = 0f,
        speed: Float = 0.2f,
    ): DitherField = MeshField(tones, drift, speed)

    /**
     * A soft edge that sweeps from the left to the right as [progress] goes from 0 to 1: tone 1 behind the edge, 0
     * ahead of it. [softness] is the width of the edge as a fraction of the element's width.
     */
    public fun sweep(
        progress: Float,
        softness: Float = 0.15f,
    ): DitherField = SweepField({ progress }, softness)

    /**
     * The same sweep, with [progress] read every time the field is drawn: pass `{ animated.value }` and a change
     * redraws the element without recomposing it. A shader draws it from one compiled program however the progress
     * moves.
     */
    public fun sweep(
        progress: () -> Float,
        softness: Float = 0.15f,
    ): DitherField = SweepField(progress, softness)
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

/**
 * A hash of three integers, in 0..1, made of float arithmetic only so that the shader computes the same value (a
 * shader has no reliable 32-bit integer overflow). After Dave Hoskins' "Hash without Sine".
 */
internal fun hash01(
    x: Int,
    y: Int,
    z: Int,
): Float {
    var a = fract(x * 0.1031f)
    var b = fract(y * 0.1031f)
    var c = fract(z * 0.1031f)
    val d = a * (c + 31.32f) + b * (b + 31.32f) + c * (a + 31.32f)
    a += d
    b += d
    c += d
    return fract((a + b) * c)
}

private fun fract(v: Float): Float = v - floor(v)

/** The same value noise as [valueNoise], for a shader: `valueNoise(float3)` returns 0..1. */
internal const val NOISE_SHADER: String = """
float hash01(float3 p) {
    float3 q = fract(p * 0.1031);
    q += dot(q, q.zyx + 31.32);
    return fract((q.x + q.y) * q.z);
}

float valueNoise(float3 p) {
    float3 i = floor(p);
    float3 f = p - i;
    float3 w = f * f * (3.0 - 2.0 * f);
    float front = mix(
        mix(hash01(i), hash01(i + float3(1.0, 0.0, 0.0)), w.x),
        mix(hash01(i + float3(0.0, 1.0, 0.0)), hash01(i + float3(1.0, 1.0, 0.0)), w.x),
        w.y);
    float back = mix(
        mix(hash01(i + float3(0.0, 0.0, 1.0)), hash01(i + float3(1.0, 0.0, 1.0)), w.x),
        mix(hash01(i + float3(0.0, 1.0, 1.0)), hash01(i + float3(1.0, 1.0, 1.0)), w.x),
        w.y);
    return mix(front, back, w.z);
}
"""

/** A soft edge sweeping from the left; [progress] is read per draw and reaches the shader as a uniform. */
internal class SweepField(
    private val progress: () -> Float,
    private val softness: Float,
) : ShaderField,
    ShaderParams {
    private val params = FloatArray(2)

    override fun tone(
        x: Float,
        y: Float,
        t: Float,
        aspect: Float,
    ): Float {
        val edge = progress().coerceIn(0f, 1f) * (1f + softness) - softness
        return 1f - ((x - edge) / softness).coerceIn(0f, 1f)
    }

    override fun shaderParams(): FloatArray {
        params[0] = progress().coerceIn(0f, 1f)
        params[1] = softness
        return params
    }

    override val shaderSource: String =
        """
        float tone(float2 uv, float t, float aspect) {
            float edge = params[0] * (1.0 + params[1]) - params[1];
            return 1.0 - clamp((uv.x - edge) / params[1], 0.0, 1.0);
        }
        """.trimIndent()
}

/** The noise threshold of one point, stretched so that the thresholds reach both 0 and 1. */
private fun dissolveThreshold(
    x: Float,
    y: Float,
    aspect: Float,
    scale: Float,
): Float = (0.5f + (valueNoise(x * aspect * scale, y * scale, 0f) - 0.5f) * DISSOLVE_CONTRAST).coerceIn(0f, 1f)

private const val DISSOLVE_CONTRAST = 1.6f

/** A reveal through noise thresholds; [progress] is read per draw and reaches the shader as a uniform. */
internal class DissolveField(
    private val progress: () -> Float,
    private val scale: Float,
    private val softness: Float,
) : ShaderField,
    ShaderParams {
    private val params = FloatArray(1)

    override fun tone(
        x: Float,
        y: Float,
        t: Float,
        aspect: Float,
    ): Float {
        val p = progress().coerceIn(0f, 1f)
        return ((p * (1f + softness) - dissolveThreshold(x, y, aspect, scale)) / softness).coerceIn(0f, 1f)
    }

    override fun shaderParams(): FloatArray {
        params[0] = progress().coerceIn(0f, 1f)
        return params
    }

    override val shaderSource: String =
        """
        $NOISE_SHADER

        float tone(float2 uv, float t, float aspect) {
            float n = valueNoise(float3(uv.x * aspect * ${glsl(scale)}, uv.y * ${glsl(scale)}, 0.0));
            float k = clamp(0.5 + (n - 0.5) * ${glsl(DISSOLVE_CONTRAST)}, 0.0, 1.0);
            return clamp((params[0] * (1.0 + ${glsl(softness)}) - k) / ${glsl(softness)}, 0.0, 1.0);
        }
        """.trimIndent()
}
