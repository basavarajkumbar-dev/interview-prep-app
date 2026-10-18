package com.interviewprep.core.domain

import com.interviewprep.core.model.CardRating
import com.interviewprep.core.model.CardSchedule
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * SM-2 spaced repetition with a short relearning step:
 * - DON'T KNOW: the card comes back in 10 minutes and its interval resets (lapse).
 * - REVIEW: shown again tomorrow; ease drops slightly.
 * - KNOW: interval grows 1 → 6 → interval × ease days.
 */
object SpacedRepetition {
    const val MINUTE = 60_000L
    const val DAY = 24 * 60 * MINUTE
    const val MIN_EASE = 1.3

    fun schedule(current: CardSchedule, rating: CardRating, nowMillis: Long): CardSchedule {
        val q = rating.quality
        val ease = max(MIN_EASE, current.easiness + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)))
        return when (rating) {
            CardRating.DONT_KNOW -> current.copy(
                easiness = ease,
                repetitions = 0,
                intervalDays = 0,
                lapses = current.lapses + 1,
                dueAtMillis = nowMillis + 10 * MINUTE,
                lastRating = rating,
            )
            CardRating.REVIEW -> current.copy(
                easiness = ease,
                repetitions = current.repetitions, // not promoted
                intervalDays = 1,
                dueAtMillis = nowMillis + DAY,
                lastRating = rating,
            )
            CardRating.KNOW -> {
                val reps = current.repetitions + 1
                val interval = when (reps) {
                    1 -> 1
                    2 -> 6
                    else -> max(1, (current.intervalDays * ease).roundToInt())
                }
                current.copy(
                    easiness = ease,
                    repetitions = reps,
                    intervalDays = interval,
                    dueAtMillis = nowMillis + interval * DAY,
                    lastRating = rating,
                )
            }
        }
    }

    fun isDue(s: CardSchedule?, nowMillis: Long): Boolean = s == null || s.dueAtMillis <= nowMillis

    /**
     * Order a deck for a session: overdue cards first (most overdue first), then cards
     * marked "don't know" before, then new cards. Lapsed cards get extra weight so
     * difficult cards appear more often.
     */
    fun orderForSession(
        cardIds: List<String>,
        schedules: Map<String, CardSchedule>,
        nowMillis: Long,
        limit: Int,
        newCardLimit: Int = limit / 2,
    ): List<String> {
        val due = cardIds.mapNotNull { id -> schedules[id]?.takeIf { it.dueAtMillis <= nowMillis } }
            .sortedWith(compareByDescending<CardSchedule> { it.lapses }.thenBy { it.dueAtMillis })
            .map { it.cardId }
        val new = cardIds.filter { it !in schedules }.take(newCardLimit)
        return (due + new).distinct().take(limit)
    }
}
