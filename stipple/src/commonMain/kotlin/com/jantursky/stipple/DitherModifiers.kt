package com.jantursky.stipple

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import com.jantursky.stipple.internal.DitherElement

/**
 * Fills the element with a dither, behind its content.
 *
 * Every cell takes its tone from [field], and [pattern] decides which cells become ink. Colors and cell size come
 * from [theme], or from [LocalStippleTheme] when it is null.
 *
 * With [animated] the field's time advances in the draw phase only: a frame redraws the element and never recomposes
 * anything. When reduced motion is on (see [LocalStippleMotion]) the dither shows the still frame at [stillTime]
 * instead. Without [animated] it always shows that frame.
 *
 * Remember a [field] that is expensive to build, because a new instance counts as a change.
 *
 * @param shape Clips the dither. The element itself isn't clipped.
 * @param speed Multiplies the passing of time. 2 plays twice as fast.
 * @param stillTime The time, in seconds, the field is drawn at when it doesn't animate. Pick a moment that looks finished.
 */
public fun Modifier.dither(
    field: DitherField,
    pattern: DitherPattern = DitherPatterns.Bayer4,
    theme: StippleTheme? = null,
    shape: Shape = RectangleShape,
    animated: Boolean = false,
    speed: Float = 1f,
    stillTime: Float = 0f,
): Modifier = this then DitherElement(field, pattern, theme, shape, animated, speed, stillTime)
