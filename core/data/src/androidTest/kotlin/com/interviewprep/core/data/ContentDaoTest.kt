package com.interviewprep.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.interviewprep.core.data.db.ContentDatabase
import com.interviewprep.core.data.db.SearchEntity
import com.interviewprep.core.data.repo.SearchQuery
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Full-text search over the FTS4 index. */
@RunWith(AndroidJUnit4::class)
class ContentDaoTest {
    private lateinit var db: ContentDatabase

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, ContentDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun searchFindsPrefixMatchesAcrossKinds() = runTest {
        val dao = db.contentDao()
        dao.insertSearch(
            listOf(
                SearchEntity("LESSON", "l1", "Fixtures and scopes", "A pytest fixture provides a resource with teardown.", "pytest"),
                SearchEntity("QUESTION", "q1", "What is a fixture?", "A function decorated with @pytest.fixture.", "pytest"),
                SearchEntity("LESSON", "l2", "Redfish sessions", "X-Auth-Token header.", "redfish"),
            ),
        )
        val hits = dao.search(SearchQuery.toFtsMatch("pytest fixtur")!!, 50)
        assertEquals(setOf("l1", "q1"), hits.map { it.refId }.toSet())
        assertTrue(dao.search(SearchQuery.toFtsMatch("nvme")!!, 50).isEmpty())
    }
}
