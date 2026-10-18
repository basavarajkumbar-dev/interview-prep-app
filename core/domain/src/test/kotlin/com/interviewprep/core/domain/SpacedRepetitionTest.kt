package com.interviewprep.core.domain

import com.interviewprep.core.model.CardRating
import com.interviewprep.core.model.CardSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpacedRepetitionTest {
    private val now = 1_000_000_000L

    @Test
    fun knowGrowsInterval_1_6_thenEase() {
        var s = CardSchedule("c")
        s = SpacedRepetition.schedule(s, CardRating.KNOW, now)
        assertEquals(1, s.intervalDays)
        s = SpacedRepetition.schedule(s, CardRating.KNOW, now)
        assertEquals(6, s.intervalDays)
        s = SpacedRepetition.schedule(s, CardRating.KNOW, now)
        assertTrue("interval grows beyond 6", s.intervalDays > 6)
        assertEquals(now + s.intervalDays * SpacedRepetition.DAY, s.dueAtMillis)
    }

    @Test
    fun dontKnowComesBackIn10MinutesAndCountsLapse() {
        val s = SpacedRepetition.schedule(CardSchedule("c", repetitions = 3, intervalDays = 20), CardRating.DONT_KNOW, now)
        assertEquals(0, s.repetitions)
        assertEquals(1, s.lapses)
        assertEquals(now + 10 * SpacedRepetition.MINUTE, s.dueAtMillis)
    }

    @Test
    fun easeNeverDropsBelowMinimum() {
        var s = CardSchedule("c")
        repeat(20) { s = SpacedRepetition.schedule(s, CardRating.DONT_KNOW, now) }
        assertTrue(s.easiness >= SpacedRepetition.MIN_EASE)
    }

    @Test
    fun reviewIsTomorrow() {
        val s = SpacedRepetition.schedule(CardSchedule("c"), CardRating.REVIEW, now)
        assertEquals(now + SpacedRepetition.DAY, s.dueAtMillis)
    }

    @Test
    fun sessionOrderPutsLapsedDueCardsFirstThenNew() {
        val schedules = mapOf(
            "a" to CardSchedule("a", dueAtMillis = now - 5, lapses = 0),
            "b" to CardSchedule("b", dueAtMillis = now - 1, lapses = 3),
            "c" to CardSchedule("c", dueAtMillis = now + 100_000),
        )
        val order = SpacedRepetition.orderForSession(listOf("a", "b", "c", "n1", "n2"), schedules, now, limit = 10)
        assertEquals(listOf("b", "a", "n1", "n2"), order)
    }
}
