package com.interviewprep.core.data.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchQueryTest {
    @Test
    fun buildsPrefixMatchForEachWord() {
        assertEquals("pytest* fixture*", SearchQuery.toFtsMatch("pytest fixture"))
    }

    @Test
    fun stripsFtsSyntaxAndShortWords() {
        assertEquals("redfish* odata* id*", SearchQuery.toFtsMatch("\"Redfish\" AND-(@odata.id) a"))
        assertNull(SearchQuery.toFtsMatch("  * - \" "))
        assertNull(SearchQuery.toFtsMatch(""))
    }

    @Test
    fun limitsWordCount() {
        assertEquals(6, SearchQuery.toFtsMatch("a1 b2 c3 d4 e5 f6 g7 h8")!!.split(' ').size)
    }

    @Test
    fun snippetCentresOnMatchAndStripsMarkdown() {
        val body = "Intro text. ".repeat(30) + "A **pytest fixture** provides a resource." + " More.".repeat(40)
        val s = SearchQuery.snippet(body, "fixture")
        assertTrue(s.contains("pytest fixture"))
        assertTrue(s.startsWith("…"))
        assertTrue(!s.contains("**"))
    }
}
