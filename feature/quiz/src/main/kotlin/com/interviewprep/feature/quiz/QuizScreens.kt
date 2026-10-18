package com.interviewprep.feature.quiz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.interviewprep.core.designsystem.AppCard
import com.interviewprep.core.designsystem.AppColors
import com.interviewprep.core.designsystem.CodeBlock
import com.interviewprep.core.designsystem.DifficultyPill
import com.interviewprep.core.designsystem.ErrorState
import com.interviewprep.core.designsystem.InlineText
import com.interviewprep.core.designsystem.LabeledProgress
import com.interviewprep.core.designsystem.LoadingState
import com.interviewprep.core.designsystem.MarkdownText
import com.interviewprep.core.designsystem.OriginPill
import com.interviewprep.core.designsystem.Pill
import com.interviewprep.core.designsystem.PrimaryAction
import com.interviewprep.core.designsystem.ProgressLine
import com.interviewprep.core.designsystem.ProgressRing
import com.interviewprep.core.designsystem.Routes
import com.interviewprep.core.designsystem.ScreenTopBar
import com.interviewprep.core.designsystem.SecondaryAction
import com.interviewprep.core.designsystem.SectionTitle
import com.interviewprep.core.designsystem.SourceButton
import com.interviewprep.core.model.QuizItem
import com.interviewprep.core.model.QuizType
import kotlin.math.roundToInt

@Composable
fun QuizRunScreen(navigate: (String) -> Unit, onBack: () -> Unit, vm: QuizViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var confirmExit by remember { mutableStateOf(false) }
    val back: () -> Unit = {
        if (!s.finished && s.answered.isNotEmpty()) {
            confirmExit = true
        } else {
            onBack()
        }
    }
    Scaffold(topBar = {
        ScreenTopBar(s.title.ifBlank { s.mode.title }, back, actions = {
            s.secondsLeft?.let { sec ->
                if (!s.finished) Pill("⏱ ${sec / 60}:${(sec % 60).toString().padStart(2, '0')}", Modifier.padding(end = 12.dp))
            }
        })
    }) { pad ->
        when {
            s.loading -> LoadingState("Building your quiz…", Modifier.padding(pad))
            s.error != null -> ErrorState(s.error!!, onRetry = { vm.load() }, modifier = Modifier.padding(pad))
            s.finished -> QuizSummary(s, Modifier.padding(pad), navigate, onBack)
            else -> QuestionPage(s, vm, Modifier.padding(pad), navigate)
        }
    }
    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Leave the quiz?") },
            text = { Text("Your answers so far are saved. You can see a summary if you finish now.") },
            confirmButton = { TextButton(onClick = { confirmExit = false; vm.endEarly() }) { Text("Finish now") } },
            dismissButton = { TextButton(onClick = { confirmExit = false; onBack() }) { Text("Leave") } },
        )
    }
}

@Composable
private fun QuestionPage(s: QuizUiState, vm: QuizViewModel, modifier: Modifier, navigate: (String) -> Unit) {
    val q = s.current ?: return
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Question ${s.index + 1} of ${s.items.size}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    DifficultyPill(q.difficulty)
                }
                Spacer(Modifier.height(8.dp))
                ProgressLine((s.index + 1f) / s.items.size)
                s.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp)) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill(q.type.label)
                Pill(s.topicTitles[q.topicId] ?: q.topicId)
            }
        }
        item { InlineText(q.prompt, style = MaterialTheme.typography.titleMedium) }
        q.code?.let { code -> item { CodeBlock(code) } }
        item { AnswerInput(q, s, vm) }
        item {
            AnimatedVisibility(s.feedback != null) { s.feedback?.let { Feedback(q, s, navigate) } }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (s.feedback == null) {
                    if (q.type == QuizType.CODE_OUTPUT && !s.revealed) {
                        PrimaryAction("Check my answer", vm::submit, enabled = s.canSubmit)
                        SecondaryAction("I worked it out in my head — reveal", vm::reveal)
                    } else if (q.type == QuizType.CODE_OUTPUT && s.revealed) {
                        Text("Handbook output:", style = MaterialTheme.typography.labelLarge)
                        CodeBlock(q.answerText.orEmpty(), isOutput = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { vm.selfMark(false) }, modifier = Modifier.weight(1f)) { Text("I got it wrong") }
                            OutlinedButton(onClick = { vm.selfMark(true) }, modifier = Modifier.weight(1f)) { Text("I got it right") }
                        }
                    } else {
                        PrimaryAction("Submit", vm::submit, enabled = s.canSubmit)
                    }
                    TextButton(onClick = vm::skip, modifier = Modifier.fillMaxWidth()) { Text("Skip") }
                } else {
                    PrimaryAction(if (s.index + 1 >= s.items.size) "See results" else "Next question", vm::next)
                }
            }
        }
    }
}

@Composable
private fun OptionRow(text: String, selected: Boolean, multi: Boolean, state: OptionState, onClick: () -> Unit) {
    val ext = AppColors.extended
    val border = when (state) {
        OptionState.CORRECT -> ext.success
        OptionState.WRONG -> ext.danger
        OptionState.NONE -> if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    }
    val bg = when (state) {
        OptionState.CORRECT -> ext.successContainer
        OptionState.WRONG -> ext.dangerContainer
        OptionState.NONE -> if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    }
    val base = Modifier.fillMaxWidth().heightIn(min = 56.dp)
    val interaction = if (multi) base.toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
    else base.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    Surface(modifier = interaction, shape = RoundedCornerShape(14.dp), color = bg, border = BorderStroke(if (selected || state != OptionState.NONE) 2.dp else 1.dp, border)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (multi) Checkbox(checked = selected, onCheckedChange = null) else RadioButton(selected = selected, onClick = null)
            Spacer(Modifier.width(8.dp))
            InlineText(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            when (state) {
                OptionState.CORRECT -> Text("✅")
                OptionState.WRONG -> Text("❌")
                OptionState.NONE -> {}
            }
        }
    }
}

private enum class OptionState { NONE, CORRECT, WRONG }

@Composable
private fun AnswerInput(q: QuizItem, s: QuizUiState, vm: QuizViewModel) {
    when (q.type) {
        QuizType.MCQ, QuizType.TRUE_FALSE, QuizType.SCENARIO, QuizType.DEBUGGING, QuizType.MULTI -> {
            val multi = q.type == QuizType.MULTI
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (multi) Text("Select all that apply", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                q.options.forEachIndexed { i, opt ->
                    // answers are only revealed after submitting
                    val st = when {
                        s.feedback == null -> OptionState.NONE
                        i in q.correct -> OptionState.CORRECT
                        i in s.selected -> OptionState.WRONG
                        else -> OptionState.NONE
                    }
                    OptionRow(opt, i in s.selected, multi, st) { vm.toggleOption(i) }
                }
            }
        }
        QuizType.FILL_BLANK -> OutlinedTextField(
            value = s.text, onValueChange = vm::setText, modifier = Modifier.fillMaxWidth(),
            label = { Text("Your answer") }, singleLine = true, enabled = s.feedback == null,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
        )
        QuizType.CODE_OUTPUT -> OutlinedTextField(
            value = s.text, onValueChange = vm::setText, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            label = { Text("Type the exact output") }, enabled = s.feedback == null && !s.revealed,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
        )
        QuizType.MATCH -> MatchInput(q, s, vm)
    }
}

@Composable
private fun MatchInput(q: QuizItem, s: QuizUiState, vm: QuizViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        q.pairs.forEachIndexed { left, pair ->
            var open by remember(q.id, left) { mutableStateOf(false) }
            val chosen = s.matches[left]
            val ok = s.feedback != null && chosen == left
            AppCard {
                Text(pair.left, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Box {
                    OutlinedButton(onClick = { open = true }, enabled = s.feedback == null, modifier = Modifier.fillMaxWidth()) {
                        Text(chosen?.let { q.pairs[it].right } ?: "Choose the meaning…", maxLines = 3)
                    }
                    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        s.rightOrder.forEach { r ->
                            DropdownMenuItem(text = { Text(q.pairs[r].right, maxLines = 3) }, onClick = { vm.setMatch(left, r); open = false })
                        }
                    }
                }
                if (s.feedback != null) {
                    Text(
                        if (ok) "✅ Correct" else "❌ Correct match: ${pair.right}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ok) AppColors.extended.success else AppColors.extended.danger,
                    )
                }
            }
        }
    }
}

@Composable
private fun Feedback(q: QuizItem, s: QuizUiState, navigate: (String) -> Unit) {
    val r = s.feedback ?: return
    val ext = AppColors.extended
    AppCard(container = if (r.correct) ext.successContainer else ext.dangerContainer) {
        Text(if (r.correct) "✅ Correct!" else "❌ Incorrect", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        if (!r.correct) {
            Text("Correct answer", style = MaterialTheme.typography.labelLarge)
            val correct = when (q.type) {
                QuizType.FILL_BLANK, QuizType.CODE_OUTPUT -> q.answerText.orEmpty()
                QuizType.MATCH -> q.pairs.joinToString("\n") { "${it.left} → ${it.right}" }
                else -> q.correct.mapNotNull { q.options.getOrNull(it) }.joinToString("\n")
            }
            if (q.type == QuizType.CODE_OUTPUT) CodeBlock(correct, isOutput = true) else InlineText(correct)
            Spacer(Modifier.height(6.dp))
            Text("Why your answer was wrong", style = MaterialTheme.typography.labelLarge)
            Text(r.feedback, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
        }
        Text(if (r.correct) "Why it's correct" else "Explanation", style = MaterialTheme.typography.labelLarge)
        MarkdownText(q.explanation.ifBlank { "See the source section in the handbook." })
        if (!r.correct) {
            Spacer(Modifier.height(6.dp))
            Text("Concept to revise", style = MaterialTheme.typography.labelLarge)
            TextButton(onClick = { navigate(Routes.topic(q.topicId)) }) { Text(s.topicTitles[q.topicId] ?: "Open topic") }
        }
        OriginPill(q.origin)
        SourceButton(q.source, quote = q.evidenceQuote)
    }
}

@Composable
private fun QuizSummary(s: QuizUiState, modifier: Modifier, navigate: (String) -> Unit, onBack: () -> Unit) {
    val graded = s.answered.count { !it.skipped }
    val strong = s.byTopic.filter { it.accuracy >= 0.75 && it.total >= 1 }
    val weak = s.byTopic.filter { it.accuracy < 0.6 }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                ProgressRing(s.accuracy.toFloat(), size = 140.dp, stroke = 14.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${s.correctCount}/${s.items.size}", style = MaterialTheme.typography.headlineMedium)
                        Text("Score", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Accuracy: ${(s.accuracy * 100).roundToInt()}%", style = MaterialTheme.typography.titleMedium)
                val skipped = s.answered.count { it.skipped }
                Text("$graded answered · $skipped skipped", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (s.newAchievements.isNotEmpty()) item {
            AppCard(container = MaterialTheme.colorScheme.tertiaryContainer) {
                Text("Achievement unlocked!", style = MaterialTheme.typography.titleSmall)
                s.newAchievements.forEach { Text(it) }
            }
        }
        if (strong.isNotEmpty()) item {
            SectionTitle("Strong areas")
            strong.forEach { LabeledProgress(it.title, it.accuracy.toFloat(), color = AppColors.extended.success, trailing = "${it.correct}/${it.total}") }
        }
        if (weak.isNotEmpty()) item {
            SectionTitle("Weak areas")
            weak.forEach { t ->
                LabeledProgress(t.title, t.accuracy.toFloat(), color = AppColors.extended.danger, trailing = "${t.correct}/${t.total}")
                TextButton(onClick = { navigate(Routes.topic(t.topicId)) }) { Text("Revise ${t.title}") }
            }
        }
        item { SectionTitle("Review") }
        items(s.answered.size) { i ->
            val a = s.answered[i]
            val mark = when { a.skipped -> "⏭"; a.result?.correct == true -> "✅"; else -> "❌" }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(mark, modifier = Modifier.width(28.dp))
                InlineText(a.item.prompt.lineSequence().first().take(140), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (weak.isNotEmpty()) PrimaryAction("Practise weak topics", { navigate(Routes.quizRun("WEAK")) })
                SecondaryAction("Done", onBack)
            }
        }
    }
}

// ================================================================== Quiz hub (bottom tab)
@Composable
fun QuizHubScreen(navigate: (String) -> Unit, vm: QuizHubViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { ScreenTopBar("Quiz") }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                AppCard(onClick = { navigate(Routes.quizRun("DAILY")) }, container = MaterialTheme.colorScheme.primaryContainer) {
                    Text("🎯 Daily Interview Challenge", style = MaterialTheme.typography.titleMedium)
                    Text("10 questions · 10 minutes · mixed topics", style = MaterialTheme.typography.bodyMedium)
                    s.dailyResult?.let { Text("Today: $it", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp)) }
                }
            }
            item {
                AppCard(onClick = { navigate(Routes.quizRun("ADAPTIVE")) }) {
                    Text("🧠 Adaptive quiz", style = MaterialTheme.typography.titleMedium)
                    Text("Adjusts difficulty per topic and brings back questions you missed.", style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                AppCard(onClick = { navigate(Routes.quizRun("WEAK")) }) {
                    Text("🔴 Weak-topic quiz", style = MaterialTheme.typography.titleMedium)
                    Text(if (s.weakCount == 0) "No weak topics detected yet." else "${s.weakCount} weak topics to work on.", style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppCard(Modifier.weight(1f), onClick = { navigate(Routes.flashcards(null)) }) {
                        Text("🃏 Flashcards", style = MaterialTheme.typography.titleSmall)
                        Text("${s.dueCards} due", style = MaterialTheme.typography.bodySmall)
                    }
                    AppCard(Modifier.weight(1f), onClick = { navigate(Routes.REVISION) }) {
                        Text("⚡ 15-min revision", style = MaterialTheme.typography.titleSmall)
                        Text("Cards · Qs · quiz · scenario", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item { SectionTitle("Quiz by topic") }
            items(s.topics, key = { it.first }) { (id, title, n) ->
                com.interviewprep.core.designsystem.ListRow(title = title, subtitle = "$n questions", leading = "📝", onClick = { navigate(Routes.quizRun("TOPIC", id)) })
            }
        }
    }
}
