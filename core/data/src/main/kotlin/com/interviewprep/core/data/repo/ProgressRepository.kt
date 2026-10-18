package com.interviewprep.core.data.repo

import androidx.room.withTransaction
import com.interviewprep.core.data.db.AchievementEntity
import com.interviewprep.core.data.db.BookmarkEntity
import com.interviewprep.core.data.db.CardReviewEntity
import com.interviewprep.core.data.db.CardScheduleEntity
import com.interviewprep.core.data.db.CodeDraftEntity
import com.interviewprep.core.data.db.CodingAttemptEntity
import com.interviewprep.core.data.db.InterviewSessionEntity
import com.interviewprep.core.data.db.J
import com.interviewprep.core.data.db.LessonProgressEntity
import com.interviewprep.core.data.db.QuestionPracticeEntity
import com.interviewprep.core.data.db.QuizAttemptEntity
import com.interviewprep.core.data.db.QuizSessionEntity
import com.interviewprep.core.data.db.ScenarioAttemptEntity
import com.interviewprep.core.data.db.StudySessionEntity
import com.interviewprep.core.data.db.UserDatabase
import com.interviewprep.core.data.db.XpEventEntity
import com.interviewprep.core.domain.AnswerEvaluation
import com.interviewprep.core.domain.ScenarioEvaluation
import com.interviewprep.core.domain.SpacedRepetition
import com.interviewprep.core.domain.Xp
import com.interviewprep.core.model.ActivityKind
import com.interviewprep.core.model.Bookmark
import com.interviewprep.core.model.BookmarkType
import com.interviewprep.core.model.CardRating
import com.interviewprep.core.model.CardSchedule
import com.interviewprep.core.model.Difficulty
import com.interviewprep.core.model.QuizItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.addJsonObject
import javax.inject.Inject
import javax.inject.Singleton

enum class SelfRating { GOT_IT, PARTLY, MISSED }

/** Every write of user progress goes through here, so XP and study time stay consistent. */
@Singleton
class ProgressRepository @Inject constructor(
    private val db: UserDatabase,
    private val time: TimeProvider,
) {
    private val dao get() = db.userDao()

    private suspend fun xp(amount: Int, reason: String) {
        if (amount > 0) dao.insertXp(XpEventEntity(amount = amount, reason = reason, at = time.now(), day = time.today()))
    }

    // ---------------------------------------------------------------- lessons
    suspend fun openLesson(lessonId: String, topicId: String) {
        val existing = dao.lessonProgress(lessonId)
        dao.upsertLessonProgress(
            existing?.copy(lastOpenedAt = time.now())
                ?: LessonProgressEntity(lessonId, topicId, completed = false, completedAt = null, lastOpenedAt = time.now()),
        )
    }

    suspend fun setLessonCompleted(lessonId: String, topicId: String, completed: Boolean) {
        val existing = dao.lessonProgress(lessonId)
        val wasDone = existing?.completed == true
        dao.upsertLessonProgress(
            LessonProgressEntity(lessonId, topicId, completed, if (completed) time.now() else null, time.now()),
        )
        if (completed && !wasDone) xp(Xp.LESSON, "lesson")
    }

    fun lessonProgress(): Flow<Map<String, LessonProgressEntity>> = dao.allLessonProgress().map { l -> l.associateBy { it.lessonId } }
    fun lastOpenedLesson(): Flow<LessonProgressEntity?> = dao.lastOpenedLesson()

    // ---------------------------------------------------------------- quiz
    suspend fun startQuizSession(id: String, mode: String, total: Int) =
        dao.upsertQuizSession(QuizSessionEntity(id, mode, time.now(), null, total, 0, time.today()))

    suspend fun recordQuizAnswer(item: QuizItem, correct: Boolean, score: Double, sessionId: String?, skipped: Boolean = false) {
        dao.insertQuizAttempt(
            QuizAttemptEntity(
                itemId = item.id, topicId = item.topicId, correct = correct, score = score, difficulty = item.difficulty.name,
                skipped = skipped, sessionId = sessionId, answeredAt = time.now(), day = time.today(),
            ),
        )
        if (!skipped) xp(if (correct) Xp.QUIZ_CORRECT else Xp.QUIZ_ANSWERED, "quiz")
    }

    suspend fun finishQuizSession(id: String, mode: String, startedAt: Long, total: Int, correct: Int) =
        dao.upsertQuizSession(QuizSessionEntity(id, mode, startedAt, time.now(), total, correct, time.today()))

    suspend fun quizHistory(): List<QuizAttemptEntity> = dao.allQuizAttempts()

    // ---------------------------------------------------------------- flashcards
    suspend fun cardSchedules(): Map<String, CardSchedule> = dao.allCardSchedules().associate {
        it.cardId to CardSchedule(
            it.cardId, it.easiness, it.intervalDays, it.repetitions, it.dueAt, it.lapses,
            CardRating.entries.firstOrNull { r -> r.name == it.lastRating },
        )
    }

    suspend fun rateCard(cardId: String, topicId: String, rating: CardRating, current: CardSchedule?) {
        val next = SpacedRepetition.schedule(current ?: CardSchedule(cardId), rating, time.now())
        db.withTransaction {
            dao.upsertCardSchedule(
                CardScheduleEntity(cardId, topicId, next.easiness, next.intervalDays, next.repetitions, next.dueAtMillis,
                    next.lapses, rating.name, time.now()),
            )
            dao.insertCardReview(CardReviewEntity(cardId = cardId, topicId = topicId, rating = rating.name, at = time.now(), day = time.today()))
        }
        xp(Xp.CARD_REVIEW, "card")
    }

    fun dueCardCount(): Flow<Int> = dao.dueCardCount(time.now())

    // ---------------------------------------------------------------- interview questions
    suspend fun recordPractice(
        questionId: String,
        topicId: String,
        mode: String,
        answer: String? = null,
        evaluation: AnswerEvaluation? = null,
        selfRating: SelfRating? = null,
        skipped: Boolean = false,
        sessionId: String? = null,
    ) {
        dao.insertPractice(
            QuestionPracticeEntity(
                questionId = questionId, topicId = topicId, mode = mode, answer = answer, score10 = evaluation?.score10,
                communication = evaluation?.communication, selfRating = selfRating?.name, skipped = skipped,
                sessionId = sessionId, at = time.now(), day = time.today(),
            ),
        )
        if (!skipped) xp(if (mode == "MOCK") Xp.MOCK_ANSWER else Xp.QUESTION_PRACTICE, "question")
    }

    suspend fun startInterview(id: String, role: String, level: String, questionIds: List<String>) =
        dao.upsertInterview(InterviewSessionEntity(id, role, level, questionIds.size, J.str(questionIds), time.now(), null, null, null))

    suspend fun finishInterview(id: String, overall: Int, reportJson: String) {
        val s = dao.interview(id) ?: return
        dao.upsertInterview(s.copy(finishedAt = time.now(), overall = overall, reportJson = reportJson))
    }

    /** role name and ordered question ids of a mock interview */
    suspend fun interviewSession(id: String): Pair<String, List<String>>? =
        dao.interview(id)?.let { it.role to J.strList(it.questionIdsJson) }

    suspend fun interviewReportJson(id: String): String? = dao.interview(id)?.reportJson

    suspend fun interviewAnswers(sessionId: String): List<QuestionPracticeEntity> = dao.practiceOfSession(sessionId)
    fun finishedInterviews(): Flow<List<InterviewSessionEntity>> = dao.finishedInterviews()

    // ---------------------------------------------------------------- scenarios & coding
    suspend fun recordScenario(scenarioId: String, topicId: String, answer: String, e: ScenarioEvaluation) {
        dao.insertScenarioAttempt(
            ScenarioAttemptEntity(
                scenarioId = scenarioId, topicId = topicId, answer = answer, overall10 = e.overall10, approach = e.approach,
                technical = e.technical, sequence = e.sequence, communication = e.communication, at = time.now(), day = time.today(),
            ),
        )
        xp(Xp.SCENARIO, "scenario")
    }

    suspend fun recordCoding(challengeId: String, code: String, passed: Int, total: Int, executed: Boolean) {
        dao.insertCodingAttempt(CodingAttemptEntity(challengeId = challengeId, code = code, passed = passed, total = total,
            executed = executed, at = time.now(), day = time.today()))
        if (total > 0 && passed == total) xp(Xp.CODING_PASS, "coding")
    }

    suspend fun saveDraft(challengeId: String, code: String) = dao.upsertDraft(CodeDraftEntity(challengeId, code, time.now()))
    suspend fun draft(challengeId: String): String? = dao.draft(challengeId)?.code

    // ---------------------------------------------------------------- bookmarks
    fun bookmarks(): Flow<List<Bookmark>> = dao.bookmarks().map { l ->
        l.mapNotNull { b -> BookmarkType.entries.firstOrNull { it.name == b.type }?.let { Bookmark(it, b.refId, b.title, b.createdAt) } }
    }
    fun bookmarkSubtitles(): Flow<Map<String, String>> = dao.bookmarks().map { l -> l.associate { "${it.type}:${it.refId}" to it.subtitle } }
    fun isBookmarked(type: BookmarkType, refId: String): Flow<Boolean> = dao.isBookmarked(type.name, refId)
    suspend fun setBookmark(type: BookmarkType, refId: String, title: String, subtitle: String, on: Boolean) {
        if (on) dao.insertBookmark(BookmarkEntity(type.name, refId, title.take(200), subtitle.take(200), time.now()))
        else dao.deleteBookmark(type.name, refId)
    }

    // ---------------------------------------------------------------- time, xp, achievements
    /** Records time spent on a screen; ignores blips and caps runaway timers (app left open). */
    suspend fun recordStudyTime(kind: ActivityKind, startedAt: Long, topicId: String? = null) {
        val seconds = ((time.now() - startedAt) / 1000).toInt()
        if (seconds < 10) return
        dao.insertStudySession(StudySessionEntity(kind = kind.name, topicId = topicId, startedAt = startedAt,
            durationSec = seconds.coerceAtMost(30 * 60), day = time.dayOf(startedAt)))
    }

    suspend fun awardDailyGoalIfNeeded() {
        if (dao.xpReasonCountOnDay("daily_goal", time.today()) == 0) xp(Xp.DAILY_GOAL, "daily_goal")
    }

    suspend fun unlockAchievements(ids: Set<String>): List<String> =
        ids.filter { dao.insertAchievement(AchievementEntity(it, time.now())) != -1L }

    // ---------------------------------------------------------------- settings actions
    suspend fun resetAll() = db.withTransaction {
        dao.clearLessonProgress(); dao.clearQuizAttempts(); dao.clearQuizSessions(); dao.clearCardSchedules()
        dao.clearCardReviews(); dao.clearPractice(); dao.clearInterviews(); dao.clearScenarioAttempts()
        dao.clearCodingAttempts(); dao.clearDrafts(); dao.clearBookmarks(); dao.clearStudySessions(); dao.clearXp()
        dao.clearAchievements()
    }

    /** Portable JSON of all progress (for backup now, cloud sync later). */
    suspend fun exportJson(): String {
        val lessons = dao.exportLessons()
        val quiz = dao.exportQuiz()
        val cards = dao.exportCards()
        val practice = dao.exportPractice()
        val scenarios = dao.exportScenarios()
        val coding = dao.exportCoding()
        val bookmarks = dao.exportBookmarks()
        val sessions = dao.exportSessions()
        val interviews = dao.exportInterviews()
        val o = buildJsonObject {
            put("format", "interviewprep-progress")
            put("version", 1)
            put("exportedAt", time.now())
            putJsonArray("lessons") { lessons.forEach { addJsonObject { put("lessonId", it.lessonId); put("topicId", it.topicId); put("completed", it.completed); it.completedAt?.let { c -> put("completedAt", c) } } } }
            putJsonArray("quizAttempts") { quiz.forEach { addJsonObject { put("itemId", it.itemId); put("topicId", it.topicId); put("correct", it.correct); put("skipped", it.skipped); put("at", it.answeredAt) } } }
            putJsonArray("cards") { cards.forEach { addJsonObject { put("cardId", it.cardId); put("ease", it.easiness); put("interval", it.intervalDays); put("reps", it.repetitions); put("due", it.dueAt); put("lapses", it.lapses) } } }
            putJsonArray("questionPractice") { practice.forEach { addJsonObject { put("questionId", it.questionId); put("mode", it.mode); it.score10?.let { s -> put("score10", s) }; it.selfRating?.let { s -> put("self", s) }; put("at", it.at) } } }
            putJsonArray("scenarios") { scenarios.forEach { addJsonObject { put("scenarioId", it.scenarioId); put("score10", it.overall10); put("at", it.at) } } }
            putJsonArray("coding") { coding.forEach { addJsonObject { put("challengeId", it.challengeId); put("passed", it.passed); put("total", it.total); put("at", it.at) } } }
            putJsonArray("bookmarks") { bookmarks.forEach { addJsonObject { put("type", it.type); put("refId", it.refId); put("title", it.title) } } }
            putJsonArray("studySessions") { sessions.forEach { addJsonObject { put("kind", it.kind); put("start", it.startedAt); put("seconds", it.durationSec) } } }
            putJsonArray("interviews") { interviews.forEach { addJsonObject { put("role", it.role); put("level", it.level); it.overall?.let { s -> put("overall", s) }; put("at", it.startedAt) } } }
        }
        return o.toString()
    }

    fun difficultyOf(name: String): Difficulty = Difficulty.parse(name)
}
