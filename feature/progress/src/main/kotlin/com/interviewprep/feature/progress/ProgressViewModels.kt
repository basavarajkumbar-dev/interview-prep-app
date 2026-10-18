package com.interviewprep.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interviewprep.core.data.db.UserDatabase
import com.interviewprep.core.data.repo.ProgressSnapshot
import com.interviewprep.core.data.repo.StatsRepository
import com.interviewprep.core.data.repo.TimeProvider
import com.interviewprep.core.data.settings.SettingsStore
import com.interviewprep.core.domain.DailyPlan
import com.interviewprep.core.domain.PlannerSignals
import com.interviewprep.core.domain.StudyPlanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProgressViewModel @Inject constructor(stats: StatsRepository) : ViewModel() {
    val state: StateFlow<ProgressSnapshot?> = stats.snapshot().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** Builds today's plan from live progress. Shared by Home (summary) and the Plan screen. */
class PlanBuilder @Inject constructor(private val db: UserDatabase, private val time: TimeProvider) {
    suspend fun build(s: ProgressSnapshot): DailyPlan {
        val weekAgo = time.now() - 7 * 86_400_000L
        val dao = db.userDao()
        val continueTopic = s.topics.firstOrNull { t ->
            val p = s.performance[t.id]
            p != null && p.lessonsDone in 1 until p.lessonsTotal
        } ?: s.topics.firstOrNull { t ->
            val p = s.performance[t.id]
            p != null && p.lessonsTotal > 0 && p.lessonsDone == 0 &&
                (s.settings.targetRoles.isEmpty() || t.roles.any { it in s.settings.targetRoles })
        }
        return StudyPlanner.plan(
            PlannerSignals(
                dailyMinutes = s.settings.dailyGoalMinutes,
                dueCards = s.dueCards,
                weakTopic = s.weak.firstOrNull()?.let { it.topicId to s.topicTitle(it.topicId) },
                continueTopic = continueTopic?.let { it.id to it.title },
                recentQuizAccuracy = s.quizAccuracy,
                interviewPracticeCount7d = dao.practiceCountSince(weekAgo),
                scenarioCount7d = dao.scenarioCountSince(weekAgo),
            ),
        )
    }
}

data class PlanUiState(val snapshot: ProgressSnapshot? = null, val plan: DailyPlan? = null)

@HiltViewModel
class PlanViewModel @Inject constructor(
    stats: StatsRepository,
    private val builder: PlanBuilder,
    private val settings: SettingsStore,
) : ViewModel() {
    val state: StateFlow<PlanUiState> = stats.snapshot().map { s -> PlanUiState(s, builder.build(s)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState())

    fun setMinutes(m: Int) = viewModelScope.launch { settings.setDailyGoal(m) }
}
