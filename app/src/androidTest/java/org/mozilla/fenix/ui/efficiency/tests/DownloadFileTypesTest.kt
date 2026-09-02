/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import mozilla.components.support.ktx.util.PromptAbuserDetector
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.TestAssetHelper.downloadPageAsset
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest

/**
 * Verifies downloading a list of different file types:
 * - initiates a download from a web page,
 * - verifies the download prompt and the completion snackbar,
 * - verifies the file appears in the downloads manager.
 *
 * Parameterized: the runner executes [allFilesAppearInDownloadsMenuTest] once per file name, so one
 * file type failing does not mask the others.
 */
@RunWith(Parameterized::class)
class DownloadFileTypesTest(private val downloadFile: String) : BaseTest() {
    private val mockWebServer get() = fenixTestRule.mockWebServer

    companion object {
        // The test takes each file name as a parameter and runs it individually.
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun downloadList() = listOf(
            "zip_small.zip",
            "docx_document.docx",
            "mp3_audio.mp3",
            "txt_file.txt",
            "png_image.png",
            "webm_video.webm",
            "csv_file.csv",
            "xml_file.xml",
            "svg_image.svg",
        )
    }

    @Before
    fun disablePromptAbuserDetector() {
        PromptAbuserDetector.validationsEnabled = false
    }

    @After
    fun restorePromptAbuserDetector() {
        PromptAbuserDetector.validationsEnabled = true
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/251028
    @SmokeTest
    @Test
    fun allFilesAppearInDownloadsMenuTest() {
        // Deliberate deviation from legacy, which loads
        // https://storage.googleapis.com/mobile_test_assets/test_app/downloads.html. The local
        // mockWebServer asset now serves the same nine file types, so coverage is unchanged while the
        // test stops depending on external network — over the real network the download dialog and
        // the completion snackbar routinely exceeded 25s on an emulator, failing a different subset
        // of the nine on every run.
        val downloadTestPage = mockWebServer.downloadPageAsset.url.toString()

        on.browserPage.navigateToPage(downloadTestPage)
            .verifyPageContent(downloadFile)
            .clickDownloadLink(downloadFile, downloadTestPage)
            .verifyDownloadPrompt()
            .clickDownloadPromptConfirmButton()
            .verifyDownloadCompleteSnackbar(downloadFile)

        on.downloads.navigateToPage()
            .verifyDownloadedFileExistsInDownloadsList(downloadFile)

        // Legacy exited the downloads manager back to the browser as the last step; keep it so the
        // test leaves the app on the page it started from.
        on.browserPage.navigateToPage()
    }
}
