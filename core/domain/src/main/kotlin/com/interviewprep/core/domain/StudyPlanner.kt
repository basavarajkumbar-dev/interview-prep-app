package com.interviewprep.core.domain

import com.interviewprep.core.model.ActivityKind
import kotlin.math.roundToInt

data class PlanBlock(val kind: ActivityKind, val minutes: Int, val title: String, val reason: String, val topicId: String? = null)

data class DailyPlan(val totalMinutes: Int, val blocks: List<PlanBlock>)

data class PlannerSignals(
    val dailyMinutes: Int, // 30, 45, 60 or 90 (90 is the recommended maximum)
    val dueCards: Int,
    val weakTopic: Pair<String, String>?, // id to title
    val continueTopic: Pair<String, String>?, // id to title
    val recentQuizAccuracy: Double?,
    val interviewPracticeCount7d: Int,
    val scenarioCount7d: Int,
)

/**
 * Adaptive daily plan. The 90-minute template is 20 learn / 20 revision / 20 quiz /
 * 20 interview questions / 10 weak topic; shorter budgets scale it, then today's
 * signals shift time to where it helps most.
 */
object StudyPlanner {
    const val MAX_MINUTES = 90
    val OPTIONS = listOf(30, 45, 60, 90)

    fun plan(s: PlannerSignals): DailyPlan {
        val total = s.dailyMinutes.coerceIn(15, MAX_MINUTES)
        val share = mutableMapOf(
            ActivityKind.LESSON to 20.0,
            ActivityKind.FLASHCARDS to 20.0,
            ActivityKind.QUIZ to 20.0,
            ActivityKind.QUESTIONS to 20.0,
            ActivityKind.REVISION to 10.0, // weak-topic revision
        )
        fun move(from: ActivityKind, to: ActivityKind, amount: Double) {
            val a = minOf(amount, share.getValue(from) - 5.0).coerceAtLeast(0.0)
            share[from] = share.getValue(from) - a
            share[to] = share.getValue(to) + a
        }
        if (s.weakTopic == null) {
            share[ActivityKind.LESSON] = share.getValue(ActivityKind.LESSON) + share.getValue(ActivityKind.REVISION)
            share[ActivityKind.REVISION] = 0.0
        }
        if (s.dueCards > 40) move(ActivityKind.LESSON, ActivityKind.FLASHCARDS, 5.0)
        if (s.dueCards < 5) move(ActivityKind.FLASHCARDS, ActivityKind.QUIZ, 8.0)
        s.recentQuizAccuracy?.let {
            if (it < 0.6) move(ActivityKind.QUIZ, ActivityKind.LESSON, 5.0) // re-learn before re-testing
            if (it > 0.85) move(ActivityKind.LESSON, ActivityKind.QUESTIONS, 5.0) // ready for harder practice
        }
        if (s.interviewPracticeCount7d == 0) move(ActivityKind.QUIZ, ActivityKind.QUESTIONS, 3.0)
        val scale = total / 90.0
        var blocks = share.entries.map { (k, v) -> k to (v * scale) }.filter { it.second >= 4.0 }
            .map { (k, v) -> k to (v / 5.0).roundToInt() * 5 }.filter { it.second > 0 }
        // fix rounding so the plan adds up exactly
        val diff = total - blocks.sumOf { it.second }
        if (blocks.isNotEmpty() && diff != 0) {
            val (k, v) = blocks.maxBy { it.second }
            blocks = blocks.map { if (it.first == k) k to (v + diff) else it }
        }
        val order = listOf(ActivityKind.FLASHCARDS, ActivityKind.LESSON, ActivityKind.QUIZ, ActivityKind.QUESTIONS, ActivityKind.REVISION)
        val out = blocks.sortedBy { order.indexOf(it.first) }.map { (k, m) ->
            when (k) {
                ActivityKind.FLASHCARDS -> PlanBlock(k, m, "Flashcard revision", if (s.dueCards > 0) "${s.dueCards} cards due" else "Keep memory fresh")
                ActivityKind.LESSON -> PlanBlock(k, m, s.continueTopic?.let { "Learn: ${it.second}" } ?: "Learn something new", "Continue where you left off", s.continueTopic?.first)
                ActivityKind.QUIZ -> PlanBlock(k, m, "Adaptive quiz", s.recentQuizAccuracy?.let { "Recent accuracy ${(it * 100).roundToInt()}%" } ?: "Find your level")
                ActivityKind.QUESTIONS -> PlanBlock(k, m, "Interview questions", "Answer aloud or type, then compare")
                ActivityKind.REVISION -> PlanBlock(k, m, s.weakTopic?.let { "Weak topic: ${it.second}" } ?: "Weak topics", "Lowest mastery right now", s.weakTopic?.first)
                else -> PlanBlock(k, m, k.name, "")
            }
        }
        return DailyPlan(total, out)
    }
}
