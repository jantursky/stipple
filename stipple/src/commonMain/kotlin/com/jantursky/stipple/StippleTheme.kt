package com.jantursky.stipple

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.pow

/** The shape of one lit cell. */
public enum class CellShape {
    /** A filled square, so neighbouring cells join into solid areas. */
    Square,

    /** A round dot inscribed in the cell, which reads as halftone. */
    Dot,
}

/**
 * The colors and the grain of a dither. Provide one with [StippleTheme] and every dither below it follows it, the
 * way dark mode follows a color scheme.
 *
 * @property ink The color of a fully inked cell. The tone 1.
 * @property paper The color behind the dots. The tone 0.
 * @property palette Optional colors for tones in between, ordered from paper to ink. With two or more entries it
 *   replaces [paper] and [ink]: the tone picks a position on the ramp and the pattern decides between its two
 *   neighbours.
 * @property cell The side of one cell, so how coarse the dither looks. Cells are never smaller than one pixel.
 * @property shape How a lit cell is drawn.
 */
@Immutable
public class StippleTheme(
    public val ink: Color,
    public val paper: Color,
    public val palette: List<Color>? = null,
    public val cell: Dp = 3.dp,
    public val shape: CellShape = CellShape.Square,
) {
    /** The colors of the ramp from paper to ink, at least two. */
    internal val ramp: List<Color> = palette?.takeIf { it.size >= 2 } ?: listOf(paper, ink)

    /**
     * The WCAG contrast ratio between [ink] and [paper], from 1 to 21. Text needs 4.5, large text and graphics 3.
     * A dither draws less than full ink, so what a viewer sees is lower still.
     */
    public val contrastRatio: Float
        get() = contrastRatio(ink, paper)

    /** A copy with the given values changed. */
    public fun copy(
        ink: Color = this.ink,
        paper: Color = this.paper,
        palette: List<Color>? = this.palette,
        cell: Dp = this.cell,
        shape: CellShape = this.shape,
    ): StippleTheme = StippleTheme(ink, paper, palette, cell, shape)

    /**
     * This theme when [contrastRatio] already reaches [minContrast] (4.5 is the WCAG level for text), otherwise a
     * copy with the same paper, black or white ink, whichever contrasts more, and no palette.
     */
    public fun ensureContrast(minContrast: Float = 4.5f): StippleTheme {
        if (contrastRatio >= minContrast) return this
        val ink =
            if (contrastRatio(Color.Black, paper) >=
                contrastRatio(Color.White, paper)
            ) {
                Color.Black
            } else {
                Color.White
            }
        return copy(ink = ink, palette = null)
    }

    override fun equals(other: Any?): Boolean =
        other is StippleTheme &&
            ink == other.ink &&
            paper == other.paper &&
            palette == other.palette &&
            cell == other.cell &&
            shape == other.shape

    override fun hashCode(): Int {
        var result = ink.hashCode()
        result = 31 * result + paper.hashCode()
        result = 31 * result + (palette?.hashCode() ?: 0)
        result = 31 * result + cell.hashCode()
        result = 31 * result + shape.hashCode()
        return result
    }

    public companion object {
        /** Near-black ink on white paper. */
        public val Light: StippleTheme = StippleTheme(ink = Color(0xFF09090B), paper = Color(0xFFFFFFFF))

        /** Off-white ink on near-black paper. */
        public val Dark: StippleTheme = StippleTheme(ink = Color(0xFFFAFAFA), paper = Color(0xFF09090B))

        /** Four greens in the spirit of a handheld LCD, on a fine 2dp grain. */
        public val GameBoy: StippleTheme =
            StippleTheme(
                ink = Color(0xFF0F380F),
                paper = Color(0xFF9BBC0F),
                palette = listOf(Color(0xFF9BBC0F), Color(0xFF8BAC0F), Color(0xFF306230), Color(0xFF0F380F)),
                cell = 2.dp,
            )

        /** White, cyan, magenta and black, the look of a 1980s PC graphics card. */
        public val Cga: StippleTheme =
            StippleTheme(
                ink = Color(0xFF000000),
                paper = Color(0xFFFFFFFF),
                palette = listOf(Color(0xFFFFFFFF), Color(0xFF55FFFF), Color(0xFFFF55FF), Color(0xFF000000)),
                cell = 2.dp,
            )

        /** Near-black ink on warm off-white paper, with round dots like a printed page. */
        public val Newsprint: StippleTheme =
            StippleTheme(ink = Color(0xFF1C1B18), paper = Color(0xFFE8E2D0), cell = 2.dp, shape = CellShape.Dot)

        /** Pure black on white with a coarse 2dp grain, like early desktop bitmaps. */
        public val Macintosh: StippleTheme =
            StippleTheme(ink = Color(0xFF000000), paper = Color(0xFFFFFFFF), cell = 2.dp)

        /** Amber phosphor ink on a near-black screen. */
        public val Amber: StippleTheme = StippleTheme(ink = Color(0xFFFFB000), paper = Color(0xFF1A0F00), cell = 2.dp)

        /** The preset themes by readable name, in a stable order, for lists and galleries. */
        public val presets: Map<String, StippleTheme> =
            linkedMapOf(
                "Game Boy" to GameBoy,
                "CGA" to Cga,
                "Newsprint" to Newsprint,
                "Macintosh" to Macintosh,
                "Amber" to Amber,
            )

        /**
         * A theme whose palette blends evenly from [paper] to [ink] in [steps] colours.
         *
         * @throws IllegalArgumentException when [steps] is outside 2..8, the most the shader draws.
         */
        public fun gradient(
            paper: Color,
            ink: Color,
            steps: Int = 4,
            cell: Dp = 3.dp,
            shape: CellShape = CellShape.Square,
        ): StippleTheme {
            require(steps in 2..8) { "A gradient needs 2 to 8 steps, was $steps" }
            val palette = List(steps) { blend(paper, ink, it / (steps - 1f)) }
            return StippleTheme(ink, paper, palette, cell, shape)
        }
    }
}

/** The theme dithers read. Defaults to [StippleTheme.Light]; use [ProvideStippleTheme] or [systemStippleTheme]. */
public val LocalStippleTheme: ProvidableCompositionLocal<StippleTheme> = compositionLocalOf { StippleTheme.Light }

/** [StippleTheme.Dark] when the system is in dark mode, otherwise [StippleTheme.Light]. */
@Composable
public fun systemStippleTheme(): StippleTheme = if (isSystemInDarkTheme()) StippleTheme.Dark else StippleTheme.Light

/** Provides [theme] to every dither in [content]. Without an argument it follows the system's light or dark mode. */
@Composable
public fun ProvideStippleTheme(
    theme: StippleTheme = systemStippleTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalStippleTheme provides theme, content = content)
}

/** [from] to [to] by [fraction], channel by channel in sRGB, the space the renderers quantise in. */
private fun blend(
    from: Color,
    to: Color,
    fraction: Float,
): Color =
    Color(
        red = from.red + (to.red - from.red) * fraction,
        green = from.green + (to.green - from.green) * fraction,
        blue = from.blue + (to.blue - from.blue) * fraction,
        alpha = from.alpha + (to.alpha - from.alpha) * fraction,
    )

/** The WCAG contrast ratio between two colors, from 1 to 21. */
public fun contrastRatio(
    a: Color,
    b: Color,
): Float {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
}

private fun relativeLuminance(color: Color): Float {
    fun linear(channel: Float): Float =
        if (channel <= 0.03928f) channel / 12.92f else powf((channel + 0.055f) / 1.055f, 2.4f)
    return 0.2126f * linear(color.red) + 0.7152f * linear(color.green) + 0.0722f * linear(color.blue)
}

private fun powf(
    base: Float,
    exponent: Float,
): Float = base.toDouble().pow(exponent.toDouble()).toFloat()
