package com.interviewprep.core.domain

import com.interviewprep.core.model.ActivityKind
import com.interviewprep.core.model.Difficulty
import com.interviewprep.core.model.QuizAttempt
import com.interviewprep.core.model.TopicPerformance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class StreakTest {
    @Test
    fun streakCountsBackFromTodayOrYesterday() {
        assertEquals(StreakInfo(3, 3, true), Streak.compute(listOf(8L, 9L, 10L), today = 10))
        // not studied yet today: yesterday's streak still counts
        assertEquals(StreakInfo(3, 3, false), Streak.compute(listOf(8L, 9L, 10L), today = 11))
        // a gap breaks it
        assertEquals(0, Streak.compute(listOf(8L, 9L), today = 11).current)
        assertEquals(StreakInfo(0, 0, false), Streak.compute(emptyList(), 5))
    }

    @Test
    fun longestRunIsTracked() {
        val s = Streak.compute(listOf(1L, 2L, 3L, 4L, 10L, 11L), today = 11)
        assertEquals(2, s.current)
        assertEquals(4, s.longest)
    }
}

class MasteryTest {
    private fun attempts(vararg correct: Boolean) = correct.mapIndexed { i, c -> QuizAttempt("i$i", "t", c, Difficulty.MEDIUM, i.toLong()) }

    @Test
    fun recentAttemptsWeighMore() {
        val improving = Mastery.recentAccuracy(attempts(false, false, false, true, true, true))!!
        val declining = Mastery.recentAccuracy(attempts(true, true, true, false, false, false))!!
        assertTrue(improving > 0.5)
        assertTrue(declining < 0.5)
        assertNull(Mastery.recentAccuracy(emptyList()))
    }

    @Test
    fun bandsAndWeakTopics() {
        val weak = Mastery.score(TopicPerformance("oop", 1, 10, 10, 3, 0.3, 2, 6, null))
        val strong = Mastery.score(TopicPerformance("regex", 8, 10, 12, 11, 0.92, 9, 10, 0.8))
        val unseen = Mastery.score(TopicPerformance("i2c", 0, 10, 0, 0, null, 0, 0, null))
        assertEquals(MasteryBand.WEAK, weak.band)
        assertEquals(MasteryBand.STRONG, strong.band)
        assertEquals(MasteryBand.UNSEEN, unseen.band)
        assertEquals(listOf("oop"), Mastery.weakTopics(listOf(weak, strong, unseen)).map { it.topicId })
    }
}

class AdaptiveQuizTest {
    @Test
    fun difficultyFollowsAccuracy() {
        val a = AdaptiveQuiz(Random(1))
        assertEquals(Difficulty.MEDIUM, a.targetDifficulty(null, 0))
        assertEquals(Difficulty.EASY, a.targetDifficulty(0.3, 10))
        assertEquals(Difficulty.HARD, a.targetDifficulty(0.8, 10))
        assertEquals(Difficulty.EXPERT, a.targetDifficulty(0.95, 10))
        assertEquals(Difficulty.MEDIUM, a.targetDifficulty(0.95, 2)) // not enough evidence
    }

    @Test
    fun weakTopicGetsMoreQuestions() {
        val pool = (1..50).map { quiz("oop$it", topic = "oop") } + (1..50).map { quiz("re$it", topic = "regex") }
        val mastery = mapOf(
            "oop" to TopicMastery("oop", 0.2, MasteryBand.WEAK, 20),
            "regex" to TopicMastery("regex", 0.95, MasteryBand.STRONG, 20),
        )
        var oop = 0
        repeat(30) { seed ->
            val picked = AdaptiveQuiz(Random(seed)).select(AdaptiveQuiz.Inputs(pool, mastery, count = 10))
            assertEquals(10, picked.size)
            assertEquals(10, picked.map { it.id }.distinct().size)
            oop += picked.count { it.topicId == "oop" }
        }
        assertTrue("oop share was $oop/300", oop > 180)
    }

    @Test
    fun highAccuracyShiftsToHarderItems() {
        val pool = (1..40).map { quiz("e$it", difficulty = Difficulty.EASY) } + (1..40).map { quiz("h$it", difficulty = Difficulty.HARD) }
        val input = AdaptiveQuiz.Inputs(pool, topicAccuracy = mapOf("py-exceptions" to (0.95 to 30)), count = 10)
        var hard = 0
        repeat(20) { hard += AdaptiveQuiz(Random(it)).select(input).count { q -> q.difficulty == Difficulty.HARD } }
        assertTrue("hard=$hard/200", hard > 150)
    }

    @Test
    fun emptyPoolGivesEmptyQuiz() {
        assertTrue(AdaptiveQuiz().select(AdaptiveQuiz.Inputs(emptyList())).isEmpty())
    }
}

class ReadinessTest {
    @Test
    fun allPerfectIs100AndNothingIsStarting() {
        val r = ReadinessCalculator.compute(ReadinessInputs(1.0, 1.0, 1.0, 1.0, 1.0, 1.0))
        assertEquals(100, r.score)
        assertEquals(ReadinessBand.READY, r.band)
        val z = ReadinessCalculator.compute(ReadinessInputs(0.0, null, null, null, null, null))
        assertEquals(0, z.score)
        assertEquals(ReadinessBand.STARTING, z.band)
    }

    @Test
    fun weightsSumToOne() {
        val r = ReadinessCalculator.compute(ReadinessInputs(0.5, 0.5, 0.5, 0.5, 0.5, 0.5))
        assertEquals(50, r.score)
        assertEquals(1.0, r.components.sumOf { it.weight }, 1e-9)
    }
}

class PlannerTest {
    private fun signals(min: Int, weak: Boolean = true, due: Int = 20, acc: Double? = 0.7) =
        PlannerSignals(min, due, if (weak) "oop" to "OOP" else null, "py-exceptions" to "Exception Handling", acc, 1, 1)

    @Test
    fun ninetyMinuteTemplateAddsUp() {
        val p = StudyPlanner.plan(signals(90))
        assertEquals(90, p.blocks.sumOf { it.minutes })
        assertTrue(p.blocks.any { it.kind == ActivityKind.REVISION && it.topicId == "oop" })
    }

    @Test
    fun everyBudgetAddsUpAndIsCapped() {
        for (m in StudyPlanner.OPTIONS) assertEquals(m, StudyPlanner.plan(signals(m)).blocks.sumOf { it.minutes })
        assertEquals(90, StudyPlanner.plan(signals(180)).totalMinutes)
    }

    @Test
    fun noWeakTopicMeansNoWeakBlock() {
        assertFalse(StudyPlanner.plan(signals(90, weak = false)).blocks.any { it.kind == ActivityKind.REVISION })
    }

    @Test
    fun lowAccuracyShiftsTimeToLearning() {
        val low = StudyPlanner.plan(signals(90, acc = 0.4)).blocks.first { it.kind == ActivityKind.LESSON }.minutes
        val high = StudyPlanner.plan(signals(90, acc = 0.95)).blocks.first { it.kind == ActivityKind.LESSON }.minutes
        assertTrue("low=$low high=$high", low > high)
    }
}

class GamificationTest {
    @Test
    fun levels() {
        assertEquals(1, Xp.level(0))
        assertEquals(2, Xp.level(50))
        assertEquals(3, Xp.level(200))
        assertEquals("Automation Warrior", Xp.title(12))
        assertTrue(Xp.progressInLevel(125) in 0.4f..0.6f)
    }

    @Test
    fun achievements() {
        val s = AchievementStats(1, 100, 7, 0.9, 1, 0.91, 25, 10, 10, 200)
        assertEquals(Achievements.all.map { it.id }.toSet(), Achievements.earned(s))
        assertTrue(Achievements.earned(AchievementStats(0, 0, 0, 0.0, 0, null, 0, 0, 0, 0)).isEmpty())
    }
}

class SessionComposerTest {
    @Test
    fun mockInterviewHasRequestedLengthIntroFirstAndHrAtEnd() {
        val qs = (1..40).map { question("t$it", topic = "redfish") } +
            (1..10).map { question("s$it", topic = "redfish", type = com.interviewprep.core.model.QuestionType.SCENARIO) } +
            listOf(question("intro", topic = "self-intro", personal = true, q = "Tell me about yourself.", type = com.interviewprep.core.model.QuestionType.BEHAVIORAL)) +
            (1..5).map { question("hr$it", topic = "hr", personal = true, type = com.interviewprep.core.model.QuestionType.BEHAVIORAL, q = "Why should we hire you $it?") }
        val tracks = mapOf("redfish" to "api", "self-intro" to "career", "hr" to "career")
        val list = SessionComposer(Random(3)).mockInterview(qs, tracks, InterviewRole.BMC_OPENBMC, ExperienceLevel.SENIOR, 20)
        assertEquals(20, list.size)
        assertEquals("intro", list.first().id)
        assertEquals(20, list.map { it.id }.distinct().size)
        assertTrue(list.count { it.type == com.interviewprep.core.model.QuestionType.SCENARIO } >= 5)
    }

    @Test
    fun interviewReport() {
        val r = InterviewReporting.report(listOf(Triple("python", 9, 0.8), Triple("api", 5, 0.6)), mapOf("python" to "Python", "api" to "API"))
        assertEquals(70, r.technical)
        assertEquals(listOf("Python"), r.strengths)
        assertEquals(listOf("API"), r.improve)
    }
}

class CodeChecksTest {
    private val sol = "from collections import Counter\ndef duplicates(serials):\n    return sorted(s for s, n in Counter(serials).items() if n > 1)\n"

    @Test
    fun starterKeepsImportsAndSignature() {
        val s = CodeChecks.starter(sol, listOf("duplicates"))
        assertTrue(s.startsWith("from collections import Counter"))
        assertTrue(s.contains("def duplicates(serials):"))
        assertTrue(s.contains("pass"))
    }

    @Test
    fun missingEntryPointsDetected() {
        assertEquals(listOf("duplicates"), CodeChecks.missingEntryPoints("def dupes(x): pass", listOf("duplicates")))
        assertTrue(CodeChecks.missingEntryPoints(sol, listOf("duplicates")).isEmpty())
    }
}
