package com.interviewprep.core.data.repo

import com.interviewprep.core.data.db.UserDatabase
import com.interviewprep.core.data.settings.SettingsStore
import com.interviewprep.core.domain.AchievementStats
import com.interviewprep.core.domain.Achievements
import com.interviewprep.core.domain.Mastery
import com.interviewprep.core.domain.Readiness
import com.interviewprep.core.domain.ReadinessCalculator
import com.interviewprep.core.domain.ReadinessInputs
import com.interviewprep.core.domain.Streak
import com.interviewprep.core.domain.StreakInfo
import com.interviewprep.core.domain.TopicMastery
import com.interviewprep.core.domain.Xp
import com.interviewprep.core.model.Achievement
import com.interviewprep.core.model.QuizAttempt
import com.interviewprep.core.model.Topic
import com.interviewprep.core.model.TopicPerformance
import com.interviewprep.core.model.Track
import com.interviewprep.core.model.UserSettings
import com.interviewprep.core.data.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject
import javax.inject.Singleton

data class TrackProgress(val track: Track, val completion: Double, val mastery: Double, val topics: Int)

data class DayStudy(val day: Long, val minutes: Int)

/** Everything the Home, Progress and Weak-topics screens need, recomputed when progress changes. */
data class ProgressSnapshot(
    val settings: UserSettings,
    val topics: List<Topic>,
    val tracks: List<TrackProgress>,
    val performance: Map<String, TopicPerformance>,
    val mastery: Map<String, TopicMastery>,
    val weak: List<TopicMastery>,
    val strong: List<TopicMastery>,
    val readiness: Readiness,
    val streak: StreakInfo,
    val todayMinutes: Int,
    val todayXp: Int,
    val totalXp: Int,
    val lessonsDone: Int,
    val lessonsTotal: Int,
    val quizAnswered: Int,
    val quizCorrect: Int,
    val quizSkipped: Int,
    val questionsPracticed: Int,
    val questionsSkipped: Int,
    val cardsReviewed: Int,
    val dueCards: Int,
    val interviewsDone: Int,
    val interviewAvg: Int?,
    val scenariosDone: Int,
    val scenarioAvg: Int?,
    val codingSolved: Int,
    val codingAttempted: Int,
    val totalStudyMinutes: Int,
    val last14Days: List<DayStudy>,
    val achievements: List<Achievement>,
    val bookmarks: Int,
    val quizzesCompleted: Int,
) {
    val level get() = Xp.level(totalXp)
    val levelTitle get() = Xp.title(level)
    val quizAccuracy: Double? get() = if (quizAnswered == 0) null else quizCorrect.toDouble() / quizAnswered
    val overallCompletion: Double get() = if (lessonsTotal == 0) 0.0 else lessonsDone.toDouble() / lessonsTotal
    fun topicTitle(id: String) = topics.firstOrNull { it.id == id }?.title ?: id
}

@Singleton
class StatsRepository @Inject constructor(
    private val content: ContentRepository,
    private val db: UserDatabase,
    private val settingsStore: SettingsStore,
    private val time: TimeProvider,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val dao get() = db.userDao()

    /** One shared, cached snapshot for all screens. */
    private val shared: Flow<ProgressSnapshot> by lazy {
        buildSnapshot().shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)
    }

    fun snapshot(): Flow<ProgressSnapshot> = shared

    private data class Base(val settings: UserSettings, val topics: List<Topic>, val tracks: List<Track>, val lessonCounts: Map<String, Int>)
    private data class Practice(
        val lessons: Map<String, Int>, // topic -> completed lessons
        val quiz: List<QuizAttempt>,
        val cards: Map<String, Pair<Int, Int>>, // topic -> reviewed, known
        val practice: Map<String, Pair<Int, Double>>, // topic -> n, avg 0..1
    )
    private data class Activity(
        val activeDays: List<Long>,
        val minutes: List<DayStudy>,
        val totalXp: Int,
        val todayXp: Int,
        val totalSeconds: Int,
    )
    private data class Extra(
        val scenarios: Pair<Int, Int?>,
        val coding: Pair<Int, Int>,
        val interviews: Pair<Int, Int?>,
        val counts: IntArray, // quizSkipped, practiceCount, practiceSkipped, cardReviews, dueCards, bookmarks, finishedQuizzes
        val achievements: Map<String, Long>,
    )

    private fun buildSnapshot(): Flow<ProgressSnapshot> {
        val base = combine(settingsStore.settings, content.topics(), content.tracks(), content.lessonCounts()) { s, t, tr, lc -> Base(s, t, tr, lc) }
        val practice = combine(
            dao.allLessonProgress(),
            dao.quizStatsByTopic().mapLatest { dao.allQuizAttempts() },
            dao.cardStatsByTopic(),
            dao.practiceStatsByTopic(),
        ) { lp, qa, cs, ps ->
            Practice(
                lp.filter { it.completed }.groupingBy { it.topicId }.eachCount(),
                qa.map { QuizAttempt(it.itemId, it.topicId, it.correct, com.interviewprep.core.model.Difficulty.parse(it.difficulty), it.answeredAt, it.skipped) },
                cs.associate { it.topicId to (it.reviewed to it.known) },
                ps.associate { it.topicId to (it.n to it.avgScore) },
            )
        }
        val today = time.today()
        val activity = combine(
            dao.activeDays(), dao.minutesByDay(today - 13), dao.totalXp(), dao.xpOnDay(today), dao.totalStudySeconds(),
        ) { days, mins, xp, txp, secs -> Activity(days, mins.map { DayStudy(it.day, it.seconds / 60) }, xp, txp, secs) }
        val counts = combine(
            combine(dao.quizSkippedCount(), dao.practiceCount(), dao.practiceSkippedCount(), dao.cardReviewCount()) { a, b, c, d -> intArrayOf(a, b, c, d) },
            combine(dao.dueCardCount(time.now()), dao.bookmarks(), dao.finishedQuizSessions()) { e, f, g -> intArrayOf(e, f.size, g) },
        ) { a, b -> a + b }
        val extra = combine(dao.scenarioAttempts(), dao.codingAttempts(), dao.finishedInterviews(), counts, dao.achievements()) { sa, ca, iv, c, ach ->
            Extra(
                sa.size to sa.takeIf { it.isNotEmpty() }?.map { it.overall10 }?.average()?.times(10)?.toInt(),
                ca.filter { it.total > 0 && it.passed == it.total }.map { it.challengeId }.distinct().size to ca.map { it.challengeId }.distinct().size,
                iv.size to iv.mapNotNull { it.overall }.takeIf { it.isNotEmpty() }?.average()?.toInt(),
                c,
                ach.associate { it.id to it.unlockedAt },
            )
        }
        return combine(base, practice, activity, extra) { b, p, a, e -> build(b, p, a, e) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
    }

    private fun build(b: Base, p: Practice, a: Activity, e: Extra): ProgressSnapshot {
        val attemptsByTopic = p.quiz.groupBy { it.topicId }
        val perf = b.topics.associate { t ->
            val at = attemptsByTopic[t.id].orEmpty().filter { !it.skipped }
            val cards = p.cards[t.id]
            t.id to TopicPerformance(
                topicId = t.id,
                lessonsDone = p.lessons[t.id] ?: 0,
                lessonsTotal = b.lessonCounts[t.id] ?: 0,
                quizAttempts = at.size,
                quizCorrect = at.count { it.correct },
                recentAccuracy = Mastery.recentAccuracy(at),
                cardsKnown = cards?.second ?: 0,
                cardsReviewed = cards?.first ?: 0,
                interviewAvg = p.practice[t.id]?.second,
            )
        }
        val mastery = perf.mapValues { Mastery.score(it.value) }
        val roleTracks = if (b.settings.targetRoles.isEmpty()) b.tracks.map { it.id }.toSet()
        else b.tracks.filter { tr -> tr.roles.any { it in b.settings.targetRoles } }.map { it.id }.toSet()
        val trackProgress = b.tracks.map { tr ->
            val ts = b.topics.filter { it.trackId == tr.id }
            val total = ts.sumOf { b.lessonCounts[it.id] ?: 0 }
            val done = ts.sumOf { p.lessons[it.id] ?: 0 }
            val ms = ts.mapNotNull { mastery[it.id] }.filter { it.evidence > 0 }
            TrackProgress(tr, if (total == 0) 0.0 else done.toDouble() / total, if (ms.isEmpty()) 0.0 else ms.map { it.score }.average(), ts.size)
        }
        val inRole = b.topics.filter { it.trackId in roleTracks }
        val roleTotal = inRole.sumOf { b.lessonCounts[it.id] ?: 0 }
        val roleDone = inRole.sumOf { p.lessons[it.id] ?: 0 }
        val graded = p.quiz.filter { !it.skipped }
        val practised = mastery.values.filter { it.evidence >= 3 }.sortedBy { it.score }
        val readiness = ReadinessCalculator.compute(
            ReadinessInputs(
                learningCompletion = if (roleTotal == 0) 0.0 else roleDone.toDouble() / roleTotal,
                quizAccuracy = Mastery.recentAccuracy(graded),
                interviewScore = p.practice.values.takeIf { it.isNotEmpty() }?.let { v -> v.sumOf { it.first * it.second } / v.sumOf { it.first } },
                scenarioScore = e.scenarios.second?.let { it / 100.0 },
                codingPassRate = if (e.coding.second == 0) null else e.coding.first.toDouble() / e.coding.second,
                weakTopicPerformance = practised.take(5).takeIf { it.isNotEmpty() }?.map { it.score }?.average(),
            ),
        )
        val today = time.today()
        val streak = Streak.compute(a.activeDays, today)
        val lessonsDone = p.lessons.values.sum()
        return ProgressSnapshot(
            settings = b.settings,
            topics = b.topics,
            tracks = trackProgress,
            performance = perf,
            mastery = mastery,
            weak = Mastery.weakTopics(mastery.values.toList()),
            strong = Mastery.strongTopics(mastery.values.toList()),
            readiness = readiness,
            streak = streak,
            todayMinutes = a.minutes.firstOrNull { it.day == today }?.minutes ?: 0,
            todayXp = a.todayXp,
            totalXp = a.totalXp,
            lessonsDone = lessonsDone,
            lessonsTotal = b.lessonCounts.values.sum(),
            quizAnswered = graded.size,
            quizCorrect = graded.count { it.correct },
            quizSkipped = e.counts[0],
            questionsPracticed = e.counts[1],
            questionsSkipped = e.counts[2],
            cardsReviewed = e.counts[3],
            dueCards = e.counts[4],
            interviewsDone = e.interviews.first,
            interviewAvg = e.interviews.second,
            scenariosDone = e.scenarios.first,
            scenarioAvg = e.scenarios.second,
            codingSolved = e.coding.first,
            codingAttempted = e.coding.second,
            totalStudyMinutes = a.totalSeconds / 60,
            last14Days = (today - 13..today).map { d -> DayStudy(d, a.minutes.firstOrNull { it.day == d }?.minutes ?: 0) },
            achievements = Achievements.all.map { it.copy(unlockedAtMillis = e.achievements[it.id]) },
            bookmarks = e.counts[5],
            quizzesCompleted = e.counts[6],
        )
    }

    fun achievementStats(s: ProgressSnapshot): AchievementStats {
        val python = s.topics.filter { it.trackId == "python" }.mapNotNull { s.mastery[it.id] }
        return AchievementStats(
            quizzesCompleted = s.quizzesCompleted,
            questionsAnswered = s.quizAnswered,
            bestStreak = s.streak.longest,
            pythonMastery = if (python.isEmpty()) 0.0 else python.map { it.score }.average(),
            mockInterviewsCompleted = s.interviewsDone,
            quizAccuracyOver20 = if (s.quizAnswered >= 20) s.quizAccuracy else null,
            lessonsCompleted = s.lessonsDone,
            codingSolved = s.codingSolved,
            scenariosDone = s.scenariosDone,
            cardsReviewed = s.cardsReviewed,
        )
    }
}
