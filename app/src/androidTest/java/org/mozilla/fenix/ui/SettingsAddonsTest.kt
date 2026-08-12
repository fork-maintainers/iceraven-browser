/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import androidx.core.net.toUri
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.R
import org.mozilla.fenix.customannotations.Converted
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.AppAndSystemHelper.registerAndCleanupIdlingResources
import org.mozilla.fenix.helpers.FenixTestRule
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.RecyclerViewIdlingResource
import org.mozilla.fenix.helpers.TestAssetHelper.enhancedTrackingProtectionAsset
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.helpers.perf.DetectMemoryLeaksRule
import org.mozilla.fenix.ui.robots.addonsMenu
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2

/**
 *  Tests for verifying the functionality of installing or removing addons
 *
 */
class SettingsAddonsTest {
    @get:Rule(order = 0)
    val fenixTestRule: FenixTestRule = FenixTestRule()

    private val mockWebServer get() = fenixTestRule.mockWebServer

    @get:Rule(order = 1)
    val composeTestRule =
        AndroidComposeTestRuleV2(
            HomeActivityIntentTestRule.withDefaultSettingsOverrides(),
        ) { it.activity }

    @get:Rule(order = 2)
    val memoryLeaksRule = DetectMemoryLeaksRule(composeTestRule = { composeTestRule })

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/875780
    // Walks through settings add-ons menu to ensure all items are present
    @Test
    fun verifyAddonsListItemsTest() {
        homeScreen(composeTestRule) {
        }.openThreeDotMenu {
        }.clickSettingsButton {
            verifyAdvancedHeading()
            verifyAddons()
        }.openAddonsManagerMenu(composeTestRule) {
            registerAndCleanupIdlingResources(
                RecyclerViewIdlingResource(composeTestRule.activity.findViewById(R.id.add_ons_list), 1),
            ) {
                verifyAddonsItems()
            }
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/875781
    // Installs an add-on from the Add-ons menu and verifies the prompts
    @Test
    fun installAddonFromMainMenuTest() {
        val addonName = "AdGuard AdBlocker"

        homeScreen(composeTestRule) {}
            .openThreeDotMenu {}
            .clickExtensionsButton {
                registerAndCleanupIdlingResources(
                    RecyclerViewIdlingResource(
                        composeTestRule.activity.findViewById(R.id.add_ons_list),
                        1,
                    ),
                ) {
                    waitForAddonsListProgressBarToBeGone()
                    clickInstallAddon(addonName)
                }
                verifyAddonDownloadOverlay()
                verifyAddonPermissionPrompt(addonName)
                cancelInstallAddon()
                clickInstallAddon(addonName)
                acceptPermissionToInstallAddon()
                verifyAddonInstallCompletedPrompt(addonName, composeTestRule.activityRule)
                closeAddonInstallCompletePrompt()
                verifyAddonIsInstalled(addonName)
                verifyEnabledTitleDisplayed()
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/561597
    // Installs an addon, then uninstalls it
    @Test
    fun verifyAddonsCanBeUninstalledTest() {
        val addonName = "uBlock Origin"

        addonsMenu(composeTestRule) {
            installAddon(addonName, composeTestRule.activityRule)
            closeAddonInstallCompletePrompt()
        }.openDetailedMenuForAddon(addonName) {
        }.removeAddon(composeTestRule.activityRule) {
        }.goBackToHomeScreen {
        }.openThreeDotMenu {
        }.clickExtensionsButton {
            verifyAddonCanBeInstalled(addonName)
        }
    }

    // TODO: Harden to dynamically install addons from position
    //   in list of detected addons on screen instead of hard-coded values.
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/561600
    // Installs 2 add-on and checks that the app doesn't crash while navigating the app
    @Converted(
        replacedBy = ["org.mozilla.fenix.ui.efficiency.tests.SettingsAddonsTest#noCrashWithAddonInstalledTest"],
        bug = 2062856,
        since = "2026-08",
    )
    @SmokeTest
    @Test
    fun noCrashWithAddonInstalledTest() {
        // setting ETP to Strict mode to test it works with add-ons
        composeTestRule.activity.components.settings.setStrictETP()

        val uBlockAddon = "uBlock Origin"
        val darkReaderAddon = "Dark Reader"
        val trackingProtectionPage = mockWebServer.enhancedTrackingProtectionAsset

        addonsMenu(composeTestRule) {
            installAddon(uBlockAddon, composeTestRule.activityRule)
            closeAddonInstallCompletePrompt()
            // installAddon(darkReaderAddon, composeTestRule.activityRule)
            clickInstallAddon(darkReaderAddon)
            verifyAddonPermissionPrompt(darkReaderAddon)
            acceptPermissionToInstallAddon()
            verifyAddonInstallCompletedPrompt(darkReaderAddon, composeTestRule.activityRule)
            closeAddonInstallCompletePrompt()
        }.goBackToHomeScreen {
        }
        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(trackingProtectionPage.url) {
            verifyUrl(trackingProtectionPage.url.toString())
        }.goToHomescreen {
        }.openTopSiteTabWithTitle("Wikipedia") {
        }.openThreeDotMenu {
        }.clickSettingsButton {
            verifySettingsView()
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/561594
    @Converted(
        replacedBy = ["org.mozilla.fenix.ui.efficiency.tests.SettingsAddonsTest#verifyUBlockWorksInPrivateModeTest"],
        bug = 2062856,
        since = "2026-08",
    )
    @SmokeTest
    @Test
    fun verifyUBlockWorksInPrivateModeTest() {
        val addonName = "uBlock Origin"
        val webPage = "https://mozilla-mobile.github.io/testapp/"

        addonsMenu(composeTestRule) {
            installAddonInPrivateMode(addonName, composeTestRule.activityRule)
            closeAddonInstallCompletePrompt()
        }.goBackToHomeScreen {
        }
        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(webPage.toUri()) {
            verifyPageContent("Lets test!")
        }.openThreeDotMenu {
        }.clickExtensionsButton {
            verifyExtensionsButtonWithInstalledExtension(addonName)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/875785
    @Test
    fun verifyUBlockWorksInNormalModeTest() {
        val genericURL = mockWebServer.getGenericAsset(1)
        val addonName = "uBlock Origin"
        val webPage = "https://mozilla-mobile.github.io/testapp/"

        addonsMenu(composeTestRule) {
            installAddon(addonName, composeTestRule.activityRule)
            closeAddonInstallCompletePrompt()
            verifyAddonIsInstalled(addonName)
        }.goBackToHomeScreen {
        }
        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(genericURL.url) {
            verifyPageContent(genericURL.content)
        }
        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(webPage.toUri()) {
            verifyPageContent("Lets test!")
        }.openThreeDotMenu {
        }.clickExtensionsButton {
            verifyExtensionsButtonWithInstalledExtension(addonName)
        }
    }
}
