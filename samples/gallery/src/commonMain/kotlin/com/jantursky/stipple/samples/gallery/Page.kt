package com.jantursky.stipple.samples.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.ProvideStippleTheme

/** What a page belongs to in the sidebar. */
enum class Group(
    val title: String,
) {
    Start("Start"),
    Engine("Engine"),
    Modifiers("Modifiers"),
    Components("Components"),
    Scenes("Backgrounds"),
    Interface("Interface"),
}

/**
 * One page of the gallery: a live example with its controls, and the source of the example next to it.
 *
 * @property sources File names, without extension, of the examples this page runs; their source is shown under the stage.
 */
class Page(
    val id: String,
    val group: Group,
    val title: String,
    val summary: String,
    val sources: List<String>,
    val content: @Composable (GalleryState) -> Unit,
)

/** The panel the example runs on: the theme's paper, so what you see is what the dither draws over. */
@Composable
fun Stage(
    state: GalleryState,
    modifier: Modifier = Modifier,
    minHeight: Int = 260,
    content: @Composable () -> Unit,
) {
    val colors = Gallery.colors
    val theme = state.theme
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, colors.hairline, RoundedCornerShape(16.dp))
            .background(theme.paper)
            .heightIn(min = minHeight.dp)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        ProvideStippleTheme(theme) { content() }
    }
}

/** The controls of one page, wrapping onto new lines. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Controls(content: @Composable () -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) { content() }
}

/** The same stage-and-controls column most pages use. */
@Composable
fun Demo(
    state: GalleryState,
    minHeight: Int = 260,
    controls: @Composable () -> Unit = {},
    stage: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Stage(state, minHeight = minHeight, content = stage)
        Controls(controls)
    }
}
