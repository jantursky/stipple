package com.jantursky.stipple

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DissolveFieldTest {
    private fun at(
        progress: Float,
        x: Float,
        y: Float,
    ) = DitherFields.dissolve({ progress }).tone(x, y, 0f, 1.5f)

    private val points = (0..30).flatMap { i -> (0..30).map { j -> i / 30f to j / 30f } }

    @Test
    fun nothingIsLitAtZero() {
        for ((x, y) in points) assertEquals(0f, at(0f, x, y), "at $x,$y")
    }

    @Test
    fun everythingIsLitAtOne() {
        for ((x, y) in points) assertEquals(1f, at(1f, x, y), "at $x,$y")
    }

    @Test
    fun aPointOnlyEverGetsDarker() {
        for ((x, y) in points) {
            var last = 0f
            for (step in 0..20) {
                val tone = at(step / 20f, x, y)
                assertTrue(tone >= last, "tone fell from $last to $tone at $x,$y, step $step")
                last = tone
            }
        }
    }

    @Test
    fun theOrderIsIrregularNotAStraightEdge() {
        // At half progress some points are fully on and some fully off on the same column.
        val column = (0..40).map { at(0.5f, 0.5f, it / 40f) }
        assertTrue(column.any { it < 0.2f } && column.any { it > 0.8f }, "column is $column")
        // And it is not left to right: the left and right halves light up about equally.
        val left = points.filter { it.first < 0.5f }.map { at(0.5f, it.first, it.second) }.average()
        val right = points.filter { it.first >= 0.5f }.map { at(0.5f, it.first, it.second) }.average()
        assertTrue(kotlin.math.abs(left - right) < 0.15, "left $left, right $right")
    }

    @Test
    fun progressIsReadEveryTimeTheFieldIsDrawn() {
        var progress = 0f
        val field = DitherFields.dissolve({ progress })
        assertEquals(0f, field.tone(0.3f, 0.3f, 0f, 1f))
        progress = 1f
        assertEquals(1f, field.tone(0.3f, 0.3f, 0f, 1f))
    }
}
