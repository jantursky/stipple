package com.jantursky.stipple.internal

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.jantursky.stipple.DitherPattern

internal actual fun createShaderPainter(): ShaderPainter? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) AgslShaderPainter() else null

/** Draws with an AGSL runtime shader, which Android has from 13 (API 33). */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class AgslShaderPainter : ShaderPainter {
    private val shaders = HashMap<String, RuntimeShader>()
    private var texturePattern: DitherPattern? = null
    private var texture: BitmapShader? = null

    /** The pattern as a repeating, nearest-sampled bitmap shader, rebuilt only when the pattern instance changes. */
    private fun textureFor(pattern: DitherPattern): BitmapShader {
        texture?.let { if (texturePattern === pattern) return it }
        val side = pattern.size
        val bytes = patternTexture(pattern)
        val pixels =
            IntArray(side * side) {
                val i = it * 4
                (0xFF shl 24) or ((bytes[i].toInt() and 0xFF) shl 16) or ((bytes[i + 1].toInt() and 0xFF) shl 8)
            }
        val bitmap = Bitmap.createBitmap(pixels, side, side, Bitmap.Config.ARGB_8888)
        val shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        shader.filterMode = BitmapShader.FILTER_MODE_NEAREST
        texturePattern = pattern
        texture = shader
        return shader
    }

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
        shader.setFloatUniform("levels", uniforms.ramp.size.toFloat())
        shader.setFloatUniform("ramp", shaderRamp(uniforms.ramp))
        uniforms.pattern?.let { pattern ->
            shader.setFloatUniform("patternSize", pattern.size.toFloat())
            shader.setInputShader("pattern", textureFor(pattern))
        }
        uniforms.params?.let { shader.setFloatUniform("params", it) }
        scope.drawRect(ShaderBrush(shader), blendMode = blendMode)
    }
}
