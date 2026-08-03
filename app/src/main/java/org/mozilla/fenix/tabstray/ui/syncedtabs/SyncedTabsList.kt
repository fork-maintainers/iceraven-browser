/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.ui.syncedtabs

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mozilla.components.browser.storage.sync.TabEntry
import mozilla.components.compose.base.annotation.FlexibleWindowLightDarkPreview
import mozilla.components.compose.base.button.FilledButton
import mozilla.components.compose.base.modifier.dashedBorder
import mozilla.components.feature.syncedtabs.view.SyncedTabsView
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.list.ExpandableListHeader
import org.mozilla.fenix.tabstray.TabsTrayTestTag
import org.mozilla.fenix.tabstray.syncedtabs.OnSectionExpansionToggled
import org.mozilla.fenix.tabstray.syncedtabs.SyncedTabsListItem
import org.mozilla.fenix.tabstray.ui.tabitems.BasicTabListItem
import org.mozilla.fenix.tabstray.ui.tabitems.TabListBorderMiddleItemShape
import org.mozilla.fenix.tabstray.ui.tabitems.TabListFirstItemShape
import org.mozilla.fenix.tabstray.ui.tabitems.TabListLastItemShape
import org.mozilla.fenix.tabstray.ui.tabitems.TabListSingleItemShape
import org.mozilla.fenix.theme.FirefoxTheme
import mozilla.components.browser.storage.sync.Tab as SyncTab
import mozilla.components.ui.icons.R as iconsR

private val SyncedTabVerticalPadding = 8.dp

/**
 * A lambda invoked when the user clicks on a synced tab in the [SyncedTabsList].
 */
typealias OnTabClick = (tab: SyncTab) -> Unit

/**
 * A lambda invoked when the user clicks a synced tab's close button in the [SyncedTabsList].
 */
typealias OnTabCloseClick = (deviceId: String, tab: SyncTab) -> Unit

/**
 * Top-level list UI for displaying Synced Tabs in the Tabs Tray.
 *
 * @param syncedTabs The tab UI items to be displayed.
 * @param onTabClick The lambda for handling clicks on synced tabs.
 * @param onTabCloseClick The lambda for handling clicks on a synced tab's close button.
 * @param expandedState A list of expanded state properties for the synced tabs.
 * @param onSectionExpansionToggled A lambda for handling section expansion/collapse
 */
@Composable
fun SyncedTabsList(
    syncedTabs: List<SyncedTabsListItem>,
    onTabClick: OnTabClick,
    onTabCloseClick: OnTabCloseClick,
    expandedState: List<Boolean>,
    onSectionExpansionToggled: OnSectionExpansionToggled,
) {
    val listState = rememberLazyListState()
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .width(FirefoxTheme.layout.size.containerMaxWidth)
                .testTag(TabsTrayTestTag.SYNCED_TABS_LIST),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(FirefoxTheme.layout.space.static25),
        ) {
            syncedTabs.forEachIndexed { index, syncedTabItem ->
                when (syncedTabItem) {
                    is SyncedTabsListItem.DeviceSection -> {
                        val sectionExpanded = expandedState[index]

                        item(key = "header-${syncedTabItem.displayName}-$index") {
                            SyncedTabsSectionHeader(
                                headerText = syncedTabItem.displayName,
                                expanded = sectionExpanded,
                            ) {
                                onSectionExpansionToggled.invoke(index)
                            }
                        }

                        item { Spacer(modifier = Modifier.height(SyncedTabVerticalPadding)) }

                        if (sectionExpanded) {
                            syncedTabsSectionContent(
                                sectionIndex = index,
                                syncedTabSection = syncedTabItem,
                                lazyScope = this,
                                onTabClick = onTabClick,
                                onTabCloseClick = onTabCloseClick,
                            )
                        }

                        item { Spacer(modifier = Modifier.height(SyncedTabVerticalPadding)) }
                    }

                    is SyncedTabsListItem.Error -> {
                        item(key = "error-${syncedTabItem.errorText}-$index") {
                            SyncedTabsErrorItem(
                                errorText = syncedTabItem.errorText,
                                errorButton = syncedTabItem.errorButton,
                            )
                        }

                        item { Spacer(modifier = Modifier.height(SyncedTabVerticalPadding)) }
                    }

                    else -> {
                        // no-op
                    }
                }
            }

            item(key = "footer-spacer") {
                // The Spacer here is to act as a footer to add padding to the bottom of the list so
                // the FAB or any potential SnackBar doesn't overlap with the items at the end.
                Spacer(modifier = Modifier.height(240.dp))
            }
        }
    }
}

/**
 * Generates the content UI for a synced device. This will lazily render the tabs individually, or
 * an empty state if there are none.
 *
 * @param sectionIndex The index of the synced device.
 * @param syncedTabSection The [SyncedTabsListItem.DeviceSection] being rendered.
 * @param lazyScope [LazyListScope] used to lazily initialize the tab items.
 * @param onTabClick Invoked when a user clicks on a synced tab.
 * @param onTabCloseClick Invoked when a user closes a synced tab.
 */
private fun syncedTabsSectionContent(
    sectionIndex: Int,
    syncedTabSection: SyncedTabsListItem.DeviceSection,
    lazyScope: LazyListScope,
    onTabClick: OnTabClick,
    onTabCloseClick: OnTabCloseClick,
) = with(lazyScope) {
    if (syncedTabSection.tabs.isNotEmpty()) {
        itemsIndexed(
            items = syncedTabSection.tabs,
            key = { index, item ->
                "device-section-${syncedTabSection.displayName}-device-index-$sectionIndex" +
                        "-tab-${item.tab.hashCode()}-${item.displayTitle}-index-$index}"
            },
        ) { index, syncedTab ->
            val itemShape = when {
                syncedTabSection.tabs.size == 1 -> TabListSingleItemShape
                index == 0 -> TabListFirstItemShape
                index == syncedTabSection.tabs.lastIndex -> TabListLastItemShape
                else -> TabListBorderMiddleItemShape
            }
            val itemModifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(shape = itemShape)
                .background(color = MaterialTheme.colorScheme.surfaceBright)
                .fillMaxWidth()

            Column(modifier = itemModifier) {
                SyncedTabListItem(
                    syncedTab = syncedTab,
                    onTabClick = onTabClick,
                    onTabCloseClick = onTabCloseClick,
                )
            }
        }
    } else {
        item(key = "no-tabs-${syncedTabSection.displayName}-$sectionIndex") { SyncedTabsNoTabsItem() }
    }
}

/**
 * A synced tab item
 *
 * @param syncedTab The backing data of the synced tab.
 * @param onTabClick Invoked when a user clicks on a synced tab.
 * @param onTabCloseClick Invoked when a user closes a synced tab.
 */
@Composable
private fun SyncedTabListItem(
    syncedTab: SyncedTabsListItem.Tab,
    onTabClick: OnTabClick,
    onTabCloseClick: OnTabCloseClick,
) {
    when (syncedTab.action) {
        is SyncedTabsListItem.Tab.Action.Close -> BasicTabListItem(
            title = syncedTab.displayTitle,
            url = syncedTab.displayURL,
            faviconShape = CircleShape,
            showCloseButton = true,
            onClick = { onTabClick(syncedTab.tab) },
            onCloseButtonClick = {
                onTabCloseClick(
                    syncedTab.action.deviceId,
                    syncedTab.tab,
                )
            },
        )
        is SyncedTabsListItem.Tab.Action.None -> BasicTabListItem(
            title = syncedTab.displayTitle,
            url = syncedTab.displayURL,
            modifier = Modifier.padding(vertical = 2.dp),
            faviconShape = CircleShape,
            onClick = { onTabClick(syncedTab.tab) },
        )
    }
}

/**
 * Collapsible header for sections of synced tabs
 *
 * @param headerText The section title for a group of synced tabs.
 * @param expanded Indicates whether the section of content is expanded. If null, the Icon will be hidden.
 * @param onClick Optional lambda for handling section header clicks.
 */
@Composable
private fun SyncedTabsSectionHeader(
    headerText: String,
    expanded: Boolean? = null,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        ExpandableListHeader(
            headerText = headerText,
            headerTextStyle = MaterialTheme.typography.bodyMedium,
            headerTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            expanded = expanded,
            expandActionContentDescription = stringResource(R.string.synced_tabs_expand_group),
            collapseActionContentDescription = stringResource(R.string.synced_tabs_collapse_group),
            onClick = onClick,
        )
    }
}

/**
 * Error UI to show if there is one of the errors outlined in [SyncedTabsView.ErrorType].
 *
 * @param errorText The text to be displayed to the user.
 * @param errorButton Optional class to set up and handle any clicks in the Error UI.
 */
@Composable
private fun SyncedTabsErrorItem(
    errorText: String,
    errorButton: SyncedTabsListItem.ErrorButton? = null,
) {
    Box(
        Modifier
            .padding(all = 8.dp)
            .height(IntrinsicSize.Min)
            .dashedBorder(
                color = MaterialTheme.colorScheme.outlineVariant,
                cornerRadius = 8.dp,
                dashHeight = 2.dp,
                dashWidth = 4.dp,
            ),
    ) {
        Column(
            Modifier
                .padding(all = 16.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 14.sp,
            )

            errorButton?.let {
                Spacer(modifier = Modifier.height(12.dp))

                FilledButton(
                    text = it.buttonText,
                    modifier = Modifier.fillMaxWidth(),
                    icon = painterResource(iconsR.drawable.mozac_ic_avatar_circle_fill_24),
                    onClick = it.onClick,
                )
            }
        }
    }
}

/**
 * UI to be displayed when a user's device has no synced tabs.
 */
@Composable
private fun SyncedTabsNoTabsItem() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
        ),
    ) {
        Text(
            text = stringResource(R.string.synced_tabs_no_open_tabs),
            color = MaterialTheme.colorScheme.secondary,
            style = FirefoxTheme.typography.body1,
            modifier = Modifier
                .padding(all = 16.dp)
                .fillMaxWidth(),
        )
    }
}

@Composable
@PreviewLightDark
private fun SyncedTabsErrorPreview() {
    FirefoxTheme {
        Surface {
            SyncedTabsErrorItem(
                errorText = stringResource(R.string.synced_tabs_no_tabs),
                errorButton = SyncedTabsListItem.ErrorButton(
                    buttonText = stringResource(R.string.synced_tabs_sign_in_button),
                ) {
                    println("SyncedTabsErrorButton click")
                },
            )
        }
    }
}

@Composable
@FlexibleWindowLightDarkPreview
private fun SyncedTabsListPreview() {
    val syncedTabsList = getFakeSyncedTabList()
    FirefoxTheme {
        Surface {
            SyncedTabsList(
                syncedTabs = syncedTabsList,
                onTabClick = {},
                onTabCloseClick = { _, _ -> },
                onSectionExpansionToggled = {},
                expandedState = syncedTabsList.map { true },
            )
        }
    }
}

/**
 * Helper function to create a List of [SyncedTabsListItem] for previewing.
 */
@VisibleForTesting
internal fun getFakeSyncedTabList(): List<SyncedTabsListItem> = listOf(
    SyncedTabsListItem.DeviceSection(
        displayName = "Device 1",
        tabs = listOf(
            generateFakeTab("Mozilla", "www.mozilla.org"),
            generateFakeTab("Google", "www.google.com"),
            generateFakeTab("", "www.google.com"),
        ),
    ),
    SyncedTabsListItem.DeviceSection(
        displayName = "Device 2",
        tabs = listOf(
            generateFakeTab("Firefox", "www.getfirefox.org", SyncedTabsListItem.Tab.Action.Close("device2222")),
            generateFakeTab("Thunderbird", "www.getthunderbird.org", SyncedTabsListItem.Tab.Action.Close("device2222")),
        ),
    ),
    SyncedTabsListItem.DeviceSection("Device 3", emptyList()),
    SyncedTabsListItem.Error("Please re-authenticate"),
)

/**
 * Helper function to create a [SyncedTabsListItem.Tab] for previewing.
 */
private fun generateFakeTab(
    tabName: String,
    tabUrl: String,
    action: SyncedTabsListItem.Tab.Action = SyncedTabsListItem.Tab.Action.None,
): SyncedTabsListItem.Tab =
    SyncedTabsListItem.Tab(
        tabName.ifEmpty { tabUrl },
        tabUrl,
        action,
        SyncTab(
            history = listOf(TabEntry(tabName, tabUrl, null)),
            active = 0,
            lastUsed = 0L,
            inactive = false,
        ),
    )
