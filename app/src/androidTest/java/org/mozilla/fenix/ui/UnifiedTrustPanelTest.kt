/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("DEPRECATION")

package org.mozilla.fenix.ui

import androidx.core.net.toUri
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.IntentReceiverActivity
import org.mozilla.fenix.customannotations.Converted
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.DataGenerationHelper.createCustomTabIntent
import org.mozilla.fenix.helpers.FenixTestRule
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestAssetHelper.enhancedTrackingProtectionAsset
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeLong
import org.mozilla.fenix.helpers.TestHelper.appContext
import org.mozilla.fenix.helpers.perf.DetectMemoryLeaksRule
import org.mozilla.fenix.ui.robots.browserScreen
import org.mozilla.fenix.ui.robots.customTabScreen
import org.mozilla.fenix.ui.robots.navigationToolbar
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2

class UnifiedTrustPanelTest {
    @get:Rule(order = 0)
    val fenixTestRule: FenixTestRule = FenixTestRule()

    private val mockWebServer get() = fenixTestRule.mockWebServer

    @get:Rule(order = 1)
    val composeTestRule =
        AndroidComposeTestRuleV2(
            HomeActivityIntentTestRule(),
        ) { it.activity }

    @get:Rule
    val intentReceiverActivityTestRule = ActivityTestRule(
        IntentReceiverActivity::class.java,
        true,
        false,
    )

    @get:Rule(order = 2)
    val memoryLeaksRule = DetectMemoryLeaksRule(composeTestRule = { composeTestRule })

    // TestRail: https://mozilla.testrail.io/index.php?/cases/view/3186718
    @Converted(
        replacedBy = ["org.mozilla.fenix.ui.efficiency.tests.UnifiedTrustPanelTest#verifySecurePageConnectionFromQuickSettingsWithNoTrackersTest"],
        bug = 2057420,
        since = "2026-07",
    )
    @SmokeTest
    @Test
    fun verifySecurePageConnectionFromQuickSettingsWithNoTrackersTest() {
        val firstPage = "https://mozilla-mobile.github.io/testapp"

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(firstPage.toUri()) {
            verifyPageContent("Lets test!")
        }
        navigationToolbar(composeTestRule) {
        }.openUnifiedTrustPanel {
            verifyUnifiedTrustPanelItems(
                composeTestRule = composeTestRule,
                webSite = "Test App",
                webSiteURL = firstPage.toUri().host.toString(),
                isTheWebSiteSecure = true,
                isEnhancedTrackingProtectionEnabled = true,
                isTrackerBlockingEnabled = true,
                areTrackersBlocked = false,
                )
            clickTheEnhancedTrackingProtectionOption(composeTestRule)
            verifyUnifiedTrustPanelItems(
                composeTestRule = composeTestRule,
                webSite = "Test App",
                webSiteURL = firstPage.toUri().host.toString(),
                isEnhancedTrackingProtectionEnabled = false,
                isTheWebSiteSecure = true,
                isTrackerBlockingEnabled = false,
                areTrackersBlocked = false,
            )
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3186721
    @Converted(
        replacedBy = ["org.mozilla.fenix.ui.efficiency.tests.UnifiedTrustPanelTest#verifyInsecurePageConnectionFromQuickSettingsWithTrackersTest"],
        bug = 2057420,
        since = "2026-07",
    )
    @SmokeTest
    @Test
    fun verifyInsecurePageConnectionFromQuickSettingsWithTrackersTest() {
        appContext.components.settings.setStrictETP()
        val genericPage = mockWebServer.getGenericAsset(1)
        val trackingProtectionPage = mockWebServer.enhancedTrackingProtectionAsset.url

        // browsing a generic page to allow GV to load on a fresh run
        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(genericPage.url) {
            verifyPageContent(genericPage.content)
        }
        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(trackingProtectionPage) {
            verifyTrackingProtectionWebContent("social blocked")
            verifyTrackingProtectionWebContent("ads blocked")
            verifyTrackingProtectionWebContent("analytics blocked")
            verifyTrackingProtectionWebContent("Fingerprinting blocked")
            verifyTrackingProtectionWebContent("Cryptomining blocked")
        }
        navigationToolbar(composeTestRule) {
        }.openUnifiedTrustPanel {
            verifyUnifiedTrustPanelItems(
                composeTestRule = composeTestRule,
                webSite = trackingProtectionPage.host.toString(),
                shouldWebSiteURLBeDisplayed = false,
                webSiteURL = trackingProtectionPage.host.toString(),
                isTheWebSiteSecure = false,
                isEnhancedTrackingProtectionEnabled = true,
                isTrackerBlockingEnabled = true,
                areTrackersBlocked = true,
            )
            clickTheEnhancedTrackingProtectionOption(composeTestRule)
            verifyUnifiedTrustPanelItems(
                composeTestRule = composeTestRule,
                webSite = trackingProtectionPage.host.toString(),
                shouldWebSiteURLBeDisplayed = false,
                webSiteURL = trackingProtectionPage.host.toString(),
                isTheWebSiteSecure = false,
                isEnhancedTrackingProtectionEnabled = false,
                isTrackerBlockingEnabled = false,
                areTrackersBlocked = false,
            )
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3186723
    @Converted(
        replacedBy = ["org.mozilla.fenix.ui.efficiency.tests.UnifiedTrustPanelTest#verifyClearCookiesAndSiteDataFromQuickSettingsTest"],
        bug = 2040857,
        since = "2026-06",
    )
    @SmokeTest
    @Test
    fun verifyClearCookiesAndSiteDataFromQuickSettingsTest() {
        val loginPage = "https://mozilla-mobile.github.io/testapp/loginForm"
        val originWebsite = "mozilla-mobile.github.io"

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(loginPage.toUri()) {
            waitForPageToLoad(waitingTimeLong)
        }
        navigationToolbar(composeTestRule) {
        }.openUnifiedTrustPanel {
            clickTheClearCookiesAndSiteDataButton(composeTestRule)
            verifyTheClearCookiesAndSiteDataDialog(composeTestRule, originWebsite)
        }
    }

    // TestRail: https://mozilla.testrail.io/index.php?/cases/view/3186719
    @Test
    fun verifySecurePageConnectionFromQuickSettingsWithTrackersTest() {
        appContext.components.settings.setStrictETP()
        val genericPage = mockWebServer.getGenericAsset(1)
        val trackingProtectionPage = "https://senglehardt.com/test/trackingprotection/test_pages/tracking_protection"

        // browsing a generic page to allow GV to load on a fresh run
        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(genericPage.url) {
            verifyPageContent(genericPage.content)
        }

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(trackingProtectionPage.toUri()) {
            waitForPageToLoad()
            verifyPageContent("Tracker Blocking")
            verifyPageContent("BLOCKED")
        }
        navigationToolbar(composeTestRule) {
        }.openUnifiedTrustPanel {
            verifyUnifiedTrustPanelItems(
                composeTestRule = composeTestRule,
                webSite = trackingProtectionPage.toUri().host.toString(),
                shouldWebSiteURLBeDisplayed = false,
                webSiteURL = trackingProtectionPage.toUri().host.toString(),
                isTheWebSiteSecure = true,
                isEnhancedTrackingProtectionEnabled = true,
                isTrackerBlockingEnabled = true,
                areTrackersBlocked = true,
            )
            clickTheEnhancedTrackingProtectionOption(composeTestRule)
            verifyUnifiedTrustPanelItems(
                composeTestRule = composeTestRule,
                webSite = trackingProtectionPage.toUri().host.toString(),
                shouldWebSiteURLBeDisplayed = false,
                webSiteURL = trackingProtectionPage.toUri().host.toString(),
                isEnhancedTrackingProtectionEnabled = false,
                isTheWebSiteSecure = true,
                isTrackerBlockingEnabled = false,
                areTrackersBlocked = true,
            )
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3186720
    @Test
    fun verifyInsecurePageConnectionFromQuickSettingsWithNoTrackersTest() {
        val genericPage = mockWebServer.getGenericAsset(1)

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(genericPage.url) {
            verifyPageContent(genericPage.content)
        }
        navigationToolbar(composeTestRule) {
        }.openUnifiedTrustPanel {
            verifyUnifiedTrustPanelItems(
                composeTestRule = composeTestRule,
                webSite = genericPage.title,
                webSiteURL = genericPage.url.host.toString(),
                isTheWebSiteSecure = false,
                isEnhancedTrackingProtectionEnabled = true,
                isTrackerBlockingEnabled = true,
                areTrackersBlocked = false,
            )
            clickTheEnhancedTrackingProtectionOption(composeTestRule)
            verifyUnifiedTrustPanelItems(
                composeTestRule = composeTestRule,
                webSite = genericPage.title,
                webSiteURL = genericPage.url.host.toString(),
                isTheWebSiteSecure = false,
                isEnhancedTrackingProtectionEnabled = false,
                isTrackerBlockingEnabled = false,
                areTrackersBlocked = false,
            )
        }
    }

    // TestRail: https://mozilla.testrail.io/index.php?/cases/view/3186709
    @Test
    fun verifySecurePageConnectionFromQuickSettingsWithNoTrackersInCustomTabsTest() {
        val customTabPage = "https://mozilla-mobile.github.io/testapp"

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                pageUrl = customTabPage,
            ),
        )

        browserScreen(composeTestRule) {
            verifyPageContent("Lets test!")
        }

        customTabScreen(composeTestRule) {
        }.openUnifiedTrustPanel {
            verifyUnifiedTrustPanelItems(
                composeTestRule = composeTestRule,
                webSite = "Test App",
                webSiteURL = customTabPage.toUri().host.toString(),
                isTheWebSiteSecure = true,
                isEnhancedTrackingProtectionEnabled = true,
                isTrackerBlockingEnabled = true,
                areTrackersBlocked = false,
            )
            clickTheEnhancedTrackingProtectionOption(composeTestRule)
            verifyUnifiedTrustPanelItems(
                composeTestRule = composeTestRule,
                webSite = "Test App",
                webSiteURL = customTabPage.toUri().host.toString(),
                isEnhancedTrackingProtectionEnabled = false,
                isTheWebSiteSecure = true,
                isTrackerBlockingEnabled = false,
                areTrackersBlocked = false,
            )
        }
    }

    // TestRail: https://mozilla.testrail.io/index.php?/cases/view/3186710
    @SmokeTest
    @Test
    fun verifySecurePageConnectionFromQuickSettingsWithTrackersInCustomTabsTest() {
        appContext.components.settings.setStrictETP()
        val genericPage = mockWebServer.getGenericAsset(1)
        val customTabPage = "https://senglehardt.com/test/trackingprotection/test_pages/tracking_protection"

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                pageUrl = genericPage.url.toString(),
            ),
        )

        browserScreen(composeTestRule) {
            verifyPageContent(genericPage.content)
        }

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                pageUrl = customTabPage,
            ),
        )

        customTabScreen(composeTestRule) {
            verifyCustomTabUrl(customTabPage.toUri().host.toString())
        }
        customTabScreen(composeTestRule) {
        }.openUnifiedTrustPanel {
            verifyUnifiedTrustPanelItems(
                composeTestRule = composeTestRule,
                webSite = customTabPage.toUri().host.toString(),
                shouldWebSiteURLBeDisplayed = false,
                webSiteURL = customTabPage.toUri().host.toString(),
                isTheWebSiteSecure = true,
                isEnhancedTrackingProtectionEnabled = true,
                isTrackerBlockingEnabled = true,
                areTrackersBlocked = true,
            )
            clickTheEnhancedTrackingProtectionOption(composeTestRule)
            verifyUnifiedTrustPanelItems(
                composeTestRule = composeTestRule,
                webSite = customTabPage.toUri().host.toString(),
                shouldWebSiteURLBeDisplayed = false,
                webSiteURL = customTabPage.toUri().host.toString(),
                isEnhancedTrackingProtectionEnabled = false,
                isTheWebSiteSecure = true,
                isTrackerBlockingEnabled = false,
                areTrackersBlocked = true,
            )
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3186711
    @Converted(
        replacedBy = ["org.mozilla.fenix.ui.efficiency.tests.UnifiedTrustPanelTest#verifyInsecurePageConnectionFromQuickSettingsWithNoTrackersInCustomTabsTest"],
        bug = 2057413,
        since = "2026-07",
    )
    @SmokeTest
    @Test
    fun verifyInsecurePageConnectionFromQuickSettingsWithNoTrackersInCustomTabsTest() {
        val customTabPage = mockWebServer.getGenericAsset(1)

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                pageUrl = customTabPage.url.toString(),
            ),
        )

        browserScreen(composeTestRule) {
            verifyPageContent(customTabPage.content)
        }

        customTabScreen(composeTestRule) {
        }.openUnifiedTrustPanel {
            verifyUnifiedTrustPanelItems(
                composeTestRule = composeTestRule,
                webSite = customTabPage.title,
                webSiteURL = customTabPage.url.host.toString(),
                isTheWebSiteSecure = false,
                isEnhancedTrackingProtectionEnabled = true,
                isTrackerBlockingEnabled = true,
                areTrackersBlocked = false,
            )
            clickTheEnhancedTrackingProtectionOption(composeTestRule)
            verifyUnifiedTrustPanelItems(
                composeTestRule = composeTestRule,
                webSite = customTabPage.title,
                webSiteURL = customTabPage.url.host.toString(),
                isTheWebSiteSecure = false,
                isEnhancedTrackingProtectionEnabled = false,
                isTrackerBlockingEnabled = false,
                areTrackersBlocked = false,
            )
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3186714
    @Converted(
        replacedBy = ["org.mozilla.fenix.ui.efficiency.tests.UnifiedTrustPanelTest#verifyClearCookiesAndSiteDataFromQuickSettingsInCustomTabsTest"],
        bug = 2057421,
        since = "2026-07",
    )
    @SmokeTest
    @Test
    fun verifyClearCookiesAndSiteDataFromQuickSettingsInCustomTabsTest() {
        val customTabPage = "https://mozilla-mobile.github.io/testapp/loginForm"
        val originWebsite = "mozilla-mobile.github.io"

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                pageUrl = customTabPage,
            ),
        )

        customTabScreen(composeTestRule) {
            waitForPageToLoad(waitingTimeLong)
        }.openUnifiedTrustPanel {
            clickTheClearCookiesAndSiteDataButton(composeTestRule)
            verifyTheClearCookiesAndSiteDataDialog(composeTestRule, originWebsite)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3186712
    @Test
    fun verifyInsecurePageConnectionFromQuickSettingsWithTrackersInCustomTabsTest() {
        appContext.components.settings.setStrictETP()

        val customTabPage = mockWebServer.enhancedTrackingProtectionAsset.url

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                pageUrl = customTabPage.toString(),
            ),
        )

        browserScreen(composeTestRule) {
            verifyTrackingProtectionWebContent("social blocked")
            verifyTrackingProtectionWebContent("ads blocked")
            verifyTrackingProtectionWebContent("analytics blocked")
            verifyTrackingProtectionWebContent("Fingerprinting blocked")
            verifyTrackingProtectionWebContent("Cryptomining blocked")
        }
        customTabScreen(composeTestRule) {
        }.openUnifiedTrustPanel {
            verifyUnifiedTrustPanelItems(
                composeTestRule = composeTestRule,
                webSite = customTabPage.host.toString(),
                shouldWebSiteURLBeDisplayed = false,
                webSiteURL = customTabPage.host.toString(),
                isTheWebSiteSecure = false,
                isEnhancedTrackingProtectionEnabled = true,
                isTrackerBlockingEnabled = true,
                areTrackersBlocked = true,
            )
            clickTheEnhancedTrackingProtectionOption(composeTestRule)
            verifyUnifiedTrustPanelItems(
                composeTestRule = composeTestRule,
                webSite = customTabPage.host.toString(),
                shouldWebSiteURLBeDisplayed = false,
                webSiteURL = customTabPage.host.toString(),
                isTheWebSiteSecure = false,
                isEnhancedTrackingProtectionEnabled = false,
                isTrackerBlockingEnabled = false,
                areTrackersBlocked = false,
            )
        }
    }
}
