package com.interviewprep.feature.flashcards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.interviewprep.core.data.repo.ContentRepository
import com.interviewprep.core.data.repo.ProgressRepository
import com.interviewprep.core.data.repo.StatsRepository
import com.interviewprep.core.data.repo.StudyTracker
import com.interviewprep.core.data.repo.TimeProvider
import com.interviewprep.core.designsystem.AppColors
import com.interviewprep.core.designsystem.BookmarkToggle
import com.interviewprep.core.designsystem.EmptyState
import com.interviewprep.core.designsystem.ErrorState
import com.interviewprep.core.designsystem.InlineText
import com.interviewprep.core.designsystem.LoadingState
import com.interviewprep.core.designsystem.Pill
import com.interviewprep.core.designsystem.PrimaryAction
import com.interviewprep.core.designsystem.ProgressLine
import com.interviewprep.core.designsystem.Routes
import com.interviewprep.core.designsystem.ScreenTopBar
import com.interviewprep.core.designsystem.SecondaryAction
import com.interviewprep.core.designsystem.SourceButton
import com.interviewprep.core.domain.Mastery
import com.interviewprep.core.domain.SpacedRepetition
import com.interviewprep.core.model.ActivityKind
import com.interviewprep.core.model.BookmarkType
import com.interviewprep.core.model.CardRating
import com.interviewprep.core.model.CardSchedule
import com.interviewprep.core.model.Flashcard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CardsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val title: String = "Flashcards",
    val deck: List<Flashcard> = emptyList(),
    val index: Int = 0,
    val flipped: Boolean = false,
    val ratings: Map<String, CardRating> = emptyMap(),
    val requeued: Int = 0,
    val bookmarked: Boolean = false,
    val topicTitles: Map<String, String> = emptyMap(),
) {
    val current get() = deck.getOrNull(index)
    val done get() = index >= deck.size && deck.isNotEmpty()
}

@HiltViewModel
class FlashcardsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val stats: StatsRepository,
    private val time: TimeProvider,
    private val tracker: StudyTracker,
) : ViewModel() {
    private val topic = savedState.get<String>("topic")?.takeIf { it.isNotBlank() }
    private val startedAt = tracker.start()
    private val base = MutableStateFlow(CardsUiState())
    private var schedules: MutableMap<String, CardSchedule> = mutableMapOf()

    val state: StateFlow<CardsUiState> = base.flatMapLatest { b ->
        val c = b.current
        if (c == null) flowOf(b) else combine(flowOf(b), progress.isBookmarked(BookmarkType.FLASHCARD, c.id)) { s, bm -> s.copy(bookmarked = bm) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CardsUiState())

    init { load() }

    fun load() = viewModelScope.launch {
        try {
            val all = content.allFlashcards()
            val snap = stats.snapshot().first()
            schedules = progress.cardSchedules().toMutableMap()
            val titles = snap.topics.associate { it.id to it.title }
            val pool = if (topic != null) all.filter { it.topicId == topic } else {
                // all-deck: weak topics first so their cards come up more often
                val weak = Mastery.weakTopics(snap.mastery.values.toList()).map { it.topicId }.toSet()
                all.sortedByDescending { it.topicId in weak }
            }
            val ids = SpacedRepetition.orderForSession(pool.map { it.id }, schedules, time.now(), limit = 20, newCardLimit = if (topic != null) 20 else 10)
            val byId = pool.associateBy { it.id }
            val deck = ids.mapNotNull { byId[it] }
            base.value = CardsUiState(
                loading = false,
                title = topic?.let { titles[it] } ?: "Flashcards",
                deck = deck,
                topicTitles = titles,
            )
        } catch (t: Throwable) {
            base.value = CardsUiState(loading = false, error = t.message ?: "Couldn't load flashcards")
        }
    }

    fun flip() = base.update { it.copy(flipped = !it.flipped) }

    fun rate(r: CardRating) {
        val s = base.value
        val c = s.current ?: return
        viewModelScope.launch { progress.rateCard(c.id, c.topicId, r, schedules[c.id]) }
        schedules[c.id] = SpacedRepetition.schedule(schedules[c.id] ?: CardSchedule(c.id), r, time.now())
        // "Don't know" cards come back at the end of this session too
        val deck = if (r == CardRating.DONT_KNOW && s.requeued < 10) s.deck + c else s.deck
        base.update { it.copy(deck = deck, index = it.index + 1, flipped = false, ratings = it.ratings + (c.id to r), requeued = it.requeued + if (r == CardRating.DONT_KNOW) 1 else 0) }
    }

    fun toggleBookmark() = viewModelScope.launch {
        val s = state.value
        val c = s.current ?: return@launch
        progress.setBookmark(BookmarkType.FLASHCARD, c.id, c.front, s.topicTitles[c.topicId].orEmpty(), !s.bookmarked)
    }

    fun restart() = load()

    override fun onCleared() = tracker.stop(ActivityKind.FLASHCARDS, startedAt, topic)
}

@Composable
fun FlashcardsScreen(navigate: (String) -> Unit, onBack: () -> Unit, vm: FlashcardsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        ScreenTopBar(s.title, onBack, actions = { if (s.current != null) BookmarkToggle(s.bookmarked) { vm.toggleBookmark() } })
    }) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when {
                s.loading -> LoadingState()
                s.error != null -> ErrorState(s.error!!, onRetry = { vm.load() })
                s.deck.isEmpty() -> EmptyState("🎉", "Nothing due", "No flashcards are due right now. Come back later or study a topic.", action = "Back", onAction = onBack)
                s.done -> CardSummary(s, vm::restart, onBack)
                else -> CardPage(s, vm, navigate)
            }
        }
    }
}

@Composable
private fun CardPage(s: CardsUiState, vm: FlashcardsViewModel, navigate: (String) -> Unit) {
    val c = s.current ?: return
    val rotation by animateFloatAsState(if (s.flipped) 180f else 0f, tween(350), label = "flip")
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Card ${s.index + 1} of ${s.deck.size}", style = MaterialTheme.typography.labelLarge)
        ProgressLine((s.index + 1f) / s.deck.size)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill(s.topicTitles[c.topicId] ?: c.topicId)
            Pill(c.origin.label)
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .graphicsLayer { rotationY = rotation; cameraDistance = 12f * density }
                .clickable(role = Role.Button, onClickLabel = if (s.flipped) "Show question" else "Reveal answer") { vm.flip() },
            shape = RoundedCornerShape(24.dp),
            color = if (rotation <= 90f) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp,
        ) {
            Box(
                Modifier.fillMaxSize().padding(24.dp).graphicsLayer { if (rotation > 90f) rotationY = 180f },
                contentAlignment = Alignment.Center,
            ) {
                Column(Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (rotation <= 90f) {
                        InlineText(c.front, style = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center))
                        Spacer(Modifier.height(16.dp))
                        Text("Tap to reveal answer", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        InlineText(c.back, style = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center))
                        if (c.keywords.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text("Keywords: ${c.keywords.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        SourceButton(c.source)
        if (s.flipped) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val ext = AppColors.extended
                Button(onClick = { vm.rate(CardRating.DONT_KNOW) }, modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ext.dangerContainer, contentColor = MaterialTheme.colorScheme.onSurface)) { Text("❌ Don't know") }
                Button(onClick = { vm.rate(CardRating.REVIEW) }, modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ext.warningContainer, contentColor = MaterialTheme.colorScheme.onSurface)) { Text("🤔 Review") }
                Button(onClick = { vm.rate(CardRating.KNOW) }, modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ext.successContainer, contentColor = MaterialTheme.colorScheme.onSurface)) { Text("👍 Know") }
            }
        } else {
            PrimaryAction("Reveal answer", vm::flip)
        }
    }
}

@Composable
private fun CardSummary(s: CardsUiState, restart: () -> Unit, onBack: () -> Unit) {
    val counts = s.ratings.values.groupingBy { it }.eachCount()
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🃏", style = MaterialTheme.typography.displaySmall)
        Text("Session complete", style = MaterialTheme.typography.headlineSmall)
        Text("👍 ${counts[CardRating.KNOW] ?: 0}   🤔 ${counts[CardRating.REVIEW] ?: 0}   ❌ ${counts[CardRating.DONT_KNOW] ?: 0}", style = MaterialTheme.typography.titleMedium)
        Text("Cards you didn't know will come back in about 10 minutes; known cards move further out each time.",
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        PrimaryAction("Another round", restart)
        SecondaryAction("Done", onBack)
    }
}

fun NavGraphBuilder.flashcardsGraph(nav: NavHostController) {
    composable(Routes.FLASHCARDS, arguments = listOf(navArgument("topic") { type = NavType.StringType; defaultValue = "" })) {
        FlashcardsScreen(navigate = { nav.navigate(it) }, onBack = { nav.popBackStack() })
    }
}
