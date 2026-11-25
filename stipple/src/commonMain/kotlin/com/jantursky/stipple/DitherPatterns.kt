package com.jantursky.stipple

import com.jantursky.stipple.internal.BLUE_NOISE_HEX
import com.jantursky.stipple.internal.BLUE_NOISE_SIZE
import com.jantursky.stipple.internal.bayerRanks
import com.jantursky.stipple.internal.crosshatchRanks
import com.jantursky.stipple.internal.halftoneRanks
import com.jantursky.stipple.internal.hexGridRanks
import com.jantursky.stipple.internal.lineRanks
import com.jantursky.stipple.internal.shuffledRanks

/** The patterns that come with Stipple. */
public object DitherPatterns {
    /** Ordered 2x2 Bayer matrix: four tone steps, a coarse and obvious grid. */
    public val Bayer2: DitherPattern = TiledPattern(2, bayerRanks(2))

    /** Ordered 4x4 Bayer matrix: sixteen tone steps. The default. */
    public val Bayer4: DitherPattern = TiledPattern(4, bayerRanks(4))

    /** Ordered 8x8 Bayer matrix: 64 tone steps and a finer, smoother crosshatch. */
    public val Bayer8: DitherPattern = TiledPattern(8, bayerRanks(8))

    /**
     * A 64x64 blue-noise texture made with void-and-cluster (`scripts/blue-noise.py`). It has no visible grid, so
     * it looks closer to film grain than the Bayer patterns do.
     */
    public val BlueNoise: DitherPattern =
        TiledPattern(
            BLUE_NOISE_SIZE,
            IntArray(BLUE_NOISE_SIZE * BLUE_NOISE_SIZE) { BLUE_NOISE_HEX.substring(it * 4, it * 4 + 4).toInt(16) },
        )

    /** Ordered 16x16 Bayer matrix: 256 tone steps and a very fine, even crosshatch. */
    public val Bayer16: DitherPattern by lazy { TiledPattern(16, bayerRanks(16)) }

    /** A clustered-dot screen at 45 degrees on an 8x8 tile, like newsprint: dots grow from the centre until they touch. */
    public val Halftone: DitherPattern by lazy { halftone(8, diagonal = true) }

    /** Dots on a staggered, hexagon-like lattice, tile 8x8. */
    public val HexGrid: DitherPattern by lazy { TiledPattern(8, hexGridRanks(8)) }

    /** Diagonal line screen, tile 8x8: lines that widen as the tone rises. */
    public val Lines: DitherPattern by lazy { lines(8, diagonal = true) }

    /** Diagonal crosshatch, tile 8x8: one set of lines first, then the cross lines. */
    public val Crosshatch: DitherPattern by lazy { TiledPattern(8, crosshatchRanks(8)) }

    /** Uncorrelated noise from a fixed seed, a 32x32 tile that is identical on every platform. */
    public val WhiteNoise: DitherPattern by lazy { TiledPattern(32, shuffledRanks(32, seed = 20_241_107L)) }

    /**
     * A clustered-dot halftone with a [size] x [size] tile. The screen is at 45 degrees when [diagonal] is true,
     * otherwise axis aligned. Other angles would show seams where the tile repeats.
     *
     * @throws IllegalArgumentException when [size] is below 2 or above 256.
     */
    public fun halftone(
        size: Int,
        diagonal: Boolean = true,
    ): DitherPattern {
        requireTileSize(size)
        return TiledPattern(size, halftoneRanks(size, diagonal))
    }

    /**
     * A line screen with a [size] x [size] tile, diagonal when [diagonal] is true and horizontal otherwise.
     *
     * @throws IllegalArgumentException when [size] is below 2 or above 256.
     */
    public fun lines(
        size: Int,
        diagonal: Boolean = true,
    ): DitherPattern {
        requireTileSize(size)
        return TiledPattern(size, lineRanks(size, diagonal))
    }

    private fun requireTileSize(size: Int) = require(size in 2..256) { "Tile size must be 2 to 256, was $size" }

    /** Every built-in pattern, ordered patterns first, for lists and galleries. */
    public val All: List<DitherPattern> by lazy {
        listOf(Bayer2, Bayer4, Bayer8, BlueNoise, Bayer16, Halftone, HexGrid, Lines, Crosshatch, WhiteNoise)
    }

    /** A readable name for a built-in pattern, or `"Custom"`. */
    public fun nameOf(pattern: DitherPattern): String =
        when (pattern) {
            Bayer2 -> "Bayer 2x2"
            Bayer4 -> "Bayer 4x4"
            Bayer8 -> "Bayer 8x8"
            BlueNoise -> "Blue noise"
            Bayer16 -> "Bayer 16x16"
            Halftone -> "Halftone"
            HexGrid -> "Hex grid"
            Lines -> "Lines"
            Crosshatch -> "Crosshatch"
            WhiteNoise -> "White noise"
            else -> "Custom"
        }
}

/** A square tile of distinct ranks 0 until size*size, read as thresholds (rank + 0.5) / (size*size). */
internal class TiledPattern(
    override val size: Int,
    private val ranks: IntArray,
) : DitherPattern {
    private val scale = 1f / (size * size)

    override fun threshold(
        x: Int,
        y: Int,
    ): Float = (ranks[y.mod(size) * size + x.mod(size)] + 0.5f) * scale
}
