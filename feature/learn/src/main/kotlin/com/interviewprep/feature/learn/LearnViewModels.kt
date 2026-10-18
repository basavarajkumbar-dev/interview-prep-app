package com.interviewprep.feature.learn

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interviewprep.core.data.repo.ContentRepository
import com.interviewprep.core.data.repo.ProgressRepository
import com.interviewprep.core.data.repo.StatsRepository
import com.interviewprep.core.data.repo.StudyTracker
import com.interviewprep.core.data.settings.SettingsStore
import com.interviewprep.core.domain.MasteryBand
import com.interviewprep.core.model.ActivityKind
import com.interviewprep.core.model.BookmarkType
import com.interviewprep.core.model.ExplanationMode
import com.interviewprep.core.model.GlossaryEntry
import com.interviewprep.core.model.InterviewQuestion
import com.interviewprep.core.model.Lesson
import com.interviewprep.core.model.LessonSummary
import com.interviewprep.core.model.Topic
import com.interviewprep.core.model.Track
import com.interviewprep.core.designsystem.Markdown
import com.interviewprep.core.designsystem.MdBlock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ------------------------------------------------------------------ Learn tree
data class TopicNode(val topic: Topic, val done: Int, val total: Int, val band: MasteryBand, val score: Double)
data class TrackNode(val track: Track, val topics: List<TopicNode>, val completion: Double)

data class LearnUiState(
    val loading: Boolean = true,
    val tracks: List<TrackNode> = emptyList(),
    val roleFilter: String? = null,
    val roles: List<Pair<String, String>> = emptyList(),
    val expanded: Set<String> = emptySet(),
)

@HiltViewModel
class LearnViewModel @Inject constructor(
    stats: StatsRepository,
    settings: SettingsStore,
) : ViewModel() {
    private val roleFilter = MutableStateFlow<String?>(null)
    private val expanded = MutableStateFlow<Set<String>>(emptySet())
    private val roleTitles = listOf(
        "PY" to "Python Automation", "TA" to "Test Automation", "SR" to "Senior Test", "EMB" to "Embedded",
        "BMC" to "BMC/OpenBMC", "SSD" to "SSD/NVMe", "SW" to "Software Test",
    )

    val state: StateFlow<LearnUiState> = combine(stats.snapshot(), roleFilter, expanded, settings.settings) { s, role, exp, st ->
        val effectiveRole = role
        val tracks = s.tracks.filter { effectiveRole == null || effectiveRole in it.track.roles }.map { tp ->
            val nodes = s.topics.filter { it.trackId == tp.track.id }.map { t ->
                val p = s.performance[t.id]
                val m = s.mastery[t.id]
                TopicNode(t, p?.lessonsDone ?: 0, p?.lessonsTotal ?: 0, m?.band ?: MasteryBand.UNSEEN, m?.score ?: 0.0)
            }
            TrackNode(tp.track, nodes, tp.completion)
        }
        LearnUiState(false, tracks, role, roleTitles, exp)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LearnUiState())

    fun setRole(r: String?) { roleFilter.value = r }
    fun toggle(trackId: String) = expanded.update { if (trackId in it) it - trackId else it + trackId }
}

// ------------------------------------------------------------------ Topic
data class LessonItem(val lesson: LessonSummary, val completed: Boolean)

data class TopicUiState(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val topic: Topic? = null,
    val trackTitle: String = "",
    val lessons: List<LessonItem> = emptyList(),
    val questions: List<InterviewQuestion> = emptyList(),
    val glossary: List<GlossaryEntry> = emptyList(),
    val mode: ExplanationMode = ExplanationMode.SIMPLE,
    val bookmarked: Boolean = false,
    val masteryScore: Double = 0.0,
    val band: MasteryBand = MasteryBand.UNSEEN,
) {
    val done get() = lessons.count { it.completed }
    val progress get() = if (lessons.isEmpty()) 0f else done.toFloat() / lessons.size
    val nextLesson get() = lessons.firstOrNull { !it.completed }?.lesson ?: lessons.firstOrNull()?.lesson
}

@HiltViewModel
class TopicViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val settings: SettingsStore,
    stats: StatsRepository,
) : ViewModel() {
    val topicId: String = savedState.get<String>("topicId").orEmpty()
    private val glossary = MutableStateFlow<List<GlossaryEntry>>(emptyList())
    private val topicFlow = MutableStateFlow<Topic?>(null)
    private val missing = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val t = content.topic(topicId)
            if (t == null) missing.value = true else topicFlow.value = t
            glossary.value = content.glossaryOfTopic(topicId)
        }
    }

    val state: StateFlow<TopicUiState> = combine(
        combine(topicFlow, content.lessonsOfTopic(topicId), progress.lessonProgress(), content.questionsOfTopic(topicId)) { t, l, p, q ->
            arrayOf<Any?>(t, l.map { LessonItem(it, p[it.id]?.completed == true) }, q)
        },
        settings.settings,
        progress.isBookmarked(BookmarkType.TOPIC, topicId),
        stats.snapshot(),
        combine(glossary, missing, content.tracks()) { g, m, tr -> Triple(g, m, tr) },
    ) { arr, st, bm, snap, extra ->
        @Suppress("UNCHECKED_CAST")
        val t = arr[0] as Topic?
        @Suppress("UNCHECKED_CAST")
        val lessons = arr[1] as List<LessonItem>
        @Suppress("UNCHECKED_CAST")
        val qs = arr[2] as List<InterviewQuestion>
        val m = snap.mastery[topicId]
        TopicUiState(
            loading = t == null && !extra.second,
            notFound = extra.second,
            topic = t,
            trackTitle = extra.third.firstOrNull { it.id == t?.trackId }?.title.orEmpty(),
            lessons = lessons,
            questions = qs.sortedWith(compareBy({ it.personal }, { it.difficulty.rank })).take(8),
            glossary = extra.first,
            mode = st.explanationMode,
            bookmarked = bm,
            masteryScore = m?.score ?: 0.0,
            band = m?.band ?: MasteryBand.UNSEEN,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TopicUiState())

    fun setMode(m: ExplanationMode) = viewModelScope.launch { settings.setExplanationMode(m) }

    fun toggleBookmark() = viewModelScope.launch {
        val s = state.value
        val t = s.topic ?: return@launch
        progress.setBookmark(BookmarkType.TOPIC, t.id, t.title, s.trackTitle, !s.bookmarked)
    }
}

// ------------------------------------------------------------------ Lesson
data class LessonUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val lesson: Lesson? = null,
    val blocks: List<MdBlock> = emptyList(),
    val completed: Boolean = false,
    val bookmarked: Boolean = false,
    val nextId: String? = null,
    val prevId: String? = null,
    val position: Int = 0,
    val count: Int = 0,
    val topicTitle: String = "",
)

@HiltViewModel
class LessonViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val tracker: StudyTracker,
) : ViewModel() {
    private val lessonId: String = savedState.get<String>("lessonId").orEmpty()
    private val base = MutableStateFlow(LessonUiState())
    private val startedAt = tracker.start()
    private var topicId: String? = null

    val state: StateFlow<LessonUiState> = combine(
        base,
        progress.lessonProgress().map { it[lessonId]?.completed == true },
        progress.isBookmarked(BookmarkType.LESSON, lessonId),
    ) { b, done, bm -> b.copy(completed = done, bookmarked = bm) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LessonUiState())

    init { load() }

    fun load() = viewModelScope.launch {
        base.value = LessonUiState(loading = true)
        val l = content.lesson(lessonId)
        if (l == null) {
            base.value = LessonUiState(loading = false, error = "This lesson isn't in the library. It may have been removed in a content update.")
            return@launch
        }
        topicId = l.summary.topicId
        val siblings = content.lessonsOfTopic(l.summary.topicId).first()
        val idx = siblings.indexOfFirst { it.id == lessonId }
        val topic = content.topic(l.summary.topicId)
        base.value = LessonUiState(
            loading = false,
            lesson = l,
            blocks = Markdown.parse(l.body),
            nextId = siblings.getOrNull(idx + 1)?.id,
            prevId = siblings.getOrNull(idx - 1)?.id,
            position = idx + 1,
            count = siblings.size,
            topicTitle = topic?.title.orEmpty(),
        )
        progress.openLesson(lessonId, l.summary.topicId)
    }

    fun setCompleted(done: Boolean) = viewModelScope.launch {
        val l = base.value.lesson ?: return@launch
        progress.setLessonCompleted(l.summary.id, l.summary.topicId, done)
    }

    fun toggleBookmark() = viewModelScope.launch {
        val s = state.value
        val l = s.lesson ?: return@launch
        progress.setBookmark(BookmarkType.LESSON, l.summary.id, l.summary.title, s.topicTitle, !s.bookmarked)
    }

    override fun onCleared() {
        tracker.stop(ActivityKind.LESSON, startedAt, topicId)
    }
}
