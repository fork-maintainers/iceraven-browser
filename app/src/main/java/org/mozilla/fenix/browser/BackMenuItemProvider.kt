/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.MenuItem
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.store.MenuEvent
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.compose.menu.ui.MenuItemState
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.ui.icons.R as iconsR
import org.mozilla.fenix.NavGraphDirections
import org.mozilla.fenix.R
import org.mozilla.fenix.components.menu.MenuHost
import org.mozilla.fenix.components.menu.MenuItemProvider
import org.mozilla.fenix.components.menu.MenuTarget
import org.mozilla.fenix.components.menu.store.MenuAction
import org.mozilla.fenix.ext.canGoBackInHistoryOrToStories

/**
 * [MenuItemProvider] for the menu item allowing to navigate back.
 *
 * @param browserStore [BrowserStore] used to know if the current page can navigate back.
 * @param target [MenuTarget] for which this menu item would be shown for.
 * @param goBack [SessionUseCases.GoBackUseCase] for navigating back in the current tab.
 * @param scope [CoroutineScope] used to keep the item up to date for as long as it can be shown.
 */
class BackMenuItemProvider(
    private val browserStore: BrowserStore,
    private val target: MenuTarget,
    private val goBack: SessionUseCases.GoBackUseCase,
    scope: CoroutineScope,
) : MenuItemProvider {
    override val itemFlow: StateFlow<MenuItem?> =
        browserStore.stateFlow
            .map { state ->
                target.browserSessionFrom(state)?.canGoBackInHistoryOrToStories() ?: false
            }
            .map { it.toMenuItem() }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue =
                    (target.browserSessionFrom(browserStore.state)?.canGoBackInHistoryOrToStories() ?: false)
                        .toMenuItem(),
            )

    override fun handles(event: MenuEvent) = event is MenuAction.Navigate.Back

    override fun onEvent(event: MenuEvent, menu: MenuHost) {
        if (event !is MenuAction.Navigate.Back) return
        val tabId = target.browserSessionFrom(browserStore.state)?.id ?: return

        if (event.viewHistory) {
            menu.navigate(
                NavGraphDirections.actionGlobalTabHistoryDialogFragment(
                    activeSessionId = (target as? MenuTarget.CustomTab)?.customTabId
                )
            )
        } else {
            menu.dismiss()
            goBack(tabId = tabId)
        }
    }
}

private fun Boolean.toMenuItem(): MenuItem {
    val state = if (this) MenuItemState.DEFAULT else MenuItemState.DISABLED

    return StandardMenuItem(
        title = Text.Resource(R.string.browser_menu_back),
        icon = MenuItemIconRes(iconsR.drawable.mozac_ic_back_24),
        onClickEvent = MenuAction.Navigate.Back(viewHistory = false),
        onLongClickEvent = MenuAction.Navigate.Back(viewHistory = true),
        state = state,
    )
}
