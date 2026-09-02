/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.FenixTestRule
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestAssetHelper.articleSummaryAsset
import org.mozilla.fenix.helpers.TestAssetHelper.loremIpsumAsset
import org.mozilla.fenix.helpers.perf.DetectMemoryLeaksRule
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2

class SettingsPageSummariesTest {
    @get:Rule(order = 0)
    val fenixTestRule: FenixTestRule = FenixTestRule()

    private val mockWebServer get() = fenixTestRule.mockWebServer

    @get:Rule(order = 1)
    val composeTestRule =
        AndroidComposeTestRuleV2(
            HomeActivityIntentTestRule(
                skipOnboarding = true,
                shakeToSummarizeFeatureFlagEnabled = true,
                hasSeenShakeToSummarizeToolbarCfr = false,
            ),
        ) { it.activity }

    @get:Rule(order = 2)
    val memoryLeaksRule = DetectMemoryLeaksRule(composeTestRule = { composeTestRule })

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4036042
    @SmokeTest
    @Test
    fun verifyPageSummariesUITest() {
        homeScreen(composeTestRule) {
        }.openThreeDotMenu {
        }.clickSettingsButton {
            verifyPageSummariesButton()
        }.openPageSummariesSubMenu(composeTestRule) {
            verifyPageSummariesView()
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4036045
    @Test
    fun verifyTheSummarizePagesToggleBehaviourTest() {
        val articlePage = mockWebServer.articleSummaryAsset
        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(articlePage.url) {
            waitForPageToLoad()
            clickTheDismissButtonOnSummarizeCFR()
        }.openThreeDotMenu {
        }.clickSettingsButton {
        }.openPageSummariesSubMenu(composeTestRule) {
            verifySummarizePagesToggle(true)
            clickSummarizePagesToggle()
            verifySummarizePagesToggle(false)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4035976
    @Test
    fun verifyTheShakeToSummarizeCFRTest() {
        val articlePage = mockWebServer.articleSummaryAsset
        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(articlePage.url) {
            waitForPageToLoad()
            verifyTheSummarizeCFR(true)
            clickTheDismissButtonOnSummarizeCFR()
            verifyTheSummarizeCFR(false)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4035977
    @Test
    fun verifyTheShakeToSummarizeCFRIsOnlyDisplayedOnceTest() {
        val firstWebsite = mockWebServer.articleSummaryAsset
        val secondWebsite = mockWebServer.loremIpsumAsset
        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(firstWebsite.url) {
            waitForPageToLoad()
            verifyTheSummarizeCFR(true)
            clickTheDismissButtonOnSummarizeCFR()
        }
        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(secondWebsite.url) {
            waitForPageToLoad()
            verifyTheSummarizeCFR(false)
        }
    }
}
