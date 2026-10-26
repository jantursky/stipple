package com.jantursky.stipple.samples.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jantursky.stipple.DitherFields
import com.jantursky.stipple.DitherPatterns
import com.jantursky.stipple.ProvideStippleTheme
import com.jantursky.stipple.components.BarEntry
import com.jantursky.stipple.components.DitherAvatar
import com.jantursky.stipple.components.DitherBarChart
import com.jantursky.stipple.components.DitherButton
import com.jantursky.stipple.components.DitherGauge
import com.jantursky.stipple.components.DitherGrid
import com.jantursky.stipple.components.DitherProgressBar
import com.jantursky.stipple.components.DitherText
import com.jantursky.stipple.dither

/** Opens a page by id. Provided by [GalleryApp]. */
val LocalNavigate = compositionLocalOf<(String) -> Unit> { {} }

/** The Stipple mark, drawn by Stipple: a sixteen-cell ramp from ink to paper through a Bayer matrix. */
@Composable
fun StippleMark(
    state: GalleryState,
    size: Int = 24,
) {
    val theme = remember(state.dark) { (if (state.dark) com.jantursky.stipple.StippleTheme.Dark else com.jantursky.stipple.StippleTheme.Light).copy(cell = (size / 16f).dp) }
    Box(Modifier.size(size.dp).dither(DitherFields.linear(1f, 0f, 45f), DitherPatterns.Bayer8, theme))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Overview(state: GalleryState) {
    val colors = Gallery.colors
    val navigate = LocalNavigate.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BasicText(
                "Every pixel is a decision.",
                style = Gallery.type.title.copy(color = colors.text, fontSize = 40.sp, lineHeight = 46.sp),
            )
            BasicText(
                "Stipple draws any Compose element with a dither: ordered Bayer and blue-noise patterns, fields that move, and a theme that follows light and dark. Static or animated, on Android, iOS, desktop and the web, from one commonMain.",
                style = Gallery.type.body.copy(color = colors.muted, fontSize = 16.sp, lineHeight = 24.sp),
            )
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, colors.hairline, RoundedCornerShape(8.dp))
                    .background(colors.code)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                BasicText("implementation(\"io.github.jantursky:stipple:1.0.0\")", style = Gallery.type.code.copy(color = colors.text))
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Tile("Patterns", "patterns", state) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DitherPatterns.All.forEach { Box(Modifier.size(34.dp).dither(DitherFields.linear(), it, state.theme.copy(cell = 2.dp))) }
                }
            }
            Tile("DitherGauge", "gauge", state) { DitherGauge(0.72f, Modifier.size(110.dp), theme = state.theme.copy(cell = 2.dp), animated = state.animated) }
            Tile("DitherBarChart", "bar-chart", state) {
                DitherBarChart(
                    List(6) { BarEntry("", 30f + unit(it, 3) * 70f) },
                    Modifier.size(width = 150.dp, height = 100.dp),
                    theme = state.theme.copy(cell = 2.dp),
                    animated = state.animated,
                    showLabels = false,
                    barGap = 6.dp,
                )
            }
            Tile("DitherText", "text", state) {
                DitherText("Aa", style = TextStyle(fontSize = 76.sp, fontWeight = FontWeight.Bold), theme = state.theme, animated = state.animated)
            }
            Tile("DitherAvatar", "avatar", state) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Ada Lovelace", "Grace Hopper", "Alan Turing").forEach { DitherAvatar(it, size = 52.dp, theme = state.theme, animated = state.animated) }
                }
            }
            Tile("DitherProgressBar", "progress-bar", state) {
                Column(Modifier.width(150.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    DitherProgressBar(0.6f, theme = state.theme.copy(cell = 2.dp), animated = state.animated)
                    DitherProgressBar(null, theme = state.theme.copy(cell = 2.dp), animated = state.animated)
                }
            }
            Tile("DitherGrid", "grid", state) {
                DitherGrid(List(7 * 12) { (unit(it, 5) * unit(it / 7, 8)).coerceIn(0f, 1f) }, 12, Modifier.width(170.dp), theme = state.theme, animated = state.animated)
            }
            Tile("DitherButton", "button", state) { DitherButton("Save", {}, theme = state.theme, animated = state.animated) }
        }
    }
}

@Composable
private fun Tile(
    title: String,
    page: String,
    state: GalleryState,
    content: @Composable () -> Unit,
) {
    val colors = Gallery.colors
    val navigate = LocalNavigate.current
    Column(
        Modifier
            .width(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, colors.hairline, RoundedCornerShape(12.dp))
            .semantics { role = Role.Button }
            .clickable { navigate(page) },
    ) {
        Box(Modifier.fillMaxWidth().height(150.dp).background(state.theme.paper), contentAlignment = Alignment.Center) {
            ProvideStippleTheme(state.theme) { content() }
        }
        Box(Modifier.fillMaxWidth().background(colors.page).padding(horizontal = 12.dp, vertical = 10.dp)) {
            BasicText(title, style = Gallery.type.label.copy(color = colors.text))
        }
    }
}
