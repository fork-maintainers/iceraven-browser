/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.webextension.MessageHandler
import mozilla.components.support.webextensions.WebExtensionSupport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.FenixTestRule
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeLong
import org.mozilla.fenix.helpers.TestHelper.appContext
import org.mozilla.fenix.helpers.TestWebExtensionInstaller
import org.mozilla.fenix.helpers.perf.DetectMemoryLeaksRule
import org.mozilla.fenix.ui.robots.browserScreen
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar
import org.mozilla.fenix.ui.robots.webExtensionActionPopup
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.test.assertNotNull
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2

/**
 * Regression tests for an extension in private browsing mode. To test, run:
 * mach gradle fenix:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.mozilla.fenix.ui.ExtensionPrivateBrowsingTest
 */
@RunWith(AndroidJUnit4::class)
class ExtensionPrivateBrowsingTest {

    companion object {
        val PRIVATE_BROWSING_TEST_EXTENSION = TestWebExtensionInstaller.BuiltInTestExtension(
            name = "Private Browsing Android Test",
            id = "{ed6cc6f0-0f66-4ec4-85a0-e82fd3f0b045}",
            uri = "resource://android/assets/extensions/private_browsing_test/",
        )

        val PRIVATE_BROWSING_NOT_ALLOWED_EXTENSION = TestWebExtensionInstaller.BuiltInTestExtension(
            name = "Private Browsing Not Allowed Android Test",
            id = "{b2f4a1c3-6d7e-4f8a-9b0c-1d2e3f4a5b6c}",
            uri = "resource://android/assets/extensions/private_browsing_not_allowed_test/",
        )
    }

    @get:Rule(order = 0)
    val fenixTestRule: FenixTestRule = FenixTestRule()

    @get:Rule(order = 1)
    val composeTestRule =
        AndroidComposeTestRuleV2(
            HomeActivityIntentTestRule.withDefaultSettingsOverrides(),
        ) { it.activity }

    @get:Rule(order = 2)
    val memoryLeaksRule = DetectMemoryLeaksRule(composeTestRule = { composeTestRule })

    @Test
    fun tabsCreateAndActionPopupInPrivateBrowsingMode() {
        // Regression test for bug 2041306: a call to browser.tabs.create()
        // when private browsing mode is enabled without any tabs in the home
        // screen should open a tab in private browsing mode (PBM).
        homeScreen(composeTestRule) {
        }.togglePrivateBrowsingMode()

        // Built-in extensions are automatically granted access to PBM.
        // (users installing extenssions need to check a box to opt in.)
        TestWebExtensionInstaller.ensureBuiltInExtensionInstalled(PRIVATE_BROWSING_TEST_EXTENSION)

        // The extension calls browser.tabs.create() to open page.html, which
        // sets the title to "page-pbm=<pbm status as reported by engine>".
        composeTestRule.waitUntil(waitingTimeLong) {
            appContext.components.core.store.state.selectedTab
                ?.content?.title?.startsWith("page-pbm=") == true
        }

        val tab = appContext.components.core.store.state.selectedTab
        assertNotNull(tab, "Expected the extension to have opened a tab")
        assertTrue("Created tab must be marked private", tab.content.private)
        assertEquals("page-pbm=true", tab.content.title)

        // The extension action button only shows up when there is a tab.
        // Since we have a tab anyway, let's also test the action popup here.
        // Regression test for bug 1748526: action popup in private browsing
        // mode should also be in private browsing mode.

        // browser.tabs.create() does not render when the (private browsing)
        // home screen is in the front (bug 1810841), so we need to put the
        // tab we just created in the foreground first.
        homeScreen(composeTestRule) {
        }.openTabDrawer {
        }.openTab("page-pbm=true") { // This is the tab from above.
        }.openThreeDotMenu {
            // The extension button is only shown when a tab is in the
            // foreground. Since we are now showing a real tab instead of the
            // home screen, we can now click the button.
            clickExtensionActionButton(PRIVATE_BROWSING_TEST_EXTENSION.name)
        }

        webExtensionActionPopup(composeTestRule) {
            verifyPopupContent("popup-pbm=true")
        }

        TestWebExtensionInstaller.uninstallExtension(PRIVATE_BROWSING_TEST_EXTENSION)
    }

    @Test
    fun tabsCreateAndActionPopupInRegularBrowsingMode() {
        // Sanity check that the other test did not pass trivially due to the
        // content being unconditionally in private browsing mode.

        TestWebExtensionInstaller.ensureBuiltInExtensionInstalled(PRIVATE_BROWSING_TEST_EXTENSION)

        // The extension calls browser.tabs.create() to open page.html, which
        // sets the title to "page-pbm=<pbm status as reported by engine>".
        composeTestRule.waitUntil(waitingTimeLong) {
            appContext.components.core.store.state.selectedTab
                ?.content?.title?.startsWith("page-pbm=") == true
        }

        val tab = appContext.components.core.store.state.selectedTab
        assertNotNull(tab, "Expected the extension to have opened a tab")
        assertFalse("Created tab is not private", tab.content.private)
        assertEquals("page-pbm=false", tab.content.title)

        // The extension action button only shows up when there is a tab.
        // Since we have a tab anyway, let's also test the action popup here.

        // browser.tabs.create() does not render when the (private browsing)
        // home screen is in the front (bug 1810841), so we need to put the
        // tab we just created in the foreground first.
        homeScreen(composeTestRule) {
        }.openTabDrawer {
        }.openTab("page-pbm=false") { // This is the tab from above.
        }.openThreeDotMenu {
            // The extension button is only shown when a tab is in the
            // foreground. Since we are now showing a real tab instead of the
            // home screen, we can now click the button.
            clickExtensionActionButton(PRIVATE_BROWSING_TEST_EXTENSION.name)
        }

        webExtensionActionPopup(composeTestRule) {
            verifyPopupContent("popup-pbm=false")
        }

        TestWebExtensionInstaller.uninstallExtension(PRIVATE_BROWSING_TEST_EXTENSION)
    }

    @Test
    fun tabsCreateRejectsInPrivateBrowsingModeWhenNotAllowed() {
        // Verifies that browser.tabs.create() rejects when in private browsing
        // with an extension that is not allowed access to private browsing.

        homeScreen(composeTestRule) {
        }.togglePrivateBrowsingMode()

        // Built-in extensions have private browsing access by default, but we
        // opt out of it with incognito:not_allowed in its manifest.json.
        TestWebExtensionInstaller.ensureBuiltInExtensionInstalled(PRIVATE_BROWSING_NOT_ALLOWED_EXTENSION)

        val extension = WebExtensionSupport.installedExtensions[PRIVATE_BROWSING_NOT_ALLOWED_EXTENSION.id]
        assertNotNull(extension, "Expected the extension to be installed")

        val report = CompletableFuture<String>()
        composeTestRule.activity.runOnUiThread {
            extension.registerBackgroundMessageHandler(
                "testresult",
                object : MessageHandler {
                    override fun onMessage(message: Any, source: EngineSession?): Any? {
                        report.complete(message.toString())
                        return null
                    }
                },
            )
        }

        val outcome = report.get(waitingTimeLong, TimeUnit.MILLISECONDS)
        assertEquals("tabs.create rejected: Cannot create new tab", outcome)

        TestWebExtensionInstaller.uninstallExtension(PRIVATE_BROWSING_NOT_ALLOWED_EXTENSION)
    }
}
