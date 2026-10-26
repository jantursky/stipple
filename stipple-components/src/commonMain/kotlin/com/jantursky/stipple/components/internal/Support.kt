package com.jantursky.stipple.components.internal

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jantursky.stipple.LocalStippleTheme
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.isStippleMotionReduced

/** The theme a component draws with: its own, or the one in the composition. */
@Composable
internal fun resolveTheme(theme: StippleTheme?): StippleTheme = theme ?: LocalStippleTheme.current

/**
 * A value that follows [target]. With [animate] it eases there, after [delayMillis], unless reduced motion is on;
 * otherwise it jumps. [initial] is where the first composition starts, so a chart can grow in from nothing.
 * Read `.value` in a draw block or a layout lambda and a frame won't recompose anything.
 */
@Composable
internal fun rememberEased(
    target: Float,
    animate: Boolean,
    initial: Float = target,
    delayMillis: Int = 0,
    durationMillis: Int = 600,
): Animatable<Float, *> {
    val reduced = isStippleMotionReduced()
    val value = remember { Animatable(if (animate && !reduced) initial else target) }
    LaunchedEffect(target, animate, reduced) {
        val spec: AnimationSpec<Float> =
            if (animate && !reduced) tween(durationMillis, delayMillis) else snap()
        value.animateTo(target, spec)
    }
    return value
}

/** A plain label in [color]; the components bring no Material, so this is their text. */
@Composable
internal fun Label(
    text: String,
    color: Color,
    sizeSp: Int = 12,
    weight: FontWeight = FontWeight.Medium,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
) {
    BasicText(
        text,
        modifier,
        TextStyle(
            color = color,
            fontSize = sizeSp.sp,
            fontWeight = weight,
            lineHeight =
                (
                    sizeSp *
                        1.35f
                ).sp,
        ),
    )
}
