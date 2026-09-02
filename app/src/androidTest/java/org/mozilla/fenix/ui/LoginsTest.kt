/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import android.os.Build
import android.view.autofill.AutofillManager
import androidx.core.net.toUri
import androidx.test.filters.SdkSuppress
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.customannotations.Converted
import org.mozilla.fenix.customannotations.SkipLeaks
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.FenixTestRule
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.MatcherHelper.itemWithResId
import org.mozilla.fenix.helpers.MatcherHelper.itemWithText
import org.mozilla.fenix.helpers.TestAssetHelper.saveLoginAsset
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeLong
import org.mozilla.fenix.helpers.TestHelper
import org.mozilla.fenix.helpers.TestHelper.exitMenu
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.helpers.TestHelper.restartApp
import org.mozilla.fenix.helpers.TestHelper.scrollToElementByText
import org.mozilla.fenix.helpers.TestHelper.verifySnackBarText
import org.mozilla.fenix.helpers.TestHelper.waitForAppWindowToBeUpdated
import org.mozilla.fenix.helpers.TestHelper.waitUntilSnackbarGone
import org.mozilla.fenix.helpers.perf.DetectMemoryLeaksRule
import org.mozilla.fenix.ui.robots.browserScreen
import org.mozilla.fenix.ui.robots.clickPageObject
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar
import org.mozilla.fenix.ui.robots.setPageObjectText
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2

/**
 * Tests for verifying:
 * - the Logins and Passwords menu and sub-menus.
 * - save login prompts.
 * - saving logins based on the user's preferences.
 */
class LoginsTest {
    @get:Rule(order = 0)
    val fenixTestRule: FenixTestRule = FenixTestRule()

    private val mockWebServer get() = fenixTestRule.mockWebServer

    @get:Rule(order = 1)
    val composeTestRule =
        AndroidComposeTestRuleV2(
            HomeActivityIntentTestRule.withDefaultSettingsOverrides(),
        ) { it.activity }

    @get:Rule(order = 2)
    val memoryLeaksRule = DetectMemoryLeaksRule(composeTestRule = { composeTestRule })

    @Before
    fun setUp() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            val autofillManager: AutofillManager =
                TestHelper.appContext.getSystemService(AutofillManager::class.java)
            autofillManager.disableAutofillServices()
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2092713
    // Tests the Passwords menu items and default values
    @Test
    fun loginsAndPasswordsSettingsItemsTest() {
        homeScreen(composeTestRule) {
        }.openThreeDotMenu {
        }.clickSettingsButton {
            // Necessary to scroll a little bit for all screen sizes
            scrollToElementByText("Passwords")
        }.openLoginsAndPasswordSubMenu {
            verifyDefaultView()
            verifyAutofillInFirefoxToggle(true)
            verifyAutofillLoginsInOtherAppsToggle(false)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/517816
    // Tests only for initial state without signing in.
    // For tests after signing in, see SyncIntegration test suite
    @Test
    fun verifySavedLoginsListTest() {
        homeScreen(composeTestRule) {
        }.openThreeDotMenu {
        }.clickSettingsButton {
            // Necessary to scroll a little bit for all screen sizes
            scrollToElementByText("Passwords")
        }.openLoginsAndPasswordSubMenu {
            verifyDefaultView()
        }.openSavedLogins(composeTestRule) {
            verifySecurityPromptForLogins()
            tapSetupLater()
            // Verify that logins list is empty
            verifyEmptySavedLoginsListView()
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2092925
    @Test
    fun verifySyncLoginsOptionsTest() {
        homeScreen(composeTestRule) {
        }.openThreeDotMenu {
        }.clickSettingsButton {
            // Necessary to scroll a little bit for all screen sizes
            scrollToElementByText("Passwords")
        }.openLoginsAndPasswordSubMenu {
        }.openSyncLogins(composeTestRule) {
            verifyReadyToScanOption()
            verifyUseEmailOption()
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/523839
    @Test
    fun saveLoginFromPromptTest() {
        val saveLoginTest = mockWebServer.saveLoginAsset

        homeScreen(composeTestRule) {
        }.openThreeDotMenu {
        }.clickSettingsButton {
        }.openLoginsAndPasswordSubMenu {
        }.openSaveLoginsAndPasswordsOptions {
            verifySaveLoginsOptionsView()
        }

        exitMenu()

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(saveLoginTest.url) {
            clickSubmitLoginButton()
            verifySaveLoginPromptIsDisplayed()
            // Click save to save the login
            clickPageObject(composeTestRule, itemWithText("Save"))
        }
        browserScreen(composeTestRule) {
        }.openThreeDotMenu {
        }.clickSettingsButton {
            scrollToElementByText("Passwords")
        }.openLoginsAndPasswordSubMenu {
            verifyDefaultView()
        }.openSavedLogins(composeTestRule) {
            verifySecurityPromptForLogins()
            tapSetupLater()
            // Verify that the login appears correctly
            verifySavedLoginsSectionUsername("test@example.com")
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/960412
    @Test
    fun openLoginWebsiteInBrowserTest() {
        val loginPage = "https://mozilla-mobile.github.io/testapp/loginForm"
        val originWebsite = "mozilla-mobile.github.io"
        val userName = "test"
        val password = "pass"

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(loginPage.toUri()) {
            setPageObjectText(composeTestRule, itemWithResId("username"), userName)
            waitForAppWindowToBeUpdated()
            setPageObjectText(composeTestRule, itemWithResId("password"), password)
            waitForAppWindowToBeUpdated()
            clickPageObject(composeTestRule, itemWithResId("submit"))
            verifySaveLoginPromptIsDisplayed()
            clickPageObject(composeTestRule, itemWithText("Save"))
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.clickSettingsButton {
        }.openLoginsAndPasswordSubMenu {
        }.openSavedLogins(composeTestRule) {
            verifySecurityPromptForLogins()
            tapSetupLater()
            viewSavedLoginDetails(userName)
        }.goToSavedWebsite {
            verifyUrl(originWebsite)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/517817
    @Test
    fun neverSaveLoginFromPromptTest() {
        val saveLoginTest = mockWebServer.saveLoginAsset

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(saveLoginTest.url) {
            clickSubmitLoginButton()
            // Don't save the login, add to exceptions
            clickPageObject(composeTestRule, itemWithText("Never save"))
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.clickSettingsButton {
        }.openLoginsAndPasswordSubMenu {
            verifyDefaultView()
        }.openSavedLogins(composeTestRule) {
            verifySecurityPromptForLogins()
            tapSetupLater()
            // Verify that the login list is empty
            verifyEmptySavedLoginsListView()
            verifyNotSavedLoginFromPrompt()
        }.goBack {
        }.openLoginExceptions(composeTestRule) {
            // Verify localhost was added to exceptions list
            verifyLocalhostExceptionAdded()
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/1508171
    @Converted(
        replacedBy = ["org.mozilla.fenix.ui.efficiency.tests.LoginsTest#verifyUpdatedLoginIsSavedTest"],
        bug = 2060474,
        since = "2026-08",
    )
    @SmokeTest
    @Test
    fun verifyUpdatedLoginIsSavedTest() {
        val saveLoginTest = mockWebServer.saveLoginAsset

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(saveLoginTest.url) {
            clickSubmitLoginButton()
            verifySaveLoginPromptIsDisplayed()
            // Click Save to save the login
            clickPageObject(composeTestRule, itemWithText("Save"))
        }
        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(saveLoginTest.url) {
            enterPassword("test")
            mDevice.waitForIdle()
            clickSubmitLoginButton()
            verifySaveLoginPromptIsDisplayed()
            // Click Update to change the saved password
            clickPageObject(composeTestRule, itemWithText("Update"))
        }.openThreeDotMenu {
        }.clickSettingsButton {
            scrollToElementByText("Passwords")
        }.openLoginsAndPasswordSubMenu {
        }.openSavedLogins(composeTestRule) {
            verifySecurityPromptForLogins()
            tapSetupLater()
            // Verify that the login appears correctly
            verifySavedLoginsSectionUsername("test@example.com")
            viewSavedLoginDetails("test@example.com")
            revealPassword()
            verifyPasswordSaved("test")
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/1049971
    @Converted(
        replacedBy = ["org.mozilla.fenix.ui.efficiency.tests.LoginsTest#verifyMultipleLoginsSelectionsTest"],
        bug = 2060474,
        since = "2026-08",
    )
    @SmokeTest
    @Test
    fun verifyMultipleLoginsSelectionsTest() {
        val loginPage = "https://mozilla-mobile.github.io/testapp/v2.0/loginForm.html"
        val firstUser = "mozilla"
        val firstPass = "firefox"
        val secondUser = "fenix"
        val secondPass = "pass"

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(loginPage.toUri()) {
            setPageObjectText(composeTestRule, itemWithResId("username"), firstUser)
            waitForAppWindowToBeUpdated()
            setPageObjectText(composeTestRule, itemWithResId("password"), firstPass)
            waitForAppWindowToBeUpdated()
            clickPageObject(composeTestRule, itemWithResId("submit"))
            verifySaveLoginPromptIsDisplayed()
            clickPageObject(composeTestRule, itemWithText("Save"))
            setPageObjectText(composeTestRule, itemWithResId("username"), secondUser)
            waitForAppWindowToBeUpdated()
            setPageObjectText(composeTestRule, itemWithResId("password"), secondPass)
            waitForAppWindowToBeUpdated()
            clickPageObject(composeTestRule, itemWithResId("submit"))
            verifySaveLoginPromptIsDisplayed()
            clickPageObject(composeTestRule, itemWithText("Save"))
            waitUntilSnackbarGone()
        }
        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(loginPage.toUri()) {
            clickPageObject(composeTestRule, itemWithResId("username"))
            clickSuggestedLoginsButton()
            verifySuggestedUserName(firstUser)
            verifySuggestedUserName(secondUser)
            clickSuggestedLogin(firstUser)
            clickPageObject(composeTestRule, itemWithResId("togglePassword"))
            verifyPrefilledLoginCredentials(firstUser, firstPass, true)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/875849
    @Test
    @SkipLeaks(reasons = ["https://bugzilla.mozilla.org/show_bug.cgi?id=1935209"])
    fun verifyEditLoginsViewTest() {
        val loginPage = "https://mozilla-mobile.github.io/testapp/loginForm"
        val originWebsite = "https://mozilla-mobile.github.io"

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(loginPage.toUri()) {
            setPageObjectText(composeTestRule, itemWithResId("username"), "mozilla")
            waitForAppWindowToBeUpdated()
            setPageObjectText(composeTestRule, itemWithResId("password"), "firefox")
            waitForAppWindowToBeUpdated()
            clickPageObject(composeTestRule, itemWithResId("submit"))
            verifySaveLoginPromptIsDisplayed()
            clickPageObject(composeTestRule, itemWithText("Save"))
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.clickSettingsButton {
        }.openLoginsAndPasswordSubMenu {
        }.openSavedLogins(composeTestRule) {
            tapSetupLater()
            viewSavedLoginDetails(originWebsite)
            clickThreeDotButton()
            clickEditLoginButton()
            setNewPasswordWhileEditingALogin("fenix")
            saveEditedLogin()
            clickThreeDotButton()
            clickEditLoginButton()
            verifyPasswordWhileEditingALogin("fenix")
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/875851
    @Test
    fun verifyEditedLoginsAreSavedTest() {
        val loginPage = "https://mozilla-mobile.github.io/testapp/v2.0/loginForm.html"
        val originWebsite = "https://mozilla-mobile.github.io"

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(loginPage.toUri()) {
            waitForPageToLoad(pageLoadWaitingTime = waitingTimeLong)
            setPageObjectText(composeTestRule, itemWithResId("username"), "mozilla")
            waitForAppWindowToBeUpdated()
            setPageObjectText(composeTestRule, itemWithResId("password"), "firefox")
            waitForAppWindowToBeUpdated()
            clickPageObject(composeTestRule, itemWithResId("submit"))
            verifySaveLoginPromptIsDisplayed()
            clickPageObject(composeTestRule, itemWithText("Save"))
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.clickSettingsButton {
        }.openLoginsAndPasswordSubMenu {
        }.openSavedLogins(composeTestRule) {
            tapSetupLater()
            viewSavedLoginDetails(originWebsite)
            clickThreeDotButton()
            clickEditLoginButton()
            setNewUserNameWhileEditingALogin("android")
            setNewPasswordWhileEditingALogin("fenix")
            saveEditedLogin()
            clickGoBackButton()
        }.goBack {
        }

        exitMenu()

        browserScreen(composeTestRule) {
        }.openThreeDotMenu {
        }.clickRefreshButton {
            waitForPageToLoad(pageLoadWaitingTime = waitingTimeLong)
            clickPageObject(composeTestRule, itemWithResId("togglePassword"))
            verifyPrefilledLoginCredentials("android", "fenix", true)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2266452
    @Test
    @SkipLeaks(reasons = ["https://bugzilla.mozilla.org/show_bug.cgi?id=1935209"])
    fun verifyLoginWithNoUserNameCanNotBeSavedTest() {
        val loginPage = "https://mozilla-mobile.github.io/testapp/loginForm"
        val originWebsite = "https://mozilla-mobile.github.io"

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(loginPage.toUri()) {
            setPageObjectText(composeTestRule, itemWithResId("username"), "mozilla")
            waitForAppWindowToBeUpdated()
            setPageObjectText(composeTestRule, itemWithResId("password"), "firefox")
            waitForAppWindowToBeUpdated()
            clickPageObject(composeTestRule, itemWithResId("submit"))
            verifySaveLoginPromptIsDisplayed()
            clickPageObject(composeTestRule, itemWithText("Save"))
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.clickSettingsButton {
        }.openLoginsAndPasswordSubMenu {
        }.openSavedLogins(composeTestRule) {
            tapSetupLater()
            viewSavedLoginDetails(originWebsite)
            clickThreeDotButton()
            clickEditLoginButton()
            clickClearUserNameButton()
            verifyUserNameRequiredErrorMessage()
            verifySaveLoginButtonIsEnabled(false)
            clickGoBackButton()
            verifyLoginItemUsername("mozilla")
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2266453
    @Test
    @SkipLeaks(reasons = ["https://bugzilla.mozilla.org/show_bug.cgi?id=1935209"])
    fun verifyLoginWithoutPasswordCanNotBeSavedTest() {
        val loginPage = "https://mozilla-mobile.github.io/testapp/loginForm"
        val originWebsite = "https://mozilla-mobile.github.io"

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(loginPage.toUri()) {
            setPageObjectText(composeTestRule, itemWithResId("username"), "mozilla")
            waitForAppWindowToBeUpdated()
            setPageObjectText(composeTestRule, itemWithResId("password"), "firefox")
            waitForAppWindowToBeUpdated()
            clickPageObject(composeTestRule, itemWithResId("submit"))
            verifySaveLoginPromptIsDisplayed()
            clickPageObject(composeTestRule, itemWithText("Save"))
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.clickSettingsButton {
        }.openLoginsAndPasswordSubMenu {
        }.openSavedLogins(composeTestRule) {
            tapSetupLater()
            viewSavedLoginDetails(originWebsite)
            clickThreeDotButton()
            clickEditLoginButton()
            clickClearPasswordButton()
            verifyPasswordRequiredErrorMessage()
            verifySaveLoginButtonIsEnabled(false)
            clickGoBackButton()
            revealPassword()
            verifyPasswordSaved("firefox")
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/876531
    @Test
    @SkipLeaks(reasons = ["https://bugzilla.mozilla.org/show_bug.cgi?id=1935209"])
    fun verifyEditModeDismissalDoesNotSaveLoginCredentialsTest() {
        val loginPage = "https://mozilla-mobile.github.io/testapp/loginForm"
        val originWebsite = "https://mozilla-mobile.github.io"

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(loginPage.toUri()) {
            setPageObjectText(composeTestRule, itemWithResId("username"), "mozilla")
            waitForAppWindowToBeUpdated()
            setPageObjectText(composeTestRule, itemWithResId("password"), "firefox")
            waitForAppWindowToBeUpdated()
            clickPageObject(composeTestRule, itemWithResId("submit"))
            verifySaveLoginPromptIsDisplayed()
            clickPageObject(composeTestRule, itemWithText("Save"))
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.clickSettingsButton {
        }.openLoginsAndPasswordSubMenu {
        }.openSavedLogins(composeTestRule) {
            tapSetupLater()
            viewSavedLoginDetails(originWebsite)
            clickThreeDotButton()
            clickEditLoginButton()
            setNewUserNameWhileEditingALogin("android")
            setNewPasswordWhileEditingALogin("fenix")
            clickGoBackButton()
            verifyLoginItemUsername("mozilla")
            revealPassword()
            verifyPasswordSaved("firefox")
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/876532
    @Test
    fun verifyDeleteLoginButtonTest() {
        val loginPage = mockWebServer.saveLoginAsset

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(loginPage.url) {
            clickSubmitLoginButton()
            clickPageObject(composeTestRule, itemWithText("Save"))
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.clickSettingsButton {
        }.openLoginsAndPasswordSubMenu {
        }.openSavedLogins(composeTestRule) {
            tapSetupLater()
            viewSavedLoginDetails("test@example.com")
            clickThreeDotButton()
            clickDeleteLoginButton()
            verifyLoginDeletionPrompt()
            clickCancelDeleteLogin()
            verifyLoginItemUsername("test@example.com")
            viewSavedLoginDetails("test@example.com")
            clickThreeDotButton()
            clickDeleteLoginButton()
            verifyLoginDeletionPrompt()
            clickConfirmDeleteLogin()
            // The account remains displayed, see: https://bugzilla.mozilla.org/show_bug.cgi?id=1812431
            // verifyNotSavedLoginFromPrompt()
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/517818
    @Converted(
        replacedBy = ["org.mozilla.fenix.ui.efficiency.tests.SettingsPasswordsTest#verifyNeverSaveLoginOptionTest"],
        bug = 2057407,
        since = "2026-07",
    )
    @SmokeTest
    @Test
    @SkipLeaks(reasons = ["https://bugzilla.mozilla.org/show_bug.cgi?id=1935209"])
    fun verifyNeverSaveLoginOptionTest() {
        val loginPage = mockWebServer.saveLoginAsset

        homeScreen(composeTestRule) {
        }.openThreeDotMenu {
        }.clickSettingsButton {
        }.openLoginsAndPasswordSubMenu {
        }.openSaveLoginsAndPasswordsOptions {
            clickNeverSaveOption()
        }.goBack {
        }

        exitMenu()

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(loginPage.url) {
            clickSubmitLoginButton()
            verifySaveLoginPromptIsNotDisplayed()
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/517819
    @Test
    @SkipLeaks(reasons = ["https://bugzilla.mozilla.org/show_bug.cgi?id=1935209"])
    fun verifyAutofillToggleTest() {
        val loginPage = "https://mozilla-mobile.github.io/testapp/v2.0/loginForm.html"

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(loginPage.toUri()) {
            waitForPageToLoad(pageLoadWaitingTime = waitingTimeLong)
            setPageObjectText(composeTestRule, itemWithResId("username"), "mozilla")
            waitForAppWindowToBeUpdated()
            setPageObjectText(composeTestRule, itemWithResId("password"), "firefox")
            waitForAppWindowToBeUpdated()
            clickPageObject(composeTestRule, itemWithResId("submit"))
            waitForPageToLoad(pageLoadWaitingTime = waitingTimeLong)
            verifySaveLoginPromptIsDisplayed()
            clickPageObject(composeTestRule, itemWithText("Save"))
        }.openTabDrawer(composeTestRule) {
            closeTab()
        }

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(loginPage.toUri()) {
            waitForPageToLoad(pageLoadWaitingTime = waitingTimeLong)
            clickPageObject(composeTestRule, itemWithResId("togglePassword"))
            verifyPrefilledLoginCredentials("mozilla", "firefox", true)
        }.openTabDrawer(composeTestRule) {
            closeTab()
        }

        homeScreen(composeTestRule) {
        }.openThreeDotMenu {
        }.clickSettingsButton {
        }.openLoginsAndPasswordSubMenu {
            verifyAutofillInFirefoxToggle(true)
            clickAutofillInFirefoxOption()
            verifyAutofillInFirefoxToggle(false)
        }.goBack {
        }

        exitMenu()

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(loginPage.toUri()) {
            verifyPrefilledLoginCredentials("mozilla", "firefox", false)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/593768
    @Test
    @SkipLeaks(reasons = ["https://bugzilla.mozilla.org/show_bug.cgi?id=1935209"])
    fun doNotSaveOptionWillNotUpdateALoginTest() {
        val loginPage = "https://mozilla-mobile.github.io/testapp/v2.0/loginForm.html"
        val originWebsite = "https://mozilla-mobile.github.io"

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(loginPage.toUri()) {
            setPageObjectText(composeTestRule, itemWithResId("username"), "mozilla")
            waitForAppWindowToBeUpdated()
            setPageObjectText(composeTestRule, itemWithResId("password"), "firefox")
            waitForAppWindowToBeUpdated()
            clickPageObject(composeTestRule, itemWithResId("submit"))
            verifySaveLoginPromptIsDisplayed()
            clickPageObject(composeTestRule, itemWithText("Save"))
            waitForAppWindowToBeUpdated()
            clickPageObject(composeTestRule, itemWithResId("togglePassword"))
            setPageObjectText(composeTestRule, itemWithResId("username"), "mozilla")
            waitForAppWindowToBeUpdated()
            setPageObjectText(composeTestRule, itemWithResId("password"), "fenix")
            waitForAppWindowToBeUpdated()
            clickPageObject(composeTestRule, itemWithResId("submit"))
            verifySaveLoginPromptIsDisplayed()
            clickPageObject(composeTestRule, itemWithText("Not now"))
        }.openThreeDotMenu {
        }.clickSettingsButton {
        }.openLoginsAndPasswordSubMenu {
        }.openSavedLogins(composeTestRule) {
            tapSetupLater()
            viewSavedLoginDetails(originWebsite)
            revealPassword()
            verifyPasswordSaved("firefox")
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2090455
    @Test
    @SkipLeaks(reasons = ["https://bugzilla.mozilla.org/show_bug.cgi?id=1935209"])
    fun searchLoginsByUsernameTest() {
        val firstLoginPage = mockWebServer.saveLoginAsset
        val secondLoginPage = "https://mozilla-mobile.github.io/testapp/v2.0/loginForm.html"
        val originWebsite = "https://mozilla-mobile.github.io"

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(firstLoginPage.url) {
            clickSubmitLoginButton()
            verifySaveLoginPromptIsDisplayed()
            clickPageObject(composeTestRule, itemWithText("Save"))
        }
        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(secondLoginPage.toUri()) {
            setPageObjectText(composeTestRule, itemWithResId("username"), "android")
            setPageObjectText(composeTestRule, itemWithResId("password"), "firefox")
            clickPageObject(composeTestRule, itemWithResId("submit"))
            verifySaveLoginPromptIsDisplayed()
            clickPageObject(composeTestRule, itemWithText("Save"))
        }.openThreeDotMenu {
        }.clickSettingsButton {
        }.openLoginsAndPasswordSubMenu {
        }.openSavedLogins(composeTestRule) {
            tapSetupLater()
            clickSearchLoginButton()
            searchLogin("ANDROID")
            viewSavedLoginDetails(originWebsite)
            verifyLoginItemUsername("android")
            revealPassword()
            verifyPasswordSaved("firefox")
            clickGoBackButton()
            clickSearchLoginButton()
            searchLogin("android")
            viewSavedLoginDetails(originWebsite)
            verifyLoginItemUsername("android")
            revealPassword()
            verifyPasswordSaved("firefox")
            clickGoBackButton()
            clickSearchLoginButton()
            searchLogin("AnDrOiD")
            viewSavedLoginDetails(originWebsite)
            verifyLoginItemUsername("android")
            revealPassword()
            verifyPasswordSaved("firefox")
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/608834
    @Test
    @SkipLeaks(reasons = ["https://bugzilla.mozilla.org/show_bug.cgi?id=1935209"])
    fun searchLoginsByUrlTest() {
        val firstLoginPage = mockWebServer.saveLoginAsset
        val secondLoginPage = "https://mozilla-mobile.github.io/testapp/v2.0/loginForm.html"
        val originWebsite = "https://mozilla-mobile.github.io"

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(firstLoginPage.url) {
            clickSubmitLoginButton()
            verifySaveLoginPromptIsDisplayed()
            clickPageObject(composeTestRule, itemWithText("Save"))
        }
        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(secondLoginPage.toUri()) {
            setPageObjectText(composeTestRule, itemWithResId("username"), "android")
            setPageObjectText(composeTestRule, itemWithResId("password"), "firefox")
            clickPageObject(composeTestRule, itemWithResId("submit"))
            verifySaveLoginPromptIsDisplayed()
            clickPageObject(composeTestRule, itemWithText("Save"))
        }.openThreeDotMenu {
        }.clickSettingsButton {
        }.openLoginsAndPasswordSubMenu {
        }.openSavedLogins(composeTestRule) {
            tapSetupLater()
            clickSearchLoginButton()
            searchLogin("MOZILLA")
            viewSavedLoginDetails(originWebsite)
            verifyLoginItemUsername("android")
            revealPassword()
            verifyPasswordSaved("firefox")
            clickGoBackButton()
            clickSearchLoginButton()
            searchLogin("mozilla")
            viewSavedLoginDetails(originWebsite)
            verifyLoginItemUsername("android")
            revealPassword()
            verifyPasswordSaved("firefox")
            clickGoBackButton()
            clickSearchLoginButton()
            searchLogin("MoZiLlA")
            viewSavedLoginDetails(originWebsite)
            verifyLoginItemUsername("android")
            revealPassword()
            verifyPasswordSaved("firefox")
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2266441
    @Test
    fun verifyLastUsedLoginSortingOptionTest() {
        val firstLoginPage = mockWebServer.saveLoginAsset
        val secondLoginPage = "https://mozilla-mobile.github.io/testapp/v2.0/loginForm.html"

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(firstLoginPage.url) {
            clickSubmitLoginButton()
            verifySaveLoginPromptIsDisplayed()
            clickPageObject(composeTestRule, itemWithText("Save"))
        }
        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(secondLoginPage.toUri()) {
            setPageObjectText(composeTestRule, itemWithResId("username"), "mozilla")
            setPageObjectText(composeTestRule, itemWithResId("password"), "firefox")
            clickPageObject(composeTestRule, itemWithResId("submit"))
            verifySaveLoginPromptIsDisplayed()
            clickPageObject(composeTestRule, itemWithText("Save"))
        }.openThreeDotMenu {
        }.clickSettingsButton {
        }.openLoginsAndPasswordSubMenu {
        }.openSavedLogins(composeTestRule) {
            tapSetupLater()
            clickSortPasswordsButton()
            verifyLoginsSortingOptions()
            clickLastUsedSortingOption()
            verifySortedLogin(1, "https://mozilla-mobile.github.io")
            verifySortedLogin(2, "${firstLoginPage.url.scheme}://${firstLoginPage.url.authority}")
        }.goBack {
        }.openSavedLogins(composeTestRule) {
            verifySortedLogin(1, "https://mozilla-mobile.github.io")
            verifySortedLogin(2, "${firstLoginPage.url.scheme}://${firstLoginPage.url.authority}")
        }

        restartApp(composeTestRule.activityRule)

        browserScreen(composeTestRule) {
        }.openThreeDotMenu {
        }.clickSettingsButton {
        }.openLoginsAndPasswordSubMenu {
        }.openSavedLogins(composeTestRule) {
            verifySortedLogin(1, "https://mozilla-mobile.github.io")
            verifySortedLogin(2, "${firstLoginPage.url.scheme}://${firstLoginPage.url.authority}")
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2266442
    @Test
    fun verifyAlphabeticalLoginSortingOptionTest() {
        val firstLoginPage = mockWebServer.saveLoginAsset
        val secondLoginPage = "https://mozilla-mobile.github.io/testapp/v2.0/loginForm.html"

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(firstLoginPage.url) {
            clickSubmitLoginButton()
            verifySaveLoginPromptIsDisplayed()
            clickPageObject(composeTestRule, itemWithText("Save"))
        }
        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(secondLoginPage.toUri()) {
            setPageObjectText(composeTestRule, itemWithResId("username"), "mozilla")
            setPageObjectText(composeTestRule, itemWithResId("password"), "firefox")
            clickPageObject(composeTestRule, itemWithResId("submit"))
            verifySaveLoginPromptIsDisplayed()
            clickPageObject(composeTestRule, itemWithText("Save"))
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.clickSettingsButton {
        }.openLoginsAndPasswordSubMenu {
        }.openSavedLogins(composeTestRule) {
            tapSetupLater()
            verifySortedLogin(1, "${firstLoginPage.url.scheme}://${firstLoginPage.url.authority}")
            verifySortedLogin(2, "https://mozilla-mobile.github.io")
        }.goBack {
        }.openSavedLogins(composeTestRule) {
            verifySortedLogin(1, "${firstLoginPage.url.scheme}://${firstLoginPage.url.authority}")
            verifySortedLogin(2, "https://mozilla-mobile.github.io")
        }

        restartApp(composeTestRule.activityRule)

        browserScreen(composeTestRule) {
        }.openThreeDotMenu {
        }.clickSettingsButton {
        }.openLoginsAndPasswordSubMenu {
        }.openSavedLogins(composeTestRule) {
            verifySortedLogin(1, "${firstLoginPage.url.scheme}://${firstLoginPage.url.authority}")
            verifySortedLogin(2, "https://mozilla-mobile.github.io")
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/1518435
    @Test
    fun verifyAddLoginManuallyTest() {
        val loginPage = "https://mozilla-mobile.github.io/testapp/v2.0/loginForm.html"

        homeScreen(composeTestRule) {
        }.openThreeDotMenu {
        }.clickSettingsButton {
        }.openLoginsAndPasswordSubMenu {
        }.openSavedLogins(composeTestRule) {
            tapSetupLater()
            clickAddLoginButton()
            verifyAddNewLoginView()
            enterSiteCredentialWhileAddingALogin("mozilla")
            verifyHostnameErrorMessage()
            enterSiteCredentialWhileAddingALogin(loginPage)
            verifyHostnameClearButton()
            setUserNameWhileAddingANewLogin("mozilla")
            setNewPasswordWhileAddingANewLogin("firefox")
            clickClearPasswordButton()
            verifyPasswordErrorMessage()
            setNewPasswordWhileAddingANewLogin("firefox")
            verifyPasswordClearButton()
            saveNewLogin()
            clickGoBackButton()
        }.goBack {
        }

        exitMenu()

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(loginPage.toUri()) {
            clickPageObject(composeTestRule, itemWithResId("username"))
            clickSuggestedLoginsButton()
            verifySuggestedUserName("mozilla")
            clickSuggestedLogin("mozilla")
            clickPageObject(composeTestRule, itemWithResId("togglePassword"))
            verifyPrefilledLoginCredentials("mozilla", "firefox", true)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2068215
    // The snackbar is not displayed for devices running on Android <= 12
    @Test
    @SdkSuppress(maxSdkVersion = 32)
    fun verifyCopyLoginCredentialsToClipboardTest() {
        val firstLoginPage = mockWebServer.saveLoginAsset

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(firstLoginPage.url) {
            clickSubmitLoginButton()
            verifySaveLoginPromptIsDisplayed()
            clickPageObject(composeTestRule, itemWithText("Save"))
        }.openThreeDotMenu {
        }.clickSettingsButton {
        }.openLoginsAndPasswordSubMenu {
        }.openSavedLogins(composeTestRule) {
            tapSetupLater()
            viewSavedLoginDetails("test@example.com")
            clickCopyUserNameButton()
            verifyCopyUserNameLoginCredentialsSnackBar()
            waitUntilCopyLoginCredentialsSnackBarIsGone()
            clickCopyPasswordButton()
            verifyCopyPasswordLoginCredentialsSnackBar()
        }
    }
}
