/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.R
import org.mozilla.fenix.components.menu.MenuDialogTestTag
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.helpers.TestHelper.appName
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

object MainMenuSelectors {

    val NEW_PRIVATE_TAB_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.browser_menu_new_private_tab),
        description = "Main menu New private tab button",
        // Removed in https://bugzilla.mozilla.org/show_bug.cgi?id=1966222 as part of the menu redesign effort
        groups = listOf("removedIn=141"),
    )

    val EXTENSIONS_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.browser_menu_extensions),
        description = "Main menu Extensions button",
        groups = listOf("requiredForPage", "homePageMainMenuItems", "browserViewMainMenuItems"),
    )

    val BOOKMARKS_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.library_bookmarks),
        description = "Main menu Bookmarks button",
        // requiresScroll: below the fold in the landscape browser main menu; a no-op in portrait
        // where the item is already displayed (mozSwipeTo returns before swiping).
        groups = listOf("requiredForPage", "homePageMainMenuItems", "browserViewMainMenuItems", "requiresScroll"),
    )

    val HISTORY_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.library_history),
        description = "Main menu History button",
        groups = listOf("requiredForPage", "homePageMainMenuItems", "browserViewMainMenuItems", "requiresScroll"),
    )

    val DOWNLOADS_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.library_downloads),
        description = "Main menu Downloads button",
        groups = listOf("requiredForPage", "homePageMainMenuItems", "browserViewMainMenuItems", "requiresScroll"),
    )

    val PASSWORDS_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.browser_menu_passwords),
        description = "Main menu Passwords button",
        groups = listOf("requiredForPage", "homePageMainMenuItems", "browserViewMainMenuItems", "requiresScroll"),
    )

    val SIGN_IN_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.browser_menu_sign_in),
        description = "Main menu Sign in button",
        groups = listOf("requiredForPage", "homePageMainMenuItems", "browserViewMainMenuItems", "requiresScroll"),
    )

    val SETTINGS_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.browser_menu_settings),
        description = "Main menu Settings button",
        groups = listOf("requiredForPage", "homePageMainMenuItems", "browserViewMainMenuItems", "requiresScroll"),
    )

    // UIAutomator, not Compose: with shouldUseExpandedToolbar the menu renders differently and the Compose
    // content-description lookup finds nothing, while the device-level one resolves in both layouts. This
    // mirrors what the legacy ThreeDotMenuMainRobot.verifyPageMainMenuItems does (itemWithDescription).
    val BOOKMARK_THIS_PAGE_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
        value = getStringResource(R.string.browser_menu_bookmark_this_page_2),
        description = "Bookmark this page button",
        groups = listOf("bookmarkActions", "browserViewMainMenuItems"),
    )

    val EDIT_BOOKMARK_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = getStringResource(R.string.browser_menu_edit_bookmark),
        description = "Edit bookmark button",
        groups = listOf("editBookmarkActions"),
    )

    val FIND_IN_PAGE_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = getStringResource(R.string.browser_menu_find_in_page),
        description = "Main menu Find in page button",
        groups = listOf("browserViewMainMenuItems"),
    )

    val BACK_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = "Back",
        description = "Main menu Back button",
        groups = listOf("browserViewMainMenuItems"),
    )

    val FORWARD_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = "Forward",
        description = "Main menu Forward button",
        groups = listOf("browserViewMainMenuItems"),
    )

    val REFRESH_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = "Refresh",
        description = "Main menu Refresh button",
        groups = listOf("browserViewMainMenuItems"),
    )

    val SHARE_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = "Share",
        description = "Main menu Share button",
        groups = listOf("browserViewMainMenuItems"),
    )

    val DESKTOP_SITE_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION_SUBSTRING,
        value = getStringResource(R.string.browser_menu_desktop_site),
        description = "Main menu Desktop site button",
        groups = listOf("browserViewMainMenuItems"),
    )

    val DESKTOP_SITE_ON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = MenuDialogTestTag.DESKTOP_SITE_ON,
        description = "Main menu Desktop site ON state",
        groups = listOf(),
    )

    val DESKTOP_SITE_OFF = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = MenuDialogTestTag.DESKTOP_SITE_OFF,
        description = "Main menu Desktop site OFF state",
        groups = listOf(),
    )

    val MORE_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = "More Collapsed",
        description = "Main menu More button",
        groups = listOf("browserViewMainMenuItems"),
    )

    val REPORT_BROKEN_SITE_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = getStringResource(R.string.browser_menu_webcompat_reporter_2),
        description = "Main menu Report broken site button",
        groups = listOf("browserViewMainMenuMoreItems"),
    )

    // The Extensions row opens its submenu in a new window, so match it by res-id at the device level
    // (the node sets testTagsAsResourceId) rather than through Compose. Mirrors the legacy
    // itemWithResId("mainMenu.extensions").clickAndWaitForNewWindow. EXTENSIONS_BUTTON above is the
    // Compose-text twin, used for verifying the row is present rather than for opening it.
    val EXTENSIONS_BUTTON_UIAUTOMATOR = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_COMPOSE_TAG,
        value = MenuDialogTestTag.EXTENSIONS,
        description = "Main menu Extensions button (UIAutomator)",
        groups = listOf(),
    )

    val EXTENSIONS_CHEVRON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = MenuDialogTestTag.EXTENSIONS_OPTION_CHEVRON,
        description = "Main menu Extensions expand/collapse chevron",
        groups = listOf(),
    )

    val TRY_RECOMMENDED_EXTENSION_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION_SUBSTRING,
        value = "Extensions Try a recommended extension",
        description = "Main menu Extensions - Try a recommended extension button",
        groups = listOf(),
    )

    val DISCOVER_MORE_EXTENSIONS_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.browser_menu_discover_more_extensions),
        description = "Main menu Discover more extensions button",
        groups = listOf("expandedExtensionsMenuItems"),
    )

    // Shown in the expanded Extensions submenu once at least one extension is installed; opens the
    // full add-ons manager. Mirrors the legacy clickManageExtensionsButtonFromRedesignedMainMenu.
    val MANAGE_EXTENSIONS_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.browser_menu_manage_extensions),
        description = "Main menu Manage extensions button",
        groups = listOf(),
    )

    // The "Add <addon>" install icon on a recommended addon row in the expanded Extensions submenu.
    // Keyed on the addon name because there is no test tag on the install icon (see AddonMenuItem);
    // matched at the device level like the legacy installRecommendedAddon (itemWithDescription).
    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun RECOMMENDED_ADDON_INSTALL_BUTTON(addonTitle: String = "") = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
        value = getStringResource(R.string.browser_menu_extension_plus_icon_content_description_2, addonTitle),
        description = "Install recommended addon '$addonTitle' button",
        groups = listOf(),
    )

    // Keyed on the addon row rather than the addon name: which addons AMO recommends is server-driven,
    // so matching names would couple the test to remote data. Must be the row tag, not
    // RECOMMENDED_ADDON_ITEM_TITLE — that one is applied via labelModifier, so the row's merged
    // semantics hide it and onAllNodesWithTag (merged tree) cannot see it.
    val RECOMMENDED_ADDON_ITEM = Selector(
        strategy = SelectorStrategy.COMPOSE_ON_ALL_NODES_BY_TAG_ON_FIRST,
        value = MenuDialogTestTag.RECOMMENDED_ADDON_ITEM,
        description = "Recommended addon row in the expanded Extensions submenu",
        groups = listOf("expandedExtensionsMenuItems"),
    )

    // TODO (M. Barone 3/20/2026): add getting 'appName' to our base helpers
    val DEFAULT_BROWSER_BANNER_TITLE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.browser_menu_default_banner_title, appName),
        description = "Make Firefox your default banner title",
        groups = listOf("homeBanner", "homePageMainMenuItems"),
    )

    val DEFAULT_BROWSER_BANNER_SUBTITLE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.browser_menu_default_banner_subtitle_2),
        description = "Make Firefox your default banner subtitle",
        groups = listOf("homeBanner", "homePageMainMenuItems"),
    )

    val DEFAULT_BROWSER_BANNER_DISMISS = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = getStringResource(R.string.browser_menu_default_banner_dismiss_promotion),
        description = "Make Firefox your default banner dismiss button",
        groups = listOf("homeBanner", "homePageMainMenuItems"),
    )

    // Quit is the last item in the scrollable main menu, so mark it requiresScroll: the framework
    // then polls/swipes it into view (ensureReachable -> mozSwipeTo) before clicking, instead of
    // asserting on it one-shot while the menu is still settling or the item is below the fold.
    val QUIT_FIREFOX_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = "Quit $appName",
        description = "Quit Firefox button",
        groups = listOf("requiresScroll"),
    )

    val CUSTOMIZE_HOMEPAGE_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = getStringResource(R.string.browser_menu_customize_homepage),
        description = "Customize homepage Settings button",
        groups = listOf("homePageMainMenuItems"),
    )

    val SAVE_TO_COLLECTIONS_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = getStringResource(R.string.browser_menu_save_to_collection_2),
        description = "Save to collections button",
        groups = listOf("moreMainMenuItems"),
    )

    val ADD_TO_SHORTCUTS_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = getStringResource(R.string.browser_menu_add_to_shortcuts),
        description = "Main menu Add to shortcuts button",
        groups = listOf("browserViewMainMenuMoreItems"),
    )

    val TRANSLATE_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = getStringResource(R.string.browser_menu_translations),
        description = "Translate page button",
        groups = listOf("moreMenuItems"),
    )

    val TRANSLATED_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = getStringResource(R.string.browser_menu_translated),
        description = "Translate page button",
        groups = listOf("moreMenuItems"),
    )

    val SAVE_AS_PDF_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = getStringResource(R.string.browser_menu_save_as_pdf_2),
        description = "Main menu save as PDF button",
        groups = listOf("moreMenuItems"),
    )

    val PRINT_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = getStringResource(R.string.browser_menu_print_2),
        description = "Print page button",
        groups = listOf("moreMenuItems"),
    )

    val REMOVE_FROM_SHORTCUTS_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = getStringResource(R.string.browser_menu_remove_from_shortcuts),
        description = "Main menu remove from shortcuts button",
        groups = listOf("browserViewMainMenuMoreItems"),
    )

    val ADD_TO_HOMESCREEN_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = getStringResource(R.string.browser_menu_add_to_homescreen),
        description = "Main menu add to homescreen button",
        groups = listOf("browserViewMainMenuMoreItems"),
    )

    val ADD_APP_TO_HOMESCREEN_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = getStringResource(R.string.browser_menu_add_app_to_homescreen),
        description = "Main menu add app (PWA) to homescreen button",
        groups = listOf("browserViewMainMenuMoreItems"),
    )

    val OPEN_IN_APP_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = getStringResource(R.string.browser_menu_open_app_link),
        description = "Main menu Open in app button",
        groups = listOf("browserViewMainMenuMoreItems"),
    )

    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun OPEN_IN_APP_NAME_BUTTON(appName: String = "") = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = getStringResource(R.string.browser_menu_open_in_fenix, appName),
        description = "Main menu Open in $appName button",
        groups = listOf(),
    )

    // Only present when the browser is showing reader view.
    val CUSTOMIZE_READER_VIEW_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = getStringResource(R.string.browser_menu_customize_reader_view_2),
        description = "Main menu Customize Reader View button",
        groups = listOf(),
    )

    // Order matters: mozVerifyElementsByGroup verifies in this declaration order. In the landscape
    // browser main menu the list scrolls, so the always-visible items must be verified first and the
    // requiresScroll items last, in on-screen top-to-bottom order (History -> ... -> Settings) — once
    // we swipe down to reach them the top row leaves the viewport and can no longer be asserted.
    val all = listOf(
        NEW_PRIVATE_TAB_BUTTON,
        EXTENSIONS_BUTTON,
        BOOKMARK_THIS_PAGE_BUTTON,
        EDIT_BOOKMARK_BUTTON,
        FIND_IN_PAGE_BUTTON,
        DEFAULT_BROWSER_BANNER_TITLE,
        DEFAULT_BROWSER_BANNER_SUBTITLE,
        DEFAULT_BROWSER_BANNER_DISMISS,
        QUIT_FIREFOX_BUTTON,
        BACK_BUTTON,
        FORWARD_BUTTON,
        REFRESH_BUTTON,
        SHARE_BUTTON,
        DESKTOP_SITE_BUTTON,
        DESKTOP_SITE_ON,
        DESKTOP_SITE_OFF,
        MORE_BUTTON,
        CUSTOMIZE_HOMEPAGE_BUTTON,
        REPORT_BROKEN_SITE_BUTTON,
        EXTENSIONS_BUTTON_UIAUTOMATOR,
        EXTENSIONS_CHEVRON,
        TRY_RECOMMENDED_EXTENSION_BUTTON,
        DISCOVER_MORE_EXTENSIONS_BUTTON,
        MANAGE_EXTENSIONS_BUTTON,
        RECOMMENDED_ADDON_INSTALL_BUTTON(),
        RECOMMENDED_ADDON_ITEM,
        HISTORY_BUTTON,
        BOOKMARKS_BUTTON,
        DOWNLOADS_BUTTON,
        PASSWORDS_BUTTON,
        SIGN_IN_BUTTON,
        SETTINGS_BUTTON,
        SAVE_TO_COLLECTIONS_BUTTON,
        ADD_TO_SHORTCUTS_BUTTON,
        TRANSLATE_BUTTON,
        TRANSLATED_BUTTON,
        SAVE_AS_PDF_BUTTON,
        PRINT_BUTTON,
        REMOVE_FROM_SHORTCUTS_BUTTON,
        ADD_TO_HOMESCREEN_BUTTON,
        CUSTOMIZE_READER_VIEW_BUTTON,
        ADD_APP_TO_HOMESCREEN_BUTTON,
        OPEN_IN_APP_BUTTON,
        OPEN_IN_APP_NAME_BUTTON(),
    )
}
