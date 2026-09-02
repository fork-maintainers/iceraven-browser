/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import android.app.Instrumentation
import android.content.Intent
import androidx.core.net.toUri
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasDataString
import org.hamcrest.Matchers.equalTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.customannotations.Converted
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.AppAndSystemHelper.assertNativeAppOpens
import org.mozilla.fenix.helpers.AppAndSystemHelper.assertYoutubeAppOpens
import org.mozilla.fenix.helpers.Constants
import org.mozilla.fenix.helpers.FenixTestRule
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.MatcherHelper.itemContainingText
import org.mozilla.fenix.helpers.OpenLinksInApp
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.TestAssetHelper.appLinksRedirectAsset
import org.mozilla.fenix.helpers.TestAssetHelper.externalLinksAsset
import org.mozilla.fenix.helpers.TestHelper
import org.mozilla.fenix.helpers.TestHelper.exitMenu
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.helpers.TestHelper.waitForAppWindowToBeUpdated
import org.mozilla.fenix.helpers.perf.DetectMemoryLeaksRule
import org.mozilla.fenix.ui.robots.clickPageObject
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2

/**
 *  Tests for verifying the advanced section in Settings
 *
 */

class SettingsAdvancedTest {
    private val intentSchemaUrlLink = itemContainingText("Intent schema link")
    private val intentSchemeWithExampleAppLink = itemContainingText("Example app link")

    private val formRedirectLink = itemContainingText("Telephone post navigation link")

    private val youtubeUrlLink = itemContainingText("Youtube link")
    private val youtubeSchemaUrlLink = itemContainingText("Youtube schema link")

    private val playStoreLink = itemContainingText("Playstore link")
    private val playStoreUrl = "play.google.com"

    private val phoneUrlLink = itemContainingText("Telephone link")
    private val phoneSchemaLink = "tel://1234567890"
    private val phoneWithFallbackLink = itemContainingText("Telephone with fallback URL")

    private val linkWithAndroidFallbackLink = itemContainingText("Link with android fallback link")
    private val linkWithFallbackLink = itemContainingText("Link with fallback link")
    private val linkWithBrowserFallbackLink = itemContainingText("Link with browser fallback link")

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

    lateinit var externalLinksPage: TestAssetHelper.TestAsset

    @Before
    fun setUp() {
        externalLinksPage = mockWebServer.externalLinksAsset
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2092699
    // Walks through settings menu and sub-menus to ensure all items are present
    @Test
    fun verifyAdvancedSettingsSectionItemsTest() {
        // ADVANCED
        homeScreen(composeTestRule) {
        }.openThreeDotMenu {
        }.clickSettingsButton {
            verifySettingsToolbar()
            verifyAdvancedHeading()
            verifyAddons()
            verifyOpenLinksInAppsButton()
            verifySettingsOptionSummary("Open links in apps", "Ask before opening")
            verifyDownloadsButton()
            verifyLeakCanaryButton()
            // LeakCanary is disabled in UI tests.
            // See BuildConfig.LEAKCANARY.
            verifyLeakCanaryToggle(false)
            verifyRemoteDebuggingButton()
            verifyRemoteDebuggingToggle(false)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2121046
    // Assumes Youtube is installed and enabled
    @Converted(
        replacedBy = ["org.mozilla.fenix.ui.efficiency.tests.SettingsOpenLinksInAppsTest#askBeforeOpeningOpenLinkInAppTest"],
        bug = 2061722,
        since = "2026-08",
        notes = "Legacy verifyUrl(\"play.google.com\") was a no-op (BrowserRobot.verifyUrl swallows its timeout); " +
            "the port asserts the open-in-app prompt instead, which is the real behavior under Ask.",
    )
    @SmokeTest
    @Test
    fun askBeforeOpeningOpenLinkInAppTest() {
        composeTestRule.activityRule.applySettingsExceptions {
            it.openLinksInExternalApp = OpenLinksInApp.ASK
        }

        exitMenu()

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(composeTestRule, playStoreLink)
            verifyUrl(playStoreUrl)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2121052
    // Assumes Youtube is installed and enabled
    @Test
    fun privateBrowsingAskBeforeOpeningOpenLinkInAppTest() {
        composeTestRule.activityRule.applySettingsExceptions {
            it.openLinksInExternalApp = OpenLinksInApp.ASK
        }

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(composeTestRule, playStoreLink)
            verifyUrl(playStoreUrl)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2121045
    // Assumes Youtube is installed and enabled
    @Converted(
        replacedBy = ["org.mozilla.fenix.ui.efficiency.tests.SettingsOpenLinksInAppsTest#askBeforeOpeningLinkInAppCancelTest"],
        bug = 2061722,
        since = "2026-08",
    )
    @SmokeTest
    @Test
    fun askBeforeOpeningLinkInAppCancelTest() {
        composeTestRule.activityRule.applySettingsExceptions {
            it.openLinksInExternalApp = OpenLinksInApp.ASK
        }

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(composeTestRule, youtubeSchemaUrlLink)
            verifyOpenLinkInAnotherAppPrompt(appName = "YouTube")
            clickPageObject(composeTestRule, itemContainingText("Stay in"))
            verifyUrl(externalLinksPage.url.toString())
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2288347
    // Assumes Youtube is installed and enabled
    @Converted(
        replacedBy = ["org.mozilla.fenix.ui.efficiency.tests.SettingsOpenLinksInAppsTest#askBeforeOpeningLinkInAppOpenTest"],
        bug = 2061722,
        since = "2026-08",
    )
    @SmokeTest
    @Test
    fun askBeforeOpeningLinkInAppOpenTest() {
        composeTestRule.activityRule.applySettingsExceptions {
            it.openLinksInExternalApp = OpenLinksInApp.ASK
        }

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(composeTestRule, youtubeSchemaUrlLink)
            verifyOpenLinkInAnotherAppPrompt(appName = "YouTube")
            waitForAppWindowToBeUpdated()
            clickPageObject(composeTestRule, itemContainingText("Open in App"))
            mDevice.waitForIdle()
            assertYoutubeAppOpens()
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2121051
    // Assumes Youtube is installed and enabled
    @Test
    fun privateBrowsingAskBeforeOpeningLinkInAppCancelTest() {
        composeTestRule.activityRule.applySettingsExceptions {
            it.openLinksInExternalApp = OpenLinksInApp.ASK
        }

        homeScreen(composeTestRule) {
        }.togglePrivateBrowsingMode()

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(composeTestRule, youtubeSchemaUrlLink)
            verifyPrivateBrowsingOpenLinkInAnotherAppPrompt(
                appName = "YouTube",
                url = "youtube",
                pageObject = youtubeSchemaUrlLink,
            )
            clickPageObject(composeTestRule, itemContainingText("Stay in"))
            verifyUrl(externalLinksPage.url.toString())
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2288350
    // Assumes Youtube is installed and enabled
    @Test
    fun privateBrowsingAskBeforeOpeningLinkInAppOpenTest() {
        composeTestRule.activityRule.applySettingsExceptions {
            it.openLinksInExternalApp = OpenLinksInApp.ASK
        }

        homeScreen(composeTestRule) {
        }.togglePrivateBrowsingMode()

        exitMenu()

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(composeTestRule, youtubeSchemaUrlLink)
            verifyPrivateBrowsingOpenLinkInAnotherAppPrompt(
                appName = "YouTube",
                url = "youtube",
                pageObject = youtubeSchemaUrlLink,
            )
            waitForAppWindowToBeUpdated()
            clickPageObject(composeTestRule, itemContainingText("Open in App"))
            mDevice.waitForIdle()
            assertYoutubeAppOpens()
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/1058618
    // Assumes Youtube is installed and enabled
    @Test
    fun alwaysOpenLinkInAppTest() {
        composeTestRule.activityRule.applySettingsExceptions {
            it.openLinksInExternalApp = OpenLinksInApp.ALWAYS
        }

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(composeTestRule, youtubeSchemaUrlLink)
            mDevice.waitForIdle()
            assertYoutubeAppOpens()
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/1058617
    @Test
    fun dismissOpenLinksInAppCFRTest() {
        composeTestRule.activityRule.applySettingsExceptions {
            it.isOpenInAppBannerEnabled = true
            it.openLinksInExternalApp = OpenLinksInApp.NEVER
        }

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser("https://m.youtube.com/".toUri()) {
            verifyPageContent("youtube")
            verifyOpenLinksInAppsCFRExists(true)
            clickOpenLinksInAppsDismissCFRButton()
            verifyOpenLinksInAppsCFRExists(false)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2288331
    @Test
    fun goToSettingsFromOpenLinksInAppCFRTest() {
        composeTestRule.activityRule.applySettingsExceptions {
            it.isOpenInAppBannerEnabled = true
            it.openLinksInExternalApp = OpenLinksInApp.NEVER
        }

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser("https://m.youtube.com/".toUri()) {
            verifyPageContent("youtube")
            verifyOpenLinksInAppsCFRExists(true)
        }.clickOpenLinksInAppsGoToSettingsCFRButton {
            verifyOpenLinksInAppsButton()
        }
    }

    /**
     * User setting: Never
     * For an https YouTube link, no external-app prompt is shown.
     * The page loads directly in-browser (verify “youtube.com”).
     * https://m.youtube.com/user/mozilla
     */
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2121046
    @Test
    fun neverOpenLinkInAppTest() {
        val externalLinksPage = mockWebServer.appLinksRedirectAsset

        composeTestRule.activityRule.applySettingsExceptions {
            it.openLinksInExternalApp = OpenLinksInApp.NEVER
        }

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(composeTestRule, youtubeUrlLink)
            mDevice.waitForIdle()
            verifyOpenLinkInAnotherAppPromptIsNotShown()
            verifyUrl("youtube.com")
        }
    }

    /**
     * User setting: Always
     * For tel: links, no prompt is shown.
     * The native Phone app opens automatically with the correct URI.
     * tel://1234567890
     */
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4026484
    @Test
    fun verifyTheAlwaysOpenPhoneLinkInAppTest() {
        val externalLinksPage = mockWebServer.appLinksRedirectAsset

        composeTestRule.activityRule.applySettingsExceptions {
            it.openLinksInExternalApp = OpenLinksInApp.ALWAYS
        }

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(composeTestRule, phoneUrlLink)
            mDevice.waitForIdle()
            assertNativeAppOpens(composeTestRule, Constants.PackageName.PHONE_APP, phoneSchemaLink)
        }
    }

    /**
     * User setting: Ask
     * Verifies that the “Open in Phone” prompt appears when tapping a tel: link.
     * tel://1234567890
     */
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4026485
    @Test
    fun askBeforeOpeningPhoneLinkPromptTest() {
        val externalLinksPage = mockWebServer.appLinksRedirectAsset

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(composeTestRule, phoneUrlLink)
            verifyOpenLinkInAnotherAppPrompt(appName = "Phone")
        }
    }

    /**
     * User setting: Ask
     * Clicking a tel: link triggers the Phone prompt.
     * Tapping “Cancel” keeps the user on the same page.
     * tel://1234567890
     */
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4026486
    @Test
    fun askBeforeOpeningLinkInAppPhoneCancelTest() {
        val externalLinksPage = mockWebServer.appLinksRedirectAsset

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(composeTestRule, phoneUrlLink)
            verifyOpenLinkInAnotherAppPrompt(appName = "Phone")
            clickPageObject(composeTestRule, itemContainingText("Stay in"))
            mDevice.waitForIdle()
            verifyUrl(externalLinksPage.url.toString())
        }
    }

    /**
     * User setting: Ask
     * When prompted for a tel: link and user taps “Open”,
     * the Phone app launches, then control returns to the same browser page.
     * tel://1234567890
     */
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4026487
    @Test
    fun askBeforeOpeningPhoneLinkInAcceptTest() {
        val externalLinksPage = mockWebServer.appLinksRedirectAsset

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(composeTestRule, phoneUrlLink)
            verifyOpenLinkInAnotherAppPrompt(appName = "Phone")
            clickPageObject(composeTestRule, itemContainingText("Open in App"))
            mDevice.waitForIdle()
            assertNativeAppOpens(composeTestRule, Constants.PackageName.PHONE_APP, phoneSchemaLink)
            mDevice.waitForIdle()
            verifyUrl(externalLinksPage.url.toString())
        }
    }

    /**
     * User setting: Ask
     * Verify the "Always open links in apps" checkbox appears in the prompt
     * when the setting is "Ask" and the tab is not private.
     * tel://1234567890
     */
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4026488
    @Test
    fun askBeforeOpeningLinkCheckboxVisibleTest() {
        val externalLinksPage = mockWebServer.appLinksRedirectAsset

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(composeTestRule, phoneUrlLink)
            verifyOpenLinkInAnotherAppPrompt(appName = "Phone")
            verifyAppLinksPromptCheckbox(exists = true)
            clickPageObject(composeTestRule, itemContainingText("Stay in"))
        }
    }

    /**
     * User setting: Ask
     * Verify the "Always open links in apps" checkbox is NOT shown when the
     * app-links prompt appears from a private browsing tab.
     * vnd.youtube://@Mozilla
     */
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4026490
    @Test
    fun askBeforeOpeningLinkInPrivateTabNoCheckboxTest() {
        val externalLinksPage = mockWebServer.appLinksRedirectAsset

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
        }.openTabDrawer(composeTestRule) {
        }.toggleToPrivateTabs {
        }.openNewTab {
        }.submitQuery(externalLinksPage.url.toString()) {
            clickPageObject(composeTestRule, youtubeSchemaUrlLink)
            verifyOpenLinkInAnotherAppPrompt(appName = "YouTube")
            verifyAppLinksPromptCheckbox(exists = false)
            clickPageObject(composeTestRule, itemContainingText("Stay in"))
        }
    }

    /**
     * User setting: Ask
     * Tests that when opening a youtube:// scheme link under “Ask”, the app prompt appears.
     * After tapping “Cancel”, the browser stays on the same page (no external app opened).
     * vnd.youtube://@Mozilla
     */
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4031563
    @Test
    fun askBeforeOpeningLinkInAppYoutubeSchemeCancelTest() {
        val externalLinksPage = mockWebServer.appLinksRedirectAsset

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(composeTestRule, youtubeSchemaUrlLink)
            verifyOpenLinkInAnotherAppPrompt(appName = "YouTube")
            clickPageObject(composeTestRule, itemContainingText("Stay in"))
            mDevice.waitForIdle()
            verifyUrl(externalLinksPage.url.toString())
        }
    }

    /**
     * User setting: Ask
     * After canceling once for youtube://, tapping the same link again in the same tab
     * should not show the prompt again. The browser remains on the test page.
     * vnd.youtube://@Mozilla
     */
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4031564
    @Test
    fun askBeforeOpeningLinkInAppYoutubeSchemeCancelMultiTapTest() {
        val externalLinksPage = mockWebServer.appLinksRedirectAsset

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(composeTestRule, youtubeSchemaUrlLink)
            verifyOpenLinkInAnotherAppPrompt(appName = "YouTube")
            clickPageObject(composeTestRule, itemContainingText("Stay in"))
            mDevice.waitForIdle()
            verifyUrl(externalLinksPage.url.toString())
            clickPageObject(composeTestRule, youtubeSchemaUrlLink)
            mDevice.waitForIdle()
            verifyUrl(externalLinksPage.url.toString())
            verifyOpenLinkInAnotherAppPromptIsNotShown()
            mDevice.waitForIdle()
            verifyUrl(externalLinksPage.url.toString())
            verifyOpenLinkInAnotherAppPromptIsNotShown()
        }
    }

    /**
     * User setting: Ask
     * Canceling a youtube:// link prompt affects only the current tab.
     * In a new tab, the same link still shows the prompt.
     * vnd.youtube://@Mozilla
     */
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4032711
    @Test
    fun askBeforeOpeningLinkInAppYoutubeSchemeCancelOnlyAffectCurrentTabTest() {
        val externalLinksPage = mockWebServer.appLinksRedirectAsset

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(composeTestRule, youtubeSchemaUrlLink)
            verifyOpenLinkInAnotherAppPrompt(appName = "YouTube")
            clickPageObject(composeTestRule, itemContainingText("Stay in"))
            mDevice.waitForIdle()
            verifyUrl(externalLinksPage.url.toString())
        }.openTabDrawer(composeTestRule) {
        }.openNewTab {
        }.submitQuery(externalLinksPage.url.toString()) {
            clickPageObject(composeTestRule, youtubeSchemaUrlLink)
            verifyOpenLinkInAnotherAppPrompt(appName = "YouTube")
            clickPageObject(composeTestRule, itemContainingText("Stay in"))
            mDevice.waitForIdle()
            verifyUrl(externalLinksPage.url.toString())
        }
    }

    /**
     * User setting: Never
     * For a youtube:// scheme link, the app prompt still appears.
     * After “Cancel”, the browser stays on the same page.
     * vnd.youtube://@Mozilla
     */
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4032712
    @Test
    fun neverOpeningLinkInAppYoutubeSchemeCancelTest() {
        val externalLinksPage = mockWebServer.appLinksRedirectAsset

        composeTestRule.activityRule.applySettingsExceptions {
            it.openLinksInExternalApp = OpenLinksInApp.NEVER
        }

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(composeTestRule, youtubeSchemaUrlLink)
            verifyOpenLinkInAnotherAppPrompt(appName = "YouTube")
            clickPageObject(composeTestRule, itemContainingText("Stay in"))
            mDevice.waitForIdle()
            verifyUrl(externalLinksPage.url.toString())
        }
    }

    /**
     * User setting: Ask
     * Clicking an intent:// link without corresponding app should not trigger the
     * external-app prompt. The user stays on the same page.
     * intent://com.example.app
     */
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4032713
    @Test
    fun askBeforeOpeningLinkWithIntentSchemeTest() {
        val externalLinksPage = mockWebServer.appLinksRedirectAsset

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(composeTestRule, intentSchemaUrlLink)
            mDevice.waitForIdle()
            verifyOpenLinkInAnotherAppPromptIsNotShown()
            verifyUrl(externalLinksPage.url.toString())
        }
    }

    /**
     * User setting: Ask
     * Form redirect leading to a tel: link should trigger the Phone app prompt.
     * <form action="tel://1234567890" method="POST"></form>
     */
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4032714
    @Test
    fun appLinksNewTabRedirectAskTest() {
        val externalLinksPage = mockWebServer.appLinksRedirectAsset

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(composeTestRule, formRedirectLink)
            verifyOpenLinkInAnotherAppPrompt(appName = "Phone")
        }
    }

    /**
     * User setting: Always
     * Form redirect leading to a tel: Phone app launches directly with no prompt.
     * <form action="tel://1234567890" method="POST"></form>
     */
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4032727
    @Test
    fun appLinksNewTabRedirectAlwaysTest() {
        val externalLinksPage = mockWebServer.appLinksRedirectAsset

        composeTestRule.activityRule.applySettingsExceptions {
            it.openLinksInExternalApp = OpenLinksInApp.ALWAYS
        }

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(composeTestRule, formRedirectLink)
            mDevice.waitForIdle()
            assertNativeAppOpens(composeTestRule, Constants.PackageName.PHONE_APP, phoneSchemaLink)
        }
    }

    /**
     * User setting: Never
     * Form redirect leading to a tel: prompt is still shown for the tel: link.
     * <form action="tel://1234567890" method="POST"></form>
     */
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4032728
    @Test
    fun appLinksNewTabRedirectNeverTest() {
        val externalLinksPage = mockWebServer.appLinksRedirectAsset

        composeTestRule.activityRule.applySettingsExceptions {
            it.openLinksInExternalApp = OpenLinksInApp.NEVER
        }

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(composeTestRule, formRedirectLink)
            verifyOpenLinkInAnotherAppPrompt(appName = "Phone")
        }
    }

    /**
     * User setting: Ask
     * When prompted for a external application not installed: user taps “Open”,
     * a marketing intent should be used.
     * intent://com.example.app#Intent;package=com.example.app;end
     */
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4032729
    @Test
    fun marketingIntentWhenOpeningLinkWithoutApp() {
        val externalLinksPage = mockWebServer.appLinksRedirectAsset

        // Use ACTION_DIAL as a non-ACTION_VIEW intent to verify that the marketing flow always
        // launches with ACTION_VIEW instead of reusing the original intent action.
        intending(hasAction(Intent.ACTION_DIAL)).respondWith(
            Instrumentation.ActivityResult(
                0,
                null,
            ),
        )

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(composeTestRule, intentSchemeWithExampleAppLink)
            clickPageObject(composeTestRule, itemContainingText("Open in App"))
            mDevice.waitForIdle()
            intended(hasAction(Intent.ACTION_VIEW))
            intended(hasDataString(equalTo("market://details?id=com.example.app")))
        }
    }

    /**
     * User setting: Ask
     * For a tel: link with a browser fallback, tapping “Cancel” navigates
     * to the fallback URL (mozilla.org).
     * intent://1234567890#Intent;scheme=tel;S.browser_fallback_url=https://www.mozilla.org;end;
     */
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4032730
    @Test
    fun appLinksBrowserFallbackURLTest() {
        val externalLinksPage = mockWebServer.appLinksRedirectAsset

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(composeTestRule, phoneWithFallbackLink)
            verifyOpenLinkInAnotherAppPrompt(appName = "Phone")
            clickPageObject(composeTestRule, itemContainingText("Stay in"))
            mDevice.waitForIdle()
            verifyUrl("mozilla.org")
        }
    }

    /**
     * User setting: Ask
     * Link with supported scheme will never load the "afl" fallback URL
     * https://mozilla.org/?afl=https://youtube.com
     */
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4032731
    @Test
    fun linkWithAndroidFallbackLinkTest() {
        val externalLinksPage = mockWebServer.appLinksRedirectAsset

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            verifyUrl(externalLinksPage.url.toString())
            clickPageObject(composeTestRule, linkWithAndroidFallbackLink)
            waitForPageToLoad()
            verifyUrl("mozilla.org")
        }
    }

    /**
     * User setting: Ask
     * Link with supported scheme will never load the "link" fallback URL
     * https://mozilla.org/?link=https://youtube.com
     */
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4032732
    @Test
    fun linkWithFallbackLinkTest() {
        val externalLinksPage = mockWebServer.appLinksRedirectAsset

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(composeTestRule, linkWithFallbackLink)
            mDevice.waitForIdle()
            verifyUrl("mozilla.org")
        }
    }

    /**
     * User setting: Ask
     * Link with supported scheme will never load the "S.browser_fallback_url" fallback URL
     * https://mozilla.org/?S.browser_fallback_url=https://youtube.com
     */
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4032733
    @Test
    fun linkWithBrowserFallbackLinkTest() {
        val externalLinksPage = mockWebServer.appLinksRedirectAsset

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(composeTestRule, linkWithBrowserFallbackLink)
            mDevice.waitForIdle()
            verifyUrl("mozilla.org")
        }
    }
}
