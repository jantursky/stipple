package com.jantursky.stipple

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MeshFieldTest {
    private val tones =
        listOf(
            listOf(0f, 0.5f, 1f),
            listOf(0.25f, 0.9f, 0.1f),
            listOf(1f, 0f, 0.6f),
        )

    private fun mesh(
        drift: Float = 0f,
        speed: Float = 0.2f,
    ) = DitherFields.mesh(tones, drift, speed)

    @Test
    fun aControlPointHasExactlyItsTone() {
        val field = mesh()
        for (r in 0..2) {
            for (c in 0..2) {
                assertEquals(tones[r][c], field.tone(c / 2f, r / 2f, 0f, 1f), 1e-6f, "point $r,$c")
            }
        }
    }

    @Test
    fun betweenTwoPointsTheToneLiesBetweenThem() {
        val field = mesh()
        // Along the top edge from 0 to 0.5: the tone rises with no step.
        var last = field.tone(0f, 0f, 0f, 1f)
        for (i in 1..50) {
            val tone = field.tone(i / 100f, 0f, 0f, 1f)
            assertTrue(tone in last..0.5f + 1e-6f, "step at $i: $last to $tone")
            last = tone
        }
    }

    @Test
    fun theSurfaceIsContinuous() {
        val field = mesh()
        var last = field.tone(0f, 0.3f, 0f, 1f)
        for (i in 1..1000) {
            val tone = field.tone(i / 1000f, 0.3f, 0f, 1f)
            assertTrue(kotlin.math.abs(tone - last) < 0.02f, "jump at $i")
            last = tone
        }
    }

    @Test
    fun noToneLeavesTheRangeOfTheControlTones() {
        for (drift in listOf(0f, 0.15f)) {
            val field = mesh(drift)
            for (i in 0..40) {
                for (j in 0..40) {
                    for (t in listOf(0f, 1.3f, 7f)) {
                        val tone = field.tone(i / 40f, j / 40f, t, 1.6f)
                        assertTrue(tone in -1e-6f..1f + 1e-6f, "tone $tone out of range at $i,$j,$t with drift $drift")
                    }
                }
            }
        }
    }

    @Test
    fun theEdgePointsStayPutWhileTheInteriorMoves() {
        val still = mesh(0f)
        val moving = mesh(0.15f)
        for (t in listOf(0.7f, 3.1f)) {
            for (i in 0..10) {
                val e = i / 10f
                assertEquals(still.tone(e, 0f, 0f, 1f), moving.tone(e, 0f, t, 1f), 1e-6f)
                assertEquals(still.tone(e, 1f, 0f, 1f), moving.tone(e, 1f, t, 1f), 1e-6f)
                assertEquals(still.tone(0f, e, 0f, 1f), moving.tone(0f, e, t, 1f), 1e-6f)
                assertEquals(still.tone(1f, e, 0f, 1f), moving.tone(1f, e, t, 1f), 1e-6f)
            }
        }
        var moved = 0f
        for (t in listOf(0.7f, 3.1f)) {
            moved +=
                kotlin.math.abs(still.tone(0.5f, 0.5f, 0f, 1f) - moving.tone(0.5f, 0.5f, t, 1f))
        }
        assertTrue(moved > 0.01f, "the centre did not move")
    }

    @Test
    fun theFieldIsAPureFunctionOfPositionAndTime() {
        val a = mesh(0.1f)
        val b = mesh(0.1f)
        assertEquals(a.tone(0.37f, 0.62f, 2.5f, 1f), b.tone(0.37f, 0.62f, 2.5f, 1f))
        assertEquals(a.tone(0.37f, 0.62f, 2.5f, 1f), a.tone(0.37f, 0.62f, 2.5f, 1f))
    }

    @Test
    fun aGridOfTheLimitsIsAccepted() {
        DitherFields.mesh(List(2) { List(2) { 0.5f } })
        DitherFields.mesh(List(8) { List(8) { 0.5f } })
    }

    @Test
    fun aBadGridIsRejectedWithAMessageThatNamesTheProblem() {
        val tooSmall = assertFailsWith<IllegalArgumentException> { DitherFields.mesh(listOf(listOf(0.5f, 0.5f))) }
        assertTrue("2 to 8" in tooSmall.message.orEmpty())
        assertFailsWith<IllegalArgumentException> { DitherFields.mesh(emptyList()) }
        assertFailsWith<IllegalArgumentException> { DitherFields.mesh(List(9) { List(2) { 0.5f } }) }
        assertFailsWith<IllegalArgumentException> { DitherFields.mesh(List(2) { List(9) { 0.5f } }) }
        val ragged =
            assertFailsWith<IllegalArgumentException> {
                DitherFields.mesh(listOf(listOf(0f, 1f), listOf(0f, 1f, 0f)))
            }
        assertTrue("same number" in ragged.message.orEmpty())
    }
}
