package com.jantursky.stipple

import com.jantursky.stipple.internal.ShaderParams
import com.jantursky.stipple.internal.shaderProgram
import org.jetbrains.skia.RuntimeEffect
import kotlin.test.Test

/**
 * Compiles the program of every built-in shader field with Skia's own compiler. The agreement tests cannot tell a
 * shader that drew from one that failed to compile and fell back to cells, because both look the same.
 */
class ShaderProgramsCompileTest {
    private fun compiles(
        name: String,
        field: DitherField,
    ) {
        val shaderField = field as? ShaderField ?: error("$name is not a shader field")
        val params = (field as? ShaderParams)?.shaderParams()?.size ?: 0
        val program = shaderProgram(shaderField.shaderSource, params)
        // Throws, with the compiler's message, when the program is rejected.
        val effect = RuntimeEffect.makeForShader(program)
        effect.close()
    }

    @Test
    fun everyBuiltInShaderFieldCompiles() {
        val fields =
            mapOf(
                "solid" to DitherFields.solid(0.4f),
                "linear" to DitherFields.linear(0.1f, 0.9f, 30f),
                "radial" to DitherFields.radial(),
                "noise" to DitherFields.noise(),
                "waves" to DitherFields.waves(),
                "ripples" to DitherFields.ripples(),
                "sweep" to DitherFields.sweep(0.5f),
                "live sweep" to DitherFields.sweep({ 0.5f }),
                "dissolve" to DitherFields.dissolve({ 0.5f }),
                "dissolve fine" to DitherFields.dissolve({ 0.5f }, scale = 60f, softness = 0.05f),
                "mesh 2x2" to DitherFields.mesh(List(2) { List(2) { 0.5f } }),
                "mesh 3x4 drifting" to DitherFields.mesh(List(3) { r -> List(4) { c -> (r + c) / 5f } }, 0.1f),
                "mesh 8x8" to DitherFields.mesh(List(8) { r -> List(8) { c -> (r * c) / 49f } }, 0.1f),
            )
        for ((name, field) in fields) compiles(name, field)
    }
}
