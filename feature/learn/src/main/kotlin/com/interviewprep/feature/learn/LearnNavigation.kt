package com.interviewprep.feature.learn

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.interviewprep.core.designsystem.Routes

fun NavGraphBuilder.learnGraph(nav: NavHostController) {
    composable(Routes.LEARN) { LearnScreen(navigate = { nav.navigate(it) }) }
    composable(Routes.TOPIC, arguments = listOf(navArgument("topicId") { type = NavType.StringType })) {
        TopicScreen(navigate = { nav.navigate(it) }, onBack = { nav.popBackStack() })
    }
    composable(Routes.LESSON, arguments = listOf(navArgument("lessonId") { type = NavType.StringType })) {
        LessonScreen(
            navigate = { nav.navigate(it) },
            replace = { route -> nav.navigate(route) { popUpTo(Routes.LESSON) { inclusive = true } } },
            onBack = { nav.popBackStack() },
        )
    }
    composable(Routes.SEARCH) { SearchScreen(navigate = { nav.navigate(it) }, onBack = { nav.popBackStack() }) }
    composable(Routes.BOOKMARKS) { BookmarksScreen(navigate = { nav.navigate(it) }, onBack = { nav.popBackStack() }) }
}
