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
import org.mozilla.fenix.ui.efficiency.selectors.ShareOverlaySelectors

class ShareOverlayPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "ShareOverlayPage"

    init {
        NavigationRegistry.register(
            from = "BrowserPage",
            to = pageName,
            steps = listOf(
                // Will need to create selectors for different pages to have a nav path
            ),
        )
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
