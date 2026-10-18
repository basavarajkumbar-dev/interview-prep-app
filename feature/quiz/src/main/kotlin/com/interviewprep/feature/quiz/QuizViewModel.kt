package com.interviewprep.feature.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interviewprep.core.data.repo.ContentRepository
import com.interviewprep.core.data.repo.ProgressRepository
import com.interviewprep.core.data.repo.StatsRepository
import com.interviewprep.core.data.repo.StudyTracker
import com.interviewprep.core.data.repo.TimeProvider
import com.interviewprep.core.data.settings.SettingsStore
import com.interviewprep.core.domain.AdaptiveQuiz
import com.interviewprep.core.domain.Achievements
import com.interviewprep.core.domain.GradeResult
import com.interviewprep.core.domain.Mastery
import com.interviewprep.core.domain.QuizAnswer
import com.interviewprep.core.domain.QuizGrader
import com.interviewprep.core.domain.SessionComposer
import com.interviewprep.core.model.ActivityKind
import com.interviewprep.core.model.QuizItem
import com.interviewprep.core.model.QuizType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

enum class QuizMode(val title: String) {
    ADAPTIVE("Adaptive quiz"), TOPIC("Topic quiz"), DAILY("Daily Interview Challenge"), WEAK("Weak-topic quiz");

    companion object {
        fun parse(s: String?) = entries.firstOrNull { it.name == s } ?: ADAPTIVE
    }
}

data class AnsweredItem(val item: QuizItem, val result: GradeResult?, val skipped: Boolean)

data class TopicResult(val topicId: String, val title: String, val correct: Int, val total: Int) {
    val accuracy get() = if (total == 0) 0.0 else correct.toDouble() / total
}

data class QuizUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val mode: QuizMode = QuizMode.ADAPTIVE,
    val title: String = "",
    val items: List<QuizItem> = emptyList(),
    val index: Int = 0,
    // current question input
    val selected: Set<Int> = emptySet(),
    val text: String = "",
    val matches: Map<Int, Int> = emptyMap(),
    val rightOrder: List<Int> = emptyList(), // shuffled order of MATCH right-hand items
    val revealed: Boolean = false, // CODE_OUTPUT: answer shown for self-check
    val feedback: GradeResult? = null,
    val answered: List<AnsweredItem> = emptyList(),
    val finished: Boolean = false,
    val secondsLeft: Int? = null,
    val topicTitles: Map<String, String> = emptyMap(),
    val note: String? = null,
    val newAchievements: List<String> = emptyList(),
) {
    val current: QuizItem? get() = items.getOrNull(index)
    val correctCount get() = answered.count { it.result?.correct == true }
    val accuracy get() = if (answered.none { !it.skipped }) 0.0 else correctCount.toDouble() / answered.count { !it.skipped }
    val canSubmit: Boolean get() {
        val q = current ?: return false
        return feedback == null && when (q.type) {
            QuizType.MCQ, QuizType.TRUE_FALSE, QuizType.SCENARIO, QuizType.DEBUGGING -> selected.size == 1
            QuizType.MULTI -> selected.isNotEmpty()
            QuizType.FILL_BLANK, QuizType.CODE_OUTPUT -> text.isNotBlank()
            QuizType.MATCH -> matches.size == q.pairs.size
        }
    }
    val byTopic: List<TopicResult> get() = answered.filter { !it.skipped }.groupBy { it.item.topicId }
        .map { (t, l) -> TopicResult(t, topicTitles[t] ?: t, l.count { it.result?.correct == true }, l.size) }
        .sortedByDescending { it.accuracy }
}

@HiltViewModel
class QuizViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val stats: StatsRepository,
    private val settings: SettingsStore,
    private val time: TimeProvider,
    private val tracker: StudyTracker,
) : ViewModel() {
    private val mode = QuizMode.parse(savedState.get<String>("mode"))
    private val topicArg = savedState.get<String>("topic")?.takeIf { it.isNotBlank() }
    private val sessionId = UUID.randomUUID().toString()
    private val startedAt = tracker.start()
    private var timer: Job? = null

    private val _state = MutableStateFlow(QuizUiState(mode = mode))
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.value = QuizUiState(mode = mode, loading = true)
        try {
            val snap = stats.snapshot().first()
            val st = settings.current()
            val pool = content.quizPool()
            val history = progress.quizHistory()
            val now = time.now()
            val inputs = AdaptiveQuiz.Inputs(
                pool = pool,
                mastery = snap.mastery,
                topicAccuracy = snap.performance.mapValues { (_, p) -> (p.recentAccuracy ?: 0.0) to p.quizAttempts }
                    .filterValues { it.second > 0 },
                lastResult = history.filter { !it.skipped }.groupBy { it.itemId }.mapValues { it.value.maxBy { a -> a.answeredAt }.correct },
                seenRecently = history.filter { now - it.answeredAt < 2 * 86_400_000L }.map { it.itemId }.toSet(),
                fixedDifficulty = st.quizDifficulty,
                count = 10,
            )
            val titles = snap.topics.associate { it.id to it.title }
            var note: String? = null
            val items: List<QuizItem> = when (mode) {
                QuizMode.DAILY -> SessionComposer().dailyChallenge(pool, time.today(), inputs)
                QuizMode.TOPIC -> {
                    val topic = topicArg
                    val own = pool.filter { it.topicId == topic }
                    if (own.size >= 5) AdaptiveQuiz(Random(now)).select(inputs.copy(pool = own, maxSharePerTopic = 1.0))
                    else {
                        val track = snap.topics.firstOrNull { it.id == topic }?.trackId
                        val related = pool.filter { q -> snap.topics.firstOrNull { it.id == q.topicId }?.trackId == track }
                        note = if (own.isEmpty()) "This topic has no quiz items of its own, so questions come from the same track."
                        else "Only ${own.size} quiz items for this topic; topped up from the same track."
                        (own + AdaptiveQuiz(Random(now)).select(inputs.copy(pool = related - own.toSet(), count = 10 - own.size))).take(10)
                    }
                }
                QuizMode.WEAK -> {
                    val weak = Mastery.weakTopics(snap.mastery.values.toList()).map { it.topicId }.toSet()
                    if (weak.isEmpty()) {
                        note = "No weak topics yet — answer a few more quizzes. Here's an adaptive quiz instead."
                        AdaptiveQuiz(Random(now)).select(inputs)
                    } else AdaptiveQuiz(Random(now)).select(inputs.copy(pool = pool.filter { it.topicId in weak }, maxSharePerTopic = 0.5))
                }
                QuizMode.ADAPTIVE -> {
                    val roleTracks = snap.tracks.filter { st.targetRoles.isEmpty() || it.track.roles.any { r -> r in st.targetRoles } }.map { it.track.id }.toSet()
                    val roleTopics = snap.topics.filter { it.trackId in roleTracks }.map { it.id }.toSet()
                    AdaptiveQuiz(Random(now)).select(inputs.copy(pool = pool.filter { it.topicId in roleTopics }.ifEmpty { pool }))
                }
            }
            if (items.isEmpty()) {
                _state.value = QuizUiState(mode = mode, loading = false, error = "Couldn't build a quiz: no questions are available for this selection.")
                return@launch
            }
            val title = when (mode) {
                QuizMode.TOPIC -> titles[topicArg] ?: mode.title
                else -> mode.title
            }
            progress.startQuizSession(sessionId, mode.name, items.size)
            _state.value = QuizUiState(
                loading = false, mode = mode, title = title, items = items, topicTitles = titles, note = note,
                rightOrder = rightOrderFor(items.first()),
                secondsLeft = if (mode == QuizMode.DAILY) 10 * 60 else null,
            )
            if (mode == QuizMode.DAILY) startTimer()
        } catch (t: Throwable) {
            _state.value = QuizUiState(mode = mode, loading = false, error = "Quiz generation failed: ${t.message ?: "unknown error"}")
        }
    }

    private fun rightOrderFor(q: QuizItem): List<Int> = if (q.type == QuizType.MATCH) q.pairs.indices.shuffled() else emptyList()

    private fun startTimer() {
        timer?.cancel()
        timer = viewModelScope.launch {
            while (true) {
                delay(1000)
                val s = _state.value
                val left = (s.secondsLeft ?: return@launch) - 1
                if (s.finished) return@launch
                if (left <= 0) {
                    _state.update { it.copy(secondsLeft = 0) }
                    finish()
                    return@launch
                }
                _state.update { it.copy(secondsLeft = left) }
            }
        }
    }

    // ---------------- input
    fun toggleOption(i: Int) = _state.update { s ->
        val q = s.current ?: return@update s
        if (s.feedback != null) return@update s
        if (q.type == QuizType.MULTI) s.copy(selected = if (i in s.selected) s.selected - i else s.selected + i)
        else s.copy(selected = setOf(i))
    }

    fun setText(t: String) = _state.update { if (it.feedback == null) it.copy(text = t) else it }
    fun setMatch(left: Int, right: Int) = _state.update { s ->
        if (s.feedback != null) s else s.copy(matches = s.matches.filterValues { it != right } + (left to right))
    }
    fun reveal() = _state.update { it.copy(revealed = true) }

    // ---------------- grading
    fun submit() = grade(null)
    fun selfMark(correct: Boolean) = grade(correct)

    private fun grade(selfMarked: Boolean?) {
        val s = _state.value
        val q = s.current ?: return
        if (s.feedback != null) return
        val answer = when {
            selfMarked != null -> QuizAnswer.SelfMarked(selfMarked)
            q.type == QuizType.FILL_BLANK || q.type == QuizType.CODE_OUTPUT -> QuizAnswer.Text(s.text)
            q.type == QuizType.MATCH -> QuizAnswer.Matches(s.matches)
            else -> QuizAnswer.Choice(s.selected)
        }
        val r = QuizGrader.grade(q, answer)
        _state.update { it.copy(feedback = r, answered = it.answered + AnsweredItem(q, r, false)) }
        viewModelScope.launch { progress.recordQuizAnswer(q, r.correct, r.score, sessionId) }
    }

    fun skip() {
        val s = _state.value
        val q = s.current ?: return
        _state.update { it.copy(answered = it.answered + AnsweredItem(q, null, true)) }
        viewModelScope.launch { progress.recordQuizAnswer(q, false, 0.0, sessionId, skipped = true) }
        next()
    }

    fun next() {
        val s = _state.value
        if (s.index + 1 >= s.items.size) { finish(); return }
        val nextItem = s.items[s.index + 1]
        _state.update {
            it.copy(index = it.index + 1, selected = emptySet(), text = "", matches = emptyMap(), revealed = false,
                feedback = null, rightOrder = rightOrderFor(nextItem))
        }
    }

    private fun finish() {
        if (_state.value.finished) return
        timer?.cancel()
        _state.update { it.copy(finished = true) }
        viewModelScope.launch {
            val s = _state.value
            progress.finishQuizSession(sessionId, mode.name, startedAt, s.answered.size, s.correctCount)
            val snap = stats.snapshot().first()
            val unlocked = progress.unlockAchievements(Achievements.earned(stats.achievementStats(snap)))
            if (unlocked.isNotEmpty()) {
                _state.update { st -> st.copy(newAchievements = unlocked.mapNotNull { id -> Achievements.all.firstOrNull { it.id == id }?.let { "${it.emoji} ${it.title}" } }) }
            }
        }
    }

    fun endEarly() = finish()

    override fun onCleared() {
        tracker.stop(ActivityKind.QUIZ, startedAt, topicArg)
    }
}
