package com.interviewprep.core.domain

/** Result of running the user's code against the handbook's test asserts. */
data class TestOutcome(val test: String, val passed: Boolean, val message: String?)

data class RunResult(
    val executed: Boolean, // false if no interpreter is available
    val compileError: String?, // syntax error in the user's code
    val outcomes: List<TestOutcome>,
    val stdout: String,
    val timedOut: Boolean = false,
) {
    val passed get() = outcomes.count { it.passed }
    val total get() = outcomes.size
}

/** Runs Python code on-device. Implemented with Chaquopy in :python-runtime. */
interface CodeRunner {
    val available: Boolean
    val description: String
    suspend fun run(userCode: String, tests: List<String>, timeoutMs: Long = 8_000): RunResult
}

/** Fallback when no interpreter is bundled: the UI switches to compare-with-reference mode. */
class ReferenceOnlyRunner : CodeRunner {
    override val available = false
    override val description = "Code execution not available in this build: compare with the reference solution."
    override suspend fun run(userCode: String, tests: List<String>, timeoutMs: Long) =
        RunResult(executed = false, compileError = null, outcomes = emptyList(), stdout = "")
}

/** Static checks used before/without execution: does the code define what the tests call? */
object CodeChecks {
    fun missingEntryPoints(code: String, entryPoints: List<String>): List<String> =
        entryPoints.filter { name -> !Regex("^\\s*(def|class)\\s+${Regex.escape(name)}\\b", RegexOption.MULTILINE).containsMatchIn(code) }

    /** Starter code: the function signatures from the reference solution with the body replaced. */
    fun starter(solution: String, entryPoints: List<String>): String {
        val lines = solution.lines()
        val out = mutableListOf<String>()
        lines.filter { it.startsWith("import ") || it.startsWith("from ") }.forEach { out += it }
        if (out.isNotEmpty()) out += ""
        for (name in entryPoints) {
            val sig = lines.firstOrNull { Regex("^(def|class)\\s+${Regex.escape(name)}\\b").containsMatchIn(it) } ?: continue
            out += sig
            out += if (sig.startsWith("class")) "    pass  # TODO: implement" else "    # TODO: implement\n    pass"
            out += ""
        }
        return out.joinToString("\n").trimEnd() + "\n"
    }
}
