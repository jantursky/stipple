package com.jantursky.stipple.internal

import com.jantursky.stipple.DitherField
import com.jantursky.stipple.ShaderField

/** A field together with the shader that computes the same tones. */
internal class ShaderBacked(
    private val field: DitherField,
    override val shaderSource: String,
) : ShaderField,
    DitherField by field

/** A float literal the shading language accepts. */
internal fun glsl(value: Float): String {
    val text = value.toString()
    return if ('.' in text || 'E' in text || 'N' in text || 'I' in text) text else "$text.0"
}
