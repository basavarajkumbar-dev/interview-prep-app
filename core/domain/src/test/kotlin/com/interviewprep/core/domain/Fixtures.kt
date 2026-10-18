package com.interviewprep.core.domain

import com.interviewprep.core.model.Difficulty
import com.interviewprep.core.model.InterviewQuestion
import com.interviewprep.core.model.MatchPair
import com.interviewprep.core.model.QuestionType
import com.interviewprep.core.model.QuizItem
import com.interviewprep.core.model.QuizOrigin
import com.interviewprep.core.model.QuizType
import com.interviewprep.core.model.Scenario
import com.interviewprep.core.model.ScenarioStep
import com.interviewprep.core.model.SourceRef

val SRC = SourceRef("python", "11 Exception Handling", "11.18 Chapter assessment", 147)

fun quiz(
    id: String,
    type: QuizType = QuizType.MCQ,
    topic: String = "py-exceptions",
    difficulty: Difficulty = Difficulty.MEDIUM,
    options: List<String> = listOf("a", "b", "c", "d"),
    correct: List<Int> = listOf(1),
    answer: String? = null,
    acceptable: List<String> = emptyList(),
    pairs: List<MatchPair> = emptyList(),
) = QuizItem(id, topic, type, "prompt $id", null, options, correct, answer, acceptable, pairs, "because", difficulty, QuizOrigin.HANDBOOK, null, SRC)

fun question(
    id: String,
    topic: String = "redfish",
    answer: String = "POST credentials to the Sessions collection, receive X-Auth-Token and Location; DELETE the Location to log out.",
    keywords: List<String> = listOf("x-auth-token", "sessions", "delete", "location"),
    type: QuestionType = QuestionType.TECHNICAL,
    personal: Boolean = false,
    difficulty: Difficulty = Difficulty.MEDIUM,
    q: String = "How do Redfish sessions work?",
) = InterviewQuestion(id, topic, q, answer, listOf("How do you log out?"), null, null, null, keywords, listOf("BMC"),
    difficulty, type, personal, null, SRC, emptyList())

fun scenario() = Scenario(
    "s1", "redfish", "Redfish returns 500", "Redfish returns 500. How would you investigate?",
    listOf(
        ScenarioStep("Investigate", "Reproduce with curl to rule out the client. Check systemctl status bmcweb and journalctl -u bmcweb. Read the same property with busctl."),
        ScenarioStep("Causes", "backend daemon dead; bmcweb handler bug; resource exhaustion"),
    ),
    listOf("curl", "bmcweb", "journalctl", "busctl", "daemon"), false, Difficulty.HARD, listOf("BMC"), SRC,
)
