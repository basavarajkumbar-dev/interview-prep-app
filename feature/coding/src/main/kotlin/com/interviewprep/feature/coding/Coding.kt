package com.interviewprep.feature.coding

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.interviewprep.core.data.db.UserDatabase
import com.interviewprep.core.data.repo.ContentRepository
import com.interviewprep.core.data.repo.ProgressRepository
import com.interviewprep.core.data.repo.StudyTracker
import com.interviewprep.core.designsystem.AppCard
import com.interviewprep.core.designsystem.AppColors
import com.interviewprep.core.designsystem.BookmarkToggle
import com.interviewprep.core.designsystem.CodeBlock
import com.interviewprep.core.designsystem.DifficultyPill
import com.interviewprep.core.designsystem.ErrorState
import com.interviewprep.core.designsystem.ListRow
import com.interviewprep.core.designsystem.LoadingState
import com.interviewprep.core.designsystem.MarkdownText
import com.interviewprep.core.designsystem.Pill
import com.interviewprep.core.designsystem.PrimaryAction
import com.interviewprep.core.designsystem.Routes
import com.interviewprep.core.designsystem.ScreenTopBar
import com.interviewprep.core.designsystem.SecondaryAction
import com.interviewprep.core.designsystem.SectionTitle
import com.interviewprep.core.designsystem.SourceButton
import com.interviewprep.core.domain.CodeChecks
import com.interviewprep.core.domain.CodeRunner
import com.interviewprep.core.domain.RunResult
import com.interviewprep.core.model.ActivityKind
import com.interviewprep.core.model.BookmarkType
import com.interviewprep.core.model.CodingChallenge
import com.interviewprep.core.model.Difficulty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ================================================================== List
data class CodingListState(
    val loading: Boolean = true,
    val sets: List<Pair<String, List<CodingChallenge>>> = emptyList(),
    val solved: Set<String> = emptySet(),
    val attempted: Set<String> = emptySet(),
    val difficulty: Difficulty? = null,
)

@HiltViewModel
class CodingListViewModel @Inject constructor(content: ContentRepository, db: UserDatabase) : ViewModel() {
    private val difficulty = MutableStateFlow<Difficulty?>(null)
    val state: StateFlow<CodingListState> = combine(content.coding(), db.userDao().codingAttempts(), difficulty) { list, attempts, d ->
        CodingListState(
            loading = false,
            sets = list.filter { d == null || it.difficulty == d }.groupBy { it.set }.toList(),
            solved = attempts.filter { it.total > 0 && it.passed == it.total }.map { it.challengeId }.toSet(),
            attempted = attempts.map { it.challengeId }.toSet(),
            difficulty = d,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CodingListState())

    fun setDifficulty(d: Difficulty?) { difficulty.value = d }
}

@Composable
fun CodingListScreen(navigate: (String) -> Unit, onBack: () -> Unit, vm: CodingListViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { ScreenTopBar("Coding practice", onBack) }) { pad ->
        if (s.loading) { LoadingState(modifier = Modifier.padding(pad)); return@Scaffold }
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Text("${s.solved.size} of ${s.sets.sumOf { it.second.size }} solved · problems from Python Handbook Ch. 42",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = s.difficulty == null, onClick = { vm.setDifficulty(null) }, label = { Text("All") }) }
                    items(listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD)) { d ->
                        FilterChip(selected = s.difficulty == d, onClick = { vm.setDifficulty(if (s.difficulty == d) null else d) }, label = { Text(d.label) })
                    }
                }
            }
            s.sets.forEach { (set, list) ->
                item(key = "h-$set") { SectionTitle("Set $set") }
                items(list, key = { it.id }) { c ->
                    ListRow(
                        title = "${c.code}. ${c.title}",
                        subtitle = "${c.difficulty.label} · ${c.tests.count { "assert" in it }} tests",
                        leading = when { c.id in s.solved -> "✅"; c.id in s.attempted -> "🟡"; else -> "💻" },
                        onClick = { navigate(Routes.codingChallenge(c.id)) },
                    )
                }
            }
        }
    }
}

// ================================================================== Challenge
data class ChallengeUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val challenge: CodingChallenge? = null,
    val code: TextFieldValue = TextFieldValue(""),
    val running: Boolean = false,
    val result: RunResult? = null,
    val showHint: Boolean = false,
    val showSolution: Boolean = false,
    val runnerAvailable: Boolean = true,
    val runnerNote: String = "",
    val missing: List<String> = emptyList(),
    val bookmarked: Boolean = false,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class ChallengeViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val runner: CodeRunner,
    private val tracker: StudyTracker,
) : ViewModel() {
    private val id = savedState.get<String>("challengeId").orEmpty()
    private val base = MutableStateFlow(ChallengeUiState(runnerAvailable = runner.available, runnerNote = runner.description))
    private val startedAt = tracker.start()

    val state: StateFlow<ChallengeUiState> = combine(base, progress.isBookmarked(BookmarkType.CODING, id)) { b, bm -> b.copy(bookmarked = bm) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), base.value)

    init {
        viewModelScope.launch {
            val c = content.codingChallenge(id)
            if (c == null) { base.update { it.copy(loading = false, error = "Challenge not found.") }; return@launch }
            val draft = progress.draft(id) ?: CodeChecks.starter(c.solution, c.entryPoints)
            base.update { it.copy(loading = false, challenge = c, code = TextFieldValue(draft)) }
        }
        // autosave drafts
        viewModelScope.launch {
            base.map { it.code.text }.drop(1).debounce(800).collect { if (it.isNotBlank()) progress.saveDraft(id, it) }
        }
    }

    fun setCode(v: TextFieldValue) = base.update { it.copy(code = v, result = null) }

    /** Phone keyboards lack Tab and some symbols; insert at the cursor. */
    fun insert(text: String) = base.update { s ->
        val v = s.code
        val start = minOf(v.selection.start, v.selection.end)
        val end = maxOf(v.selection.start, v.selection.end)
        val newText = v.text.substring(0, start) + text + v.text.substring(end)
        s.copy(code = TextFieldValue(newText, TextRange(start + text.length)))
    }

    fun run() {
        val c = base.value.challenge ?: return
        val code = base.value.code.text
        val missing = CodeChecks.missingEntryPoints(code, c.entryPoints)
        base.update { it.copy(running = true, missing = missing) }
        viewModelScope.launch {
            val r = runCatching { runner.run(code, c.tests) }.getOrElse {
                RunResult(executed = false, compileError = "Runner error: ${it.message}", outcomes = emptyList(), stdout = "")
            }
            base.update { it.copy(running = false, result = r) }
            if (r.executed) progress.recordCoding(c.id, code, r.passed, r.total, executed = true)
        }
    }

    /** Without an interpreter: user compares with the reference and marks the result. */
    fun selfCheck(passed: Boolean) = viewModelScope.launch {
        val c = base.value.challenge ?: return@launch
        val total = c.tests.count { "assert" in it }.coerceAtLeast(1)
        progress.recordCoding(c.id, base.value.code.text, if (passed) total else 0, total, executed = false)
        base.update { it.copy(showSolution = true) }
    }

    fun toggleHint() = base.update { it.copy(showHint = !it.showHint) }
    fun revealSolution() = base.update { it.copy(showSolution = true) }
    fun resetCode() = base.update { s -> s.challenge?.let { s.copy(code = TextFieldValue(CodeChecks.starter(it.solution, it.entryPoints)), result = null) } ?: s }

    fun toggleBookmark() = viewModelScope.launch {
        val s = state.value
        val c = s.challenge ?: return@launch
        progress.setBookmark(BookmarkType.CODING, c.id, "${c.code}. ${c.title}", c.set, !s.bookmarked)
    }

    override fun onCleared() = tracker.stop(ActivityKind.CODING, startedAt, "coding-sets")
}

@Composable
fun ChallengeScreen(onBack: () -> Unit, vm: ChallengeViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        ScreenTopBar(s.challenge?.let { "${it.code}. ${it.title}" } ?: "Challenge", onBack, actions = { BookmarkToggle(s.bookmarked) { vm.toggleBookmark() } })
    }) { pad ->
        val c = s.challenge
        when {
            s.loading -> LoadingState(modifier = Modifier.padding(pad))
            s.error != null || c == null -> ErrorState(s.error ?: "Challenge not found.", modifier = Modifier.padding(pad))
            else -> LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { DifficultyPill(c.difficulty); Pill("Set ${c.set}"); Pill("${c.tests.count { "assert" in it }} tests") } }
                item { AppCard { Text("Problem", style = MaterialTheme.typography.labelLarge); MarkdownText(c.statement) } }
                if (c.examples.isNotEmpty()) item {
                    Column { Text("Example", style = MaterialTheme.typography.labelLarge); c.examples.forEach { CodeBlock(it, label = "EXAMPLE") } }
                }
                c.hint?.let { hint ->
                    item {
                        TextButton(onClick = vm::toggleHint) { Text(if (s.showHint) "Hide hint" else "💡 Show hint") }
                        if (s.showHint) Text(hint, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                item { SectionTitle("Your solution (Python)") }
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("⇥ Tab" to "    ", "( )" to "()", "[ ]" to "[]", "{ }" to "{}", ":" to ":", "\" \"" to "\"\"", "=" to "=", "_" to "_", "#" to "#").forEach { (label, ins) ->
                            AssistChip(onClick = { vm.insert(ins) }, label = { Text(label, fontFamily = FontFamily.Monospace) })
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = s.code, onValueChange = vm::setCode,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
                        label = { Text("Code") },
                    )
                }
                if (s.missing.isNotEmpty()) item {
                    Text("Your code must define: ${s.missing.joinToString()}", color = AppColors.extended.danger, style = MaterialTheme.typography.bodyMedium)
                }
                item {
                    if (s.runnerAvailable) {
                        PrimaryAction(if (s.running) "Running tests…" else "Run tests", vm::run, enabled = !s.running)
                    } else {
                        Text(s.runnerNote, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SecondaryAction("Mine differs", { vm.selfCheck(false) }, Modifier.weight(1f))
                            PrimaryAction("Mine matches", { vm.selfCheck(true) }, Modifier.weight(1f))
                        }
                    }
                }
                s.result?.let { r -> item { ResultCard(r) } }
                item {
                    Row {
                        TextButton(onClick = vm::resetCode) { Text("Reset code") }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = vm::revealSolution) { Text("Show reference solution") }
                    }
                }
                if (s.showSolution) {
                    item { CodeBlock(c.solution, label = "REFERENCE SOLUTION (HANDBOOK)") }
                    c.complexity?.let { item { Text(it, style = MaterialTheme.typography.bodyMedium) } }
                    c.notes?.let { item { Text(it, style = MaterialTheme.typography.bodySmall) } }
                    item { CodeBlock(c.tests.joinToString("\n"), label = "TESTS") }
                }
                item { SourceButton(c.source) }
            }
        }
    }
}

@Composable
private fun ResultCard(r: RunResult) {
    val ext = AppColors.extended
    val allPassed = r.total > 0 && r.passed == r.total && r.compileError == null
    AppCard(container = if (allPassed) ext.successContainer else ext.dangerContainer) {
        Text(if (allPassed) "✅ Passed" else "❌ Failed", style = MaterialTheme.typography.titleLarge)
        Text("Test cases passed: ${r.passed}/${r.total}", style = MaterialTheme.typography.titleSmall)
        r.compileError?.let { Text(it, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace) }
        if (r.timedOut) Text("Stopped: time limit exceeded — check for an infinite loop.", style = MaterialTheme.typography.bodyMedium)
        r.outcomes.forEach { o ->
            Spacer(Modifier.height(6.dp))
            Text((if (o.passed) "✅ " else "❌ ") + o.test.lineSequence().first(), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            if (!o.passed && o.message != null) Text("   ${o.message}", style = MaterialTheme.typography.bodySmall, color = ext.danger)
        }
        if (r.stdout.isNotBlank()) { Spacer(Modifier.height(6.dp)); CodeBlock(r.stdout, isOutput = true, label = "PRINTED OUTPUT") }
    }
}

fun NavGraphBuilder.codingGraph(nav: NavHostController) {
    composable(Routes.CODING) { CodingListScreen(navigate = { nav.navigate(it) }, onBack = { nav.popBackStack() }) }
    composable(Routes.CODING_CHALLENGE, arguments = listOf(navArgument("challengeId") { type = NavType.StringType })) {
        ChallengeScreen(onBack = { nav.popBackStack() })
    }
}
