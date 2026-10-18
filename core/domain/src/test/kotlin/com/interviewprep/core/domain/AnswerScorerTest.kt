package com.interviewprep.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerScorerTest {
    @Test
    fun emptyAnswerScoresZero() {
        val e = AnswerScorer.evaluate("", question("q"))
        assertEquals(0, e.score10)
        assertEquals(4, e.missed.size)
    }

    @Test
    fun goodAnswerScoresHigherThanVagueAnswer() {
        val good = AnswerScorer.evaluate(
            "First I POST the credentials to the SessionService Sessions collection. The response gives an X-Auth-Token header " +
                "and a Location. I send the token on every request, and at the end I DELETE the Location to log out, for example in a fixture teardown.",
            question("q"),
        )
        val vague = AnswerScorer.evaluate("You log in and then use it.", question("q"))
        assertTrue("good=${good.score10} vague=${vague.score10}", good.score10 >= 7)
        assertTrue(vague.score10 <= 3)
        assertTrue(good.matched.contains("x-auth-token"))
    }

    @Test
    fun stemmingMatchesInflections() {
        val (m, _) = AnswerScorer.keywordHits("the BMC rebooted twice", listOf("reboot"))
        assertEquals(listOf("reboot"), m)
    }

    @Test
    fun scenarioRewardsOrderedEvidenceFirstAnswer() {
        val s = scenario()
        val ordered = AnswerScorer.evaluateScenario(
            "First I reproduce it with curl to rule out the client. Then I check the bmcweb service and journalctl -u bmcweb for crashes. " +
                "Then I read the same property with busctl to see whether the daemon answers. I capture the logs as evidence before any reboot.",
            s,
        )
        val shuffled = AnswerScorer.evaluateScenario("busctl. journalctl. curl.", s)
        assertTrue(ordered.overall10 > shuffled.overall10)
        assertTrue(ordered.sequence >= 0.99)
    }

    @Test
    fun longestIncreasingSubsequence() {
        assertEquals(3, AnswerScorer.longestIncreasing(listOf(5, 1, 2, 9, 3)).coerceAtMost(4))
        assertEquals(4, AnswerScorer.longestIncreasing(listOf(1, 2, 3, 4)))
    }
}
