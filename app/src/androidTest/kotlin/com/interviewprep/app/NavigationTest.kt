package com.interviewprep.app

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test of the real app: imports bundled content, gets past onboarding and visits each
 * bottom-navigation tab. Run on an emulator with: ./gradlew :app:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun waitForText(text: String) =
        rule.waitUntil(timeoutMillis = 60_000) { rule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun bottomTabsNavigate() {
        // first launch imports the handbook content, then shows onboarding (or Home on later runs)
        rule.waitUntil(60_000) {
            rule.onAllNodes(hasText("Start studying")).fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodes(hasText("Today's goal")).fetchSemanticsNodes().isNotEmpty()
        }
        if (rule.onAllNodes(hasText("Start studying")).fetchSemanticsNodes().isNotEmpty()) {
            rule.onAllNodesWithText("Start studying").onFirst().performClick()
        }
        waitForText("Today's goal")
        for ((tab, expected) in listOf("Learn" to "Python Core", "Practice" to "Mock interview", "Quiz" to "Adaptive quiz", "Progress" to "INTERVIEW READINESS", "Home" to "Today's goal")) {
            rule.onAllNodesWithText(tab).onFirst().performClick()
            waitForText(expected)
        }
    }
}
