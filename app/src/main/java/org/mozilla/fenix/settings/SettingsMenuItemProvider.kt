/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.MenuItem
import mozilla.components.compose.menu.data.MenuItemTitle
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.store.MenuEvent
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.ui.icons.R as iconsR
import org.mozilla.fenix.NavGraphDirections
import org.mozilla.fenix.R
import org.mozilla.fenix.components.menu.MenuHost
import org.mozilla.fenix.components.menu.MenuItemProvider
import org.mozilla.fenix.components.menu.store.MenuAction

/** [MenuItemProvider] for the menu item allowing to open the application settings. */
class SettingsMenuItemProvider : MenuItemProvider {
    override val itemFlow: StateFlow<MenuItem?> =
        MutableStateFlow(
            StandardMenuItem(
                title = MenuItemTitle(Text.Resource(R.string.browser_menu_settings)),
                icon = MenuItemIconRes(iconsR.drawable.mozac_ic_settings_24),
                onClickEvent = MenuAction.Navigate.Settings,
            )
        )

    override fun handles(event: MenuEvent) = event == MenuAction.Navigate.Settings

    override fun onEvent(event: MenuEvent, menu: MenuHost) {
        menu.navigate(NavGraphDirections.actionGlobalSettingsFragment())
    }
}
