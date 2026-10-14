/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.downloads

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.MenuItem
import mozilla.components.compose.menu.data.MenuItemTitle
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.store.MenuEvent
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.ui.icons.R as iconsR
import org.mozilla.fenix.NavGraphDirections
import org.mozilla.fenix.R
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.SupportedMenuNotifications
import org.mozilla.fenix.components.menu.MenuHost
import org.mozilla.fenix.components.menu.MenuItemProvider
import org.mozilla.fenix.components.menu.store.MenuAction

/**
 * [MenuItemProvider] for the menu item allowing to open the downloads screen.
 *
 * @param appStore [AppStore] to observe the downloads notification status.
 * @param scope [CoroutineScope] used to keep the item up to date for as long as it can be shown.
 */
class DownloadsMenuItemProvider(
    appStore: AppStore,
    scope: CoroutineScope,
) : MenuItemProvider {
    override val itemFlow: StateFlow<MenuItem?> =
        appStore.stateFlow
            .map { it.supportedMenuNotifications.contains(SupportedMenuNotifications.Downloads) }
            .distinctUntilChanged()
            .map { isHighlighted -> createMenuItem(isHighlighted) }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue =
                    createMenuItem(
                        appStore.state.supportedMenuNotifications.contains(SupportedMenuNotifications.Downloads)
                    ),
            )

    private fun createMenuItem(isHighlighted: Boolean) =
        StandardMenuItem(
            title = MenuItemTitle(Text.Resource(R.string.library_downloads)),
            icon =
                MenuItemIconRes(
                    iconsR.drawable.mozac_ic_download_24,
                    isHighlighted = isHighlighted,
                ),
            onClickEvent = MenuAction.Navigate.Downloads,
        )

    override fun handles(event: MenuEvent) = event == MenuAction.Navigate.Downloads

    override fun onEvent(event: MenuEvent, menu: MenuHost) {
        menu.navigate(NavGraphDirections.actionGlobalDownloadsFragment())
    }
}
