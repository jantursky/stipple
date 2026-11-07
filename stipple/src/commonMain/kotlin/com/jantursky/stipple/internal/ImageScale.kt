package com.jantursky.stipple.internal

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/** This bitmap drawn into a new one of [width] x [height], or itself when the size already fits. */
internal fun ImageBitmap.scaledTo(
    width: Int,
    height: Int,
): ImageBitmap {
    if (width == this.width && height == this.height) return this
    val out = ImageBitmap(width, height)
    val paint = Paint().apply { filterQuality = FilterQuality.Medium }
    Canvas(out).drawImageRect(
        this,
        IntOffset.Zero,
        IntSize(this.width, this.height),
        IntOffset.Zero,
        IntSize(width, height),
        paint,
    )
    return out
}

/**
 * The ARGB pixels of this bitmap at [width] x [height]. The canvas path scales before the readback, so the buffer
 * stays small. Android hands out hardware bitmaps that a software canvas refuses to draw, so there it falls back to
 * reading every pixel and averaging them down on the CPU.
 */
internal fun ImageBitmap.readPixelsScaled(
    width: Int,
    height: Int,
): IntArray {
    val pixels = IntArray(width * height)
    try {
        scaledTo(width, height).readPixels(pixels, 0, 0, width, height)
    } catch (_: IllegalArgumentException) {
        val full = IntArray(this.width * this.height)
        readPixels(full, 0, 0, this.width, this.height)
        return boxDownscale(full, this.width, this.height, width, height)
    }
    return pixels
}

/** Averages [source] down to [width] x [height], weighting colors by opacity so see-through pixels add no tint. */
internal fun boxDownscale(
    source: IntArray,
    sourceWidth: Int,
    sourceHeight: Int,
    width: Int,
    height: Int,
): IntArray {
    if (width == sourceWidth && height == sourceHeight) return source
    val out = IntArray(width * height)
    for (y in 0 until height) {
        val y0 = y * sourceHeight / height
        val y1 = maxOf(y0 + 1, (y + 1) * sourceHeight / height).coerceAtMost(sourceHeight)
        for (x in 0 until width) {
            val x0 = x * sourceWidth / width
            val x1 = maxOf(x0 + 1, (x + 1) * sourceWidth / width).coerceAtMost(sourceWidth)
            var a = 0L
            var r = 0L
            var g = 0L
            var b = 0L
            for (sy in y0 until y1) {
                for (sx in x0 until x1) {
                    val p = source[sy * sourceWidth + sx]
                    val pa = p ushr 24
                    a += pa
                    r += ((p shr 16) and 0xFF) * pa
                    g += ((p shr 8) and 0xFF) * pa
                    b += (p and 0xFF) * pa
                }
            }
            val n = ((x1 - x0) * (y1 - y0)).toLong()
            out[y * width + x] =
                if (a == 0L) {
                    0
                } else {
                    ((a / n).toInt() shl 24) or ((r / a).toInt() shl 16) or ((g / a).toInt() shl 8) or (b / a).toInt()
                }
        }
    }
    return out
}
