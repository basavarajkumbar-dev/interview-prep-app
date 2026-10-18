package com.interviewprep.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.interviewprep.core.model.Difficulty
import com.interviewprep.core.model.ExplanationMode
import com.interviewprep.core.model.ThemeMode
import com.interviewprep.core.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsStore @Inject constructor(@ApplicationContext private val context: Context) {
    private object K {
        val name = stringPreferencesKey("name")
        val theme = stringPreferencesKey("theme")
        val goal = intPreferencesKey("daily_goal_minutes")
        val difficulty = stringPreferencesKey("quiz_difficulty")
        val roles = stringSetPreferencesKey("target_roles")
        val onboarded = booleanPreferencesKey("onboarded")
        val explanation = stringPreferencesKey("explanation_mode")
        val contentVersion = intPreferencesKey("content_version")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { p ->
        UserSettings(
            name = p[K.name].orEmpty(),
            themeMode = ThemeMode.entries.firstOrNull { it.name == p[K.theme] } ?: ThemeMode.SYSTEM,
            dailyGoalMinutes = (p[K.goal] ?: 60).coerceIn(15, 90),
            quizDifficulty = p[K.difficulty]?.let { d -> Difficulty.entries.firstOrNull { it.name == d } },
            targetRoles = p[K.roles] ?: emptySet(),
            onboarded = p[K.onboarded] ?: false,
            explanationMode = ExplanationMode.entries.firstOrNull { it.name == p[K.explanation] } ?: ExplanationMode.SIMPLE,
        )
    }

    suspend fun current(): UserSettings = settings.first()

    suspend fun setName(v: String) = context.dataStore.edit { it[K.name] = v.trim().take(40) }
    suspend fun setTheme(v: ThemeMode) = context.dataStore.edit { it[K.theme] = v.name }
    suspend fun setDailyGoal(v: Int) = context.dataStore.edit { it[K.goal] = v.coerceIn(15, 90) }
    suspend fun setQuizDifficulty(v: Difficulty?) = context.dataStore.edit {
        if (v == null) it.remove(K.difficulty) else it[K.difficulty] = v.name
    }
    suspend fun setTargetRoles(v: Set<String>) = context.dataStore.edit { it[K.roles] = v }
    suspend fun setOnboarded(v: Boolean) = context.dataStore.edit { it[K.onboarded] = v }
    suspend fun setExplanationMode(v: ExplanationMode) = context.dataStore.edit { it[K.explanation] = v.name }

    suspend fun contentVersion(): Int = context.dataStore.data.first()[K.contentVersion] ?: -1
    suspend fun setContentVersion(v: Int) = context.dataStore.edit { it[K.contentVersion] = v }
}
