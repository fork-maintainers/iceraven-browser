/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import mozilla.components.compose.menu.R as MozacMenuR
import mozilla.components.feature.summarize.R as summarizeR
import org.mozilla.fenix.R
import org.mozilla.fenix.components.menu.MenuDialogTestTag
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.helpers.TestHelper.appName
import org.mozilla.fenix.ui.efficiency.helpers.PageReadinessProfiles
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorContainer
import org.mozilla.fenix.ui.efficiency.helpers.SelectorGroup
import org.mozilla.fenix.ui.efficiency.helpers.SelectorLifecycle
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy
import org.mozilla.fenix.ui.efficiency.helpers.SwipeDirection

object MainMenuSelectors : SelectorContainer {
    enum class Group : SelectorGroup {
        HOME_PAGE_MAIN_MENU_ITEMS,
        BROWSER_VIEW_MAIN_MENU_ITEMS,
        BOOKMARK_ACTIONS,
        EDIT_BOOKMARK_ACTIONS,
        BROWSER_VIEW_MAIN_MENU_MORE_ITEMS,
        MORE_MAIN_MENU_SUB_LIST,
        EXPANDED_EXTENSIONS_MENU_ITEMS,
        HOME_BANNER,
        MORE_MAIN_MENU_ITEMS,
        MORE_MENU_ITEMS,
        SIGN_IN_TO_SUMMARIZE_BOTTOM_SHEET,
    }

    val MAIN_MENU_ANCHOR =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_COMPOSE_TAG,
            value = MenuDialogTestTag.EXTENSIONS,
            description = "Main menu Extensions item anchor",
            readiness = PageReadinessProfiles.IDENTITY_ANCHOR,
        )

    val NEW_PRIVATE_TAB_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(R.string.browser_menu_new_private_tab),
            description = "Main menu New private tab button",
            // Removed in https://bugzilla.mozilla.org/show_bug.cgi?id=1966222 as part of the menu redesign effort
            lifecycle = SelectorLifecycle.RemovedIn(141),
        )

    val EXTENSIONS_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(R.string.browser_menu_extensions),
            description = "Main menu Extensions button",
            groups = setOf(Group.HOME_PAGE_MAIN_MENU_ITEMS, Group.BROWSER_VIEW_MAIN_MENU_ITEMS),
        )

    val BOOKMARKS_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(R.string.library_bookmarks),
            description = "Main menu Bookmarks button",
            // Below the fold in the landscape browser main menu; a no-op in portrait.
            // where the item is already displayed (mozSwipeTo returns before swiping).
            groups = setOf(Group.HOME_PAGE_MAIN_MENU_ITEMS, Group.BROWSER_VIEW_MAIN_MENU_ITEMS),
            scrollDirection = SwipeDirection.UP,
        )

    val HISTORY_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(R.string.library_history),
            description = "Main menu History button",
            groups = setOf(Group.HOME_PAGE_MAIN_MENU_ITEMS, Group.BROWSER_VIEW_MAIN_MENU_ITEMS),
            scrollDirection = SwipeDirection.UP,
        )

    val DOWNLOADS_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(R.string.library_downloads),
            description = "Main menu Downloads button",
            groups = setOf(Group.HOME_PAGE_MAIN_MENU_ITEMS, Group.BROWSER_VIEW_MAIN_MENU_ITEMS),
            scrollDirection = SwipeDirection.UP,
        )

    val PASSWORDS_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(R.string.browser_menu_passwords),
            description = "Main menu Passwords button",
            groups = setOf(Group.HOME_PAGE_MAIN_MENU_ITEMS, Group.BROWSER_VIEW_MAIN_MENU_ITEMS),
            scrollDirection = SwipeDirection.UP,
        )

    val SIGN_IN_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(R.string.browser_menu_sign_in),
            description = "Main menu Sign in button",
            groups = setOf(Group.HOME_PAGE_MAIN_MENU_ITEMS, Group.BROWSER_VIEW_MAIN_MENU_ITEMS),
            scrollDirection = SwipeDirection.UP,
        )

    val SETTINGS_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(R.string.browser_menu_settings),
            description = "Main menu Settings button",
            groups = setOf(Group.HOME_PAGE_MAIN_MENU_ITEMS, Group.BROWSER_VIEW_MAIN_MENU_ITEMS),
            scrollDirection = SwipeDirection.UP,
        )

    // UIAutomator, not Compose: with shouldUseExpandedToolbar the menu renders differently and the Compose
    // content-description lookup finds nothing, while the device-level one resolves in both layouts. This
    // mirrors what the legacy ThreeDotMenuMainRobot.verifyPageMainMenuItems does (itemWithDescription).
    val BOOKMARK_THIS_PAGE_BUTTON =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
            value = getStringResource(R.string.browser_menu_bookmark_this_page_2),
            description = "Bookmark this page button",
            groups = setOf(Group.BOOKMARK_ACTIONS, Group.BROWSER_VIEW_MAIN_MENU_ITEMS),
        )

    val EDIT_BOOKMARK_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
            value = getStringResource(R.string.browser_menu_edit_bookmark),
            description = "Edit bookmark button",
            groups = setOf(Group.EDIT_BOOKMARK_ACTIONS),
        )

    val FIND_IN_PAGE_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
            value = getStringResource(R.string.browser_menu_find_in_page),
            description = "Main menu Find in page button",
            groups = setOf(Group.BROWSER_VIEW_MAIN_MENU_ITEMS),
        )

    val BACK_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = "Back",
            description = "Main menu Back button",
            groups = setOf(Group.BROWSER_VIEW_MAIN_MENU_ITEMS),
        )

    val FORWARD_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = "Forward",
            description = "Main menu Forward button",
            groups = setOf(Group.BROWSER_VIEW_MAIN_MENU_ITEMS),
        )

    val REFRESH_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = "Refresh",
            description = "Main menu Refresh button",
            groups = setOf(Group.BROWSER_VIEW_MAIN_MENU_ITEMS),
        )

    val SHARE_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = "Share",
            description = "Main menu Share button",
            groups = setOf(Group.BROWSER_VIEW_MAIN_MENU_ITEMS),
        )

    val DESKTOP_SITE_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION_SUBSTRING,
            value = getStringResource(R.string.browser_menu_desktop_site),
            description = "Main menu Desktop site button",
            groups = setOf(Group.BROWSER_VIEW_MAIN_MENU_ITEMS),
        )

    val DESKTOP_SITE_ON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TAG,
            value = MenuDialogTestTag.DESKTOP_SITE_ON,
            description = "Main menu Desktop site ON state",
        )

    val DESKTOP_SITE_OFF =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TAG,
            value = MenuDialogTestTag.DESKTOP_SITE_OFF,
            description = "Main menu Desktop site OFF state",
        )

    val MORE_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
            value = "More Collapsed",
            description = "Main menu More button",
            groups = setOf(Group.BROWSER_VIEW_MAIN_MENU_ITEMS),
        )

    val REPORT_BROKEN_SITE_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
            value = getStringResource(R.string.browser_menu_webcompat_reporter_2),
            description = "Main menu Report broken site button",
            groups = setOf(Group.BROWSER_VIEW_MAIN_MENU_MORE_ITEMS, Group.MORE_MAIN_MENU_SUB_LIST),
        )

    // The Extensions row opens its submenu in a new window, so match it by res-id at the device level
    // (the node sets testTagsAsResourceId) rather than through Compose. Mirrors the legacy
    // itemWithResId("mainMenu.extensions").clickAndWaitForNewWindow. EXTENSIONS_BUTTON above is the
    // Compose-text twin, used for verifying the row is present rather than for opening it.
    val EXTENSIONS_BUTTON_UIAUTOMATOR =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_COMPOSE_TAG,
            value = MenuDialogTestTag.EXTENSIONS,
            description = "Main menu Extensions button (UIAutomator)",
        )

    val EXTENSIONS_CHEVRON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TAG,
            value = MenuDialogTestTag.EXTENSIONS_OPTION_CHEVRON,
            description = "Main menu Extensions expand/collapse chevron",
        )

    val TRY_RECOMMENDED_EXTENSION_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION_SUBSTRING,
            value = "Extensions Try a recommended extension",
            description = "Main menu Extensions - Try a recommended extension button",
        )

    // Shown on the collapsed Extensions row when extensions are installed but all disabled. Mirrors the
    // legacy verifyNoExtensionsEnabledButton (contentDescription "Extensions No extensions enabled").
    val NO_EXTENSIONS_ENABLED_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION_SUBSTRING,
            value = "Extensions No extensions enabled",
            description = "Main menu Extensions - No extensions enabled button",
        )

    val DISCOVER_MORE_EXTENSIONS_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(R.string.browser_menu_discover_more_extensions),
            description = "Main menu Discover more extensions button",
            groups = setOf(Group.EXPANDED_EXTENSIONS_MENU_ITEMS),
        )

    // Shown in the expanded Extensions submenu once at least one extension is installed; opens the
    // full add-ons manager. Mirrors the legacy clickManageExtensionsButtonFromRedesignedMainMenu.
    val MANAGE_EXTENSIONS_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(R.string.browser_menu_manage_extensions),
            description = "Main menu Manage extensions button",
        )

    // The "Add <addon>" install icon on a recommended addon row in the expanded Extensions submenu.
    // Keyed on the addon name because there is no test tag on the install icon (see AddonMenuItem);
    // matched at the device level like the legacy installRecommendedAddon (itemWithDescription).
    @Suppress("FunctionName")
    fun RECOMMENDED_ADDON_INSTALL_BUTTON(addonTitle: String = "") =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
            value = getStringResource(R.string.browser_menu_extension_plus_icon_content_description_2, addonTitle),
            description = "Install recommended addon '$addonTitle' button",
        )

    // Keyed on the addon row rather than the addon name: which addons AMO recommends is server-driven,
    // so matching names would couple the test to remote data. Must be the row tag, not
    // RECOMMENDED_ADDON_ITEM_TITLE — that one is applied via labelModifier, so the row's merged
    // semantics hide it and onAllNodesWithTag (merged tree) cannot see it.
    val RECOMMENDED_ADDON_ITEM =
        Selector(
            strategy = SelectorStrategy.COMPOSE_ON_ALL_NODES_BY_TAG_ON_FIRST,
            value = MenuDialogTestTag.RECOMMENDED_ADDON_ITEM,
            description = "Recommended addon row in the expanded Extensions submenu",
            groups = setOf(Group.EXPANDED_EXTENSIONS_MENU_ITEMS),
        )

    @Suppress("FunctionName")
    fun EXTENSIONS_BUTTON_WITH_INSTALLED_EXTENSION(addonTitle: String = "") =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TAG_AND_CONTENT_DESCRIPTION_SUBSTRING,
            value = MenuDialogTestTag.EXTENSIONS,
            secondaryValue = addonTitle,
            description = "Extensions menu row advertising installed extension '$addonTitle'",
        )

    // The installed extension row in the expanded Extensions submenu. Mirrors the legacy
    // verifyInstalledExtension (hasTestTag(WEB_EXTENSION_ITEM) + content description contains <addon>).
    @Suppress("FunctionName")
    fun INSTALLED_EXTENSION_ITEM(addonTitle: String = "") =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TAG_AND_CONTENT_DESCRIPTION_SUBSTRING,
            value = MenuDialogTestTag.WEB_EXTENSION_ITEM,
            secondaryValue = addonTitle,
            description = "Installed extension '$addonTitle' row in the expanded Extensions submenu",
        )

    // TODO (M. Barone 3/20/2026): add getting 'appName' to our base helpers
    val DEFAULT_BROWSER_BANNER_TITLE =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(R.string.browser_menu_default_banner_title, appName),
            description = "Make Firefox your default banner title",
            groups = setOf(Group.HOME_BANNER, Group.HOME_PAGE_MAIN_MENU_ITEMS),
        )

    val DEFAULT_BROWSER_BANNER_SUBTITLE =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(R.string.browser_menu_default_banner_subtitle_2),
            description = "Make Firefox your default banner subtitle",
            groups = setOf(Group.HOME_BANNER, Group.HOME_PAGE_MAIN_MENU_ITEMS),
        )

    val DEFAULT_BROWSER_BANNER_DISMISS =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
            value = getStringResource(MozacMenuR.string.mozac_menu_banner_dismiss_description),
            description = "Make Firefox your default banner dismiss button",
            groups = setOf(Group.HOME_BANNER, Group.HOME_PAGE_MAIN_MENU_ITEMS),
        )

    // Quit is the last item in the scrollable main menu, so give it an explicit scroll direction: the framework
    // then polls/swipes it into view (ensureReachable -> mozSwipeTo) before clicking, instead of
    // asserting on it one-shot while the menu is still settling or the item is below the fold.
    val QUIT_FIREFOX_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
            value = "Quit $appName",
            description = "Quit Firefox button",
            scrollDirection = SwipeDirection.UP,
        )

    val CUSTOMIZE_HOMEPAGE_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
            value = getStringResource(R.string.browser_menu_customize_homepage),
            description = "Customize homepage Settings button",
            groups = setOf(Group.HOME_PAGE_MAIN_MENU_ITEMS),
        )

    val SAVE_TO_COLLECTIONS_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
            value = getStringResource(R.string.browser_menu_save_to_collection_2),
            description = "Save to collections button",
            groups = setOf(Group.MORE_MAIN_MENU_ITEMS, Group.MORE_MAIN_MENU_SUB_LIST),
        )

    val ADD_TO_SHORTCUTS_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
            value = getStringResource(R.string.browser_menu_add_to_shortcuts),
            description = "Main menu Add to shortcuts button",
            groups = setOf(Group.BROWSER_VIEW_MAIN_MENU_MORE_ITEMS, Group.MORE_MAIN_MENU_SUB_LIST),
        )

    val TRANSLATE_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
            value = getStringResource(R.string.browser_menu_translations),
            description = "Translate page button",
            groups = setOf(Group.MORE_MENU_ITEMS, Group.MORE_MAIN_MENU_SUB_LIST),
        )

    val TRANSLATED_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
            value = getStringResource(R.string.browser_menu_translated),
            description = "Translate page button",
            groups = setOf(Group.MORE_MENU_ITEMS),
        )

    val SAVE_AS_PDF_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
            value = getStringResource(R.string.browser_menu_save_as_pdf_2),
            description = "Main menu save as PDF button",
            groups = setOf(Group.MORE_MENU_ITEMS, Group.MORE_MAIN_MENU_SUB_LIST),
        )

    val SUMMARIZE_PAGE_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
            value = getStringResource(R.string.browser_menu_summarize_page),
            description = "Main menu Summarize page button",
            groups = setOf(Group.MORE_MAIN_MENU_SUB_LIST),
        )

    // The "Sign in to summarize" bottom sheet shown when an unauthenticated user taps Summarize page.
    val SIGN_IN_TO_SUMMARIZE_TITLE =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(summarizeR.string.mozac_summarize_fxa_sign_in_title),
            description = "Sign in to summarize bottom sheet title",
            groups = setOf(Group.SIGN_IN_TO_SUMMARIZE_BOTTOM_SHEET),
        )

    // The message and its inline "Learn more" link render as a single text node, so match them joined.
    val SIGN_IN_TO_SUMMARIZE_MESSAGE =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value =
                getStringResource(summarizeR.string.mozac_summarize_fxa_sign_in_message) +
                    " " +
                    getStringResource(summarizeR.string.mozac_summarize_learn_more_link),
            description = "Sign in to summarize bottom sheet message",
            groups = setOf(Group.SIGN_IN_TO_SUMMARIZE_BOTTOM_SHEET),
        )

    val SIGN_IN_TO_SUMMARIZE_SIGN_IN_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(summarizeR.string.mozac_summarize_fxa_sign_in_button_positive),
            description = "Sign in to summarize bottom sheet Sign in button",
            groups = setOf(Group.SIGN_IN_TO_SUMMARIZE_BOTTOM_SHEET),
        )

    val SIGN_IN_TO_SUMMARIZE_DISMISS_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(summarizeR.string.mozac_summarize_error_dissmiss),
            description = "Sign in to summarize bottom sheet Dismiss button",
            groups = setOf(Group.SIGN_IN_TO_SUMMARIZE_BOTTOM_SHEET),
        )

    val PRINT_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
            value = getStringResource(R.string.browser_menu_print_2),
            description = "Print page button",
            groups = setOf(Group.MORE_MENU_ITEMS, Group.MORE_MAIN_MENU_SUB_LIST),
        )

    val REMOVE_FROM_SHORTCUTS_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
            value = getStringResource(R.string.browser_menu_remove_from_shortcuts),
            description = "Main menu remove from shortcuts button",
            groups = setOf(Group.BROWSER_VIEW_MAIN_MENU_MORE_ITEMS),
        )

    val ADD_TO_HOMESCREEN_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
            value = getStringResource(R.string.browser_menu_add_to_homescreen),
            description = "Main menu add to homescreen button",
            groups = setOf(Group.BROWSER_VIEW_MAIN_MENU_MORE_ITEMS, Group.MORE_MAIN_MENU_SUB_LIST),
        )

    // Only present in the "More settings" submenu while a private tab is in the foreground; not part of the
    // MORE_MAIN_MENU_SUB_LIST group, which models the normal-browsing sub-list.
    val MOVE_TO_NON_PRIVATE_TAB_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
            value = getStringResource(R.string.browser_menu_move_to_non_private_tab),
            description = "Main menu Move to non-private tab button",
            groups = setOf(),
        )

    val ADD_APP_TO_HOMESCREEN_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
            value = getStringResource(R.string.browser_menu_add_app_to_homescreen),
            description = "Main menu add app (PWA) to homescreen button",
            groups = setOf(Group.BROWSER_VIEW_MAIN_MENU_MORE_ITEMS),
        )

    val OPEN_IN_APP_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
            value = getStringResource(R.string.browser_menu_open_app_link),
            description = "Main menu Open in app button",
            groups = setOf(Group.BROWSER_VIEW_MAIN_MENU_MORE_ITEMS, Group.MORE_MAIN_MENU_SUB_LIST),
        )

    @Suppress("FunctionName")
    fun OPEN_IN_APP_NAME_BUTTON(appName: String = "") =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
            value = getStringResource(R.string.browser_menu_open_in_fenix, appName),
            description = "Main menu Open in $appName button",
        )

    // Only present when the browser is showing reader view.
    val CUSTOMIZE_READER_VIEW_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
            value = getStringResource(R.string.browser_menu_customize_reader_view_2),
            description = "Main menu Customize Reader View button",
        )

    override val scrollTraversalOrder: Map<SelectorGroup, List<Selector>> =
        mapOf(
            Group.HOME_PAGE_MAIN_MENU_ITEMS to
                listOf(
                    HISTORY_BUTTON,
                    BOOKMARKS_BUTTON,
                    DOWNLOADS_BUTTON,
                    PASSWORDS_BUTTON,
                    SIGN_IN_BUTTON,
                    SETTINGS_BUTTON,
                ),
            Group.BROWSER_VIEW_MAIN_MENU_ITEMS to
                listOf(
                    HISTORY_BUTTON,
                    BOOKMARKS_BUTTON,
                    DOWNLOADS_BUTTON,
                    PASSWORDS_BUTTON,
                    SIGN_IN_BUTTON,
                    SETTINGS_BUTTON,
                ),
        )
}
