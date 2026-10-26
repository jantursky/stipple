package com.jantursky.stipple

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Color
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.jetbrains.skia.Surface
import kotlin.test.Test
import kotlin.test.assertTrue

class ShaderSpikeTest {
    @Test
    fun skslRuntimeEffectCompilesAndDraws() {
        val sksl =
            """
            uniform float2 size;
            half4 main(float2 p) {
                float2 uv = p / size;
                return half4(uv.x, 0.0, 0.0, 1.0);
            }
            """.trimIndent()
        val effect = RuntimeEffect.makeForShader(sksl)
        val builder = RuntimeShaderBuilder(effect)
        builder.uniform("size", 10f, 10f)
        val surface = Surface.makeRaster(ImageInfo.makeN32Premul(10, 10))
        surface.canvas.drawPaint(Paint().apply { shader = builder.makeShader() })
        val bitmap = Bitmap().apply { allocPixels(ImageInfo.makeN32Premul(10, 10)) }
        surface.readPixels(bitmap, 0, 0)
        val left = Color.getR(bitmap.getColor(0, 5))
        val right = Color.getR(bitmap.getColor(9, 5))
        assertTrue(right > left, "red ramps left to right: $left < $right")
    }
}
