/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.termsofuse

import androidx.annotation.VisibleForTesting
import org.mozilla.fenix.termsofuse.store.TermsOfUsePromptRepository

/**
 * Helps determine when the Terms of Use prompt should show.
 *
 * @param repository the repository for data related to the Terms of Use prompt.
 * @param currentTimeMillis provider for the current time in milliseconds, injectable for testing.
 */
class TermsOfUseManager(
    private val repository: TermsOfUsePromptRepository,
    private val currentTimeMillis: () -> Long = { System.currentTimeMillis() },
) {

    private var isFirstCheckSinceAppStart: Boolean = false

    /**
     * Determines whether the Terms of Use bottom sheet should be shown on the homepage.
     */
    fun shouldShowTermsOfUsePromptOnHomepage() =
        shouldShowTermsOfUsePrompt(ignoreFirstCheckSinceAppStart = true)

    /**
     * Determines whether the Terms of Use bottom sheet should be shown in the browser fragment.
     */
    fun shouldShowTermsOfUsePromptOnBrowserFragment() = shouldShowTermsOfUsePrompt()

    /**
     * Determines whether the Terms of Use bottom sheet should be shown.
     *
     * @param ignoreFirstCheckSinceAppStart Used to check whether the app start check is required.
     * @param currentTimeInMillis The current time in milliseconds.
     *
     * @return `true` if the Terms of Use bottom sheet should be shown; otherwise, `false`.
     */
    @VisibleForTesting
    internal fun shouldShowTermsOfUsePrompt(
        ignoreFirstCheckSinceAppStart: Boolean = false,
        currentTimeInMillis: Long = currentTimeMillis(),
    ): Boolean = repository.canShowTermsOfUsePrompt() &&
            !repository.userPostponedAndWithinCooldownPeriod(currentTimeInMillis) &&
            isFirstCheckFromAppStart(ignoreFirstCheckSinceAppStart)

    /**
     * This is the first time checking to see if we should show the prompt since starting the app
     * OR the [ignore] flag is true (we should ignore this when checking from homepage).
     */
    private fun isFirstCheckFromAppStart(ignore: Boolean): Boolean {
        val isFirstCheck = isFirstCheckSinceAppStart
        isFirstCheckSinceAppStart = false
        return ignore || isFirstCheck
    }

    /**
     * Called from the [org.mozilla.fenix.HomeActivity]'s onStart. Used to track the first check
     * since starting the app.
     */
    fun onStart() {
        isFirstCheckSinceAppStart = true
    }
}
