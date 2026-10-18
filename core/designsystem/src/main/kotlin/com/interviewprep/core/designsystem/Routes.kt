package com.interviewprep.core.designsystem

import android.net.Uri

/** All navigation routes, shared by feature modules so any screen can link to any other. */
object Routes {
    const val HOME = "home"
    const val LEARN = "learn"
    const val PRACTICE = "practice"
    const val QUIZ = "quiz"
    const val PROGRESS = "progress"

    const val TOPIC = "topic/{topicId}"
    fun topic(id: String) = "topic/${Uri.encode(id)}"
    const val LESSON = "lesson/{lessonId}"
    fun lesson(id: String) = "lesson/${Uri.encode(id)}"
    const val SEARCH = "search"
    const val BOOKMARKS = "bookmarks"

    const val QUESTIONS = "questions?topic={topic}&personal={personal}"
    fun questions(topic: String? = null, personal: Boolean = false) =
        "questions?topic=${Uri.encode(topic.orEmpty())}&personal=$personal"
    const val QUESTION = "question/{questionId}"
    fun question(id: String) = "question/${Uri.encode(id)}"

    const val MOCK_SETUP = "mock"
    const val MOCK_SESSION = "mock/session/{sessionId}"
    fun mockSession(id: String) = "mock/session/${Uri.encode(id)}"
    const val MOCK_REPORT = "mock/report/{sessionId}"
    fun mockReport(id: String) = "mock/report/${Uri.encode(id)}"

    const val SCENARIOS = "scenarios"
    const val SCENARIO = "scenario/{scenarioId}"
    fun scenario(id: String) = "scenario/${Uri.encode(id)}"

    const val CODING = "coding"
    const val CODING_CHALLENGE = "coding/{challengeId}"
    fun codingChallenge(id: String) = "coding/${Uri.encode(id)}"

    /** mode: ADAPTIVE, TOPIC, DAILY, WEAK, TRACK */
    const val QUIZ_RUN = "quiz/run?mode={mode}&topic={topic}"
    fun quizRun(mode: String, topic: String? = null) = "quiz/run?mode=$mode&topic=${Uri.encode(topic.orEmpty())}"

    const val FLASHCARDS = "flashcards?topic={topic}"
    fun flashcards(topic: String? = null) = "flashcards?topic=${Uri.encode(topic.orEmpty())}"

    const val REVISION = "revision"
    const val WEAK = "weak"
    const val ACHIEVEMENTS = "achievements"
    const val PLAN = "plan"
    const val SETTINGS = "settings"
    const val SOURCES = "sources"
}
