package com.jantursky.stipple.internal

import kotlin.math.floor

/**
 * The index of the ramp color a cell gets. [tone] picks a position on a ramp of [levels] colors and [threshold]
 * decides between the two colors around it, so with two levels the cell is ink when the tone is above the threshold.
 */
internal fun quantize(
    tone: Float,
    threshold: Float,
    levels: Int,
): Int {
    val top = levels - 1
    val position = tone.coerceIn(0f, 1f) * top
    val base = floor(position).toInt()
    if (base >= top) return top
    return if (position - base > threshold) base + 1 else base
}
