package com.interviewprep.core.data.db

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

/* Content tables: rebuilt from assets/content/content.json whenever the content version changes.
 * List fields are stored as JSON text (see Converters / Mappers). */

@Entity(tableName = "handbooks")
data class HandbookEntity(@PrimaryKey val id: String, val title: String, val shortTitle: String, val pages: Int)

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val emoji: String,
    val description: String,
    val sortOrder: Int,
    val rolesCsv: String, // ",PY,TA," for LIKE filtering
)

@Entity(tableName = "topics", indices = [Index("trackId")])
data class TopicEntity(
    @PrimaryKey val id: String,
    val trackId: String,
    val title: String,
    val sortOrder: Int,
    val intro: String,
    val objectivesJson: String,
    val summaryJson: String,
    val keyPointsJson: String,
    val mistakesJson: String,
    val rolesCsv: String,
    val sourcesJson: String,
    val resumeTag: String?,
    val resumeTagSourceJson: String?,
)

@Entity(tableName = "lessons", indices = [Index("topicId")])
data class LessonEntity(
    @PrimaryKey val id: String,
    val topicId: String,
    val title: String,
    val sortOrder: Int,
    val kind: String,
    val body: String,
    val readMinutes: Int,
    val hasCode: Boolean,
    val book: String,
    val chapter: String,
    val section: String,
    val page: Int,
)

/** Lesson list rows without the (large) body, so lists never load whole lessons. */
data class LessonRow(
    val id: String,
    val topicId: String,
    val title: String,
    val sortOrder: Int,
    val kind: String,
    val readMinutes: Int,
    val hasCode: Boolean,
    val book: String,
    val chapter: String,
    val section: String,
    val page: Int,
)

@Entity(tableName = "questions", indices = [Index("topicId"), Index("type"), Index("difficulty")])
data class QuestionEntity(
    @PrimaryKey val id: String,
    val topicId: String,
    val question: String,
    val answer: String,
    val followUpsJson: String,
    val commonWrong: String?,
    val why: String?,
    val keyPoints: String?,
    val keywordsJson: String,
    val rolesCsv: String,
    val difficulty: String,
    val type: String,
    val personal: Boolean,
    val resumeTag: String?,
    val book: String,
    val chapter: String,
    val section: String,
    val page: Int,
    val altSourcesJson: String,
)

@Entity(tableName = "flashcards", indices = [Index("topicId")])
data class FlashcardEntity(
    @PrimaryKey val id: String,
    val topicId: String,
    val front: String,
    val back: String,
    val origin: String,
    val keywordsJson: String,
    val book: String,
    val chapter: String,
    val section: String,
    val page: Int,
)

@Entity(tableName = "quiz_items", indices = [Index("topicId"), Index("type")])
data class QuizItemEntity(
    @PrimaryKey val id: String,
    val topicId: String,
    val type: String,
    val prompt: String,
    val code: String?,
    val optionsJson: String,
    val correctJson: String,
    val answerText: String?,
    val acceptableJson: String,
    val pairsJson: String,
    val explanation: String,
    val difficulty: String,
    val origin: String,
    val evidenceQuote: String?,
    val book: String,
    val chapter: String,
    val section: String,
    val page: Int,
)

@Entity(tableName = "scenarios", indices = [Index("topicId")])
data class ScenarioEntity(
    @PrimaryKey val id: String,
    val topicId: String,
    val title: String,
    val prompt: String,
    val stepsJson: String,
    val keywordsJson: String,
    val concept: Boolean,
    val difficulty: String,
    val rolesCsv: String,
    val book: String,
    val chapter: String,
    val section: String,
    val page: Int,
)

@Entity(tableName = "coding")
data class CodingEntity(
    @PrimaryKey val id: String,
    val code: String,
    val setName: String,
    val topicId: String,
    val title: String,
    val difficulty: String,
    val statement: String,
    val examplesJson: String,
    val hint: String?,
    val solution: String,
    val testsJson: String,
    val entryPointsJson: String,
    val complexity: String?,
    val notes: String?,
    val verified: Boolean,
    val book: String,
    val chapter: String,
    val section: String,
    val page: Int,
)

@Entity(tableName = "glossary", indices = [Index("topicId")])
data class GlossaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val term: String,
    val definition: String,
    val topicId: String,
    val book: String,
    val chapter: String,
    val section: String,
    val page: Int,
)

/** Full-text search across every kind of content. */
@Fts4
@Entity(tableName = "search_index")
data class SearchEntity(
    val kind: String,
    val refId: String,
    val title: String,
    val body: String,
    val topicId: String,
)

data class SearchRow(val kind: String, val refId: String, val title: String, val body: String, val topicId: String)

/** Aggregates for the Learn tree. */
data class TopicCount(val topicId: String, val n: Int)
