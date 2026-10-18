package com.interviewprep.core.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/* User tables: progress and history. Never cleared by content updates.
 * Times are epoch millis; `day` columns are local epoch days for streaks and charts. */

@Entity(tableName = "lesson_progress", indices = [Index("topicId")])
data class LessonProgressEntity(
    @PrimaryKey val lessonId: String,
    val topicId: String,
    val completed: Boolean,
    val completedAt: Long?,
    val lastOpenedAt: Long,
)

@Entity(tableName = "quiz_attempts", indices = [Index("itemId"), Index("topicId"), Index("day")])
data class QuizAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: String,
    val topicId: String,
    val correct: Boolean,
    val score: Double,
    val difficulty: String,
    val skipped: Boolean,
    val sessionId: String?,
    val answeredAt: Long,
    val day: Long,
)

@Entity(tableName = "quiz_sessions")
data class QuizSessionEntity(
    @PrimaryKey val id: String,
    val mode: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val total: Int,
    val correct: Int,
    val day: Long,
)

@Entity(tableName = "card_schedules", indices = [Index("topicId"), Index("dueAt")])
data class CardScheduleEntity(
    @PrimaryKey val cardId: String,
    val topicId: String,
    val easiness: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val dueAt: Long,
    val lapses: Int,
    val lastRating: String?,
    val reviewedAt: Long,
)

@Entity(tableName = "card_reviews", indices = [Index("cardId"), Index("topicId"), Index("day")])
data class CardReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: String,
    val topicId: String,
    val rating: String,
    val at: Long,
    val day: Long,
)

@Entity(tableName = "question_practice", indices = [Index("questionId"), Index("topicId"), Index("day")])
data class QuestionPracticeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: String,
    val topicId: String,
    val mode: String, // BANK, MOCK, REVISION
    val answer: String?,
    val score10: Int?,
    val communication: Double?,
    val selfRating: String?, // GOT_IT / PARTLY / MISSED for reveal-only practice
    val skipped: Boolean,
    val sessionId: String?,
    val at: Long,
    val day: Long,
)

@Entity(tableName = "interview_sessions")
data class InterviewSessionEntity(
    @PrimaryKey val id: String,
    val role: String,
    val level: String,
    val length: Int,
    val questionIdsJson: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val overall: Int?,
    val reportJson: String?,
)

@Entity(tableName = "scenario_attempts", indices = [Index("scenarioId"), Index("topicId")])
data class ScenarioAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scenarioId: String,
    val topicId: String,
    val answer: String,
    val overall10: Int,
    val approach: Double,
    val technical: Double,
    val sequence: Double,
    val communication: Double,
    val at: Long,
    val day: Long,
)

@Entity(tableName = "coding_attempts", indices = [Index("challengeId")])
data class CodingAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val challengeId: String,
    val code: String,
    val passed: Int,
    val total: Int,
    val executed: Boolean, // false when only self-checked against the reference
    val at: Long,
    val day: Long,
)

@Entity(tableName = "code_drafts")
data class CodeDraftEntity(@PrimaryKey val challengeId: String, val code: String, val updatedAt: Long)

@Entity(tableName = "bookmarks", primaryKeys = ["type", "refId"])
data class BookmarkEntity(val type: String, val refId: String, val title: String, val subtitle: String, val createdAt: Long)

@Entity(tableName = "study_sessions", indices = [Index("day")])
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val topicId: String?,
    val startedAt: Long,
    val durationSec: Int,
    val day: Long,
)

@Entity(tableName = "xp_events", indices = [Index("day")])
data class XpEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Int,
    val reason: String,
    val at: Long,
    val day: Long,
)

@Entity(tableName = "achievements")
data class AchievementEntity(@PrimaryKey val id: String, val unlockedAt: Long)

/* Query result rows */
data class TopicQuizStat(val topicId: String, val attempts: Int, val correct: Int)
data class TopicCardStat(val topicId: String, val reviewed: Int, val known: Int)
data class TopicScoreStat(val topicId: String, val n: Int, val avgScore: Double)
data class DayMinutes(val day: Long, val seconds: Int)
data class DayCount(val day: Long, val n: Int)
