package com.jantursky.stipple

/**
 * The tone each cell is dithered against: 0 is paper, 1 is ink, and values between become a mix of both by the
 * [DitherPattern].
 *
 * A field is a pure function, so it works in common code and is easy to test. Keep it smooth: return a continuous
 * value and let the pattern make the dots. Values outside 0..1 are clamped.
 */
public fun interface DitherField {
    /**
     * The tone at a point.
     *
     * @param x Horizontal position, 0 at the left edge and 1 at the right edge of the element.
     * @param y Vertical position, 0 at the top edge and 1 at the bottom edge.
     * @param t Time in seconds. Static fields ignore it.
     * @param aspect Width divided by height. Multiply `x` differences by it before measuring a distance, or circles
     *   turn into ellipses on wide elements.
     */
    public fun tone(
        x: Float,
        y: Float,
        t: Float,
        aspect: Float,
    ): Float
}
