package com.jantursky.stipple.internal

import com.jantursky.stipple.DitherField
import com.jantursky.stipple.ShaderField

/** A field together with the shader that computes the same tones. */
internal class ShaderBacked(
    private val field: DitherField,
    override val shaderSource: String,
) : ShaderField,
    DitherField by field

/**
 * Implemented by shader fields whose values change while their source does not. The values go to the shader as the
 * `params` uniform array, and the source reads `params[0]`, `params[1]` and so on, so a moving value never compiles a
 * new program. The size of the array must not change.
 */
internal interface ShaderParams {
    fun shaderParams(): FloatArray
}

/** A float literal the shading language accepts. */
internal fun glsl(value: Float): String {
    val text = value.toString()
    return if ('.' in text || 'E' in text || 'N' in text || 'I' in text) text else "$text.0"
}
