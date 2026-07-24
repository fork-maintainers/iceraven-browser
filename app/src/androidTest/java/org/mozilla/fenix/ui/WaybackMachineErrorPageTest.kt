/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import androidx.core.net.toUri
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.FenixTestRule
import org.mozilla.fenix.helpers.HomeActivityTestRule
import org.mozilla.fenix.helpers.MatcherHelper.assertUIObjectExists
import org.mozilla.fenix.helpers.MatcherHelper.itemWithResId
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeLong
import org.mozilla.fenix.helpers.TestHelper.appContext
import org.mozilla.fenix.ui.robots.navigationToolbar
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2

/**
 * UI test for the Wayback Machine archive action on error pages.
 *
 * Only the presence of the "View archived version" button on an eligible error page is asserted:
 * the availability lookup runs in the error page's JavaScript against the live archive.org, so the
 * redirect / "no archived version found" outcomes cannot be exercised deterministically here. Those
 * paths are covered by the JavaScript in `lowMediumErrorPages.js` and, for the URL handling, by
 * `AppRequestInterceptorTest`.
 *
 * The failed host must resolve to an unknown-host error while the device is online, otherwise the
 * error type is downgraded to "no internet" (for which the archive action is intentionally not
 * offered).
 */
class WaybackMachineErrorPageTest {

    @get:Rule(order = 0)
    val fenixTestRule = FenixTestRule()

    @get:Rule(order = 1)
    val composeTestRule =
        AndroidComposeTestRuleV2(HomeActivityTestRule.withDefaultSettingsOverrides()) { it.activity }

    @Before
    fun setUp() {
        appContext.components.settings.isWaybackMachineEnabled = true
    }

    @After
    fun tearDown() {
        appContext.components.settings.isWaybackMachineEnabled = false
    }

    @Test
    fun archivedVersionButtonIsShownOnEligibleErrorPage() {
        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser("ww.example.com".toUri()) {
            waitForPageToLoad(pageLoadWaitingTime = waitingTimeLong)
            assertUIObjectExists(itemWithResId("viewArchivedButton"))
        }
    }
}
