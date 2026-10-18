package com.interviewprep.core.model

/**
 * Learning content extracted from the handbooks. Every item keeps a [SourceRef] so the
 * app can show exactly where it came from.
 */
data class SourceRef(
    val book: String,
    val chapter: String,
    val section: String,
    val page: Int,
) {
    val label: String
        get() = buildString {
            append(chapter.ifBlank { "Front matter" })
            if (section.isNotBlank()) append(" · ").append(section)
            append(" · p.").append(page)
        }
}

data class Handbook(val id: String, val title: String, val shortTitle: String, val pages: Int)

data class Role(val id: String, val title: String)

data class Track(
    val id: String,
    val title: String,
    val emoji: String,
    val description: String,
    val order: Int,
    val roles: List<String>,
)

data class Topic(
    val id: String,
    val trackId: String,
    val title: String,
    val order: Int,
    val intro: String,
    val objectives: List<String>,
    val summary: List<String>,
    val keyPoints: List<String>,
    val commonMistakes: List<String>,
    val roles: List<String>,
    val sources: List<SourceRef>,
    /** How this topic relates to your resume (BMC/SSD prep handbook §2.7), if the handbook says. */
    val resumeTag: ResumeTag? = null,
    val resumeTagSource: SourceRef? = null,
)

enum class LessonKind { CONCEPT, PRACTICE, SOLUTIONS, SUMMARY, MISTAKES, BEST_PRACTICES }

data class LessonSummary(
    val id: String,
    val topicId: String,
    val title: String,
    val order: Int,
    val kind: LessonKind,
    val readMinutes: Int,
    val hasCode: Boolean,
    val source: SourceRef,
)

data class Lesson(
    val summary: LessonSummary,
    val body: String,
)

enum class Difficulty(val label: String, val rank: Int) {
    EASY("Easy", 0), MEDIUM("Medium", 1), HARD("Hard", 2), EXPERT("Expert", 3);

    fun harder(): Difficulty = entries.getOrElse(rank + 1) { EXPERT }
    fun easier(): Difficulty = entries.getOrElse(rank - 1) { EASY }

    companion object {
        fun parse(s: String?): Difficulty = entries.firstOrNull { it.name == s } ?: MEDIUM
    }
}

enum class QuestionType(val label: String) {
    TECHNICAL("Technical"), SCENARIO("Scenario"), CODING("Coding"), DEBUGGING("Debugging"), BEHAVIORAL("Behavioral");

    companion object {
        fun parse(s: String?): QuestionType = entries.firstOrNull { it.name == s } ?: TECHNICAL
    }
}

/** Tags the BMC/SSD resume handbook puts on every area. */
enum class ResumeTag(val label: String, val advice: String) {
    ON_RESUME("On your resume", "You must be able to defend this in depth, with examples."),
    IMPLIED("Implied by your role", "Not written on your resume, but expected of anyone in your role. Revise it."),
    NOT_ON_RESUME("Not on your resume", "Learn the basics and answer honestly. Don't claim hands-on use."),
    ;

    companion object {
        fun parse(s: String?): ResumeTag? = entries.firstOrNull { it.name == s }
    }
}

data class InterviewQuestion(
    val id: String,
    val topicId: String,
    val question: String,
    val answer: String,
    val followUps: List<String>,
    val commonWrong: String?,
    val why: String?,
    val keyPoints: String?,
    val keywords: List<String>,
    val roles: List<String>,
    val difficulty: Difficulty,
    val type: QuestionType,
    val personal: Boolean,
    val resumeTag: ResumeTag?,
    val source: SourceRef,
    val altSources: List<SourceRef>,
)

enum class CardOrigin(val label: String) {
    ASSESSMENT("Chapter assessment"), REVISION("Revision flashcard"), QUICK_REVISION("Quick revision"), GLOSSARY("Glossary");

    companion object {
        fun parse(s: String?): CardOrigin = entries.firstOrNull { it.name == s } ?: REVISION
    }
}

data class Flashcard(
    val id: String,
    val topicId: String,
    val front: String,
    val back: String,
    val origin: CardOrigin,
    val keywords: List<String>,
    val source: SourceRef,
)

enum class QuizType(val label: String) {
    MCQ("Multiple choice"),
    TRUE_FALSE("True / False"),
    MULTI("Multiple answer"),
    FILL_BLANK("Fill in the blank"),
    CODE_OUTPUT("Code output"),
    SCENARIO("Scenario"),
    DEBUGGING("Debugging"),
    MATCH("Match the following"),
    ;

    companion object {
        fun parse(s: String?): QuizType = entries.firstOrNull { it.name == s } ?: MCQ
    }
}

/** HANDBOOK = question and answer both come from the handbook text.
 *  DERIVED = answer comes from the handbook, wording/distractors were written for the app. */
enum class QuizOrigin(val label: String) {
    HANDBOOK("Source-backed"), DERIVED("Source-backed answer · app-written options");

    companion object {
        fun parse(s: String?): QuizOrigin = entries.firstOrNull { it.name == s } ?: HANDBOOK
    }
}

data class MatchPair(val left: String, val right: String)

data class QuizItem(
    val id: String,
    val topicId: String,
    val type: QuizType,
    val prompt: String,
    val code: String?,
    val options: List<String>,
    val correct: List<Int>,
    val answerText: String?,
    val acceptable: List<String>,
    val pairs: List<MatchPair>,
    val explanation: String,
    val difficulty: Difficulty,
    val origin: QuizOrigin,
    val evidenceQuote: String?,
    val source: SourceRef,
)

data class ScenarioStep(val label: String, val text: String)

data class Scenario(
    val id: String,
    val topicId: String,
    val title: String,
    val prompt: String,
    val steps: List<ScenarioStep>,
    val keywords: List<String>,
    val concept: Boolean,
    val difficulty: Difficulty,
    val roles: List<String>,
    val source: SourceRef,
)

data class CodingChallenge(
    val id: String,
    val code: String,
    val set: String,
    val topicId: String,
    val title: String,
    val difficulty: Difficulty,
    val statement: String,
    val examples: List<String>,
    val hint: String?,
    val solution: String,
    val tests: List<String>,
    val entryPoints: List<String>,
    val complexity: String?,
    val notes: String?,
    val verified: Boolean,
    val source: SourceRef,
)

data class GlossaryEntry(val term: String, val definition: String, val topicId: String, val source: SourceRef)

enum class SearchKind(val label: String) {
    TOPIC("Topics"), LESSON("Lessons"), QUESTION("Questions"), FLASHCARD("Flashcards"),
    SCENARIO("Scenarios"), CODING("Coding"), GLOSSARY("Glossary");

    companion object {
        fun parse(s: String?): SearchKind = entries.firstOrNull { it.name == s } ?: LESSON
    }
}

data class SearchHit(val kind: SearchKind, val refId: String, val title: String, val snippet: String, val topicId: String?)
