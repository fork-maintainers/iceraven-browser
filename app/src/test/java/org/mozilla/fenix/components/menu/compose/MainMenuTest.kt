/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.menu.compose

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.service.fxa.manager.AccountState.AuthenticationProblem
import mozilla.components.support.test.robolectric.testContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.mozilla.fenix.components.menu.MenuAccessPoint
import org.mozilla.fenix.components.menu.store.IPProtectionMenuState
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.Theme

@RunWith(AndroidJUnit4::class)
class MainMenuTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val customizeHomepageLabel: String
        get() = testContext.getString(R.string.browser_menu_customize_homepage)

    @Test
    fun `WHEN the access point is Home THEN the customize homepage menu item is displayed`() {
        setMainMenuContent(accessPoint = MenuAccessPoint.Home)

        composeTestRule.onNodeWithText(customizeHomepageLabel, useUnmergedTree = true).assertExists()
    }

    @Test
    fun `WHEN the access point is Browser THEN the customize homepage menu item is not displayed`() {
        setMainMenuContent(accessPoint = MenuAccessPoint.Browser)

        composeTestRule.onNodeWithText(customizeHomepageLabel, useUnmergedTree = true).assertDoesNotExist()
    }

    private fun setMainMenuContent(accessPoint: MenuAccessPoint) {
        composeTestRule.setContent {
            FirefoxTheme(theme = Theme.Light) {
                MainMenu(
                    accessPoint = accessPoint,
                    account = null,
                    accountState = AuthenticationProblem,
                    showQuitMenu = false,
                    isBottomToolbar = false,
                    isExpandedToolbarEnabled = false,
                    isSiteLoading = false,
                    isExtensionsExpanded = false,
                    isMoreMenuExpanded = false,
                    isBookmarked = false,
                    isDesktopMode = false,
                    isPdf = false,
                    isReaderViewActive = false,
                    isExtensionsProcessDisabled = false,
                    isMoreMenuHighlighted = false,
                    isAllWebExtensionsDisabled = false,
                    canGoBack = true,
                    canGoForward = true,
                    scrollState = ScrollState(0),
                    showBanner = false,
                    isDownloadHighlighted = false,
                    webExtensionMenuCount = 0,
                    showIPProtection = false,
                    ipProtectionMenuState = IPProtectionMenuState(),
                    onMoreMenuClick = {},
                    onCustomizeReaderViewMenuClick = {},
                    onMozillaAccountButtonClick = {},
                    onSettingsButtonClick = {},
                    onCustomizeHomepageButtonClick = {},
                    onBookmarkPageMenuClick = {},
                    onEditBookmarkButtonClick = {},
                    onSwitchToDesktopSiteMenuClick = {},
                    onFindInPageMenuClick = {},
                    onBannerClick = {},
                    onBannerDismiss = {},
                    onExtensionsMenuClick = {},
                    onBookmarksMenuClick = {},
                    onHistoryMenuClick = {},
                    onDownloadsMenuClick = {},
                    onPasswordsMenuClick = {},
                    onQuitMenuClick = {},
                    onBackButtonClick = {},
                    onForwardButtonClick = {},
                    onRefreshButtonClick = {},
                    onStopButtonClick = {},
                    onIPProtectionClick = {},
                    onIPProtectionNavigate = {},
                    onShareButtonClick = {},
                    extensionsMenuItemDescription = null,
                    moreSettingsSubmenu = {},
                    extensionSubmenu = {},
                )
            }
        }
    }
}
