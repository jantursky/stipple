package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.components.DitherEffectText
import com.jantursky.stipple.components.DitherTextEffect

// The effect plays from its own timeline whenever the trigger changes, or follows a value you own: here a slider.
// A dissolve reads the value while drawing, so dragging the slider redraws the text without recomposing it.
@Composable
fun TextEffectsExample(
    effect: DitherTextEffect,
    progress: Float?,
    trigger: Int,
    theme: StippleTheme,
    pattern: DitherPattern,
) {
    DitherEffectText(
        "Dither everything",
        effect,
        Modifier,
        TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold),
        theme,
        pattern,
        progress = progress?.let { value -> { value } },
        trigger = trigger,
        durationMillis = 1600,
    )
}
