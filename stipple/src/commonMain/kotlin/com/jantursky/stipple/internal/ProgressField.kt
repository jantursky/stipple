package com.jantursky.stipple.internal

import com.jantursky.stipple.DitherField
import com.jantursky.stipple.ProgressDirection

/**
 * Tone 1 up to the progress edge and [track] beyond it, with a dithered ramp [softness] wide in between. The
 * progress is read once a frame, in the draw phase, so a changing value redraws and never recomposes.
 */
internal class ProgressField(
    private val progress: () -> Float,
    private val direction: ProgressDirection,
    private val softness: Float,
    private val track: Float,
) : DitherField,
    FrameAware {
    private var edge = 0f

    override fun beginFrame() {
        edge = progress().coerceIn(0f, 1f) * (1f + softness) - softness
    }

    override fun tone(
        x: Float,
        y: Float,
        t: Float,
        aspect: Float,
    ): Float {
        val along =
            when (direction) {
                ProgressDirection.Right -> x
                ProgressDirection.Left -> 1f - x
                ProgressDirection.Down -> y
                ProgressDirection.Up -> 1f - y
            }
        val filled = 1f - ((along - edge) / softness).coerceIn(0f, 1f)
        return track + (1f - track) * filled
    }
}
