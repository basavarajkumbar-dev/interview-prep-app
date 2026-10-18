package com.interviewprep.feature.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.interviewprep.core.data.db.UserDatabase
import com.interviewprep.core.data.repo.ContentRepository
import com.interviewprep.core.data.repo.ProgressRepository
import com.interviewprep.core.data.repo.ProgressSnapshot
import com.interviewprep.core.data.repo.StatsRepository
import com.interviewprep.core.data.repo.TimeProvider
import com.interviewprep.core.designsystem.AppCard
import com.interviewprep.core.designsystem.AppColors
import com.interviewprep.core.designsystem.LoadingState
import com.interviewprep.core.designsystem.ProgressLine
import com.interviewprep.core.designsystem.Routes
import com.interviewprep.core.designsystem.ScreenTopBar
import com.interviewprep.core.designsystem.SectionTitle
import com.interviewprep.core.designsystem.StatTile
import com.interviewprep.core.domain.Achievements
import com.interviewprep.core.domain.DailyPlan
import com.interviewprep.feature.progress.PlanBuilder
import com.interviewprep.feature.progress.ReadinessCard
import com.interviewprep.feature.progress.routeForBlock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

data class ContinueInfo(val lessonId: String, val lessonTitle: String, val topicId: String, val topicTitle: String, val topicProgress: Float)

data class HomeUiState(
    val snapshot: ProgressSnapshot? = null,
    val plan: DailyPlan? = null,
    val continueInfo: ContinueInfo? = null,
    val todayQuizAnswered: Int = 0,
    val todayQuizCorrect: Int = 0,
    val todayLessons: Int = 0,
    val todayQuestions: Int = 0,
    val greeting: String = "Hello",
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val stats: StatsRepository,
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val planBuilder: PlanBuilder,
    db: UserDatabase,
    private val time: TimeProvider,
) : ViewModel() {
    private val dao = db.userDao()
    private val today = time.today()

    private val todayCounts = combine(
        dao.quizAnsweredOn(today), dao.quizCorrectOn(today), dao.lessonsCompletedSince(time.startOfDay(today)), dao.practiceOn(today),
    ) { a, c, l, p -> intArrayOf(a, c, l, p) }

    private val continueFlow = progress.lastOpenedLesson().map { lp ->
        if (lp == null) null else {
            val lesson = content.lesson(lp.lessonId)
            val topic = content.topic(lp.topicId)
            if (lesson == null || topic == null) null else ContinueInfo(lesson.summary.id, lesson.summary.title, topic.id, topic.title, 0f)
        }
    }

    val state: StateFlow<HomeUiState> = combine(stats.snapshot(), todayCounts, continueFlow) { s, t, cont ->
        val greeting = when (time.hourOfDay()) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
        val c = cont?.let { ci ->
            val p = s.performance[ci.topicId]
            ci.copy(topicProgress = if (p == null || p.lessonsTotal == 0) 0f else p.lessonsDone.toFloat() / p.lessonsTotal)
        }
        HomeUiState(s, planBuilder.build(s), c, t[0], t[1], t[2], t[3], greeting)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Side effects that depend on fresh progress: daily-goal XP and achievements. */
    fun onSnapshot(s: ProgressSnapshot) = viewModelScope.launch {
        if (s.todayMinutes >= s.settings.dailyGoalMinutes) progress.awardDailyGoalIfNeeded()
        progress.unlockAchievements(Achievements.earned(stats.achievementStats(s)))
    }
}

@Composable
fun HomeScreen(navigate: (String) -> Unit, vm: HomeViewModel = hiltViewModel()) {
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.snapshot
    LaunchedEffect(s?.todayMinutes, s?.quizAnswered, s?.lessonsDone, s?.streak) { s?.let { vm.onSnapshot(it) } }
    Scaffold(topBar = {
        ScreenTopBar("Interview Prep", actions = {
            IconButton(onClick = { navigate(Routes.SEARCH) }) { Icon(Icons.Outlined.Search, contentDescription = "Search") }
            IconButton(onClick = { navigate(Routes.SETTINGS) }) { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }
        })
    }) { pad ->
        if (s == null) { LoadingState(modifier = Modifier.padding(pad)); return@Scaffold }
        val goal = s.settings.dailyGoalMinutes
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Column {
                    Text("${st.greeting}${s.settings.name.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""}", style = MaterialTheme.typography.headlineMedium)
                    Text("Level ${s.level} · ${s.levelTitle} · ${s.totalXp} XP", style = MaterialTheme.typography.bodyMedium, color = AppColors.extended.xp)
                }
            }
            // ---- today's goal + streak
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Today's goal", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${s.todayMinutes} min / $goal min", style = MaterialTheme.typography.titleLarge)
                        }
                        Text("🔥 ${s.streak.current} day${if (s.streak.current == 1) "" else "s"}", style = MaterialTheme.typography.titleMedium, color = AppColors.extended.streak)
                    }
                    Spacer(Modifier.height(10.dp))
                    ProgressLine((s.todayMinutes.toFloat() / goal).coerceAtMost(1f), color = if (s.todayMinutes >= goal) AppColors.extended.success else MaterialTheme.colorScheme.primary)
                    if (!s.streak.studiedToday && s.streak.current > 0) {
                        Text("Study today to keep your streak.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("${st.todayLessons}", "Lessons today", Modifier.weight(1f), "📖")
                    StatTile(if (st.todayQuizAnswered == 0) "—" else "${(st.todayQuizCorrect * 100f / st.todayQuizAnswered).roundToInt()}%", "Quiz score today", Modifier.weight(1f), "🎯")
                    StatTile("${st.todayQuizAnswered + st.todayQuestions}", "Answered today", Modifier.weight(1f), "💬")
                }
            }
            // ---- continue learning
            item {
                val c = st.continueInfo
                AppCard(onClick = { navigate(c?.let { Routes.lesson(it.lessonId) } ?: Routes.LEARN) }, container = MaterialTheme.colorScheme.primaryContainer) {
                    Text("Continue learning", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    if (c != null) {
                        Text(c.topicTitle, style = MaterialTheme.typography.titleMedium)
                        Text(c.lessonTitle, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Progress: ${(c.topicProgress * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall)
                        ProgressLine(c.topicProgress)
                    } else {
                        Text("Start with your first topic", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            // ---- quick actions
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickAction("📖", "Learn", Modifier.weight(1f)) { navigate(st.continueInfo?.let { Routes.lesson(it.lessonId) } ?: Routes.LEARN) }
                    QuickAction("🧠", "Quiz", Modifier.weight(1f)) { navigate(Routes.quizRun("ADAPTIVE")) }
                    QuickAction("🃏", "Cards", Modifier.weight(1f)) { navigate(Routes.flashcards(null)) }
                    QuickAction("🎤", "Mock", Modifier.weight(1f)) { navigate(Routes.MOCK_SETUP) }
                }
            }
            item { ReadinessCard(s) { navigate(Routes.PROGRESS) } }
            // ---- recommended + weak area
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val rec = st.plan?.blocks?.firstOrNull { it.topicId != null && it.kind == com.interviewprep.core.model.ActivityKind.LESSON }
                    AppCard(Modifier.weight(1f), onClick = { navigate(rec?.topicId?.let { Routes.topic(it) } ?: Routes.LEARN) }) {
                        Text("Recommended", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(rec?.title?.removePrefix("Learn: ") ?: "Browse topics", style = MaterialTheme.typography.titleSmall, maxLines = 3)
                    }
                    val weak = s.weak.firstOrNull()
                    AppCard(Modifier.weight(1f), onClick = { navigate(weak?.let { Routes.topic(it.topicId) } ?: Routes.WEAK) }) {
                        Text("Weak area", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(weak?.let { "${it.band.emoji} ${s.topicTitle(it.topicId)}" } ?: "None yet 💪", style = MaterialTheme.typography.titleSmall, maxLines = 3)
                    }
                }
            }
            item {
                AppCard(onClick = { navigate(Routes.quizRun("DAILY")) }) {
                    Text("🎯 Daily Interview Challenge", style = MaterialTheme.typography.titleSmall)
                    Text("10 questions · 10 minutes", style = MaterialTheme.typography.bodySmall)
                }
            }
            // ---- what should I study today?
            st.plan?.let { plan ->
                item { SectionTitle("What should I study today?", action = "Plan", onAction = { navigate(Routes.PLAN) }) }
                item {
                    AppCard {
                        plan.blocks.forEach { b ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${b.minutes} min", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(64.dp))
                                Text(b.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                androidx.compose.material3.TextButton(onClick = { navigate(routeForBlock(b.kind, b.topicId)) }) { Text("Go") }
                            }
                        }
                    }
                }
            }
            item {
                AppCard(onClick = { navigate(Routes.topic("likely-questions")) }) {
                    Text("What questions am I likely to get?", style = MaterialTheme.typography.titleSmall)
                    Text("Ranked by probability for your resume (BMC/SSD prep handbook)", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (s.dueCards > 0) item {
                AppCard(onClick = { navigate(Routes.flashcards(null)) }) {
                    Text("What should I revise next?", style = MaterialTheme.typography.titleSmall)
                    Text("${s.dueCards} flashcards are due for review", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun QuickAction(emoji: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier.height(64.dp), contentPadding = PaddingValues(4.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji)
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

fun NavGraphBuilder.homeGraph(nav: NavHostController) {
    composable(Routes.HOME) { HomeScreen(navigate = { nav.navigate(it) }) }
}
