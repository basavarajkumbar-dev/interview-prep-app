package com.interviewprep.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {
    // ---------- import
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertHandbooks(items: List<HandbookEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTracks(items: List<TrackEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTopics(items: List<TopicEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertLessons(items: List<LessonEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertQuestions(items: List<QuestionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertFlashcards(items: List<FlashcardEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertQuiz(items: List<QuizItemEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertScenarios(items: List<ScenarioEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCoding(items: List<CodingEntity>)
    @Insert suspend fun insertGlossary(items: List<GlossaryEntity>)
    @Insert suspend fun insertSearch(items: List<SearchEntity>)

    @Query("DELETE FROM handbooks") suspend fun clearHandbooks()
    @Query("DELETE FROM tracks") suspend fun clearTracks()
    @Query("DELETE FROM topics") suspend fun clearTopics()
    @Query("DELETE FROM lessons") suspend fun clearLessons()
    @Query("DELETE FROM questions") suspend fun clearQuestions()
    @Query("DELETE FROM flashcards") suspend fun clearFlashcards()
    @Query("DELETE FROM quiz_items") suspend fun clearQuiz()
    @Query("DELETE FROM scenarios") suspend fun clearScenarios()
    @Query("DELETE FROM coding") suspend fun clearCoding()
    @Query("DELETE FROM glossary") suspend fun clearGlossary()
    @Query("DELETE FROM search_index") suspend fun clearSearch()

    @Query("SELECT COUNT(*) FROM lessons") suspend fun lessonCount(): Int

    // ---------- tracks & topics
    @Query("SELECT * FROM handbooks ORDER BY id") fun handbooks(): Flow<List<HandbookEntity>>
    @Query("SELECT * FROM tracks ORDER BY sortOrder") fun tracks(): Flow<List<TrackEntity>>
    @Query("SELECT * FROM tracks ORDER BY sortOrder") suspend fun tracksOnce(): List<TrackEntity>
    @Query("SELECT * FROM topics ORDER BY sortOrder") fun topics(): Flow<List<TopicEntity>>
    @Query("SELECT * FROM topics ORDER BY sortOrder") suspend fun topicsOnce(): List<TopicEntity>
    @Query("SELECT * FROM topics WHERE id = :id") suspend fun topic(id: String): TopicEntity?
    @Query("SELECT * FROM topics WHERE trackId = :trackId ORDER BY sortOrder") fun topicsOfTrack(trackId: String): Flow<List<TopicEntity>>

    // ---------- lessons
    @Query("SELECT id, topicId, title, sortOrder, kind, readMinutes, hasCode, book, chapter, section, page FROM lessons WHERE topicId = :topicId ORDER BY sortOrder")
    fun lessonsOfTopic(topicId: String): Flow<List<LessonRow>>

    @Query("SELECT id, topicId, title, sortOrder, kind, readMinutes, hasCode, book, chapter, section, page FROM lessons ORDER BY topicId, sortOrder")
    suspend fun allLessonRows(): List<LessonRow>

    @Query("SELECT * FROM lessons WHERE id = :id") suspend fun lesson(id: String): LessonEntity?

    @Query("SELECT topicId, COUNT(*) AS n FROM lessons GROUP BY topicId") fun lessonCounts(): Flow<List<TopicCount>>
    @Query("SELECT topicId, COUNT(*) AS n FROM lessons GROUP BY topicId") suspend fun lessonCountsOnce(): List<TopicCount>

    // ---------- interview questions
    @Query(
        """SELECT * FROM questions
           WHERE (:topicId IS NULL OR topicId = :topicId)
             AND (:role IS NULL OR rolesCsv LIKE '%,' || :role || ',%')
             AND (:difficulty IS NULL OR difficulty = :difficulty)
             AND (:type IS NULL OR type = :type)
             AND (:personalOnly = 0 OR personal = 1)
             AND (:query IS NULL OR question LIKE '%' || :query || '%' OR answer LIKE '%' || :query || '%')
           ORDER BY personal DESC, topicId, page
           LIMIT :limit"""
    )
    fun questions(
        topicId: String?,
        role: String?,
        difficulty: String?,
        type: String?,
        personalOnly: Boolean,
        query: String?,
        limit: Int,
    ): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE id = :id") suspend fun question(id: String): QuestionEntity?
    @Query("SELECT * FROM questions WHERE id IN (:ids)") suspend fun questionsByIds(ids: List<String>): List<QuestionEntity>
    @Query("SELECT * FROM questions WHERE topicId = :topicId ORDER BY difficulty, page") fun questionsOfTopic(topicId: String): Flow<List<QuestionEntity>>
    @Query("SELECT * FROM questions") suspend fun allQuestions(): List<QuestionEntity>
    @Query("SELECT topicId, COUNT(*) AS n FROM questions GROUP BY topicId") suspend fun questionCounts(): List<TopicCount>

    // ---------- flashcards
    @Query("SELECT * FROM flashcards WHERE topicId IN (:topicIds)") suspend fun flashcardsOfTopics(topicIds: List<String>): List<FlashcardEntity>
    @Query("SELECT * FROM flashcards") suspend fun allFlashcards(): List<FlashcardEntity>
    @Query("SELECT * FROM flashcards WHERE id = :id") suspend fun flashcard(id: String): FlashcardEntity?
    @Query("SELECT topicId, COUNT(*) AS n FROM flashcards GROUP BY topicId") suspend fun flashcardCounts(): List<TopicCount>

    // ---------- quiz
    @Query("SELECT * FROM quiz_items WHERE topicId IN (:topicIds)") suspend fun quizOfTopics(topicIds: List<String>): List<QuizItemEntity>
    @Query("SELECT * FROM quiz_items") suspend fun allQuiz(): List<QuizItemEntity>
    @Query("SELECT * FROM quiz_items WHERE id IN (:ids)") suspend fun quizByIds(ids: List<String>): List<QuizItemEntity>
    @Query("SELECT topicId, COUNT(*) AS n FROM quiz_items GROUP BY topicId") suspend fun quizCounts(): List<TopicCount>

    // ---------- scenarios & coding
    @Query("SELECT * FROM scenarios ORDER BY topicId, title") fun scenarios(): Flow<List<ScenarioEntity>>
    @Query("SELECT * FROM scenarios") suspend fun allScenarios(): List<ScenarioEntity>
    @Query("SELECT * FROM scenarios WHERE id = :id") suspend fun scenario(id: String): ScenarioEntity?
    @Query("SELECT * FROM coding ORDER BY setName, code") fun coding(): Flow<List<CodingEntity>>
    @Query("SELECT * FROM coding WHERE id = :id") suspend fun codingChallenge(id: String): CodingEntity?

    // ---------- glossary & search
    @Query("SELECT * FROM glossary WHERE topicId = :topicId ORDER BY term") suspend fun glossaryOfTopic(topicId: String): List<GlossaryEntity>

    @Query("SELECT kind, refId, title, body, topicId FROM search_index WHERE search_index MATCH :match LIMIT :limit")
    suspend fun search(match: String, limit: Int): List<SearchRow>
}
