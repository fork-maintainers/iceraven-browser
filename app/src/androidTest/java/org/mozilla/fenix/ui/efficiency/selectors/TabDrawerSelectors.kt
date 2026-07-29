/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.DataGenerationHelper.getPluralStringResource
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.tabstray.TabsTrayTestTag
import org.mozilla.fenix.tabstray.TabsTrayTestTag.CLOSE_TAB_GROUP
import org.mozilla.fenix.tabstray.TabsTrayTestTag.GROUP_NAME
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

object TabDrawerSelectors {

    val TABS_TRAY = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = TabsTrayTestTag.TABS_TRAY,
        description = "Tabs tray container",
        groups = listOf("requiredForPage"),
    )

    val NORMAL_BROWSING_EMPTY_TABS_PAGE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = TabsTrayTestTag.EMPTY_NORMAL_TABS_LIST,
        description = "Normal browsing empty tabs tray page",
        groups = listOf("emptyNormalBrowsingTabDrawerView"),
    )

    val NORMAL_BROWSING_TABS_PAGE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = TabsTrayTestTag.NORMAL_TABS_LIST,
        description = "Normal browsing tabs tray page",
        groups = listOf("normalBrowsingTabDrawerView"),
    )

    val NORMAL_BROWSING_OPEN_TABS_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = TabsTrayTestTag.NORMAL_TABS_PAGE_BUTTON,
        description = "Normal browsing tabs tray button",
        groups = listOf("requiredForPage", "tabDrawerBannerButtons"),
    )

    val NORMAL_TABS_LIST = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = TabsTrayTestTag.NORMAL_TABS_LIST,
        description = "Normal tabs list grid view",
        groups = listOf(),
    )

    val EMPTY_NORMAL_TABS_LIST = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = TabsTrayTestTag.EMPTY_NORMAL_TABS_LIST,
        description = "Empty normal tabs list placeholder",
        groups = listOf(),
    )

    val TAB_ITEM_ROOT = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = TabsTrayTestTag.TAB_ITEM_ROOT,
        description = "Tab item root",
        groups = listOf(),
    )

    val SYNCED_TABS_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = TabsTrayTestTag.SYNCED_TABS_PAGE_BUTTON,
        description = "Synced tabs button",
        groups = listOf("requiredForPage", "tabDrawerBannerButtons"),
    )

    val SIGN_IN_TO_SYNC_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = getStringResource(R.string.tab_manager_empty_synced_tabs_page_sign_in_cta),
        description = "Sign in to sync button",
        groups = listOf("tabDrawerUnauthenticatedSyncedTabs"),
    )

    val UNAUTHENTICATED_SYNCED_TABS_PAGE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = TabsTrayTestTag.UNAUTHENTICATED_SYNCED_TABS_PAGE,
        description = "Unauthenticated synced tabs page",
        groups = listOf("tabDrawerUnauthenticatedSyncedTabs"),
    )

    val UNAUTHENTICATED_SYNCED_TABS_PAGE_HEADER = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = getStringResource(R.string.tab_manager_empty_synced_tabs_page_header),
        description = "Unauthenticated synced tabs page header",
        groups = listOf("tabDrawerUnauthenticatedSyncedTabs"),
    )

    val UNAUTHENTICATED_SYNCED_TABS_PAGE_DESCRIPTION = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = getStringResource(R.string.tab_manager_empty_synced_tabs_page_description),
        description = "Unauthenticated synced tabs page description",
        groups = listOf("tabDrawerUnauthenticatedSyncedTabs"),
    )

    val PRIVATE_TABS_PAGE_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = TabsTrayTestTag.PRIVATE_TABS_PAGE_BUTTON,
        description = "Private browsing tabs tray button",
        groups = listOf("requiredForPage", "tabDrawerBannerButtons"),
    )

    val TAB_GROUPS_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = TabsTrayTestTag.TAB_GROUPS_PAGE_BUTTON,
        description = "Tab groups button",
        groups = listOf("requiredForPage", "tabDrawerBannerButtons"),
    )

    val EMPTY_TAB_GROUP_PAGE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = TabsTrayTestTag.EMPTY_TAB_GROUPS_LIST,
        description = "Empty tab groups page",
        groups = listOf("emptyTabGroupsTabDrawerView"),
    )

    val THREE_DOT_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = TabsTrayTestTag.THREE_DOT_BUTTON,
        description = "Three dot menu button",
        groups = listOf("requiredForPage"),
    )

    val SELECT_TABS_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = TabsTrayTestTag.SELECT_TABS,
        description = "Three dot menu select tabs button",
        groups = listOf("tabDrawerThreeDotMainMenu"),
    )

    val SELECT_ALL_TABS_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = TabsTrayTestTag.SELECT_ALL_TABS,
        description = "Three dot menu select all tabs button",
        groups = listOf("tabDrawerThreeDotMainMenu"),
    )

    val TAB_SELECTION_THREE_DOT_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = TabsTrayTestTag.THREE_DOT_BUTTON,
        description = "Tab selection view three dot menu button",
        groups = listOf("tabSelectionView"),
    )

    val ADD_TO_GROUP_THREE_DOT_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.tab_manager_multiselect_menu_item_add_to_tab_group),
        description = "Tab selection view three dot menu add to group button",
        groups = listOf("tabSelectionThreeDotMainMenu"),
    )

    val CREATE_TAB_GROUP_NAME_TEXT_FIELD = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = GROUP_NAME,
        description = "Create tab group name text field",
        groups = listOf("createTabGroupView"),
    )

    @Suppress("ktlint:standard:function-naming")
    fun CREATE_TAB_GROUP_COLOR_BUTTON(color: String = "") = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = color,
        description = "Create tab group color: $color button",
        groups = listOf("createTabGroupView"),
    )

    val CREATE_TAB_GROUP_SAVE_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.create_tab_group_save_button),
        description = "Create tab group save button",
        groups = listOf("createTabGroupView"),
    )

    val ADD_TO_NEW_TAB_GROUP_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = TabsTrayTestTag.ADD_TO_NEW_TAB_GROUP,
        description = "Add to new tab group button",
        groups = listOf("addToTabGroupView"),
    )

    @Suppress("ktlint:standard:function-naming")
    fun ADD_TO_EXISTING_TAB_GROUP_BUTTON(title: String = "") = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = title,
        description = "Add to existing tab group button",
        groups = listOf("addToTabGroupView"),
    )

    @Suppress("ktlint:standard:function-naming")
    fun TAB_GROUP_ITEM(
        tabGroupTitle: String = "",
        numberOfTabs: Int = 1,
        tabGroupColor: String = "",
    ): Selector {
        val generatedDescription = getPluralStringResource(
            id = R.plurals.add_to_exiting_tab_group_content_description,
            quantity = numberOfTabs,
            tabGroupTitle,
            numberOfTabs,
            tabGroupColor,
        )

        return Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION_SUBSTRING,
            value = generatedDescription,
            description = "Tab group with description: $generatedDescription",
            groups = listOf("tabGroupItem"),
        )
    }

    val TAB_GROUP_MORE_OPTIONS_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = "More options",
        description = "Tab group more options button",
        groups = listOf("tabGroupItem"),
    )

    val TAB_GROUP_MORE_OPTIONS_DELETE_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = "Delete",
        description = "Tab group more options menu delete button",
        groups = listOf("tabGroupsMoreOptionsMenu"),
    )

    val TAB_GROUP_MORE_OPTIONS_CLOSE_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = CLOSE_TAB_GROUP,
        description = "Tab group more options menu close button",
        groups = listOf("tabGroupsMoreOptionsMenu"),
    )

    val DELETE_TAB_GROUP_DIALOG_TITLE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.delete_tab_group_confirmation_dialog_title),
        description = "Delete tab group dialog title",
        groups = listOf("deleteTabGroupDialog"),
    )

    val DELETE_TAB_GROUP_DIALOG_MESSAGE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.delete_tab_group_confirmation_dialog_body),
        description = "Delete tab group dialog message",
        groups = listOf("deleteTabGroupDialog"),
    )

    val DELETE_TAB_GROUP_DIALOG_CANCEL_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.close_tab_and_delete_group_confirmation_dialog_cancel),
        description = "Delete tab group dialog cancel button",
        groups = listOf("deleteTabGroupDialog"),
    )

    val DELETE_TAB_GROUP_DIALOG_DELETE_GROUP_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.close_tab_and_delete_group_confirmation_dialog_confirm),
        description = "Delete tab group dialog delete group button",
        groups = listOf("deleteTabGroupDialog"),
    )

    val TAB_GROUP_BOTTOM_SHEET_HANDLE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = getStringResource(R.string.tab_group_sheet_dismiss_description),
        description = "Tab group bottom sheet handle",
        groups = listOf("tabGroupBottomSheet"),
    )

    val FAB = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = TabsTrayTestTag.FAB,
        description = "Floating action button",
        groups = listOf("requiredForPage"),
    )

    val EMPTY_PRIVATE_TABS_LIST = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = TabsTrayTestTag.EMPTY_PRIVATE_TABS_LIST,
        description = "Empty private tabs list",
        groups = listOf("emptyPrivateTabsList"),
    )

    val PRIVATE_TABS_LIST = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = TabsTrayTestTag.PRIVATE_TABS_LIST,
        description = "Private tabs list",
        groups = listOf("privateTabsList"),
    )

    val TAB_ITEM_CLOSE = Selector(
        strategy = SelectorStrategy.COMPOSE_ON_ALL_NODES_BY_TAG_ON_FIRST,
        value = TabsTrayTestTag.TAB_ITEM_CLOSE,
        description = "Tab close button",
        groups = listOf("tabItem"),
    )

    val TAB_ITEM_THUMBNAIL = Selector(
        strategy = SelectorStrategy.COMPOSE_ON_ALL_NODES_BY_TAG_ON_FIRST,
        value = TabsTrayTestTag.TAB_ITEM_THUMBNAIL,
        description = "Tab thumbnail",
        groups = listOf("tabItem"),
    )

    @Suppress("ktlint:standard:function-naming")
    fun TAB_ITEM_WITH_TITLE(tabTitle: String = "") = Selector(
        strategy = SelectorStrategy.COMPOSE_ON_ALL_NODES_BY_TAG_WITH_CHILD_TEXT_ON_FIRST,
        value = TabsTrayTestTag.TAB_ITEM_ROOT,
        secondaryValue = tabTitle,
        description = "Tab with title: $tabTitle",
        groups = listOf("tabItem"),
    )

    val all = listOf(
        TABS_TRAY,
        NORMAL_BROWSING_EMPTY_TABS_PAGE,
        NORMAL_BROWSING_TABS_PAGE,
        NORMAL_BROWSING_OPEN_TABS_BUTTON,
        NORMAL_TABS_LIST,
        EMPTY_NORMAL_TABS_LIST,
        TAB_ITEM_ROOT,
        SYNCED_TABS_BUTTON,
        SIGN_IN_TO_SYNC_BUTTON,
        UNAUTHENTICATED_SYNCED_TABS_PAGE,
        UNAUTHENTICATED_SYNCED_TABS_PAGE_HEADER,
        UNAUTHENTICATED_SYNCED_TABS_PAGE_DESCRIPTION,
        PRIVATE_TABS_PAGE_BUTTON,
        TAB_GROUPS_BUTTON,
        EMPTY_TAB_GROUP_PAGE,
        THREE_DOT_BUTTON,
        SELECT_TABS_BUTTON,
        SELECT_ALL_TABS_BUTTON,
        TAB_SELECTION_THREE_DOT_BUTTON,
        ADD_TO_GROUP_THREE_DOT_BUTTON,
        CREATE_TAB_GROUP_NAME_TEXT_FIELD,
        CREATE_TAB_GROUP_COLOR_BUTTON(),
        CREATE_TAB_GROUP_SAVE_BUTTON,
        TAB_GROUP_ITEM(),
        TAB_GROUP_MORE_OPTIONS_BUTTON,
        TAB_GROUP_MORE_OPTIONS_DELETE_BUTTON,
        TAB_GROUP_MORE_OPTIONS_CLOSE_BUTTON,
        DELETE_TAB_GROUP_DIALOG_TITLE,
        DELETE_TAB_GROUP_DIALOG_MESSAGE,
        DELETE_TAB_GROUP_DIALOG_CANCEL_BUTTON,
        DELETE_TAB_GROUP_DIALOG_DELETE_GROUP_BUTTON,
        TAB_GROUP_BOTTOM_SHEET_HANDLE,
        FAB,
        EMPTY_PRIVATE_TABS_LIST,
        PRIVATE_TABS_LIST,
        TAB_ITEM_CLOSE,
        TAB_ITEM_THUMBNAIL,
        TAB_ITEM_WITH_TITLE(),
        ADD_TO_NEW_TAB_GROUP_BUTTON,
        ADD_TO_EXISTING_TAB_GROUP_BUTTON(),
    )
}
