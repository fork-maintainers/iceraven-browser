/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.experimentintegration

import android.content.pm.ActivityInfo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.HomeActivityTestRule
import org.mozilla.fenix.helpers.TestHelper
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.surveyScreen
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2

/**
 *  Tests for verifying functionality of the message survey surface
 */
class SurveyExperimentIntegrationTest {
    private val surveyURL = "qsurvey.mozilla.com"
    private val experimentName = "Viewpoint"

    @get:Rule
    val composeTestRule =
        AndroidComposeTestRuleV2(
            HomeActivityTestRule(
                isDeleteSitePermissionsEnabled = true,
            ),
        ) { it.activity }

    @Before
    fun setUp() {
        TestHelper.appContext.components.settings.showSecretDebugMenuThisSession = true
    }

    @After
    fun tearDown() {
        TestHelper.appContext.components.settings.showSecretDebugMenuThisSession = false
    }

    fun checkExperimentExists() {
        homeScreen(composeTestRule) {
        }.openThreeDotMenu {
        }.clickSettingsButton {
        }.openExperimentsMenu {
            verifyExperimentExists(experimentName)
        }
    }

    @Test
    fun checkSurveyNavigatesCorrectly() {
        surveyScreen(composeTestRule) {
            verifySurveyButton(composeTestRule)
        }.clickSurveyButton {
            verifyUrl(surveyURL)
        }

        checkExperimentExists()
    }

    @Test
    fun checkSurveyNoThanksNavigatesCorrectly() {
        surveyScreen(composeTestRule) {
            verifySurveyNoThanksButton(composeTestRule)
        }.clickNoThanksSurveyButton {
            verifyTabCounter("0")
        }

        checkExperimentExists()
    }

    @Test
    fun checkHomescreenSurveyDismissesCorrectly() {
        surveyScreen(composeTestRule) {
            verifyHomeScreenSurveyCloseButton(true)
        }.clickHomeScreenSurveyCloseButton {
            verifyTabCounter("0")
            verifySurveyButtonDoesNotExist()
        }

        checkExperimentExists()
    }

    @Test
    fun checkSurveyLandscapeLooksCorrect() {
        composeTestRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        surveyScreen(composeTestRule) {
            verifySurveyNoThanksButton(composeTestRule)
            verifySurveyButton(composeTestRule)
        }.clickNoThanksSurveyButton {
            verifyTabCounter("0")
        }
    }
}
