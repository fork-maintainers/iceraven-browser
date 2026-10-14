/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.menu

import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.nav

/** Collection of menu related operations. */
interface MenuHost {
    /** Close the menu. */
    fun dismiss()

    /** Close the menu and opens the screen [directions] lead to. */
    fun navigate(directions: NavDirections)

    /**
     * Close the menu and go back to the [destinationId] screen if it was opened before.
     *
     * @return Whether the [destinationId] screen was found and shown, otherwise the menu stays open.
     */
    fun navigateBackTo(@IdRes destinationId: Int): Boolean
}

/**
 * Default handling of the menu related operations.
 *
 * @param navController [NavController] for closing the menu or navigating away from it.
 */
internal class DefaultMenuHost(private val navController: NavController) : MenuHost {
    override fun dismiss() {
        navController.popBackStack(R.id.menuFragment, true)
    }

    override fun navigate(directions: NavDirections) {
        navController.nav(
            id = R.id.menuFragment,
            directions = directions,
            navOptions = NavOptions.Builder().setPopUpTo(R.id.menuFragment, true).build(),
        )
    }

    override fun navigateBackTo(@IdRes destinationId: Int) = navController.popBackStack(destinationId, false)
}
