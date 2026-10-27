package com.jantursky.stipple

import com.jantursky.stipple.internal.ShaderParams
import com.jantursky.stipple.internal.glsl
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

/** The most rows or columns a mesh can have. It bounds the uniform array and the per-pixel cost of the shader. */
internal const val MAX_MESH_SIDE = 8

/**
 * A grid of tones blended smoothly across the element. The control points sit on a regular grid with the first and
 * last at the edges; each grid cell blends its four corners with smoothstep weights, so the surface is continuous,
 * flat at the control points and never leaves the range of the control tones.
 *
 * With [drift] above 0 the sample position is pushed around by a bounded sine that vanishes at the element's edge,
 * which moves the interior control points and keeps the edge ones where they are.
 */
internal class MeshField(
    tones: List<List<Float>>,
    private val drift: Float,
    private val speed: Float,
) : ShaderField,
    ShaderParams {
    init {
        // Before anything reads the grid: a ragged or empty list would fail with a less helpful exception.
        val first = tones.firstOrNull()?.size ?: 0
        require(tones.size in 2..MAX_MESH_SIDE && first in 2..MAX_MESH_SIDE) {
            "A mesh needs 2 to $MAX_MESH_SIDE rows and columns of tones, got ${tones.size} rows of $first"
        }
        require(tones.all { it.size == first }) { "Every row of a mesh needs the same number of tones" }
    }

    private val rows = tones.size
    private val columns = tones[0].size
    private val values = FloatArray(rows * columns) { tones[it / columns][it % columns] }
    private val params = FloatArray(rows * columns + 2)

    override fun tone(
        x: Float,
        y: Float,
        t: Float,
        aspect: Float,
    ): Float {
        var u = x
        var v = y
        if (drift > 0f) {
            val bump = sin(PI.toFloat() * x) * sin(PI.toFloat() * y)
            val phase = t * speed
            u = (x + drift * sin(TWO_PI * (y * 1.3f + phase)) * bump).coerceIn(0f, 1f)
            v = (y + drift * sin(TWO_PI * (x * 1.1f + phase + 0.25f)) * bump).coerceIn(0f, 1f)
        }
        val gx = u * (columns - 1)
        val gy = v * (rows - 1)
        val c = floor(gx).toInt().coerceAtMost(columns - 2)
        val r = floor(gy).toInt().coerceAtMost(rows - 2)
        val fx = smooth(gx - c)
        val fy = smooth(gy - r)
        val top = lerp(values[r * columns + c], values[r * columns + c + 1], fx)
        val bottom = lerp(values[(r + 1) * columns + c], values[(r + 1) * columns + c + 1], fx)
        return lerp(top, bottom, fy)
    }

    override fun shaderParams(): FloatArray {
        values.copyInto(params)
        params[values.size] = drift
        params[values.size + 1] = speed
        return params
    }

    /** The grid size is in the text, because the unrolled lookups depend on it; the tones are in `params`. */
    override val shaderSource: String =
        buildString {
            val driftIndex = rows * columns
            appendLine("float at(float r, float c) {")
            appendLine("    float v = 0.0;")
            for (r in 0 until rows) {
                for (c in 0 until columns) {
                    val k = r * columns + c
                    appendLine(
                        "    v += params[$k] * step(abs(r - ${glsl(
                            r.toFloat(),
                        )}), 0.5) * step(abs(c - ${glsl(c.toFloat())}), 0.5);",
                    )
                }
            }
            appendLine("    return v;")
            appendLine("}")
            appendLine()
            appendLine("float tone(float2 uv, float t, float aspect) {")
            appendLine("    float u = uv.x;")
            appendLine("    float v = uv.y;")
            appendLine("    float drift = params[$driftIndex];")
            appendLine("    if (drift > 0.0) {")
            appendLine("        float bump = sin(3.1415927 * uv.x) * sin(3.1415927 * uv.y);")
            appendLine("        float phase = t * params[${driftIndex + 1}];")
            appendLine("        u = clamp(uv.x + drift * sin(6.2831855 * (uv.y * 1.3 + phase)) * bump, 0.0, 1.0);")
            appendLine(
                "        v = clamp(uv.y + drift * sin(6.2831855 * (uv.x * 1.1 + phase + 0.25)) * bump, 0.0, 1.0);",
            )
            appendLine("    }")
            appendLine("    float gx = u * ${glsl((columns - 1).toFloat())};")
            appendLine("    float gy = v * ${glsl((rows - 1).toFloat())};")
            appendLine("    float c = min(floor(gx), ${glsl((columns - 2).toFloat())});")
            appendLine("    float r = min(floor(gy), ${glsl((rows - 2).toFloat())});")
            appendLine("    float fx = (gx - c) * (gx - c) * (3.0 - 2.0 * (gx - c));")
            appendLine("    float fy = (gy - r) * (gy - r) * (3.0 - 2.0 * (gy - r));")
            appendLine("    float top = mix(at(r, c), at(r, c + 1.0), fx);")
            appendLine("    float bottom = mix(at(r + 1.0, c), at(r + 1.0, c + 1.0), fx);")
            appendLine("    return mix(top, bottom, fy);")
            appendLine("}")
        }

    private companion object {
        const val TWO_PI = 2f * PI.toFloat()

        fun smooth(k: Float): Float = k * k * (3f - 2f * k)

        fun lerp(
            a: Float,
            b: Float,
            k: Float,
        ): Float = a + (b - a) * k
    }
}
