package com.lifepad.app

import android.os.SystemClock
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @OptIn(ExperimentalTestApi::class)
    private fun waitForTag(tag: String, timeoutMs: Long = 10000) {
        val start = SystemClock.uptimeMillis()
        while (SystemClock.uptimeMillis() - start < timeoutMs) {
            try {
                composeRule.waitForIdle()
                if (composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()) {
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
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size
        } catch (_: IllegalStateException) {
            0
        }
    }

    private fun maybeSkipPin() {
        if (nodeCount("screen_pin") > 0) {
            composeRule.onNodeWithTag("pin_skip").performClick()
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
        maybeSkipPin()
        waitForTag("screen_dashboard")
        composeRule.onNodeWithTag("screen_dashboard").assertIsDisplayed()

        // Notes: create two notes, one with wikilink + hashtag
        composeRule.onNodeWithTag("nav_notes").performClick()
        waitForTag("screen_notes")

        composeRule.onNodeWithTag("fab_create_note").performClick()
        waitForTag("screen_note_editor")
        composeRule.onNodeWithTag("note_title_input").performTextInput("Smoke Note A")
        composeRule.onNodeWithTag("note_content_input").performTextInput("Body #smoke [[Smoke Note B]]")
        SystemClock.sleep(1000)
        composeRule.onNodeWithTag("nav_back").performClick()
        waitForTag("screen_notes")
        waitForTag("note_item", timeoutMs = 30000)
        assertTrue("Expected at least one note item", nodeCount("note_item") > 0)
        composeRule.onNodeWithText("Smoke Note A").assertIsDisplayed()

        composeRule.onNodeWithTag("fab_create_note").performClick()
        waitForTag("screen_note_editor")
        composeRule.onNodeWithTag("note_title_input").performTextInput("Smoke Note B")
        composeRule.onNodeWithTag("note_content_input").performTextInput("Second note #smoke")
        SystemClock.sleep(1000)
        composeRule.onNodeWithTag("nav_back").performClick()
        waitForTag("screen_notes")
        waitForTag("note_item", timeoutMs = 30000)
        composeRule.onNodeWithText("Smoke Note B").assertIsDisplayed()

        // Reminder dialog (best-effort)
        composeRule.onNodeWithText("Smoke Note A").performClick()
        waitForTag("screen_note_editor")
        tryOpenReminder("note_reminder")
        composeRule.onNodeWithTag("nav_back").performClick()
        waitForTag("screen_notes")

        // Graph
        composeRule.onNodeWithTag("nav_graph").performClick()
        waitForTag("screen_graph")
        composeRule.onNodeWithTag("nav_back").performClick()
        waitForTag("screen_notes")

        // Journal
        composeRule.onNodeWithTag("nav_journal").performClick()
        waitForTag("screen_journal")
        composeRule.onNodeWithTag("fab_create_entry").performClick()
        waitForTag("screen_journal_editor")
        composeRule.onNodeWithTag("journal_content_input").performTextInput("Journal entry #smoke")
        SystemClock.sleep(1000)
        tryOpenReminder("journal_reminder")
        composeRule.onNodeWithTag("nav_back").performClick()
        waitForTag("screen_journal")
        waitForTag("journal_item", timeoutMs = 30000)
        assertTrue("Expected at least one journal item", nodeCount("journal_item") > 0)

        // Finance
        composeRule.onNodeWithTag("nav_finance").performClick()
        waitForTag("screen_finance")
        composeRule.onNodeWithTag("fab_create_transaction").performClick()
        waitForTag("screen_transaction_editor")
        composeRule.onNodeWithTag("transaction_type_income").performClick()
        composeRule.onNodeWithTag("transaction_amount_input").performTextInput("100")
        composeRule.onNodeWithTag("transaction_description_input").performTextInput("Salary #smoke")
        composeRule.onNodeWithTag("transaction_save").performClick()
        waitForTag("screen_finance")
        waitForTag("transaction_item", timeoutMs = 30000)
        assertTrue("Expected at least one transaction item", nodeCount("transaction_item") > 0)
        composeRule.onNodeWithText("Salary #smoke").assertIsDisplayed()

        // Insights row should appear after income is recorded
        waitForTag("finance_insights_row", timeoutMs = 30000)

        // Safe-to-spend amount should be positive
        val amountNode = composeRule.onNodeWithTag("safe_to_spend_amount").fetchSemanticsNode()
        val textList = if (amountNode.config.contains(SemanticsProperties.Text)) {
            amountNode.config[SemanticsProperties.Text]
        } else {
            emptyList()
        }
        val amountText = textList.joinToString(separator = "") { it.text }.trim()
        val numeric = amountText.replace(Regex("[^0-9.-]"), "").toDoubleOrNull() ?: 0.0
        assertTrue("Expected safe-to-spend amount > 0, got '$amountText'", numeric > 0.0)

        // Finance sub-screens
        composeRule.onNodeWithTag("finance_safe_to_spend").performClick()
        waitForTag("screen_safe_to_spend")
        composeRule.onNodeWithTag("nav_back").performClick()
        waitForTag("screen_finance")

        composeRule.onNodeWithTag("nav_bills").performClick()
        waitForTag("screen_bills")
        composeRule.onNodeWithTag("nav_back").performClick()
        waitForTag("screen_finance")

        composeRule.onNodeWithTag("nav_goals").performClick()
        waitForTag("screen_goals")
        composeRule.onNodeWithTag("nav_back").performClick()
        waitForTag("screen_finance")

        composeRule.onNodeWithTag("nav_networth").performClick()
        waitForTag("screen_networth")
        composeRule.onNodeWithTag("nav_back").performClick()
        waitForTag("screen_finance")

        // Search
        composeRule.onNodeWithTag("nav_search").performClick()
        waitForTag("screen_search")
        composeRule.onNodeWithTag("search_input").performTextInput("#smoke")
        waitForTag("search_result", timeoutMs = 30000)
        assertTrue("Expected search results", nodeCount("search_result") > 0)
    }
}
