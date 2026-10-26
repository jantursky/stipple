package com.jantursky.stipple.samples.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement

/** The source of the example on the page, selectable, with Kotlin highlighted. */
@Composable
fun CodePanel(
    source: String,
    modifier: Modifier = Modifier,
) {
    val colors = Gallery.colors
    val highlighted = remember(source, colors) { highlight(source, colors) }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, colors.hairline, RoundedCornerShape(12.dp))
            .background(colors.code),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BasicText("Kotlin", style = Gallery.type.caption.copy(color = colors.faint))
            BasicText("${source.lines().size} lines", style = Gallery.type.caption.copy(color = colors.faint))
        }
        BasicText(
            highlighted,
            Modifier
                .semantics { contentDescription = "Source code of the example" }
                .horizontalScroll(rememberScrollState())
                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
            style = Gallery.type.code.copy(color = colors.text),
            softWrap = false,
        )
    }
}

private val keywords =
    setOf(
        "fun", "val", "var", "if", "else", "when", "return", "class", "object", "private", "public", "internal", "override",
        "remember", "by", "in", "for", "is", "null", "true", "false", "it", "interface", "import", "package", "this",
    )

private val token = Regex("""//[^\n]*|"(?:\\.|[^"\\])*"|\b\d[\d_.]*[fFLdp]*\b|@[A-Za-z]+|\b[A-Za-z_][A-Za-z0-9_]*\b""")

/** A small highlighter: comments, strings, numbers, annotations, keywords and composable-looking calls. */
internal fun highlight(
    source: String,
    colors: GalleryColors,
): AnnotatedString {
    val comment = colors.faint
    val string = if (colors.page == Color(0xFFFFFFFF)) Color(0xFF0D542B) else Color(0xFF7EE2A8)
    val keyword = colors.accent
    val number = if (colors.page == Color(0xFFFFFFFF)) Color(0xFFB45309) else Color(0xFFFFCF5C)
    val call = colors.text
    return buildAnnotatedString {
        append(source)
        for (match in token.findAll(source)) {
            val text = match.value
            val style =
                when {
                    text.startsWith("//") -> SpanStyle(color = comment)
                    text.startsWith("\"") -> SpanStyle(color = string)
                    text.startsWith("@") -> SpanStyle(color = comment)
                    text[0].isDigit() -> SpanStyle(color = number)
                    text in keywords -> SpanStyle(color = keyword, fontWeight = FontWeight.Medium)
                    text[0].isUpperCase() -> SpanStyle(color = call, fontWeight = FontWeight.Medium)
                    else -> null
                }
            if (style != null) addStyle(style, match.range.first, match.range.last + 1)
        }
    }
}
