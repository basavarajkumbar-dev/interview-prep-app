package com.interviewprep.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        HandbookEntity::class, TrackEntity::class, TopicEntity::class, LessonEntity::class, QuestionEntity::class,
        FlashcardEntity::class, QuizItemEntity::class, ScenarioEntity::class, CodingEntity::class,
        GlossaryEntity::class, SearchEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ContentDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
}

/** Separate database so content can be rebuilt freely while progress is kept (and later synced). */
@Database(
    entities = [
        LessonProgressEntity::class, QuizAttemptEntity::class, QuizSessionEntity::class, CardScheduleEntity::class,
        CardReviewEntity::class, QuestionPracticeEntity::class, InterviewSessionEntity::class,
        ScenarioAttemptEntity::class, CodingAttemptEntity::class, CodeDraftEntity::class, BookmarkEntity::class,
        StudySessionEntity::class, XpEventEntity::class, AchievementEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
