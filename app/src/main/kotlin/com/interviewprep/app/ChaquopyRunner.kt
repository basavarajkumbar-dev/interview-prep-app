package com.interviewprep.app

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.interviewprep.core.domain.CodeRunner
import com.interviewprep.core.domain.RunResult
import com.interviewprep.core.domain.TestOutcome
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Singleton

/**
 * Runs the user's Python on-device with Chaquopy (CPython embedded in the APK) using
 * src/main/python/runner.py. One run at a time; infinite loops are stopped by a trace-based
 * time limit inside the runner.
 */
class ChaquopyRunner(private val context: Context) : CodeRunner {
    private val lock = Mutex()
    override val available = true
    override val description = "Runs Python on your phone (offline) against the handbook's tests."

    override suspend fun run(userCode: String, tests: List<String>, timeoutMs: Long): RunResult = lock.withLock {
        withContext(Dispatchers.Default) {
            if (!Python.isStarted()) Python.start(AndroidPlatform(context))
            val module = Python.getInstance().getModule("runner")
            val json = module.callAttr("run", userCode, JSONArray(tests).toString(), timeoutMs / 1000.0).toString()
            parse(json)
        }
    }

    private fun parse(json: String): RunResult {
        val o = JSONObject(json)
        val arr = o.optJSONArray("outcomes") ?: JSONArray()
        val outcomes = (0 until arr.length()).map { i ->
            val t = arr.getJSONObject(i)
            TestOutcome(t.getString("test"), t.getBoolean("passed"), if (t.isNull("message")) null else t.getString("message"))
        }
        return RunResult(
            executed = true,
            compileError = if (o.isNull("compileError")) null else o.getString("compileError"),
            outcomes = outcomes,
            stdout = o.optString("stdout"),
            timedOut = o.optBoolean("timedOut"),
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object CodeRunnerModule {
    @Provides
    @Singleton
    fun codeRunner(@ApplicationContext context: Context): CodeRunner = ChaquopyRunner(context)
    // Without Chaquopy: return com.interviewprep.core.domain.ReferenceOnlyRunner()
}
