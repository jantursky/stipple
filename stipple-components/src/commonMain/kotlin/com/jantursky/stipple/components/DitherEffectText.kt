package com.jantursky.stipple.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jantursky.stipple.DitherFields
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.DitherPatterns
import com.jantursky.stipple.DitherRenderer
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.components.internal.resolveTheme
import com.jantursky.stipple.ditherFill
import com.jantursky.stipple.ditherMask
import com.jantursky.stipple.isStippleMotionReduced
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlin.math.floor

/** How [DitherEffectText] brings its text in. */
public enum class DitherTextEffect {
    /** Letters flicker through random characters and settle, first to last, into the real text. */
    Scramble,

    /** The text appears, or goes away, through a dither threshold in an irregular order. */
    Dissolve,
}

/** The characters a scramble draws from unless told otherwise. */
public const val DEFAULT_SCRAMBLE_CHARSET: String = "abcdefghijklmnopqrstuvwxyz0123456789#%&*"

/**
 * [DitherText] that plays an [effect] as it appears: [DitherTextEffect.Scramble] settles random characters into the
 * text, [DitherTextEffect.Dissolve] reveals it grain by grain. At progress 1 the text is exactly [text], filled like
 * [DitherText]; at 0 a dissolve shows nothing and a scramble shows no real letter yet.
 *
 * The component always measures as the final [text], so the effect never moves anything around it, and a screen
 * reader reads the final text, never a scrambled frame. With reduced motion the finished text shows at once and
 * [progress] is ignored. Like [DitherText] it is for headings and labels, not body text.
 *
 * Progress comes from the caller or from a timeline. Pass [progress] to drive it yourself, for example from scroll
 * or a transition: read the state inside the lambda, and a dissolve redraws without recomposing. Leave it null and
 * the component plays 0 to 1 over [durationMillis] after [delayMillis], again whenever [trigger] changes, and keeps
 * repeating when [loop] is true.
 *
 * @param cell The size of one dither cell. Text needs a finer grain than the theme's default.
 * @param minContrast The least contrast ratio between ink and paper; see [DitherText].
 * @param progress Where the effect is, from 0 to 1. Null plays the built-in timeline.
 * @param trigger Plays the timeline again when it changes. Ignored when [progress] is given.
 * @param charset The characters a scramble draws from. Spaces and line breaks are never scrambled.
 * @param renderer How the fills are drawn; see [com.jantursky.stipple.DitherRenderer].
 */
@Composable
public fun DitherEffectText(
    text: String,
    effect: DitherTextEffect,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold),
    theme: StippleTheme? = null,
    pattern: DitherPattern = DitherPatterns.Bayer4,
    cell: Dp = 1.dp,
    minContrast: Float = 4.5f,
    progress: (() -> Float)? = null,
    trigger: Any? = null,
    durationMillis: Int = 1200,
    delayMillis: Int = 0,
    loop: Boolean = false,
    charset: String = DEFAULT_SCRAMBLE_CHARSET,
    renderer: DitherRenderer = DitherRenderer.Cells,
) {
    val resolved = readable(resolveTheme(theme), minContrast).copy(cell = cell)
    val reduced = isStippleMotionReduced()
    val timeline = remember { Animatable(0f) }
    if (progress == null) {
        LaunchedEffect(trigger, durationMillis, delayMillis, loop, reduced) {
            if (reduced) {
                timeline.snapTo(1f)
                return@LaunchedEffect
            }
            do {
                timeline.snapTo(0f)
                delay(delayMillis.toLong())
                timeline.animateTo(1f, tween(durationMillis, easing = LinearEasing))
            } while (loop)
        }
    }
    val source: () -> Float =
        when {
            reduced -> ALWAYS_DONE
            progress != null -> progress
            else -> remember(timeline) { { timeline.value } }
        }
    val fill = remember { DitherFields.linear(from = 1f, to = 0.7f, angleDegrees = 0f) }
    val ink = style.copy(color = Color.Black)

    when (effect) {
        DitherTextEffect.Dissolve -> {
            val reveal = remember(source) { DitherFields.dissolve(source) }
            BasicText(
                text,
                modifier
                    .ditherMask(reveal, pattern, resolved, renderer = renderer)
                    .ditherFill(fill, pattern, resolved, renderer = renderer),
                ink,
            )
        }

        DitherTextEffect.Scramble -> {
            val ticks = (durationMillis / 1000f * SCRAMBLES_PER_SECOND).toInt().coerceAtLeast(MIN_TICKS)
            // The text as it breaks into lines, so the scrambled copy can break in the same places.
            var lineStarts by remember(text) { mutableStateOf(emptyList<Int>()) }
            val shown by remember(text, charset, ticks, source) {
                derivedStateOf {
                    val p = source().coerceIn(0f, 1f)
                    scrambleText(text, p, (p * ticks).toInt(), charset)
                }
            }
            Box(modifier) {
                // Measures as the final text and carries its semantics; it is never visible.
                BasicText(
                    text,
                    style = style.copy(color = Color.Transparent),
                    onTextLayout = { layout -> lineStarts = List(layout.lineCount) { layout.getLineStart(it) } },
                )
                BasicText(
                    breakAt(shown, text, lineStarts),
                    Modifier
                        .matchParentSize()
                        .clipToBounds()
                        .clearAndSetSemantics { }
                        .wrapContentSize(Alignment.TopStart, unbounded = true)
                        .ditherFill(fill, pattern, resolved, renderer = renderer),
                    ink,
                    softWrap = false,
                )
            }
        }
    }
}

private val ALWAYS_DONE: () -> Float = { 1f }

/** A scramble changes its characters about this often, which reads as flicker rather than blur. */
private const val SCRAMBLES_PER_SECOND = 30f
private const val MIN_TICKS = 10

/**
 * [text] with every letter that has not settled yet replaced by a character of [charset]. The first
 * `floor(progress * letters)` letters, in reading order, are the real ones; whitespace is never touched. [tick]
 * picks the random characters, so one progress and tick always give the same string.
 */
internal fun scrambleText(
    text: String,
    progress: Float,
    tick: Int,
    charset: String,
): String {
    if (charset.isEmpty() || progress >= 1f) return text
    val letters = text.count { !it.isWhitespace() }
    val settled = floor(progress.coerceIn(0f, 1f) * letters).toInt()
    var seen = 0
    return buildString(text.length) {
        text.forEachIndexed { index, char ->
            if (char.isWhitespace()) {
                append(char)
            } else {
                append(if (seen < settled) char else charset[hash(index, tick) % charset.length])
                seen++
            }
        }
    }
}

private fun hash(
    index: Int,
    tick: Int,
): Int = ((index * 73856093) xor (tick * 19349663)).absoluteValue

/** [shown] with a line break wherever [final] wrapped, so a scramble keeps the line breaks of the final text. */
private fun breakAt(
    shown: String,
    final: String,
    lineStarts: List<Int>,
): String {
    if (lineStarts.size < 2 || shown.length != final.length) return shown
    return buildString(shown.length + lineStarts.size) {
        shown.forEachIndexed { index, char ->
            if (index > 0 && index in lineStarts && final[index - 1] != '\n') append('\n')
            append(char)
        }
    }
}
