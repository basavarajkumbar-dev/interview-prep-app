package com.interviewprep.core.domain

import com.interviewprep.core.model.QuizItem
import com.interviewprep.core.model.QuizType

/** The user's answer to one quiz item. */
sealed interface QuizAnswer {
    data class Choice(val selected: Set<Int>) : QuizAnswer
    data class Text(val value: String) : QuizAnswer
    data class Matches(val rightForLeft: Map<Int, Int>) : QuizAnswer // left index -> chosen right index
    data class SelfMarked(val correct: Boolean) : QuizAnswer
}

data class GradeResult(
    val correct: Boolean,
    val score: Double, // 0..1 (partial credit for MULTI and MATCH)
    val feedback: String,
)

object QuizGrader {
    fun grade(item: QuizItem, answer: QuizAnswer): GradeResult = when (item.type) {
        QuizType.MCQ, QuizType.TRUE_FALSE, QuizType.SCENARIO, QuizType.DEBUGGING -> gradeSingle(item, answer)
        QuizType.MULTI -> gradeMulti(item, answer)
        QuizType.FILL_BLANK -> gradeText(item, answer, output = false)
        QuizType.CODE_OUTPUT -> gradeText(item, answer, output = true)
        QuizType.MATCH -> gradeMatch(item, answer)
    }

    private fun gradeSingle(item: QuizItem, answer: QuizAnswer): GradeResult {
        val sel = (answer as? QuizAnswer.Choice)?.selected ?: emptySet()
        val ok = sel.size == 1 && sel.first() in item.correct
        val chosen = sel.firstOrNull()?.let { item.options.getOrNull(it) }
        val right = item.correct.mapNotNull { item.options.getOrNull(it) }.joinToString()
        return GradeResult(
            ok, if (ok) 1.0 else 0.0,
            if (ok) "Correct." else buildString {
                if (chosen != null) append("You chose “$chosen”. ")
                append("The correct answer is “$right”.")
            },
        )
    }

    private fun gradeMulti(item: QuizItem, answer: QuizAnswer): GradeResult {
        val sel = (answer as? QuizAnswer.Choice)?.selected ?: emptySet()
        val correct = item.correct.toSet()
        val hits = sel.intersect(correct).size
        val wrong = (sel - correct).size
        val score = ((hits - wrong).toDouble() / correct.size).coerceIn(0.0, 1.0)
        val ok = sel == correct
        val missed = (correct - sel).mapNotNull { item.options.getOrNull(it) }
        val extra = (sel - correct).mapNotNull { item.options.getOrNull(it) }
        return GradeResult(ok, score, if (ok) "Correct — all the right options." else buildString {
            if (missed.isNotEmpty()) append("Missed: ${missed.joinToString("; ")}. ")
            if (extra.isNotEmpty()) append("Not correct: ${extra.joinToString("; ")}.")
        }.trim())
    }

    private fun gradeText(item: QuizItem, answer: QuizAnswer, output: Boolean): GradeResult {
        if (answer is QuizAnswer.SelfMarked) {
            return GradeResult(answer.correct, if (answer.correct) 1.0 else 0.0, "Self-marked against the handbook answer.")
        }
        val given = (answer as? QuizAnswer.Text)?.value.orEmpty()
        val accepted = (item.acceptable + listOfNotNull(item.answerText)).distinct()
        val ok = if (output) {
            val g = Text.normalizeOutput(given)
            accepted.any { Text.normalizeOutput(it) == g }
        } else {
            val g = Text.loose(given)
            g.isNotEmpty() && accepted.any { Text.loose(it) == g }
        }
        return GradeResult(ok, if (ok) 1.0 else 0.0, if (ok) "Correct." else "Expected: ${item.answerText ?: accepted.firstOrNull().orEmpty()}")
    }

    private fun gradeMatch(item: QuizItem, answer: QuizAnswer): GradeResult {
        val m = (answer as? QuizAnswer.Matches)?.rightForLeft ?: emptyMap()
        val n = item.pairs.size
        val right = item.pairs.indices.count { m[it] == it }
        val ok = right == n && n > 0
        return GradeResult(ok, if (n == 0) 0.0 else right.toDouble() / n, if (ok) "All pairs matched." else "$right of $n pairs matched.")
    }
}
