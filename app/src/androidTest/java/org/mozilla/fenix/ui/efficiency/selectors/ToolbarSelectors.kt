/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import mozilla.components.compose.browser.toolbar.concept.BrowserToolbarTestTags.ADDRESSBAR_URL_BOX
import mozilla.components.compose.browser.toolbar.concept.BrowserToolbarTestTags.TABS_COUNTER
import org.mozilla.fenix.R
import org.mozilla.fenix.components.toolbar.BrowserToolbarTestTags.SITE_INFO_SECURE
import org.mozilla.fenix.components.toolbar.BrowserToolbarTestTags.SITE_INFO_UNKNOWN
import org.mozilla.fenix.components.toolbar.BrowserToolbarTestTags.SITE_INFO_UNSECURE
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

    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun SEARCH_ENGINE_SELECTOR_ICON(searchEngineName: String = "") = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = getStringResource(R.string.search_engine_selector_content_description, searchEngineName),
        description = "Search engine selector icon",
        groups = listOf("homeScreenToolbar"),
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

    val SECURE_SITE_INFORMATION_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = SITE_INFO_SECURE,
        description = "Secure site information button",
        groups = listOf("browserViewToolbarItems"),
    )

    val UNSECURE_SITE_INFORMATION_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = SITE_INFO_UNSECURE,
        description = "Unsecure site information button",
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
        SEARCH_ENGINE_SELECTOR_ICON(),
        TAB_COUNTER_WITH_COUNT(),
        SECURE_SITE_INFORMATION_BUTTON,
        UNSECURE_SITE_INFORMATION_BUTTON,
        UNKNOWN_SITE_INFORMATION_BUTTON,
    )
}
