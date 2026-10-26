package com.jantursky.stipple.samples.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jantursky.stipple.CellShape
import com.jantursky.stipple.DitherPatterns
import com.jantursky.stipple.LocalStippleMotion
import com.jantursky.stipple.StippleMotion
import com.jantursky.stipple.StippleTheme

const val GALLERY_VERSION = "1.0.0"

/**
 * The whole gallery: a top bar, a sidebar of pages (a menu on narrow screens), the shared dither settings and the
 * current page with its source.
 *
 * @param initialPage The id of the page to open, such as `gauge`; the overview by default.
 * @param onPageChange Called with the id of each page that opens, so a host can keep it in a URL.
 * @param onDarkModeChange Called with the theme in use, so a host can match its system bars.
 * @param initialAnimated Starts with animation on, as the docs' live examples do.
 * @param embed Shows only the current page's example, with no bar or sidebar, for embedding in another page.
 */
@Composable
fun GalleryApp(
    modifier: Modifier = Modifier,
    initialPage: String? = null,
    initialDark: Boolean = isSystemInDarkTheme(),
    onPageChange: (String) -> Unit = {},
    onDarkModeChange: (Boolean) -> Unit = {},
    initialAnimated: Boolean = false,
    embed: Boolean = false,
) {
    val state = remember { GalleryState(initialDark).also { it.animated = initialAnimated } }
    var pageId by remember { mutableStateOf(Catalog.firstOrNull { it.id == initialPage }?.id ?: "overview") }
    var menuOpen by remember { mutableStateOf(false) }
    LaunchedEffect(pageId) { onPageChange(pageId) }
    LaunchedEffect(state.dark) { onDarkModeChange(state.dark) }
    GalleryTheme(state.dark) {
        val colors = Gallery.colors
        CompositionLocalProvider(
            LocalStippleMotion provides state.motion,
            LocalNavigate provides { id ->
                pageId = id
                menuOpen = false
            },
        ) {
            val frame = modifier.fillMaxSize().background(colors.page).windowInsetsPadding(WindowInsets.safeDrawing)
            if (embed) {
                val example = Catalog.firstOrNull { it.id == pageId } ?: Catalog.first()
                Box(frame.verticalScroll(rememberScrollState()).padding(12.dp)) { example.content(state) }
                return@CompositionLocalProvider
            }
            BoxWithConstraints(frame) {
                val wide = maxWidth >= 880.dp
                Column(Modifier.fillMaxSize()) {
                    TopBar(state, wide, menuOpen) { menuOpen = !menuOpen }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.hairline))
                    Row(Modifier.fillMaxSize()) {
                        if (wide) {
                            Sidebar(pageId, Modifier.width(232.dp).fillMaxHeight()) { pageId = it }
                            Box(Modifier.width(1.dp).fillMaxHeight().background(colors.hairline))
                        }
                        if (!wide && menuOpen) {
                            Sidebar(pageId, Modifier.fillMaxSize()) {
                                pageId = it
                                menuOpen = false
                            }
                        } else {
                            PageView(state, Catalog.first { it.id == pageId }, wide)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    state: GalleryState,
    wide: Boolean,
    menuOpen: Boolean,
    onMenu: () -> Unit,
) {
    val colors = Gallery.colors
    val uri = LocalUriHandler.current
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = if (wide) 20.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!wide) PillButton(if (menuOpen) "Close" else "Menu", onMenu)
        StippleMark(state)
        BasicText("Stipple", style = Gallery.type.heading.copy(color = colors.text))
        if (wide) BasicText("v$GALLERY_VERSION", style = Gallery.type.number.copy(color = colors.faint))
        Spacer(Modifier.weight(1f))
        if (wide) {
            Link("Docs") { uri.openUri("https://stipple-docs.vercel.app") }
            Link("GitHub") { uri.openUri("https://github.com/jantursky/stipple") }
        }
        FrameMeter(compact = !wide)
        PillButton(if (state.dark) "Light" else "Dark", { state.dark = !state.dark })
    }
}

@Composable
private fun Link(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .semantics {
                role = Role.Button
            }.clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        BasicText(text, style = Gallery.type.label.copy(color = Gallery.colors.muted))
    }
}

@Composable
private fun Sidebar(
    selected: String,
    modifier: Modifier,
    onSelect: (String) -> Unit,
) {
    val colors = Gallery.colors
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Group.entries.forEach { group ->
            if (group != Group.Start) {
                BasicText(
                    group.title.uppercase(),
                    Modifier.padding(start = 10.dp, top = 16.dp, bottom = 6.dp).semantics { heading() },
                    style = Gallery.type.caption.copy(color = colors.faint, letterSpacing = 0.8.sp),
                )
            }
            Catalog.filter { it.group == group }.forEach { page ->
                val on = page.id == selected
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (on) colors.panel else Color.Transparent)
                        .semantics {
                            role = Role.Tab
                            this.selected = on
                        }.clickable { onSelect(page.id) }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                ) {
                    BasicText(
                        page.title,
                        style = Gallery.type.label.copy(color = if (on) colors.text else colors.muted),
                    )
                }
            }
        }
    }
}

@Composable
private fun PageView(
    state: GalleryState,
    page: Page,
    wide: Boolean,
) {
    val colors = Gallery.colors
    val scroll = rememberScrollState()
    LaunchedEffect(page.id) { scroll.scrollTo(0) }
    Box(Modifier.fillMaxSize().verticalScroll(scroll), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 940.dp)
                .fillMaxWidth()
                .padding(horizontal = if (wide) 32.dp else 16.dp, vertical = if (wide) 32.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (page.id != "overview") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BasicText(page.group.title.uppercase(), style = Gallery.type.caption.copy(color = colors.accent))
                    BasicText(
                        page.title,
                        Modifier.semantics { heading() },
                        style = Gallery.type.title.copy(color = colors.text),
                    )
                    BasicText(page.summary, style = Gallery.type.body.copy(color = colors.muted, lineHeight = 22.sp))
                }
                Settings(state, wide)
            }
            page.content(state)
            page.sources.forEach { name -> ExampleSources[name]?.let { CodePanel(it) } }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Settings(
    state: GalleryState,
    wide: Boolean,
) {
    val colors = Gallery.colors
    var open by remember { mutableStateOf(wide) }
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(
                shape,
            ).border(1.dp, colors.hairline, shape)
            .background(colors.panel)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { open = !open },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText("Dither settings", style = Gallery.type.label.copy(color = colors.text))
            BasicText(if (open) "Hide" else "Show", style = Gallery.type.caption.copy(color = colors.faint))
        }
        if (open) {
            val base = if (state.dark) StippleTheme.Dark else StippleTheme.Light
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Labeled("Pattern") {
                    val name = { pattern: com.jantursky.stipple.DitherPattern ->
                        DitherPatterns.nameOf(pattern).removePrefix("Bayer ")
                    }
                    Segmented(DitherPatterns.All, state.pattern, { state.pattern = it }, name)
                }
                SliderField("Cell size", state.cell, { state.cell = it }, 1f..10f, step = 0.5f, format = { "${it}dp" })
                Labeled("Shape") { Segmented(CellShape.entries, state.shape, { state.shape = it }, { it.name }) }
                Labeled("Ink") { Swatches(GalleryState.Inks, state.ink, { state.ink = it }, base.ink, "Ink") }
                Labeled(
                    "Paper",
                ) { Swatches(GalleryState.Papers, state.paper, { state.paper = it }, base.paper, "Paper") }
                Labeled("Reduced motion") {
                    Segmented(StippleMotion.entries, state.motion, { state.motion = it }, ::motionLabel)
                }
                SwitchField("Animated", state.animated, { state.animated = it }, Modifier.padding(top = 22.dp))
            }
        }
    }
}

private fun motionLabel(motion: StippleMotion): String =
    when (motion) {
        StippleMotion.System -> "System"
        StippleMotion.Full -> "Off"
        StippleMotion.Reduced -> "On"
    }
