/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import mozilla.components.compose.browser.toolbar.concept.BrowserToolbarTestTags.ADDRESSBAR_URL_BOX
import mozilla.components.compose.browser.toolbar.concept.BrowserToolbarTestTags.TABS_COUNTER
import org.mozilla.fenix.R
import org.mozilla.fenix.components.toolbar.BrowserToolbarTestTags.SITE_INFO_INSECURE_CONNECTION
import org.mozilla.fenix.components.toolbar.BrowserToolbarTestTags.SITE_INFO_SECURE
import org.mozilla.fenix.components.toolbar.BrowserToolbarTestTags.SITE_INFO_TRACKING_PROTECTION_OFF
import org.mozilla.fenix.components.toolbar.BrowserToolbarTestTags.SITE_INFO_UNKNOWN
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

object ToolbarSelectors {
    val TOOLBAR = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "composable_toolbar",
        description = "Toolbar",
        groups = listOf("requiredForPage"),
    )

    val TAB_COUNTER = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = TABS_COUNTER,
        description = "Tab counter button",
        groups = listOf("requiredForPage"),
    )

    val TOOLBAR_URL_BOX = Selector(
        strategy = SelectorStrategy.COMPOSE_ON_ALL_NODES_BY_TAG_ON_FIRST,
        value = ADDRESSBAR_URL_BOX,
        description = "URL box",
        groups = listOf("requiredForPage"),
    )

    // Use UIAutomator when navigating from BrowserPage — avoids Compose sync hanging when GeckoView is active.
    val TOOLBAR_URL_BOX_UIAUTOMATOR = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_COMPOSE_TAG,
        value = ADDRESSBAR_URL_BOX,
        description = "URL box",
        groups = listOf(),
    )

    val TAB_COUNTER_UIAUTOMATOR = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_COMPOSE_TAG,
        value = TABS_COUNTER,
        description = "Tab counter button",
        groups = listOf(),
    )

    // Layout-agnostic tab counter. The TABS_COUNTER testTag only exists on the address-bar counter: with
    // shouldUseExpandedToolbar the counter moves into the bottom navigation bar and exposes no tag at all,
    // only this content-description. The description is present in BOTH layouts, so navigation edges that
    // must work either way should use this rather than the tag variants. Matching on the "Tabs Open:"
    // fragment covers the normal ("Non-private Tabs Open: N") and private ("Private Tabs Open: N") forms
    // and is independent of the tab count.
    val TAB_COUNTER_ANY_LAYOUT = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
        value = "Tabs Open:",
        description = "Tab counter button (either toolbar layout)",
        groups = listOf(),
    )

    val NEW_TAB_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
        value = "New tab",
        description = "New tab button",
        groups = listOf(),
    )

    val NEW_PRIVATE_TAB_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
        value = "New private tab",
        description = "New private tab button",
        groups = listOf(),
    )

    val EXPANDED_TOOLBAR_ADD_BOOKMARK_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
        value = getStringResource(R.string.browser_menu_bookmark_this_page_2),
        description = "Expanded toolbar bookmark page button",
        groups = listOf("expandedToolbarItem"),
    )

    val EXPANDED_TOOLBAR_EDIT_BOOKMARK_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
        value = getStringResource(R.string.browser_menu_edit_bookmark),
        description = "Expanded toolbar edit bookmark button",
        groups = listOf("expandedToolbarItem"),
    )

    val EXPANDED_TOOLBAR_SHARE_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
        value = getStringResource(R.string.browser_menu_share),
        description = "Expanded toolbar share button",
        groups = listOf("expandedToolbarItem"),
    )

    val EXPANDED_TOOLBAR_BACK_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
        value = getStringResource(R.string.browser_menu_back),
        description = "Expanded toolbar back button",
        groups = listOf("expandedToolbarInLandscapeItem"),
    )

    val EXPANDED_TOOLBAR_FORWARD_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
        value = getStringResource(R.string.browser_menu_forward),
        description = "Expanded toolbar forward button",
        groups = listOf("expandedToolbarInLandscapeItem"),
    )

    val EXPANDED_TOOLBAR_REFRESH_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
        value = getStringResource(R.string.browser_menu_refresh),
        description = "Expanded toolbar refresh button",
        groups = listOf("expandedToolbarInLandscapeItem"),
    )

    val SITE_INFO_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
        value = "Site information",
        description = "Site information button",
        groups = listOf(),
    )

    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun SEARCH_ENGINE_SELECTOR_ICON(searchEngineName: String = "") = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = getStringResource(R.string.search_engine_selector_content_description, searchEngineName),
        description = "Search engine selector icon",
        groups = listOf("homeScreenToolbar"),
    )

    val READER_VIEW_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = getStringResource(R.string.browser_menu_read),
        description = "Reader view toolbar button",
        groups = listOf(),
    )

    val READER_VIEW_CLOSE_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = getStringResource(R.string.browser_menu_read_close),
        description = "Close reader view toolbar button",
        groups = listOf(),
    )

    // UIAutomator rather than Compose: this is asserted on BrowserPage with GeckoView active, where
    // Compose sync can hang (same reason TAB_COUNTER_UIAUTOMATOR exists).
    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun TAB_COUNTER_WITH_COUNT(openTabs: String = "") = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
        value = "Non-private Tabs Open: $openTabs",
        description = "Tab counter showing $openTabs open tab(s)",
        groups = listOf(),
    )

    // UIAutomator rather than Compose: asserted on BrowserPage with GeckoView active (see
    // TAB_COUNTER_WITH_COUNT). The capitalized "Private Tabs Open:" is distinct from the normal
    // counter's "Non-private Tabs Open:" fragment, so a description-contains match won't cross over.
    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun PRIVATE_TAB_COUNTER_WITH_COUNT(openTabs: String = "") = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
        value = "Private Tabs Open: $openTabs",
        description = "Private tab counter showing $openTabs open tab(s)",
        groups = listOf(),
    )

    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun TAB_STRIP_TAB_COUNTER_WITH_COUNT(openTabs: String = "") = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = "Non-private Tabs Open: $openTabs. Tap to switch tabs.",
        description = "Tab strip tab counter showing $openTabs open tab(s)",
        groups = listOf(),
    )

    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun TAB_STRIP_TAB(tabTitle: String = "") = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = tabTitle,
        description = "Tab strip tab '$tabTitle'",
        groups = listOf(),
    )

    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun TAB_STRIP_CLOSE_TAB_BUTTON(tabTitle: String = "") = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
        value = "Close tab $tabTitle",
        description = "Tab strip close button for '$tabTitle'",
        groups = listOf(),
    )

    val INSECURE_CONNECTION_INFORMATION_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = SITE_INFO_INSECURE_CONNECTION,
        description = "Insecure connection information button",
        groups = listOf("browserViewToolbarItems"),
    )

    val SECURE_SITE_INFORMATION_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = SITE_INFO_SECURE,
        description = "Secure site information button",
        groups = listOf("browserViewToolbarItems"),
    )

    val TRACKING_PROTECTION_OFF_INFORMATION_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = SITE_INFO_TRACKING_PROTECTION_OFF,
        description = "Tracking protection off information button",
        groups = listOf("browserViewToolbarItems"),
    )

    val UNKNOWN_SITE_INFORMATION_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = SITE_INFO_UNKNOWN,
        description = "Unknown-state site information button",
        groups = listOf("browserViewToolbarItems"),
    )

    val all = listOf(
        TOOLBAR,
        TAB_COUNTER,
        TAB_COUNTER_UIAUTOMATOR,
        TAB_COUNTER_ANY_LAYOUT,
        TOOLBAR_URL_BOX,
        TOOLBAR_URL_BOX_UIAUTOMATOR,
        NEW_TAB_BUTTON,
        NEW_PRIVATE_TAB_BUTTON,
        EXPANDED_TOOLBAR_ADD_BOOKMARK_BUTTON,
        EXPANDED_TOOLBAR_EDIT_BOOKMARK_BUTTON,
        EXPANDED_TOOLBAR_SHARE_BUTTON,
        EXPANDED_TOOLBAR_BACK_BUTTON,
        EXPANDED_TOOLBAR_FORWARD_BUTTON,
        EXPANDED_TOOLBAR_REFRESH_BUTTON,
        SITE_INFO_BUTTON,
        READER_VIEW_BUTTON,
        READER_VIEW_CLOSE_BUTTON,
        SEARCH_ENGINE_SELECTOR_ICON(),
        TAB_COUNTER_WITH_COUNT(),
        PRIVATE_TAB_COUNTER_WITH_COUNT(),
        TAB_STRIP_TAB_COUNTER_WITH_COUNT(),
        TAB_STRIP_TAB(),
        TAB_STRIP_CLOSE_TAB_BUTTON(),
        INSECURE_CONNECTION_INFORMATION_BUTTON,
        SECURE_SITE_INFORMATION_BUTTON,
        TRACKING_PROTECTION_OFF_INFORMATION_BUTTON,
        UNKNOWN_SITE_INFORMATION_BUTTON,
    )
}
