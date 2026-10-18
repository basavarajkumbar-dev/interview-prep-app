package com.interviewprep.core.model

/** User-generated data (progress, attempts, settings). Kept separate from content so
 *  content updates never wipe progress, and so it can be synced later. */

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserSettings(
    val name: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dailyGoalMinutes: Int = 60,
    val quizDifficulty: Difficulty? = null, // null = adaptive
    val targetRoles: Set<String> = emptySet(),
    val onboarded: Boolean = false,
    val explanationMode: ExplanationMode = ExplanationMode.SIMPLE,
)

enum class ExplanationMode { SIMPLE, INTERVIEW }

enum class CardRating(val quality: Int, val label: String) {
    KNOW(5, "Know"), REVIEW(3, "Review"), DONT_KNOW(1, "Don't know")
}

data class CardSchedule(
    val cardId: String,
    val easiness: Double = 2.5,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val dueAtMillis: Long = 0,
    val lapses: Int = 0,
    val lastRating: CardRating? = null,
)

data class QuizAttempt(
    val itemId: String,
    val topicId: String,
    val correct: Boolean,
    val difficulty: Difficulty,
    val answeredAtMillis: Long,
    val skipped: Boolean = false,
)

enum class BookmarkType(val label: String) {
    TOPIC("Topic"), LESSON("Lesson"), QUESTION("Question"), FLASHCARD("Flashcard"),
    CODING("Code"), SCENARIO("Scenario"), NOTE("Note");
}

data class Bookmark(
    val type: BookmarkType,
    val refId: String,
    val title: String,
    val createdAtMillis: Long,
)

enum class ActivityKind { LESSON, QUIZ, FLASHCARDS, QUESTIONS, MOCK_INTERVIEW, SCENARIO, CODING, REVISION }

data class StudySession(
    val kind: ActivityKind,
    val startedAtMillis: Long,
    val durationSeconds: Int,
    val topicId: String? = null,
)

/** Per-topic performance used for weak-topic detection, adaptivity and readiness. */
data class TopicPerformance(
    val topicId: String,
    val lessonsDone: Int,
    val lessonsTotal: Int,
    val quizAttempts: Int,
    val quizCorrect: Int,
    val recentAccuracy: Double?, // recency-weighted, null if no attempts
    val cardsKnown: Int,
    val cardsReviewed: Int,
    val interviewAvg: Double?, // 0..1
) {
    val lessonCompletion: Double get() = if (lessonsTotal == 0) 0.0 else lessonsDone.toDouble() / lessonsTotal
}

data class Achievement(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val unlockedAtMillis: Long? = null,
)
