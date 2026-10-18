package com.interviewprep.feature.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.interviewprep.core.data.repo.ContentRepository
import com.interviewprep.core.data.repo.ProgressRepository
import com.interviewprep.core.designsystem.EmptyState
import com.interviewprep.core.designsystem.ListRow
import com.interviewprep.core.designsystem.LoadingState
import com.interviewprep.core.designsystem.Routes
import com.interviewprep.core.designsystem.ScreenTopBar
import com.interviewprep.core.designsystem.SectionTitle
import com.interviewprep.core.model.Bookmark
import com.interviewprep.core.model.BookmarkType
import com.interviewprep.core.model.SearchHit
import com.interviewprep.core.model.SearchKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(val query: String = "", val searching: Boolean = false, val results: Map<SearchKind, List<SearchHit>> = emptyMap())

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(private val content: ContentRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val results = query.debounce(250).distinctUntilChanged().mapLatest { q ->
        if (q.trim().length < 2) emptyMap() else content.search(q).groupBy { it.kind }
    }
    val state: StateFlow<SearchUiState> = combine(query, results) { q, r -> SearchUiState(q, false, r) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun setQuery(q: String) { query.value = q }
}

fun routeFor(kind: SearchKind, refId: String, topicId: String?): String = when (kind) {
    SearchKind.TOPIC -> Routes.topic(refId)
    SearchKind.LESSON -> Routes.lesson(refId)
    SearchKind.QUESTION -> Routes.question(refId)
    SearchKind.FLASHCARD -> Routes.flashcards(topicId)
    SearchKind.SCENARIO -> Routes.scenario(refId)
    SearchKind.CODING -> Routes.codingChallenge(refId)
    SearchKind.GLOSSARY -> Routes.topic(topicId.orEmpty())
}

private fun emojiFor(kind: SearchKind) = when (kind) {
    SearchKind.TOPIC -> "📚"; SearchKind.LESSON -> "📖"; SearchKind.QUESTION -> "💬"; SearchKind.FLASHCARD -> "🃏"
    SearchKind.SCENARIO -> "🔍"; SearchKind.CODING -> "💻"; SearchKind.GLOSSARY -> "🔤"
}

@Composable
fun SearchScreen(navigate: (String) -> Unit, onBack: () -> Unit, vm: SearchViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Scaffold(topBar = { ScreenTopBar("Search", onBack) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item {
                OutlinedTextField(
                    value = s.query,
                    onValueChange = vm::setQuery,
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    placeholder = { Text("Search topics, questions, code… e.g. pytest fixture") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (s.query.isNotEmpty()) IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Outlined.Close, contentDescription = "Clear search") }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )
            }
            if (s.query.trim().length >= 2 && s.results.isEmpty()) {
                item { EmptyState("🔎", "No results", "Nothing in the handbooks matches “${s.query.trim()}”. Try fewer or different words.") }
            }
            if (s.query.isBlank()) {
                item { EmptyState("📚", "Search all five handbooks", "Topics, lessons, interview questions, flashcards, scenarios, code and glossary terms.") }
            }
            SearchKind.entries.forEach { kind ->
                val hits = s.results[kind].orEmpty()
                if (hits.isNotEmpty()) {
                    item(key = "h-$kind") { SectionTitle("${kind.label} (${hits.size})") }
                    items(hits.take(25), key = { "${it.kind}-${it.refId}" }) { h ->
                        ListRow(title = h.title, subtitle = h.snippet, leading = emojiFor(kind), onClick = { navigate(routeFor(h.kind, h.refId, h.topicId)) })
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Bookmarks
@HiltViewModel
class BookmarksViewModel @Inject constructor(private val progress: ProgressRepository) : ViewModel() {
    val state: StateFlow<List<Bookmark>?> = progress.bookmarks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun remove(b: Bookmark) = viewModelScope.launch { progress.setBookmark(b.type, b.refId, b.title, "", false) }
}

private fun bookmarkRoute(b: Bookmark): String = when (b.type) {
    BookmarkType.TOPIC -> Routes.topic(b.refId)
    BookmarkType.LESSON -> Routes.lesson(b.refId)
    BookmarkType.QUESTION -> Routes.question(b.refId)
    BookmarkType.FLASHCARD -> Routes.flashcards(null)
    BookmarkType.CODING -> Routes.codingChallenge(b.refId)
    BookmarkType.SCENARIO -> Routes.scenario(b.refId)
    BookmarkType.NOTE -> Routes.lesson(b.refId)
}

@Composable
fun BookmarksScreen(navigate: (String) -> Unit, onBack: () -> Unit, vm: BookmarksViewModel = hiltViewModel()) {
    val list by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { ScreenTopBar("Bookmarks", onBack) }) { pad ->
        val saved = list
        when {
            saved == null -> LoadingState(modifier = Modifier.padding(pad))
            saved.isEmpty() -> EmptyState("⭐", "No bookmarks yet", "Tap the bookmark icon on topics, lessons, questions, flashcards and code to save them here.", Modifier.padding(pad))
            else -> LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp)) {
                saved.groupBy { it.type }.forEach { (type, group) ->
                    item(key = "h-$type") { SectionTitle("${type.label}s") }
                    items(group, key = { "${it.type}-${it.refId}" }) { b ->
                        ListRow(
                            title = b.title,
                            leading = "⭐",
                            onClick = { navigate(bookmarkRoute(b)) },
                            trailing = { IconButton(onClick = { vm.remove(b) }) { Icon(Icons.Outlined.Delete, contentDescription = "Remove bookmark") } },
                        )
                    }
                }
            }
        }
    }
}
