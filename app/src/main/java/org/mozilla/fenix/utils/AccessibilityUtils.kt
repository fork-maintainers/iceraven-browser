/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.utils

import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.Toolbar
import androidx.core.view.children
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R

/**
 * General utilities to to improve a11y support, such as managing screen reader focus.
 */
object AccessibilityUtils {

    private val debouncer = AnnouncementDebouncer()

    /**
     * This function attempts to move focus to the back button on the navigation bar as a generic
     * default location to send focus.
     *
     * Only use this function when focus should be specifically controlled due to TalkBack focus not
     * consistently setting or other inconsistent behavior.
     *
     * Caution, focus should only be manually controlled in certain situations. Be sure this is the
     * ideal solution.
     *
     * @param activity The activity that focus should move to.
     */
    fun moveFocusToBackNavButton(activity: AppCompatActivity) {
        val toolbar = (activity as HomeActivity).findViewById<Toolbar>(R.id.navigationToolbar)
        var backNavigationView: AppCompatImageButton? = null
        for (child in toolbar.children) {
            if (child is AppCompatImageButton) {
                backNavigationView = child
            }
        }

        // Ensures the back button is focusable
        backNavigationView?.isFocusableInTouchMode = true
        // Clear current focus
        backNavigationView?.clearFocus()
        backNavigationView?.requestFocus()
        // Removes depressed UI state from back button
        backNavigationView?.clearFocus()
    }

    /**
     * Sends an accessibility event. The announcement is only triggered after a minimum time interval.
     */
    fun View.announcePrivateModeForAccessibility() = debouncer.debounce {
        // Using the deprecated method instead of recommended setStateDescription()
        // due to limited support when called on binding.root
        @Suppress("Deprecation")
        announceForAccessibility(
            context.getString(R.string.private_browsing_a11y_session_announcement),
        )
    }
}

/**
 * Debounces accessibility announcements so they fire at most once per [debounce] delay window.
 *
 * @param currentTimeMillis provider for the current time in milliseconds, injectable for testing.
 */
@VisibleForTesting
internal class AnnouncementDebouncer(
    private val currentTimeMillis: () -> Long = { System.currentTimeMillis() },
) {
    private var lastAnnouncementTime = 0L

    /**
     * Executes the given [action] only if the time since the last invocation is at least [delay].
     *
     * @param delay Minimum interval in milliseconds between allowed executions.
     * @param action The action to execute.
     */
    fun debounce(delay: Long = 2000, action: () -> Unit) {
        val currentTime = currentTimeMillis()
        if (currentTime - lastAnnouncementTime >= delay) {
            lastAnnouncementTime = currentTime
            action()
        }
    }
}
