/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import android.text.format.DateUtils
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.helpers.TestHelper.appContext
import org.mozilla.fenix.ui.efficiency.helpers.PageReadinessProfiles
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorContainer
import org.mozilla.fenix.ui.efficiency.helpers.SelectorGroup
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

object HistorySelectors : SelectorContainer {
    enum class Group : SelectorGroup {
        HISTORY_MENU_VIEW_WITH_HISTORY_ITEMS,
        EMPTY_HISTORY_MENU_VIEW,
        DELETE_CONFIRMATION,
    }

    val TOOLBAR_TITLE =
        navigationToolbarTitle(
            title = getStringResource(R.string.library_history),
            description = "History toolbar title",
            groups = setOf(Group.HISTORY_MENU_VIEW_WITH_HISTORY_ITEMS),
        )

    val NAVIGATE_BACK_BUTTON =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
            value = getStringResource(R.string.action_bar_up_description),
            description = "Navigate back toolbar button",
            groups = setOf(Group.HISTORY_MENU_VIEW_WITH_HISTORY_ITEMS),
            readiness = PageReadinessProfiles.READY_CONTENT,
        )

    val SEARCH_HISTORY_BUTTON =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
            value = "history_search",
            description = "Search history toolbar button",
            groups = setOf(Group.HISTORY_MENU_VIEW_WITH_HISTORY_ITEMS),
            readiness = PageReadinessProfiles.READY_CONTENT,
        )

    val RECENTLY_CLOSED_TABS_BUTTON =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
            value = "recently_closed_tabs_header",
            description = "Recently closed tabs button",
            groups = setOf(Group.HISTORY_MENU_VIEW_WITH_HISTORY_ITEMS),
            readiness = PageReadinessProfiles.READY_CONTENT,
        )

    // Mirrors HistoryListItemViewHolder: the description reads "1 tab" (singular) but "0 tabs"/"N tabs"
    // (plural), so the resource is chosen by count. Not a val, so it is never part of a group check;
    // pass the concrete count and verify it explicitly with mozVerify.
    @Suppress("FunctionName")
    fun RECENTLY_CLOSED_TABS_NUMBER_OF_TABS(numberOfRecentlyClosedTabs: Int = 0) =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID_AND_TEXT,
            value = "recently_closed_tabs_description",
            secondaryValue =
                getStringResource(
                    if (numberOfRecentlyClosedTabs == 1) {
                        R.string.recently_closed_tab
                    } else {
                        R.string.recently_closed_tabs
                    },
                    numberOfRecentlyClosedTabs,
                ),
            description = "Number of recently closed tabs",
        )

    val EMPTY_HISTORY_LIST =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
            value = "history_empty_view",
            description = "Empty history view",
            groups = setOf(Group.EMPTY_HISTORY_MENU_VIEW),
        )

    val HISTORY_LIST =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
            value = "history_list",
            description = "Browsing history list view",
            groups = setOf(Group.HISTORY_MENU_VIEW_WITH_HISTORY_ITEMS),
        )

    val VISITED_TIME_TITLE =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
            value =
                DateUtils.formatDateTime(
                    appContext,
                    System.currentTimeMillis(),
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR,
                ),
            description = "History date chronological timeline title",
            groups = setOf(Group.HISTORY_MENU_VIEW_WITH_HISTORY_ITEMS),
        )

    val HISTORY_ITEM_TITLE =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
            value = "title",
            description = "History item title",
            groups = setOf(Group.HISTORY_MENU_VIEW_WITH_HISTORY_ITEMS),
        )

    val HISTORY_ITEM_URL =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
            value = "url",
            description = "History item URL",
            groups = setOf(Group.HISTORY_MENU_VIEW_WITH_HISTORY_ITEMS),
        )

    val HISTORY_ITEM_DELETE_BUTTON =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
            value = "Delete",
            description = "History item delete button",
            groups = setOf(Group.HISTORY_MENU_VIEW_WITH_HISTORY_ITEMS),
        )

    val DELETE_ALL_HISTORY_BUTTON =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
            value = "history_delete",
            description = "Delete all history button",
            groups = setOf(Group.HISTORY_MENU_VIEW_WITH_HISTORY_ITEMS),
            readiness = PageReadinessProfiles.READY_CONTENT,
        )

    val DELETE_CONFIRMATION_DIALOG_TITLE =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
            value = "title",
            description = "Delete confirmation dialog title",
            groups = setOf(Group.DELETE_CONFIRMATION),
        )

    val DELETE_CONFIRMATION_DIALOG_MESSAGE =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
            value = "body",
            description = "Delete confirmation dialog message",
            groups = setOf(Group.DELETE_CONFIRMATION),
        )

    val DELETE_EVERYTHING_OPTION_BUTTON =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
            value = "everything_button",
            description = "Everything option button in delete dialog",
            groups = setOf(Group.DELETE_CONFIRMATION),
        )

    val DELETE_CONFIRM_BUTTON =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
            value = "Delete",
            description = "Confirm delete button in dialog",
            groups = setOf(Group.DELETE_CONFIRMATION),
        )

    // Any row on the History screen whose text contains [text]. Mirrors the legacy generic
    // verifyHistoryItemExists, which is how a search group is identified there: by its size caption
    // ("3 pages"), since the group's own title is the search term and that also appears inside the visited
    // search-results URL.
    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun HISTORY_ITEM_WITH_TEXT(text: String = "") =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
            value = text,
            description = "History item containing '$text'",
        )
}
