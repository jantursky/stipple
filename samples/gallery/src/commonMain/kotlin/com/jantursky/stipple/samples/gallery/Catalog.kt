package com.jantursky.stipple.samples.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.DitherAlgorithm
import com.jantursky.stipple.samples.gallery.examples.AvatarExample
import com.jantursky.stipple.samples.gallery.examples.BackgroundExample
import com.jantursky.stipple.samples.gallery.examples.BarChartExample
import com.jantursky.stipple.samples.gallery.examples.ButtonExample
import com.jantursky.stipple.samples.gallery.examples.CustomFieldExample
import com.jantursky.stipple.samples.gallery.examples.CustomKind
import com.jantursky.stipple.samples.gallery.examples.DitherModifierExample
import com.jantursky.stipple.samples.gallery.examples.FieldKind
import com.jantursky.stipple.samples.gallery.examples.FieldsExample
import com.jantursky.stipple.samples.gallery.examples.FillExample
import com.jantursky.stipple.samples.gallery.examples.GaugeExample
import com.jantursky.stipple.samples.gallery.examples.GridExample
import com.jantursky.stipple.samples.gallery.examples.ImageExample
import com.jantursky.stipple.samples.gallery.examples.ImageModifierExample
import com.jantursky.stipple.samples.gallery.examples.MaskExample
import com.jantursky.stipple.samples.gallery.examples.PatternsExample
import com.jantursky.stipple.samples.gallery.examples.ProgressBarExample
import com.jantursky.stipple.samples.gallery.examples.ProgressModifierExample
import com.jantursky.stipple.samples.gallery.examples.StatCardExample
import com.jantursky.stipple.samples.gallery.examples.TextExample
import com.jantursky.stipple.samples.gallery.examples.ThemeExample
import kotlin.math.roundToInt

/** A repeatable pseudo-random number in 0..1, so "randomise" and the first look are deterministic. */
internal fun unit(
    index: Int,
    seed: Int,
): Float {
    var h = index * 374761393 + seed * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    return ((h xor (h ushr 16)) and 0xFFFF) / 65535f
}

private fun percent(v: Float): String = "${(v * 100).roundToInt()}%"

/** Every page of the gallery, in sidebar order. */
val Catalog: List<Page> =
    listOf(
        Page("overview", Group.Start, "Stipple", "Dithered UI for Compose Multiplatform.", emptyList()) { Overview(it) },
        Page(
            "patterns",
            Group.Engine,
            "Patterns",
            "A pattern is a threshold map. A cell is ink when the tone is above its threshold, so the pattern decides where dots appear as the tone rises.",
            listOf("PatternsExample"),
        ) { g -> Demo(g, minHeight = 200) { PatternsExample(g.theme) } },
        Page(
            "fields",
            Group.Engine,
            "Fields",
            "A field is a pure function of position and time that returns the tone each cell wants. Static fields ignore time; animated ones are frozen at a still frame under reduced motion.",
            listOf("FieldsExample"),
        ) { g ->
            var kind by remember { mutableStateOf(FieldKind.Linear) }
            var progress by remember { mutableFloatStateOf(0.5f) }
            Demo(
                g,
                controls = {
                    Segmented(FieldKind.entries, kind, { kind = it }, { it.name })
                    if (kind == FieldKind.Sweep) SliderField("Progress", progress, { progress = it }, 0f..1f, format = ::percent)
                },
            ) { FieldsExample(kind, g.theme, g.pattern, g.animated, progress) }
        },
        Page(
            "theme",
            Group.Engine,
            "Theme",
            "StippleTheme holds ink, paper, an optional palette, the cell size and the cell shape. Provide it once and every dither below follows, with no argument at the call site.",
            listOf("ThemeExample"),
        ) { g -> Demo(g, minHeight = 200) { ThemeExample(g.theme, g.pattern) } },
        Page(
            "custom-fields",
            Group.Engine,
            "Write your own",
            "Implement DitherField for any shape you can describe with maths. Return a smooth tone from 0 to 1, scale x by the aspect, and let the pattern make the dots.",
            listOf("CustomFieldExample"),
        ) { g ->
            var kind by remember { mutableStateOf(CustomKind.Spiral) }
            Demo(g, controls = { Segmented(CustomKind.entries, kind, { kind = it }, { it.name }) }) {
                CustomFieldExample(kind, g.theme, g.pattern, g.animated)
            }
        },
        Page(
            "dither-modifier",
            Group.Modifiers,
            "Modifier.dither",
            "Fills the element's shape with a field, behind its content. Everything else builds on it.",
            listOf("DitherModifierExample"),
        ) { g -> Demo(g) { DitherModifierExample(g.theme, g.pattern, g.animated) } },
        Page(
            "dither-progress",
            Group.Modifiers,
            "Modifier.ditherProgress",
            "A fill driven by a value: the building block of bars, gauges and chart columns. The value is read in the draw phase, so a change redraws and never recomposes.",
            listOf("ProgressModifierExample"),
        ) { g ->
            var progress by remember { mutableFloatStateOf(0.6f) }
            Demo(g, controls = { SliderField("Progress", progress, { progress = it }, 0f..1f, format = ::percent) }) {
                ProgressModifierExample(progress, g.theme, g.pattern, g.animated)
            }
        },
        Page(
            "dither-image",
            Group.Modifiers,
            "Modifier.ditherImage",
            "Dithers whatever the element draws. The content is read back once and dithered on a background dispatcher, with ordered dithering or error diffusion, then cached.",
            listOf("ImageModifierExample"),
        ) { g -> Demo(g, minHeight = 240) { ImageModifierExample(g.theme, g.pattern) } },
        Page(
            "dither-fill",
            Group.Modifiers,
            "Modifier.ditherFill",
            "Keeps the shape of what the element draws and swaps its colours for the dither. It works on text, icons and silhouettes, live and animated.",
            listOf("FillExample"),
        ) { g -> Demo(g, minHeight = 240) { FillExample(g.theme, g.pattern, g.animated) } },
        Page(
            "dither-mask",
            Group.Modifiers,
            "Modifier.ditherMask",
            "Shows the content only where the dither is lit. A sweep reveals a picture from the left through the pattern.",
            listOf("MaskExample"),
        ) { g ->
            var reveal by remember { mutableFloatStateOf(0.5f) }
            Demo(g, controls = { SliderField("Reveal", reveal, { reveal = it }, 0f..1f, format = ::percent) }) {
                MaskExample(reveal, g.theme, g.pattern)
            }
        },
        Page(
            "button",
            Group.Components,
            "DitherButton",
            "A paper face with a dithered shadow. Hover and focus thicken the shadow; pressing sinks the face into it.",
            listOf("ButtonExample"),
        ) { g ->
            var clicks by remember { mutableIntStateOf(0) }
            Demo(g, minHeight = 200, controls = { BasicText("Clicked $clicks times", style = Gallery.type.number.copy(color = Gallery.colors.muted)) }) {
                ButtonExample(g.theme, g.pattern, g.animated) { clicks++ }
            }
        },
        Page(
            "bar-chart",
            Group.Components,
            "DitherBarChart",
            "Columns filled with a dither that is dense at the foot and lighter at the tip. The chart reads out as one accessibility node.",
            listOf("BarChartExample"),
        ) { g ->
            var seed by remember { mutableIntStateOf(1) }
            val values = List(7) { 20f + unit(it, seed) * 80f }
            Demo(g, minHeight = 300, controls = { PillButton("Randomise", { seed++ }) }) {
                BarChartExample(values, g.theme, g.pattern, g.animated)
            }
        },
        Page(
            "grid",
            Group.Components,
            "DitherGrid",
            "Squares dithered to their own tones: a heatmap, a contribution calendar, a level meter.",
            listOf("GridExample"),
        ) { g ->
            var columns by remember { mutableFloatStateOf(16f) }
            var gap by remember { mutableFloatStateOf(0.16f) }
            var seed by remember { mutableIntStateOf(1) }
            val values = List(columns.roundToInt() * 7) { i -> (unit(i, seed) * unit(i / 7, seed + 9)).let { it * it * 1.4f }.coerceIn(0f, 1f) }
            Demo(
                g,
                controls = {
                    SliderField("Columns", columns, { columns = it }, 6f..30f, step = 1f, format = { it.roundToInt().toString() })
                    SliderField("Gap", gap, { gap = it }, 0f..0.5f, format = ::percent)
                    PillButton("Randomise", { seed++ })
                },
            ) { GridExample(values, columns.roundToInt(), gap, g.theme, g.pattern, g.animated) }
        },
        Page(
            "gauge",
            Group.Components,
            "DitherGauge",
            "A ring that fills with a dither from the start of its arc. It announces its value as a range.",
            listOf("GaugeExample"),
        ) { g ->
            var value by remember { mutableFloatStateOf(0.72f) }
            Demo(g, minHeight = 320, controls = { SliderField("Value", value, { value = it }, 0f..1f, format = ::percent) }) {
                GaugeExample(value, g.theme, g.pattern, g.animated)
            }
        },
        Page(
            "stat-card",
            Group.Components,
            "DitherStatCard",
            "A metric with a label, a value, a delta and a dithered trend along the foot. Text sits on plain paper.",
            listOf("StatCardExample"),
        ) { g ->
            var seed by remember { mutableIntStateOf(2) }
            val trend = List(14) { 20f + unit(it, seed) * 30f + it * 4f }
            Demo(g, minHeight = 300, controls = { PillButton("New data", { seed++ }) }) {
                StatCardExample(trend, g.theme, g.pattern, g.animated)
            }
        },
        Page(
            "progress-bar",
            Group.Components,
            "DitherProgressBar",
            "A bar with a dithered fill, and an indeterminate mode with a travelling band. Both are real progress indicators to screen readers.",
            listOf("ProgressBarExample"),
        ) { g ->
            var progress by remember { mutableFloatStateOf(0.45f) }
            Demo(g, minHeight = 200, controls = { SliderField("Progress", progress, { progress = it }, 0f..1f, format = ::percent) }) {
                ProgressBarExample(progress, g.theme, g.pattern, g.animated)
            }
        },
        Page(
            "text",
            Group.Components,
            "DitherText",
            "Letters filled with a dither. Contrast between ink and paper is enforced, and the text stays real text for screen readers. Use it for headings, not body copy.",
            listOf("TextExample"),
        ) { g ->
            var text by remember { mutableStateOf("Stipple") }
            var fadeTo by remember { mutableFloatStateOf(0.7f) }
            val colors = Gallery.colors
            Demo(
                g,
                minHeight = 220,
                controls = {
                    Labeled("Text", Modifier.width(220.dp)) {
                        BasicTextField(
                            value = text,
                            onValueChange = { text = it.take(14) },
                            singleLine = true,
                            textStyle = Gallery.type.body.copy(color = colors.text),
                            cursorBrush = SolidColor(colors.accent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, colors.hairline, RoundedCornerShape(6.dp))
                                .background(colors.panel, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        )
                    }
                    SliderField("Fade to", fadeTo, { fadeTo = it }, 0.2f..1f, format = ::percent)
                },
            ) { TextExample(text.ifBlank { " " }, fadeTo, g.theme, g.pattern, g.animated) }
        },
        Page(
            "avatar",
            Group.Components,
            "DitherAvatar",
            "Initials on a pattern made from the name, or a dithered photo. The same name always gives the same avatar.",
            listOf("AvatarExample"),
        ) { g -> Demo(g, minHeight = 200) { AvatarExample(g.theme, g.pattern, g.animated) } },
        Page(
            "image",
            Group.Components,
            "DitherImage",
            "A picture as a dither, by ordered dithering or error diffusion, with an optional reveal of the original through the pattern.",
            listOf("ImageExample"),
        ) { g ->
            var algorithm by remember { mutableStateOf(DitherAlgorithm.FloydSteinberg) }
            var reveal by remember { mutableFloatStateOf(0f) }
            Demo(
                g,
                minHeight = 280,
                controls = {
                    Segmented(DitherAlgorithm.entries, algorithm, { algorithm = it }, { it.name })
                    SliderField("Reveal", reveal, { reveal = it }, 0f..1f, format = ::percent)
                },
            ) { ImageExample(algorithm, reveal, g.theme, g.pattern, g.animated) }
        },
        Page(
            "background",
            Group.Components,
            "DitherBackground",
            "A dithered backdrop with content on top. Every cell is drawn on every frame, so a screen-sized background wants a larger cell.",
            listOf("BackgroundExample"),
        ) { g -> Demo(g, minHeight = 280) { BackgroundExample(g.theme, g.pattern, g.animated) } },
    )
