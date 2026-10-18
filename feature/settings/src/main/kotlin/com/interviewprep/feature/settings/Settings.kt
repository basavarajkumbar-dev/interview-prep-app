package com.interviewprep.feature.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.interviewprep.core.data.content.ContentImporter
import com.interviewprep.core.data.repo.ContentRepository
import com.interviewprep.core.data.repo.ProgressRepository
import com.interviewprep.core.data.settings.SettingsStore
import com.interviewprep.core.designsystem.AppCard
import com.interviewprep.core.designsystem.Handbooks
import com.interviewprep.core.designsystem.ListRow
import com.interviewprep.core.designsystem.PrimaryAction
import com.interviewprep.core.designsystem.Routes
import com.interviewprep.core.designsystem.ScreenTopBar
import com.interviewprep.core.designsystem.SectionTitle
import com.interviewprep.core.designsystem.Segmented
import com.interviewprep.core.domain.StudyPlanner
import com.interviewprep.core.model.Difficulty
import com.interviewprep.core.model.Handbook
import com.interviewprep.core.model.ThemeMode
import com.interviewprep.core.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

val ALL_ROLES = listOf(
    "PY" to "Python Automation Engineer", "TA" to "Test Automation Engineer", "SR" to "Senior Test Automation Engineer",
    "EMB" to "Embedded Test / Validation Engineer", "SW" to "Software Test Engineer", "BMC" to "BMC/OpenBMC Testing",
    "SSD" to "SSD/NVMe Validation",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsStore,
    private val progress: ProgressRepository,
    private val importer: ContentImporter,
    private val content: ContentRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    val settings: StateFlow<UserSettings?> = store.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val handbooks: StateFlow<List<Handbook>> = content.handbooks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTheme(t: ThemeMode) = viewModelScope.launch { store.setTheme(t) }
    fun setGoal(m: Int) = viewModelScope.launch { store.setDailyGoal(m) }
    fun setDifficulty(d: Difficulty?) = viewModelScope.launch { store.setQuizDifficulty(d) }
    fun setName(n: String) = viewModelScope.launch { store.setName(n) }
    fun toggleRole(r: String) = viewModelScope.launch {
        val cur = store.current().targetRoles
        store.setTargetRoles(if (r in cur) cur - r else cur + r)
    }

    fun reset(done: () -> Unit) = viewModelScope.launch { progress.resetAll(); done() }

    fun export(uri: Uri, done: (Boolean) -> Unit) = viewModelScope.launch {
        val ok = runCatching {
            val json = progress.exportJson()
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } ?: error("Couldn't open file")
            }
        }.isSuccess
        done(ok)
    }

    fun reimport(done: () -> Unit) = viewModelScope.launch { importer.ensureImported(force = true); content.invalidateCaches(); done() }

    fun finishOnboarding(name: String, roles: Set<String>, minutes: Int) = viewModelScope.launch {
        store.setName(name); store.setTargetRoles(roles); store.setDailyGoal(minutes); store.setOnboarded(true)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(navigate: (String) -> Unit, onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmReset by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.export(uri) { ok -> scope.launch { snackbar.showSnackbar(if (ok) "Progress exported" else "Export failed") } }
    }
    Scaffold(topBar = { ScreenTopBar("Settings", onBack) }, snackbarHost = { SnackbarHost(snackbar) }) { pad ->
        val st = s ?: return@Scaffold
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { SectionTitle("Profile") }
            item {
                var name by rememberSaveable(st.name) { mutableStateOf(st.name) }
                OutlinedTextField(value = name, onValueChange = { name = it; vm.setName(it) }, label = { Text("Your name (for greetings)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            item { Text("Target roles", style = MaterialTheme.typography.labelLarge) }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ALL_ROLES.forEach { (id, title) -> FilterChip(selected = id in st.targetRoles, onClick = { vm.toggleRole(id) }, label = { Text(title) }) }
                }
            }
            item { SectionTitle("Appearance") }
            item { Segmented(ThemeMode.entries, st.themeMode, { when (it) { ThemeMode.SYSTEM -> "System"; ThemeMode.LIGHT -> "Light"; ThemeMode.DARK -> "Dark" } }, vm::setTheme) }
            item { SectionTitle("Study") }
            item { Text("Daily study goal", style = MaterialTheme.typography.labelLarge) }
            item { Segmented(StudyPlanner.OPTIONS, st.dailyGoalMinutes, { "$it min" }, vm::setGoal) }
            item { Text("Quiz difficulty", style = MaterialTheme.typography.labelLarge) }
            item {
                val opts: List<Difficulty?> = listOf(null) + Difficulty.entries
                Segmented(opts, st.quizDifficulty, { it?.label ?: "Adaptive" }, vm::setDifficulty)
            }
            item {
                AppCard {
                    Text("Notifications", style = MaterialTheme.typography.titleSmall)
                    Text("This version sends no notifications and asks for no notification permission. Use the streak and daily goal on Home as your reminder.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            item { SectionTitle("Data") }
            item { ListRow("Export progress", "Save a JSON backup of your progress", "📤", onClick = { exportLauncher.launch("interview-prep-progress.json") }) }
            item { ListRow("Reload handbook content", "Rebuild the library from the bundled content (progress is kept)", "🔄", onClick = { vm.reimport { scope.launch { snackbar.showSnackbar("Content reloaded") } } }) }
            item { ListRow("Reset progress", "Delete all progress, attempts, bookmarks and XP", "🗑️", onClick = { confirmReset = true }) }
            item { SectionTitle("About") }
            item { ListRow("Source handbooks", "The five PDFs this app was built from", "📚", onClick = { navigate(Routes.SOURCES) }) }
            item {
                AppCard {
                    Text("Interview Prep v1.0", style = MaterialTheme.typography.titleSmall)
                    Text("All learning content is extracted from your handbooks and works offline. Every lesson, question and card links to its source page. " +
                        "Quiz options marked “written by app” have handbook-backed answers with app-written distractors. AI features are not included in this version.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset all progress?") },
            text = { Text("This permanently deletes lesson progress, quiz history, flashcard schedules, interview answers, bookmarks and XP. Consider exporting first.") },
            confirmButton = { TextButton(onClick = { confirmReset = false; vm.reset { scope.launch { snackbar.showSnackbar("Progress reset") } } }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun SourcesScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val books by vm.handbooks.collectAsStateWithLifecycle()
    Scaffold(topBar = { ScreenTopBar("Source handbooks", onBack) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("The PDFs are the source of truth. Content was extracted by the content pipeline (content-pipeline/ in the project), " +
                    "code outputs and coding solutions were executed to verify them, and anything not covered is labelled “Not covered in the uploaded handbook.”",
                    style = MaterialTheme.typography.bodyMedium)
            }
            items(books, key = { it.id }) { b ->
                AppCard {
                    Text(b.title, style = MaterialTheme.typography.titleSmall)
                    Text("${b.pages} pages · shown as “${Handbooks.short(b.id)}” in source links", style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                AppCard {
                    Text("Known gaps", style = MaterialTheme.typography.titleSmall)
                    Text("• Selenium: the handbook teaches Playwright (Python Ch. 29); Selenium isn't covered in depth.\n" +
                        "• SPI: only mentioned in passing.\n• System design: no dedicated chapter; see Automation Framework Architecture.\n" +
                        "• SSD/NVMe has one detailed source (BMC/SSD prep §9), so performance testing and telemetry are brief.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// ================================================================== Onboarding
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(onDone: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    var name by rememberSaveable { mutableStateOf("") }
    var roles by rememberSaveable { mutableStateOf(setOf("TA", "EMB", "BMC", "SSD")) }
    var minutes by rememberSaveable { mutableStateOf(60) }
    Scaffold { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Column {
                    Text("👋", style = MaterialTheme.typography.displaySmall)
                    Text("Welcome to Interview Prep", style = MaterialTheme.typography.headlineMedium)
                    Text("Your five handbooks, turned into lessons, quizzes, flashcards, scenarios, coding practice and mock interviews.",
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("What should we call you?") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item { Text("Which roles are you preparing for?", style = MaterialTheme.typography.titleSmall) }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ALL_ROLES.forEach { (id, title) -> FilterChip(selected = id in roles, onClick = { roles = if (id in roles) roles - id else roles + id }, label = { Text(title) }) }
                }
            }
            item { Text("Daily study time", style = MaterialTheme.typography.titleSmall) }
            item { Segmented(StudyPlanner.OPTIONS, minutes, { "$it min" }, { minutes = it }) }
            item { Spacer(Modifier.height(8.dp)); PrimaryAction("Start studying", { vm.finishOnboarding(name, roles, minutes); onDone() }) }
        }
    }
}

fun NavGraphBuilder.settingsGraph(nav: NavHostController) {
    composable(Routes.SETTINGS) { SettingsScreen(navigate = { nav.navigate(it) }, onBack = { nav.popBackStack() }) }
    composable(Routes.SOURCES) { SourcesScreen(onBack = { nav.popBackStack() }) }
}
