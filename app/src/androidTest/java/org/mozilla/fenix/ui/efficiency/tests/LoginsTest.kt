/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.TestAssetHelper.saveLoginAsset
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.selectors.BrowserPageSelectors
import org.mozilla.fenix.ui.efficiency.selectors.SettingsSavedPasswordsSelectors

class LoginsTest : BaseTest() {

    private val mockWebServer get() = fenixTestRule.mockWebServer

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
}
