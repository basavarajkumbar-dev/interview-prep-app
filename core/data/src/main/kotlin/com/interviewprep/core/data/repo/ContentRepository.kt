package com.interviewprep.core.data.repo

import com.interviewprep.core.data.db.ContentDao
import com.interviewprep.core.data.db.toModel
import com.interviewprep.core.model.CodingChallenge
import com.interviewprep.core.model.Difficulty
import com.interviewprep.core.model.Flashcard
import com.interviewprep.core.model.GlossaryEntry
import com.interviewprep.core.model.Handbook
import com.interviewprep.core.model.InterviewQuestion
import com.interviewprep.core.model.Lesson
import com.interviewprep.core.model.LessonSummary
import com.interviewprep.core.model.QuestionType
import com.interviewprep.core.model.QuizItem
import com.interviewprep.core.model.Scenario
import com.interviewprep.core.model.SearchHit
import com.interviewprep.core.model.SearchKind
import com.interviewprep.core.model.Topic
import com.interviewprep.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class QuestionFilter(
    val topicId: String? = null,
    val role: String? = null,
    val difficulty: Difficulty? = null,
    val type: QuestionType? = null,
    val personalOnly: Boolean = false,
    val query: String? = null,
    val limit: Int = 300,
)

/** Read access to handbook content. Large pools (quiz, flashcards) are cached after first load. */
@Singleton
class ContentRepository @Inject constructor(private val dao: ContentDao) {
    private val cacheLock = Mutex()
    private var quizCache: List<QuizItem>? = null
    private var cardCache: List<Flashcard>? = null
    private var questionCache: List<InterviewQuestion>? = null
    private var topicCache: List<Topic>? = null

    suspend fun invalidateCaches() = cacheLock.withLock {
        quizCache = null; cardCache = null; questionCache = null; topicCache = null
    }

    fun handbooks(): Flow<List<Handbook>> = dao.handbooks().map { l -> l.map { it.toModel() } }
    fun tracks(): Flow<List<Track>> = dao.tracks().map { l -> l.map { it.toModel() } }
    fun topics(): Flow<List<Topic>> = dao.topics().map { l -> l.map { it.toModel() } }.flowOn(Dispatchers.Default)
    suspend fun topicsOnce(): List<Topic> = cacheLock.withLock {
        topicCache ?: withContext(Dispatchers.IO) { dao.topicsOnce().map { it.toModel() } }.also { topicCache = it }
    }
    suspend fun topic(id: String): Topic? = withContext(Dispatchers.IO) { dao.topic(id)?.toModel() }
    suspend fun tracksOnce(): List<Track> = withContext(Dispatchers.IO) { dao.tracksOnce().map { it.toModel() } }
    fun lessonCounts(): Flow<Map<String, Int>> = dao.lessonCounts().map { l -> l.associate { it.topicId to it.n } }
    suspend fun lessonCountsOnce(): Map<String, Int> = withContext(Dispatchers.IO) { dao.lessonCountsOnce().associate { it.topicId to it.n } }

    fun lessonsOfTopic(topicId: String): Flow<List<LessonSummary>> = dao.lessonsOfTopic(topicId).map { l -> l.map { it.toModel() } }
    suspend fun allLessons(): List<LessonSummary> = withContext(Dispatchers.IO) { dao.allLessonRows().map { it.toModel() } }
    suspend fun lesson(id: String): Lesson? = withContext(Dispatchers.IO) { dao.lesson(id)?.toModel() }

    fun questions(f: QuestionFilter): Flow<List<InterviewQuestion>> =
        dao.questions(f.topicId, f.role, f.difficulty?.name, f.type?.name, f.personalOnly, f.query?.trim()?.takeIf { it.length >= 2 }, f.limit)
            .map { l -> l.map { it.toModel() } }
            .flowOn(Dispatchers.Default)

    fun questionsOfTopic(topicId: String): Flow<List<InterviewQuestion>> = dao.questionsOfTopic(topicId).map { l -> l.map { it.toModel() } }
    suspend fun question(id: String): InterviewQuestion? = withContext(Dispatchers.IO) { dao.question(id)?.toModel() }
    suspend fun questionsByIds(ids: List<String>): List<InterviewQuestion> = withContext(Dispatchers.IO) {
        val byId = dao.questionsByIds(ids).associateBy { it.id }
        ids.mapNotNull { byId[it]?.toModel() }
    }
    suspend fun allQuestions(): List<InterviewQuestion> = cacheLock.withLock {
        questionCache ?: withContext(Dispatchers.IO) { dao.allQuestions().map { it.toModel() } }.also { questionCache = it }
    }

    suspend fun allFlashcards(): List<Flashcard> = cacheLock.withLock {
        cardCache ?: withContext(Dispatchers.IO) { dao.allFlashcards().map { it.toModel() } }.also { cardCache = it }
    }

    suspend fun quizPool(): List<QuizItem> = cacheLock.withLock {
        quizCache ?: withContext(Dispatchers.IO) { dao.allQuiz().map { it.toModel() } }.also { quizCache = it }
    }
    suspend fun quizByIds(ids: List<String>): List<QuizItem> = withContext(Dispatchers.IO) {
        val byId = dao.quizByIds(ids).associateBy { it.id }
        ids.mapNotNull { byId[it]?.toModel() }
    }

    fun scenarios(): Flow<List<Scenario>> = dao.scenarios().map { l -> l.map { it.toModel() } }
    suspend fun allScenarios(): List<Scenario> = withContext(Dispatchers.IO) { dao.allScenarios().map { it.toModel() } }
    suspend fun scenario(id: String): Scenario? = withContext(Dispatchers.IO) { dao.scenario(id)?.toModel() }

    fun coding(): Flow<List<CodingChallenge>> = dao.coding().map { l -> l.map { it.toModel() } }
    suspend fun codingChallenge(id: String): CodingChallenge? = withContext(Dispatchers.IO) { dao.codingChallenge(id)?.toModel() }

    suspend fun glossaryOfTopic(topicId: String): List<GlossaryEntry> = withContext(Dispatchers.IO) { dao.glossaryOfTopic(topicId).map { it.toModel() } }

    /** Content counts per topic (lessons, questions, cards, quiz) for the Learn tree. */
    suspend fun countsByTopic(): Map<String, IntArray> = withContext(Dispatchers.IO) {
        val out = mutableMapOf<String, IntArray>()
        fun add(list: List<com.interviewprep.core.data.db.TopicCount>, idx: Int) =
            list.forEach { out.getOrPut(it.topicId) { IntArray(4) }[idx] = it.n }
        add(dao.lessonCountsOnce(), 0); add(dao.questionCounts(), 1); add(dao.flashcardCounts(), 2); add(dao.quizCounts(), 3)
        out
    }

    /** Global full-text search. Every word must match (prefix match), results grouped by kind. */
    suspend fun search(raw: String): List<SearchHit> = withContext(Dispatchers.IO) {
        val match = SearchQuery.toFtsMatch(raw) ?: return@withContext emptyList()
        runCatching { dao.search(match, 120) }.getOrDefault(emptyList()).map {
            SearchHit(SearchKind.parse(it.kind), it.refId, it.title, SearchQuery.snippet(it.body, raw), it.topicId)
        }.sortedBy { SearchKind.entries.indexOf(it.kind) }
    }
}
