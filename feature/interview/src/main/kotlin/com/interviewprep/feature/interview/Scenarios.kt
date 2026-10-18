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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.interviewprep.core.data.repo.ContentRepository
import com.interviewprep.core.data.repo.ProgressRepository
import com.interviewprep.core.data.repo.StudyTracker
import com.interviewprep.core.designsystem.AppCard
import com.interviewprep.core.designsystem.AppColors
import com.interviewprep.core.designsystem.BookmarkToggle
import com.interviewprep.core.designsystem.EmptyState
import com.interviewprep.core.designsystem.ErrorState
import com.interviewprep.core.designsystem.LabeledProgress
import com.interviewprep.core.designsystem.ListRow
import com.interviewprep.core.designsystem.LoadingState
import com.interviewprep.core.designsystem.MarkdownText
import com.interviewprep.core.designsystem.Pill
import com.interviewprep.core.designsystem.PrimaryAction
import com.interviewprep.core.designsystem.Routes
import com.interviewprep.core.designsystem.ScreenTopBar
import com.interviewprep.core.designsystem.SecondaryAction
import com.interviewprep.core.designsystem.SourceButton
import com.interviewprep.core.designsystem.Handbooks
import com.interviewprep.core.domain.AnswerScorer
import com.interviewprep.core.domain.ScenarioEvaluation
import com.interviewprep.core.model.ActivityKind
import com.interviewprep.core.model.BookmarkType
import com.interviewprep.core.model.Scenario
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScenarioListState(
    val loading: Boolean = true,
    val items: List<Scenario> = emptyList(),
    val book: String? = null,
    val done: Map<String, Int> = emptyMap(), // best score per scenario
)

@HiltViewModel
class ScenarioListViewModel @Inject constructor(content: ContentRepository, progress: ProgressRepository, db: com.interviewprep.core.data.db.UserDatabase) : ViewModel() {
    private val book = MutableStateFlow<String?>(null)
    val state: StateFlow<ScenarioListState> = combine(content.scenarios(), book, db.userDao().scenarioAttempts()) { list, b, attempts ->
        ScenarioListState(
            false,
            list.filter { b == null || it.source.book == b },
            b,
            attempts.groupBy { it.scenarioId }.mapValues { (_, v) -> v.maxOf { it.overall10 } },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScenarioListState())

    fun setBook(b: String?) { book.value = b }
}

@Composable
fun ScenarioListScreen(navigate: (String) -> Unit, onBack: () -> Unit, vm: ScenarioListViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { ScreenTopBar("Troubleshooting scenarios", onBack) }) { pad ->
        if (s.loading) { LoadingState(modifier = Modifier.padding(pad)); return@Scaffold }
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = s.book == null, onClick = { vm.setBook(null) }, label = { Text("All") }) }
                    items(listOf("bmcssd", "api", "linux", "cicd")) { b ->
                        FilterChip(selected = s.book == b, onClick = { vm.setBook(if (s.book == b) null else b) }, label = { Text(Handbooks.short(b)) })
                    }
                }
            }
            if (s.items.isEmpty()) item { EmptyState("🔍", "No scenarios", "No scenarios for this filter.") }
            items(s.items, key = { it.id }) { sc ->
                ListRow(
                    title = sc.title,
                    subtitle = "${Handbooks.short(sc.source.book)} · ${sc.steps.size} steps" + (if (sc.concept) " · concept (not on resume)" else "") +
                        (s.done[sc.id]?.let { " · best $it/10" } ?: ""),
                    leading = if (s.done.containsKey(sc.id)) "✅" else "🔍",
                    onClick = { navigate(Routes.scenario(sc.id)) },
                )
            }
        }
    }
}

data class ScenarioUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val scenario: Scenario? = null,
    val typed: String = "",
    val evaluation: ScenarioEvaluation? = null,
    val showIdeal: Boolean = false,
    val bookmarked: Boolean = false,
)

@HiltViewModel
class ScenarioViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val tracker: StudyTracker,
) : ViewModel() {
    private val id = savedState.get<String>("scenarioId").orEmpty()
    private val base = MutableStateFlow(ScenarioUiState())
    private val startedAt = tracker.start()
    val state: StateFlow<ScenarioUiState> = combine(base, progress.isBookmarked(BookmarkType.SCENARIO, id)) { b, bm -> b.copy(bookmarked = bm) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScenarioUiState())

    init {
        viewModelScope.launch {
            val sc = content.scenario(id)
            base.value = if (sc == null) ScenarioUiState(loading = false, error = "Scenario not found.") else ScenarioUiState(loading = false, scenario = sc)
        }
    }

    fun setTyped(t: String) = base.update { it.copy(typed = t) }

    fun evaluate() {
        val sc = base.value.scenario ?: return
        val e = AnswerScorer.evaluateScenario(base.value.typed, sc)
        base.update { it.copy(evaluation = e, showIdeal = true) }
        viewModelScope.launch { progress.recordScenario(sc.id, sc.topicId, base.value.typed, e) }
    }

    fun showIdeal() = base.update { it.copy(showIdeal = true) }

    fun toggleBookmark() = viewModelScope.launch {
        val s = state.value
        val sc = s.scenario ?: return@launch
        progress.setBookmark(BookmarkType.SCENARIO, sc.id, sc.title, Handbooks.short(sc.source.book), !s.bookmarked)
    }

    override fun onCleared() = tracker.stop(ActivityKind.SCENARIO, startedAt, base.value.scenario?.topicId)
}

@Composable
fun ScenarioScreen(navigate: (String) -> Unit, onBack: () -> Unit, vm: ScenarioViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { ScreenTopBar("Scenario", onBack, actions = { BookmarkToggle(s.bookmarked) { vm.toggleBookmark() } }) }) { pad ->
        val sc = s.scenario
        when {
            s.loading -> LoadingState(modifier = Modifier.padding(pad))
            s.error != null || sc == null -> ErrorState(s.error ?: "Scenario not found.", modifier = Modifier.padding(pad))
            else -> LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Pill(Handbooks.short(sc.source.book))
                        if (sc.concept) Pill("Concept — not on your resume")
                    }
                }
                item { Text(sc.prompt, style = MaterialTheme.typography.headlineSmall) }
                if (!s.showIdeal) {
                    item {
                        Text("Describe what you'd check, in order, what the likely causes are and how you'd report it.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item {
                        OutlinedTextField(value = s.typed, onValueChange = vm::setTyped, modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                            label = { Text("Your troubleshooting approach") })
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PrimaryAction("Evaluate my approach", vm::evaluate, enabled = s.typed.isNotBlank())
                            SecondaryAction("Show the ideal flow", vm::showIdeal)
                        }
                    }
                } else {
                    s.evaluation?.let { e -> item { ScenarioScoreCard(e) } }
                    item { Text("Ideal troubleshooting flow", style = MaterialTheme.typography.titleMedium) }
                    itemsIndexed(sc.steps) { i, st ->
                        AppCard {
                            Text("${i + 1}. ${st.label}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            MarkdownText(st.text)
                        }
                    }
                    item { SourceButton(sc.source) }
                    item { androidx.compose.material3.TextButton(onClick = { navigate(Routes.topic(sc.topicId)) }) { Text("Revise the related topic") } }
                }
            }
        }
    }
}

@Composable
private fun ScenarioScoreCard(e: ScenarioEvaluation) {
    val ext = AppColors.extended
    AppCard(container = if (e.overall10 >= 7) ext.successContainer else if (e.overall10 >= 4) ext.warningContainer else ext.dangerContainer) {
        Text("Overall: ${e.overall10}/10", style = MaterialTheme.typography.titleLarge)
        Text("Offline keyword-based evaluation — approximate.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        LabeledProgress("1. Approach", e.approach.toFloat())
        LabeledProgress("2. Technical correctness", e.technical.toFloat())
        LabeledProgress("3. Troubleshooting sequence", e.sequence.toFloat())
        LabeledProgress("5. Communication", e.communication.toFloat())
        Text("4. Missing checks", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp))
        Text(if (e.missedChecks.isEmpty()) "None of the key checks were missed." else e.missedChecks.take(8).joinToString(", "), style = MaterialTheme.typography.bodyMedium)
        e.notes.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
    }
}
