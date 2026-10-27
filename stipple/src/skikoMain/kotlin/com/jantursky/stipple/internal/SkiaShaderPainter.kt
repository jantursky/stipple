package com.jantursky.stipple.internal

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.jantursky.stipple.DitherPattern
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Shader
import org.jetbrains.skia.BlendMode as SkiaBlendMode

internal actual fun createShaderPainter(): ShaderPainter? = SkiaShaderPainter()

/** Draws with a Skia runtime effect, which desktop, iOS and web all have. */
private class SkiaShaderPainter : ShaderPainter {
    private val effects = HashMap<String, RuntimeEffect>()
    private var texturePattern: DitherPattern? = null
    private var texture: Shader? = null

    /** The pattern as a repeating, nearest-sampled image shader, rebuilt only when the pattern instance changes. */
    private fun textureFor(pattern: DitherPattern): Shader {
        texture?.let { if (texturePattern === pattern) return it }
        texture?.close()
        val side = pattern.size
        val info = ImageInfo(side, side, ColorType.RGBA_8888, ColorAlphaType.OPAQUE)
        val image = Image.makeRaster(info, patternTexture(pattern), side * 4)
        val shader =
            try {
                image.makeShader(FilterTileMode.REPEAT, FilterTileMode.REPEAT, SamplingMode.DEFAULT)
            } finally {
                image.close()
            }
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
        val effect = effects.getOrPut(program) { RuntimeEffect.makeForShader(program) }
        val width = scope.size.width
        val height = scope.size.height
        val builder = RuntimeShaderBuilder(effect)
        val paint = Paint()
        try {
            builder.uniform("size", width, height)
            builder.uniform("time", uniforms.time)
            builder.uniform("cell", uniforms.cell)
            builder.uniform("bayer", uniforms.bayer.toFloat())
            builder.uniform("levels", uniforms.ramp.size.toFloat())
            builder.uniform("ramp", shaderRamp(uniforms.ramp))
            uniforms.pattern?.let { pattern ->
                builder.uniform("patternSize", pattern.size.toFloat())
                builder.child("pattern", textureFor(pattern))
            }
            uniforms.params?.let { builder.uniform("params", it) }
            paint.shader = builder.makeShader()
            paint.blendMode = if (blendMode == BlendMode.SrcIn) SkiaBlendMode.SRC_IN else SkiaBlendMode.SRC_OVER
            scope.drawIntoCanvas { it.nativeCanvas.drawRect(Rect.makeWH(width, height), paint) }
        } finally {
            paint.close()
            builder.close()
        }
    }
}
