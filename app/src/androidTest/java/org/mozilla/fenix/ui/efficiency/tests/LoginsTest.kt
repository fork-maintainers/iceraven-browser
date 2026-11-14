/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mozilla.fenix.customannotations.Critical
import org.mozilla.fenix.customannotations.SkipLeaks
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.TestAssetHelper.saveLoginAsset
import org.mozilla.fenix.helpers.TestAssetHelper.signupFormAsset
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.selectors.BrowserPageSelectors
import org.mozilla.fenix.ui.efficiency.selectors.SettingsLoginExceptionsSelectors
import org.mozilla.fenix.ui.efficiency.selectors.SettingsPasswordsSelectors
import org.mozilla.fenix.ui.efficiency.selectors.SettingsSavedPasswordsSelectors

class LoginsTest : BaseTest() {

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/1508171
    @SmokeTest
    @Test
    fun verifyUpdatedLoginIsSavedTest() {
        val loginPage = mockWebServer.saveLoginAsset

        // Save the login for the first time
        on.browserPage.navigateToPage(loginPage.url.toString())
        on.browserPage.mozVerify(BrowserPageSelectors.SUBMIT_LOGIN_BUTTON)
        on.browserPage.clickSubmitLoginButton()
        on.browserPage.verifySaveLoginPromptIsDisplayed()
        on.browserPage.mozClick(BrowserPageSelectors.SAVE_LOGIN_PROMPT_CONFIRM_BUTTON)

        // Re-submit with a changed password and update the saved login. Route back through Home first
        on.home.navigateToPage()
        on.browserPage.navigateToPage(loginPage.url.toString())
        on.browserPage.mozVerify(BrowserPageSelectors.PASSWORD_WEB_FIELD)
        on.browserPage.mozClick(BrowserPageSelectors.PASSWORD_WEB_FIELD)
        on.browserPage.mozEnterText("test", BrowserPageSelectors.PASSWORD_WEB_FIELD)
        on.browserPage.clickSubmitLoginButton()
        on.browserPage.verifySaveLoginPromptIsDisplayed()
        on.browserPage.mozClick(BrowserPageSelectors.SAVE_LOGIN_PROMPT_CONFIRM_BUTTON)

        // Verify the updated password is saved
        on.settingsSavedPasswords.navigateToPage()
        on.settingsSavedPasswords.mozVerify(SettingsSavedPasswordsSelectors.SAVED_LOGIN_ENTRY("test@example.com"))
        on.settingsSavedPasswords.mozClick(SettingsSavedPasswordsSelectors.SAVED_LOGIN_ENTRY("test@example.com"))
        on.settingsSavedPasswords.mozClick(SettingsSavedPasswordsSelectors.REVEAL_PASSWORD_BUTTON)
        on.settingsSavedPasswords.mozVerify(SettingsSavedPasswordsSelectors.LOGIN_DETAILS_PASSWORD("test"))
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/1049971
    // Converted from legacy LoginsTest.verifyMultipleLoginsSelectionsTest
    @SmokeTest
    @Test
    fun verifyMultipleLoginsSelectionsTest() {
        val loginPage = "https://mozilla-mobile.github.io/testapp/v2.0/loginForm.html"
        val firstUser = "mozilla"
        val firstPass = "firefox"
        val secondUser = "fenix"
        val secondPass = "pass"

        // Save two distinct logins for the same site.
        on.browserPage.navigateToPage(loginPage)
        on.browserPage.mozVerify(BrowserPageSelectors.USERNAME_WEB_FIELD)
        on.browserPage.setLoginFormCredentials(firstUser, firstPass)
        on.browserPage.clickSubmitLoginButton()
        on.browserPage.verifySaveLoginPromptIsDisplayed()
        on.browserPage.mozClick(BrowserPageSelectors.SAVE_LOGIN_PROMPT_CONFIRM_BUTTON)

        on.browserPage.setLoginFormCredentials(secondUser, secondPass)
        on.browserPage.clickSubmitLoginButton()
        on.browserPage.verifySaveLoginPromptIsDisplayed()
        on.browserPage.mozClick(BrowserPageSelectors.SAVE_LOGIN_PROMPT_CONFIRM_BUTTON)

        // Reload from Home and check both logins are
        // suggested, then pick the first and confirm the form is prefilled with its credentials.
        on.home.navigateToPage()
        on.browserPage.navigateToPage(loginPage)
        on.browserPage.mozVerify(BrowserPageSelectors.USERNAME_WEB_FIELD)
        on.browserPage.clickSuggestedLoginsBar()
        on.browserPage.mozVerify(BrowserPageSelectors.SUGGESTED_LOGIN(firstUser))
        on.browserPage.mozVerify(BrowserPageSelectors.SUGGESTED_LOGIN(secondUser))
        on.browserPage.mozClick(BrowserPageSelectors.SUGGESTED_LOGIN(firstUser))
        on.browserPage.mozClick(BrowserPageSelectors.TOGGLE_PASSWORD_WEB_BUTTON)
        on.browserPage.mozVerify(BrowserPageSelectors.PREFILLED_USERNAME(firstUser))
        on.browserPage.mozVerify(BrowserPageSelectors.PREFILLED_PASSWORD(firstPass))
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/875849
    @Critical
    @Test
    fun verifyEditLoginsViewTest() {
        val loginPage = "https://mozilla-mobile.github.io/testapp/loginForm"
        val originWebsite = "https://mozilla-mobile.github.io"

        // Save a login for the site.
        on.browserPage.navigateToPage(loginPage)
        on.browserPage.mozVerify(BrowserPageSelectors.USERNAME_WEB_FIELD)
        on.browserPage.setLoginFormCredentials("mozilla", "firefox")
        on.browserPage.clickSubmitLoginButton()
        on.browserPage.verifySaveLoginPromptIsDisplayed()
        on.browserPage.mozClick(BrowserPageSelectors.SAVE_LOGIN_PROMPT_CONFIRM_BUTTON)

        // Open the saved login, edit its password, save, then reopen the editor and confirm it stuck.
        on.settingsSavedPasswords.navigateToPage()
        on.settingsSavedPasswords.mozClick(SettingsSavedPasswordsSelectors.SAVED_LOGIN_ENTRY(originWebsite))
        on.settingsSavedPasswords.mozClick(SettingsSavedPasswordsSelectors.LOGIN_DETAILS_MENU_BUTTON)
        on.settingsSavedPasswords.mozClick(SettingsSavedPasswordsSelectors.EDIT_LOGIN_MENU_OPTION)
        on.settingsSavedPasswords.mozClick(SettingsSavedPasswordsSelectors.EDIT_LOGIN_PASSWORD_FIELD)
        on.settingsSavedPasswords.mozClearAndEnterText(
            "fenix",
            SettingsSavedPasswordsSelectors.EDIT_LOGIN_PASSWORD_FIELD,
        )
        on.settingsSavedPasswords.mozClick(SettingsSavedPasswordsSelectors.SAVE_EDITED_LOGIN_BUTTON)
        on.settingsSavedPasswords.mozClick(SettingsSavedPasswordsSelectors.LOGIN_DETAILS_MENU_BUTTON)
        on.settingsSavedPasswords.mozClick(SettingsSavedPasswordsSelectors.EDIT_LOGIN_MENU_OPTION)
        on.settingsSavedPasswords.mozVerify(SettingsSavedPasswordsSelectors.EDIT_LOGIN_PASSWORD("fenix"))
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/517817
    @Critical
    @Test
    fun neverSaveLoginFromPromptTest() {
        val loginPage = mockWebServer.saveLoginAsset

        // Submit the login form and decline saving, which adds the site to the never-save exceptions.
        on.browserPage.navigateToPage(loginPage.url.toString())
        on.browserPage.mozVerify(BrowserPageSelectors.SUBMIT_LOGIN_BUTTON)
        on.browserPage.clickSubmitLoginButton()
        on.browserPage.verifySaveLoginPromptIsDisplayed()
        // The prompt shows the accessed domain's favicon.
        on.browserPage.mozVerify(BrowserPageSelectors.SAVE_LOGIN_PROMPT_FAVICON)
        on.browserPage.mozClick(BrowserPageSelectors.SAVE_LOGIN_PROMPT_NEVER_SAVE_BUTTON)

        // The Logins and passwords settings screen shows its default options.
        on.settingsPasswords.navigateToPage()
        on.settingsPasswords.mozVerifyElementsByGroup(SettingsPasswordsSelectors.Group.PASSWORD_SETTINGS)

        // The site was added to the login exceptions list (one tap from the Passwords screen).
        on.settingsLoginExceptions.navigateToPage()
        on.settingsLoginExceptions.mozVerify(SettingsLoginExceptionsSelectors.EXCEPTION_ROW("localhost"))

        // The login was not saved: back to the Passwords screen, then the saved passwords list is empty.
        // Parity gap: legacy verifySecurityPromptForLogins() is not asserted here because the
        // navigation layer auto-dismisses the "Secure your saved passwords" dialog via a
        // ClickIfPresent step, leaving no seam to assert it beforehand.
        on.settingsSavedPasswords.navigateToPage()
        on.settingsSavedPasswords.mozVerify(SettingsSavedPasswordsSelectors.EMPTY_SAVED_PASSWORDS_LIST_DESCRIPTION)
        on.settingsSavedPasswords.mozVerify(
            SettingsSavedPasswordsSelectors.EMPTY_SAVED_PASSWORDS_LIST_LEARN_MORE_ABOUT_SYNC
        )
        on.settingsSavedPasswords.mozVerify(
            SettingsSavedPasswordsSelectors.EMPTY_SAVED_PASSWORDS_LIST_ADD_PASSWORD_BUTTON
        )
        on.settingsSavedPasswords.mozVerifyElementAbsent(
            SettingsSavedPasswordsSelectors.SAVED_LOGIN_ENTRY("test@example.com")
        )
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/593768
    @Critical
    @Test
    fun doNotSaveOptionWillNotUpdateALoginTest() {
        val loginPage = mockWebServer.saveLoginAsset

        // Save the login with a known password so the declined update can be checked against it.
        on.browserPage.navigateToPage(loginPage.url.toString())
        on.browserPage.mozVerify(BrowserPageSelectors.PASSWORD_WEB_FIELD)
        on.browserPage.mozClick(BrowserPageSelectors.PASSWORD_WEB_FIELD)
        on.browserPage.mozEnterText("firefox", BrowserPageSelectors.PASSWORD_WEB_FIELD)
        on.browserPage.clickSubmitLoginButton()
        on.browserPage.verifySaveLoginPromptIsDisplayed()
        on.browserPage.mozClick(BrowserPageSelectors.SAVE_LOGIN_PROMPT_CONFIRM_BUTTON)

        // Reload the page so the fixture's script re-randomizes the password to a value that differs
        // from the saved one, re-submit, and decline the update via "Not now" (the update prompt's
        // negative button shares the save_cancel res-id). The reload is required: re-submitting the
        // un-reloaded form does not re-fire GeckoView's update detection, so no update prompt would
        // appear. Route back through Home first.
        on.home.navigateToPage()
        on.browserPage.navigateToPage(loginPage.url.toString())
        on.browserPage.mozVerify(BrowserPageSelectors.PASSWORD_WEB_FIELD)
        on.browserPage.clickSubmitLoginButton()
        on.browserPage.verifySaveLoginPromptIsDisplayed()
        on.browserPage.mozClick(BrowserPageSelectors.SAVE_LOGIN_PROMPT_NEVER_SAVE_BUTTON)

        // The saved login keeps its original password because the update was declined.
        on.settingsSavedPasswords.navigateToPage()
        on.settingsSavedPasswords.mozClick(SettingsSavedPasswordsSelectors.SAVED_LOGIN_ENTRY("test@example.com"))
        on.settingsSavedPasswords.mozClick(SettingsSavedPasswordsSelectors.REVEAL_PASSWORD_BUTTON)
        on.settingsSavedPasswords.mozVerify(SettingsSavedPasswordsSelectors.LOGIN_DETAILS_PASSWORD("firefox"))
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2710735
    // SkipLeaks: known BrowserFragment autofill-bar view-retention leak, bug 1935100, not caused by this test.
    @Critical
    @SkipLeaks(reasons = ["https://bugzilla.mozilla.org/show_bug.cgi?id=1935100"])
    @Test
    fun verifySuggestedStrongPasswordDoesNotChangeTest() {
        val signupPage = mockWebServer.signupFormAsset

        // The first engagement auto-opens the generator.
        on.browserPage.navigateToPage(signupPage.url.toString())
        on.browserPage.clickPasswordField()
        on.browserPage.verifyStrongPasswordPromptIsDisplayed()
        on.browserPage.clickUseStrongPassword()
        val firstSuggestion = on.browserPage.getGeneratedPassword()

        // On a reload the generator is raised via the bar, not auto-opened. Route back through Home.
        on.home.navigateToPage()
        on.browserPage.navigateToPage(signupPage.url.toString(), forceNavigation = true)
        on.browserPage.clickSuggestStrongPasswordBar()
        on.browserPage.verifyStrongPasswordPromptIsDisplayed()
        on.browserPage.clickUseStrongPassword()
        val secondSuggestion = on.browserPage.getGeneratedPassword()

        // The same password is suggested until used for a new account.
        assertEquals(firstSuggestion, secondSuggestion)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2710736
    // SkipLeaks: known BrowserFragment autofill-bar view-retention leak, bug 1935100, not caused by this test.
    @Critical
    @SkipLeaks(reasons = ["https://bugzilla.mozilla.org/show_bug.cgi?id=1935100"])
    @Test
    fun verifyUpdatePromptForGeneratedPasswordTest() {
        val signupPage = mockWebServer.signupFormAsset
        val username = "test@example.com"

        // Accept the suggested password: saves the login with no username.
        on.browserPage.navigateToPage(signupPage.url.toString())
        on.browserPage.clickPasswordField()
        on.browserPage.verifyStrongPasswordPromptIsDisplayed()
        on.browserPage.clickUseStrongPassword()
        val generatedPassword = on.browserPage.getGeneratedPassword()

        // Adding a username raises a second prompt to update the login.
        on.browserPage.mozClearAndEnterText(username, BrowserPageSelectors.USERNAME_WEB_FIELD)
        on.browserPage.clickSubmitLoginButton()
        on.browserPage.verifyUpdateUsernamePromptIsDisplayed()
        on.browserPage.mozClick(BrowserPageSelectors.SAVE_LOGIN_PROMPT_CONFIRM_BUTTON)

        // The login now carries the username alongside the generated password.
        on.settingsSavedPasswords.navigateToPage()
        on.settingsSavedPasswords.mozClick(SettingsSavedPasswordsSelectors.SAVED_LOGIN_ENTRY(username))
        on.settingsSavedPasswords.mozClick(SettingsSavedPasswordsSelectors.REVEAL_PASSWORD_BUTTON)
        on.settingsSavedPasswords.mozVerify(SettingsSavedPasswordsSelectors.LOGIN_DETAILS_PASSWORD(generatedPassword))
    }
}
