/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import org.junit.Ignore
import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.AppAndSystemHelper.assertYoutubeAppOpens
import org.mozilla.fenix.helpers.OpenLinksInApp
import org.mozilla.fenix.helpers.TestAssetHelper.externalLinksAsset
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeLong
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest

class SettingsOpenLinksInAppsTest : BaseTest() {

    private val mockWebServer get() = fenixTestRule.mockWebServer

    @Ignore("Covered by verifyNavigationReachability[1: SettingsOpenLinksInAppsPage (TBD) — Navigation Reachability]")
    @Test
    fun verifyTheSettingsOpenLinksInAppsTest() {
        on.settingsOpenLinksInApps.navigateToPage()
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2121046
    // Converted from legacy SettingsAdvancedTest.askBeforeOpeningOpenLinkInAppTest.
    // The "Playstore link" is a plain http link (http://play.google.com/). Under "Ask", the applinks
    // prompt only appears when a non-browser app is registered to handle the link; the CI emulator has
    // no Play Store installed, so the http link just navigates in-browser and no prompt is raised
    // (unlike the vnd.youtube:// scheme links in the tests below). The legacy body asserted
    // verifyUrl("play.google.com") but the legacy BrowserRobot.verifyUrl swallowed its timeout (no-op);
    // the efficiency verifyUrl actually waits, so assert the navigation the legacy test meant to check.
    @SmokeTest
    @Test
    fun askBeforeOpeningOpenLinkInAppTest() {
        val externalLinksPage = mockWebServer.externalLinksAsset

        composeRule.activityRule.applySettingsExceptions {
            it.openLinksInExternalApp = OpenLinksInApp.ASK
        }

        on.browserPage.navigateToPage(externalLinksPage.url.toString())
        on.browserPage.clickPageContent("Playstore link")
        on.browserPage.verifyUrl("play.google.com", timeout = waitingTimeLong)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2121045
    // Converted from legacy SettingsAdvancedTest.askBeforeOpeningLinkInAppCancelTest
    @SmokeTest
    @Test
    fun askBeforeOpeningLinkInAppCancelTest() {
        val externalLinksPage = mockWebServer.externalLinksAsset

        composeRule.activityRule.applySettingsExceptions {
            it.openLinksInExternalApp = OpenLinksInApp.ASK
        }

        on.browserPage.navigateToPage(externalLinksPage.url.toString())
        on.browserPage.clickPageContent("Youtube schema link")
        on.browserPage.verifyOpenLinkInAppPrompt(appName = "YouTube")
        on.browserPage.clickStayInBrowserPromptButton()
        on.browserPage.verifyUrl(externalLinksPage.url.toString())
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2288347
    // Converted from legacy SettingsAdvancedTest.askBeforeOpeningLinkInAppOpenTest
    @SmokeTest
    @Test
    fun askBeforeOpeningLinkInAppOpenTest() {
        val externalLinksPage = mockWebServer.externalLinksAsset

        composeRule.activityRule.applySettingsExceptions {
            it.openLinksInExternalApp = OpenLinksInApp.ASK
        }

        on.browserPage.navigateToPage(externalLinksPage.url.toString())
        on.browserPage.clickPageContent("Youtube schema link")
        on.browserPage.verifyOpenLinkInAppPrompt(appName = "YouTube")
        on.browserPage.clickOpenLinkInAppPromptOpenButton()
        assertYoutubeAppOpens()
    }
}
