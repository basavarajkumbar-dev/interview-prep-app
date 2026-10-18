package com.interviewprep.core.data.db

import com.interviewprep.core.data.content.PairDto
import com.interviewprep.core.data.content.SourceDto
import com.interviewprep.core.data.content.StepDto
import com.interviewprep.core.model.CardOrigin
import com.interviewprep.core.model.CodingChallenge
import com.interviewprep.core.model.Difficulty
import com.interviewprep.core.model.Flashcard
import com.interviewprep.core.model.GlossaryEntry
import com.interviewprep.core.model.Handbook
import com.interviewprep.core.model.InterviewQuestion
import com.interviewprep.core.model.Lesson
import com.interviewprep.core.model.LessonKind
import com.interviewprep.core.model.LessonSummary
import com.interviewprep.core.model.MatchPair
import com.interviewprep.core.model.QuestionType
import com.interviewprep.core.model.QuizItem
import com.interviewprep.core.model.QuizOrigin
import com.interviewprep.core.model.QuizType
import com.interviewprep.core.model.ResumeTag
import com.interviewprep.core.model.Scenario
import com.interviewprep.core.model.ScenarioStep
import com.interviewprep.core.model.SourceRef
import com.interviewprep.core.model.Topic
import com.interviewprep.core.model.Track
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** JSON (de)serialisation of list columns. Malformed values degrade to empty lists, never crash. */
internal object J {
    val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val strings = ListSerializer(String.serializer())
    private val ints = ListSerializer(Int.serializer())
    private val sources = ListSerializer(SourceDto.serializer())
    private val pairs = ListSerializer(PairDto.serializer())
    private val steps = ListSerializer(StepDto.serializer())

    fun str(v: List<String>): String = json.encodeToString(strings, v)
    fun strList(s: String?): List<String> = runCatching { json.decodeFromString(strings, s ?: "[]") }.getOrDefault(emptyList())
    fun int(v: List<Int>): String = json.encodeToString(ints, v)
    fun intList(s: String?): List<Int> = runCatching { json.decodeFromString(ints, s ?: "[]") }.getOrDefault(emptyList())
    fun src(v: List<SourceDto>): String = json.encodeToString(sources, v)
    fun srcList(s: String?): List<SourceDto> = runCatching { json.decodeFromString(sources, s ?: "[]") }.getOrDefault(emptyList())
    fun pair(v: List<PairDto>): String = json.encodeToString(pairs, v)
    fun pairList(s: String?): List<PairDto> = runCatching { json.decodeFromString(pairs, s ?: "[]") }.getOrDefault(emptyList())
    fun step(v: List<StepDto>): String = json.encodeToString(steps, v)
    fun stepList(s: String?): List<StepDto> = runCatching { json.decodeFromString(steps, s ?: "[]") }.getOrDefault(emptyList())

    fun csv(roles: List<String>): String = roles.joinToString(",", prefix = ",", postfix = ",")
    fun fromCsv(s: String): List<String> = s.split(',').filter { it.isNotBlank() }
}

internal fun SourceDto.toModel() = SourceRef(book, chapter, section, page)
private fun src(book: String, chapter: String, section: String, page: Int) = SourceRef(book, chapter, section, page)

fun HandbookEntity.toModel() = Handbook(id, title, shortTitle, pages)

fun TrackEntity.toModel() = Track(id, title, emoji, description, sortOrder, J.fromCsv(rolesCsv))

fun TopicEntity.toModel() = Topic(
    id, trackId, title, sortOrder, intro, J.strList(objectivesJson), J.strList(summaryJson),
    J.strList(keyPointsJson), J.strList(mistakesJson), J.fromCsv(rolesCsv), J.srcList(sourcesJson).map { it.toModel() },
    ResumeTag.parse(resumeTag), resumeTagSourceJson?.let { s -> J.srcList("[$s]").firstOrNull()?.toModel() },
)

private fun lessonKind(s: String) = LessonKind.entries.firstOrNull { it.name == s } ?: LessonKind.CONCEPT

fun LessonRow.toModel() = LessonSummary(id, topicId, title, sortOrder, lessonKind(kind), readMinutes, hasCode, src(book, chapter, section, page))

fun LessonEntity.toModel() = Lesson(
    LessonSummary(id, topicId, title, sortOrder, lessonKind(kind), readMinutes, hasCode, src(book, chapter, section, page)),
    body,
)

fun QuestionEntity.toModel() = InterviewQuestion(
    id, topicId, question, answer, J.strList(followUpsJson), commonWrong, why, keyPoints, J.strList(keywordsJson),
    J.fromCsv(rolesCsv), Difficulty.parse(difficulty), QuestionType.parse(type), personal, ResumeTag.parse(resumeTag),
    src(book, chapter, section, page), J.srcList(altSourcesJson).map { it.toModel() },
)

fun FlashcardEntity.toModel() = Flashcard(id, topicId, front, back, CardOrigin.parse(origin), J.strList(keywordsJson), src(book, chapter, section, page))

fun QuizItemEntity.toModel() = QuizItem(
    id, topicId, QuizType.parse(type), prompt, code, J.strList(optionsJson), J.intList(correctJson), answerText,
    J.strList(acceptableJson), J.pairList(pairsJson).map { MatchPair(it.left, it.right) }, explanation,
    Difficulty.parse(difficulty), QuizOrigin.parse(origin), evidenceQuote, src(book, chapter, section, page),
)

fun ScenarioEntity.toModel() = Scenario(
    id, topicId, title, prompt, J.stepList(stepsJson).map { ScenarioStep(it.label, it.text) }, J.strList(keywordsJson),
    concept, Difficulty.parse(difficulty), J.fromCsv(rolesCsv), src(book, chapter, section, page),
)

fun CodingEntity.toModel() = CodingChallenge(
    id, code, setName, topicId, title, Difficulty.parse(difficulty), statement, J.strList(examplesJson), hint, solution,
    J.strList(testsJson), J.strList(entryPointsJson), complexity, notes, verified, src(book, chapter, section, page),
)

fun GlossaryEntity.toModel() = GlossaryEntry(term, definition, topicId, src(book, chapter, section, page))
