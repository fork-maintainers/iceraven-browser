/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeLong
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.selectors.BrowserPageSelectors
import org.mozilla.fenix.ui.efficiency.selectors.ToolbarSelectors

class PDFViewerTest : BaseTest() {

    private val mockWebServer get() = fenixTestRule.mockWebServer

    // Converted from legacy PDFViewerTest.verifyPDFFileIsOpenedInTheSameTabTest
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2048140
    @SmokeTest
    @Test
    fun verifyPDFFileIsOpenedInTheSameTabTest() {
        val genericURL = mockWebServer.getGenericAsset(3)

        on.browserPage.navigateToPage(genericURL.url.toString())
        on.browserPage.mozClick(BrowserPageSelectors.PAGE_CONTENT("PDF form file"))
        // The applinks "Stay in Firefox" prompt only appears when the device has an external PDF handler,
        // so click it only if present (legacy clicked it unconditionally).
        on.browserPage.mozClickIfPresent(BrowserPageSelectors.STAY_IN_FIREFOX_PROMPT_BUTTON, timeout = waitingTime)
        on.browserPage.mozVerify(
            BrowserPageSelectors.PAGE_CONTENT("Washington Crossing the Delaware"),
            timeout = waitingTimeLong,
        )
        on.browserPage.mozVerify(ToolbarSelectors.TAB_COUNTER_WITH_COUNT("1"))
    }
}
