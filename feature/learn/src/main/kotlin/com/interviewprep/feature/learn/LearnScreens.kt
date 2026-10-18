package com.interviewprep.feature.learn

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.interviewprep.core.designsystem.AppCard
import com.interviewprep.core.designsystem.AppColors
import com.interviewprep.core.designsystem.BookmarkToggle
import com.interviewprep.core.designsystem.DifficultyPill
import com.interviewprep.core.designsystem.EmptyState
import com.interviewprep.core.designsystem.ErrorState
import com.interviewprep.core.designsystem.InlineText
import com.interviewprep.core.designsystem.LoadingState
import com.interviewprep.core.designsystem.MarkdownText
import com.interviewprep.core.designsystem.MdBlockView
import com.interviewprep.core.designsystem.Pill
import com.interviewprep.core.designsystem.PrimaryAction
import com.interviewprep.core.designsystem.ProgressLine
import com.interviewprep.core.designsystem.ResumeTagPill
import com.interviewprep.core.designsystem.Routes
import com.interviewprep.core.designsystem.ScreenTopBar
import com.interviewprep.core.designsystem.SectionTitle
import com.interviewprep.core.designsystem.Segmented
import com.interviewprep.core.designsystem.SourceButton
import com.interviewprep.core.domain.MasteryBand
import com.interviewprep.core.model.ExplanationMode
import com.interviewprep.core.model.InterviewQuestion
import com.interviewprep.core.model.LessonKind
import kotlin.math.roundToInt

private fun bandColorEmoji(b: MasteryBand) = b.emoji

// ================================================================== Learn tree
@Composable
fun LearnScreen(navigate: (String) -> Unit, vm: LearnViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        ScreenTopBar("Learn", actions = {
            IconButton(onClick = { navigate(Routes.SEARCH) }) { Icon(Icons.Outlined.Search, contentDescription = "Search") }
        })
    }) { pad ->
        if (s.loading) { LoadingState(modifier = Modifier.padding(pad)); return@Scaffold }
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = s.roleFilter == null, onClick = { vm.setRole(null) }, label = { Text("All roles") }) }
                    items(s.roles) { (id, title) ->
                        FilterChip(selected = s.roleFilter == id, onClick = { vm.setRole(if (s.roleFilter == id) null else id) }, label = { Text(title) })
                    }
                }
            }
            if (s.tracks.isEmpty()) item { EmptyState("📭", "Nothing here", "No tracks match this role filter.") }
            items(s.tracks, key = { it.track.id }) { node ->
                val open = node.track.id in s.expanded
                AppCard(onClick = { vm.toggle(node.track.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(node.track.emoji, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(node.track.title, style = MaterialTheme.typography.titleMedium)
                            Text("${node.topics.size} topics · ${(node.completion * 100).roundToInt()}% complete",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = if (open) "Collapse" else "Expand")
                    }
                    Spacer(Modifier.height(10.dp))
                    ProgressLine(node.completion.toFloat())
                    AnimatedVisibility(open) {
                        Column(Modifier.padding(top = 8.dp)) {
                            node.topics.forEachIndexed { i, t ->
                                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                TopicRow(t) { navigate(Routes.topic(t.topic.id)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicRow(t: TopicNode, onClick: () -> Unit) {
    com.interviewprep.core.designsystem.ListRow(
        title = t.topic.title,
        subtitle = if (t.total > 0) "${t.done}/${t.total} lessons" else "Interview practice",
        leading = bandColorEmoji(t.band),
        onClick = onClick,
    )
}

// ================================================================== Topic
@Composable
fun TopicScreen(navigate: (String) -> Unit, onBack: () -> Unit, vm: TopicViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        ScreenTopBar(s.topic?.title ?: "Topic", onBack, actions = {
            BookmarkToggle(s.bookmarked) { vm.toggleBookmark() }
        })
    }) { pad ->
        when {
            s.notFound -> ErrorState("This topic isn't in the library.", modifier = Modifier.padding(pad))
            s.loading || s.topic == null -> LoadingState(modifier = Modifier.padding(pad))
            else -> TopicContent(s, Modifier.padding(pad), navigate, vm::setMode)
        }
    }
}

@Composable
private fun TopicContent(s: TopicUiState, modifier: Modifier, navigate: (String) -> Unit, setMode: (ExplanationMode) -> Unit) {
    val t = s.topic!!
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column {
                Text(s.trackTitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(t.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Progress: ${(s.progress * 100).roundToInt()}%", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Pill("${s.band.emoji} ${s.band.label}")
                }
                Spacer(Modifier.height(6.dp))
                ProgressLine(s.progress)
                t.resumeTag?.let { tag ->
                    Spacer(Modifier.height(10.dp))
                    AppCard(container = MaterialTheme.colorScheme.surfaceContainerLow) {
                        ResumeTagPill(tag)
                        Spacer(Modifier.height(4.dp))
                        Text(tag.advice, style = MaterialTheme.typography.bodySmall)
                        t.resumeTagSource?.let { SourceButton(it) }
                    }
                }
            }
        }
        item {
            Segmented(
                options = listOf(ExplanationMode.SIMPLE, ExplanationMode.INTERVIEW),
                selected = s.mode,
                label = { if (it == ExplanationMode.SIMPLE) "Simple" else "Interview" },
                onSelect = setMode,
            )
        }
        if (s.mode == ExplanationMode.SIMPLE) {
            item { SimpleExplanation(s) }
        } else {
            item {
                Text(
                    "Model answers from your handbooks. Read the question, answer aloud, then compare.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (s.questions.isEmpty()) item { Text("No interview questions for this topic in the handbooks.", style = MaterialTheme.typography.bodyMedium) }
            items(s.questions, key = { it.id }) { q -> InterviewCard(q) { navigate(Routes.question(q.id)) } }
            if (t.keyPoints.isNotEmpty()) item { BulletCard("Key interview points", t.keyPoints) }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                s.nextLesson?.let { next ->
                    PrimaryAction(if (s.done == 0) "Start learning" else "Continue: ${next.title}", { navigate(Routes.lesson(next.id)) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { navigate(Routes.quizRun("TOPIC", t.id)) }, modifier = Modifier.weight(1f)) { Text("Take quiz") }
                    OutlinedButton(onClick = { navigate(Routes.questions(t.id)) }, modifier = Modifier.weight(1f)) { Text("Questions") }
                    OutlinedButton(onClick = { navigate(Routes.flashcards(t.id)) }, modifier = Modifier.weight(1f)) { Text("Cards") }
                }
            }
        }
        if (s.lessons.isNotEmpty()) item { SectionTitle("Lessons (${s.done}/${s.lessons.size})") }
        itemsIndexed(s.lessons, key = { _, l -> l.lesson.id }) { _, item ->
            val l = item.lesson
            com.interviewprep.core.designsystem.ListRow(
                title = l.title,
                subtitle = buildString {
                    append(kindLabel(l.kind)); append(" · "); append("${l.readMinutes} min")
                    if (l.hasCode) append(" · code")
                    append(" · ${com.interviewprep.core.designsystem.Handbooks.short(l.source.book)} p.${l.source.page}")
                },
                onClick = { navigate(Routes.lesson(l.id)) },
                trailing = {
                    if (item.completed) Icon(Icons.Filled.CheckCircle, contentDescription = "Completed", tint = AppColors.extended.success)
                    else Icon(Icons.Outlined.Circle, contentDescription = "Not completed", tint = MaterialTheme.colorScheme.outlineVariant)
                },
            )
        }
        if (s.glossary.isNotEmpty()) {
            item { SectionTitle("Glossary") }
            items(s.glossary, key = { "g-" + it.term }) { g ->
                Column(Modifier.padding(vertical = 4.dp)) {
                    Text(g.term, style = MaterialTheme.typography.titleSmall)
                    Text(g.definition, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            SectionTitle("Sources")
            t.sources.forEach { SourceButton(it) }
        }
    }
}

private fun kindLabel(k: LessonKind) = when (k) {
    LessonKind.CONCEPT -> "Concept"
    LessonKind.PRACTICE -> "Practice"
    LessonKind.SOLUTIONS -> "Solutions"
    LessonKind.SUMMARY -> "Summary"
    LessonKind.MISTAKES -> "Common mistakes"
    LessonKind.BEST_PRACTICES -> "Best practices"
}

@Composable
private fun SimpleExplanation(s: TopicUiState) {
    val t = s.topic!!
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (t.intro.isNotBlank()) {
            AppCard(container = MaterialTheme.colorScheme.primaryContainer) {
                Text("In plain words", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(6.dp))
                InlineText(t.intro.replace(Regex("\\s*Label: .*$"), ""), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Text("Not covered in the uploaded handbook.", style = MaterialTheme.typography.bodyMedium)
        }
        if (t.objectives.isNotEmpty()) BulletCard("What you'll be able to do", t.objectives)
        if (t.summary.isNotEmpty()) BulletCard("Key points", t.summary)
        if (t.commonMistakes.isNotEmpty()) BulletCard("Common mistakes", t.commonMistakes.take(6))
    }
}

@Composable
private fun BulletCard(title: String, items: List<String>) {
    AppCard {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        items.forEach {
            Row(Modifier.padding(vertical = 3.dp)) {
                Text("•", color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(18.dp))
                InlineText(it, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun InterviewCard(q: InterviewQuestion, open: () -> Unit) {
    var revealed by rememberSaveable(q.id) { mutableStateOf(false) }
    AppCard {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DifficultyPill(q.difficulty)
            q.resumeTag?.let { ResumeTagPill(it) }
        }
        Spacer(Modifier.height(8.dp))
        Text(q.question, style = MaterialTheme.typography.titleSmall)
        if (revealed) {
            Spacer(Modifier.height(8.dp))
            MarkdownText(q.answer)
            if (q.followUps.isNotEmpty()) {
                Text("Likely follow-up: ${q.followUps.first()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { revealed = !revealed }) { Text(if (revealed) "Hide answer" else "Show answer") }
            TextButton(onClick = open) { Text("Practise") }
            Spacer(Modifier.weight(1f))
            SourceButton(q.source)
        }
    }
}

// ================================================================== Lesson
@Composable
fun LessonScreen(navigate: (String) -> Unit, replace: (String) -> Unit, onBack: () -> Unit, vm: LessonViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            ScreenTopBar(s.topicTitle.ifBlank { "Lesson" }, onBack, actions = { BookmarkToggle(s.bookmarked) { vm.toggleBookmark() } })
        },
        bottomBar = {
            if (s.lesson != null) Surface(tonalElevation = 3.dp) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { vm.setCompleted(!s.completed) },
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) { Text(if (s.completed) "✓ Learned" else "Mark as learned") }
                    val next = s.nextId
                    if (next != null) {
                        androidx.compose.material3.Button(
                            onClick = { if (!s.completed) vm.setCompleted(true); replace(Routes.lesson(next)) },
                            modifier = Modifier.weight(1f).height(52.dp),
                        ) { Text("Next lesson") }
                    } else {
                        androidx.compose.material3.Button(
                            onClick = { if (!s.completed) vm.setCompleted(true); navigate(Routes.quizRun("TOPIC", s.lesson!!.summary.topicId)) },
                            modifier = Modifier.weight(1f).height(52.dp),
                        ) { Text("Take quiz") }
                    }
                }
            }
        },
    ) { pad ->
        when {
            s.error != null -> ErrorState(s.error!!, onRetry = { vm.load() }, modifier = Modifier.padding(pad))
            s.loading || s.lesson == null -> LoadingState(modifier = Modifier.padding(pad))
            else -> {
                val l = s.lesson!!
                LazyColumn(
                    Modifier.fillMaxSize().padding(pad),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    item {
                        Text("Lesson ${s.position} of ${s.count} · ${l.summary.readMinutes} min read", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(l.summary.title, style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        SourceButton(l.summary.source)
                        Spacer(Modifier.height(4.dp))
                    }
                    items(s.blocks.size) { i -> MdBlockView(s.blocks[i]) }
                    item {
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            s.prevId?.let { p -> TextButton(onClick = { replace(Routes.lesson(p)) }) { Text("← Previous") } }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { navigate(Routes.questions(l.summary.topicId)) }) { Text("Practice questions") }
                        }
                    }
                }
            }
        }
    }
}
