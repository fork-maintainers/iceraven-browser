/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.SwipeDirection
import org.mozilla.fenix.ui.efficiency.selectors.ProtectionsDashboardSelectors

class ProtectionsDashboardPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) :
    BasePage(composeRule) {
    override val pageName = "ProtectionsDashboardPage"

    // Not in PageContext and no nav edge: a transient bottom-sheet overlay only reachable from the tabs-tray pill once
    // trackers have been blocked, which the graph can't express. Tests drive the entry via on.tabDrawer.

    override val selectorCatalog = ProtectionsDashboardSelectors

    /**
     * Assert the dashboard's documented elements: the "Trackers blocked this week" header, the four category rows, and
     * the "trackers blocked since <date>" footer. The shield icon (no content description) and the run-dependent weekly
     * count (merged into the header node) have no handle of their own, so the header title stands in for that section.
     */
    fun verifyDashboardElements(): ProtectionsDashboardPage {
        mozVerify(ProtectionsDashboardSelectors.DASHBOARD_CLOSE_HANDLE)
        mozVerify(ProtectionsDashboardSelectors.TRACKERS_BLOCKED_THIS_WEEK_TITLE)
        mozVerify(ProtectionsDashboardSelectors.CROSS_SITE_COOKIES_CATEGORY)
        mozVerify(ProtectionsDashboardSelectors.SOCIAL_MEDIA_TRACKERS_CATEGORY)
        mozVerify(ProtectionsDashboardSelectors.FINGERPRINTERS_CATEGORY)
        mozVerify(ProtectionsDashboardSelectors.TRACKING_CONTENT_CATEGORY)
        mozVerify(ProtectionsDashboardSelectors.TOTAL_TRACKERS_BLOCKED_SINCE_FOOTER)
        return this
    }

    /**
     * Swipe the bottom sheet down until dismissed. The drag is View-layer BottomSheetBehavior, so swipe the sheet view
     * itself (through UIAutomator), not the Compose handle.
     */
    fun swipeToDismiss(): ProtectionsDashboardPage {
        mozSwipeElementUntilAbsent(ProtectionsDashboardSelectors.DASHBOARD_SHEET, SwipeDirection.DOWN)
        return this
    }

    /**
     * Tap the scrim above the (fit-to-contents) sheet to dismiss. A coordinate tap near the top is used because
     * clicking the touch_outside view by its centre lands on the tall sheet, not the scrim.
     */
    fun tapOutsideToDismiss(): ProtectionsDashboardPage {
        mDevice.click(mDevice.displayWidth / 2, mDevice.displayHeight / 8)
        mozWaitUntilAbsent(ProtectionsDashboardSelectors.DASHBOARD_CLOSE_HANDLE)
        return this
    }
}
