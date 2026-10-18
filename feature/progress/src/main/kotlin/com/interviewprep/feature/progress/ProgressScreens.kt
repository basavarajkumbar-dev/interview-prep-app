package com.interviewprep.feature.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.interviewprep.core.data.repo.ProgressSnapshot
import com.interviewprep.core.designsystem.AppCard
import com.interviewprep.core.designsystem.AppColors
import com.interviewprep.core.designsystem.EmptyState
import com.interviewprep.core.designsystem.LabeledProgress
import com.interviewprep.core.designsystem.LoadingState
import com.interviewprep.core.designsystem.PrimaryAction
import com.interviewprep.core.designsystem.ProgressLine
import com.interviewprep.core.designsystem.ProgressRing
import com.interviewprep.core.designsystem.Routes
import com.interviewprep.core.designsystem.ScreenTopBar
import com.interviewprep.core.designsystem.SectionTitle
import com.interviewprep.core.designsystem.Segmented
import com.interviewprep.core.designsystem.StatTile
import com.interviewprep.core.designsystem.describe
import com.interviewprep.core.domain.MasteryBand
import com.interviewprep.core.domain.ReadinessBand
import com.interviewprep.core.domain.StudyPlanner
import com.interviewprep.core.domain.Xp
import com.interviewprep.core.model.ActivityKind
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ReadinessCard(s: ProgressSnapshot, onClick: (() -> Unit)? = null) {
    val r = s.readiness
    val color = when (r.band) {
        ReadinessBand.READY, ReadinessBand.GOOD -> AppColors.extended.success
        ReadinessBand.FAIR -> AppColors.extended.warning
        else -> AppColors.extended.danger
    }
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(r.score / 100f, size = 92.dp, stroke = 10.dp, color = color, modifier = Modifier.describe("Interview readiness ${r.score} out of 100")) {
                Text("${r.score}", style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("INTERVIEW READINESS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${r.score} / 100", style = MaterialTheme.typography.titleLarge)
                Text("${r.band.emoji} ${r.band.label}", style = MaterialTheme.typography.titleSmall, color = color)
            }
        }
    }
}

@Composable
fun ProgressScreen(navigate: (String) -> Unit, vm: ProgressViewModel = hiltViewModel()) {
    val snap by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        ScreenTopBar("Progress", actions = { IconButton(onClick = { navigate(Routes.SETTINGS) }) { Icon(Icons.Outlined.Settings, contentDescription = "Settings") } })
    }) { pad ->
        val s = snap
        if (s == null) { LoadingState(modifier = Modifier.padding(pad)); return@Scaffold }
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ReadinessCard(s) }
            item { ReadinessExplanation(s, navigate) }
            item {
                AppCard {
                    Text("Overall", style = MaterialTheme.typography.titleSmall)
                    LabeledProgress("Overall completion", s.overallCompletion.toFloat(), trailing = "${s.lessonsDone}/${s.lessonsTotal} lessons")
                    s.tracks.forEach { t -> LabeledProgress("${t.track.emoji} ${t.track.title}", t.completion.toFloat()) }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(s.quizAccuracy?.let { "${(it * 100).roundToInt()}%" } ?: "—", "Quiz accuracy", Modifier.weight(1f), "🎯")
                    StatTile("${s.streak.current}", "Day streak (best ${s.streak.longest})", Modifier.weight(1f), "🔥")
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("${s.quizAnswered}", "Questions answered", Modifier.weight(1f), "✅")
                    StatTile("${s.quizSkipped + s.questionsSkipped}", "Skipped", Modifier.weight(1f), "⏭")
                    StatTile("${s.bookmarks}", "Bookmarks", Modifier.weight(1f), "⭐")
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(s.interviewAvg?.let { "$it%" } ?: "—", "Mock interview avg (${s.interviewsDone})", Modifier.weight(1f), "🎤")
                    StatTile("${s.codingSolved}/${s.codingAttempted}", "Coding solved/tried", Modifier.weight(1f), "💻")
                    StatTile(s.scenarioAvg?.let { "$it%" } ?: "—", "Scenario avg (${s.scenariosDone})", Modifier.weight(1f), "🔍")
                }
            }
            item { StudyTimeChart(s) }
            item {
                AppCard(onClick = { navigate(Routes.ACHIEVEMENTS) }) {
                    Text("Level ${s.level} · ${s.levelTitle}", style = MaterialTheme.typography.titleMedium)
                    Text("${s.totalXp} XP · ${s.achievements.count { it.unlockedAtMillis != null }}/${s.achievements.size} achievements", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    ProgressLine(Xp.progressInLevel(s.totalXp), color = AppColors.extended.xp)
                }
            }
            item { SectionTitle("Topics", action = "Weak topics", onAction = { navigate(Routes.WEAK) }) }
            items(s.topics.filter { (s.mastery[it.id]?.evidence ?: 0) > 0 }.sortedByDescending { s.mastery[it.id]?.score ?: 0.0 }, key = { it.id }) { t ->
                val m = s.mastery[t.id]!!
                LabeledProgress("${m.band.emoji} ${t.title}", m.score.toFloat(), color = bandColor(m.band))
            }
            if (s.mastery.values.none { it.evidence > 0 }) item {
                EmptyState("📈", "No topic data yet", "Take a quiz or review flashcards to see per-topic strength.")
            }
        }
    }
}

@Composable
fun bandColor(b: MasteryBand) = when (b) {
    MasteryBand.STRONG -> AppColors.extended.success
    MasteryBand.FAIR -> AppColors.extended.warning
    MasteryBand.WEAK -> AppColors.extended.danger
    MasteryBand.UNSEEN -> MaterialTheme.colorScheme.outline
}

@Composable
private fun ReadinessExplanation(s: ProgressSnapshot, navigate: (String) -> Unit) {
    AppCard {
        Text("What makes up your score", style = MaterialTheme.typography.titleSmall)
        s.readiness.components.forEach { c ->
            LabeledProgress("${c.label} (${(c.weight * 100).roundToInt()}%)", (c.value ?: 0.0).toFloat(), trailing = c.value?.let { "${(it * 100).roundToInt()}%" } ?: "not started")
        }
        val strong = s.tracks.filter { it.mastery >= 0.75 }.map { it.track.title }
        val weakTracks = s.tracks.filter { it.mastery in 0.01..0.6 }.map { it.track.title }
        val notStarted = s.readiness.components.filter { it.value == null }
        Spacer(Modifier.height(8.dp))
        if (strong.isNotEmpty()) Text("Strengths: ${strong.joinToString()}", style = MaterialTheme.typography.bodyMedium)
        if (weakTracks.isNotEmpty() || s.weak.isNotEmpty()) {
            Text("Needs improvement: " + (weakTracks + s.weak.take(3).map { s.topicTitle(it.topicId) }).distinct().joinToString(), style = MaterialTheme.typography.bodyMedium)
        }
        notStarted.firstOrNull()?.let { Text("Next step: ${it.hint}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun StudyTimeChart(s: ProgressSnapshot) {
    val max = (s.last14Days.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(s.settings.dailyGoalMinutes)
    AppCard {
        Text("Study time — last 14 days", style = MaterialTheme.typography.titleSmall)
        Text("${s.totalStudyMinutes} min in total · goal ${s.settings.dailyGoalMinutes} min/day", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
            s.last14Days.forEach { d ->
                val frac = if (max == 0) 0f else d.minutes.toFloat() / max
                val metGoal = d.minutes >= s.settings.dailyGoalMinutes
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height((100 * frac).coerceAtLeast(2f).dp)
                            .background(if (metGoal) AppColors.extended.success else MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                            .describe("${LocalDate.ofEpochDay(d.day)}: ${d.minutes} minutes"),
                    )
                    Text(LocalDate.ofEpochDay(d.day).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ================================================================== Weak topics
@Composable
fun WeakTopicsScreen(navigate: (String) -> Unit, onBack: () -> Unit, vm: ProgressViewModel = hiltViewModel()) {
    val snap by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { ScreenTopBar("Weak topics", onBack) }) { pad ->
        val s = snap
        if (s == null) { LoadingState(modifier = Modifier.padding(pad)); return@Scaffold }
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (s.weak.isEmpty()) {
                item { EmptyState("💪", "No weak topics detected", "Weak areas appear once you've answered a few quiz questions, rated flashcards or practised interview answers in a topic.", action = "Take an adaptive quiz", onAction = { navigate(Routes.quizRun("ADAPTIVE")) }) }
            } else {
                item { Text("Your weak areas", style = MaterialTheme.typography.titleMedium) }
                items(s.weak, key = { it.topicId }) { m ->
                    val title = s.topicTitle(m.topicId)
                    AppCard(onClick = { navigate(Routes.topic(m.topicId)) }) {
                        LabeledProgress("${m.band.emoji} $title", m.score.toFloat(), color = bandColor(m.band))
                        val p = s.performance[m.topicId]
                        Text(
                            buildString {
                                p?.let { append("Quiz ${it.quizCorrect}/${it.quizAttempts}") }
                                if ((p?.cardsReviewed ?: 0) > 0) append(" · cards known ${p!!.cardsKnown}/${p.cardsReviewed}")
                                if (p?.interviewAvg != null) append(" · answers ${(p.interviewAvg!! * 100).roundToInt()}%")
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row {
                            TextButton(onClick = { navigate(Routes.quizRun("TOPIC", m.topicId)) }) { Text("Quiz") }
                            TextButton(onClick = { navigate(Routes.flashcards(m.topicId)) }) { Text("Cards") }
                            TextButton(onClick = { navigate(Routes.questions(m.topicId)) }) { Text("Questions") }
                        }
                    }
                }
                item {
                    val w = s.weak.first()
                    AppCard(container = MaterialTheme.colorScheme.primaryContainer) {
                        Text("Recommended", style = MaterialTheme.typography.labelLarge)
                        Text("Spend 20 minutes revising ${s.topicTitle(w.topicId)}.", style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    PrimaryAction("Start weak-topic quiz", { navigate(Routes.quizRun("WEAK")) })
                }
            }
        }
    }
}

// ================================================================== Achievements
@Composable
fun AchievementsScreen(onBack: () -> Unit, vm: ProgressViewModel = hiltViewModel()) {
    val snap by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { ScreenTopBar("Achievements", onBack) }) { pad ->
        val s = snap
        if (s == null) { LoadingState(modifier = Modifier.padding(pad)); return@Scaffold }
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                AppCard {
                    Text("Level ${s.level}", style = MaterialTheme.typography.headlineSmall)
                    Text("“${s.levelTitle}” · ${s.totalXp} XP", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    ProgressLine(Xp.progressInLevel(s.totalXp), color = AppColors.extended.xp)
                    Text("${Xp.xpForLevel(s.level + 1) - s.totalXp} XP to level ${s.level + 1}", style = MaterialTheme.typography.bodySmall)
                }
            }
            items(s.achievements, key = { it.id }) { a ->
                val unlocked = a.unlockedAtMillis != null
                com.interviewprep.core.designsystem.ListRow(
                    title = a.title,
                    subtitle = a.description + if (unlocked) " · unlocked" else "",
                    leading = if (unlocked) a.emoji else "🔒",
                )
            }
        }
    }
}

// ================================================================== Study plan
@Composable
fun StudyPlanScreen(navigate: (String) -> Unit, onBack: () -> Unit, vm: PlanViewModel = hiltViewModel()) {
    val st by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { ScreenTopBar("Today's study plan", onBack) }) { pad ->
        val plan = st.plan
        val s = st.snapshot
        if (plan == null || s == null) { LoadingState(modifier = Modifier.padding(pad)); return@Scaffold }
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Daily study time", style = MaterialTheme.typography.titleSmall) }
            item { Segmented(StudyPlanner.OPTIONS, s.settings.dailyGoalMinutes, { "$it min" }, { vm.setMinutes(it) }) }
            item { Text("90 minutes is the recommended maximum. The plan adapts each day to your weak topics, due cards and quiz accuracy.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { Text("TODAY — ${plan.totalMinutes} MINUTES", style = MaterialTheme.typography.titleMedium) }
            items(plan.blocks) { b ->
                AppCard(onClick = { navigate(routeForBlock(b.kind, b.topicId)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${b.minutes} min", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(72.dp))
                        Column(Modifier.weight(1f)) {
                            Text(b.title, style = MaterialTheme.typography.titleSmall)
                            Text(b.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                Text("Today so far: ${s.todayMinutes} of ${s.settings.dailyGoalMinutes} minutes", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                ProgressLine((s.todayMinutes.toFloat() / s.settings.dailyGoalMinutes).coerceAtMost(1f))
            }
        }
    }
}

fun routeForBlock(kind: ActivityKind, topicId: String?): String = when (kind) {
    ActivityKind.FLASHCARDS -> Routes.flashcards(null)
    ActivityKind.LESSON -> topicId?.let { Routes.topic(it) } ?: Routes.LEARN
    ActivityKind.QUIZ -> Routes.quizRun("ADAPTIVE")
    ActivityKind.QUESTIONS -> Routes.questions()
    ActivityKind.REVISION -> topicId?.let { Routes.topic(it) } ?: Routes.WEAK
    ActivityKind.MOCK_INTERVIEW -> Routes.MOCK_SETUP
    ActivityKind.SCENARIO -> Routes.SCENARIOS
    ActivityKind.CODING -> Routes.CODING
}

fun NavGraphBuilder.progressGraph(nav: NavHostController) {
    composable(Routes.PROGRESS) { ProgressScreen(navigate = { nav.navigate(it) }) }
    composable(Routes.WEAK) { WeakTopicsScreen(navigate = { nav.navigate(it) }, onBack = { nav.popBackStack() }) }
    composable(Routes.ACHIEVEMENTS) { AchievementsScreen(onBack = { nav.popBackStack() }) }
    composable(Routes.PLAN) { StudyPlanScreen(navigate = { nav.navigate(it) }, onBack = { nav.popBackStack() }) }
}
