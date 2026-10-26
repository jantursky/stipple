package com.jantursky.stipple

/** The way [ditherProgress] fills an element as progress grows. */
public enum class ProgressDirection {
    /** From the left edge to the right edge. */
    Right,

    /** From the right edge to the left edge. */
    Left,

    /** From the top edge down. */
    Down,

    /** From the bottom edge up. */
    Up,
}
