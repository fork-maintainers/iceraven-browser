package org.mozilla.fenix.ui.efficiency.tests

import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.MockBrowserDataHelper
import org.mozilla.fenix.helpers.TestAssetHelper.genericAssets
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.helpers.TestHelper.closeApp
import org.mozilla.fenix.helpers.TestHelper.restartApp
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.selectors.HomeSelectors
import org.mozilla.fenix.ui.efficiency.selectors.SearchBarSelectors
import org.mozilla.fenix.ui.efficiency.selectors.TabDrawerSelectors
import org.mozilla.fenix.ui.efficiency.selectors.TabDrawerSelectors.DELETE_TAB_GROUP_DIALOG_CANCEL_BUTTON
import org.mozilla.fenix.ui.efficiency.selectors.TabDrawerSelectors.DELETE_TAB_GROUP_DIALOG_DELETE_GROUP_BUTTON
import org.mozilla.fenix.ui.efficiency.selectors.TabDrawerSelectors.TAB_GROUPS_BUTTON
import org.mozilla.fenix.ui.efficiency.selectors.TabDrawerSelectors.TAB_ITEM_WITH_TITLE

class TabbedBrowsingTest : BaseTest() {

    private val mockWebServer get() = fenixTestRule.mockWebServer

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/1046683
    @Test
    fun verifySyncedTabsWhenUserIsNotSignedInTest() {
        on.tabDrawer.navigateToPage()
            .mozVerifyElementIsNotSelected(TabDrawerSelectors.SYNCED_TABS_BUTTON)
        on.tabDrawer
            .mozClick(TabDrawerSelectors.SYNCED_TABS_BUTTON)
            .mozVerifyElementIsSelected(TabDrawerSelectors.SYNCED_TABS_BUTTON)
        on.tabDrawer
            .mozVerifyElementsByGroup("tabDrawerUnauthenticatedSyncedTabs")
            .mozClick(TabDrawerSelectors.SIGN_IN_TO_SYNC_BUTTON)
        on.settingsTurnOnSync
            .mozVerifyElementsByGroup()
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/903587
    @SmokeTest
    @Test
    fun verifyPrivateTabsTrayWithOpenTabTest() {
        val website = mockWebServer.getGenericAsset(1)

        on.tabDrawer.navigateToPage()
            .mozClick(TabDrawerSelectors.PRIVATE_TABS_PAGE_BUTTON)
            .mozClick(TabDrawerSelectors.FAB)
        on.searchBar
            .mozEnterText(website.url.toString(), SearchBarSelectors.TOOLBAR_IN_EDIT_MODE)
            .mozPressEnter(SearchBarSelectors.TOOLBAR_IN_EDIT_MODE)
        on.browserPage.navigateToPage()
        on.tabDrawer.navigateToPage()
            .mozVerifyElementIsNotSelected(TabDrawerSelectors.NORMAL_BROWSING_OPEN_TABS_BUTTON)
        on.tabDrawer.mozVerifyElementIsSelected(TabDrawerSelectors.PRIVATE_TABS_PAGE_BUTTON)
        on.tabDrawer.mozVerifyElementIsNotSelected(TabDrawerSelectors.TAB_GROUPS_BUTTON)
        on.tabDrawer.mozVerifyElementIsNotSelected(TabDrawerSelectors.SYNCED_TABS_BUTTON)
        on.tabDrawer.mozVerify(TabDrawerSelectors.THREE_DOT_BUTTON)
        on.tabDrawer.mozVerify(TabDrawerSelectors.PRIVATE_TABS_LIST)
        on.tabDrawer.mozVerify(TabDrawerSelectors.TAB_ITEM_WITH_TITLE(website.title))
        on.tabDrawer.mozVerify(TabDrawerSelectors.TAB_ITEM_CLOSE)
        on.tabDrawer.mozVerify(TabDrawerSelectors.TAB_ITEM_THUMBNAIL)
        on.tabDrawer.mozVerify(TabDrawerSelectors.FAB)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4034499
    @SmokeTest
    @Test
    fun verifyTheTabsGroupButtonTabsTrayPositionTest() {
        on.tabDrawer
            .navigateToPage()
            .mozVerifyElementsByGroup("tabDrawerBannerButtons")
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4034504
    @SmokeTest
    @Test
    fun verifyTheDeletionOfATabGroupTest() {
        val webPages = mockWebServer.genericAssets

        MockBrowserDataHelper.createTabItem(webPages[0].url.toString())
        MockBrowserDataHelper.createTabItem(webPages[1].url.toString())

        on.tabDrawer
            .navigateToPage()
        on.tabDrawer
            .selectAllTabsAndCreateTabGroup()
        on.tabDrawer
            .mozClick(TAB_GROUPS_BUTTON)
        on.tabDrawer
            .deleteTabGroupFromTabGroupPage()
            .mozVerifyElementsByGroup("deleteTabGroupDialog")
            .mozClick(DELETE_TAB_GROUP_DIALOG_CANCEL_BUTTON)
        on.tabDrawer
            .deleteTabGroupFromTabGroupPage()
            .mozVerifyElementsByGroup("deleteTabGroupDialog")
            .mozClick(DELETE_TAB_GROUP_DIALOG_DELETE_GROUP_BUTTON)
            .mozVerifyElementsByGroup("emptyTabGroupsView")
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4034505
    @SmokeTest
    @Test
    fun verifyTheClosingOfATabGroupTest() {
        val tabGroupTitle = "Group 1"
        val tabGroupColor = "Grey"

        val webPages = mockWebServer.genericAssets
        MockBrowserDataHelper.createTabItem(webPages[0].url.toString())
        MockBrowserDataHelper.createTabItem(webPages[1].url.toString())

        on.tabDrawer
            .navigateToPage()
        on.tabDrawer
            .selectAllTabsAndCreateTabGroup(tabGroupColor = tabGroupColor)
            .mozVerify(TAB_ITEM_WITH_TITLE(tabTitle = tabGroupTitle))
        on.tabDrawer
            .closeTabGroup()
            .mozVerifyElementsByGroup("emptyNormalBrowsingTabDrawerView")
        on.tabDrawer
            .openTabGroupFromTabGroupPage(tabGroupTitle = tabGroupTitle, numberOfTabs = 2, tabGroupColor = tabGroupColor)
            .swipCloseTabGroupBottomSheet()
            .mozVerifyElementsByGroup("normalBrowsingTabDrawerView")
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4034506
    @SmokeTest
    @Test
    fun verifyTheCreationOfATabGroupBySelectingTabsTest() {
        val tabGroupTitle = "Mozilla"
        val tabGroupColor = "Grey"

        val webPages = mockWebServer.genericAssets
        MockBrowserDataHelper.createTabItem(webPages[0].url.toString())
        MockBrowserDataHelper.createTabItem(webPages[1].url.toString())
        MockBrowserDataHelper.createTabItem(webPages[2].url.toString())

        on.tabDrawer
            .navigateToPage()
        on.tabDrawer
            .selectTabsAndCreateFirstTabGroup(
                tabTitle = webPages[0].title,
                tabGroupTitle = tabGroupTitle,
                tabGroupColor = tabGroupColor,
            )

        closeApp(composeRule.activityRule)
        restartApp(composeRule.activityRule)

        verifyGroup(
            tabGroupTitle = tabGroupTitle,
            tabGroupColor = tabGroupColor,
        )
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4034502
    @Test
    fun verifyCreationOfNewGroupWhenGroupExistsTest() {
        val firstTabGroupTitle = "Mozilla"
        val firstTabGroupColor = "Grey"
        val secondTabGroupTitle = "Test"
        val secondTabGroupColor = "Blue"

        val webPages = mockWebServer.genericAssets
        MockBrowserDataHelper.createTabItem(webPages[0].url.toString())
        MockBrowserDataHelper.createTabItem(webPages[1].url.toString())
        MockBrowserDataHelper.createTabItem(webPages[2].url.toString())

        on.tabDrawer
            .navigateToPage()
        on.tabDrawer
            .selectTabsAndCreateFirstTabGroup(
                tabTitle = webPages[0].title,
                tabGroupTitle = firstTabGroupTitle,
                tabGroupColor = firstTabGroupColor,
            )
        on.tabDrawer
            .selectTabsAndAddToNewTabGroup(
                tabTitle = webPages[1].title,
                tabGroupTitle = secondTabGroupTitle,
                tabGroupColor = secondTabGroupColor,
            )

        closeApp(composeRule.activityRule)
        restartApp(composeRule.activityRule)

        verifyGroup(
            tabGroupTitle = firstTabGroupTitle,
            tabGroupColor = firstTabGroupColor,
            numberOfTabs = 1,
        )
        verifyGroup(
            tabGroupTitle = secondTabGroupTitle,
            tabGroupColor = secondTabGroupColor,
            numberOfTabs = 1,
        )
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4162826
    @Test
    fun verifyAddingTabToExistingGroupWhenGroupExistsTest() {
        val tabGroupTitle = "Mozilla"
        val tabGroupColor = "Grey"

        val webPages = mockWebServer.genericAssets
        MockBrowserDataHelper.createTabItem(webPages[0].url.toString())
        MockBrowserDataHelper.createTabItem(webPages[1].url.toString())
        MockBrowserDataHelper.createTabItem(webPages[2].url.toString())

        on.tabDrawer
            .navigateToPage()
        on.tabDrawer
            .selectTabsAndCreateFirstTabGroup(
                tabTitle = webPages[0].title,
                tabGroupTitle = tabGroupTitle,
                tabGroupColor = tabGroupColor,
            )
        on.tabDrawer
            .selectTabsAndAddToExistingTabGroup(
                tabTitle = webPages[1].title,
                tabGroupTitle = tabGroupTitle,
                numberOfTabs = 1,
                tabGroupColor = tabGroupColor,
            )

        closeApp(composeRule.activityRule)
        restartApp(composeRule.activityRule)

        verifyGroup(
            tabGroupTitle = tabGroupTitle,
            tabGroupColor = tabGroupColor,
            numberOfTabs = 2,
        )
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2228470
    @SmokeTest
    @Test
    fun privateTabsDoNotPersistAfterClosingAppTest() {
        val firstWebPage = mockWebServer.getGenericAsset(1)
        val secondWebPage = mockWebServer.getGenericAsset(2)

        on.home
            .mozClick(HomeSelectors.PRIVATE_BROWSING_BUTTON)
        on.browserPage.navigateToPage(firstWebPage.url.toString())
        on.tabDrawer.navigateToPage()
            .mozClick(TabDrawerSelectors.FAB)
        on.searchBar
            .mozEnterText(secondWebPage.url.toString(), SearchBarSelectors.TOOLBAR_IN_EDIT_MODE)
            .mozPressEnter(SearchBarSelectors.TOOLBAR_IN_EDIT_MODE)
        on.browserPage.navigateToPage()

        closeApp(composeRule.activityRule)
        restartApp(composeRule.activityRule)

        on.home
            .mozVerify(HomeSelectors.PRIVATE_BROWSING_INFO_CARD_TITLE)
        on.tabDrawer.navigateToPage()
            .mozVerify(TabDrawerSelectors.EMPTY_PRIVATE_TABS_LIST)
    }

    private fun verifyGroup(tabGroupTitle: String, tabGroupColor: String, numberOfTabs: Int = 1) {
        on.home
            .navigateToPage()
        on.tabDrawer
            .navigateToPage()
            .mozVerify(TAB_ITEM_WITH_TITLE(tabTitle = tabGroupTitle))
        on.tabDrawer
            .verifyTabGroupFromTabGroupPage(tabGroupTitle = tabGroupTitle, numberOfTabs = numberOfTabs, tabGroupColor = tabGroupColor)
    }
}
