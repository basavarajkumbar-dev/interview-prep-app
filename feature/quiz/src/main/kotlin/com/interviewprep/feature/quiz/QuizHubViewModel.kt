package com.interviewprep.feature.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interviewprep.core.data.db.UserDatabase
import com.interviewprep.core.data.repo.ContentRepository
import com.interviewprep.core.data.repo.StatsRepository
import com.interviewprep.core.data.repo.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuizHubState(
    val topics: List<Triple<String, String, Int>> = emptyList(),
    val dueCards: Int = 0,
    val weakCount: Int = 0,
    val dailyResult: String? = null,
)

@HiltViewModel
class QuizHubViewModel @Inject constructor(
    content: ContentRepository,
    stats: StatsRepository,
    db: UserDatabase,
    time: TimeProvider,
) : ViewModel() {
    private val counts = MutableStateFlow<Map<String, Int>>(emptyMap())

    init {
        viewModelScope.launch { counts.value = content.countsByTopic().mapValues { it.value[3] } }
    }

    val state: StateFlow<QuizHubState> = combine(stats.snapshot(), counts, db.userDao().dailyChallengeResult(time.today())) { s, c, daily ->
        QuizHubState(
            topics = s.topics.mapNotNull { t -> c[t.id]?.takeIf { it > 0 }?.let { Triple(t.id, t.title, it) } },
            dueCards = s.dueCards,
            weakCount = s.weak.size,
            dailyResult = daily?.let { "${it.correct}/${it.total} correct" },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuizHubState())
}
