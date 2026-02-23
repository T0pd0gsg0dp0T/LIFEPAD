package com.lifepad.app

import android.Manifest
import android.os.SystemClock
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.rule.GrantPermissionRule
import androidx.test.espresso.Espresso.pressBack
import org.junit.Rule
import org.junit.Test

class AppSmokeTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.POST_NOTIFICATIONS
    )

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @OptIn(ExperimentalTestApi::class)
    private fun waitForTag(tag: String, timeoutMs: Long = 10000) {
        val start = SystemClock.uptimeMillis()
        while (SystemClock.uptimeMillis() - start < timeoutMs) {
            try {
                composeRule.waitForIdle()
                if (composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) {
                    return
                }
            } catch (_: IllegalStateException) {
                // Compose hierarchy not ready yet.
            }
            SystemClock.sleep(200)
        }
        throw AssertionError("Timeout waiting for tag: $tag")
    }

    private fun nodeCount(tag: String): Int {
        return try {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size
        } catch (_: IllegalStateException) {
            0
        }
    }

    @OptIn(ExperimentalTestApi::class)
    private fun waitForAnyTag(tags: List<String>, timeoutMs: Long = 10000): String {
        val start = SystemClock.uptimeMillis()
        while (SystemClock.uptimeMillis() - start < timeoutMs) {
            try {
                composeRule.waitForIdle()
                tags.firstOrNull { tag ->
                    composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
                }?.let { return it }
            } catch (_: IllegalStateException) {
                // Compose hierarchy not ready yet.
            }
            SystemClock.sleep(200)
        }
        throw AssertionError("Timeout waiting for tags: ${tags.joinToString()}")
    }

    private fun ensureUnlocked() {
        composeRule.activityRule.scenario.onActivity { }
        val firstScreen = waitForAnyTag(listOf("screen_dashboard", "screen_pin"), timeoutMs = 20000)
        if (firstScreen == "screen_pin") {
            if (nodeCount("pin_skip") > 0) {
                composeRule.onNodeWithTag("pin_skip").performClick()
                waitForTag("screen_dashboard", timeoutMs = 20000)
            } else {
                enterPin("1234")
                if (nodeCount("pin_next") > 0) {
                    composeRule.onNodeWithTag("pin_next").performClick()
                    enterPin("1234")
                    composeRule.onNodeWithTag("pin_confirm").performClick()
                } else if (nodeCount("pin_unlock") > 0) {
                    composeRule.onNodeWithTag("pin_unlock").performClick()
                } else {
                    // Fallback to text buttons if tags aren't available yet.
                    clickIfExists("Next")
                    clickIfExists("Confirm")
                    clickIfExists("Unlock")
                }
                waitForTag("screen_dashboard", timeoutMs = 20000)
            }
        }
    }

    private fun enterPin(pin: String) {
        pin.forEach { digit ->
            composeRule.onNodeWithText(digit.toString(), useUnmergedTree = true).performClick()
        }
    }

    private fun clickIfExists(text: String) {
        if (composeRule.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText(text, useUnmergedTree = true).performClick()
        }
    }

    private fun tryOpenReminder(tag: String) {
        if (nodeCount(tag) > 0) {
            composeRule.onNodeWithTag(tag).performClick()
            try {
                waitForTag("reminder_dialog", timeoutMs = 3000)
                composeRule.onNodeWithTag("reminder_cancel").performClick()
            } catch (_: AssertionError) {
                // Permission dialogs can block the reminder UI in tests.
            }
        }
    }

    @Test
    fun smokeTest_coreNavigationAndCreateFlows() {
        ensureUnlocked()
        waitForTag("screen_dashboard")
        composeRule.onNodeWithTag("screen_dashboard").assertIsDisplayed()

        // Notes: create two notes, one with wikilink + hashtag
        composeRule.onNodeWithTag("fab_add").performClick()
        composeRule.onNodeWithTag("dashboard_add_note").performClick()
        waitForTag("screen_note_editor")
        composeRule.onNodeWithTag("note_title_input").performTextInput("Smoke Note A")
        composeRule.onNodeWithTag("note_content_input").performTextInput("Body #smoke [[Smoke Note B]]")
        SystemClock.sleep(1000)
        composeRule.onNodeWithTag("nav_back", useUnmergedTree = true).performClick()
        waitForTag("screen_dashboard")

        composeRule.onNodeWithTag("fab_add").performClick()
        composeRule.onNodeWithTag("dashboard_add_note").performClick()
        waitForTag("screen_note_editor")
        composeRule.onNodeWithTag("note_title_input").performTextInput("Smoke Note B")
        composeRule.onNodeWithTag("note_content_input").performTextInput("Second note #smoke")
        SystemClock.sleep(1000)
        composeRule.onNodeWithTag("nav_back", useUnmergedTree = true).performClick()
        waitForTag("screen_dashboard")

        // Journal (via dashboard add dialog)
        composeRule.onNodeWithTag("fab_add").performClick()
        composeRule.onNodeWithTag("dashboard_add_journal").performClick()
        composeRule.onNodeWithText("Free Writing").performClick()
        waitForTag("screen_journal_editor")
        composeRule.onNodeWithTag("journal_content_input").performTextInput("Journal entry #smoke")
        SystemClock.sleep(1000)
        tryOpenReminder("journal_reminder")
        composeRule.onNodeWithTag("nav_back", useUnmergedTree = true).performClick()
        waitForTag("screen_dashboard")

        // Finance
        composeRule.onNodeWithTag("fab_add").performClick()
        composeRule.onNodeWithTag("dashboard_add_transaction").performClick()
        waitForTag("screen_transaction_editor")
        composeRule.onNodeWithTag("transaction_type_income").performClick()
        composeRule.onNodeWithTag("transaction_amount_input").performTextInput("100")
        composeRule.onNodeWithTag("transaction_description_input").performTextInput("Salary #smoke")
        composeRule.onNodeWithTag("transaction_save").performClick()
        waitForTag("screen_dashboard")
    }
}
