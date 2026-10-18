package com.interviewprep.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    // ---------- lessons
    @Upsert suspend fun upsertLessonProgress(p: LessonProgressEntity)
    @Query("SELECT * FROM lesson_progress WHERE lessonId = :id") suspend fun lessonProgress(id: String): LessonProgressEntity?
    @Query("SELECT * FROM lesson_progress") fun allLessonProgress(): Flow<List<LessonProgressEntity>>
    @Query("SELECT * FROM lesson_progress") suspend fun allLessonProgressOnce(): List<LessonProgressEntity>
    @Query("SELECT * FROM lesson_progress ORDER BY lastOpenedAt DESC LIMIT 1") fun lastOpenedLesson(): Flow<LessonProgressEntity?>
    @Query("SELECT COUNT(*) FROM lesson_progress WHERE completed = 1") fun completedLessonCount(): Flow<Int>

    // ---------- quiz
    @Insert suspend fun insertQuizAttempt(a: QuizAttemptEntity)
    @Upsert suspend fun upsertQuizSession(s: QuizSessionEntity)
    @Query("SELECT * FROM quiz_attempts ORDER BY answeredAt DESC LIMIT :limit") suspend fun recentQuizAttempts(limit: Int): List<QuizAttemptEntity>
    @Query("SELECT * FROM quiz_attempts WHERE topicId = :topicId ORDER BY answeredAt DESC LIMIT 60") suspend fun quizAttemptsOfTopic(topicId: String): List<QuizAttemptEntity>
    @Query("SELECT * FROM quiz_attempts ORDER BY answeredAt DESC") suspend fun allQuizAttempts(): List<QuizAttemptEntity>
    @Query("SELECT topicId, COUNT(*) AS attempts, SUM(CASE WHEN correct THEN 1 ELSE 0 END) AS correct FROM quiz_attempts WHERE skipped = 0 GROUP BY topicId")
    fun quizStatsByTopic(): Flow<List<TopicQuizStat>>
    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE skipped = 0") fun quizAnsweredCount(): Flow<Int>
    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE skipped = 1") fun quizSkippedCount(): Flow<Int>
    @Query("SELECT COUNT(*) FROM quiz_sessions WHERE finishedAt IS NOT NULL") fun finishedQuizSessions(): Flow<Int>
    @Query("SELECT * FROM quiz_sessions WHERE finishedAt IS NOT NULL ORDER BY startedAt DESC LIMIT :limit") fun recentQuizSessions(limit: Int): Flow<List<QuizSessionEntity>>
    @Query("SELECT * FROM quiz_sessions WHERE mode = 'DAILY' AND day = :day AND finishedAt IS NOT NULL LIMIT 1") fun dailyChallengeResult(day: Long): Flow<QuizSessionEntity?>

    // ---------- flashcards
    @Upsert suspend fun upsertCardSchedule(s: CardScheduleEntity)
    @Insert suspend fun insertCardReview(r: CardReviewEntity)
    @Query("SELECT * FROM card_schedules") suspend fun allCardSchedules(): List<CardScheduleEntity>
    @Query("SELECT COUNT(*) FROM card_schedules WHERE dueAt <= :now") fun dueCardCount(now: Long): Flow<Int>
    @Query("SELECT topicId, COUNT(*) AS reviewed, SUM(CASE WHEN lastRating = 'KNOW' THEN 1 ELSE 0 END) AS known FROM card_schedules GROUP BY topicId")
    fun cardStatsByTopic(): Flow<List<TopicCardStat>>
    @Query("SELECT COUNT(*) FROM card_reviews") fun cardReviewCount(): Flow<Int>

    // ---------- interview questions
    @Insert suspend fun insertPractice(p: QuestionPracticeEntity)
    @Query("SELECT topicId, COUNT(*) AS n, AVG(CASE WHEN score10 IS NOT NULL THEN score10 / 10.0 WHEN selfRating = 'GOT_IT' THEN 1.0 WHEN selfRating = 'PARTLY' THEN 0.5 ELSE 0.0 END) AS avgScore FROM question_practice WHERE skipped = 0 GROUP BY topicId")
    fun practiceStatsByTopic(): Flow<List<TopicScoreStat>>
    @Query("SELECT COUNT(*) FROM question_practice WHERE skipped = 0") fun practiceCount(): Flow<Int>
    @Query("SELECT COUNT(*) FROM question_practice WHERE skipped = 1") fun practiceSkippedCount(): Flow<Int>
    @Query("SELECT COUNT(*) FROM question_practice WHERE at >= :since") suspend fun practiceCountSince(since: Long): Int
    @Query("SELECT * FROM question_practice WHERE sessionId = :sessionId ORDER BY at") suspend fun practiceOfSession(sessionId: String): List<QuestionPracticeEntity>
    @Query("SELECT DISTINCT questionId FROM question_practice WHERE at >= :since") suspend fun practicedQuestionIdsSince(since: Long): List<String>

    // ---------- mock interviews
    @Upsert suspend fun upsertInterview(s: InterviewSessionEntity)
    @Query("SELECT * FROM interview_sessions WHERE id = :id") suspend fun interview(id: String): InterviewSessionEntity?
    @Query("SELECT * FROM interview_sessions WHERE finishedAt IS NOT NULL ORDER BY startedAt DESC") fun finishedInterviews(): Flow<List<InterviewSessionEntity>>

    // ---------- scenarios
    @Insert suspend fun insertScenarioAttempt(a: ScenarioAttemptEntity)
    @Query("SELECT * FROM scenario_attempts ORDER BY at DESC") fun scenarioAttempts(): Flow<List<ScenarioAttemptEntity>>
    @Query("SELECT COUNT(*) FROM scenario_attempts WHERE at >= :since") suspend fun scenarioCountSince(since: Long): Int

    // ---------- coding
    @Insert suspend fun insertCodingAttempt(a: CodingAttemptEntity)
    @Query("SELECT * FROM coding_attempts ORDER BY at DESC") fun codingAttempts(): Flow<List<CodingAttemptEntity>>
    @Upsert suspend fun upsertDraft(d: CodeDraftEntity)
    @Query("SELECT * FROM code_drafts WHERE challengeId = :id") suspend fun draft(id: String): CodeDraftEntity?

    // ---------- bookmarks
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBookmark(b: BookmarkEntity)
    @Query("DELETE FROM bookmarks WHERE type = :type AND refId = :refId") suspend fun deleteBookmark(type: String, refId: String)
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC") fun bookmarks(): Flow<List<BookmarkEntity>>
    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE type = :type AND refId = :refId)") fun isBookmarked(type: String, refId: String): Flow<Boolean>

    // ---------- time, xp, achievements
    @Insert suspend fun insertStudySession(s: StudySessionEntity)
    @Query("SELECT day, SUM(durationSec) AS seconds FROM study_sessions WHERE day >= :fromDay GROUP BY day ORDER BY day")
    fun minutesByDay(fromDay: Long): Flow<List<DayMinutes>>
    @Query("SELECT COALESCE(SUM(durationSec), 0) FROM study_sessions") fun totalStudySeconds(): Flow<Int>
    @Query("SELECT DISTINCT day FROM study_sessions UNION SELECT DISTINCT day FROM quiz_attempts UNION SELECT DISTINCT day FROM card_reviews UNION SELECT DISTINCT day FROM question_practice")
    fun activeDays(): Flow<List<Long>>
    @Query("SELECT kind FROM study_sessions WHERE topicId = :topicId ORDER BY startedAt DESC LIMIT 1") suspend fun lastKindForTopic(topicId: String): String?

    @Insert suspend fun insertXp(e: XpEventEntity)
    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_events") fun totalXp(): Flow<Int>
    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_events WHERE day = :day") fun xpOnDay(day: Long): Flow<Int>
    @Query("SELECT COUNT(*) FROM xp_events WHERE reason = :reason AND day = :day") suspend fun xpReasonCountOnDay(reason: String, day: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAchievement(a: AchievementEntity): Long
    @Query("SELECT * FROM achievements") fun achievements(): Flow<List<AchievementEntity>>

    // ---------- today
    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE day = :day AND skipped = 0") fun quizAnsweredOn(day: Long): Flow<Int>
    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE day = :day AND skipped = 0 AND correct = 1") fun quizCorrectOn(day: Long): Flow<Int>
    @Query("SELECT COUNT(*) FROM lesson_progress WHERE completed = 1 AND completedAt >= :since") fun lessonsCompletedSince(since: Long): Flow<Int>
    @Query("SELECT COUNT(*) FROM question_practice WHERE day = :day AND skipped = 0") fun practiceOn(day: Long): Flow<Int>

    // ---------- reset
    @Query("DELETE FROM lesson_progress") suspend fun clearLessonProgress()
    @Query("DELETE FROM quiz_attempts") suspend fun clearQuizAttempts()
    @Query("DELETE FROM quiz_sessions") suspend fun clearQuizSessions()
    @Query("DELETE FROM card_schedules") suspend fun clearCardSchedules()
    @Query("DELETE FROM card_reviews") suspend fun clearCardReviews()
    @Query("DELETE FROM question_practice") suspend fun clearPractice()
    @Query("DELETE FROM interview_sessions") suspend fun clearInterviews()
    @Query("DELETE FROM scenario_attempts") suspend fun clearScenarioAttempts()
    @Query("DELETE FROM coding_attempts") suspend fun clearCodingAttempts()
    @Query("DELETE FROM code_drafts") suspend fun clearDrafts()
    @Query("DELETE FROM bookmarks") suspend fun clearBookmarks()
    @Query("DELETE FROM study_sessions") suspend fun clearStudySessions()
    @Query("DELETE FROM xp_events") suspend fun clearXp()
    @Query("DELETE FROM achievements") suspend fun clearAchievements()

    // ---------- export
    @Query("SELECT * FROM lesson_progress") suspend fun exportLessons(): List<LessonProgressEntity>
    @Query("SELECT * FROM quiz_attempts") suspend fun exportQuiz(): List<QuizAttemptEntity>
    @Query("SELECT * FROM card_schedules") suspend fun exportCards(): List<CardScheduleEntity>
    @Query("SELECT * FROM question_practice") suspend fun exportPractice(): List<QuestionPracticeEntity>
    @Query("SELECT * FROM scenario_attempts") suspend fun exportScenarios(): List<ScenarioAttemptEntity>
    @Query("SELECT * FROM coding_attempts") suspend fun exportCoding(): List<CodingAttemptEntity>
    @Query("SELECT * FROM bookmarks") suspend fun exportBookmarks(): List<BookmarkEntity>
    @Query("SELECT * FROM study_sessions") suspend fun exportSessions(): List<StudySessionEntity>
    @Query("SELECT * FROM interview_sessions") suspend fun exportInterviews(): List<InterviewSessionEntity>
}
