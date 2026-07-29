/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.pageObjects

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import mozilla.components.compose.browser.toolbar.concept.BrowserToolbarTestTags.ADDRESSBAR_URL
import org.junit.Assert.assertTrue
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeLong
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeShort
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.helpers.TestHelper.packageName
import org.mozilla.fenix.helpers.ext.waitNotNull
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.navigation.NavigationRegistry
import org.mozilla.fenix.ui.efficiency.navigation.NavigationStep
import org.mozilla.fenix.ui.efficiency.selectors.BrowserPageSelectors
import org.mozilla.fenix.ui.efficiency.selectors.HomeSelectors
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors
import org.mozilla.fenix.ui.efficiency.selectors.SearchBarSelectors
import org.mozilla.fenix.ui.efficiency.selectors.ToolbarSelectors

class BrowserPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "BrowserPage"

    init {
        NavigationRegistry.register(
            from = "HomePage",
            to = pageName,
            steps = listOf(
                NavigationStep.Click(ToolbarSelectors.TOOLBAR_URL_BOX),
                NavigationStep.EnterText(SearchBarSelectors.TOOLBAR_IN_EDIT_MODE),
                NavigationStep.PressEnter(SearchBarSelectors.TOOLBAR_IN_EDIT_MODE),
            ),
        )

        // Use UIAutomator selector to avoid Compose sync hanging when GeckoView is active.
        NavigationRegistry.register(
            from = pageName,
            to = pageName,
            steps = listOf(
                NavigationStep.Click(ToolbarSelectors.TOOLBAR_URL_BOX_UIAUTOMATOR),
                NavigationStep.EnterText(SearchBarSelectors.TOOLBAR_IN_EDIT_MODE),
                NavigationStep.PressEnter(SearchBarSelectors.TOOLBAR_IN_EDIT_MODE),
            ),
        )

        NavigationRegistry.register(
            from = "SearchBarComponent",
            to = pageName,
            steps = listOf(
                NavigationStep.EnterText(SearchBarSelectors.TOOLBAR_IN_EDIT_MODE),
                NavigationStep.PressEnter(SearchBarSelectors.TOOLBAR_IN_EDIT_MODE),
            ),
        )

        // Use UIAutomator selector to avoid Compose sync hanging when GeckoView is active.
        NavigationRegistry.register(
            from = pageName,
            to = "MainMenuPage",
            steps = listOf(NavigationStep.Click(HomeSelectors.MAIN_MENU_BUTTON_UIAUTOMATOR)),
        )

        // Use UIAutomator selector to avoid Compose sync hanging when GeckoView is active.
        NavigationRegistry.register(
            from = pageName,
            to = "HomePage",
            steps = listOf(
                NavigationStep.Click(ToolbarSelectors.NEW_TAB_BUTTON),
                NavigationStep.PressBack,
            ),
        )

        NavigationRegistry.register(
            from = pageName,
            to = "AddToHomeScreenComponent",
            steps = listOf(
                NavigationStep.Click(BrowserPageSelectors.MAIN_MENU_BUTTON),
                NavigationStep.Click(MainMenuSelectors.MORE_BUTTON),
                NavigationStep.Click(MainMenuSelectors.ADD_TO_HOMESCREEN_BUTTON),
            ),
        )

        // UIAutomator (Compose sync can hang while GeckoView is active) AND the content-description
        // variant rather than the testTag: with shouldUseExpandedToolbar the counter moves to the bottom
        // navigation bar, where it carries no tag. NOTE: TabDrawerPage registers this same edge — keep the
        // two in step until the duplicate is removed.
        NavigationRegistry.register(
            from = pageName,
            to = "TabDrawerPage",
            steps = listOf(NavigationStep.Click(ToolbarSelectors.TAB_COUNTER_ANY_LAYOUT)),
        )
    }

    override fun navigateToPage(url: String, forceNavigation: Boolean): BrowserPage {
        super.navigateToPage(url = url.ifBlank { "example.com" }, forceNavigation = forceNavigation)
        return this
    }

    override fun mozGetSelectorsByGroup(group: String): List<Selector> {
        return BrowserPageSelectors.all.filter { it.groups.contains(group) }
    }

    fun verifyPageContent(text: String): BrowserPage {
        mDevice.waitNotNull(
            Until.findObject(By.res("$packageName:id/engineView")),
            waitingTime,
        )
        assertTrue(
            mDevice.wait(Until.findObject(By.textContains(text)), waitingTimeLong) != null,
        )
        return this
    }

    fun verifyHttpsOnlyErrorPage(): BrowserPage {
        return verifyPageContent("Secure site not available")
            .verifyPageContent("Most likely, the website simply does not support HTTPS.")
            .verifyPageContent("HTTPS-Only mode will be turned off temporarily")
            .verifyPageContent(HTTPS_ERROR_GO_BACK)
    }

    fun goBackFromHttpsError(): BrowserPage {
        return clickPageContent(HTTPS_ERROR_GO_BACK)
            .clickPageContentIfPresent(HTTPS_ERROR_GO_BACK)
    }

    fun clickPageContent(text: String): BrowserPage {
        mozClick(BrowserPageSelectors.PAGE_CONTENT(text))
        return this
    }

    fun clickPageContentIfPresent(text: String): BrowserPage {
        mozClickIfPresent(BrowserPageSelectors.PAGE_CONTENT(text))
        return this
    }

    fun continueToHttpSite(): BrowserPage {
        return clickPageContent("Continue to HTTP Site")
    }

    fun verifyUrl(url: String): BrowserPage {
        val expectedText = url.replace("http://", "")
        val textMatcher = hasText(expectedText, substring = true, ignoreCase = true)
        try {
            composeRule.waitUntil(waitingTimeShort) {
                composeRule.onAllNodesWithTag(ADDRESSBAR_URL, useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .any { textMatcher.matches(it) }
            }
        } catch (_: ComposeTimeoutException) {
            val actual = composeRule.onAllNodesWithTag(ADDRESSBAR_URL, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .mapNotNull { it.config.getOrNull(SemanticsProperties.Text)?.joinToString("") }
            throw AssertionError("Expected URL to contain '$expectedText' but found: $actual")
        }
        return this
    }

    fun openMainMenu(): BrowserPage {
        mozClick(BrowserPageSelectors.MAIN_MENU_BUTTON)

        return this
    }

    private companion object {
        const val HTTPS_ERROR_GO_BACK = "Go Back (Recommended)"
    }
}
