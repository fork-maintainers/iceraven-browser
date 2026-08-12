/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.ui.tabitems

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import mozilla.components.compose.base.RadioCheckmark
import mozilla.components.support.base.utils.MAX_URI_LENGTH
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.DismissibleItemBackground
import org.mozilla.fenix.compose.TabThumbnail
import org.mozilla.fenix.ext.toShortUrl
import org.mozilla.fenix.tabstray.TabsTrayTestTag
import org.mozilla.fenix.tabstray.browser.compose.TabItemInteractionState
import org.mozilla.fenix.tabstray.data.TabsTrayItem
import org.mozilla.fenix.tabstray.data.createTab
import org.mozilla.fenix.theme.FirefoxTheme
import mozilla.components.browser.tabstray.R as tabstrayR

private val ThumbnailWidth = 78.dp
private val ThumbnailHeight = 68.dp
internal val TabListItemHeight: Dp
    @Composable
    @ReadOnlyComposable
    get() = ThumbnailHeight + FirefoxTheme.layout.space.static100 * 2

/**
 * List item used to display a tab that supports clicks,
 * long clicks, multiselection, and media controls.
 *
 * @param tab The given tab to render as list item.
 * @param modifier [Modifier] to be applied to the tab list item content.
 * @param interactionState: [TabItemInteractionState] holding hovered and dragged status.
 * @param selectionState: The tab item's [TabsTrayItemSelectionState]
 * @param shouldClickListen Whether the item should stop listening to click events.
 * @param swipingEnabled Whether the item is swipeable.
 * @param onCloseClick Invoked when the close button is clicked.
 * @param onClick Invoked when the item is clicked.
 * @param onLongClick Invoked when the item is long clicked.
 */
@Composable
fun TabListTabItem(
    tab: TabsTrayItem.Tab,
    modifier: Modifier = Modifier,
    interactionState: TabItemInteractionState = TabItemInteractionState(),
    selectionState: TabsTrayItemSelectionState = TabsTrayItemSelectionState(),
    shouldClickListen: Boolean = true,
    swipingEnabled: Boolean = true,
    onCloseClick: (TabsTrayItem.Tab) -> Unit,
    onClick: (TabsTrayItem) -> Unit,
    onLongClick: ((TabsTrayItem) -> Unit)? = null,
) {
    val swipeToDismissBoxState = rememberTabSwipeToDismissBoxState(tabId = tab.id)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    // SwipeToDismissBox invokes onDismiss from a LaunchedEffect keyed on the callback, so an
    // unstable lambda would re-close the tab on every recomposition that follows the dismissal.
    val currentTab by rememberUpdatedState(tab)
    val currentOnCloseClick by rememberUpdatedState(onCloseClick)
    val onDismiss = remember { { _: SwipeToDismissBoxValue -> currentOnCloseClick(currentTab) } }

    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        onDismiss = onDismiss,
        gesturesEnabled = !selectionState.multiSelectEnabled && swipingEnabled,
        backgroundContent = {
            // dismissDirection comes from the raw offset and is not mirrored for RTL.
            val contentMovedRight =
                swipeToDismissBoxState.dismissDirection == SwipeToDismissBoxValue.StartToEnd

            DismissibleItemBackground(
                isSwipeActive = swipeToDismissBoxState.dismissDirection != SwipeToDismissBoxValue.Settled,
                isSwipingToStart = if (isRtl) contentMovedRight else !contentMovedRight,
            )
        },
    ) {
        TabContent(
            tab = tab,
            selectionState = selectionState,
            interactionState = interactionState,
            shouldClickListen = shouldClickListen,
            // The fade has to wrap the caller's modifier: the focus outline is a border applied
            // there by tabListItemShapeStyling, and it only picks up the alpha if it is drawn
            // inside the layer.
            modifier = Modifier
                .fadeOnSwipeToDismiss(swipeToDismissBoxState)
                .then(modifier),
            onCloseClick = onCloseClick,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun TabContent(
    tab: TabsTrayItem.Tab,
    interactionState: TabItemInteractionState,
    selectionState: TabsTrayItemSelectionState,
    shouldClickListen: Boolean,
    modifier: Modifier = Modifier,
    onCloseClick: (TabsTrayItem.Tab) -> Unit,
    onClick: (TabsTrayItem) -> Unit,
    onLongClick: ((TabsTrayItem) -> Unit)? = null,
) {
    val contentBackgroundColor = if (selectionState.isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceBright
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .tabItemListInteractionAnimation(
                interactionState = interactionState,
            )
            .background(contentBackgroundColor)
            .tabItemClickable(
                clickHandler = TabsTrayItemClickHandler(
                    enabled = shouldClickListen,
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
                clickedItem = tab,
            )
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
            .testTag(TabsTrayTestTag.TAB_ITEM_ROOT)
            .semantics {
                selected = selectionState.isFocused
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumbnail(tab = tab)

        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(weight = 1f),
        ) {
            Text(
                text = tab.title.take(MAX_URI_LENGTH),
                color = MaterialTheme.colorScheme.onSurface,
                style = FirefoxTheme.typography.body1,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
            )

            Text(
                text = tab.url.toShortUrl(),
                color = MaterialTheme.colorScheme.secondary,
                style = FirefoxTheme.typography.body2,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }

        TabListIcon(
            selectionState = selectionState,
            onCloseClick = onCloseClick,
            tab = tab,
        )
    }
}

@Composable
private fun TabListIcon(
    selectionState: TabsTrayItemSelectionState,
    onCloseClick: (TabsTrayItem.Tab) -> Unit,
    tab: TabsTrayItem.Tab,
) {
    if (!selectionState.multiSelectEnabled) {
        ListItemDismissButton(
            contentDescription = stringResource(
                id = R.string.close_tab_title,
                tab.title,
            ),
            modifier = Modifier
                .testTag(TabsTrayTestTag.TAB_ITEM_CLOSE),
            onClick = { onCloseClick(tab) },
        )
    } else {
        RadioCheckmark(
            isSelected = selectionState.isSelected,
            modifier = Modifier.padding(end = FirefoxTheme.layout.space.dynamic200),
        )
    }
}

@Composable
private fun Thumbnail(
    tab: TabsTrayItem.Tab,
) {
    val density = LocalDensity.current
    val thumbnailSize = with(density) { ThumbnailWidth.toPx() }.toInt()
    TabThumbnail(
        tabThumbnailImageData = tab.toThumbnailImageData(),
        thumbnailSizePx = thumbnailSize,
        modifier = Modifier
            .size(
                width = ThumbnailWidth,
                height = ThumbnailHeight,
            )
            .testTag(TabsTrayTestTag.TAB_ITEM_THUMBNAIL),
        shape = MaterialTheme.shapes.extraSmall,
        border = tablistItemThumbnailBorder,
        contentDescription = stringResource(id = tabstrayR.string.mozac_browser_tabstray_open_tab),
    )
}

private data class TabListItemPreviewState(
    val tabItemSelectionState: TabsTrayItemSelectionState,
    val url: String = "www.mozilla.org",
    val title: String = "Mozilla Domain",
    val tabItemInteractionState: TabItemInteractionState = TabItemInteractionState(),
)

private class TabListItemParameterProvider : PreviewParameterProvider<TabListItemPreviewState> {
    val data: List<Pair<String, TabListItemPreviewState>> = listOf(
        Pair(
            "Not focused or selected",
            TabListItemPreviewState(
                tabItemSelectionState = TabsTrayItemSelectionState(
                    isFocused = false,
                    multiSelectEnabled = false,
                    isSelected = false,
                ),
            ),
        ),
        Pair(
            "Focused, not selected",
            TabListItemPreviewState(
                tabItemSelectionState = TabsTrayItemSelectionState(
                    isFocused = true,
                    multiSelectEnabled = false,
                    isSelected = false,
                ),
            ),
        ),
        Pair(
            "Multiselection enabled, not focused or selected",
            TabListItemPreviewState(
                tabItemSelectionState = TabsTrayItemSelectionState(
                    isFocused = false,
                    multiSelectEnabled = true,
                    isSelected = false,
                ),
            ),
        ),
        Pair(
            "Multiselection enabled, focused, not selected",
            TabListItemPreviewState(
                tabItemSelectionState = TabsTrayItemSelectionState(
                    isFocused = true,
                    multiSelectEnabled = true,
                    isSelected = false,
                ),
            ),
        ),
        Pair(
            "Multiselection enabled, not focused, selected",
            TabListItemPreviewState(
                tabItemSelectionState = TabsTrayItemSelectionState(
                    isFocused = false,
                    multiSelectEnabled = true,
                    isSelected = true,
                ),
            ),
        ),
        Pair(
            "Multiselection enabled, focused and selected",
            TabListItemPreviewState(
                tabItemSelectionState = TabsTrayItemSelectionState(
                    isFocused = true,
                    multiSelectEnabled = true,
                    isSelected = true,
                ),
            ),
        ),
        Pair(
            "Not focused or selected, long title",
            TabListItemPreviewState(
                tabItemSelectionState = TabsTrayItemSelectionState(
                    isFocused = false,
                    multiSelectEnabled = false,
                    isSelected = false,
                ),
                url = "www.google.com/superlongurl",
                title = LOREM_IPSUM,
            ),
        ),
        Pair(
            "Dragged",
            TabListItemPreviewState(
                tabItemSelectionState = TabsTrayItemSelectionState(
                    isFocused = false,
                    multiSelectEnabled = false,
                    isSelected = false,
                ),
                tabItemInteractionState = TabItemInteractionState(
                    isDragged = true,
                    isHoveredByItem = false,
                ),
            ),
        ),
        Pair(
            "Hovered",
            TabListItemPreviewState(
                tabItemSelectionState = TabsTrayItemSelectionState(
                    isFocused = false,
                    multiSelectEnabled = false,
                    isSelected = false,
                ),
                tabItemInteractionState = TabItemInteractionState(
                    isDragged = false,
                    isHoveredByItem = true,
                ),
            ),
        ),
    )

    override fun getDisplayName(index: Int): String? {
        return data[index].first
    }

    override val values: Sequence<TabListItemPreviewState>
        get() = data.map { it.second }.asSequence()
}

@Composable
@PreviewLightDark
private fun TabListTabItemPreview(
    @PreviewParameter(TabListItemParameterProvider::class) tabListItemState: TabListItemPreviewState,
) {
    FirefoxTheme {
        TabListTabItem(
            tab = createTab(
                url = tabListItemState.url,
                title = tabListItemState.title,
            ),
            onCloseClick = {},
            onClick = {},
            selectionState = tabListItemState.tabItemSelectionState,
            interactionState = tabListItemState.tabItemInteractionState,
        )
    }
}
