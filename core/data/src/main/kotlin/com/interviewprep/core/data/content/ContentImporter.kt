package com.interviewprep.core.data.content

import android.content.Context
import androidx.room.withTransaction
import com.interviewprep.core.data.db.CodingEntity
import com.interviewprep.core.data.db.ContentDatabase
import com.interviewprep.core.data.db.FlashcardEntity
import com.interviewprep.core.data.db.GlossaryEntity
import com.interviewprep.core.data.db.HandbookEntity
import com.interviewprep.core.data.db.J
import com.interviewprep.core.data.db.LessonEntity
import com.interviewprep.core.data.db.QuestionEntity
import com.interviewprep.core.data.db.QuizItemEntity
import com.interviewprep.core.data.db.ScenarioEntity
import com.interviewprep.core.data.db.SearchEntity
import com.interviewprep.core.data.db.TopicEntity
import com.interviewprep.core.data.db.TrackEntity
import com.interviewprep.core.data.settings.SettingsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ContentState {
    data object Checking : ContentState
    data class Importing(val step: String) : ContentState
    data object Ready : ContentState
    data class Failed(val message: String) : ContentState
}

/**
 * Imports the bundled handbook content (assets/content/content.json) into the content database
 * on first launch and whenever the bundled content version changes. User progress lives in a
 * separate database and is untouched.
 */
@Singleton
class ContentImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: ContentDatabase,
    private val settings: SettingsStore,
) {
    private val _state = MutableStateFlow<ContentState>(ContentState.Checking)
    val state: StateFlow<ContentState> = _state.asStateFlow()
    private val mutex = Mutex()

    suspend fun ensureImported(force: Boolean = false) = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                _state.value = ContentState.Checking
                val bundledVersion = readBundledVersion()
                val installed = settings.contentVersion()
                val dao = db.contentDao()
                if (!force && installed == bundledVersion && dao.lessonCount() > 0) {
                    _state.value = ContentState.Ready
                    return@withContext
                }
                _state.value = ContentState.Importing("Reading handbooks…")
                val bundle = context.assets.open(ASSET).use { input ->
                    J.json.decodeFromString(ContentBundleDto.serializer(), input.readBytes().decodeToString())
                }
                validate(bundle)
                _state.value = ContentState.Importing("Building your library…")
                db.withTransaction {
                    dao.clearSearch(); dao.clearGlossary(); dao.clearCoding(); dao.clearScenarios(); dao.clearQuiz()
                    dao.clearFlashcards(); dao.clearQuestions(); dao.clearLessons(); dao.clearTopics(); dao.clearTracks()
                    dao.clearHandbooks()
                    dao.insertHandbooks(bundle.handbooks.map { HandbookEntity(it.id, it.title, it.shortTitle, it.pages) })
                    dao.insertTracks(bundle.tracks.map { TrackEntity(it.id, it.title, it.emoji, it.description, it.order, J.csv(it.roles)) })
                    dao.insertTopics(bundle.topics.map {
                        TopicEntity(
                            it.id, it.trackId, it.title, it.order, it.intro, J.str(it.objectives), J.str(it.summary),
                            J.str(it.keyPoints), J.str(it.commonMistakes), J.csv(it.roles), J.src(it.sources),
                            it.resumeTag, it.resumeTagSource?.let { s -> J.src(listOf(s)).removePrefix("[").removeSuffix("]") },
                        )
                    })
                    bundle.lessons.chunked(200).forEach { chunk ->
                        dao.insertLessons(chunk.map {
                            LessonEntity(it.id, it.topicId, it.title, it.order, it.kind, it.body, it.readMinutes, it.hasCode,
                                it.source.book, it.source.chapter, it.source.section, it.source.page)
                        })
                    }
                    dao.insertQuestions(bundle.questions.map {
                        QuestionEntity(
                            it.id, it.topicId, it.question, it.answer, J.str(it.followUps), it.commonWrong, it.why, it.keyPoints,
                            J.str(it.keywords), J.csv(it.roles), it.difficulty, it.type, it.personal, it.resumeTag,
                            it.source.book, it.source.chapter, it.source.section, it.source.page, J.src(it.altSources),
                        )
                    })
                    dao.insertFlashcards(bundle.flashcards.map {
                        FlashcardEntity(it.id, it.topicId, it.front, it.back, it.origin, J.str(it.keywords),
                            it.source.book, it.source.chapter, it.source.section, it.source.page)
                    })
                    dao.insertQuiz(bundle.quiz.map {
                        QuizItemEntity(
                            it.id, it.topicId, it.type, it.prompt, it.code, J.str(it.options), J.int(it.correct), it.answerText,
                            J.str(it.acceptable), J.pair(it.pairs), it.explanation, it.difficulty, it.origin, it.evidenceQuote,
                            it.source.book, it.source.chapter, it.source.section, it.source.page,
                        )
                    })
                    dao.insertScenarios(bundle.scenarios.map {
                        ScenarioEntity(it.id, it.topicId, it.title, it.prompt, J.step(it.steps), J.str(it.keywords), it.concept,
                            it.difficulty, J.csv(it.roles), it.source.book, it.source.chapter, it.source.section, it.source.page)
                    })
                    dao.insertCoding(bundle.coding.map {
                        CodingEntity(
                            it.id, it.code, it.set, it.topicId, it.title, it.difficulty, it.statement, J.str(it.examples), it.hint,
                            it.solution, J.str(it.tests), J.str(it.entryPoints), it.complexity, it.notes, it.verified,
                            it.source.book, it.source.chapter, it.source.section, it.source.page,
                        )
                    })
                    dao.insertGlossary(bundle.glossary.map {
                        GlossaryEntity(term = it.term, definition = it.definition, topicId = it.topicId, book = it.source.book,
                            chapter = it.source.chapter, section = it.source.section, page = it.source.page)
                    })
                    _state.value = ContentState.Importing("Indexing for search…")
                    buildSearchIndex(bundle).chunked(500).forEach { dao.insertSearch(it) }
                }
                settings.setContentVersion(bundledVersion)
                _state.value = ContentState.Ready
            } catch (t: Throwable) {
                _state.value = ContentState.Failed(t.message ?: t::class.java.simpleName)
            }
        }
    }

    private fun readBundledVersion(): Int = context.assets.open(ASSET).use { input ->
        // version is the first key in the file; read only the head instead of the whole bundle
        val head = ByteArray(256)
        val n = input.read(head)
        Regex("\"version\"\\s*:\\s*(\\d+)").find(String(head, 0, maxOf(0, n)))?.groupValues?.get(1)?.toInt() ?: 0
    }

    private fun validate(b: ContentBundleDto) {
        require(b.topics.isNotEmpty()) { "Content has no topics" }
        val topicIds = b.topics.map { it.id }.toSet()
        val orphans = b.lessons.count { it.topicId !in topicIds } + b.questions.count { it.topicId !in topicIds }
        require(orphans == 0) { "Content has $orphans items pointing to unknown topics" }
    }

    private fun buildSearchIndex(b: ContentBundleDto): List<SearchEntity> = buildList {
        b.topics.forEach { add(SearchEntity("TOPIC", it.id, it.title, it.intro + " " + it.objectives.joinToString(" "), it.id)) }
        b.lessons.forEach { add(SearchEntity("LESSON", it.id, it.title, it.body.take(6000), it.topicId)) }
        b.questions.forEach { add(SearchEntity("QUESTION", it.id, it.question, it.answer, it.topicId)) }
        b.flashcards.forEach { add(SearchEntity("FLASHCARD", it.id, it.front, it.back, it.topicId)) }
        b.scenarios.forEach { add(SearchEntity("SCENARIO", it.id, it.title, it.steps.joinToString(" ") { s -> s.text }, it.topicId)) }
        b.coding.forEach { add(SearchEntity("CODING", it.id, "${it.code}. ${it.title}", it.statement + " " + it.solution, it.topicId)) }
        b.glossary.forEach { add(SearchEntity("GLOSSARY", it.term, it.term, it.definition, it.topicId)) }
    }

    companion object {
        const val ASSET = "content/content.json"
    }
}
