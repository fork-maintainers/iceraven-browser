/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.shortcut

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.MenuItem
import mozilla.components.compose.menu.data.MenuItemTitle
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.store.MenuEvent
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.feature.pwa.WebAppUseCases
import mozilla.components.ui.icons.R as iconsR
import org.mozilla.fenix.R
import org.mozilla.fenix.components.menu.MenuFragmentDirections
import org.mozilla.fenix.components.menu.MenuHost
import org.mozilla.fenix.components.menu.MenuItemProvider
import org.mozilla.fenix.components.menu.MenuTarget
import org.mozilla.fenix.components.menu.store.MenuAction
import org.mozilla.fenix.utils.Settings

/**
 * [MenuItemProvider] for the menu item allowing to add the current webpage as a shortcut on the device's home screen.
 *
 * @param browserStore [BrowserStore] used to get information about the current webpage.
 * @param target [MenuTarget] for which this menu item would be shown for.
 * @param webAppUseCases [WebAppUseCases] used to know whether and how that webpage can be added to the home screen, and
 *   to add it.
 * @param settings [Settings] for remembering that the user added a page to the home screen.
 * @param scope [CoroutineScope] used to keep the item up to date for as long as it can be shown, and to add the page to
 *   the home screen.
 */
class AddToHomeScreenMenuItemProvider(
    private val browserStore: BrowserStore,
    private val target: MenuTarget,
    private val webAppUseCases: WebAppUseCases,
    private val settings: Settings,
    private val scope: CoroutineScope,
) : MenuItemProvider {
    // Whether the launcher accepts shortcuts at all is a property of the device, so it cannot change while the menu
    // is shown.
    private val isPinningSupported = webAppUseCases.isPinningSupported()

    override val itemFlow: StateFlow<MenuItem?> =
        browserStore.stateFlow
            .map { state -> addToHomeScreenItem(hasPageShown = target.browserSessionFrom(state) != null) }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue =
                    addToHomeScreenItem(hasPageShown = target.browserSessionFrom(browserStore.state) != null),
            )

    /**
     * The item to show, or `null` if there is no page to add or the device cannot show shortcuts.
     *
     * A page offering a web app manifest is installed as a PWA rather than added as a simple shortcut.
     */
    private fun addToHomeScreenItem(hasPageShown: Boolean): MenuItem? {
        if (!hasPageShown || !isPinningSupported) return null

        return StandardMenuItem(
            title =
                MenuItemTitle(
                    Text.Resource(
                        when (webAppUseCases.isInstallable()) {
                            true -> R.string.browser_menu_add_app_to_homescreen
                            else -> R.string.browser_menu_add_to_homescreen
                        }
                    )
                ),
            icon = MenuItemIconRes(iconsR.drawable.mozac_ic_add_to_homescreen_24),
            onClickEvent = MenuAction.Navigate.AddToHomeScreen,
        )
    }

    override fun handles(event: MenuEvent) = event == MenuAction.Navigate.AddToHomeScreen

    /**
     * A page offering a web app manifest is installed as a PWA right away. Any other is added as a simple shortcut, for
     * which the user is first asked to confirm the name it will have on the home screen.
     */
    override fun onEvent(event: MenuEvent, menu: MenuHost) {
        if (target.browserSessionFrom(browserStore.state) == null) return

        settings.installPwaOpened = true

        when (webAppUseCases.isInstallable()) {
            true ->
                scope.launch {
                    webAppUseCases.addToHomescreen()
                    menu.dismiss()
                }
            false -> menu.navigate(MenuFragmentDirections.actionMenuFragmentToCreateShortcutFragment())
        }
    }
}
