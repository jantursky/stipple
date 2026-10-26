package com.jantursky.stipple.internal

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

internal actual fun createShaderPainter(): ShaderPainter? = SkiaShaderPainter()

/** Draws with a Skia runtime effect, which desktop, iOS and web all have. */
private class SkiaShaderPainter : ShaderPainter {
    private val effects = HashMap<String, RuntimeEffect>()

    override fun draw(
        scope: DrawScope,
        program: String,
        uniforms: ShaderUniforms,
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
            builder.uniform("ink", uniforms.ink.red, uniforms.ink.green, uniforms.ink.blue, uniforms.ink.alpha)
            builder.uniform(
                "paper",
                uniforms.paper.red,
                uniforms.paper.green,
                uniforms.paper.blue,
                uniforms.paper.alpha,
            )
            paint.shader = builder.makeShader()
            scope.drawIntoCanvas { it.nativeCanvas.drawRect(Rect.makeWH(width, height), paint) }
        } finally {
            paint.close()
            builder.close()
        }
    }
}
