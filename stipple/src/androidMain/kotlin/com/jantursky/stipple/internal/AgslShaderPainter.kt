package com.jantursky.stipple.internal

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope

internal actual fun createShaderPainter(): ShaderPainter? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) AgslShaderPainter() else null

/** Draws with an AGSL runtime shader, which Android has from 13 (API 33). */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class AgslShaderPainter : ShaderPainter {
    private val shaders = HashMap<String, RuntimeShader>()

    override fun draw(
        scope: DrawScope,
        program: String,
        uniforms: ShaderUniforms,
        blendMode: BlendMode,
    ) {
        val shader = shaders.getOrPut(program) { RuntimeShader(program) }
        shader.setFloatUniform("size", scope.size.width, scope.size.height)
        shader.setFloatUniform("time", uniforms.time)
        shader.setFloatUniform("cell", uniforms.cell)
        shader.setFloatUniform("bayer", uniforms.bayer.toFloat())
        shader.setFloatUniform("ink", uniforms.ink.red, uniforms.ink.green, uniforms.ink.blue, uniforms.ink.alpha)
        shader.setFloatUniform(
            "paper",
            uniforms.paper.red,
            uniforms.paper.green,
            uniforms.paper.blue,
            uniforms.paper.alpha,
        )
        uniforms.params?.let { shader.setFloatUniform("params", it) }
        scope.drawRect(ShaderBrush(shader), blendMode = blendMode)
    }
}
