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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.interviewprep.core.data.repo.QuestionFilter
import com.interviewprep.core.data.repo.SelfRating
import com.interviewprep.core.data.repo.StudyTracker
import com.interviewprep.core.designsystem.AppCard
import com.interviewprep.core.designsystem.AppColors
import com.interviewprep.core.designsystem.BookmarkToggle
import com.interviewprep.core.designsystem.DifficultyPill
import com.interviewprep.core.designsystem.EmptyState
import com.interviewprep.core.designsystem.ErrorState
import com.interviewprep.core.designsystem.InlineText
import com.interviewprep.core.designsystem.LoadingState
import com.interviewprep.core.designsystem.MarkdownText
import com.interviewprep.core.designsystem.Pill
import com.interviewprep.core.designsystem.PrimaryAction
import com.interviewprep.core.designsystem.ResumeTagPill
import com.interviewprep.core.designsystem.Routes
import com.interviewprep.core.designsystem.ScreenTopBar
import com.interviewprep.core.designsystem.SecondaryAction
import com.interviewprep.core.designsystem.SectionTitle
import com.interviewprep.core.designsystem.SourceButton
import com.interviewprep.core.domain.AnswerEvaluation
import com.interviewprep.core.domain.AnswerScorer
import com.interviewprep.core.model.ActivityKind
import com.interviewprep.core.model.BookmarkType
import com.interviewprep.core.model.Difficulty
import com.interviewprep.core.model.InterviewQuestion
import com.interviewprep.core.model.QuestionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

val ROLE_FILTERS = listOf(
    "PY" to "Python Automation", "TA" to "Test Automation", "EMB" to "Embedded Testing",
    "BMC" to "BMC/OpenBMC", "SSD" to "SSD/NVMe", "SR" to "Senior Test Engineer",
)

data class BankUiState(
    val loading: Boolean = true,
    val filter: QuestionFilter = QuestionFilter(),
    val questions: List<InterviewQuestion> = emptyList(),
    val topicTitle: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class QuestionBankViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val content: ContentRepository,
) : ViewModel() {
    private val topic = savedState.get<String>("topic")?.takeIf { it.isNotBlank() }
    private val personal = savedState.get<Boolean>("personal") ?: false
    private val filter = MutableStateFlow(QuestionFilter(topicId = topic, personalOnly = personal))
    private val topicTitle = MutableStateFlow<String?>(null)

    init { viewModelScope.launch { topicTitle.value = topic?.let { content.topic(it)?.title } } }

    val state: StateFlow<BankUiState> = combine(
        filter.debounce(200).flatMapLatest { f -> content.questions(f).map { f to it } },
        topicTitle,
        filter,
    ) { (_, qs), title, f -> BankUiState(false, f, qs, title) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BankUiState(filter = filter.value))

    fun setRole(r: String?) = filter.update { it.copy(role = r) }
    fun setDifficulty(d: Difficulty?) = filter.update { it.copy(difficulty = d) }
    fun setType(t: QuestionType?) = filter.update { it.copy(type = t) }
    fun setQuery(q: String) = filter.update { it.copy(query = q) }
    fun setPersonal(p: Boolean) = filter.update { it.copy(personalOnly = p) }
    fun clearTopic() = filter.update { it.copy(topicId = null) }
}

@Composable
fun QuestionBankScreen(navigate: (String) -> Unit, onBack: () -> Unit, vm: QuestionBankViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val f = s.filter
    Scaffold(topBar = { ScreenTopBar(s.topicTitle ?: "Interview question bank", onBack) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                OutlinedTextField(
                    value = f.query.orEmpty(), onValueChange = vm::setQuery, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) }, placeholder = { Text("Search questions and answers") },
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (f.topicId != null) item { FilterChip(selected = true, onClick = vm::clearTopic, label = { Text("This topic ✕") }) }
                    item { FilterChip(selected = f.personalOnly, onClick = { vm.setPersonal(!f.personalOnly) }, label = { Text("From my resume") }) }
                    items(ROLE_FILTERS) { (id, t) -> FilterChip(selected = f.role == id, onClick = { vm.setRole(if (f.role == id) null else id) }, label = { Text(t) }) }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Difficulty.entries) { d -> FilterChip(selected = f.difficulty == d, onClick = { vm.setDifficulty(if (f.difficulty == d) null else d) }, label = { Text(d.label) }) }
                    items(QuestionType.entries) { t -> FilterChip(selected = f.type == t, onClick = { vm.setType(if (f.type == t) null else t) }, label = { Text(t.label) }) }
                }
            }
            item { Text("${s.questions.size}${if (s.questions.size >= f.limit) "+" else ""} questions", style = MaterialTheme.typography.labelLarge) }
            if (!s.loading && s.questions.isEmpty()) item { EmptyState("🔎", "No questions", "Try removing a filter.") }
            items(s.questions, key = { it.id }) { q ->
                AppCard(onClick = { navigate(Routes.question(q.id)) }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DifficultyPill(q.difficulty)
                        Pill(q.type.label)
                        q.resumeTag?.let { ResumeTagPill(it) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(q.question, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

// ================================================================== Question detail
data class QuestionUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val question: InterviewQuestion? = null,
    val revealed: Boolean = false,
    val typed: String = "",
    val evaluation: AnswerEvaluation? = null,
    val rated: SelfRating? = null,
    val bookmarked: Boolean = false,
    val topicTitle: String = "",
)

@HiltViewModel
class QuestionViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val tracker: StudyTracker,
) : ViewModel() {
    private val id = savedState.get<String>("questionId").orEmpty()
    private val base = MutableStateFlow(QuestionUiState())
    private val startedAt = tracker.start()

    val state: StateFlow<QuestionUiState> = combine(base, progress.isBookmarked(BookmarkType.QUESTION, id)) { b, bm -> b.copy(bookmarked = bm) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuestionUiState())

    init {
        viewModelScope.launch {
            val q = content.question(id)
            base.value = if (q == null) QuestionUiState(loading = false, error = "Question not found.")
            else QuestionUiState(loading = false, question = q, topicTitle = content.topic(q.topicId)?.title.orEmpty())
        }
    }

    fun setTyped(t: String) = base.update { it.copy(typed = t) }

    fun evaluate() {
        val q = base.value.question ?: return
        val e = AnswerScorer.evaluate(base.value.typed, q)
        base.update { it.copy(evaluation = e, revealed = true) }
        viewModelScope.launch { progress.recordPractice(q.id, q.topicId, "BANK", base.value.typed, e) }
    }

    fun reveal() = base.update { it.copy(revealed = true) }

    fun rate(r: SelfRating) {
        val q = base.value.question ?: return
        if (base.value.rated != null || base.value.evaluation != null) { base.update { it.copy(rated = r) }; return }
        base.update { it.copy(rated = r) }
        viewModelScope.launch { progress.recordPractice(q.id, q.topicId, "BANK", selfRating = r) }
    }

    fun toggleBookmark() = viewModelScope.launch {
        val s = state.value
        val q = s.question ?: return@launch
        progress.setBookmark(BookmarkType.QUESTION, q.id, q.question, s.topicTitle, !s.bookmarked)
    }

    override fun onCleared() = tracker.stop(ActivityKind.QUESTIONS, startedAt, base.value.question?.topicId)
}

@Composable
fun QuestionScreen(navigate: (String) -> Unit, onBack: () -> Unit, vm: QuestionViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { ScreenTopBar(s.topicTitle.ifBlank { "Question" }, onBack, actions = { BookmarkToggle(s.bookmarked) { vm.toggleBookmark() } }) }) { pad ->
        when {
            s.loading -> LoadingState(modifier = Modifier.padding(pad))
            s.error != null -> ErrorState(s.error!!, modifier = Modifier.padding(pad))
            else -> QuestionBody(s, vm, Modifier.padding(pad), navigate)
        }
    }
}

@Composable
private fun QuestionBody(s: QuestionUiState, vm: QuestionViewModel, modifier: Modifier, navigate: (String) -> Unit) {
    val q = s.question ?: return
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DifficultyPill(q.difficulty)
                Pill(q.type.label)
                q.resumeTag?.let { ResumeTagPill(it) }
            }
        }
        item { Text(q.question, style = MaterialTheme.typography.headlineSmall) }
        q.resumeTag?.let { tag -> item { Text(tag.advice, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        if (!s.revealed) {
            item {
                AppCard(container = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Text("🤔 Think before revealing", style = MaterialTheme.typography.titleSmall)
                    Text("Say your answer aloud, or type it and get offline feedback against the handbook's model answer.", style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                OutlinedTextField(
                    value = s.typed, onValueChange = vm::setTyped, modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                    label = { Text("Your answer (optional)") },
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryAction("Evaluate my answer", vm::evaluate, enabled = s.typed.isNotBlank())
                    SecondaryAction("Show answer", vm::reveal)
                }
            }
        } else {
            s.evaluation?.let { e -> item { EvaluationCard(e) } }
            item {
                AppCard {
                    Text(if (q.personal) "Model answer (from your resume handbook)" else "Answer — concept explanation", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    MarkdownText(q.answer)
                    q.why?.let { Spacer(Modifier.height(8.dp)); Text("Why it's asked / what they evaluate", style = MaterialTheme.typography.labelLarge); InlineText(it, style = MaterialTheme.typography.bodyMedium) }
                    q.commonWrong?.let { Spacer(Modifier.height(8.dp)); Text("Common wrong answer", style = MaterialTheme.typography.labelLarge); InlineText(it, style = MaterialTheme.typography.bodyMedium) }
                    SourceButton(q.source, extra = q.altSources)
                }
            }
            item { HowToAnswer(q) }
            if (q.followUps.isNotEmpty()) item {
                AppCard {
                    Text("Follow-up questions", style = MaterialTheme.typography.labelLarge)
                    q.followUps.forEach { InlineText("• $it", style = MaterialTheme.typography.bodyMedium) }
                }
            }
            if (s.evaluation == null) item {
                Column {
                    Text("How did you do?", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelfRating.entries.forEach { r ->
                            val label = when (r) { SelfRating.GOT_IT -> "👍 Got it"; SelfRating.PARTLY -> "🤔 Partly"; SelfRating.MISSED -> "❌ Missed" }
                            OutlinedButton(onClick = { vm.rate(r) }, modifier = Modifier.weight(1f)) { Text(if (s.rated == r) "✓ $label" else label) }
                        }
                    }
                }
            }
            item { TextButton(onClick = { navigate(Routes.topic(q.topicId)) }) { Text("Open topic: ${s.topicTitle}") } }
        }
    }
}

@Composable
fun EvaluationCard(e: AnswerEvaluation) {
    val ext = AppColors.extended
    val good = e.score10 >= 7
    AppCard(container = if (good) ext.successContainer else if (e.score10 >= 4) ext.warningContainer else ext.dangerContainer) {
        Text("Score: ${e.score10}/10", style = MaterialTheme.typography.titleLarge)
        Text("Offline keyword-based feedback — approximate, not a human judgement.", style = MaterialTheme.typography.bodySmall)
        if (e.strengths.isNotEmpty()) {
            Spacer(Modifier.height(8.dp)); Text("What you did well", style = MaterialTheme.typography.labelLarge)
            e.strengths.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        }
        if (e.gaps.isNotEmpty()) {
            Spacer(Modifier.height(8.dp)); Text("What you missed", style = MaterialTheme.typography.labelLarge)
            e.gaps.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        }
        if (e.matched.isNotEmpty() || e.missed.isNotEmpty()) {
            Spacer(Modifier.height(8.dp)); Text("Important keywords", style = MaterialTheme.typography.labelLarge)
            Text((e.matched.map { "✓ $it" } + e.missed.map { "○ $it" }).joinToString("   "), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

val ANSWER_METHOD_SOURCE = com.interviewprep.core.model.SourceRef(
    "python", "39 Python Interview Questions", "39.1 How to answer a technical question", 871,
)

/** "How YOU can answer": personal answers only come from the resume handbook. */
@Composable
fun HowToAnswer(q: InterviewQuestion) {
    AppCard(container = MaterialTheme.colorScheme.secondaryContainer) {
        Text("How to answer in the interview", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        if (q.personal) {
            q.keyPoints?.let { InlineText("Key points: $it", style = MaterialTheme.typography.bodyMedium) }
            Text(
                "Parts marked [adapt] must be replaced with what you actually did. Never say a sentence that isn't true for you.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Text(
                "1. One-sentence definition.\n2. A tiny example.\n3. Why it matters in your work.\n4. A limitation or trade-off.\nThen stop and let them ask the follow-up.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (q.keywords.isNotEmpty()) Text("Mention: ${q.keywords.take(6).joinToString(", ")}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
            Text(
                "This is the concept answer. Only add personal experience you really have — the handbook doesn't know your projects beyond your resume.",
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp),
            )
            SourceButton(ANSWER_METHOD_SOURCE)
        }
    }
}
