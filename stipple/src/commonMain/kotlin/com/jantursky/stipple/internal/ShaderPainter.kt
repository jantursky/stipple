package com.jantursky.stipple.internal

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.DitherPatterns

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
    val bayer: Int,
    val ink: Color,
    val paper: Color,
    /** The values of the field's `params` array, or null when its shader declares none. */
    val params: FloatArray? = null,
)

/** A painter for this platform, or null when it has no runtime shaders (Android below 13). */
internal expect fun createShaderPainter(): ShaderPainter?

/** The side of the Bayer matrix the shader can build, or 0 for patterns it can't (blue noise, custom ones). */
internal fun shaderBayerSide(pattern: DitherPattern): Int =
    when (pattern) {
        DitherPatterns.Bayer2 -> 2
        DitherPatterns.Bayer4 -> 4
        DitherPatterns.Bayer8 -> 8
        else -> 0
    }

/**
 * The whole program: the uniforms, the [field] function and the ordered dither. It decides like
 * [quantize]: a cell is ink when the tone is above the Bayer threshold, and a tone of 1 is always ink.
 */
internal fun shaderProgram(
    field: String,
    paramCount: Int = 0,
): String =
    """
    uniform float2 size;
    uniform float time;
    uniform float cell;
    uniform float bayer;
    uniform float4 ink;
    uniform float4 paper;
    ${if (paramCount > 0) "uniform float params[$paramCount];" else ""}

    $field

    float step4(float q) {
        return q < 0.5 ? 0.0 : (q < 1.5 ? 2.0 : (q < 2.5 ? 3.0 : 1.0));
    }

    float quadrant(float2 c, float s) {
        return step4(mod(floor(c.y / s), 2.0) * 2.0 + mod(floor(c.x / s), 2.0));
    }

    float rank(float2 c) {
        float v = quadrant(c, 1.0);
        if (bayer > 2.5) v = v * 4.0 + quadrant(c, 2.0);
        if (bayer > 4.5) v = v * 4.0 + quadrant(c, 4.0);
        return v;
    }

    half4 main(float2 frag) {
        float2 c = floor(frag / cell);
        float2 uv = min((c + 0.5) * cell / size, float2(1.0));
        float tn = clamp(tone(uv, time, size.x / size.y), 0.0, 1.0);
        float threshold = (rank(c) + 0.5) / (bayer * bayer);
        float4 color = (tn >= 1.0 || tn > threshold) ? ink : paper;
        return half4(half3(color.rgb * color.a), half(color.a));
    }
    """.trimIndent()
