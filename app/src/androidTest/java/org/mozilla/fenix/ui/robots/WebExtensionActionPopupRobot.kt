/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.robots

import android.util.Log
import androidx.compose.ui.test.junit4.ComposeTestRule
import org.mozilla.fenix.helpers.Constants.TAG
import org.mozilla.fenix.helpers.MatcherHelper.assertUIObjectExists
import org.mozilla.fenix.helpers.MatcherHelper.itemContainingText
import org.mozilla.fenix.helpers.MatcherHelper.itemWithResId
import org.mozilla.fenix.helpers.TestHelper.packageName

/**
 * Robot for interacting with a web extension browserAction/pageAction popup.
 */
class WebExtensionActionPopupRobot(private val composeTestRule: ComposeTestRule) {
    fun verifyPopupContent(expectedText: String) {
        // Relevant implementation detail: the popup is displayed in the same
        // view that is shared with the options page in the extension settings.
        // See addonSettingsEngineView in WebExtensionActionPopupFragment.kt
        Log.i(TAG, "verifyPopupContent: Waiting for the popup view")
        assertUIObjectExists(itemWithResId("$packageName:id/addonSettingsEngineView"))
        Log.i(TAG, "verifyPopupContent: Waiting for popup to contain: $expectedText")
        assertUIObjectExists(itemContainingText(expectedText))
        Log.i(TAG, "verifyPopupContent: Verified that popup contains: $expectedText")
    }
}

/**
 * Enters the web extension action popup robot scope for assertions and interactions.
 */
fun webExtensionActionPopup(
    composeTestRule: ComposeTestRule,
    interact: WebExtensionActionPopupRobot.() -> Unit,
): WebExtensionActionPopupRobot {
    val robot = WebExtensionActionPopupRobot(composeTestRule)
    robot.interact()
    return robot
}
