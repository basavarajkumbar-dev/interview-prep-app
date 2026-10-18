package com.interviewprep.core.domain

import com.interviewprep.core.model.InterviewQuestion
import com.interviewprep.core.model.Scenario
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Offline evaluation of typed answers against the handbook's model answer.
 *
 * This is keyword/coverage based, not semantic understanding, and the UI says so.
 * It rewards: covering the model answer's technical keywords, structure (several
 * sentences / steps), and concrete detail (commands, numbers, examples).
 */
data class AnswerEvaluation(
    val score10: Int, // 0..10
    val coverage: Double, // 0..1 share of keywords mentioned
    val matched: List<String>,
    val missed: List<String>,
    val communication: Double, // 0..1
    val strengths: List<String>,
    val gaps: List<String>,
)

data class ScenarioEvaluation(
    val overall10: Int,
    val approach: Double,
    val technical: Double,
    val sequence: Double,
    val communication: Double,
    val matched: List<String>,
    val missedChecks: List<String>,
    val notes: List<String>,
)

object AnswerScorer {
    /** Common synonyms/abbreviations so honest paraphrases still match. */
    private val synonyms: Map<String, List<String>> = mapOf(
        "journalctl" to listOf("journal", "logs"),
        "dmesg" to listOf("kernel log", "kernel messages"),
        "reproduce" to listOf("repro", "reproduction"),
        "timeout" to listOf("time out", "deadline"),
        "idempotent" to listOf("idempotency"),
        "authentication" to listOf("auth", "login", "credentials"),
        "firmware" to listOf("fw"),
        "configuration" to listOf("config"),
        "regression" to listOf("regress"),
        "evidence" to listOf("logs", "capture"),
    )

    fun keywordHits(answer: String, keywords: List<String>): Pair<List<String>, List<String>> {
        val text = Text.normalize(answer).lowercase()
        val stems = Text.tokens(answer).map { Text.stem(it) }.toSet()
        val matched = mutableListOf<String>()
        val missed = mutableListOf<String>()
        for (k in keywords) {
            val kl = k.lowercase()
            val hit = kl in text ||
                Text.stem(kl) in stems ||
                synonyms[kl]?.any { it in text } == true ||
                (kl.contains(' ') && kl.split(' ').all { Text.stem(it) in stems })
            if (hit) matched += k else missed += k
        }
        return matched to missed
    }

    fun communication(answer: String): Double {
        val t = Text.normalize(answer)
        val words = t.split(Regex("\\s+")).count { it.isNotBlank() }
        if (words == 0) return 0.0
        val sentences = t.split(Regex("[.!?\\n]+")).count { it.trim().split(" ").size >= 3 }
        val lengthScore = when {
            words < 12 -> 0.2
            words < 30 -> 0.5
            words <= 220 -> 1.0
            else -> 0.75 // rambling
        }
        val structure = min(1.0, sentences / 3.0)
        val concrete = listOf(
            Regex("\\b(for example|e\\.g\\.|such as|in my|i would|i'd|first|then|finally)\\b", RegexOption.IGNORE_CASE),
            Regex("[a-z]+ -[a-z]|/[a-z]|\\d"), // commands, paths, numbers
        ).count { it.containsMatchIn(t) } / 2.0
        return (0.45 * lengthScore + 0.35 * structure + 0.2 * concrete).coerceIn(0.0, 1.0)
    }

    fun evaluate(answer: String, question: InterviewQuestion): AnswerEvaluation =
        evaluate(answer, question.keywords.ifEmpty { Text.tokens(question.answer).distinct().take(8) })

    fun evaluate(answer: String, keywords: List<String>): AnswerEvaluation {
        if (answer.isBlank()) {
            return AnswerEvaluation(0, 0.0, emptyList(), keywords, 0.0, emptyList(), listOf("No answer given."))
        }
        val (matched, missed) = keywordHits(answer, keywords)
        val coverage = if (keywords.isEmpty()) 0.5 else matched.size.toDouble() / keywords.size
        val comm = communication(answer)
        // coverage saturates: mentioning ~70% of key terms is already a strong answer
        val content = min(1.0, coverage / 0.7)
        val raw = 0.75 * content + 0.25 * comm
        val score = (raw * 10).roundToInt().coerceIn(0, 10)
        val strengths = buildList {
            if (matched.isNotEmpty()) add("Covered: ${matched.take(5).joinToString(", ")}")
            if (comm >= 0.7) add("Clear, structured answer")
            if (Regex("for example|e\\.g\\.|in my", RegexOption.IGNORE_CASE).containsMatchIn(answer)) add("Gave an example")
        }
        val gaps = buildList {
            if (missed.isNotEmpty()) add("Not mentioned: ${missed.take(6).joinToString(", ")}")
            if (comm < 0.45) add("Add structure: definition → example → why it matters → trade-off")
            if (answer.trim().split(Regex("\\s+")).size < 15) add("Too short for an interview answer")
        }
        return AnswerEvaluation(score, coverage, matched, missed, comm, strengths, gaps)
    }

    /**
     * Scenario answers are judged on five dimensions (approach, technical correctness,
     * troubleshooting sequence, missing checks, communication).
     */
    fun evaluateScenario(answer: String, scenario: Scenario): ScenarioEvaluation {
        val keywords = scenario.keywords
        val (matched, missed) = keywordHits(answer, keywords)
        val technical = if (keywords.isEmpty()) 0.0 else min(1.0, matched.size.toDouble() / keywords.size / 0.6)
        val lower = Text.normalize(answer).lowercase()
        val approachSignals = listOf(
            "reproduce", "evidence", "log", "baseline", "previous build", "compare", "isolate", "hypothes",
            "first", "check", "console", "capture", "one variable", "root cause",
        )
        val approach = min(1.0, approachSignals.count { it in lower } / 4.0)
        // sequence: do matched keywords appear in the same order as in the ideal flow?
        val ideal = scenario.steps.joinToString(" ") { it.text }.lowercase()
        val idealPos = matched.mapNotNull { k -> ideal.indexOf(k.lowercase()).takeIf { it >= 0 }?.let { k to it } }.toMap()
        val answerOrder = matched.filter { it in idealPos }.sortedBy { lower.indexOf(it.lowercase()).let { p -> if (p < 0) Int.MAX_VALUE else p } }
        val seq = answerOrder.map { idealPos.getValue(it) }
        val sequence = if (seq.size < 2) (if (seq.isEmpty()) 0.0 else 0.5) else longestIncreasing(seq).toDouble() / seq.size
        val comm = communication(answer)
        val overall = (0.25 * approach + 0.35 * technical + 0.15 * sequence + 0.25 * comm)
        val notes = buildList {
            if (approach < 0.5) add("Start by stating how you'd gather evidence before changing anything.")
            if (sequence < 0.6 && seq.size >= 2) add("Order your checks from the most basic layer upward, as in the ideal flow.")
            if (comm < 0.5) add("Speak in steps: symptom → checks → likely causes → how you'd report it.")
        }
        return ScenarioEvaluation(
            (overall * 10).roundToInt().coerceIn(0, 10), approach, technical, sequence, comm,
            matched, missed, notes,
        )
    }

    internal fun longestIncreasing(xs: List<Int>): Int {
        val tails = mutableListOf<Int>()
        for (x in xs) {
            var lo = 0
            var hi = tails.size
            while (lo < hi) {
                val mid = (lo + hi) / 2
                if (tails[mid] < x) lo = mid + 1 else hi = mid
            }
            if (lo == tails.size) tails += x else tails[lo] = x
        }
        return tails.size
    }
}
