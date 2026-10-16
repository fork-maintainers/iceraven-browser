/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.pageObjects

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeShort
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.navigation.NavigationArrival
import org.mozilla.fenix.ui.efficiency.navigation.NavigationGraph
import org.mozilla.fenix.ui.efficiency.selectors.HomeSelectors

class HomePage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {

    override val pageName = "HomePage"

    internal override fun registerNavigation(builder: NavigationGraph.Builder) {
        builder.register(
            from = "AppEntry",
            to = pageName,
            steps = listOf(),
            arrival = NavigationArrival.LAUNCH_REACHED,
        )
    }

    override val selectorCatalog = HomeSelectors

    /*
     * Temporary stub for the Test Factory demo.
     *
     * This method exists only to illustrate how the `SettingsPrivateBrowsingTest`
     * (and the Test Factory pattern) would toggle Private Browsing in a real page
     * object. It is **not** connected to functional UI code and should be replaced
     * with the actual implementation when Settings pages are integrated.
     *
     * The `UnsupportedOperationException` is intentional to ensure this placeholder
     * is never used in production or non-demo tests.
     */
    @Suppress("UnusedParameter")
    fun visitWebsite(url: String) {
        throw UnsupportedOperationException("visitWebsite is not supported by ${this::class.simpleName}")
    }

    /**
     * Read the title of the first popular-site suggestion in the "Add to homepage" bottom sheet. The suggestions are
     * data-driven (top-ranked Merino manifest domains minus the current top sites), so the title is unknown at
     * authoring time and must be read from the live cell. The title node lives inside the cell's merging clickable
     * container, so it is only addressable in the unmerged tree.
     */
    fun getFirstPopularSiteTitle(): String {
        composeRule.waitForIdle()
        val titleTag = HomeSelectors.ADD_TO_HOMEPAGE_BOTTOM_SHEET_POPULAR_SITES_ITEM_TITLE.value
        val node = composeRule.onAllNodesWithTag(titleTag, useUnmergedTree = true).onFirst().fetchSemanticsNode()
        return node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }
            ?: error("First popular site cell has no title text")
    }

    /**
     * Switch the homepage into private browsing mode and confirm it took effect. The homepage private/normal button is
     * a toggle, and the session may already be in either mode (state can leak from a prior test or run), so a single
     * blind click can land back on the normal homepage. Click, and if the private homepage card is not shown, toggle
     * once more, then assert it.
     */
    fun switchToPrivateBrowsingMode(): HomePage {
        for (attempt in 1..2) {
            mozClick(HomeSelectors.PRIVATE_BROWSING_BUTTON)
            try {
                mozVerify(HomeSelectors.PRIVATE_BROWSING_INFO_CARD_TITLE, timeout = waitingTimeShort)
                return this
            } catch (e: AssertionError) {
                if (attempt == 2) throw e
            }
        }
        return this
    }
}
