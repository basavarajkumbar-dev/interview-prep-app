package com.interviewprep.core.domain

import com.interviewprep.core.model.Difficulty
import com.interviewprep.core.model.Flashcard
import com.interviewprep.core.model.InterviewQuestion
import com.interviewprep.core.model.QuestionType
import com.interviewprep.core.model.QuizItem
import com.interviewprep.core.model.Scenario
import kotlin.random.Random

enum class InterviewRole(val label: String, val roleIds: Set<String>, val tracks: Set<String>) {
    PYTHON_AUTOMATION("Python Automation Engineer", setOf("PY"), setOf("python", "automation", "api", "coding", "cicd")),
    TEST_AUTOMATION("Test Automation Engineer", setOf("TA"), setOf("automation", "python", "api", "cicd")),
    SENIOR_TEST_AUTOMATION("Senior Test Automation Engineer", setOf("TA", "SR"), setOf("automation", "python", "api", "cicd", "debugging")),
    EMBEDDED_TEST("Embedded Test / Validation Engineer", setOf("EMB"), setOf("protocols", "linux", "debugging", "openbmc", "ssd", "python")),
    SOFTWARE_TEST("Software Test Engineer", setOf("SW"), setOf("automation", "python", "coding", "cicd")),
    BMC_OPENBMC("BMC / OpenBMC Testing", setOf("BMC"), setOf("openbmc", "protocols", "api", "linux", "debugging")),
    SSD_NVME("SSD / NVMe Validation", setOf("SSD"), setOf("ssd", "linux", "debugging", "python", "protocols")),
}

enum class ExperienceLevel(val label: String) { INTERMEDIATE("Intermediate"), SENIOR("Senior") }

/** Builds the question mixes for mock interviews, daily challenges and quick revision. */
class SessionComposer(private val random: Random = Random.Default) {

    /**
     * Mock interview: ~10% personal/HR (from your resume handbook), the rest technical
     * questions from the role's tracks, with more scenarios and harder questions at senior level.
     */
    fun mockInterview(
        questions: List<InterviewQuestion>,
        topicTrack: Map<String, String>,
        role: InterviewRole,
        level: ExperienceLevel,
        length: Int,
        weakTopics: Set<String> = emptySet(),
    ): List<InterviewQuestion> {
        val usable = questions.filter { it.answer.length >= 40 }
        val personal = usable.filter { it.personal || it.type == QuestionType.BEHAVIORAL }
        val technical = usable.filter { !it.personal && it.type != QuestionType.BEHAVIORAL && topicTrack[it.topicId] in role.tracks }
        val nPersonal = maxOf(1, length / 10)
        val scenarioShare = if (level == ExperienceLevel.SENIOR) 0.35 else 0.2
        val nScenario = (length * scenarioShare).toInt()
        val nTech = length - nPersonal - nScenario
        val preferHard = level == ExperienceLevel.SENIOR
        fun weight(q: InterviewQuestion): Double {
            var w = 1.0
            if (q.topicId in weakTopics) w *= 1.8
            w *= when (q.difficulty) {
                Difficulty.EASY -> if (preferHard) 0.4 else 1.0
                Difficulty.MEDIUM -> 1.0
                Difficulty.HARD, Difficulty.EXPERT -> if (preferHard) 1.6 else 0.7
            }
            return w
        }
        val intro = personal.filter { it.question.contains("yourself", true) }.take(1)
        val picked = mutableListOf<InterviewQuestion>()
        picked += intro
        picked += weightedSample(personal - intro.toSet(), nPersonal - intro.size, ::weight)
        val scen = technical.filter { it.type == QuestionType.SCENARIO || it.type == QuestionType.DEBUGGING }
        picked += weightedSample(scen, nScenario, ::weight)
        val tech = technical.filter { it !in picked }
        picked += weightedSample(spreadByTopic(tech), nTech, ::weight)
        if (picked.size < length) picked += (usable - picked.toSet()).shuffled(random).take(length - picked.size)
        // order: intro first, then technical warm-up, scenarios, HR close
        val first = picked.filter { it in intro }
        val hr = picked.filter { it !in intro && (it.personal || it.type == QuestionType.BEHAVIORAL) }
        val sc = picked.filter { it.type == QuestionType.SCENARIO && it !in hr }
        val rest = picked.filter { it !in first && it !in hr && it !in sc }.sortedBy { it.difficulty.rank }
        return (first + rest + sc + hr).distinct().take(length)
    }

    private fun spreadByTopic(qs: List<InterviewQuestion>): List<InterviewQuestion> =
        qs.groupBy { it.topicId }.values.flatMap { it.shuffled(random).take(4) }

    fun <T> weightedSample(items: List<T>, n: Int, weight: (T) -> Double): List<T> {
        if (n <= 0 || items.isEmpty()) return emptyList()
        val pool = items.toMutableList()
        val out = mutableListOf<T>()
        while (out.size < n && pool.isNotEmpty()) {
            val ws = pool.map { weight(it).coerceAtLeast(0.0001) }
            var r = random.nextDouble() * ws.sum()
            var i = 0
            while (i < ws.size - 1 && r >= ws[i]) { r -= ws[i]; i++ }
            out += pool.removeAt(i)
        }
        return out
    }

    /** 10 questions, 10 minutes, at most 2 per topic, same set all day (seeded by date). */
    fun dailyChallenge(pool: List<QuizItem>, epochDay: Long, inputs: AdaptiveQuiz.Inputs): List<QuizItem> {
        val seeded = AdaptiveQuiz(Random(epochDay))
        return seeded.select(inputs.copy(pool = pool.filter { it.type != com.interviewprep.core.model.QuizType.MATCH }, count = 10, maxSharePerTopic = 0.2))
    }

    data class QuickRevision(
        val flashcards: List<Flashcard>,
        val questions: List<InterviewQuestion>,
        val quiz: List<QuizItem>,
        val scenario: Scenario?,
    )

    /** "15 minute revision": 5 flashcards, 3 interview questions, 2 quiz questions, 1 scenario. */
    fun quickRevision(
        cards: List<Flashcard>,
        questions: List<InterviewQuestion>,
        quiz: List<QuizItem>,
        scenarios: List<Scenario>,
        focusTopics: Set<String>,
        dueCardIds: Set<String>,
    ): QuickRevision {
        fun <T> focus(items: List<T>, topic: (T) -> String): List<T> {
            val f = items.filter { topic(it) in focusTopics }
            return if (f.size >= 3) f else items
        }
        val due = cards.filter { it.id in dueCardIds }
        val c = (focus(due, Flashcard::topicId) + focus(cards, Flashcard::topicId).shuffled(random)).distinct().take(5)
        val q = focus(questions.filter { !it.personal && it.answer.length in 40..900 }, InterviewQuestion::topicId).shuffled(random).take(3)
        val z = focus(quiz.filter { it.type != com.interviewprep.core.model.QuizType.MATCH }, QuizItem::topicId).shuffled(random).take(2)
        val s = focus(scenarios, Scenario::topicId).shuffled(random).firstOrNull()
        return QuickRevision(c, q, z, s)
    }
}

/** Final mock-interview report: per-area and overall percentages. */
data class InterviewReport(
    val overall: Int,
    val technical: Int,
    val communication: Int,
    val byArea: Map<String, Int>,
    val strengths: List<String>,
    val improve: List<String>,
)

object InterviewReporting {
    /** results: (track id, score 0..10, communication 0..1) per answered question. */
    fun report(results: List<Triple<String, Int, Double>>, trackTitles: Map<String, String>): InterviewReport {
        if (results.isEmpty()) return InterviewReport(0, 0, 0, emptyMap(), emptyList(), emptyList())
        val tech = results.map { it.second * 10.0 }.average()
        val comm = results.map { it.third * 100.0 }.average()
        val byArea = results.groupBy { it.first }.mapValues { (_, v) -> v.map { it.second * 10.0 }.average().toInt() }
            .mapKeys { trackTitles[it.key] ?: it.key }
        val overall = (0.8 * tech + 0.2 * comm).toInt()
        return InterviewReport(
            overall, tech.toInt(), comm.toInt(), byArea,
            byArea.filter { it.value >= 70 }.keys.toList(),
            byArea.filter { it.value < 60 }.keys.toList(),
        )
    }
}
