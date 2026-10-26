package com.jantursky.stipple.internal

/**
 * The ranks of a Bayer matrix with [size] cells per side (a power of two), row by row. Each tile is built from four
 * copies of the half-size tile, spaced so that consecutive ranks land as far apart as they can.
 */
internal fun bayerRanks(size: Int): IntArray {
    require(size >= 2 && size and (size - 1) == 0) { "Bayer size must be a power of two, was $size" }
    var side = 2
    var ranks = intArrayOf(0, 2, 3, 1)
    while (side < size) {
        val next = side * 2
        val grown = IntArray(next * next)
        for (y in 0 until next) {
            for (x in 0 until next) {
                val base = ranks[(y % side) * side + (x % side)] * 4
                val quadrant = (y / side) * 2 + (x / side)
                grown[y * next + x] = base + intArrayOf(0, 2, 3, 1)[quadrant]
            }
        }
        ranks = grown
        side = next
    }
    return ranks
}
