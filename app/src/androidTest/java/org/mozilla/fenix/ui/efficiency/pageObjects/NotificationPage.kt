/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.uiautomator.UiSelector
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.navigation.NavigationRegistry
import org.mozilla.fenix.ui.efficiency.navigation.NavigationStep
import org.mozilla.fenix.ui.efficiency.selectors.NotificationSelectors

class NotificationPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "NotificationPage"

    init {
        NavigationRegistry.register(
            from = "HomePage",
            to = pageName,
            steps = listOf(NavigationStep.OpenNotificationsTray),
        )
    }

    // Narrowed return type so the helpers below stay reachable when chained off navigateToPage(), which
    // otherwise hands back a BasePage. Same pattern as SettingsAutofillPage and TabHistoryPage.
    override fun navigateToPage(url: String, forceNavigation: Boolean): NotificationPage {
        super.navigateToPage(url = url, forceNavigation = forceNavigation)
        return this
    }

    /**
     * Opens the notification shade from wherever the app currently is.
     *
     * Prefer this over [navigateToPage]. The shade is a system overlay pulled down from the status bar,
     * so it is reachable from every app state — it is not a screen you route to through the nav graph.
     * The registered HomePage edge only covers one starting state, so navigateToPage() fails with "no
     * navigation path found" from anywhere else (e.g. inside a custom tab). Modelling the remaining
     * states as more graph edges would mean an edge per page for something globally reachable.
     */
    fun openNotificationTray(): NotificationPage {
        mozOpenNotificationsTray()
        try {
            mozVerify(NotificationSelectors.NOTIFICATION_SHADE)
        } catch (e: AssertionError) {
            // The shade is already open at this point, and it holds window focus. Leaving it open makes
            // everything after this fail on an unrelated error — including a retry of this test, which
            // cannot even launch the activity over it. Close it and let the real failure through.
            closeNotificationTray()
            throw e
        }
        return this
    }

    /**
     * Expands a collapsed notification so its action buttons become reachable, by swiping down on its
     * top line. Notifications in the shade are collapsed by default and their action buttons are not
     * in the hierarchy at all until expanded, so this must run before [clickNotificationActionButton].
     */
    fun expandNotification(text: String): NotificationPage {
        mozVerify(NotificationSelectors.SYSTEM_NOTIFICATION(text))
        mDevice.findObject(
            UiSelector()
                .resourceId("android:id/notification_top_line")
                .childSelector(UiSelector().textContains(text)),
        ).swipeDown(SWIPE_STEPS)
        return this
    }

    /**
     * Clicks a notification action button (e.g. "PAUSE", "RESUME", "CANCEL" on a download).
     *
     * Retries, because the shade re-lays-out as a download's state changes and a click can land while
     * the row is being rebuilt. Success is confirmed by the button no longer being present rather than
     * by the click returning — clicking PAUSE swaps the button for RESUME, so the old label going away
     * is what actually proves the action was applied.
     */
    fun clickNotificationActionButton(action: String): NotificationPage {
        val selector = NotificationSelectors.NOTIFICATION_ACTION_BUTTON(action)
        var lastError: AssertionError? = null
        repeat(ACTION_RETRIES) {
            try {
                mozVerify(selector)
                mozClick(selector)
                mozVerifyElementAbsent(selector)
                return this
            } catch (e: AssertionError) {
                lastError = e
                mDevice.waitForWindowUpdate(null, WINDOW_UPDATE_TIMEOUT)
            }
        }
        throw lastError ?: AssertionError("Could not click notification action button: $action")
    }

    fun verifyNotificationExists(text: String): NotificationPage {
        mozVerify(NotificationSelectors.SYSTEM_NOTIFICATION(text))
        return this
    }

    fun verifyNotificationDoesNotExist(text: String): NotificationPage {
        mozVerifyElementAbsent(NotificationSelectors.SYSTEM_NOTIFICATION(text))
        return this
    }

    /**
     * Closes the shade and returns the app to the foreground.
     *
     * Back is what dismisses the shade, but it does not always take, so the close is confirmed and
     * escalated to home rather than assumed. An open shade is not a recoverable state for a later
     * interaction: it keeps window focus, which blocks both Espresso and the next activity launch.
     */
    fun closeNotificationTray(): NotificationPage {
        mDevice.pressBack()
        mDevice.waitForIdle()
        if (isNotificationTrayOpen()) {
            mDevice.pressHome()
            mDevice.waitForIdle()
        }
        return this
    }

    private fun isNotificationTrayOpen(): Boolean =
        mDevice.findObject(
            UiSelector().resourceId(NotificationSelectors.NOTIFICATION_STACK_SCROLLER_RES_ID),
        ).exists() || mDevice.currentPackageName == SYSTEM_UI_PACKAGE

    override fun mozGetSelectorsByGroup(group: String): List<Selector> {
        return NotificationSelectors.all.filter { it.groups.contains(group) }
    }

    private companion object {
        const val SWIPE_STEPS = 10
        const val ACTION_RETRIES = 3
        const val WINDOW_UPDATE_TIMEOUT = 5_000L
        const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }
}
