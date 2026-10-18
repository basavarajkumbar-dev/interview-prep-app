package com.interviewprep.core.domain

import kotlin.math.roundToInt

data class ReadinessInputs(
    val learningCompletion: Double, // 0..1 of lessons in the user's role tracks
    val quizAccuracy: Double?, // recent, 0..1
    val interviewScore: Double?, // mock interview / question practice average, 0..1
    val scenarioScore: Double?, // 0..1
    val codingPassRate: Double?, // 0..1
    val weakTopicPerformance: Double?, // average mastery of the 5 weakest practised topics, 0..1
)

data class ReadinessComponent(val label: String, val weight: Double, val value: Double?, val hint: String)

data class Readiness(
    val score: Int, // 0..100
    val band: ReadinessBand,
    val components: List<ReadinessComponent>,
)

enum class ReadinessBand(val label: String, val emoji: String) {
    STARTING("Getting started", "⚪"),
    BUILDING("Building", "🔴"),
    FAIR("Fair", "🟡"),
    GOOD("Good", "🟢"),
    READY("Interview ready", "🏆"),
}

/**
 * Readiness = weighted blend of six signals. A signal you haven't practised counts as 0
 * (you can't be ready for a round you've never tried), and the hint says what to do.
 */
object ReadinessCalculator {
    fun compute(i: ReadinessInputs): Readiness {
        val comps = listOf(
            ReadinessComponent("Learning completion", 0.20, i.learningCompletion, "Finish lessons in your target tracks"),
            ReadinessComponent("Quiz performance", 0.25, i.quizAccuracy, "Take quizzes; aim for 80%+"),
            ReadinessComponent("Interview answers", 0.20, i.interviewScore, "Practise typed answers or a mock interview"),
            ReadinessComponent("Scenarios", 0.15, i.scenarioScore, "Work through troubleshooting scenarios"),
            ReadinessComponent("Coding", 0.10, i.codingPassRate, "Solve coding challenges"),
            ReadinessComponent("Weak topics", 0.10, i.weakTopicPerformance, "Revise your weakest topics"),
        )
        val raw = comps.sumOf { (it.value ?: 0.0).coerceIn(0.0, 1.0) * it.weight }
        val score = (raw * 100).roundToInt().coerceIn(0, 100)
        val band = when {
            comps.count { it.value != null } <= 1 && score < 20 -> ReadinessBand.STARTING
            score < 45 -> ReadinessBand.BUILDING
            score < 65 -> ReadinessBand.FAIR
            score < 85 -> ReadinessBand.GOOD
            else -> ReadinessBand.READY
        }
        return Readiness(score, band, comps)
    }
}
