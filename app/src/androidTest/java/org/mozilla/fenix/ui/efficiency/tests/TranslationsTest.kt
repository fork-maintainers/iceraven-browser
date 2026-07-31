/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.TestAssetHelper.firstForeignWebPageAsset
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.helpers.SwipeDirection
import org.mozilla.fenix.ui.efficiency.selectors.BrowserPageSelectors
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors

class TranslationsTest : BaseTest(isPageLoadTranslationsPromptEnabled = true) {

    private val mockWebServer get() = fenixTestRule.mockWebServer

    @SmokeTest
    @Test
    fun verifyTheFirstTranslationNotNowButtonFunctionalityTest() {
        val testPage = mockWebServer.firstForeignWebPageAsset
        val url = testPage.url.toString()

        on.browserPage.navigateToPage(url)
            .verifyTranslationSheetWithReload(url)
            .mozClickIfPresent(BrowserPageSelectors.TRANSLATION_SHEET_NOT_NOW_BUTTON)
            .mozWaitUntilAbsent(BrowserPageSelectors.TRANSLATION_SHEET_NOT_NOW_BUTTON, TestAssetHelper.waitingTimeLong)
        on.browserPage
            .openMainMenu()
            .mozClick(MainMenuSelectors.MORE_BUTTON)
            .mozClick(MainMenuSelectors.TRANSLATE_BUTTON)
            .mozVerifyElementsByGroup("notTranslatedPageTranslationSheet")
            .mozSwipeElementUntilAbsent(BrowserPageSelectors.TRANSLATION_SHEET, SwipeDirection.DOWN, maxSwipes = 3)
        on.browserPage
            .openMainMenu()
            .mozClick(MainMenuSelectors.MORE_BUTTON)
            .mozClick(MainMenuSelectors.TRANSLATE_BUTTON)
            .mozVerifyElementsByGroup("notTranslatedPageTranslationSheet")
    }
}
