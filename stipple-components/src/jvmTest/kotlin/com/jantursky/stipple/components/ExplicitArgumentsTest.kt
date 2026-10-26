package com.jantursky.stipple.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jantursky.stipple.DitherAlgorithm
import com.jantursky.stipple.DitherFields
import com.jantursky.stipple.DitherPatterns
import com.jantursky.stipple.StippleTheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Every component with every argument given and with motion on, so the paths that defaults hide are drawn too: custom
 * ranges, angles, gaps, patterns, themes, shapes, algorithms and animated variants.
 */
@OptIn(ExperimentalTestApi::class)
class ExplicitArgumentsTest {
    private val theme = StippleTheme.Dark.copy(cell = 2.dp)
    private val pattern = DitherPatterns.Bayer8

    @Test
    fun everyComponentDrawsWithEveryArgumentGiven() =
        runComponentTest {
            var animated by mutableStateOf(false)
            setComponent {
                Column {
                    DitherButton(
                        onClick = {},
                        modifier = Modifier.size(80.dp, 30.dp),
                        enabled = false,
                        theme = theme,
                        pattern = pattern,
                        animated = animated,
                        depth = 3.dp,
                        contentPadding = PaddingValues(4.dp),
                    ) { BasicText("Go") }
                    DitherButton("Go", {}, Modifier.size(80.dp, 30.dp), true, theme, pattern, animated, 4.dp)
                    DitherBarChart(
                        entries = listOf(BarEntry("a", 2f), BarEntry("b", 5f)),
                        modifier = Modifier.size(100.dp, 50.dp),
                        maxValue = 8f,
                        theme = theme,
                        pattern = pattern,
                        animated = animated,
                        barGap = 2.dp,
                        showLabels = false,
                        contentDescription = "chart",
                    )
                    DitherGrid(
                        values = listOf(0.1f, 0.5f, 0.9f, 0.3f),
                        columns = 2,
                        modifier = Modifier.size(40.dp),
                        theme = theme,
                        pattern = pattern,
                        animated = animated,
                        gap = 0.3f,
                        cell = 2.dp,
                        contentDescription = "grid",
                    )
                    DitherGauge(
                        value = 30f,
                        modifier = Modifier.size(60.dp),
                        range = 0f..60f,
                        theme = theme,
                        pattern = pattern,
                        animated = animated,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        thickness = 0.4f,
                        contentDescription = "gauge",
                    ) { BasicText("30") }
                    DitherStatCard(
                        label = "Users",
                        value = "12",
                        modifier = Modifier.size(100.dp, 70.dp),
                        delta = "-3%",
                        trend = listOf(1f, 3f, 2f),
                        theme = theme,
                        pattern = pattern,
                        animated = animated,
                        trendHeight = 20.dp,
                    )
                    DitherProgressBar(
                        progress = 0.4f,
                        modifier = Modifier.size(100.dp, 10.dp),
                        height = 8.dp,
                        theme = theme,
                        pattern = pattern,
                        animated = animated,
                        track = 0.3f,
                        contentDescription = "progress",
                    )
                    DitherProgressBar(progress = null, theme = theme, pattern = pattern, animated = animated)
                    DitherText(
                        text = "Dither",
                        modifier = Modifier.size(100.dp, 30.dp),
                        style = TextStyle(fontSize = 20.sp),
                        theme = theme,
                        pattern = pattern,
                        animated = animated,
                        cell = 1.dp,
                        fadeTo = 0.5f,
                        field = DitherFields.solid(0.9f),
                        minContrast = 3f,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    DitherAvatar(
                        name = "Ada Lovelace",
                        modifier = Modifier,
                        size = 32.dp,
                        painter = ColorPainter(Color.Gray),
                        shape = RoundedCornerShape(8.dp),
                        theme = theme,
                        pattern = pattern,
                        algorithm = DitherAlgorithm.Atkinson,
                        animated = animated,
                    )
                    DitherImage(
                        painter = ColorPainter(Color.Gray),
                        contentDescription = "photo",
                        modifier = Modifier.size(40.dp),
                        contentScale = ContentScale.Fit,
                        theme = theme,
                        pattern = pattern,
                        algorithm = DitherAlgorithm.Ordered,
                        reveal = 0.5f,
                        animated = animated,
                    )
                    DitherBackground(
                        modifier = Modifier.size(40.dp),
                        field = DitherFields.waves(),
                        theme = theme,
                        pattern = pattern,
                        animated = animated,
                    ) { BasicText("x") }
                }
            }
            mainClock.advanceTimeBy(100)
            animated = true
            mainClock.advanceTimeBy(400)
            waitForIdle()
            assertTrue(capture().shareOf(theme.ink) > 0f)
        }
}
