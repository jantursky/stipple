package com.jantursky.stipple.internal

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.DitherPatterns
import com.jantursky.stipple.DitherRenderer
import kotlin.math.roundToInt

/** Draws a dither with one fragment shader. One painter per node, so compiled shaders are never shared by threads. */
internal interface ShaderPainter {
    /**
     * Draws over the whole scope with [blendMode], and throws if the platform's compiler rejects the program. Only
     * [BlendMode.SrcOver] and [BlendMode.SrcIn] are drawn as asked, the two the dither modes use; any other mode
     * draws as SrcOver.
     */
    fun draw(
        scope: DrawScope,
        program: String,
        uniforms: ShaderUniforms,
        blendMode: BlendMode = BlendMode.SrcOver,
    )
}

internal class ShaderUniforms(
    val time: Float,
    val cell: Float,
    /** The side of the analytic Bayer matrix, or 0 when [pattern] is read from a texture. */
    val bayer: Int,
    /** The colors from paper to ink, 2 to [SHADER_MAX_COLORS] of them. */
    val ramp: List<Color>,
    /** The values of the field's `params` array, or null when its shader declares none. */
    val params: FloatArray? = null,
    /** The pattern the program reads as a texture, or null for the analytic Bayer matrix. */
    val pattern: DitherPattern? = null,
)

/** A painter for this platform, or null when it has no runtime shaders (Android below 13). */
internal expect fun createShaderPainter(): ShaderPainter?

/** The most colors a ramp can have in the shader, and the size of its `ramp` uniform. */
internal const val SHADER_MAX_COLORS = 8

/** The largest side of a pattern the shader reads from a texture. */
internal const val SHADER_MAX_PATTERN = 256

/** The ramp as the `ramp` uniform holds it: four floats per color, the unused entries left at zero. */
internal fun shaderRamp(ramp: List<Color>): FloatArray {
    val out = FloatArray(SHADER_MAX_COLORS * 4)
    ramp.forEachIndexed { i, color ->
        out[i * 4] = color.red
        out[i * 4 + 1] = color.green
        out[i * 4 + 2] = color.blue
        out[i * 4 + 3] = color.alpha
    }
    return out
}

/** The side of the Bayer matrix the shader builds itself, or 0 for every other pattern. */
internal fun shaderBayerSide(pattern: DitherPattern): Int =
    when (pattern) {
        DitherPatterns.Bayer2 -> 2
        DitherPatterns.Bayer4 -> 4
        DitherPatterns.Bayer8 -> 8
        else -> 0
    }

/**
 * Whether the shader renderer draws this dither. [cells] is only asked for under [DitherRenderer.Auto], which
 * switches over once the cell renderer would draw too many rectangles.
 */
internal fun shaderEligibility(
    renderer: DitherRenderer,
    hasProgram: Boolean,
    broken: Boolean,
    pattern: DitherPattern,
    colors: Int,
    cells: () -> Int,
): Boolean =
    renderer != DitherRenderer.Cells &&
        hasProgram &&
        !broken &&
        pattern.size in 1..SHADER_MAX_PATTERN &&
        colors in 2..SHADER_MAX_COLORS &&
        (renderer == DitherRenderer.Shader || cells() > AUTO_SHADER_CELLS)

/**
 * A pattern as the RGBA8 texture the shader reads: the threshold times 65535 split over red (high byte) and green
 * (low byte), blue 0 and alpha 255 so no color conversion touches it.
 */
internal fun patternTexture(pattern: DitherPattern): ByteArray {
    val side = pattern.size
    val out = ByteArray(side * side * 4)
    for (y in 0 until side) {
        for (x in 0 until side) {
            val v = (pattern.threshold(x, y) * 65535f).roundToInt().coerceIn(0, 65535)
            val i = (y * side + x) * 4
            out[i] = (v shr 8).toByte()
            out[i + 1] = (v and 255).toByte()
            out[i + 2] = 0
            out[i + 3] = -1
        }
    }
    return out
}

/**
 * The whole program: the uniforms, the [field] function and the ordered dither. It decides like [quantize]. The
 * threshold comes from the Bayer matrix, or from a texture when [texture] is set; [dots] draws round cells.
 */
internal fun shaderProgram(
    field: String,
    paramCount: Int = 0,
    texture: Boolean = false,
    dots: Boolean = false,
): String {
    val threshold =
        if (texture) {
            """
            float threshold(float2 c) {
                float4 px = float4(pattern.eval(mod(c, patternSize) + 0.5)) * 255.0;
                return (floor(px.r + 0.5) * 256.0 + floor(px.g + 0.5)) / 65535.0;
            }
            """
        } else {
            """
            float step4(float q) {
                return q < 0.5 ? 0.0 : (q < 1.5 ? 2.0 : (q < 2.5 ? 3.0 : 1.0));
            }

            float quadrant(float2 c, float s) {
                return step4(mod(floor(c.y / s), 2.0) * 2.0 + mod(floor(c.x / s), 2.0));
            }

            float threshold(float2 c) {
                float v = quadrant(c, 1.0);
                if (bayer > 2.5) v = v * 4.0 + quadrant(c, 2.0);
                if (bayer > 4.5) v = v * 4.0 + quadrant(c, 4.0);
                return (v + 0.5) / (bayer * bayer);
            }
            """
        }
    val shape =
        if (dots) {
            """
            float2 origin = c * cell;
            float radius = min(cell, min(size.x - origin.x, size.y - origin.y)) * 0.5;
            float coverage = 1.0 - smoothstep(radius - 0.5, radius + 0.5, length(frag - origin - cell * 0.5));
            float4 color = mix(ramp[0], pick(level), level > 0.5 ? coverage : 0.0);
            """
        } else {
            "float4 color = pick(level);"
        }
    return """
        uniform float2 size;
        uniform float time;
        uniform float cell;
        uniform float bayer;
        uniform float levels;
        uniform float4 ramp[$SHADER_MAX_COLORS];
        ${if (texture) "uniform shader pattern;\n        uniform float patternSize;" else ""}
        ${if (paramCount > 0) "uniform float params[$paramCount];" else ""}

        $field

        float4 pick(float level) {
            float4 color = ramp[0];
            for (int i = 1; i < $SHADER_MAX_COLORS; i++) {
                if (float(i) == level) color = ramp[i];
            }
            return color;
        }

        $threshold

        half4 main(float2 frag) {
            float2 c = floor(frag / cell);
            float2 uv = min((c + 0.5) * cell / size, float2(1.0));
            float tn = clamp(tone(uv, time, size.x / size.y), 0.0, 1.0);
            float top = levels - 1.0;
            float position = tn * top;
            float base = floor(position);
            float level = base >= top ? top : (position - base > threshold(c) ? base + 1.0 : base);
            $shape
            return half4(half3(color.rgb * color.a), half(color.a));
        }
        """.trimIndent()
}
