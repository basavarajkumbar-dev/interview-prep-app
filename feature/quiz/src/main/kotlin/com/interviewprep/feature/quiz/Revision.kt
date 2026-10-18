package com.interviewprep.feature.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.interviewprep.core.data.repo.ContentRepository
import com.interviewprep.core.data.repo.ProgressRepository
import com.interviewprep.core.data.repo.SelfRating
import com.interviewprep.core.data.repo.StatsRepository
import com.interviewprep.core.data.repo.StudyTracker
import com.interviewprep.core.data.repo.TimeProvider
import com.interviewprep.core.designsystem.AppCard
import com.interviewprep.core.designsystem.CodeBlock
import com.interviewprep.core.designsystem.EmptyState
import com.interviewprep.core.designsystem.InlineText
import com.interviewprep.core.designsystem.LoadingState
import com.interviewprep.core.designsystem.MarkdownText
import com.interviewprep.core.designsystem.Pill
import com.interviewprep.core.designsystem.PrimaryAction
import com.interviewprep.core.designsystem.ProgressLine
import com.interviewprep.core.designsystem.Routes
import com.interviewprep.core.designsystem.ScreenTopBar
import com.interviewprep.core.designsystem.SecondaryAction
import com.interviewprep.core.designsystem.SourceButton
import com.interviewprep.core.domain.Mastery
import com.interviewprep.core.domain.QuizAnswer
import com.interviewprep.core.domain.QuizGrader
import com.interviewprep.core.domain.SessionComposer
import com.interviewprep.core.domain.SpacedRepetition
import com.interviewprep.core.model.ActivityKind
import com.interviewprep.core.model.CardRating
import com.interviewprep.core.model.Flashcard
import com.interviewprep.core.model.InterviewQuestion
import com.interviewprep.core.model.QuizItem
import com.interviewprep.core.model.QuizType
import com.interviewprep.core.model.Scenario
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RevisionStep {
    data class Card(val card: Flashcard) : RevisionStep
    data class Question(val q: InterviewQuestion) : RevisionStep
    data class Quiz(val item: QuizItem) : RevisionStep
    data class Scen(val s: Scenario) : RevisionStep
}

data class RevisionUiState(
    val loading: Boolean = true,
    val steps: List<RevisionStep> = emptyList(),
    val index: Int = 0,
    val revealed: Boolean = false,
    val selected: Int? = null,
    val done: Boolean = false,
) {
    val current get() = steps.getOrNull(index)
}

@HiltViewModel
class RevisionViewModel @Inject constructor(
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val stats: StatsRepository,
    private val time: TimeProvider,
    private val tracker: StudyTracker,
) : ViewModel() {
    private val startedAt = tracker.start()
    private val _state = MutableStateFlow(RevisionUiState())
    val state: StateFlow<RevisionUiState> = _state.asStateFlow()
    private var schedules = emptyMap<String, com.interviewprep.core.model.CardSchedule>()

    init {
        viewModelScope.launch {
            val snap = stats.snapshot().first()
            schedules = progress.cardSchedules()
            val cards = content.allFlashcards()
            val due = SpacedRepetition.orderForSession(cards.map { it.id }, schedules, time.now(), limit = 40, newCardLimit = 0).toSet()
            val focus = Mastery.weakTopics(snap.mastery.values.toList()).map { it.topicId }.toSet()
            val r = SessionComposer().quickRevision(
                cards, content.allQuestions(),
                content.quizPool().filter { it.type in setOf(QuizType.MCQ, QuizType.TRUE_FALSE, QuizType.SCENARIO, QuizType.DEBUGGING) },
                content.allScenarios(), focus, due,
            )
            val steps = r.flashcards.map { RevisionStep.Card(it) } + r.questions.map { RevisionStep.Question(it) } +
                r.quiz.map { RevisionStep.Quiz(it) } + listOfNotNull(r.scenario?.let { RevisionStep.Scen(it) })
            _state.value = RevisionUiState(loading = false, steps = steps)
        }
    }

    fun reveal() = _state.update { it.copy(revealed = true) }

    fun rateCard(r: CardRating) {
        val step = _state.value.current as? RevisionStep.Card ?: return
        viewModelScope.launch { progress.rateCard(step.card.id, step.card.topicId, r, schedules[step.card.id]) }
        next()
    }

    fun rateQuestion(r: SelfRating) {
        val step = _state.value.current as? RevisionStep.Question ?: return
        viewModelScope.launch { progress.recordPractice(step.q.id, step.q.topicId, "REVISION", selfRating = r) }
        next()
    }

    fun choose(i: Int) {
        val step = _state.value.current as? RevisionStep.Quiz ?: return
        if (_state.value.revealed) return
        val g = QuizGrader.grade(step.item, QuizAnswer.Choice(setOf(i)))
        _state.update { it.copy(selected = i, revealed = true) }
        viewModelScope.launch { progress.recordQuizAnswer(step.item, g.correct, g.score, sessionId = null) }
    }

    fun next() = _state.update {
        if (it.index + 1 >= it.steps.size) it.copy(done = true) else it.copy(index = it.index + 1, revealed = false, selected = null)
    }

    override fun onCleared() = tracker.stop(ActivityKind.REVISION, startedAt)
}

@Composable
fun RevisionScreen(navigate: (String) -> Unit, onBack: () -> Unit, vm: RevisionViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { ScreenTopBar("15-minute revision", onBack) }) { pad ->
        when {
            s.loading -> LoadingState("Picking what matters most…", Modifier.padding(pad))
            s.steps.isEmpty() -> EmptyState("📭", "Nothing to revise", "Content isn't available yet.", Modifier.padding(pad))
            s.done -> EmptyState("⚡", "Revision complete", "Nice work. Weak topics and missed cards will come back sooner.", Modifier.padding(pad), "Done", onBack)
            else -> {
                val step = s.current!!
                LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        val (label, n) = when (step) {
                            is RevisionStep.Card -> "Flashcard" to s.steps.count { it is RevisionStep.Card }
                            is RevisionStep.Question -> "Interview question" to s.steps.count { it is RevisionStep.Question }
                            is RevisionStep.Quiz -> "Quiz question" to s.steps.count { it is RevisionStep.Quiz }
                            is RevisionStep.Scen -> "Scenario" to 1
                        }
                        Text("Step ${s.index + 1} of ${s.steps.size} · $label", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        ProgressLine((s.index + 1f) / s.steps.size)
                    }
                    when (step) {
                        is RevisionStep.Card -> {
                            item { AppCard { InlineText(step.card.front, style = MaterialTheme.typography.titleLarge) } }
                            if (!s.revealed) item { PrimaryAction("Reveal answer", vm::reveal) } else {
                                item { AppCard(container = MaterialTheme.colorScheme.primaryContainer) { InlineText(step.card.back) } }
                                item {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { vm.rateCard(CardRating.DONT_KNOW) }, modifier = Modifier.weight(1f)) { Text("❌") }
                                        OutlinedButton(onClick = { vm.rateCard(CardRating.REVIEW) }, modifier = Modifier.weight(1f)) { Text("🤔") }
                                        OutlinedButton(onClick = { vm.rateCard(CardRating.KNOW) }, modifier = Modifier.weight(1f)) { Text("👍") }
                                    }
                                }
                                item { SourceButton(step.card.source) }
                            }
                        }
                        is RevisionStep.Question -> {
                            item { Text(step.q.question, style = MaterialTheme.typography.titleLarge) }
                            if (!s.revealed) item { PrimaryAction("Show answer", vm::reveal) } else {
                                item { AppCard { MarkdownText(step.q.answer) } }
                                item {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { vm.rateQuestion(SelfRating.MISSED) }, modifier = Modifier.weight(1f)) { Text("Missed") }
                                        OutlinedButton(onClick = { vm.rateQuestion(SelfRating.PARTLY) }, modifier = Modifier.weight(1f)) { Text("Partly") }
                                        OutlinedButton(onClick = { vm.rateQuestion(SelfRating.GOT_IT) }, modifier = Modifier.weight(1f)) { Text("Got it") }
                                    }
                                }
                                item { SourceButton(step.q.source) }
                            }
                        }
                        is RevisionStep.Quiz -> {
                            item { InlineText(step.item.prompt, style = MaterialTheme.typography.titleMedium) }
                            step.item.code?.let { c -> item { CodeBlock(c) } }
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    step.item.options.forEachIndexed { i, o ->
                                        val mark = when {
                                            !s.revealed -> ""
                                            i in step.item.correct -> "✅ "
                                            i == s.selected -> "❌ "
                                            else -> ""
                                        }
                                        OutlinedButton(onClick = { vm.choose(i) }, modifier = Modifier.fillMaxWidth()) { InlineText(mark + o) }
                                    }
                                }
                            }
                            if (s.revealed) {
                                item { AppCard { MarkdownText(step.item.explanation) } }
                                item { PrimaryAction("Next", vm::next) }
                            }
                        }
                        is RevisionStep.Scen -> {
                            item { Pill("Think it through, then reveal the ideal flow") }
                            item { Text(step.s.prompt, style = MaterialTheme.typography.titleLarge) }
                            if (!s.revealed) item { PrimaryAction("Reveal ideal flow", vm::reveal) } else {
                                step.s.steps.forEach { st ->
                                    item { AppCard { Text(st.label, style = MaterialTheme.typography.labelLarge); MarkdownText(st.text) } }
                                }
                                item { SourceButton(step.s.source) }
                                item { PrimaryAction("Finish", vm::next) }
                                item { SecondaryAction("Practise this scenario properly", { navigate(Routes.scenario(step.s.id)) }) }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun NavGraphBuilder.quizGraph(nav: NavHostController) {
    composable(Routes.QUIZ) { QuizHubScreen(navigate = { nav.navigate(it) }) }
    composable(
        Routes.QUIZ_RUN,
        arguments = listOf(
            navArgument("mode") { type = NavType.StringType; defaultValue = "ADAPTIVE" },
            navArgument("topic") { type = NavType.StringType; defaultValue = "" },
        ),
    ) { QuizRunScreen(navigate = { nav.navigate(it) }, onBack = { nav.popBackStack() }) }
    composable(Routes.REVISION) { RevisionScreen(navigate = { nav.navigate(it) }, onBack = { nav.popBackStack() }) }
}
