package com.jantursky.stipple

/**
 * A threshold map. A cell is ink when the field's tone is above the pattern's threshold for that cell, so the
 * pattern decides where dots appear as the tone rises.
 *
 * The map repeats every [size] cells in both directions. Implement it to add your own pattern.
 */
public interface DitherPattern {
    /** Width and height of the repeating tile, in cells. */
    public val size: Int

    /** The threshold for the cell at ([x], [y]), in the open interval (0, 1). Coordinates may be any integers. */
    public fun threshold(
        x: Int,
        y: Int,
    ): Float
}
