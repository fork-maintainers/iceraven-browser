/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabgroups

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mozilla.components.compose.base.annotation.FlexibleWindowLightDarkPreview
import mozilla.components.compose.base.button.IconButton
import org.mozilla.fenix.R
import org.mozilla.fenix.tabstray.LocalTabManagementFeatureHelper
import org.mozilla.fenix.tabstray.TabsTrayTestTag
import org.mozilla.fenix.tabstray.controller.NoOpTabInteractionHandler
import org.mozilla.fenix.tabstray.controller.TabInteractionHandler
import org.mozilla.fenix.tabstray.data.TabGroupTheme
import org.mozilla.fenix.tabstray.data.TabsTrayItem
import org.mozilla.fenix.tabstray.data.createTab
import org.mozilla.fenix.tabstray.data.createTabGroup
import org.mozilla.fenix.tabstray.redux.state.TabsTrayState
import org.mozilla.fenix.tabstray.ui.tabitems.LOREM_IPSUM
import org.mozilla.fenix.tabstray.ui.tabitems.TabGroupMenuButton
import org.mozilla.fenix.tabstray.ui.tabpage.TabLayout
import org.mozilla.fenix.theme.FirefoxTheme
import mozilla.components.ui.icons.R as iconsR

/**
 * Renders an expanded view of a user's tab group.
 * @param group [TabsTrayItem.TabGroup] item rendered by the card.
 * @param actions [ExpandedTabGroupActions] invoked in response to user interactions.
 * @param tabInteractionHandler Handler for tab interactions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedTabGroup(
    group: TabsTrayItem.TabGroup,
    actions: ExpandedTabGroupActions,
    tabInteractionHandler: TabInteractionHandler,
) {
    Column(
        modifier = Modifier
            .testTag(TabsTrayTestTag.TAB_GROUP_BOTTOM_SHEET_ROOT)
            .padding(
                start = FirefoxTheme.layout.space.dynamic200,
                end = FirefoxTheme.layout.space.dynamic200,
            ),
    ) {
        ViewTabGroupHeader(
            title = group.title,
            groupTheme = group.theme,
            groupTabsSize = group.tabs.size,
            onDeleteTabGroupClick = actions.onDeleteTabGroupClick,
            onEditTabGroupClick = actions.onEditTabGroupClick,
            onCloseTabGroupClick = actions.onCloseTabGroupClick,
            onAddNewTabClick = actions.onAddNewTabClick,
        )

        TabLayout(
            tabs = group.tabs,
            displayTabsInGrid = true,
            dragAndDropEnabled = false,
            reorderingEnabled = true,
            displayTabGroupOnboarding = false,
            liveReorderEnabled = true,
            selectedItemIndex = group.initialScrollIndex,
            selectionMode = TabsTrayState.Mode.Normal,
            tabInteractionHandler = tabInteractionHandler,
            modifier = Modifier,
            onTabClose = actions.onTabClose,
            onItemClick = actions.onItemClick,
            onItemLongClick = { item -> }, // Ignore long click
            onDeleteTabGroupClick = { }, // Ignore tab group deletes
            onEditTabGroupClick = { }, // Ignore tab group edits
            onCloseTabGroupClick = { }, // Ignore tab group closes
            onTabGroupOnboardingDismiss = { }, // Ignore onboarding dismissals - onboarding is not shown in this layout
            contentPadding = PaddingValues(0.dp), // TabLayout should not have its own content padding inside this view
            focusEnabled = true, // Drag and drop is not possible in this view, so focus should never be suppressed
        )
    }
}

@Composable
private fun ViewTabGroupHeader(
    title: String,
    groupTabsSize: Int,
    groupTheme: TabGroupTheme,
    onDeleteTabGroupClick: () -> Unit,
    onEditTabGroupClick: () -> Unit,
    onCloseTabGroupClick: () -> Unit,
    onAddNewTabClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = FirefoxTheme.layout.space.static150,
                bottom = FirefoxTheme.layout.space.static200,
            )
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val headerContentDescription = pluralStringResource(
            id = R.plurals.expanded_tab_group_header_description,
            count = groupTabsSize,
            title,
            groupTabsSize,
            groupTheme.contentLabel,
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {
                    heading()
                    contentDescription = headerContentDescription
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabGroupThemeDot(groupTheme)

            Spacer(modifier = Modifier.width(FirefoxTheme.layout.space.static100))

            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics { },
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = FirefoxTheme.typography.headline7,
            )
        }

        Spacer(
            modifier = Modifier.width(
                FirefoxTheme.layout.space.static200 +
                    FirefoxTheme.layout.space.static25,
            ),
        )

        if (LocalTabManagementFeatureHelper.current.shareTabGroupEnabled) {
            ShareTabGroupButton(
                title = title,
                groupTabsSize = groupTabsSize,
                onClick = {},
            )

            Spacer(modifier = Modifier.width(FirefoxTheme.layout.space.static100))
        }

        if (onAddNewTabClick != null) {
            AddTabToGroupButton(
                onClick = onAddNewTabClick,
            )

            Spacer(modifier = Modifier.width(FirefoxTheme.layout.space.static100))
        }

        TabGroupMenuButton(
            includeCloseOption = true,
            onDeleteTabGroupClick = onDeleteTabGroupClick,
            onEditTabGroupClick = onEditTabGroupClick,
            onCloseTabGroupClick = onCloseTabGroupClick,
        )
    }
}

@Composable
private fun AddTabToGroupButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        contentDescription = stringResource(id = R.string.add_tab),
        modifier = modifier.testTag(TabsTrayTestTag.BOTTOM_SHEET_ADD_TAB_BUTTON),
    ) {
        Icon(
            painter = painterResource(id = iconsR.drawable.mozac_ic_plus_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ShareTabGroupButton(
    title: String,
    groupTabsSize: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        contentDescription = pluralStringResource(
            id = R.plurals.share_tab_group_button_content_description,
            count = groupTabsSize,
            title,
            groupTabsSize,
        ),
        modifier = modifier.testTag(TabsTrayTestTag.BOTTOM_SHEET_SHARE_BUTTON),
    ) {
        Icon(
            painter = painterResource(id = iconsR.drawable.mozac_ic_share_android_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@FlexibleWindowLightDarkPreview
@Composable
private fun ExpandedTabGroupPreview(
    @PreviewParameter(ExpandedTabGroupPreviewProvider::class)
    previewState: ExpandedTabGroupPreviewState,
) {
    val sheetState = rememberModalBottomSheetState()
    LaunchedEffect(Unit) {
        if (!sheetState.isVisible) {
            sheetState.show()
        }
    }

    FirefoxTheme {
        Surface {
            ModalBottomSheet(
                modifier = Modifier.testTag(TabsTrayTestTag.TAB_GROUP_BOTTOM_SHEET_ROOT),
                sheetState = sheetState,
                onDismissRequest = {},
            ) {
                ExpandedTabGroup(
                    group = previewState.group,
                    actions = ExpandedTabGroupActions(
                        onItemClick = {},
                        onTabClose = {},
                        onDeleteTabGroupClick = {},
                        onEditTabGroupClick = {},
                        onCloseTabGroupClick = {},
                        onAddNewTabClick = {},
                    ),
                    tabInteractionHandler = NoOpTabInteractionHandler,
                )
            }
        }
    }
}

private fun generateFakeTabsList(
    tabCount: Int = 10,
): MutableList<TabsTrayItem.Tab> = MutableList(tabCount) { index ->
    createTab(
        id = "tab$index",
        title = "Tab $index",
        url = "www.mozilla.com",
        private = false,
    )
}

private data class ExpandedTabGroupPreviewState(
    val group: TabsTrayItem.TabGroup,
    val selectedTabId: String? = null,
)

private class ExpandedTabGroupPreviewProvider :
    PreviewParameterProvider<ExpandedTabGroupPreviewState> {
    val data = listOf(
        Pair(
            "1 Tab",
            ExpandedTabGroupPreviewState(
                group = createTabGroup(
                    title = "Tab Group",
                    tabs = generateFakeTabsList(tabCount = 1),
                ),
            ),
        ),
        Pair(
            "2 Tabs",
            ExpandedTabGroupPreviewState(
                group = createTabGroup(
                    title = "Tab Group",
                    tabs = generateFakeTabsList(tabCount = 2),
                ),
            ),
        ),
        Pair(
            "3 Tabs",
            ExpandedTabGroupPreviewState(
                group = createTabGroup(
                    title = "Tab Group",
                    tabs = generateFakeTabsList(tabCount = 3),
                ),
            ),
        ),
        Pair(
            "4 Tabs",
            ExpandedTabGroupPreviewState(
                group = createTabGroup(
                    title = "Tab Group",
                    tabs = generateFakeTabsList(),
                ),
            ),
        ),
        Pair(
            "Selected tab",
            ExpandedTabGroupPreviewState(
                group = createTabGroup(
                    title = "Tab Group",
                    tabs = generateFakeTabsList(),
                ),
                selectedTabId = "tabid0",
            ),
        ),
        Pair(
            "Large title",
            ExpandedTabGroupPreviewState(
                group = createTabGroup(
                    title = LOREM_IPSUM,
                    tabs = generateFakeTabsList(),
                ),
                selectedTabId = "tabid0",
            ),
        ),
    )
    override val values: Sequence<ExpandedTabGroupPreviewState>
        get() = data.map { it.second }.asSequence()

    override fun getDisplayName(index: Int): String {
        return data[index].first
    }
}

/**
 * User interactions handled by the [ExpandedTabGroup] view.
 *
 * @property onItemClick Invoked when the user clicks on a [TabsTrayItem] in the group.
 * @property onTabClose Invoked when the user clicks to close a [TabsTrayItem.Tab] in the group.
 * @property onDeleteTabGroupClick Invoked when the user clicks on delete tab group.
 * @property onEditTabGroupClick Invoked when the user clicks to edit the group.
 * @property onCloseTabGroupClick Invoked when the user clicks to close a tab group.
 * @property onAddNewTabClick Invoked when the user clicks to add a new tab to the group. When null,
 * the add-tab button is hidden.
 */
data class ExpandedTabGroupActions(
    val onItemClick: (TabsTrayItem) -> Unit,
    val onTabClose: (TabsTrayItem.Tab) -> Unit,
    val onDeleteTabGroupClick: () -> Unit,
    val onEditTabGroupClick: () -> Unit,
    val onCloseTabGroupClick: () -> Unit,
    val onAddNewTabClick: (() -> Unit)?,
)
