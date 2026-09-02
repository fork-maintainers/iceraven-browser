/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.pageObjects

import android.content.Intent
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.uiautomator.UiSelector
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertTrue
import org.mozilla.fenix.helpers.AppAndSystemHelper.forceCloseApp
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.navigation.NavigationRegistry
import org.mozilla.fenix.ui.efficiency.navigation.NavigationStep
import org.mozilla.fenix.ui.efficiency.selectors.BrowserPageSelectors
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors
import org.mozilla.fenix.ui.efficiency.selectors.ShareOverlaySelectors

class ShareOverlayPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "ShareOverlayPage"

    init {
        // Rooted at BrowserPage, with the menu-opening click inline, because Share exists only in the
        // browser main menu. MainMenuPage is a single graph node standing for two different screens —
        // the home menu and the browser menu — and it is reachable from HomePage in one step, so an
        // edge from MainMenuPage let the planner route AppEntry -> HomePage -> MainMenuPage and then
        // look for a Share item the home menu does not have. Duplicating the menu click here is the
        // cost of that node being ambiguous; FindInPagePage and TabHistoryPage are rooted the same way.
        NavigationRegistry.register(
            from = "BrowserPage",
            to = pageName,
            steps = listOf(
                NavigationStep.Click(BrowserPageSelectors.MAIN_MENU_BUTTON),
                NavigationStep.Click(MainMenuSelectors.SHARE_BUTTON),
            ),
        )
    }

    // Reaching the share sheet means having a page to share, so an empty url still has to load one.
    override fun navigateToPage(url: String, forceNavigation: Boolean): ShareOverlayPage {
        super.navigateToPage(url = url.ifBlank { "example.com" }, forceNavigation = forceNavigation)
        return this
    }

    override fun mozGetSelectorsByGroup(group: String): List<Selector> {
        return ShareOverlaySelectors.all.filter { it.groups.contains(group) }
    }

    fun verifySharingWithSelectedApp(
        appName: String,
        appPackageName: String,
        content: String,
        subject: String,
    ): ShareOverlayPage {
        val sharingApp = mDevice.findObject(UiSelector().text(appName))
        assertTrue("Sharing app '$appName' not found on device", sharingApp.exists())
        sharingApp.clickAndWaitForNewWindow()
        val urlMatchers = content.split("\n\n").map { IntentMatchers.hasExtra(Intent.EXTRA_TEXT, containsString(it)) }
        val subjectMatchers = subject.split(", ").map { IntentMatchers.hasExtra(Intent.EXTRA_SUBJECT, containsString(it)) }
        Intents.intended(allOf(*(urlMatchers + subjectMatchers).toTypedArray()))
        forceCloseApp(appPackageName)
        return this
    }
}
