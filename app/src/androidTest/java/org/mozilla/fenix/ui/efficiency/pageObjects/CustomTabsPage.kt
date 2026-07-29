/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.pageObjects

import android.content.Intent
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry
import org.mozilla.fenix.IntentReceiverActivity
import org.mozilla.fenix.helpers.DataGenerationHelper.createCustomTabIntent
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.PageStateTracker
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.selectors.BrowserPageSelectors
import org.mozilla.fenix.ui.efficiency.selectors.CustomTabsSelectors

class CustomTabsPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "CustomTabsPage"

    // Custom tabs are LAUNCH-reached: they run in their own activity (CustomTabActivity), started by firing
    // a custom-tabs intent at IntentReceiverActivity — not by navigating a click-path from HomePage. So there
    // is no NavigationRegistry edge; use launchCustomTab() as the entry point (it sets the page state).

    fun launchCustomTab(url: String, customMenuItemLabel: String = ""): CustomTabsPage {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = createCustomTabIntent(url, customMenuItemLabel).apply {
            setClass(context, IntentReceiverActivity::class.java)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        mozVerify(CustomTabsSelectors.MAIN_MENU_BUTTON) // wait for the custom-tab toolbar to settle
        PageStateTracker.currentPageName = pageName
        return this
    }

    fun openMainMenu(): CustomTabsPage {
        mozClick(CustomTabsSelectors.MAIN_MENU_BUTTON)
        return this
    }

    // Web content is rendered by GeckoView and matched device-wide (UIAutomator text), so the BrowserPage
    // page-content locator works inside a custom tab too.
    fun clickWebContent(text: String): CustomTabsPage {
        mozClick(BrowserPageSelectors.PAGE_CONTENT(text))
        return this
    }

    fun verifyWebContent(text: String): CustomTabsPage {
        mozVerify(BrowserPageSelectors.PAGE_CONTENT(text))
        return this
    }

    override fun mozGetSelectorsByGroup(group: String): List<Selector> {
        return CustomTabsSelectors.all.filter { it.groups.contains(group) }
    }
}
