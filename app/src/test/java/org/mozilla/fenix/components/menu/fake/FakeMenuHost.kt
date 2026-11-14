/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.menu.fake

import androidx.navigation.NavDirections
import org.mozilla.fenix.components.menu.MenuHost

/**
 * [MenuHost] remembering how it was used, failing if used more than once to close the menu or navigate away.
 *
 * @param previousScreens IDs of the screens opened before the menu, which it can navigate back to.
 */
class FakeMenuHost(private val previousScreens: Set<Int> = emptySet()) : MenuHost {
    var isDismissed = false
        private set

    var directions: NavDirections? = null
        private set

    var backDestinationId: Int? = null
        private set

    val isUsed
        get() = isDismissed || directions != null || backDestinationId != null

    override fun dismiss() {
        checkNotUsed()
        isDismissed = true
    }

    override fun navigate(directions: NavDirections) {
        checkNotUsed()
        this.directions = directions
    }

    override fun navigateBackTo(destinationId: Int): Boolean {
        checkNotUsed()
        if (destinationId in previousScreens) {
            backDestinationId = destinationId
        }
        return backDestinationId != null
    }

    private fun checkNotUsed() = check(!isUsed) { "The menu was already closed or navigated away from" }
}
