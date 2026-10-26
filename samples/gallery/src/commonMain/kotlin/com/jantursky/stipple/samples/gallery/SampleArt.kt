package com.jantursky.stipple.samples.gallery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter

/** A drawn landscape, so the gallery needs no image files: a sky, a sun and three ridges. */
class LandscapePainter : Painter() {
    override val intrinsicSize: Size = Size.Unspecified

    override fun DrawScope.onDraw() = drawLandscape()
}

/** The landscape as content, for modifiers that dither whatever an element draws. */
@Composable
fun Landscape(modifier: Modifier = Modifier) {
    Box(modifier) { Canvas(Modifier.fillMaxSize()) { drawLandscape() } }
}

private fun DrawScope.drawLandscape() {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(Color(0xFF1B1F3B), Color(0xFFF5C27A), Color(0xFFFFF1CF)), 0f, h * 0.7f))
    drawCircle(Color(0xFFFFF8E5), radius = h * 0.17f, center = Offset(w * 0.68f, h * 0.42f))
    ridge(h * 0.62f, h * 0.12f, 0.9f, Color(0xFF6B5B95))
    ridge(h * 0.72f, h * 0.14f, 1.7f, Color(0xFF3B3560))
    ridge(h * 0.84f, h * 0.10f, 2.6f, Color(0xFF14122B))
}

private fun DrawScope.ridge(
    base: Float,
    amplitude: Float,
    phase: Float,
    color: Color,
) {
    val path =
        Path().apply {
            moveTo(0f, size.height)
            lineTo(0f, base)
            val steps = 24
            for (i in 1..steps) {
                val x = size.width * i / steps
                val wave = kotlin.math.sin(i * 0.55f + phase) * 0.6f + kotlin.math.sin(i * 1.3f + phase * 2f) * 0.4f
                lineTo(x, base - wave * amplitude)
            }
            lineTo(size.width, size.height)
            close()
        }
    drawPath(path, color)
}
