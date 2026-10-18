package com.interviewprep.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTest {
    @Test
    fun parsesAllBlockKinds() {
        val md = """
            ### Title

            A **bold** line
            continues here.

            - first bullet
            2. second item

            ```code
            def f():
                return 1
            ```

            ```output
            1
            ```

            | Term | Meaning |
            | ACK | acknowledge |
        """.trimIndent()
        val b = Markdown.parse(md)
        assertEquals(MdBlock.Heading("Title"), b[0])
        assertEquals(MdBlock.Paragraph("A **bold** line continues here."), b[1])
        assertEquals(MdBlock.Bullet("first bullet", "•"), b[2])
        assertEquals(MdBlock.Bullet("second item", "2."), b[3])
        assertEquals(MdBlock.Code("def f():\n    return 1", false), b[4])
        assertEquals(MdBlock.Code("1", true), b[5])
        assertEquals(MdBlock.Table(listOf(listOf("Term", "Meaning"), listOf("ACK", "acknowledge"))), b[6])
    }

    @Test
    fun unterminatedFenceStillProducesCode() {
        val b = Markdown.parse("```code\nx = 1")
        assertEquals(listOf(MdBlock.Code("x = 1", false)), b)
    }

    @Test
    fun spansHandleBoldCodeAndUnmatchedMarkers() {
        val s = Markdown.spans("Use **pytest** with `-q` and **kwargs alone")
        assertEquals(MdSpan.Bold("pytest"), s[1])
        assertEquals(MdSpan.Code("-q"), s[3])
        assertTrue(s.last() is MdSpan.Plain && s.last().text.contains("**kwargs"))
    }

    @Test
    fun emptyInputIsEmpty() {
        assertTrue(Markdown.parse("").isEmpty())
        assertTrue(Markdown.spans("").isEmpty())
    }
}
