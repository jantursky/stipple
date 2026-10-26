package com.jantursky.stipple

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import com.jantursky.stipple.internal.DitherElement
import com.jantursky.stipple.internal.DitherMode
import com.jantursky.stipple.internal.DitherImageElement
import com.jantursky.stipple.internal.ProgressField

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
): Modifier = this then DitherElement(DitherMode.Behind, field, pattern, theme, shape, animated, speed, stillTime)

/**
 * Fills the element up to a [progress] between 0 and 1, behind its content: a progress bar, a gauge segment or the
 * fill of a bar chart. Cells behind the edge are ink, a [softness]-wide band fades from ink to [track] through the
 * pattern, and the rest shows [track].
 *
 * Read the state inside [progress], for example `ditherProgress({ animated.value })`, so a change redraws the element
 * without recomposing it. Progress is information, so reduced motion doesn't freeze it.
 *
 * @param track The tone of the unfilled part: 0 is plain paper, a little more leaves a faint dotted track.
 * @param softness The width of the dithered edge as a fraction of the element's length along [direction].
 * @param fill The tone of the filled part. Defaults to solid ink; give a gradient for a fill that fades, or an
 *   animated field with [animated] for a fill that moves. [animated], [speed] and [stillTime] are those of [dither].
 */
public fun Modifier.ditherProgress(
    progress: () -> Float,
    pattern: DitherPattern = DitherPatterns.Bayer4,
    theme: StippleTheme? = null,
    direction: ProgressDirection = ProgressDirection.Right,
    shape: Shape = RectangleShape,
    track: Float = 0f,
    softness: Float = 0.12f,
    fill: DitherField = DitherFields.solid(1f),
    animated: Boolean = false,
    speed: Float = 1f,
    stillTime: Float = 0f,
): Modifier =
    dither(
        ProgressField(progress, direction, softness.coerceAtLeast(0.001f), track, fill),
        pattern,
        theme,
        shape,
        animated,
        speed,
        stillTime,
    )

/**
 * Dithers whatever the element draws: an image, an avatar, text. The content is drawn into an offscreen layer, read
 * back once and turned into cells, so nothing but the dither reaches the screen. Transparent parts stay transparent.
 *
 * It is for content that holds still. The result is cached and rebuilt only when the size, the theme or a setting
 * changes, so pass a [key] that changes with the content, for example the image itself. For [algorithm]s other than
 * [DitherAlgorithm.Ordered] the work runs once on a background dispatcher, never per frame, and the previous result
 * stays on screen until the new one is ready. For live or animated content use the shader renderer.
 *
 * Tones follow the theme: with light ink on dark paper bright parts of the content become ink, with dark ink on light
 * paper dark parts do, so a photo looks like a photo either way. [invert] flips that.
 *
 * @param key Rebuilds the dither when it changes.
 */
public fun Modifier.ditherImage(
    pattern: DitherPattern = DitherPatterns.Bayer4,
    theme: StippleTheme? = null,
    algorithm: DitherAlgorithm = DitherAlgorithm.Ordered,
    invert: Boolean = false,
    key: Any? = null,
): Modifier = this then DitherImageElement(pattern, theme, algorithm, invert, key)

/**
 * Paints what the element draws with a dither: the content keeps its shape and its transparency, and its colors come
 * from the cells. Use it on text, icons and silhouettes. Where the field's tone is low the content takes the paper
 * color, so on a paper-colored background it fades away; keep the tone high where it must stay readable.
 *
 * The parameters are those of [dither]. The content is drawn into an offscreen layer, so this works on every target,
 * live and animated, and costs one extra layer.
 */
public fun Modifier.ditherFill(
    field: DitherField,
    pattern: DitherPattern = DitherPatterns.Bayer4,
    theme: StippleTheme? = null,
    animated: Boolean = false,
    speed: Float = 1f,
    stillTime: Float = 0f,
): Modifier = this then DitherElement(DitherMode.Fill, field, pattern, theme, RectangleShape, animated, speed, stillTime)

/**
 * Shows what the element draws only where the dither is lit, so a photo or a card appears through a pattern of dots
 * and vanishes through the rest. Drive [field] with [DitherFields.sweep] to reveal or hide content. The element's own
 * content is drawn into an offscreen layer.
 *
 * The parameters are those of [dither].
 */
public fun Modifier.ditherMask(
    field: DitherField,
    pattern: DitherPattern = DitherPatterns.Bayer4,
    theme: StippleTheme? = null,
    animated: Boolean = false,
    speed: Float = 1f,
    stillTime: Float = 0f,
): Modifier = this then DitherElement(DitherMode.Mask, field, pattern, theme, RectangleShape, animated, speed, stillTime)
