/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import android.view.View
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.customannotations.Converted
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.AppAndSystemHelper.registerAndCleanupIdlingResources
import org.mozilla.fenix.helpers.FenixTestRule
import org.mozilla.fenix.helpers.HomeActivityTestRule
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.helpers.TestAssetHelper.loremIpsumAsset
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.helpers.ViewVisibilityIdlingResource
import org.mozilla.fenix.helpers.perf.DetectMemoryLeaksRule
import org.mozilla.fenix.ui.robots.browserScreen
import org.mozilla.fenix.ui.robots.navigationToolbar
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2
import mozilla.components.browser.toolbar.R as toolbarR

/**
 *  Tests for verifying basic functionality of content context menus
 *
 *  - Verifies Reader View entry and detection when available UI and functionality
 *  - Verifies Reader View exit UI and functionality
 *  - Verifies Reader View appearance controls UI and functionality
 *
 */

class ReaderViewTest {
    @get:Rule(order = 0)
    val fenixTestRule: FenixTestRule = FenixTestRule()

    private val mockWebServer get() = fenixTestRule.mockWebServer

    private val estimatedReadingTime = "1 - 2 minutes"

    @get:Rule(order = 1)
    val composeTestRule =
        AndroidComposeTestRuleV2(
            HomeActivityTestRule.withDefaultSettingsOverrides(),
        ) { it.activity }

    @get:Rule(order = 2)
    val memoryLeaksRule = DetectMemoryLeaksRule(composeTestRule = { composeTestRule })

    /**
     *  Verify that Reader View capable pages
     *
     *   - Show the toggle button in the navigation bar
     *
     */
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/250592
    @Test
    fun verifyReaderModePageDetectionTest() {
        val readerViewPage = mockWebServer.loremIpsumAsset
        val genericPage = mockWebServer.getGenericAsset(1)

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(readerViewPage.url) {
        }

        navigationToolbar(composeTestRule) {
            verifyReaderViewToolbarButton(true)
        }.enterURLAndEnterToBrowser(genericPage.url) {
        }
        navigationToolbar(composeTestRule) {
            verifyReaderViewToolbarButton(false)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/250585
    @Converted(
        replacedBy = ["org.mozilla.fenix.ui.efficiency.tests.ReaderViewTest#verifyReaderModeControlsTest"],
        bug = 2061142,
        since = "2026-08",
    )
    @SmokeTest
    @Test
    fun verifyReaderModeControlsTest() {
        val readerViewPage = mockWebServer.loremIpsumAsset

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(readerViewPage.url) {
            waitForPageToLoad()
        }

        navigationToolbar(composeTestRule) {
            verifyReaderViewToolbarButton(true)
            clickReaderViewToolbarButton(isReaderViewEnabled = false)
        }

        browserScreen(composeTestRule) {
            verifyPageContent(estimatedReadingTime)
        }.openThreeDotMenu {
            verifyCustomizeReaderViewButton(true)
        }.clickCustomizeReaderViewButton {
            verifyAppearanceFontGroup(true)
            verifyAppearanceFontSansSerif(true)
            verifyAppearanceFontSerif(true)
            verifyAppearanceFontIncrease(true)
            verifyAppearanceFontDecrease(true)
            verifyAppearanceFontSize(3)
            verifyAppearanceColorGroup(true)
            verifyAppearanceColorDark(true)
            verifyAppearanceColorLight(true)
            verifyAppearanceColorSepia(true)
        }.toggleSansSerif {
            verifyAppearanceFontIsActive("SANSSERIF")
        }.toggleSerif {
            verifyAppearanceFontIsActive("SERIF")
        }.toggleFontSizeIncrease {
            verifyAppearanceFontSize(4)
        }.toggleFontSizeIncrease {
            verifyAppearanceFontSize(5)
        }.toggleFontSizeIncrease {
            verifyAppearanceFontSize(6)
        }.toggleFontSizeDecrease {
            verifyAppearanceFontSize(5)
        }.toggleFontSizeDecrease {
            verifyAppearanceFontSize(4)
        }.toggleFontSizeDecrease {
            verifyAppearanceFontSize(3)
        }.toggleColorSchemeChangeDark {
            verifyAppearanceColorSchemeChange("DARK")
        }.toggleColorSchemeChangeSepia {
            verifyAppearanceColorSchemeChange("SEPIA")
        }.toggleColorSchemeChangeLight {
            verifyAppearanceColorSchemeChange("LIGHT")
        }.closeReaderViewControlMenu(composeTestRule) {
        }
        navigationToolbar(composeTestRule) {
            clickReaderViewToolbarButton(isReaderViewEnabled = true)
            verifyReaderViewToolbarButton(true)
        }
        browserScreen(composeTestRule) {
        }.openThreeDotMenu {
            verifyCustomizeReaderViewButton(false)
        }
    }
}
