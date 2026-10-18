package com.interviewprep.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.interviewprep.core.data.db.BookmarkEntity
import com.interviewprep.core.data.db.LessonProgressEntity
import com.interviewprep.core.data.db.QuizAttemptEntity
import com.interviewprep.core.data.db.StudySessionEntity
import com.interviewprep.core.data.db.UserDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Database, bookmarks, read/unread tracking, quiz stats and streak-day queries. */
@RunWith(AndroidJUnit4::class)
class UserDaoTest {
    private lateinit var db: UserDatabase

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, UserDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun bookmarkToggle() = runTest {
        val dao = db.userDao()
        assertFalse(dao.isBookmarked("TOPIC", "pytest").first())
        dao.insertBookmark(BookmarkEntity("TOPIC", "pytest", "pytest", "Test Automation", 1L))
        assertTrue(dao.isBookmarked("TOPIC", "pytest").first())
        dao.insertBookmark(BookmarkEntity("TOPIC", "pytest", "pytest", "Test Automation", 2L)) // replace, no duplicate
        assertEquals(1, dao.bookmarks().first().size)
        dao.deleteBookmark("TOPIC", "pytest")
        assertFalse(dao.isBookmarked("TOPIC", "pytest").first())
    }

    @Test
    fun readUnreadTracking() = runTest {
        val dao = db.userDao()
        dao.upsertLessonProgress(LessonProgressEntity("l1", "t", completed = false, completedAt = null, lastOpenedAt = 10))
        assertEquals(0, dao.completedLessonCount().first())
        dao.upsertLessonProgress(LessonProgressEntity("l1", "t", completed = true, completedAt = 20, lastOpenedAt = 20))
        dao.upsertLessonProgress(LessonProgressEntity("l2", "t", completed = false, completedAt = null, lastOpenedAt = 30))
        assertEquals(1, dao.completedLessonCount().first())
        assertEquals("l2", dao.lastOpenedLesson().first()?.lessonId)
    }

    @Test
    fun quizStatsIgnoreSkipped() = runTest {
        val dao = db.userDao()
        fun a(item: String, correct: Boolean, skipped: Boolean = false) =
            QuizAttemptEntity(itemId = item, topicId = "oop", correct = correct, score = if (correct) 1.0 else 0.0, difficulty = "MEDIUM",
                skipped = skipped, sessionId = null, answeredAt = 1, day = 100)
        dao.insertQuizAttempt(a("1", true)); dao.insertQuizAttempt(a("2", false)); dao.insertQuizAttempt(a("3", false, skipped = true))
        val stat = dao.quizStatsByTopic().first().single()
        assertEquals(2, stat.attempts)
        assertEquals(1, stat.correct)
        assertEquals(1, dao.quizSkippedCount().first())
    }

    @Test
    fun activeDaysUnionAcrossActivities() = runTest {
        val dao = db.userDao()
        dao.insertStudySession(StudySessionEntity(kind = "LESSON", topicId = null, startedAt = 0, durationSec = 60, day = 5))
        dao.insertQuizAttempt(QuizAttemptEntity(itemId = "1", topicId = "t", correct = true, score = 1.0, difficulty = "EASY", skipped = false, sessionId = null, answeredAt = 0, day = 6))
        assertEquals(setOf(5L, 6L), dao.activeDays().first().toSet())
    }
}
