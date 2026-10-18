package com.interviewprep.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.interviewprep.core.data.content.ContentImporter
import com.interviewprep.core.data.content.ContentState
import com.interviewprep.core.data.repo.ContentRepository
import com.interviewprep.core.data.settings.SettingsStore
import com.interviewprep.core.designsystem.AppTheme
import com.interviewprep.core.designsystem.ErrorState
import com.interviewprep.core.designsystem.LoadingState
import com.interviewprep.core.designsystem.Routes
import com.interviewprep.core.model.UserSettings
import com.interviewprep.feature.coding.codingGraph
import com.interviewprep.feature.flashcards.flashcardsGraph
import com.interviewprep.feature.home.homeGraph
import com.interviewprep.feature.interview.interviewGraph
import com.interviewprep.feature.learn.learnGraph
import com.interviewprep.feature.progress.progressGraph
import com.interviewprep.feature.quiz.quizGraph
import com.interviewprep.feature.settings.OnboardingScreen
import com.interviewprep.feature.settings.settingsGraph
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val importer: ContentImporter,
    private val content: ContentRepository,
    settings: SettingsStore,
) : ViewModel() {
    val contentState: StateFlow<ContentState> = importer.state
    val settings: StateFlow<UserSettings?> = settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init { importContent() }

    fun importContent() = viewModelScope.launch {
        importer.ensureImported()
        content.invalidateCaches()
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { Root() }
    }
}

@Composable
private fun Root(vm: MainViewModel = hiltViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val content by vm.contentState.collectAsStateWithLifecycle()
    val s = settings
    AppTheme(themeMode = s?.themeMode ?: com.interviewprep.core.model.ThemeMode.SYSTEM) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when {
                s == null -> LoadingState()
                content is ContentState.Failed -> ErrorState(
                    "Couldn't load the handbook library: ${(content as ContentState.Failed).message}",
                    onRetry = { vm.importContent() },
                )
                content !is ContentState.Ready -> LoadingState(
                    (content as? ContentState.Importing)?.step ?: "Preparing your library…",
                )
                else -> AppNav(startOnboarding = !s.onboarded)
            }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val TABS = listOf(
    Tab(Routes.HOME, "Home", Icons.Outlined.Home, Icons.Filled.Home),
    Tab(Routes.LEARN, "Learn", Icons.Outlined.MenuBook, Icons.Filled.MenuBook),
    Tab(Routes.PRACTICE, "Practice", Icons.Outlined.TrackChanges, Icons.Filled.TrackChanges),
    Tab(Routes.QUIZ, "Quiz", Icons.Outlined.Psychology, Icons.Filled.Psychology),
    Tab(Routes.PROGRESS, "Progress", Icons.Outlined.BarChart, Icons.Filled.BarChart),
)

private const val ONBOARDING = "onboarding"

@Composable
private fun AppNav(startOnboarding: Boolean) {
    val nav = rememberNavController()
    // keep the graph stable when onboarding completes (settings change would otherwise rebuild it)
    val start = remember { if (startOnboarding) ONBOARDING else Routes.HOME }
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination
    val showBar = TABS.any { t -> current?.hierarchy?.any { it.route == t.route } == true }
    Scaffold(
        bottomBar = {
            if (showBar) NavigationBar {
                TABS.forEach { tab ->
                    val selected = current?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { pad ->
        // Screens have their own top bars; only the bottom bar inset is applied here.
        Box(Modifier.padding(bottom = pad.calculateBottomPadding()).consumeWindowInsets(pad)) {
            NavHost(nav, startDestination = start) {
                composable(ONBOARDING) {
                    OnboardingScreen(onDone = {
                        nav.navigate(Routes.HOME) { popUpTo(ONBOARDING) { inclusive = true } }
                    })
                }
                homeGraph(nav)
                learnGraph(nav)
                quizGraph(nav)
                flashcardsGraph(nav)
                interviewGraph(nav)
                codingGraph(nav)
                progressGraph(nav)
                settingsGraph(nav)
            }
        }
    }
}
