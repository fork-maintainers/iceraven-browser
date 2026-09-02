/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.navigation.NavigationRegistry
import org.mozilla.fenix.ui.efficiency.navigation.NavigationStep
import org.mozilla.fenix.ui.efficiency.selectors.CustomTabsSelectors
import org.mozilla.fenix.ui.efficiency.selectors.ToolbarSelectors
import org.mozilla.fenix.ui.efficiency.selectors.UnifiedTrustPanelSelectors

class UnifiedTrustPanelPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "UnifiedTrustPanelPage"

    init {
        NavigationRegistry.register(
            from = "BrowserPage",
            to = pageName,
            // The site-info button's tag depends on the page's state (insecure connection / secure /
            // tracking protection off / unknown). Try each - ClickIfPresent opens the panel from
            // whichever one the page shows.
            steps = listOf(
                NavigationStep.ClickIfPresent(ToolbarSelectors.INSECURE_CONNECTION_INFORMATION_BUTTON),
                NavigationStep.ClickIfPresent(ToolbarSelectors.SECURE_SITE_INFORMATION_BUTTON),
                NavigationStep.ClickIfPresent(ToolbarSelectors.TRACKING_PROTECTION_OFF_INFORMATION_BUTTON),
                NavigationStep.ClickIfPresent(ToolbarSelectors.UNKNOWN_SITE_INFORMATION_BUTTON),
            ),
        )

        // From a custom tab, the trust panel opens via the custom-tab toolbar's "Site information" button.
        NavigationRegistry.register(
            from = "CustomTabsPage",
            to = pageName,
            steps = listOf(
                NavigationStep.Click(CustomTabsSelectors.SITE_INFO_BUTTON),
            ),
        )
    }

    override fun navigateToPage(url: String, forceNavigation: Boolean): UnifiedTrustPanelPage {
        super.navigateToPage(url = url.ifBlank { "example.com" }, forceNavigation = forceNavigation)
        return this
    }

    override fun mozGetSelectorsByGroup(group: String): List<Selector> {
        return UnifiedTrustPanelSelectors.all.filter { it.groups.contains(group) }
    }

    fun verifyTheClearCookiesAndSiteDataDialog(webSite: String): UnifiedTrustPanelPage {
        mozVerify(UnifiedTrustPanelSelectors.CLEAR_COOKIES_AND_SITE_DATA_DIALOG_TITLE)
        mozVerify(UnifiedTrustPanelSelectors.CLEAR_COOKIES_AND_SITE_DATA_DIALOG_DESCRIPTION(webSite))
        mozVerify(UnifiedTrustPanelSelectors.CLEAR_COOKIES_AND_SITE_DATA_DIALOG_CLEAR_BUTTON)
        mozVerify(UnifiedTrustPanelSelectors.CLEAR_COOKIES_AND_SITE_DATA_DIALOG_CANCEL_BUTTON)

        return this
    }

    // Shared capability: assert the full trust-panel state (site identity, ETP banner + toggle,
    // trackers-blocked option, connection security). Mirrors the legacy verifyUnifiedTrustPanelItems.
    // Reused by every quick-settings / site-security test (incl. the custom-tab variants).
    fun verifyUnifiedTrustPanelItems(
        webSite: String,
        webSiteURL: String,
        isTheWebSiteSecure: Boolean,
        isEnhancedTrackingProtectionEnabled: Boolean,
        isTrackerBlockingEnabled: Boolean,
        areTrackersBlocked: Boolean,
        shouldWebSiteURLBeDisplayed: Boolean = true,
    ): UnifiedTrustPanelPage {
        mozVerify(UnifiedTrustPanelSelectors.WEBSITE_TITLE(webSite))
        if (shouldWebSiteURLBeDisplayed) {
            mozVerify(UnifiedTrustPanelSelectors.WEBSITE_URL(webSiteURL))
        }
        verifyEnhancedTrackingProtectionState(isEnhancedTrackingProtectionEnabled, isTheWebSiteSecure)
        verifyTrackersBlockedState(isTheWebSiteSecure, isTrackerBlockingEnabled, areTrackersBlocked)
        verifySiteSecurityState(isTheWebSiteSecure)
        mozVerify(UnifiedTrustPanelSelectors.CLEAR_COOKIES_AND_SITE_DATA_BUTTON)
        mozVerify(UnifiedTrustPanelSelectors.PRIVACY_SETTINGS_LINK)
        return this
    }

    fun clickTheEnhancedTrackingProtectionOption(): UnifiedTrustPanelPage {
        mozClick(UnifiedTrustPanelSelectors.ETP_TOGGLE_LABEL)
        return this
    }

    private fun verifyEnhancedTrackingProtectionState(isEnabled: Boolean, isSecure: Boolean) {
        if (isEnabled) {
            if (isSecure) {
                mozVerify(UnifiedTrustPanelSelectors.ETP_BANNER_PROTECTED_TITLE)
            } else {
                mozVerify(UnifiedTrustPanelSelectors.ETP_BANNER_NOT_SECURE_TITLE)
                mozVerify(UnifiedTrustPanelSelectors.ETP_BANNER_NOT_SECURE_DESCRIPTION)
            }
            mozVerify(UnifiedTrustPanelSelectors.ETP_TOGGLE_LABEL)
            mozVerify(UnifiedTrustPanelSelectors.ETP_TOGGLE_ENABLED_DESCRIPTION)
            mozVerify(UnifiedTrustPanelSelectors.ETP_TOGGLE_ON)
        } else {
            if (isSecure) {
                mozVerify(UnifiedTrustPanelSelectors.ETP_BANNER_NOT_PROTECTED_TITLE)
                mozVerify(UnifiedTrustPanelSelectors.ETP_BANNER_NOT_PROTECTED_DESCRIPTION)
            } else {
                mozVerify(UnifiedTrustPanelSelectors.ETP_BANNER_NOT_SECURE_TITLE)
                mozVerify(UnifiedTrustPanelSelectors.ETP_BANNER_NOT_SECURE_DESCRIPTION)
            }
            mozVerify(UnifiedTrustPanelSelectors.ETP_TOGGLE_LABEL)
            mozVerify(UnifiedTrustPanelSelectors.ETP_TOGGLE_DISABLED_DESCRIPTION)
            mozVerify(UnifiedTrustPanelSelectors.ETP_TOGGLE_OFF)
        }
    }

    private fun verifyTrackersBlockedState(isSecure: Boolean, isTrackerBlockingEnabled: Boolean, areTrackersBlocked: Boolean) {
        if (!isTrackerBlockingEnabled) {
            mozVerify(UnifiedTrustPanelSelectors.TRACKERS_DISABLED_NONE_BLOCKED)
        } else if (isSecure) {
            if (areTrackersBlocked) {
                mozVerify(UnifiedTrustPanelSelectors.TRACKERS_BLOCKED_ON_SITE)
            } else {
                mozVerify(UnifiedTrustPanelSelectors.TRACKERS_PROTECTED_NONE_BLOCKED)
            }
        }
        // insecure + tracker-blocking enabled: legacy asserts nothing here
    }

    private fun verifySiteSecurityState(isSecure: Boolean) {
        if (isSecure) {
            mozVerify(UnifiedTrustPanelSelectors.CONNECTION_SECURE)
            mozVerify(UnifiedTrustPanelSelectors.CONNECTION_VERIFIED_BY)
        } else {
            mozVerify(UnifiedTrustPanelSelectors.CONNECTION_NOT_SECURE)
        }
    }
}
