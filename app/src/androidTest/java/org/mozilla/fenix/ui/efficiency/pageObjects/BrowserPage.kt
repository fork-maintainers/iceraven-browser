/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.pageObjects

import android.util.Log
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
import org.mozilla.fenix.ui.efficiency.selectors.DownloadsSelectors
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

    /**
     * Verify page content that the page only produces after some asynchronous work has settled, reloading
     * between attempts. Needed for the tracking-protection test page, whose "<category> blocked" report is
     * written once trackers have been processed — a plain [verifyPageContent] can run before that lands and
     * no amount of waiting on the current document will make it appear.
     *
     * Mirrors the legacy BrowserRobot.verifyTrackingProtectionWebContent, which retried each assertion with
     * a page refresh for the same reason.
     */
    fun verifyPageContentWithReload(url: String, text: String, attempts: Int = 3): BrowserPage {
        for (attempt in 1..attempts) {
            try {
                return verifyPageContent(text)
            } catch (e: AssertionError) {
                if (attempt == attempts) throw e
                Log.i("BrowserPage", "verifyPageContentWithReload: '$text' absent on attempt $attempt, reloading")
                navigateToPage(url, forceNavigation = true)
            }
        }
        return this
    }

    fun verifyTranslationSheetWithReload(url: String, attempts: Int = 3): BrowserPage {
        for (attempt in 1..attempts) {
            try {
                mozVerify(BrowserPageSelectors.TRANSLATION_SHEET, timeout = waitingTimeLong)
                mozVerifyElementsByGroup("notTranslatedPageTranslationSheet")
                return this
            } catch (e: AssertionError) {
                if (attempt == attempts) throw e
                Log.i("BrowserPage", "verifyTranslationSheetWithReload: translation sheet absent on attempt $attempt, reloading")
                navigateToPage(url, forceNavigation = true)
            }
        }
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

    /**
     * Click a web-content element and wait for [expectedContent] to render, reloading [url] and
     * re-clicking between attempts. Mirrors the legacy clickPageObject retry-with-refresh: a tap can
     * land before GeckoView has wired up the page's DOM handlers, in which case the click is a silent
     * no-op and waiting on the same document never recovers it.
     */
    fun clickPageObjectUntilContent(
        selector: Selector,
        url: String,
        expectedContent: String,
        attempts: Int = 3,
    ): BrowserPage {
        for (attempt in 1..attempts) {
            mozClick(selector)
            try {
                return verifyPageContent(expectedContent)
            } catch (e: AssertionError) {
                if (attempt == attempts) throw e
                Log.i("BrowserPage", "clickPageObjectUntilContent: '$expectedContent' absent on attempt $attempt, reloading")
                navigateToPage(url, forceNavigation = true)
            }
        }
        return this
    }

    // --- Downloads from a web page ---

    /**
     * Click the download link named [fileName] and wait for the download prompt, reloading [url]
     * between attempts.
     *
     * Mirrors the legacy BrowserRobot.clickDownloadLink, which retried the click three times with a
     * page refresh in between: the link can be tapped before the page is fully interactive, in which
     * case the tap lands but no prompt opens — waiting longer on the same document never helps.
     */
    fun clickDownloadLink(fileName: String, url: String, attempts: Int = 3): BrowserPage {
        for (attempt in 1..attempts) {
            try {
                mozClick(DownloadsSelectors.DOWNLOAD_LINK(fileName))
                mozVerify(DownloadsSelectors.DOWNLOAD_DIALOG_TITLE, timeout = waitingTimeLong)
                return this
            } catch (e: AssertionError) {
                if (attempt == attempts) throw e
                Log.i("BrowserPage", "clickDownloadLink: no download prompt for '$fileName' on attempt $attempt, reloading")
                navigateToPage(url, forceNavigation = true)
            }
        }
        return this
    }

    /**
     * Assert the full download prompt, not just its title: legacy verifyDownloadPrompt checked the
     * dialog, its Cancel button and its Download button were all displayed.
     */
    fun verifyDownloadPrompt(): BrowserPage {
        mozVerify(DownloadsSelectors.DOWNLOAD_DIALOG_TITLE, timeout = waitingTimeLong)
        mozVerify(DownloadsSelectors.DOWNLOAD_DIALOG_CANCEL_BUTTON)
        mozVerify(DownloadsSelectors.DOWNLOAD_DIALOG_CONFIRM_BUTTON)
        return this
    }

    /** Confirm the download prompt, starting the download. */
    fun clickDownloadPromptConfirmButton(): BrowserPage {
        mozClick(DownloadsSelectors.DOWNLOAD_DIALOG_CONFIRM_BUTTON)
        return this
    }

    /**
     * Assert the "Download completed" snackbar for [fileName] — the completion text, the "Open"
     * action (tag and label) and the file name itself, as legacy verifyDownloadCompleteSnackbar did.
     */
    fun verifyDownloadCompleteSnackbar(fileName: String): BrowserPage {
        // The snackbar only appears once the file has actually transferred, so this waits on the
        // network, not on rendering.
        mozVerify(DownloadsSelectors.DOWNLOAD_COMPLETE_SNACKBAR, timeout = waitingTimeLong)
        mozVerify(DownloadsSelectors.DOWNLOAD_SNACK_BAR_OPEN_BUTTON)
        mozVerify(DownloadsSelectors.DOWNLOAD_SNACK_BAR_OPEN_ACTION_LABEL)
        mozVerify(DownloadsSelectors.FILE_NAME_TEXT(fileName))
        return this
    }

    fun clickSubmitLoginButton(): BrowserPage {
        mozClick(BrowserPageSelectors.SUBMIT_LOGIN_BUTTON)
        return this
    }

    fun verifySaveLoginPromptIsDisplayed(): BrowserPage {
        mozVerify(BrowserPageSelectors.SAVE_LOGIN_PROMPT)
        return this
    }

    fun verifySaveLoginPromptIsNotDisplayed(): BrowserPage {
        mozVerifyElementAbsent(BrowserPageSelectors.SAVE_LOGIN_PROMPT)
        return this
    }

    fun continueToHttpSite(): BrowserPage {
        return clickPageContent("Continue to HTTP Site")
    }

    fun verifyOpenLinkInAppPrompt(appName: String): BrowserPage {
        mozVerify(BrowserPageSelectors.OPEN_IN_APP_PROMPT(appName), timeout = waitingTimeLong)
        return this
    }

    fun clickOpenLinkInAppPromptOpenButton(): BrowserPage {
        mozClick(BrowserPageSelectors.OPEN_IN_APP_PROMPT_BUTTON)
        return this
    }

    fun clickStayInBrowserPromptButton(): BrowserPage {
        mozClick(BrowserPageSelectors.STAY_IN_FIREFOX_PROMPT_BUTTON)
        return this
    }

    /**
     * Type a username/password into the web login form. Submitting the form GETs to itself, so after
     * the first save the page reloads; typing the next set of credentials while that reload is still in
     * flight silently loses them (and the following submit then re-saves the previous login instead of
     * the new one). Each attempt therefore waits for the reloaded form to be present, enters the
     * credentials, and asserts the username actually stuck — re-entering if the reload wiped it.
     * Mirrors the legacy setPageObjectText (waitForExists + retry-with-refresh) sequence.
     */
    fun setLoginFormCredentials(username: String, password: String, attempts: Int = 4): BrowserPage {
        for (attempt in 1..attempts) {
            mozVerify(BrowserPageSelectors.USERNAME_WEB_FIELD, timeout = waitingTimeLong)
            mozClearAndEnterText(username, BrowserPageSelectors.USERNAME_WEB_FIELD)
            mozClearAndEnterText(password, BrowserPageSelectors.PASSWORD_WEB_FIELD)
            try {
                mozVerify(BrowserPageSelectors.PREFILLED_USERNAME(username), timeout = 3_000)
                return this
            } catch (e: AssertionError) {
                if (attempt == attempts) throw e
                Log.i("BrowserPage", "setLoginFormCredentials: username '$username' didn't stick on attempt $attempt, re-entering")
            }
        }
        return this
    }

    /**
     * Open the saved-login suggestions bar over the web form's username field. Focusing the field is
     * what summons the bar, but the first focus sometimes autofills the field directly without ever
     * showing it, so we re-focus and retry. Mirrors the legacy BrowserRobot.clickSuggestedLoginsButton,
     * which re-clicked the username field for the same reason.
     */
    fun clickSuggestedLoginsBar(attempts: Int = 3): BrowserPage {
        for (attempt in 1..attempts) {
            mozClick(BrowserPageSelectors.USERNAME_WEB_FIELD)
            try {
                mozVerify(BrowserPageSelectors.SUGGESTED_LOGINS_BAR, timeout = 3_000)
                mozClick(BrowserPageSelectors.SUGGESTED_LOGINS_BAR)
                return this
            } catch (e: AssertionError) {
                if (attempt == attempts) throw e
                Log.i("BrowserPage", "clickSuggestedLoginsBar: bar absent on attempt $attempt, re-focusing username")
            }
        }
        return this
    }

    fun verifyUrl(url: String, timeout: Long = waitingTimeShort): BrowserPage {
        val expectedText = url.replace("http://", "")
        val textMatcher = hasText(expectedText, substring = true, ignoreCase = true)
        try {
            composeRule.waitUntil(timeout) {
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

    // --- Address autofill on a web form ---

    /** Focus the web address form's street-address field, which triggers the autofill prompt. */
    fun clickAddressFormStreetField(): BrowserPage {
        // Android stylus handwriting pops a "Try out your stylus" dialog when a web text field is
        // focused, covering the page and suppressing the autofill prompt. Disable it for the run so
        // the tap behaves like the legacy environment; the dismiss below is a belt-and-suspenders
        // fallback in case the setting doesn't take effect before the first focus.
        runCatching { mDevice.executeShellCommand("settings put secure stylus_handwriting_enabled 0") }
        mozClick(BrowserPageSelectors.ADDRESS_STREET_WEB_FIELD)
        dismissKnownOverlaysIfPresent()
        return this
    }

    /**
     * Click the "Select address" header of the autofill prompt.
     *
     * Mirrors the legacy BrowserRobot.clickSelectAddressButton retry that works around
     * https://bugzilla.mozilla.org/show_bug.cgi?id=1816869 — the prompt sometimes fails to surface,
     * so we re-focus other form fields to re-trigger Gecko autofill and retry.
     */
    fun clickSelectAddressButton(): BrowserPage {
        for (i in 1..AUTOFILL_RETRY_COUNT) {
            // Clear any lingering blocking overlay (e.g. stylus prompt) before checking for the prompt.
            dismissKnownOverlaysIfPresent()
            try {
                mozVerify(BrowserPageSelectors.SELECT_ADDRESS_HEADER, timeout = 3_000)
                mozClick(BrowserPageSelectors.SELECT_ADDRESS_HEADER)
                return this
            } catch (e: AssertionError) {
                if (i == AUTOFILL_RETRY_COUNT) throw e
                // bug 1816869: the prompt sometimes fails to surface. Re-focus the street field to
                // re-trigger Gecko autofill (mozClick auto-dismisses a blocking overlay if one appears).
                mozClick(BrowserPageSelectors.ADDRESS_STREET_WEB_FIELD)
            }
        }
        return this
    }

    /** Pick the saved-address suggestion whose row contains [streetAddress]. */
    fun clickAddressSuggestion(streetAddress: String): BrowserPage {
        val suggestion = BrowserPageSelectors.ADDRESS_SUGGESTION(streetAddress)
        mozVerify(suggestion)
        mozClick(suggestion)
        return this
    }

    /** Assert the web street-address field is autofilled with [streetAddress]. */
    fun verifyAutofilledAddress(streetAddress: String): BrowserPage {
        mozVerify(BrowserPageSelectors.AUTOFILLED_STREET_ADDRESS(streetAddress), timeout = waitingTime)
        return this
    }

    private companion object {
        const val HTTPS_ERROR_GO_BACK = "Go Back (Recommended)"
        const val AUTOFILL_RETRY_COUNT = 3
    }
}
