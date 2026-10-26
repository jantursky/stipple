package com.jantursky.stipple.samples.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** A label above a control. */
@Composable
fun Labeled(
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    content: @Composable () -> Unit,
) {
    // Intrinsic width, so a labeled control takes the room it needs and the settings can sit side by side.
    Column(modifier.width(IntrinsicSize.Max), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText(label, style = Gallery.type.caption.copy(color = Gallery.colors.muted))
            if (value != null) BasicText(value, style = Gallery.type.number.copy(color = Gallery.colors.text))
        }
        content()
    }
}

/** One choice out of a few, as a row of pills. */
@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    val colors = Gallery.colors
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, colors.hairline, RoundedCornerShape(8.dp))
            .background(colors.panel)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            val on = option == selected
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (on) colors.page else Color.Transparent)
                    .then(if (on) Modifier.border(1.dp, colors.hairline, RoundedCornerShape(6.dp)) else Modifier)
                    .semantics { role = Role.RadioButton; this.selected = on }
                    .clickable { onSelect(option) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(label(option), style = Gallery.type.caption.copy(color = if (on) colors.text else colors.muted))
            }
        }
    }
}

/** A slider with no Material: a track, a fill and a thumb, dragged or tapped. */
@Composable
fun GallerySlider(
    value: Float,
    onChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    description: String,
    step: Float = 0f,
) {
    val colors = Gallery.colors
    var width by remember { mutableIntStateOf(1) }
    val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

    fun snap(raw: Float): Float {
        val stepped = if (step > 0f) (raw / step).roundToInt() * step else raw
        return stepped.coerceIn(range.start, range.endInclusive)
    }

    fun at(x: Float): Float = snap(range.start + (x / width).coerceIn(0f, 1f) * (range.endInclusive - range.start))
    Box(
        modifier
            .height(24.dp)
            .fillMaxWidth()
            .onSizeChanged { width = it.width.coerceAtLeast(1) }
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(value, range)
                setProgress { onChange(snap(it)); true }
            }.pointerInput(range, step) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onChange(at(down.position.x))
                    drag(down.id) {
                        onChange(at(it.position.x))
                        it.consume()
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(colors.hairline))
        Box(Modifier.fillMaxWidth(fraction).height(4.dp).clip(CircleShape).background(colors.accent))
        val thumb = 16.dp
        Box(
            Modifier
                .offset { androidx.compose.ui.unit.IntOffset((fraction * (width - thumb.roundToPx())).roundToInt(), 0) }
                .size(thumb)
                .clip(CircleShape)
                .background(colors.page)
                .border(2.dp, colors.accent, CircleShape),
        )
    }
}

/** A labeled slider that shows its value. */
@Composable
fun SliderField(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    step: Float = 0f,
    format: (Float) -> String = { (it * 100).roundToInt().div(100f).toString() },
) {
    Labeled(label, modifier.width(200.dp), value = format(value)) {
        GallerySlider(value, onChange, range, description = label, step = step)
    }
}

/** An on or off switch with a label. */
@Composable
fun SwitchField(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Gallery.colors
    Row(
        modifier
            .semantics(mergeDescendants = true) {
                role = Role.Switch
                stateDescription = if (checked) "On" else "Off"
            }.clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(width = 32.dp, height = 18.dp)
                .clip(CircleShape)
                .background(if (checked) colors.accent else colors.hairline)
                .padding(2.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(if (checked) colors.onAccent else colors.page))
        }
        BasicText(label, style = Gallery.type.label.copy(color = colors.text))
    }
}

/** A row of color swatches; null means "follow the theme". */
@Composable
fun Swatches(
    options: List<Color?>,
    selected: Color?,
    onSelect: (Color?) -> Unit,
    followColor: Color,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = Gallery.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            val on = option == selected
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(option ?: followColor)
                    .border(if (on) 2.dp else 1.dp, if (on) colors.accent else colors.hairline, CircleShape)
                    .semantics {
                        role = Role.RadioButton
                        this.selected = on
                        contentDescription = if (option == null) "$description: theme default" else "$description colour"
                    }.clickable { onSelect(option) },
            ) {
                if (option == null) {
                    // A slash marks the swatch that follows the theme.
                    Box(Modifier.align(Alignment.Center).width(14.dp).height(1.5.dp).background(colors.faint))
                }
            }
        }
    }
}

/** A pill button for the bar and the code panel. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    val colors = Gallery.colors
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (primary) colors.text else colors.page)
            .border(1.dp, if (primary) colors.text else colors.hairline, RoundedCornerShape(6.dp))
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = Gallery.type.label.copy(color = if (primary) colors.page else colors.text), maxLines = 1, softWrap = false)
    }
}

@Composable
fun Gap(size: Int) = Spacer(Modifier.height(size.dp))
