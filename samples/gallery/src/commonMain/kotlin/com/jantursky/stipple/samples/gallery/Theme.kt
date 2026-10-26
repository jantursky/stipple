package com.jantursky.stipple.samples.gallery

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jantursky.stipple.samples.gallery.resources.Res
import com.jantursky.stipple.samples.gallery.resources.geist
import com.jantursky.stipple.samples.gallery.resources.geist_mono
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource

/** The colors of the gallery's own chrome, from design/DESIGN.md. The dithers on the stage use StippleTheme. */
@Immutable
class GalleryColors(
    val page: Color,
    val panel: Color,
    val hairline: Color,
    val text: Color,
    val muted: Color,
    val faint: Color,
    val accent: Color,
    val onAccent: Color,
    val code: Color,
) {
    companion object {
        val Light =
            GalleryColors(
                page = Color(0xFFFFFFFF),
                panel = Color(0xFFFAFAFA),
                hairline = Color(0xFFE4E4E7),
                text = Color(0xFF09090B),
                muted = Color(0xFF52525C),
                faint = Color(0xFF71717A),
                accent = Color(0xFF615FFF),
                onAccent = Color(0xFFFFFFFF),
                code = Color(0xFFF4F4F5),
            )
        val Dark =
            GalleryColors(
                page = Color(0xFF09090B),
                panel = Color(0xFF111113),
                hairline = Color(0xFF27272A),
                text = Color(0xFFFAFAFA),
                muted = Color(0xFFA1A1AA),
                faint = Color(0xFF8E8E98),
                accent = Color(0xFF8B8AFF),
                onAccent = Color(0xFF09090B),
                code = Color(0xFF18181B),
            )
    }
}

@Immutable
class GalleryType(
    val title: TextStyle,
    val heading: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val code: TextStyle,
    val number: TextStyle,
)

val LocalGalleryColors = compositionLocalOf { GalleryColors.Light }
val LocalGalleryType =
    compositionLocalOf {
        val base = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
        GalleryType(base, base, base, base, base, base, base)
    }

/** The colors and type of the chrome. */
object Gallery {
    val colors: GalleryColors @Composable get() = LocalGalleryColors.current
    val type: GalleryType @Composable get() = LocalGalleryType.current
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun variableFamily(
    font: FontResource,
    vararg weights: FontWeight,
): FontFamily =
    FontFamily(
        weights.map { Font(font, it, variationSettings = FontVariation.Settings(FontVariation.weight(it.weight))) },
    )

@Composable
fun GalleryTheme(
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    val sans = variableFamily(Res.font.geist, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold)
    val mono = variableFamily(Res.font.geist_mono, FontWeight.Normal, FontWeight.Medium)
    val type =
        remember(sans, mono) {
            fun style(
                family: FontFamily,
                size: Int,
                line: Int,
                weight: FontWeight,
                tracking: Float = 0f,
            ) = TextStyle(
                fontFamily = family,
                fontSize = size.sp,
                lineHeight = line.sp,
                fontWeight = weight,
                letterSpacing = tracking.sp,
            )
            GalleryType(
                title = style(sans, 30, 36, FontWeight.SemiBold, -0.6f),
                heading = style(sans, 18, 26, FontWeight.SemiBold, -0.2f),
                body = style(sans, 14, 21, FontWeight.Normal),
                label = style(sans, 13, 18, FontWeight.Medium),
                caption = style(sans, 12, 16, FontWeight.Medium),
                code = style(mono, 12, 19, FontWeight.Normal),
                number = style(mono, 12, 16, FontWeight.Medium),
            )
        }
    CompositionLocalProvider(
        LocalGalleryColors provides if (dark) GalleryColors.Dark else GalleryColors.Light,
        LocalGalleryType provides type,
    ) {
        SelectionContainer(content = content)
    }
}
