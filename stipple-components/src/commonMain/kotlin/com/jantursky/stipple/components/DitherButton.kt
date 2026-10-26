package com.jantursky.stipple.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jantursky.stipple.DitherField
import com.jantursky.stipple.DitherFields
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.DitherPatterns
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.components.internal.resolveTheme
import com.jantursky.stipple.dither
import com.jantursky.stipple.isStippleMotionReduced
import kotlin.math.roundToInt

/**
 * A button with a dithered shadow. The face is plain paper with an ink outline, so its label keeps full contrast; the
 * shadow behind it thickens when the pointer is over the button and the face sinks into it when pressed.
 *
 * With [animated] the shadow shimmers. Hover, focus and press feedback keep working under reduced motion, as an
 * instant change.
 *
 * @param depth How far the shadow reaches to the right and below.
 */
@Composable
public fun DitherButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    theme: StippleTheme? = null,
    pattern: DitherPattern = DitherPatterns.Bayer4,
    animated: Boolean = false,
    depth: Dp = 6.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit,
) {
    val resolved = resolveTheme(theme)
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val reduced = isStippleMotionReduced()
    val spec = if (reduced) snap<Float>() else tween(120)
    val shadowTone by animateFloatAsState(
        when {
            !enabled -> 0.18f
            pressed -> 1f
            hovered || focused -> 0.8f
            else -> 0.5f
        },
        spec,
    )
    val sink by animateFloatAsState(if (pressed && enabled) 1f else 0f, spec)
    val shimmer = remember { DitherFields.waves(count = 5f, speed = 0.5f, angleDegrees = 35f, low = 0.55f, high = 1f) }
    val shadow =
        remember(animated) {
            if (animated) {
                DitherField { x, y, t, aspect -> shimmer.tone(x, y, t, aspect) }
            } else {
                DitherFields.solid(1f)
            }
        }
    val shadowField = DitherField { x, y, t, aspect -> shadow.tone(x, y, t, aspect) * shadowTone }
    Box(
        modifier
            .hoverable(interactionSource, enabled)
            .clickable(interactionSource, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .padding(start = depth, top = depth)
                .dither(shadowField, pattern, resolved, animated = animated, stillTime = 0.3f),
        )
        Box(
            Modifier
                .padding(end = depth, bottom = depth)
                .offset { IntOffset((sink * depth.toPx()).roundToInt(), (sink * depth.toPx()).roundToInt()) }
                .background(resolved.paper)
                .border(if (focused) 2.5.dp else 1.5.dp, resolved.ink)
                .alpha(if (enabled) 1f else 0.5f)
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, content = content)
        }
    }
}

/** A [DitherButton] with a text label. */
@Composable
public fun DitherButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    theme: StippleTheme? = null,
    pattern: DitherPattern = DitherPatterns.Bayer4,
    animated: Boolean = false,
    depth: Dp = 6.dp,
) {
    val resolved = resolveTheme(theme)
    DitherButton(onClick, modifier, enabled, resolved, pattern, animated, depth) {
        BasicText(text, style = TextStyle(color = resolved.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
    }
}
