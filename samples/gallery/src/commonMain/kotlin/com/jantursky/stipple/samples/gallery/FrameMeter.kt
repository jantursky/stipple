package com.jantursky.stipple.samples.gallery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.withFrameNanos

/** The time between the last frames, in milliseconds. The ring is read only when the meter draws. */
private const val FRAMES = 48

/**
 * A small bar chart of the last frame times with the average beside it. It keeps the frame clock running, so it also
 * shows what the page costs; the numbers are the time between frames, which is the refresh interval when idle.
 */
@Composable
fun FrameMeter(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = Gallery.colors
    val times = remember { FloatArray(FRAMES) }
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        var index = 0
        while (true) {
            val now = withFrameNanos { it }
            times[index % FRAMES] = (now - last) / 1_000_000f
            last = now
            index++
            tick = index.toLong()
        }
    }
    val average = run {
        tick
        times.filter { it > 0f }.let { if (it.isEmpty()) 0f else it.average().toFloat() }
    }
    Row(modifier.semantics { contentDescription = "Frame time ${average.toInt()} milliseconds" }, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.width(if (compact) 28.dp else 72.dp).height(20.dp)) {
            tick
            val barWidth = size.width / FRAMES
            for (i in 0 until FRAMES) {
                val ms = times[i]
                if (ms <= 0f) continue
                val h = (ms / 40f).coerceIn(0.05f, 1f) * size.height
                drawRect(
                    color = if (ms > 25f) colors.accent else colors.faint,
                    topLeft = Offset(i * barWidth, size.height - h),
                    size = Size((barWidth - 1f).coerceAtLeast(1f), h),
                )
            }
        }
        BasicText(
            "${average.toInt()} ms".padStart(6),
            Modifier.padding(start = 6.dp),
            style = Gallery.type.number.copy(color = colors.muted),
            maxLines = 1,
            softWrap = false,
        )
    }
}
