/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.TestAssetHelper.loremIpsumAsset
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.pageObjects.ReaderViewPage
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors
import org.mozilla.fenix.ui.efficiency.selectors.ReaderViewSelectors
import org.mozilla.fenix.ui.efficiency.selectors.ToolbarSelectors

class ReaderViewTest : BaseTest() {

    private val mockWebServer get() = fenixTestRule.mockWebServer
    private val estimatedReadingTime = "1 - 2 minutes"

    // ReaderViewPage is not part of PageContext (the appearance controls are a transient overlay the
    // navigation graph cannot reach), so instantiate it locally. The test drives the entry sequence
    // explicitly via on.browserPage / on.mainMenu below.
    private val readerView get() = ReaderViewPage(composeRule)

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/250585
    // Converted from legacy org.mozilla.fenix.ui.ReaderViewTest.verifyReaderModeControlsTest
    @SmokeTest
    @Test
    fun verifyReaderModeControlsTest() {
        val readerViewPage = mockWebServer.loremIpsumAsset

        // Reader-view appearance prefs persist across runs/retries; reset them so the baseline
        // assertions (font size 3) are deterministic regardless of prior state on the device.
        readerView.resetAppearancePrefsToDefault()

        // Load a reader-view-capable page and enter reader view from the toolbar toggle.
        on.browserPage.navigateToPage(readerViewPage.url.toString())
        // The toggle only appears once the page is detected as reader-able, which can lag the load, so
        // give it a longer poll than the default before asserting/clicking it.
        on.browserPage.mozVerify(ToolbarSelectors.READER_VIEW_BUTTON, timeout = 15_000)
        on.browserPage.mozClick(ToolbarSelectors.READER_VIEW_BUTTON)

        // Reader view renders the article, including the estimated reading time.
        on.browserPage.verifyPageContent(estimatedReadingTime)

        // Customize Reader View is offered in the main menu only while reader view is active.
        on.mainMenu.navigateToPage()
        on.mainMenu.mozVerify(MainMenuSelectors.CUSTOMIZE_READER_VIEW_BUTTON)
        on.mainMenu.mozClick(MainMenuSelectors.CUSTOMIZE_READER_VIEW_BUTTON)

        // The appearance controls bar shows every control, with the default font size (3). The bar
        // animates in after the menu click, so give the first control a longer poll to appear.
        readerView.mozVerify(ReaderViewSelectors.APPEARANCE_FONT_GROUP, timeout = 15_000)
        readerView.mozVerify(ReaderViewSelectors.APPEARANCE_FONT_SANS_SERIF)
        readerView.mozVerify(ReaderViewSelectors.APPEARANCE_FONT_SERIF)
        readerView.mozVerify(ReaderViewSelectors.APPEARANCE_FONT_SIZE_INCREASE)
        readerView.mozVerify(ReaderViewSelectors.APPEARANCE_FONT_SIZE_DECREASE)
        readerView.verifyFontSize(3)
        readerView.mozVerify(ReaderViewSelectors.APPEARANCE_COLOR_GROUP)
        readerView.mozVerify(ReaderViewSelectors.APPEARANCE_COLOR_DARK)
        readerView.mozVerify(ReaderViewSelectors.APPEARANCE_COLOR_LIGHT)
        readerView.mozVerify(ReaderViewSelectors.APPEARANCE_COLOR_SEPIA)

        // Font type: toggling a font selects its radio button.
        readerView.mozClick(ReaderViewSelectors.APPEARANCE_FONT_SANS_SERIF)
        readerView.mozVerifyElementIsChecked(ReaderViewSelectors.APPEARANCE_FONT_SANS_SERIF)
        readerView.mozClick(ReaderViewSelectors.APPEARANCE_FONT_SERIF)
        readerView.mozVerifyElementIsChecked(ReaderViewSelectors.APPEARANCE_FONT_SERIF)

        // Font size: increase three times, then decrease three times, back to the default.
        readerView.mozClick(ReaderViewSelectors.APPEARANCE_FONT_SIZE_INCREASE)
        readerView.verifyFontSize(4)
        readerView.mozClick(ReaderViewSelectors.APPEARANCE_FONT_SIZE_INCREASE)
        readerView.verifyFontSize(5)
        readerView.mozClick(ReaderViewSelectors.APPEARANCE_FONT_SIZE_INCREASE)
        readerView.verifyFontSize(6)
        readerView.mozClick(ReaderViewSelectors.APPEARANCE_FONT_SIZE_DECREASE)
        readerView.verifyFontSize(5)
        readerView.mozClick(ReaderViewSelectors.APPEARANCE_FONT_SIZE_DECREASE)
        readerView.verifyFontSize(4)
        readerView.mozClick(ReaderViewSelectors.APPEARANCE_FONT_SIZE_DECREASE)
        readerView.verifyFontSize(3)

        // Color scheme: toggling a scheme selects its radio button.
        readerView.mozClick(ReaderViewSelectors.APPEARANCE_COLOR_DARK)
        readerView.mozVerifyElementIsChecked(ReaderViewSelectors.APPEARANCE_COLOR_DARK)
        readerView.mozClick(ReaderViewSelectors.APPEARANCE_COLOR_SEPIA)
        readerView.mozVerifyElementIsChecked(ReaderViewSelectors.APPEARANCE_COLOR_SEPIA)
        readerView.mozClick(ReaderViewSelectors.APPEARANCE_COLOR_LIGHT)
        readerView.mozVerifyElementIsChecked(ReaderViewSelectors.APPEARANCE_COLOR_LIGHT)

        // Close the appearance controls (device back); reader view stays active.
        readerView.mozPressBackUntilGone(ReaderViewSelectors.APPEARANCE_FONT_SANS_SERIF)

        // Exit reader view from the toolbar; the "enter reader view" toggle returns. Dismissing the
        // appearance bar (device back) triggers a toolbar recomposition, so the close button can lag;
        // poll for it before clicking rather than clicking blind (mozClick does not wait, which is
        // exactly what makes the legacy robot's single-back-then-click flaky here).
        on.browserPage.mozVerify(ToolbarSelectors.READER_VIEW_CLOSE_BUTTON, timeout = 15_000)
        on.browserPage.mozClick(ToolbarSelectors.READER_VIEW_CLOSE_BUTTON)
        on.browserPage.mozVerify(ToolbarSelectors.READER_VIEW_BUTTON)

        // Re-sync page state to the browser (the control-bar detour left it on the menu), reopen the
        // menu, and confirm Customize Reader View is gone now that reader view is off.
        on.browserPage.navigateToPage()
        on.mainMenu.navigateToPage()
        on.mainMenu.mozVerifyElementAbsent(MainMenuSelectors.CUSTOMIZE_READER_VIEW_BUTTON)
    }
}
