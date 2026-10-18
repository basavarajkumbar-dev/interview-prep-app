package com.interviewprep.core.data.repo

import com.interviewprep.core.data.di.ApplicationScope
import com.interviewprep.core.model.ActivityKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Screens call start() when shown and stop() when left; time is saved even after the ViewModel dies. */
@Singleton
class StudyTracker @Inject constructor(
    private val progress: ProgressRepository,
    private val time: TimeProvider,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun start(): Long = time.now()

    fun stop(kind: ActivityKind, startedAt: Long, topicId: String? = null) {
        scope.launch { runCatching { progress.recordStudyTime(kind, startedAt, topicId) } }
    }
}
