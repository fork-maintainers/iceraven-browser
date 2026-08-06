/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.uiautomator.UiSelector
import mozilla.components.compose.browser.toolbar.concept.BrowserToolbarTestTags.NAVIGATION_BAR
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.helpers.TestHelper.packageName
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.navigation.NavigationRegistry
import org.mozilla.fenix.ui.efficiency.selectors.ToolbarSelectors

class ToolbarComponent(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "ToolbarComponent"

    init {
        NavigationRegistry.register(
            from = "HomePage",
            to = pageName,
            steps = listOf(),
        )

        NavigationRegistry.register(
            from = "BrowserPage",
            to = pageName,
            steps = listOf(),
        )
    }

    override fun mozGetSelectorsByGroup(group: String): List<Selector> {
        return ToolbarSelectors.all.filter { it.groups.contains(group) }
    }

    /**
     * Assert the address-bar toolbar sits in the top half of the screen. With shouldUseExpandedToolbar the
     * toolbar is at the bottom in portrait, but moves to the top in landscape and when the tab strip is
     * enabled. Ports the legacy HomeScreenRobot.verifyToolbarPosition(bottomPosition = false) geometry.
     */
    fun verifyToolbarIsAtTop(): ToolbarComponent {
        val toolbar = mDevice.findObject(UiSelector().resourceId("$packageName:id/composable_toolbar"))
        if (!toolbar.waitForExists(TestAssetHelper.waitingTime)) {
            throw AssertionError("Toolbar must be present in the view hierarchy")
        }
        if (toolbar.visibleBounds.centerY() >= mDevice.displayHeight / 2) {
            throw AssertionError("Toolbar should be positioned at the top of the screen")
        }
        return this
    }

    /**
     * Assert the bottom navigation bar sits below the address-bar toolbar. Ports the legacy
     * NavigationToolbarRobot.assertNavBarIsPositioned(referenceResourceId = composable_toolbar).
     */
    fun verifyNavBarIsAtBottom(): ToolbarComponent {
        val navBarBounds = composeRule.onNodeWithTag(NAVIGATION_BAR).fetchSemanticsNode().boundsInWindow
        val reference = mDevice.findObject(UiSelector().resourceId("$packageName:id/composable_toolbar"))
        if (!reference.waitForExists(TestAssetHelper.waitingTime)) {
            throw AssertionError("Reference toolbar view must be present in the view hierarchy")
        }
        if (navBarBounds.top < reference.visibleBounds.bottom) {
            throw AssertionError("Navigation bar should be positioned below the toolbar")
        }
        return this
    }
}
