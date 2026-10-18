package com.interviewprep.core.domain

import com.interviewprep.core.model.MatchPair
import com.interviewprep.core.model.QuizType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizGraderTest {
    @Test
    fun mcqCorrectAndWrong() {
        val q = quiz("1")
        assertTrue(QuizGrader.grade(q, QuizAnswer.Choice(setOf(1))).correct)
        val wrong = QuizGrader.grade(q, QuizAnswer.Choice(setOf(2)))
        assertFalse(wrong.correct)
        assertTrue(wrong.feedback.contains("“b”"))
    }

    @Test
    fun noSelectionIsWrong() {
        assertFalse(QuizGrader.grade(quiz("1"), QuizAnswer.Choice(emptySet())).correct)
    }

    @Test
    fun multiNeedsExactSetButGivesPartialCredit() {
        val q = quiz("m", type = QuizType.MULTI, correct = listOf(0, 2, 3))
        assertTrue(QuizGrader.grade(q, QuizAnswer.Choice(setOf(0, 2, 3))).correct)
        val partial = QuizGrader.grade(q, QuizAnswer.Choice(setOf(0, 2)))
        assertFalse(partial.correct)
        assertEquals(2.0 / 3, partial.score, 1e-9)
        assertEquals(0.0, QuizGrader.grade(q, QuizAnswer.Choice(setOf(1))).score, 1e-9)
    }

    @Test
    fun fillBlankIsCaseAndPunctuationInsensitive() {
        val q = quiz("f", type = QuizType.FILL_BLANK, answer = "SIGTERM (15)", acceptable = listOf("SIGTERM (15)"))
        assertTrue(QuizGrader.grade(q, QuizAnswer.Text("sigterm 15")).correct)
        assertFalse(QuizGrader.grade(q, QuizAnswer.Text("SIGKILL")).correct)
        assertFalse(QuizGrader.grade(q, QuizAnswer.Text("   ")).correct)
    }

    @Test
    fun codeOutputIgnoresTrailingWhitespaceOnly() {
        val q = quiz("o", type = QuizType.CODE_OUTPUT, answer = "True False\nTrue", acceptable = listOf("True False\nTrue"))
        assertTrue(QuizGrader.grade(q, QuizAnswer.Text("True False  \nTrue\n")).correct)
        assertFalse(QuizGrader.grade(q, QuizAnswer.Text("True  False\nTrue")).correct)
        assertTrue(QuizGrader.grade(q, QuizAnswer.SelfMarked(true)).correct)
    }

    @Test
    fun matchScoresPairs() {
        val q = quiz("x", type = QuizType.MATCH, pairs = listOf(MatchPair("a", "1"), MatchPair("b", "2"), MatchPair("c", "3")))
        assertTrue(QuizGrader.grade(q, QuizAnswer.Matches(mapOf(0 to 0, 1 to 1, 2 to 2))).correct)
        val r = QuizGrader.grade(q, QuizAnswer.Matches(mapOf(0 to 1, 1 to 0, 2 to 2)))
        assertFalse(r.correct)
        assertEquals(1.0 / 3, r.score, 1e-9)
    }
}
