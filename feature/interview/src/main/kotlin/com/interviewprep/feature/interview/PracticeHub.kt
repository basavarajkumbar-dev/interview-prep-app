package com.interviewprep.feature.interview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.interviewprep.core.designsystem.AppCard
import com.interviewprep.core.designsystem.Routes
import com.interviewprep.core.designsystem.ScreenTopBar
import com.interviewprep.core.designsystem.SectionTitle

@Composable
private fun HubCard(emoji: String, title: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    AppCard(modifier, onClick = onClick) {
        Text("$emoji  $title", style = MaterialTheme.typography.titleSmall)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PracticeHubScreen(navigate: (String) -> Unit) {
    Scaffold(topBar = { ScreenTopBar("Practice") }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { SectionTitle("Interview") }
            item { HubCard("🎤", "Mock interview", "Role-based, one question at a time, with a final report") { navigate(Routes.MOCK_SETUP) } }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HubCard("💬", "Question bank", "1,200+ handbook Q&As", Modifier.weight(1f)) { navigate(Routes.questions()) }
                    HubCard("🧑‍💼", "From my resume", "Defend your story", Modifier.weight(1f)) { navigate(Routes.questions(personal = true)) }
                }
            }
            item { HubCard("🔍", "Troubleshooting scenarios", "Answer, get scored on approach and sequence, see the ideal flow") { navigate(Routes.SCENARIOS) } }
            item { SectionTitle("Skills") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HubCard("💻", "Coding practice", "105 problems with tests", Modifier.weight(1f)) { navigate(Routes.CODING) }
                    HubCard("⚡", "15-min revision", "Before work or an interview", Modifier.weight(1f)) { navigate(Routes.REVISION) }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HubCard("🔴", "Weak topics", "What to revise next", Modifier.weight(1f)) { navigate(Routes.WEAK) }
                    HubCard("⭐", "Bookmarks", "Saved items", Modifier.weight(1f)) { navigate(Routes.BOOKMARKS) }
                }
            }
            item { HubCard("🗓️", "Today's study plan", "Adaptive plan up to 90 minutes") { navigate(Routes.PLAN) } }
        }
    }
}

fun NavGraphBuilder.interviewGraph(nav: NavHostController) {
    composable(Routes.PRACTICE) { PracticeHubScreen(navigate = { nav.navigate(it) }) }
    composable(
        Routes.QUESTIONS,
        arguments = listOf(
            navArgument("topic") { type = NavType.StringType; defaultValue = "" },
            navArgument("personal") { type = NavType.BoolType; defaultValue = false },
        ),
    ) { QuestionBankScreen(navigate = { nav.navigate(it) }, onBack = { nav.popBackStack() }) }
    composable(Routes.QUESTION, arguments = listOf(navArgument("questionId") { type = NavType.StringType })) {
        QuestionScreen(navigate = { nav.navigate(it) }, onBack = { nav.popBackStack() })
    }
    composable(Routes.MOCK_SETUP) { MockSetupScreen(navigate = { nav.navigate(it) }, onBack = { nav.popBackStack() }) }
    composable(Routes.MOCK_SESSION, arguments = listOf(navArgument("sessionId") { type = NavType.StringType })) {
        MockSessionScreen(
            navigate = { nav.navigate(it) },
            replace = { route -> nav.navigate(route) { popUpTo(Routes.MOCK_SETUP) { inclusive = true } } },
            onBack = { nav.popBackStack() },
        )
    }
    composable(Routes.MOCK_REPORT, arguments = listOf(navArgument("sessionId") { type = NavType.StringType })) {
        MockReportScreen(navigate = { nav.navigate(it) }, onBack = { nav.popBackStack() })
    }
    composable(Routes.SCENARIOS) { ScenarioListScreen(navigate = { nav.navigate(it) }, onBack = { nav.popBackStack() }) }
    composable(Routes.SCENARIO, arguments = listOf(navArgument("scenarioId") { type = NavType.StringType })) {
        ScenarioScreen(navigate = { nav.navigate(it) }, onBack = { nav.popBackStack() })
    }
}
