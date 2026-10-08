/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.navigation.NavigationGraph
import org.mozilla.fenix.ui.efficiency.navigation.NavigationStep
import org.mozilla.fenix.ui.efficiency.selectors.SettingsPrivateBrowsingSelectors
import org.mozilla.fenix.ui.efficiency.selectors.SettingsSelectors

class SettingsPrivateBrowsingPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) :
    BasePage(composeRule) {
    override val pageName = "SettingsPrivateBrowsingPage"

    internal override fun registerNavigation(builder: NavigationGraph.Builder) {
        // This page intentionally has no direct HomePage route. The planner would prefer it to the Settings-hub
        // route even though its New tab action is unavailable from a private browser.
        builder.register(
            from = pageName,
            to = "SettingsPage",
            steps = listOf(NavigationStep.Click(SettingsSelectors.GO_BACK_BUTTON)),
        )
    }

    override val selectorCatalog = SettingsPrivateBrowsingSelectors

    fun toggleAllowScreenshotsInPrivateBrowsing(): SettingsPrivateBrowsingPage {
        mozClick(SettingsPrivateBrowsingSelectors.ALLOW_SCREENSHOTS_IN_PRIVATE_BROWSING)
        return this
    }

    fun toggleOpenLinksInAPrivateTab(): SettingsPrivateBrowsingPage {
        mozClick(SettingsPrivateBrowsingSelectors.OPEN_LINKS_IN_PRIVATE_TAB)
        return this
    }

    /**
     * NOTE: Temporary stub for the Test Factory demo.
     *
     * This method exists only to illustrate how the `SettingsPrivateBrowsingTest` (and the Test Factory pattern) would
     * toggle Private Browsing in a real page object. It is **not** connected to functional UI code and should be
     * replaced with the actual implementation when Settings pages are integrated.
     *
     * The `UnsupportedOperationException` is intentional to ensure this placeholder is never used in production or
     * non-demo tests.
     */
    @Suppress("UnusedParameter")
    fun setPrivateBrowsing(on: Boolean) {
        throw UnsupportedOperationException("setPrivateBrowsing is not supported by ${this::class.simpleName}")
    }
}
