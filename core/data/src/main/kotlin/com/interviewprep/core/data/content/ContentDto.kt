package com.interviewprep.core.data.content

import kotlinx.serialization.Serializable

/** Mirrors content.json produced by content-pipeline/build_content.py. */
@Serializable
data class ContentBundleDto(
    val version: Int,
    val handbooks: List<HandbookDto> = emptyList(),
    val roles: List<RoleDto> = emptyList(),
    val tracks: List<TrackDto> = emptyList(),
    val topics: List<TopicDto> = emptyList(),
    val lessons: List<LessonDto> = emptyList(),
    val questions: List<QuestionDto> = emptyList(),
    val flashcards: List<FlashcardDto> = emptyList(),
    val quiz: List<QuizDto> = emptyList(),
    val scenarios: List<ScenarioDto> = emptyList(),
    val coding: List<CodingDto> = emptyList(),
    val glossary: List<GlossaryDto> = emptyList(),
)

@Serializable
data class SourceDto(val book: String, val chapter: String = "", val section: String = "", val page: Int = 0)

@Serializable
data class HandbookDto(val id: String, val title: String, val shortTitle: String, val pages: Int)

@Serializable
data class RoleDto(val id: String, val title: String)

@Serializable
data class TrackDto(val id: String, val title: String, val emoji: String, val description: String, val order: Int, val roles: List<String>)

@Serializable
data class TopicDto(
    val id: String,
    val trackId: String,
    val title: String,
    val order: Int,
    val intro: String = "",
    val objectives: List<String> = emptyList(),
    val summary: List<String> = emptyList(),
    val keyPoints: List<String> = emptyList(),
    val commonMistakes: List<String> = emptyList(),
    val roles: List<String> = emptyList(),
    val sources: List<SourceDto> = emptyList(),
    val resumeTag: String? = null,
    val resumeTagSource: SourceDto? = null,
)

@Serializable
data class LessonDto(
    val id: String,
    val topicId: String,
    val title: String,
    val order: Int,
    val kind: String,
    val body: String,
    val readMinutes: Int,
    val hasCode: Boolean,
    val source: SourceDto,
)

@Serializable
data class QuestionDto(
    val id: String,
    val topicId: String,
    val question: String,
    val answer: String,
    val followUps: List<String> = emptyList(),
    val commonWrong: String? = null,
    val why: String? = null,
    val keyPoints: String? = null,
    val keywords: List<String> = emptyList(),
    val roles: List<String> = emptyList(),
    val difficulty: String = "MEDIUM",
    val type: String = "TECHNICAL",
    val personal: Boolean = false,
    val resumeTag: String? = null,
    val source: SourceDto,
    val altSources: List<SourceDto> = emptyList(),
)

@Serializable
data class FlashcardDto(
    val id: String,
    val topicId: String,
    val front: String,
    val back: String,
    val origin: String,
    val keywords: List<String> = emptyList(),
    val source: SourceDto,
)

@Serializable
data class PairDto(val left: String, val right: String)

@Serializable
data class QuizDto(
    val id: String,
    val topicId: String,
    val type: String,
    val prompt: String,
    val code: String? = null,
    val options: List<String> = emptyList(),
    val correct: List<Int> = emptyList(),
    val answerText: String? = null,
    val acceptable: List<String> = emptyList(),
    val pairs: List<PairDto> = emptyList(),
    val explanation: String = "",
    val difficulty: String = "MEDIUM",
    val origin: String = "HANDBOOK",
    val evidenceQuote: String? = null,
    val source: SourceDto,
)

@Serializable
data class StepDto(val label: String, val text: String)

@Serializable
data class ScenarioDto(
    val id: String,
    val topicId: String,
    val title: String,
    val prompt: String,
    val steps: List<StepDto>,
    val keywords: List<String> = emptyList(),
    val concept: Boolean = false,
    val difficulty: String = "HARD",
    val roles: List<String> = emptyList(),
    val source: SourceDto,
)

@Serializable
data class CodingDto(
    val id: String,
    val code: String,
    val set: String,
    val topicId: String,
    val title: String,
    val difficulty: String,
    val statement: String,
    val examples: List<String> = emptyList(),
    val hint: String? = null,
    val solution: String = "",
    val tests: List<String> = emptyList(),
    val entryPoints: List<String> = emptyList(),
    val complexity: String? = null,
    val notes: String? = null,
    val verified: Boolean = false,
    val source: SourceDto,
)

@Serializable
data class GlossaryDto(val term: String, val definition: String, val topicId: String, val source: SourceDto)
