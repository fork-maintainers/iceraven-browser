/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import mozilla.components.support.ktx.util.PromptAbuserDetector
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.Constants
import org.mozilla.fenix.helpers.MockBrowserDataHelper
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.TestAssetHelper.firstForeignWebPageAsset
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.helpers.TestAssetHelper.pdfFormAsset
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.selectors.AddToHomeScreenSelectors
import org.mozilla.fenix.ui.efficiency.selectors.BookmarksSelectors
import org.mozilla.fenix.ui.efficiency.selectors.BookmarksSelectors.DELETE_BOOKMARK_BUTTON
import org.mozilla.fenix.ui.efficiency.selectors.BrowserPageSelectors
import org.mozilla.fenix.ui.efficiency.selectors.CollectionsSelectors
import org.mozilla.fenix.ui.efficiency.selectors.DownloadsSelectors
import org.mozilla.fenix.ui.efficiency.selectors.HistorySelectors.NAVIGATE_BACK_BUTTON
import org.mozilla.fenix.ui.efficiency.selectors.HomeSelectors
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors.BACK_BUTTON
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors.BOOKMARK_THIS_PAGE_BUTTON
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors.DESKTOP_SITE_BUTTON
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors.DESKTOP_SITE_OFF
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors.DESKTOP_SITE_ON
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors.EDIT_BOOKMARK_BUTTON
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors.FORWARD_BUTTON

class MainMenuTest : BaseTest(
    isPageLoadTranslationsPromptEnabled = false,
) {

    private val mockWebServer get() = fenixTestRule.mockWebServer

    @Before
    fun disablePromptAbuserDetector() {
        PromptAbuserDetector.validationsEnabled = false
    }

    @After
    fun restorePromptAbuserDetector() {
        PromptAbuserDetector.validationsEnabled = true
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3080168
    @SmokeTest
    @Test
    fun verifyMainMenuItemsTest() {
        on.mainMenu.navigateToPage()
            .mozVerifyElementsByGroup("homePageMainMenuItems")
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3080124
    @SmokeTest
    @Test
    fun verifyTheBrowserViewMainMenuItemsTest() {
        val website = mockWebServer.getGenericAsset(1)

        on.browserPage.navigateToPage(website.url.toString())
        on.mainMenu.navigateToPage()
            .mozVerifyElementsByGroup("browserViewMainMenuItems")
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3080172
    @SmokeTest
    @Test
    fun verifyTheExtensionsMenuOptionTest() {
        on.settingsAddonsManager.navigateToPage()
            .mozVerifyElementsByGroup("addOns")
        on.home.navigateToPage()
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3080181
    @SmokeTest
    @Ignore("Covered by verifyNavigationReachability[1: SettingsHomepagePage (TBD) — Navigation Reachability]")
    @Test
    fun verifyTheHomePageSettingsMenuItemTest() {
        on.settings.navigateToPage()
        on.home.navigateToPage()
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3080129
    @SmokeTest
    @Test
    fun verifyBookmarkPageMenuOptionTest() {
        val testPage = mockWebServer.getGenericAsset(1)
        on.browserPage.navigateToPage(testPage.url.toString())
            .mozClick(HomeSelectors.MAIN_MENU_BUTTON_UIAUTOMATOR)
            .mozClick(MainMenuSelectors.BOOKMARK_THIS_PAGE_BUTTON)
            .mozClick(BrowserPageSelectors.SNACKBAR_EDIT_BUTTON)
        on.bookmarks
            .mozVerifyElementsByGroup("bookmarkEdit")
            .mozClick(BookmarksSelectors.DELETE_BOOKMARK_BUTTON)
        on.browserPage
            .mozClick(HomeSelectors.MAIN_MENU_BUTTON_UIAUTOMATOR)
        on.mainMenu
            .mozVerifyElementsByGroup("bookmarkActions")
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3080138
    @SmokeTest
    @Test
    fun verifyTheDownloadsMenuItemTest() {
        val testPage = mockWebServer.getGenericAsset(1)

        on.browserPage.navigateToPage(testPage.url.toString())
        on.downloads.navigateToPage()
            .mozVerifyElementsByGroup("emptyDownloads")
        on.browserPage.navigateToPage()
            .verifyPageContent(testPage.content)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3080129
    @SmokeTest
    @Test
    fun verifyTheBookmarkPageMenuOptionTest() {
        val testPage = mockWebServer.getGenericAsset(1)

        on.browserPage.navigateToPage(testPage.url.toString())
        on.mainMenu.navigateToPage()
            .mozClick(BOOKMARK_THIS_PAGE_BUTTON)
        on.browserPage.navigateToPage()
        on.mainMenu.navigateToPage()
            .mozClick(EDIT_BOOKMARK_BUTTON)
        on.bookmarks
            .mozVerifyElementsByGroup("editBookmarksView")
            .mozClick(DELETE_BOOKMARK_BUTTON)
        on.browserPage.navigateToPage()
        on.mainMenu.navigateToPage()
            .mozVerify(BOOKMARK_THIS_PAGE_BUTTON)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3080136
    @SmokeTest
    @Test
    fun verifyTheHistoryMenuItemTest() {
        val testPage = mockWebServer.getGenericAsset(1)

        on.browserPage.navigateToPage(testPage.url.toString())
        on.history.navigateToPage()
            .mozVerifyElementsByGroup("historyMenuViewWithHistoryItems")
            .mozClick(NAVIGATE_BACK_BUTTON)
        on.browserPage
            .verifyPageContent(testPage.content)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3080133
    @SmokeTest
    @Test
    fun verifySwitchToDesktopSiteIsDisabledOnPDFsTest() {
        val pdfPage = mockWebServer.pdfFormAsset

        on.browserPage.navigateToPage(pdfPage.url.toString())
        on.mainMenu.navigateToPage()
            .mozVerifyElementIsNotEnabled(MainMenuSelectors.DESKTOP_SITE_BUTTON)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3080139
    @SmokeTest
    @Test
    fun verifyThePasswordsMenuItemTest() {
        val testPage = mockWebServer.getGenericAsset(1)

        on.browserPage.navigateToPage(testPage.url.toString())
        on.settingsSavedPasswords.navigateToPage()
            .mozVerifyElementsByGroup("emptySavedPasswordsList")
        on.browserPage.navigateToPage()
            .verifyPageContent(testPage.content)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3080126
    @SmokeTest
    @Test
    fun verifyTheMainMenuForwardButtonTest() {
        val firstWebPage = mockWebServer.getGenericAsset(1)
        val nextWebPage = mockWebServer.getGenericAsset(2)

        on.browserPage
            .navigateToPage(firstWebPage.url.toString())
            .verifyUrl(firstWebPage.url.toString())
            .navigateToPage(nextWebPage.url.toString(), forceNavigation = true)
            .verifyUrl(nextWebPage.url.toString())
        on.mainMenu.navigateToPage()
            .mozClick(BACK_BUTTON)
        on.browserPage.navigateToPage()
            .verifyUrl(firstWebPage.url.toString())
        on.mainMenu.navigateToPage()
            .mozClick(FORWARD_BUTTON)
        on.browserPage.navigateToPage()
            .verifyUrl(nextWebPage.url.toString())
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3080125
    @SmokeTest
    @Test
    fun verifyTheMainMenuBackButtonTest() {
        val firstWebPage = mockWebServer.getGenericAsset(1)
        val nextWebPage = mockWebServer.getGenericAsset(2)

        on.browserPage
            .navigateToPage(firstWebPage.url.toString())
            .verifyUrl(firstWebPage.url.toString())
            .navigateToPage(nextWebPage.url.toString(), forceNavigation = true)
            .verifyUrl(nextWebPage.url.toString())
        on.mainMenu.navigateToPage()
            .mozClick(BACK_BUTTON)
        on.browserPage.navigateToPage()
            .verifyUrl(firstWebPage.url.toString())
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3080131
    @SmokeTest
    @Test
    fun verifyDesktopSiteModeOnOffIsEnabledTest() {
        val defaultWebPage = mockWebServer.getGenericAsset(1)

        on.browserPage
            .navigateToPage(defaultWebPage.url.toString())
        on.mainMenu
            .navigateToPage()
            .mozVerify(DESKTOP_SITE_BUTTON)
            .mozVerify(DESKTOP_SITE_OFF)
        on.mainMenu
            .mozClick(DESKTOP_SITE_BUTTON)
        on.browserPage
            .navigateToPage()
        on.mainMenu
            .navigateToPage()
            .mozVerify(DESKTOP_SITE_ON)
        on.mainMenu
            .mozClick(DESKTOP_SITE_BUTTON)
        on.browserPage
            .navigateToPage()
        on.mainMenu
            .navigateToPage()
            .mozVerify(DESKTOP_SITE_OFF)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3080115
    @SmokeTest
    @Test
    fun verifyTheSaveToCollectionSubMenuOptionTest() {
        val collectionTitle = "First Collection"
        val firstTestPage = mockWebServer.getGenericAsset(1)
        val secondTestPage = mockWebServer.getGenericAsset(2)

        composeRule.activityRule.applySettingsExceptions {
            // Disabling these features to have better visibility of the Collections view
            it.isRecentlyVisitedFeatureEnabled = false
            it.isRecentTabsFeatureEnabled = false
        }

        MockBrowserDataHelper
            .createCollection(
                Pair(firstTestPage.url.toString(), firstTestPage.title),
                title = collectionTitle,
            )

        on.browserPage.navigateToPage(secondTestPage.url.toString())
            .mozClick(BrowserPageSelectors.MAIN_MENU_BUTTON)
            .mozClick(MainMenuSelectors.MORE_BUTTON)
            .mozClick(MainMenuSelectors.SAVE_TO_COLLECTIONS_BUTTON)
            .mozClick(CollectionsSelectors.EXISTING_COLLECTION_WITH_TITLE(collectionTitle))
        on.home.navigateToPage()
            .mozClick(CollectionsSelectors.COLLECTION_WITH_TITLE(collectionTitle))
            .mozVerify(CollectionsSelectors.COLLECTION_TAB_WITH_TITLE(firstTestPage.title))
            .mozVerify(CollectionsSelectors.COLLECTION_TAB_WITH_TITLE(secondTestPage.title))
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3080111
    @SmokeTest
    @Ignore("Failing: https://bugzilla.mozilla.org/show_bug.cgi?id=2056644")
    @Test
    fun verifyTheTranslatePageSubMenuOptionTest() {
        val testPage = mockWebServer.firstForeignWebPageAsset

        on.browserPage.navigateToPage(testPage.url.toString())
            .openMainMenu()
            .mozClick(MainMenuSelectors.MORE_BUTTON)
            .mozClick(MainMenuSelectors.TRANSLATE_BUTTON)
            .mozClickIfPresent(BrowserPageSelectors.TRANSLATION_SHEET_TRANSLATE_BUTTON)
            .mozWaitUntilAbsent(BrowserPageSelectors.TRANSLATION_SHEET_TRANSLATE_BUTTON, TestAssetHelper.waitingTimeLong)
        on.browserPage
            .openMainMenu()
            .mozClick(MainMenuSelectors.MORE_BUTTON)
            .mozClick(MainMenuSelectors.TRANSLATED_BUTTON)
            .mozClickIfPresent(BrowserPageSelectors.TRANSLATION_SHEET_SHOW_ORIGINAL_BUTTON)
            .mozWaitUntilAbsent(BrowserPageSelectors.TRANSLATION_SHEET_SHOW_ORIGINAL_BUTTON)
        on.browserPage
            .verifyPageContent(testPage.content)
            .openMainMenu()
            .mozClick(MainMenuSelectors.MORE_BUTTON)
            .mozVerify(MainMenuSelectors.TRANSLATE_BUTTON)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3080113
    @SmokeTest
    @Test
    fun verifyTheAddToShortcutsSubMenuOptionTest() {
        val testPage = mockWebServer.getGenericAsset(1)

        on.browserPage.navigateToPage(testPage.url.toString())
        on.mainMenu.navigateToPage()
            .mozClick(MainMenuSelectors.MORE_BUTTON)
            .mozClick(MainMenuSelectors.ADD_TO_SHORTCUTS_BUTTON)
        on.browserPage.navigateToPage()
            .mozVerifyElementsByGroup("addedToShortcutsSnackbar")
        on.home.navigateToPage()
            .mozVerify(HomeSelectors.TOP_SITE_ITEM(testPage.title))
            .mozClick(HomeSelectors.TOP_SITE_ITEM(testPage.title))
        on.browserPage.navigateToPage()
        on.mainMenu.navigateToPage()
            .mozClick(MainMenuSelectors.MORE_BUTTON)
            .mozClick(MainMenuSelectors.REMOVE_FROM_SHORTCUTS_BUTTON)
        on.browserPage.navigateToPage()
        on.home.navigateToPage()
            .mozWaitUntilAbsent(HomeSelectors.TOP_SITE_ITEM(testPage.title))
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3080114
    @SmokeTest
    @Test
    fun verifyTheAddToHomeScreenSubMenuOptionTest() {
        val testPage = mockWebServer.getGenericAsset(1)

        on.browserPage.navigateToPage(testPage.url.toString())
        on.addToHomescreen.navigateToPage()
            .mozClickIfPresent(AddToHomeScreenSelectors.CANCEL_DIALOG_BUTTON)
        on.browserPage.navigateToPage()
        on.addToHomescreen.navigateToPage()
            .mozClickIfPresent(AddToHomeScreenSelectors.ADD_DIALOG_BUTTON)
            .mozClick(AddToHomeScreenSelectors.SYSTEM_PROMPT_ADD_TO_HOME_SCREEN_BUTTON)
            .mozClickIfPresent(AddToHomeScreenSelectors.HOME_SCREEN_SHORTCUT(testPage.title))
        on.browserPage.navigateToPage()
            .verifyPageContent(testPage.content)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3080118
    @SmokeTest
    @Test
    fun verifyTheSaveAsPDFSubMenuOptionTest() {
        val testPage = mockWebServer.getGenericAsset(1)

        on.browserPage.navigateToPage(testPage.url.toString())
        on.mainMenu.navigateToPage()
            .mozClick(MainMenuSelectors.MORE_BUTTON)
            .mozClick(MainMenuSelectors.SAVE_AS_PDF_BUTTON)
        on.downloads
            .mozVerifyElementsByGroup("downloadDialog")
            .mozClick(DownloadsSelectors.DOWNLOAD_DIALOG_CONFIRM_BUTTON)
            .mozVerify(DownloadsSelectors.DOWNLOAD_COMPLETE_SNACKBAR, timeout = 15_000)
            .mozClick(DownloadsSelectors.DOWNLOAD_SNACK_BAR_OPEN_BUTTON)
            .mozVerifyFileOpensInExternalApp(Constants.PackageName.GOOGLE_DOCS)
    }
}
