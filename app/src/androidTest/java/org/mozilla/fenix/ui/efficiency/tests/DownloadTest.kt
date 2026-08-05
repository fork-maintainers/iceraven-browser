/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import mozilla.components.support.ktx.util.PromptAbuserDetector
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.Constants
import org.mozilla.fenix.helpers.TestAssetHelper.loremIpsumAsset
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.selectors.DownloadsSelectors
import org.mozilla.fenix.ui.efficiency.selectors.ShareOverlaySelectors

class DownloadTest : BaseTest() {

    private val mockWebServer get() = fenixTestRule.mockWebServer

    // Required for the download prompt: without it the Download button's click is treated as prompt
    // abuse and silently dropped -- the button resolves and the click reports success, but the dialog
    // stays open and no download starts. The legacy DownloadRobot.clickDownload toggles this around the
    // click for the same reason.
    @Before
    fun disablePromptAbuserDetector() {
        PromptAbuserDetector.validationsEnabled = false
    }

    @After
    fun restorePromptAbuserDetector() {
        PromptAbuserDetector.validationsEnabled = true
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2048448
    // Converted from legacy DownloadTest.saveAsPdfFunctionalityTest
    @SmokeTest
    @Test
    fun saveAsPdfFunctionalityTest() {
        val genericURL = mockWebServer.loremIpsumAsset

        on.browserPage.navigateToPage(genericURL.url.toString())
        // Save as PDF is reached through the share sheet here, not the main menu's More submenu --
        // that second route is covered by MainMenuTest.verifyTheSaveAsPDFSubMenuOptionTest.
        // SAVE_AS_PDF_LABEL, not SAVE_AS_PDF_BUTTON: the latter is UIAUTOMATOR_WITH_RES_ID with the
        // value "Save as PDF", so it resolves to the res-id "<pkg>:id/Save as PDF" and can never match.
        // It has no other callers. SAVE_AS_PDF_LABEL matches on text, like the legacy robot did.
        on.shareOverlay.navigateToPage()
            .mozClick(ShareOverlaySelectors.SAVE_AS_PDF_LABEL)
        on.downloads
            .mozVerifyElementsByGroup("downloadDialog")
            .mozClick(DownloadsSelectors.DOWNLOAD_DIALOG_CONFIRM_BUTTON)
            .mozVerify(DownloadsSelectors.DOWNLOAD_COMPLETE_SNACKBAR, timeout = 15_000)
            .mozClick(DownloadsSelectors.DOWNLOAD_SNACK_BAR_OPEN_BUTTON)
            .mozVerifyFileOpensInExternalApp(Constants.PackageName.GOOGLE_DOCS)
        // Parity gap: legacy verifyDownloadPrompt also asserted the prompt named the file ("Lorem") and
        // that the Cancel button was displayed. The downloadDialog group covers the dialog title and the
        // Download button only; there is no filename or cancel-button selector yet.
    }
}
