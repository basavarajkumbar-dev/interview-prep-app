package com.interviewprep.core.domain

import com.interviewprep.core.model.QuizAttempt
import com.interviewprep.core.model.TopicPerformance
import kotlin.math.pow

enum class MasteryBand(val label: String, val emoji: String) {
    WEAK("Needs work", "🔴"), FAIR("Getting there", "🟡"), STRONG("Strong", "🟢"), UNSEEN("Not started", "⚪")
}

data class TopicMastery(val topicId: String, val score: Double, val band: MasteryBand, val evidence: Int)

object Mastery {
    /** Recency-weighted accuracy: the last attempt counts most (half-life of ~6 attempts). */
    fun recentAccuracy(attempts: List<QuizAttempt>): Double? {
        val graded = attempts.filter { !it.skipped }.sortedByDescending { it.answeredAtMillis }
        if (graded.isEmpty()) return null
        var num = 0.0
        var den = 0.0
        graded.take(40).forEachIndexed { i, a ->
            val w = 0.89.pow(i)
            den += w
            if (a.correct) num += w
        }
        return num / den
    }

    /**
     * 0..1 mastery from quiz accuracy, flashcards, interview answers and lessons read.
     * Missing signals are left out (weights renormalised) instead of counting as zero.
     */
    fun score(p: TopicPerformance): TopicMastery {
        val parts = mutableListOf<Pair<Double, Double>>() // value to weight
        p.recentAccuracy?.let { parts += it to 0.45 }
        if (p.cardsReviewed > 0) parts += (p.cardsKnown.toDouble() / p.cardsReviewed) to 0.2
        p.interviewAvg?.let { parts += it to 0.25 }
        if (p.lessonsTotal > 0) parts += p.lessonCompletion to 0.1
        val evidence = p.quizAttempts + p.cardsReviewed + p.lessonsDone
        if (parts.isEmpty() || evidence == 0) return TopicMastery(p.topicId, 0.0, MasteryBand.UNSEEN, 0)
        val w = parts.sumOf { it.second }
        val s = parts.sumOf { it.first * it.second } / w
        val band = when {
            p.quizAttempts + p.cardsReviewed < 3 && p.interviewAvg == null -> if (s >= 0.7) MasteryBand.FAIR else MasteryBand.UNSEEN
            s < 0.55 -> MasteryBand.WEAK
            s < 0.75 -> MasteryBand.FAIR
            else -> MasteryBand.STRONG
        }
        return TopicMastery(p.topicId, s, band, evidence)
    }

    /** Weak topics: enough evidence and a low score, weakest first. */
    fun weakTopics(all: List<TopicMastery>, limit: Int = 8): List<TopicMastery> =
        all.filter { it.band == MasteryBand.WEAK || (it.band == MasteryBand.FAIR && it.score < 0.65) }
            .sortedBy { it.score }
            .take(limit)

    fun strongTopics(all: List<TopicMastery>, limit: Int = 5): List<TopicMastery> =
        all.filter { it.band == MasteryBand.STRONG }.sortedByDescending { it.score }.take(limit)
}
