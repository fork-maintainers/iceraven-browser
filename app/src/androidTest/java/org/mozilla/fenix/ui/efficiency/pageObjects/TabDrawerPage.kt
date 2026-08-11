/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SwipeDirection
import org.mozilla.fenix.ui.efficiency.navigation.NavigationRegistry
import org.mozilla.fenix.ui.efficiency.navigation.NavigationStep
import org.mozilla.fenix.ui.efficiency.selectors.TabDrawerSelectors
import org.mozilla.fenix.ui.efficiency.selectors.ToolbarSelectors

class TabDrawerPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "TabDrawerPage"

    init {
        NavigationRegistry.register(
            from = "HomePage",
            to = pageName,
            steps = listOf(
                NavigationStep.Click(ToolbarSelectors.TAB_COUNTER),
            ),
        )

        // The tab counter is reachable from the browser too, so this edge is generally useful. It has to be
        // the content-description variant: UIAutomator (not Compose, which can hang while GeckoView is
        // active) AND not the testTag, because with shouldUseExpandedToolbar the counter moves to the bottom
        // navigation bar and carries no tag there.
        NavigationRegistry.register(
            from = "BrowserPage",
            to = pageName,
            steps = listOf(
                NavigationStep.Click(ToolbarSelectors.TAB_COUNTER_ANY_LAYOUT),
            ),
        )

        NavigationRegistry.register(
            from = pageName,
            to = "HomePage",
            steps = listOf(NavigationStep.PressBack),
        )

        NavigationRegistry.register(
            from = pageName,
            to = "BrowserPage",
            steps = listOf(NavigationStep.PressBack),
        )
    }

    override fun mozGetSelectorsByGroup(group: String): List<Selector> {
        return TabDrawerSelectors.all.filter { it.groups.contains(group) }
    }

    override fun navigateToPage(url: String, forceNavigation: Boolean): TabDrawerPage {
        super.navigateToPage(url, forceNavigation)
        return this
    }

    fun closeTabWithTitle(title: String): TabDrawerPage {
        mozClickFirstWithParentText(TabDrawerSelectors.TAB_ITEM_CLOSE, title)
        return this
    }

    fun verifyNoOpenTabsInNormalBrowsing(): TabDrawerPage {
        mozVerifyElementsByGroup("emptyNormalBrowsingTabDrawerView")
        return this
    }

    fun verifyNormalTabsList(): TabDrawerPage {
        mozWaitUntilAbsent(TabDrawerSelectors.EMPTY_NORMAL_TABS_LIST)
        mozVerify(TabDrawerSelectors.NORMAL_TABS_LIST)
        return this
    }

    fun verifyExistingOpenTabs(vararg urls: String): TabDrawerPage {
        urls.forEach { url ->
            mozVerifyAnyHasChildWithText(TabDrawerSelectors.TAB_ITEM_ROOT, url)
        }
        return this
    }

    private fun selectTabsAndTapAddToGroup(tabTitle: String) {
        mozClick(TabDrawerSelectors.THREE_DOT_BUTTON)
        mozClick(TabDrawerSelectors.SELECT_TABS_BUTTON)
        mozClick(TabDrawerSelectors.TAB_ITEM_WITH_TITLE(tabTitle))
        mozClick(TabDrawerSelectors.TAB_SELECTION_THREE_DOT_BUTTON)
        mozClick(TabDrawerSelectors.ADD_TO_GROUP_THREE_DOT_BUTTON)
    }

    fun selectTabsAndCreateFirstTabGroup(
        tabTitle: String,
        tabGroupTitle: String = "",
        tabGroupColor: String = "",
    ): TabDrawerPage {
        selectTabsAndTapAddToGroup(tabTitle)

        if (tabGroupTitle.isNotEmpty()) {
            mozEnterText(tabGroupTitle, TabDrawerSelectors.CREATE_TAB_GROUP_NAME_TEXT_FIELD)
        }

        if (tabGroupColor.isNotEmpty()) {
            mozClick(TabDrawerSelectors.CREATE_TAB_GROUP_COLOR_BUTTON(tabGroupColor))
        }

        mozClick(TabDrawerSelectors.CREATE_TAB_GROUP_SAVE_BUTTON)

        return this
    }

    fun selectTabsAndAddToExistingTabGroup(tabTitle: String, tabGroupTitle: String, numberOfTabs: Int, tabGroupColor: String): TabDrawerPage {
        selectTabsAndTapAddToGroup(tabTitle)
        mozClick(TabDrawerSelectors.TAB_GROUP_ITEM(tabGroupTitle, numberOfTabs, tabGroupColor))
        return this
    }

    fun selectTabsAndAddToNewTabGroup(
        tabTitle: String,
        tabGroupTitle: String = "",
        tabGroupColor: String = "",
    ): TabDrawerPage {
        selectTabsAndTapAddToGroup(tabTitle)
        mozClick(TabDrawerSelectors.ADD_TO_NEW_TAB_GROUP_BUTTON)

        if (tabGroupTitle.isNotEmpty()) {
            mozEnterText(tabGroupTitle, TabDrawerSelectors.CREATE_TAB_GROUP_NAME_TEXT_FIELD)
        }

        if (tabGroupColor.isNotEmpty()) {
            mozClick(TabDrawerSelectors.CREATE_TAB_GROUP_COLOR_BUTTON(tabGroupColor))
        }

        mozClick(TabDrawerSelectors.CREATE_TAB_GROUP_SAVE_BUTTON)
        return this
    }

    fun selectAllTabsAndCreateTabGroup(
        tabGroupTitle: String = "",
        tabGroupColor: String = "",
    ): TabDrawerPage {
        mozClick(TabDrawerSelectors.THREE_DOT_BUTTON)
        mozClick(TabDrawerSelectors.SELECT_ALL_TABS_BUTTON)
        mozClick(TabDrawerSelectors.TAB_SELECTION_THREE_DOT_BUTTON)
        mozClick(TabDrawerSelectors.ADD_TO_GROUP_THREE_DOT_BUTTON)

        if (tabGroupTitle.isNotEmpty()) {
            mozEnterText(tabGroupTitle, TabDrawerSelectors.CREATE_TAB_GROUP_NAME_TEXT_FIELD)
        }

        if (tabGroupColor.isNotEmpty()) {
            mozClick(TabDrawerSelectors.CREATE_TAB_GROUP_COLOR_BUTTON(tabGroupColor))
        }

        mozClick(TabDrawerSelectors.CREATE_TAB_GROUP_SAVE_BUTTON)

        return this
    }

    fun deleteTabGroupFromTabGroupPage(): TabDrawerPage {
        mozClick(TabDrawerSelectors.TAB_GROUP_MORE_OPTIONS_BUTTON)
        mozClick(TabDrawerSelectors.TAB_GROUP_MORE_OPTIONS_DELETE_BUTTON)

        return this
    }

    fun closeTabGroup(): TabDrawerPage {
        mozClick(TabDrawerSelectors.TAB_GROUP_MORE_OPTIONS_BUTTON)
        mozClick(TabDrawerSelectors.TAB_GROUP_MORE_OPTIONS_CLOSE_BUTTON)

        return this
    }

    fun swipCloseTabGroupBottomSheet(): TabDrawerPage {
        mozSwipeElement(TabDrawerSelectors.TAB_GROUP_BOTTOM_SHEET_HANDLE, SwipeDirection.DOWN)
        mozWaitUntilAbsent(TabDrawerSelectors.TAB_GROUP_BOTTOM_SHEET_HANDLE)

        return this
    }

    fun openTabGroupFromTabGroupPage(tabGroupTitle: String, numberOfTabs: Int, tabGroupColor: String): TabDrawerPage {
        mozClick(TabDrawerSelectors.TAB_GROUPS_BUTTON)
        mozClick(TabDrawerSelectors.TAB_GROUP_ITEM(tabGroupTitle, numberOfTabs, tabGroupColor))

        return this
    }

    fun verifyTabGroupFromTabGroupPage(tabGroupTitle: String, numberOfTabs: Int, tabGroupColor: String): TabDrawerPage {
        mozClick(TabDrawerSelectors.TAB_GROUPS_BUTTON)
        mozVerify(TabDrawerSelectors.TAB_GROUP_ITEM(tabGroupTitle, numberOfTabs, tabGroupColor))

        return this
    }
}
