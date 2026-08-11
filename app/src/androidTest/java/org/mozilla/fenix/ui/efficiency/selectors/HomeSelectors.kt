/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

object HomeSelectors {
    val TOP_SITES_LIST = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "top_sites_list",
        description = "Top Sites List",
        groups = listOf("topSites"),
    )

    val TOP_SITES_LIST_COMPOSE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = "top_sites_list",
        description = "Top Sites List",
        groups = listOf("topSitesCompose"),
    )

    val HOMEPAGE_VIEW = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = "homepage.view",
        description = "Homepage view",
        groups = listOf("requiredForPage"),
    )

    val MAIN_MENU_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = getStringResource(R.string.content_description_menu),
        description = "Three Dot Menu",
        groups = listOf("requiredForPage"),
    )

    // Use UIAutomator when navigating from BrowserPage — avoids Compose sync hanging when GeckoView is active.
    val MAIN_MENU_BUTTON_UIAUTOMATOR = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
        value = getStringResource(R.string.content_description_menu),
        description = "Three Dot Menu",
        groups = listOf(),
    )

    val PRIVATE_BROWSING_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = getStringResource(R.string.content_description_private_browsing),
        description = "Private browsing button",
        groups = listOf("privateBrowsing"),
    )

    val HOME_WORDMARK_LOGO = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = "homepage.wordmark.logo",
        description = "the home screen wordmark logo",
        groups = listOf("homeScreen"),
    )

    val HOME_WORDMARK_TEXT = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = "homepage.wordmark.text",
        description = "the home screen wordmark text",
        groups = listOf("homeScreen"),
    )

    val COLLECTIONS_HEADER = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.collections_header),
        description = "the Collections header",
        groups = listOf("homeScreen"),
    )

    val TAB_COUNTER_ZERO = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = "Non-private Tabs Open: 0. Tap to switch tabs.",
        description = "the tab counter showing zero open tabs",
        groups = listOf("homeScreen"),
    )

    val JUMP_BACK_IN_SECTION = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.recent_tabs_header),
        description = "Jump Back In section header",
        groups = listOf("jumpBackIn"),
    )

    val JUMP_BACK_IN_SHOW_ALL = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = getStringResource(R.string.recent_tabs_show_all),
        description = "Jump Back In Show All button",
        groups = listOf("jumpBackIn"),
    )

    val RECENT_BOOKMARKS_SECTION = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = getStringResource(R.string.home_bookmarks_title),
        description = "Bookmarks section header",
        groups = listOf("recentBookmarksSection"),
    )

    val PRIVATE_BROWSING_INFO_CARD_TITLE = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = getStringResource(R.string.felt_privacy_desc_card_title),
        description = "Private browsing info card title",
        groups = listOf("privateBrowsingHomeScreen"),
    )

    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun TOP_SITE_ITEM(topSiteTitle: String = "") = Selector(
        strategy = SelectorStrategy.COMPOSE_ON_ALL_NODES_BY_TAG_WITH_CHILD_TEXT_ON_FIRST,
        value = "top_sites_list.top_site_item",
        secondaryValue = topSiteTitle,
        description = "Top site item with title: $topSiteTitle",
        groups = listOf("topSiteItem"),
    )

    val all = listOf(
        TOP_SITES_LIST,
        TOP_SITES_LIST_COMPOSE,
        HOMEPAGE_VIEW,
        MAIN_MENU_BUTTON,
        MAIN_MENU_BUTTON_UIAUTOMATOR,
        PRIVATE_BROWSING_BUTTON,
        TOP_SITES_LIST,
        HOME_WORDMARK_LOGO,
        HOME_WORDMARK_TEXT,
        COLLECTIONS_HEADER,
        TAB_COUNTER_ZERO,
        JUMP_BACK_IN_SECTION,
        JUMP_BACK_IN_SHOW_ALL,
        RECENT_BOOKMARKS_SECTION,
        PRIVATE_BROWSING_INFO_CARD_TITLE,
        TOP_SITE_ITEM(),
    )
}
