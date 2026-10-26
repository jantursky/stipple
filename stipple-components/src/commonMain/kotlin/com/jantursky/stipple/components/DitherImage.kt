package com.jantursky.stipple.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import com.jantursky.stipple.DitherAlgorithm
import com.jantursky.stipple.DitherField
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.DitherPatterns
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.components.internal.resolveTheme
import com.jantursky.stipple.ditherImage
import com.jantursky.stipple.ditherMask
import com.jantursky.stipple.isStippleMotionReduced

/**
 * An image shown as a dither. It uses Floyd-Steinberg error diffusion by default, computed once on a background
 * dispatcher and cached, so the picture looks like itself in the theme's two colors.
 *
 * [reveal] lets the original through a dither pattern from left to right: 0 shows only the dither and 1 only the
 * original. With [animated] the reveal sweeps there and back forever, or rests at [reveal] under reduced motion.
 *
 * The dither is rebuilt only when the size, the theme, the pattern, the algorithm or [painter] changes, so remember
 * the painter instead of creating one on every composition.
 *
 * @param contentDescription Read by screen readers. Pass null for a purely decorative image.
 * @param reveal How much of the original shows, from 0 to 1.
 */
@Composable
public fun DitherImage(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    theme: StippleTheme? = null,
    pattern: DitherPattern = DitherPatterns.Bayer4,
    algorithm: DitherAlgorithm = DitherAlgorithm.FloydSteinberg,
    reveal: Float = 0f,
    animated: Boolean = false,
) {
    val resolved = resolveTheme(theme)
    val level = reveal.coerceIn(0f, 1f)
    val moving = animated && !isStippleMotionReduced()
    val phase: State<Float> =
        if (moving) {
            rememberInfiniteTransition().animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Reverse),
            )
        } else {
            remember { mutableFloatStateOf(0f) }
        }
    Box(modifier) {
        Image(
            painter,
            contentDescription,
            Modifier.matchParentSize().ditherImage(pattern, resolved, algorithm, key = painter),
            alignment = Alignment.Center,
            contentScale = contentScale,
        )
        if (moving || level > 0f) {
            val sweep = remember(moving, level, phase) { RevealSweep { if (moving) phase.value else level } }
            Image(
                painter,
                null,
                Modifier.matchParentSize().ditherMask(sweep, pattern, resolved),
                alignment = Alignment.Center,
                contentScale = contentScale,
            )
        }
    }
}

/** A soft edge sweeping from the left as [progress] grows; the value is read in the draw phase. */
private class RevealSweep(
    private val progress: () -> Float,
) : DitherField {
    override fun tone(
        x: Float,
        y: Float,
        t: Float,
        aspect: Float,
    ): Float {
        val edge = progress().coerceIn(0f, 1f) * (1f + SOFTNESS) - SOFTNESS
        return 1f - ((x - edge) / SOFTNESS).coerceIn(0f, 1f)
    }

    private companion object {
        const val SOFTNESS = 0.18f
    }
}
