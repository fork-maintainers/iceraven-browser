/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.helpers.FenixTestRule
import org.mozilla.fenix.helpers.HomeActivityTestRule
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.helpers.perf.DetectMemoryLeaksRule
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar
import org.mozilla.fenix.ui.robots.notificationShade
import org.mozilla.fenix.utils.DURATION_MS_TRANSLATIONS
import org.mozilla.fenix.utils.exitMenu
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2

/**
 *  Tests for verifying the the privacy and security section of the Settings menu
 *
 */

class SettingsPrivacyTest {
    @get:Rule(order = 0)
    val fenixTestRule: FenixTestRule = FenixTestRule()

    private val mockWebServer get() = fenixTestRule.mockWebServer

    @get:Rule(order = 1)
    val composeTestRule =
        AndroidComposeTestRuleV2(
            HomeActivityTestRule.withDefaultSettingsOverrides(),
        ) { it.activity }

    @get:Rule(order = 2)
    val memoryLeaksRule = DetectMemoryLeaksRule(composeTestRule = { composeTestRule })

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2092698
    @Test
    fun settingsPrivacyItemsTest() {
        homeScreen(composeTestRule) {
        }.openThreeDotMenu {
        }.clickSettingsButton {
            verifySettingsToolbar()
            verifyPrivacyHeading()
            verifyPrivateBrowsingButton()
            verifyHTTPSOnlyModeButton()
            verifySettingsOptionSummary("HTTPS-Only Mode", "Off")
            verifyEnhancedTrackingProtectionButton()
            verifySettingsOptionSummary("Enhanced Tracking Protection", "Standard")
            verifySiteSettingsButton()
            verifyDeleteBrowsingDataButton()
            verifyDeleteBrowsingDataOnQuitButton()
            verifySettingsOptionSummary("Delete browsing data on quit", "Off")
            verifyNotificationsButton()
            verifySettingsOptionSummary("Notifications", "Allowed")
            verifyDataCollectionButton()
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/243362
    @Test
    fun verifyDataCollectionSettingsTest() {
        homeScreen(composeTestRule) {
        }.openThreeDotMenu {
        }.clickSettingsButton {
        }.openSettingsSubMenuDataCollection {
            // Studies depends on the telemetry switch,  if telemetry is off studies will be
            // turned off as well, and will require the app to be restarted.
            // Daily usage ping should default to telemetry pref value
            verifyDataCollectionView(
                composeTestRule,
                isSendTechnicalDataEnabled = true,
                isDailyUsagePingEnabled = true,
                studiesSummary = "On",
            )
            clickUsageAndTechnicalDataToggle(composeTestRule)
            verifyDataCollectionView(
                composeTestRule,
                isSendTechnicalDataEnabled = false,
                isDailyUsagePingEnabled = true,
                studiesSummary = "Off",
            )
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/1024594
    @Test
    fun allowAppToSendNotifications() {
        val defaultWebPage = mockWebServer.getGenericAsset(1)

        // Clear all existing notifications
        notificationShade {
            mDevice.openNotification()
            clearNotifications()
        }

        homeScreen(composeTestRule) {
        }.togglePrivateBrowsingMode()

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openNotificationShade {
            verifySystemNotificationExists("Close private tabs?")
        }.closeNotificationTray(composeTestRule) {
        }.openThreeDotMenu {
        }.clickSettingsButton {
            verifySettingsOptionSummary("Notifications", "Allowed")
        }.openSettingsSubMenuNotifications {
            verifyAllSystemNotificationsToggleState(true)
            verifyPrivateBrowsingSystemNotificationsToggleState(true)
            exitMenu(DURATION_MS_TRANSLATIONS)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2939287
    @Test
    fun verifyTheDailyUsagePingCanBeEnabledAndDisabledTest() {
        homeScreen(composeTestRule) {
        }.openThreeDotMenu {
        }.clickSettingsButton {
        }.openSettingsSubMenuDataCollection {
            verifyDailyUsagePingToggle(composeTestRule, isChecked = true)
            clickDailyUsagePingToggle(composeTestRule)
            verifyDailyUsagePingToggle(composeTestRule, isChecked = false)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3215044
    @Test
    fun verifyTheCrashReportsOptionsTest() {
        homeScreen(composeTestRule) {
        }.openThreeDotMenu {
        }.clickSettingsButton {
        }.openSettingsSubMenuDataCollection {
            verifyTheCrashReportsSection(composeTestRule)
            verifyTheCrashReportOptionStates(
                composeTestRule,
                isAskBeforeSendingCrashReportsEnabled = true,
                isAutomaticallySendCrashReportsEnabled = false,
                isNeverSendCrashReportsEnabled = false,
            )
            clickTheCrashReportsRadioButton(composeTestRule, "Send automatically")
            verifyTheCrashReportOptionStates(
                composeTestRule,
                isAskBeforeSendingCrashReportsEnabled = false,
                isAutomaticallySendCrashReportsEnabled = true,
                isNeverSendCrashReportsEnabled = false,
            )
            clickTheCrashReportsRadioButton(composeTestRule, "Never send")
            verifyTheCrashReportOptionStates(
                composeTestRule,
                isAskBeforeSendingCrashReportsEnabled = false,
                isAutomaticallySendCrashReportsEnabled = false,
                isNeverSendCrashReportsEnabled = true,
            )
            clickTheCrashReportsRadioButton(composeTestRule, "Ask before sending")
            verifyTheCrashReportOptionStates(
                composeTestRule,
                isAskBeforeSendingCrashReportsEnabled = true,
                isAutomaticallySendCrashReportsEnabled = false,
                isNeverSendCrashReportsEnabled = false,
            )
        }
    }
}
