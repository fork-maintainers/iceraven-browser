/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ipprotection

import org.mozilla.fenix.ipprotection.store.IPProtectionPromptRepository

/**
 * Helps determine when the IP Protection prompt should show.
 *
 * @param repository the repository for data related to the IP Protection prompt.
 * @param currentTimeMillis provider for the current time in milliseconds, injectable for testing.
 */
class IPProtectionManager(
    private val repository: IPProtectionPromptRepository,
    private val currentTimeMillis: () -> Long = { System.currentTimeMillis() },
) {

    /**
     * Determines whether the IP Protection bottom sheet should be shown.
     *
     * @return `true` if the IP Protection bottom sheet should be shown; otherwise, `false`.
     */
    fun shouldShowIPProtectionPrompt(): Boolean =
        repository.canShowIPProtectionPrompt(currentTimeMillis())
}
