package com.jantursky.stipple.internal

import kotlin.math.abs
import kotlin.math.min

/** Ranks 0 until size of [scores], lowest score first; equal scores keep their index order, so ranks are distinct. */
internal fun ranksByScore(scores: DoubleArray): IntArray {
    val order = scores.indices.sortedWith(compareBy<Int> { scores[it] }.thenBy { it })
    val ranks = IntArray(scores.size)
    for ((rank, index) in order.withIndex()) ranks[index] = rank
    return ranks
}

/** Distance between two coordinates on a ring of [size] cells. */
private fun ring(
    a: Double,
    b: Double,
    size: Int,
): Double {
    val d = abs(a - b) % size
    return min(d, size - d)
}

private fun scores(
    size: Int,
    score: (x: Int, y: Int) -> Double,
): DoubleArray = DoubleArray(size * size) { score(it % size, it / size) }

private fun nearest(
    size: Int,
    x: Int,
    y: Int,
    centres: List<Pair<Double, Double>>,
): Double =
    centres.minOf { (cx, cy) ->
        val dx = ring(x.toDouble(), cx, size)
        val dy = ring(y.toDouble(), cy, size)
        dx * dx + dy * dy
    }

/**
 * A clustered-dot screen: dots start at the tile centre (and, on the 45 degree screen, at the opposite corner too)
 * and grow until they touch, then the paper side fills in.
 */
internal fun halftoneRanks(
    size: Int,
    diagonal: Boolean,
): IntArray {
    val c = size / 2.0
    val centres =
        if (diagonal) listOf(c to c, (c + size / 2.0) % size to (c + size / 2.0) % size) else listOf(c to c)
    return ranksByScore(scores(size) { x, y -> nearest(size, x, y, centres) })
}

/** Dot centres on a staggered lattice: two rows per tile, the second shifted by half the dot spacing. */
internal fun hexGridRanks(size: Int): IntArray {
    val h = size / 2.0
    val q = size / 4.0
    val centres = listOf(0.0 to 0.0, h to 0.0, q to h, q + h to h)
    return ranksByScore(scores(size) { x, y -> nearest(size, x, y, centres) })
}

/** Lines through the tile centre, horizontal or on the 45 degree diagonal, widening as the tone rises. */
internal fun lineRanks(
    size: Int,
    diagonal: Boolean,
): IntArray {
    val c = size / 2.0
    return ranksByScore(scores(size) { x, y -> ring(if (diagonal) (x + y).toDouble() else y.toDouble(), c, size) })
}

/** One diagonal family of lines first; the cross family starts once the first has some width. */
internal fun crosshatchRanks(size: Int): IntArray {
    val c = size / 2.0
    val bias = size / 5.0
    return ranksByScore(
        scores(size) { x, y ->
            min(ring((x + y).toDouble(), c, size), ring((x - y).toDouble(), c, size) + bias)
        },
    )
}

/** A fixed-seed shuffle of 0 until size*size. The generator is written out so every platform gets the same tile. */
internal fun shuffledRanks(
    size: Int,
    seed: Long,
): IntArray {
    val ranks = IntArray(size * size) { it }
    var state = seed
    for (i in ranks.size - 1 downTo 1) {
        state = (state * 6364136223846793005L + 1442695040888963407L)
        val j = ((state ushr 33) % (i + 1)).toInt()
        val t = ranks[i]
        ranks[i] = ranks[j]
        ranks[j] = t
    }
    return ranks
}
