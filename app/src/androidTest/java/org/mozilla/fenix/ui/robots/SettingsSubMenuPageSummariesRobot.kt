/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import mozilla.components.feature.summarize.R as summarizeR
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.Constants.TAG
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.helpers.MatcherHelper.assertUIObjectExists
import org.mozilla.fenix.helpers.MatcherHelper.itemContainingText

/** Implementation of Robot Pattern for the settings Page summaries sub menu. */
class SettingsSubMenuPageSummariesRobot(private val composeTestRule: ComposeTestRule) {

    fun verifyPageSummariesView() {
        Log.i(TAG, "verifyPageSummariesView: Trying to verify that the \"Page summaries\" toolbar title is visible")
        assertUIObjectExists(pageSummariesToolbarTitle())
        Log.i(TAG, "verifyPageSummariesView: Verified that the \"Page summaries\" toolbar title is visible")
        Log.i(TAG, "verifyPageSummariesView: Trying to verify that the go back button is displayed")
        goBackButton().check(matches(isDisplayed()))
        Log.i(TAG, "verifyPageSummariesView: Verified that the go back button is displayed")
        Log.i(TAG, "verifyPageSummariesView: Trying to verify that the \"Summarize pages\" option is displayed")
        composeTestRule.summarizePagesOption().assertIsDisplayed()
        Log.i(TAG, "verifyPageSummariesView: Verified that the \"Summarize pages\" option is displayed")
        Log.i(TAG, "verifyPageSummariesView: Trying to verify that the learn more link is displayed")
        composeTestRule.learnMoreLink().assertIsDisplayed()
        Log.i(TAG, "verifyPageSummariesView: Verified that the learn more link is displayed")
        Log.i(TAG, "verifyPageSummariesView: Trying to verify that the gestures sub header is displayed")
        composeTestRule.gesturesSubHeader().assertIsDisplayed()
        Log.i(TAG, "verifyPageSummariesView: Verified that the gestures sub header is displayed")
        Log.i(TAG, "verifyPageSummariesView: Trying to verify that the \"Shake to summarize\" option is displayed")
        composeTestRule.shakeToSummarizeOption().assertIsDisplayed()
        Log.i(TAG, "verifyPageSummariesView: Verified that the \"Shake to summarize\" option is displayed")
    }

    fun verifySummarizePagesToggle(isChecked: Boolean) {
        if (isChecked) {
            Log.i(
                TAG,
                "verifySummarizePagesToggle: Trying to verify that the \"Summarize pages\" toggle is enabled: $isChecked",
            )
            composeTestRule.summarizePagesOption().assertIsOn()
            Log.i(TAG, "verifySummarizePagesToggle: Verified that the \"Summarize pages\" toggle is enabled")
        } else {
            Log.i(
                TAG,
                "verifySummarizePagesToggle: Trying to verify that the \"Summarize pages\" toggle is disabled: $isChecked",
            )
            composeTestRule.summarizePagesOption().assertIsOff()
            Log.i(TAG, "verifySummarizePagesToggle: Verified that the \"Summarize pages\" toggle is disabled")
        }
    }

    fun clickSummarizePagesToggle() {
        Log.i(TAG, "clickSummarizePagesToggle: Trying to click the \"Summarize pages\" toggle")
        composeTestRule.summarizePagesOption().performClick()
        Log.i(TAG, "clickSummarizePagesToggle: Clicked the \"Summarize pages\" toggle")
    }

    class Transition(private val composeTestRule: ComposeTestRule)
}

private fun pageSummariesToolbarTitle() = itemContainingText(getStringResource(R.string.preferences_page_summaries))

private fun goBackButton() = onView(withContentDescription("Navigate up"))

private fun ComposeTestRule.summarizePagesOption() =
    onNodeWithText(getStringResource(summarizeR.string.mozac_summarize_settings_summarize_pages))

private fun ComposeTestRule.learnMoreLink() =
    onNodeWithText(getStringResource(summarizeR.string.mozac_summarize_settings_learn_more))

private fun ComposeTestRule.gesturesSubHeader() =
    onNodeWithText(getStringResource(summarizeR.string.mozac_summarize_settings_gestures))

private fun ComposeTestRule.shakeToSummarizeOption() =
    onNodeWithText(getStringResource(summarizeR.string.mozac_summarize_settings_shake_to_summarize))
