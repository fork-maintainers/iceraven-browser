/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.compose.content
import mozilla.components.compose.menu.CustomizeMenuFragment
import mozilla.components.compose.menu.store.MenuState
import mozilla.components.compose.menu.store.MenuStore
import org.mozilla.fenix.theme.FirefoxTheme

/** Fenix implementation of [CustomizeMenuFragment]. */
class FenixCustomizeMenuFragment : CustomizeMenuFragment() {
    override val menuStore: MenuStore by lazy { buildMenuStore() }

    private fun buildMenuStore(): MenuStore {
        return MenuStore(initialState = MenuState(menuGroups = emptyList()))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = content {
        FirefoxTheme {}
    }
}
