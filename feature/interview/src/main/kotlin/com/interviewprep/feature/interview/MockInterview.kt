package com.interviewprep.feature.interview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.interviewprep.core.data.repo.ContentRepository
import com.interviewprep.core.data.repo.ProgressRepository
import com.interviewprep.core.data.repo.StatsRepository
import com.interviewprep.core.data.repo.StudyTracker
import com.interviewprep.core.designsystem.AppCard
import com.interviewprep.core.designsystem.DifficultyPill
import com.interviewprep.core.designsystem.ErrorState
import com.interviewprep.core.designsystem.LabeledProgress
import com.interviewprep.core.designsystem.LoadingState
import com.interviewprep.core.designsystem.MarkdownText
import com.interviewprep.core.designsystem.Pill
import com.interviewprep.core.designsystem.PrimaryAction
import com.interviewprep.core.designsystem.ProgressLine
import com.interviewprep.core.designsystem.ProgressRing
import com.interviewprep.core.designsystem.Routes
import com.interviewprep.core.designsystem.ScreenTopBar
import com.interviewprep.core.designsystem.SecondaryAction
import com.interviewprep.core.designsystem.Segmented
import com.interviewprep.core.designsystem.SectionTitle
import com.interviewprep.core.designsystem.SourceButton
import com.interviewprep.core.domain.Achievements
import com.interviewprep.core.domain.AnswerEvaluation
import com.interviewprep.core.domain.AnswerScorer
import com.interviewprep.core.domain.ExperienceLevel
import com.interviewprep.core.domain.InterviewReport
import com.interviewprep.core.domain.InterviewReporting
import com.interviewprep.core.domain.InterviewRole
import com.interviewprep.core.domain.Mastery
import com.interviewprep.core.domain.SessionComposer
import com.interviewprep.core.model.ActivityKind
import com.interviewprep.core.model.InterviewQuestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

// ================================================================== Setup
@HiltViewModel
class MockSetupViewModel @Inject constructor(
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val stats: StatsRepository,
) : ViewModel() {
    val busy = MutableStateFlow(false)

    fun start(role: InterviewRole, level: ExperienceLevel, length: Int, onReady: (String) -> Unit) = viewModelScope.launch {
        busy.value = true
        val snap = stats.snapshot().first()
        val questions = content.allQuestions()
        val trackOf = snap.topics.associate { it.id to it.trackId }
        val weak = Mastery.weakTopics(snap.mastery.values.toList()).map { it.topicId }.toSet()
        val picked = SessionComposer().mockInterview(questions, trackOf, role, level, length, weak)
        val id = UUID.randomUUID().toString()
        progress.startInterview(id, role.name, level.name, picked.map { it.id })
        busy.value = false
        onReady(id)
    }
}

@Composable
fun MockSetupScreen(navigate: (String) -> Unit, onBack: () -> Unit, vm: MockSetupViewModel = hiltViewModel()) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    var role by rememberSaveable { mutableStateOf(InterviewRole.SENIOR_TEST_AUTOMATION) }
    var level by rememberSaveable { mutableStateOf(ExperienceLevel.INTERMEDIATE) }
    var length by rememberSaveable { mutableStateOf(10) }
    Scaffold(topBar = { ScreenTopBar("Mock interview", onBack) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("One question at a time. Type your answer; you'll get feedback against the handbook's model answer, then a final report.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { SectionTitle("Role") }
            items(InterviewRole.entries) { r ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = r == role, role = Role.RadioButton, onClick = { role = r }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = r == role, onClick = null)
                    Text(r.label, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }
            item { SectionTitle("Experience level") }
            item { Segmented(ExperienceLevel.entries, level, { it.label }, { level = it }) }
            item { SectionTitle("Interview length") }
            item { Segmented(listOf(10, 20, 30), length, { "$it questions" }, { length = it }) }
            item {
                Spacer(Modifier.height(8.dp))
                PrimaryAction(if (busy) "Preparing…" else "Start interview", { vm.start(role, level, length) { navigate(Routes.mockSession(it)) } }, enabled = !busy)
            }
        }
    }
}

// ================================================================== Session
data class MockUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val questions: List<InterviewQuestion> = emptyList(),
    val index: Int = 0,
    val typed: String = "",
    val evaluation: AnswerEvaluation? = null,
    val results: List<Triple<String, Int, Double>> = emptyList(), // track, score, communication
    val finished: Boolean = false,
    val role: String = "",
) {
    val current get() = questions.getOrNull(index)
}

@Serializable
data class StoredReport(val overall: Int, val technical: Int, val communication: Int, val byArea: Map<String, Int>, val strengths: List<String>, val improve: List<String>, val readiness: Int)

@HiltViewModel
class MockSessionViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val stats: StatsRepository,
    private val tracker: StudyTracker,
) : ViewModel() {
    val sessionId: String = savedState.get<String>("sessionId").orEmpty()
    private val startedAt = tracker.start()
    private val _state = MutableStateFlow(MockUiState())
    val state: StateFlow<MockUiState> = _state.asStateFlow()
    private var trackOf: Map<String, String> = emptyMap()

    init {
        viewModelScope.launch {
            val session = runCatching { progress.interviewSession(sessionId) }.getOrNull()
            if (session == null) { _state.value = MockUiState(loading = false, error = "Interview session not found."); return@launch }
            trackOf = stats.snapshot().first().topics.associate { it.id to it.trackId }
            val qs = content.questionsByIds(session.second)
            _state.value = MockUiState(loading = false, questions = qs, role = session.first)
        }
    }

    fun setTyped(t: String) = _state.update { it.copy(typed = t) }

    fun submit() {
        val s = _state.value
        val q = s.current ?: return
        val e = AnswerScorer.evaluate(s.typed, q)
        _state.update { it.copy(evaluation = e, results = it.results + Triple(trackOf[q.topicId] ?: "career", e.score10, e.communication)) }
        viewModelScope.launch { progress.recordPractice(q.id, q.topicId, "MOCK", s.typed, e, sessionId = sessionId) }
    }

    fun skip() {
        val s = _state.value
        val q = s.current ?: return
        _state.update { it.copy(results = it.results + Triple(trackOf[q.topicId] ?: "career", 0, 0.0)) }
        viewModelScope.launch { progress.recordPractice(q.id, q.topicId, "MOCK", skipped = true, sessionId = sessionId) }
        next()
    }

    fun next() {
        val s = _state.value
        if (s.index + 1 >= s.questions.size) { finish(); return }
        _state.update { it.copy(index = it.index + 1, typed = "", evaluation = null) }
    }

    fun finish() {
        if (_state.value.finished) return
        _state.update { it.copy(finished = true) }
        viewModelScope.launch {
            val snap = stats.snapshot().first()
            val titles = snap.tracks.associate { it.track.id to it.track.title }
            val r = InterviewReporting.report(_state.value.results, titles)
            val stored = StoredReport(r.overall, r.technical, r.communication, r.byArea, r.strengths, r.improve, snap.readiness.score)
            progress.finishInterview(sessionId, r.overall, Json.encodeToString(StoredReport.serializer(), stored))
            progress.unlockAchievements(Achievements.earned(stats.achievementStats(stats.snapshot().first())))
        }
    }

    override fun onCleared() = tracker.stop(ActivityKind.MOCK_INTERVIEW, startedAt)
}

@Composable
fun MockSessionScreen(navigate: (String) -> Unit, replace: (String) -> Unit, onBack: () -> Unit, vm: MockSessionViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { ScreenTopBar("Mock interview", onBack) }) { pad ->
        val q = s.current
        when {
            s.loading -> LoadingState(modifier = Modifier.padding(pad))
            s.error != null -> ErrorState(s.error!!, modifier = Modifier.padding(pad))
            s.finished -> {
                LoadingState("Preparing your report…", Modifier.padding(pad))
                androidx.compose.runtime.LaunchedEffect(Unit) { kotlinx.coroutines.delay(400); replace(Routes.mockReport(vm.sessionId)) }
            }
            q == null -> ErrorState("No questions available for this interview.", modifier = Modifier.padding(pad))
            else -> LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("Question ${s.index + 1} of ${s.questions.size}", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    ProgressLine((s.index + 1f) / s.questions.size)
                }
                item { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { DifficultyPill(q.difficulty); Pill(q.type.label) } }
                item { Text(q.question, style = MaterialTheme.typography.headlineSmall) }
                if (s.evaluation == null) {
                    item {
                        OutlinedTextField(
                            value = s.typed, onValueChange = vm::setTyped, modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                            label = { Text("Type your answer as you'd say it") },
                        )
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PrimaryAction("Submit answer", vm::submit, enabled = s.typed.isNotBlank())
                            Row {
                                TextButton(onClick = vm::skip) { Text("Skip question") }
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = vm::finish) { Text("End interview") }
                            }
                        }
                    }
                } else {
                    item { EvaluationCard(s.evaluation!!) }
                    item {
                        AppCard {
                            Text("Better answer (handbook)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            MarkdownText(q.answer)
                            SourceButton(q.source)
                        }
                    }
                    if (q.followUps.isNotEmpty()) item {
                        AppCard { Text("Follow-up they may ask", style = MaterialTheme.typography.labelLarge); Text(q.followUps.first(), style = MaterialTheme.typography.bodyMedium) }
                    }
                    item { PrimaryAction(if (s.index + 1 >= s.questions.size) "Finish & see report" else "Next question", vm::next) }
                }
            }
        }
    }
}

// ================================================================== Report
@HiltViewModel
class MockReportViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val progress: ProgressRepository,
) : ViewModel() {
    private val sessionId = savedState.get<String>("sessionId").orEmpty()
    val report = MutableStateFlow<StoredReport?>(null)
    val error = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            // the report is written right after the interview ends; retry briefly
            repeat(10) {
                val json = progress.interviewReportJson(sessionId)
                if (json != null) {
                    report.value = runCatching { Json.decodeFromString(StoredReport.serializer(), json) }.getOrNull()
                    if (report.value == null) error.value = "The report couldn't be read."
                    return@launch
                }
                kotlinx.coroutines.delay(300)
            }
            error.value = "No report found for this interview."
        }
    }
}

@Composable
fun MockReportScreen(navigate: (String) -> Unit, onBack: () -> Unit, vm: MockReportViewModel = hiltViewModel()) {
    val r by vm.report.collectAsStateWithLifecycle()
    val err by vm.error.collectAsStateWithLifecycle()
    Scaffold(topBar = { ScreenTopBar("Interview report", onBack) }) { pad ->
        val rep = r
        when {
            err != null -> ErrorState(err!!, modifier = Modifier.padding(pad))
            rep == null -> LoadingState(modifier = Modifier.padding(pad))
            else -> LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        ProgressRing(rep.overall / 100f, size = 140.dp, stroke = 14.dp) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${rep.overall}%", style = MaterialTheme.typography.headlineMedium)
                                Text("This interview", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                item {
                    AppCard {
                        LabeledProgress("Technical knowledge", rep.technical / 100f)
                        rep.byArea.forEach { (area, v) -> LabeledProgress(area, v / 100f) }
                        LabeledProgress("Communication", rep.communication / 100f, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
                item {
                    AppCard(container = MaterialTheme.colorScheme.primaryContainer) {
                        Text("Overall interview readiness: ${rep.readiness}%", style = MaterialTheme.typography.titleMedium)
                        Text("Blends lessons, quizzes, interview answers, scenarios and coding — see Progress.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (rep.strengths.isNotEmpty()) item { AppCard { Text("Strengths", style = MaterialTheme.typography.labelLarge); rep.strengths.forEach { Text("• $it") } } }
                if (rep.improve.isNotEmpty()) item { AppCard { Text("Needs improvement", style = MaterialTheme.typography.labelLarge); rep.improve.forEach { Text("• $it") } } }
                item {
                    Text("Scores are offline keyword estimates against handbook answers; treat them as a guide.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    PrimaryAction("Practise weak topics", { navigate(Routes.quizRun("WEAK")) })
                    Spacer(Modifier.height(8.dp))
                    SecondaryAction("Done", onBack)
                }
            }
        }
    }
}
