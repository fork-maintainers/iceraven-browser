/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.ui.fab

import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mozilla.components.compose.base.button.FloatingActionButton
import mozilla.components.compose.base.button.FloatingActionButtonDefaults
import mozilla.components.compose.base.button.IconButton
import mozilla.components.compose.base.button.TextButton
import mozilla.components.compose.base.menu.DropdownMenu
import mozilla.components.compose.base.menu.MenuItem
import mozilla.components.compose.base.modifier.animateRotation
import mozilla.components.compose.base.text.Text
import org.mozilla.fenix.R
import org.mozilla.fenix.tabstray.TabsTrayTestTag
import org.mozilla.fenix.tabstray.data.createTab
import org.mozilla.fenix.tabstray.redux.action.TabGroupAction
import org.mozilla.fenix.tabstray.redux.action.TabsTrayAction
import org.mozilla.fenix.tabstray.redux.state.Page
import org.mozilla.fenix.tabstray.redux.state.TabsTrayState
import org.mozilla.fenix.tabstray.redux.store.TabsTrayStore
import org.mozilla.fenix.tabstray.syncedtabs.SyncedTabsListItem
import org.mozilla.fenix.theme.FirefoxTheme
import androidx.compose.material3.FloatingActionButtonDefaults as M3FloatingActionButtonDefaults
import mozilla.components.ui.icons.R as iconsR

/**
 * Floating Toolbar for the Tab Manager.
 *
 * @param state The current snapshot of [TabsTrayState].
 * @param modifier The [Modifier] to be applied to this FAB.
 * @param onAction Invoked to pass upwards a [TabsTrayAction] in response to a UI event.
 * @param onOpenNewNormalTabClicked Invoked when the fab is clicked in [Page.NormalTabs].
 * @param onOpenNewPrivateTabClicked Invoked when the fab is clicked in [Page.PrivateTabs].
 * @param onSyncedTabsFabClicked Invoked when the fab is clicked in [Page.SyncedTabs].
 * @param onTabSettingsClick Invoked when the user clicks on the tab settings banner menu item.
 * @param onRecentlyClosedClick Invoked when the user clicks on the recently closed banner menu item.
 * @param onAccountSettingsClick Invoked when the user clicks on the account settings banner menu item.
 * @param onDeleteAllTabsClick Invoked when the user clicks on the close all tabs banner menu item.
 */
@Suppress("LongParameterList")
@Composable
internal fun TabManagerFloatingToolbar(
    state: TabsTrayState,
    modifier: Modifier = Modifier,
    onAction: (TabsTrayAction) -> Unit,
    onOpenNewNormalTabClicked: () -> Unit,
    onOpenNewPrivateTabClicked: () -> Unit,
    onSyncedTabsFabClicked: () -> Unit,
    onTabSettingsClick: () -> Unit,
    onRecentlyClosedClick: () -> Unit,
    onAccountSettingsClick: () -> Unit,
    onDeleteAllTabsClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = state.isFloatingToolbarVisible,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = FirefoxTheme.layout.space.static200),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (state.selectedPage != Page.TabGroups) {
                    FloatingToolbarActions(
                        state = state,
                        onMenuShown = {
                            onAction(TabsTrayAction.ThreeDotMenuShown)
                        },
                        onEnterMultiselectModeClick = {
                            onAction(TabsTrayAction.EnterSelectMode)
                        },
                        onSelectAllTabsClick = {
                            onAction(TabsTrayAction.SelectAllNormalTabs)
                        },
                        onTabSettingsClick = onTabSettingsClick,
                        onRecentlyClosedClick = onRecentlyClosedClick,
                        onNewTabGroupClick = {
                            onAction(TabGroupAction.NewTabGroupMenuClicked)
                        },
                        onAccountSettingsClick = onAccountSettingsClick,
                        onDeleteAllTabsClick = onDeleteAllTabsClick,
                        onSearchClicked = {
                            onAction(TabsTrayAction.TabSearchClicked)
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.width(FirefoxTheme.layout.space.static100))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd,
            ) {
                FloatingToolbarFAB(
                    state = state,
                    onOpenNewNormalTabClicked = onOpenNewNormalTabClicked,
                    onOpenNewPrivateTabClicked = onOpenNewPrivateTabClicked,
                    onSyncedTabsFabClicked = onSyncedTabsFabClicked,
                    onTabGroupsFabClicked = {
                        onAction(TabGroupAction.NewTabGroupFabClicked)
                    },
                )
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun FloatingToolbarActions(
    state: TabsTrayState,
    onMenuShown: () -> Unit,
    onEnterMultiselectModeClick: () -> Unit,
    onSelectAllTabsClick: () -> Unit,
    onTabSettingsClick: () -> Unit,
    onRecentlyClosedClick: () -> Unit,
    onNewTabGroupClick: () -> Unit,
    onAccountSettingsClick: () -> Unit,
    onDeleteAllTabsClick: () -> Unit,
    onSearchClicked: () -> Unit,
) {
    var showBottomAppBarMenu by remember { mutableStateOf(false) }
    var showCloseAllTabsDialog by remember { mutableStateOf(false) }

    val menuItems = generateMenuItems(
        selectedPage = state.selectedPage,
        normalTabCount = state.normalTabsState.items.size,
        privateTabCount = state.privateBrowsing.tabs.size,
        homepageAsNewTabEnabled = state.config.homepageAsNewTabEnabled,
        onAccountSettingsClick = onAccountSettingsClick,
        onTabSettingsClick = onTabSettingsClick,
        onRecentlyClosedClick = onRecentlyClosedClick,
        onNewTabGroupClick = onNewTabGroupClick,
        onEnterMultiselectModeClick = onEnterMultiselectModeClick,
        onSelectAllTabsClick = onSelectAllTabsClick,
        onDeleteAllTabsClick = { showCloseAllTabsDialog = true },
    )

    Card(
        modifier = Modifier.height(56.dp),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
    ) {
        Row(
            modifier = Modifier.padding(all = FirefoxTheme.layout.space.static100),
            horizontalArrangement = Arrangement.spacedBy(FirefoxTheme.layout.space.static50),
        ) {
            if (state.searchIconVisible) {
                IconButton(
                    onClick = onSearchClicked,
                    contentDescription = stringResource(id = R.string.tab_manager_open_tab_search),
                    modifier = Modifier.testTag(TabsTrayTestTag.TAB_SEARCH_ICON),
                    enabled = state.searchIconEnabled,
                ) {
                    Icon(
                        painter = painterResource(iconsR.drawable.mozac_ic_search_24),
                        contentDescription = null,
                    )
                }
            }

            IconButton(
                onClick = {
                    onMenuShown()
                    showBottomAppBarMenu = true
                },
                contentDescription = stringResource(id = R.string.open_tabs_menu),
                modifier = Modifier.testTag(TabsTrayTestTag.THREE_DOT_BUTTON),
            ) {
                Icon(
                    painter = painterResource(iconsR.drawable.mozac_ic_ellipsis_vertical_24),
                    contentDescription = null,
                )

                DropdownMenu(
                    menuItems = menuItems,
                    expanded = showBottomAppBarMenu,
                    onDismissRequest = { showBottomAppBarMenu = false },
                )
            }
        }
    }

    if (showCloseAllTabsDialog) {
        CloseAllTabsConfirmationDialog(
            onConfirm = {
                showCloseAllTabsDialog = false
                onDeleteAllTabsClick()
            },
            onDismiss = { showCloseAllTabsDialog = false },
        )
    }
}

@VisibleForTesting
@Composable
internal fun FloatingToolbarFAB(
    state: TabsTrayState,
    onOpenNewNormalTabClicked: () -> Unit,
    onOpenNewPrivateTabClicked: () -> Unit,
    onSyncedTabsFabClicked: () -> Unit,
    onTabGroupsFabClicked: () -> Unit,
) {
    val isSyncDisabled = !state.sync.isSignedIn || state.sync.syncedTabs.any {
        it is SyncedTabsListItem.Error && it.errorText == stringResource(
            id = R.string.synced_tabs_reauth,
        )
    }

    @DrawableRes val icon: Int
    val contentDescription: String
    var colors = FloatingActionButtonDefaults.colorsPrimary()
    var elevation = M3FloatingActionButtonDefaults.elevation()
    val onClick: () -> Unit
    var iconModifier: Modifier = Modifier

    when (state.selectedPage) {
        Page.NormalTabs -> {
            icon = iconsR.drawable.mozac_ic_plus_24
            contentDescription = stringResource(id = R.string.add_tab)
            onClick = onOpenNewNormalTabClicked
        }

        Page.PrivateTabs -> {
            icon = iconsR.drawable.mozac_ic_plus_24
            contentDescription = stringResource(id = R.string.add_private_tab)
            onClick = onOpenNewPrivateTabClicked
        }

        Page.TabGroups -> {
            icon = iconsR.drawable.mozac_ic_plus_24
            contentDescription = stringResource(id = R.string.create_tab_group_content_description)
            onClick = onTabGroupsFabClicked
        }

        Page.SyncedTabs -> {
            icon = iconsR.drawable.mozac_ic_sync_24
            contentDescription = stringResource(id = R.string.resync_button_content_description)
            onClick = {
                if (!isSyncDisabled) {
                    onSyncedTabsFabClicked()
                }
            }
            if (isSyncDisabled) {
                colors = FloatingActionButtonDefaults.colorsDisabled()
                elevation = M3FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp,
                )
            }
            iconModifier = Modifier.animateRotation(animate = state.sync.isSyncing)
        }
    }

    FloatingActionButton(
        modifier = Modifier.testTag(TabsTrayTestTag.FAB),
        icon = {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = contentDescription,
                modifier = iconModifier,
            )
        },
        colors = colors,
        elevation = elevation,
        onClick = onClick,
    )
}

/**
 * Confirmation dialog shown when the user selects the "Close all tabs" action
 * from the tab manager.
 *
 * @param onConfirm Invoked when the user confirms in closing all open tabs.
 * @param onDismiss Invoked when the dialog is dismissed without confirming.
 */
@Composable
private fun CloseAllTabsConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.tab_manager_close_all_tabs_dialog_title),
                style = FirefoxTheme.typography.headline5,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.tab_manager_close_all_tabs_dialog_body),
                style = FirefoxTheme.typography.body2,
            )
        },
        confirmButton = {
            TextButton(
                text = stringResource(R.string.tab_manager_close_all_tabs_dialog_confirm),
                onClick = onConfirm,
            )
        },
        dismissButton = {
            TextButton(
                text = stringResource(R.string.tab_manager_close_all_tabs_dialog_cancel),
                onClick = onDismiss,
            )
        },
    )
}

@Suppress("LongParameterList")
private fun generateMenuItems(
    selectedPage: Page,
    normalTabCount: Int,
    privateTabCount: Int,
    homepageAsNewTabEnabled: Boolean,
    onTabSettingsClick: () -> Unit,
    onRecentlyClosedClick: () -> Unit,
    onNewTabGroupClick: () -> Unit,
    onEnterMultiselectModeClick: () -> Unit,
    onSelectAllTabsClick: () -> Unit,
    onDeleteAllTabsClick: () -> Unit,
    onAccountSettingsClick: () -> Unit,
): List<MenuItem> {
    val enterSelectModeItem = MenuItem.IconItem(
        text = Text.Resource(R.string.tabs_tray_select_tabs),
        drawableRes = iconsR.drawable.mozac_ic_checkmark_24,
        testTag = TabsTrayTestTag.SELECT_TABS,
        onClick = onEnterMultiselectModeClick,
    )
    val selectAllTabsItem = MenuItem.IconItem(
        text = Text.Resource(R.string.tab_tray_menu_select_all_tabs),
        drawableRes = iconsR.drawable.mozac_ic_select_all_24,
        testTag = TabsTrayTestTag.SELECT_ALL_TABS,
        onClick = onSelectAllTabsClick,
    )
    val recentlyClosedTabsItem = MenuItem.IconItem(
        text = Text.Resource(R.string.tab_tray_menu_recently_closed),
        drawableRes = iconsR.drawable.mozac_ic_history_24,
        testTag = TabsTrayTestTag.RECENTLY_CLOSED_TABS,
        onClick = onRecentlyClosedClick,
    )
    val newTabGroupItem = MenuItem.IconItem(
        text = Text.Resource(R.string.add_to_new_tab_group_title),
        drawableRes = iconsR.drawable.mozac_ic_tab_group_24,
        testTag = TabsTrayTestTag.NEW_TAB_GROUP,
        onClick = onNewTabGroupClick,
    )
    val tabSettingsItem = MenuItem.IconItem(
        text = Text.Resource(R.string.tab_tray_menu_tab_settings),
        drawableRes = iconsR.drawable.mozac_ic_settings_24,
        testTag = TabsTrayTestTag.TAB_SETTINGS,
        onClick = onTabSettingsClick,
    )
    val deleteAllTabsItem = MenuItem.IconItem(
        text = Text.Resource(R.string.tab_tray_menu_item_close),
        drawableRes = iconsR.drawable.mozac_ic_delete_24,
        testTag = TabsTrayTestTag.CLOSE_ALL_TABS,
        onClick = onDeleteAllTabsClick,
        level = MenuItem.FixedItem.Level.Critical,
    )
    val accountSettingsItem = MenuItem.IconItem(
        text = Text.Resource(R.string.tab_tray_menu_account_settings),
        drawableRes = iconsR.drawable.mozac_ic_avatar_circle_24,
        testTag = TabsTrayTestTag.ACCOUNT_SETTINGS,
        onClick = onAccountSettingsClick,
    )
    return when {
        selectedPage == Page.NormalTabs && normalTabCount == 0 -> listOfNotNull(
            recentlyClosedTabsItem,
            newTabGroupItem.takeIf { homepageAsNewTabEnabled },
            tabSettingsItem,
        )

        selectedPage == Page.PrivateTabs && privateTabCount == 0 -> listOf(
            recentlyClosedTabsItem,
            tabSettingsItem,
        )

        selectedPage == Page.NormalTabs -> listOfNotNull(
            enterSelectModeItem,
            selectAllTabsItem,
            recentlyClosedTabsItem,
            newTabGroupItem.takeIf { homepageAsNewTabEnabled },
            tabSettingsItem,
            deleteAllTabsItem,
        )

        selectedPage == Page.PrivateTabs -> listOf(
            recentlyClosedTabsItem,
            tabSettingsItem,
            deleteAllTabsItem,
        )

        selectedPage == Page.SyncedTabs -> listOf(
            accountSettingsItem,
            recentlyClosedTabsItem,
        )

        else -> emptyList()
    }
}

private class TabManagerFloatingToolbarParameterProvider : PreviewParameterProvider<TabsTrayState> {
        val data = listOf(
            Pair(
                "Normal tabs page with at least one tab",
                TabsTrayState(
                    selectedPage = Page.NormalTabs,
                    normalTabsState = TabsTrayState.NormalTabsState(
                        items = listOf(createTab(url = "url")),
                    ),
                ),
            ),
            Pair(
                "Private tabs page with no tabs",
                TabsTrayState(
                    selectedPage = Page.NormalTabs,
                    normalTabsState = TabsTrayState.NormalTabsState(
                        items = emptyList(),
                    ),
                ),
            ),
            Pair(
                "Private tabs page with tabs",
                TabsTrayState(
                    selectedPage = Page.PrivateTabs,
                    privateBrowsing = TabsTrayState.PrivateBrowsingState(
                        tabs = listOf(createTab(url = "url")),
                    ),
                ),
            ),
            Pair(
                "Private tabs page with no tabs",
                TabsTrayState(
                    selectedPage = Page.PrivateTabs,
                    privateBrowsing = TabsTrayState.PrivateBrowsingState(
                        tabs = emptyList(),
                    ),
                ),
            ),
            Pair(
                "Synced page while signed in",
                TabsTrayState(
                    selectedPage = Page.SyncedTabs,
                    sync = TabsTrayState.SyncState(isSignedIn = true),
                ),
            ),
            Pair(
                "Synced page while syncing",
                TabsTrayState(
                    selectedPage = Page.SyncedTabs,
                    sync = TabsTrayState.SyncState(
                        isSignedIn = true,
                        isSyncing = true,
                    ),
                ),
            ),
            Pair(
                "Synced page while signed out",
                TabsTrayState(
                    selectedPage = Page.SyncedTabs,
                    sync = TabsTrayState.SyncState(isSignedIn = false),
                ),
            ),
        )

    override val values: Sequence<TabsTrayState>
        get() = data.map { it.second }.asSequence()

    override fun getDisplayName(index: Int): String {
        return data[index].first
    }
}

@PreviewLightDark
@Composable
private fun TabManagerFloatingToolbarPreview(
    @PreviewParameter(TabManagerFloatingToolbarParameterProvider::class) previewState: TabsTrayState,
) {
    val store = remember { TabsTrayStore(initialState = previewState) }
    val state by store.stateFlow.collectAsState(initial = store.state)

    FirefoxTheme {
        Surface {
            TabManagerFloatingToolbar(
                state = state,
                modifier = Modifier.padding(all = 16.dp),
                onAction = store::dispatch,
                onOpenNewNormalTabClicked = {},
                onOpenNewPrivateTabClicked = {},
                onSyncedTabsFabClicked = {},
                onTabSettingsClick = {},
                onAccountSettingsClick = {},
                onDeleteAllTabsClick = {},
                onRecentlyClosedClick = {},
            )
        }
    }
}
