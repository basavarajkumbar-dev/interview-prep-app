package com.interviewprep.core.domain

import com.interviewprep.core.model.Achievement
import kotlin.math.floor
import kotlin.math.sqrt

object Xp {
    const val LESSON = 10
    const val QUIZ_CORRECT = 5
    const val QUIZ_ANSWERED = 1
    const val CARD_REVIEW = 2
    const val QUESTION_PRACTICE = 6
    const val MOCK_ANSWER = 10
    const val SCENARIO = 15
    const val CODING_PASS = 25
    const val DAILY_GOAL = 30

    /** Level n needs 50·(n-1)² XP: 0, 50, 200, 450, 800, ... */
    fun level(xp: Int): Int = floor(sqrt(xp / 50.0)).toInt() + 1
    fun xpForLevel(level: Int): Int = 50 * (level - 1) * (level - 1)

    fun progressInLevel(xp: Int): Float {
        val l = level(xp)
        val lo = xpForLevel(l)
        val hi = xpForLevel(l + 1)
        return ((xp - lo).toFloat() / (hi - lo)).coerceIn(0f, 1f)
    }

    fun title(level: Int): String = when {
        level < 3 -> "Bug Spotter"
        level < 6 -> "Test Apprentice"
        level < 9 -> "Script Smith"
        level < 13 -> "Automation Warrior"
        level < 18 -> "Protocol Pro"
        level < 24 -> "Validation Veteran"
        else -> "Interview Ready"
    }
}

data class AchievementStats(
    val quizzesCompleted: Int,
    val questionsAnswered: Int,
    val bestStreak: Int,
    val pythonMastery: Double, // 0..1 average of Python track
    val mockInterviewsCompleted: Int,
    val quizAccuracyOver20: Double?, // accuracy once at least 20 answered
    val lessonsCompleted: Int,
    val codingSolved: Int,
    val scenariosDone: Int,
    val cardsReviewed: Int,
)

object Achievements {
    val all = listOf(
        Achievement("first_quiz", "🏆", "First Quiz", "Complete your first quiz"),
        Achievement("q100", "💯", "100 Questions", "Answer 100 quiz questions"),
        Achievement("streak7", "🔥", "7 Day Streak", "Study 7 days in a row"),
        Achievement("python_master", "🐍", "Python Master", "Reach 85% mastery across Python Core"),
        Achievement("mock_done", "🎤", "Mock Interview Completed", "Finish a full mock interview"),
        Achievement("acc90", "🎯", "90% Quiz Accuracy", "Keep 90% accuracy over 20+ answers"),
        Achievement("lessons25", "📚", "Bookworm", "Complete 25 lessons"),
        Achievement("coder10", "💻", "Problem Solver", "Pass 10 coding challenges"),
        Achievement("scenario10", "🔍", "Troubleshooter", "Complete 10 scenarios"),
        Achievement("cards200", "🃏", "Card Shark", "Review 200 flashcards"),
    )

    fun earned(s: AchievementStats): Set<String> = buildSet {
        if (s.quizzesCompleted >= 1) add("first_quiz")
        if (s.questionsAnswered >= 100) add("q100")
        if (s.bestStreak >= 7) add("streak7")
        if (s.pythonMastery >= 0.85) add("python_master")
        if (s.mockInterviewsCompleted >= 1) add("mock_done")
        if ((s.quizAccuracyOver20 ?: 0.0) >= 0.9) add("acc90")
        if (s.lessonsCompleted >= 25) add("lessons25")
        if (s.codingSolved >= 10) add("coder10")
        if (s.scenariosDone >= 10) add("scenario10")
        if (s.cardsReviewed >= 200) add("cards200")
    }
}
