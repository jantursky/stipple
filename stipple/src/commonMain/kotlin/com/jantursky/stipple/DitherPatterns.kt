package com.jantursky.stipple

import com.jantursky.stipple.internal.BLUE_NOISE_HEX
import com.jantursky.stipple.internal.BLUE_NOISE_SIZE
import com.jantursky.stipple.internal.bayerRanks

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

    /** Every built-in pattern, from the coarsest, for lists and galleries. */
    public val All: List<DitherPattern> = listOf(Bayer2, Bayer4, Bayer8, BlueNoise)

    /** A readable name for a built-in pattern, or `"Custom"`. */
    public fun nameOf(pattern: DitherPattern): String =
        when (pattern) {
            Bayer2 -> "Bayer 2x2"
            Bayer4 -> "Bayer 4x4"
            Bayer8 -> "Bayer 8x8"
            BlueNoise -> "Blue noise"
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
