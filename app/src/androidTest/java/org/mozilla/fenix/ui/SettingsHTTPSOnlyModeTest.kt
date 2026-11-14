/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2
import androidx.core.net.toUri
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.customannotations.Converted
import org.mozilla.fenix.customannotations.Critical
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.FenixTestRule
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.MatcherHelper.itemContainingText
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeShort
import org.mozilla.fenix.helpers.TestHelper.exitMenu
import org.mozilla.fenix.helpers.perf.DetectMemoryLeaksRule
import org.mozilla.fenix.ui.robots.clickPageObject
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar

class SettingsHTTPSOnlyModeTest {
    @get:Rule(order = 0) val fenixTestRule: FenixTestRule = FenixTestRule()

    private val secondHttpPageUrl = "http://permission.site/"
    private val secondHttpsPageUrl = "https://permission.site/"
    private val insecureHttpPage = "http.badssl.com"

    // "HTTPs not supported" error page contents:
    private val httpsOnlyErrorTitle = "Secure site not available"
    private val httpsOnlyErrorMessage = "Most likely, the website simply does not support HTTPS."
    private val httpsOnlyErrorMessage2 =
        "However, it’s also possible that an attacker is involved. If you continue to the website, you should not enter any sensitive info. If you continue, HTTPS-Only mode will be turned off temporarily for the site."
    private val httpsOnlyContinueButton = "Continue to HTTP Site"
    private val httpsOnlyBackButton = "Go Back (Recommended)"

    @get:Rule(order = 1)
    val composeTestRule =
        AndroidComposeTestRuleV2(HomeActivityIntentTestRule.withDefaultSettingsOverrides()) { it.activity }

    @get:Rule(order = 2) val memoryLeaksRule = DetectMemoryLeaksRule(composeTestRule = { composeTestRule })

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/1724825
    @Converted(
        replacedBy = ["org.mozilla.fenix.ui.efficiency.tests.SettingsHTTPSOnlyModeTest#httpsOnlyModeMenuItemsTest"],
        bug = 2076438,
        since = "2026-10",
    )
    @Critical
    @Test
    fun httpsOnlyModeMenuItemsTest() {
        homeScreen(composeTestRule) {}
            .openThreeDotMenu {}
            .clickSettingsButton {}
            .openHttpsOnlyModeMenu {
                verifyHttpsOnlyModeMenuHeader()
                verifyHttpsOnlyModeSummary()
                verifyHttpsOnlyModeIsEnabled(false)
                verifyHttpsOnlyModeOptionsEnabled(false)
                verifyHttpsOnlyOptionSelected(
                    allTabsOptionSelected = false,
                    privateTabsOptionSelected = false,
                )
                clickHttpsOnlyModeSwitch()
                verifyHttpsOnlyModeIsEnabled(true)
                verifyHttpsOnlyModeOptionsEnabled(true)
                verifyHttpsOnlyOptionSelected(
                    allTabsOptionSelected = true,
                    privateTabsOptionSelected = false,
                )
            }
            .goBack {
                verifySettingsToolbar()
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/1724827
    @Converted(
        replacedBy =
            [
                "org.mozilla.fenix.ui.efficiency.tests.SettingsHTTPSOnlyModeTest#httpsOnlyModeEnabledInNormalBrowsingTest"
            ],
        bug = 2037892,
        since = "2026-05",
    )
    @SmokeTest
    @Test
    fun httpsOnlyModeEnabledInNormalBrowsingTest() {
        homeScreen(composeTestRule) {}
            .openThreeDotMenu {}
            .clickSettingsButton {}
            .openHttpsOnlyModeMenu {
                clickHttpsOnlyModeSwitch()
                verifyHttpsOnlyOptionSelected(
                    allTabsOptionSelected = true,
                    privateTabsOptionSelected = false,
                )
            }
            .goBack {
                verifySettingsOptionSummary("HTTPS-Only Mode", "On in all tabs")
                exitMenu()
            }
        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(secondHttpPageUrl.toUri()) {
                verifyPageContent("permission.site")
            }
            .openSearch {
                verifyTypedToolbarText(secondHttpsPageUrl, exists = true)
            }
            .dismissSearchBar {}
        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(insecureHttpPage.toUri()) {
                verifyPageContent(httpsOnlyErrorTitle)
                verifyPageContent(httpsOnlyErrorMessage)
                verifyPageContent(httpsOnlyErrorMessage2)
                verifyPageContent(httpsOnlyBackButton)
                clickPageObject(composeTestRule, itemContainingText(httpsOnlyBackButton))
                // Workaround required with Fission ON:
                // Click back twice to avoid https://bugzilla.mozilla.org/show_bug.cgi?id=1932498
                if (itemContainingText(httpsOnlyBackButton).waitForExists(waitingTimeShort)) {
                    clickPageObject(composeTestRule, itemContainingText(httpsOnlyBackButton))
                }
                verifyPageContent("permission.site")
            }
        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(insecureHttpPage.toUri()) {
                clickPageObject(composeTestRule, itemContainingText(httpsOnlyContinueButton))
                verifyPageContent("http.badssl.com")
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2091057
    @Converted(
        replacedBy =
            [
                "org.mozilla.fenix.ui.efficiency.tests.SettingsHTTPSOnlyModeTest#httpsOnlyModeExceptionPersistsForCurrentSessionTest"
            ],
        bug = 2076438,
        since = "2026-10",
    )
    @Critical
    @Test
    fun httpsOnlyModeExceptionPersistsForCurrentSessionTest() {
        homeScreen(composeTestRule) {}
            .openThreeDotMenu {}
            .clickSettingsButton {}
            .openHttpsOnlyModeMenu {
                clickHttpsOnlyModeSwitch()
                verifyHttpsOnlyOptionSelected(
                    allTabsOptionSelected = true,
                    privateTabsOptionSelected = false,
                )
                exitMenu()
            }
        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(insecureHttpPage.toUri()) {
                verifyPageContent(httpsOnlyErrorTitle)
                clickPageObject(composeTestRule, itemContainingText(httpsOnlyContinueButton))
                verifyPageContent("http.badssl.com")
            }
            .openTabDrawer(composeTestRule) {
                closeTab()
            }
        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(insecureHttpPage.toUri()) {
                verifyPageContent("http.badssl.com")
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/1724828
    @Test
    fun httpsOnlyModeEnabledOnlyInPrivateBrowsingTest() {
        homeScreen(composeTestRule) {}
            .openThreeDotMenu {}
            .clickSettingsButton {}
            .openHttpsOnlyModeMenu {
                clickHttpsOnlyModeSwitch()
                selectHttpsOnlyModeOption(
                    allTabsOptionSelected = false,
                    privateTabsOptionSelected = true,
                )
            }
            .goBack {
                verifySettingsOptionSummary("HTTPS-Only Mode", "On in private tabs")
                exitMenu()
            }
        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(insecureHttpPage.toUri()) {
                verifyPageContent("http.badssl.com")
            }
            .goToHomescreen {}
            .togglePrivateBrowsingMode()
        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(secondHttpPageUrl.toUri()) {
                verifyPageContent("Notifications")
            }
            .openSearch {
                verifyTypedToolbarText(secondHttpsPageUrl, exists = true)
            }
            .dismissSearchBar {}
        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(insecureHttpPage.toUri()) {
                verifyPageContent(httpsOnlyErrorTitle)
                verifyPageContent(httpsOnlyErrorMessage)
                verifyPageContent(httpsOnlyErrorMessage2)
                verifyPageContent(httpsOnlyBackButton)
                clickPageObject(composeTestRule, itemContainingText(httpsOnlyBackButton))
                // Workaround required with Fission ON:
                // Click back twice to avoid https://bugzilla.mozilla.org/show_bug.cgi?id=1932498
                if (itemContainingText(httpsOnlyBackButton).waitForExists(waitingTimeShort)) {
                    clickPageObject(composeTestRule, itemContainingText(httpsOnlyBackButton))
                }
                verifyPageContent("Notifications")
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2091058
    @Test
    fun turnOffHttpsOnlyModeTest() {
        homeScreen(composeTestRule) {}
            .openThreeDotMenu {}
            .clickSettingsButton {}
            .openHttpsOnlyModeMenu {
                clickHttpsOnlyModeSwitch()
                verifyHttpsOnlyOptionSelected(
                    allTabsOptionSelected = true,
                    privateTabsOptionSelected = false,
                )
                exitMenu()
            }
        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(secondHttpPageUrl.toUri()) {
                verifyPageContent("permission.site")
            }
            .openSearch {
                verifyTypedToolbarText(secondHttpsPageUrl, exists = true)
            }
            .dismissSearchBar {}
        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(insecureHttpPage.toUri()) {
                verifyPageContent(httpsOnlyErrorTitle)
                verifyPageContent(httpsOnlyErrorMessage)
                verifyPageContent(httpsOnlyErrorMessage2)
                verifyPageContent(httpsOnlyBackButton)
                clickPageObject(composeTestRule, itemContainingText(httpsOnlyBackButton))
                // Workaround required with Fission ON:
                // Click back twice to avoid https://bugzilla.mozilla.org/show_bug.cgi?id=1932498
                if (itemContainingText(httpsOnlyBackButton).waitForExists(waitingTimeShort)) {
                    clickPageObject(composeTestRule, itemContainingText(httpsOnlyBackButton))
                }
                verifyPageContent("permission.site")
            }
            .openThreeDotMenu {}
            .clickSettingsButton {}
            .openHttpsOnlyModeMenu {
                clickHttpsOnlyModeSwitch()
                verifyHttpsOnlyModeIsEnabled(false)
            }
            .goBack {
                verifySettingsOptionSummary("HTTPS-Only Mode", "Off")
                exitMenu()
            }
        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(insecureHttpPage.toUri()) {
                verifyPageContent("http.badssl.com")
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4024985
    @Test
    fun verifyHttpsFirstModeExceptionPersistenceTest() {
        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser("http://permission.site".toUri()) {
                verifyPageContent("permission.site")
            }
            .openSearch {
                verifyTypedToolbarText("http://permission.site/", exists = true)
            }
            .dismissSearchBar {}

        // Exception should persist
        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser("permission.site".toUri()) {
                verifyPageContent("permission.site")
            }
            .openSearch {
                verifyTypedToolbarText("http://permission.site/", exists = true)
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4024993
    @Test
    fun verifySecureConnectionByDefaultForSchemelessUrlsTest() {
        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser("permission.site".toUri()) {
                verifyPageContent("permission.site")
            }
            .openSearch {
                verifyTypedToolbarText("https://permission.site/", exists = true)
            }
    }
}
