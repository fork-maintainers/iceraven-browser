/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.session

import android.content.ComponentName
import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.privatemode.notification.AbstractPrivateNotificationService.Companion.ACTION_ERASE
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.HomeActivity.Companion.PRIVATE_BROWSING_MODE
import org.mozilla.fenix.components.usecases.FenixBrowserUseCases
import org.mozilla.fenix.ext.components
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class PrivateNotificationServiceTest {

    private lateinit var controller: ServiceController<PrivateNotificationService>

    @Before
    fun setup() {
        controller = Robolectric.buildService(
            PrivateNotificationService::class.java,
            Intent(ACTION_ERASE),
        )
    }

    @Test
    fun `service opens home activity in private mode if app is in private mode`() {
        val selectedPrivateTab = createTab("https://mozilla.org", private = true)
        val store = BrowserStore(
            BrowserState(tabs = listOf(selectedPrivateTab), selectedTabId = selectedPrivateTab.id),
        )

        every { testContext.components.core.store } returns store
        every { testContext.components.useCases.tabsUseCases } returns TabsUseCases(store)
        every { testContext.components.settings.enableHomepageAsNewTab } returns false

        val service = shadowOf(controller.get())
        controller.startCommand(0, 0)

        val intent = service.nextStartedActivity
        assertNotNull(intent)
        assertEquals(ComponentName(testContext, HomeActivity::class.java), intent.component)
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK, intent.flags)
        assertEquals(true, intent.extras?.getBoolean(PRIVATE_BROWSING_MODE))
    }

    @Test
    fun `WHEN homepage as a new tab is enabled AND app is in private mode THEN erasing private tabs reopens a private homepage tab and opens the home activity in private mode`() {
        val selectedPrivateTab = createTab("https://mozilla.org", private = true)
        val store = BrowserStore(
            BrowserState(tabs = listOf(selectedPrivateTab), selectedTabId = selectedPrivateTab.id),
        )
        val fenixBrowserUseCases = mockk<FenixBrowserUseCases>(relaxed = true)

        every { testContext.components.core.store } returns store
        every { testContext.components.useCases.tabsUseCases } returns TabsUseCases(store)
        every { testContext.components.useCases.fenixBrowserUseCases } returns fenixBrowserUseCases
        every { testContext.components.settings.enableHomepageAsNewTab } returns true

        val service = shadowOf(controller.get())
        controller.startCommand(0, 0)

        verify { fenixBrowserUseCases.addNewHomepageTab(private = true) }

        val intent = service.nextStartedActivity
        assertNotNull(intent)
        assertEquals(ComponentName(testContext, HomeActivity::class.java), intent.component)
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK, intent.flags)
        assertEquals(true, intent.extras?.getBoolean(PRIVATE_BROWSING_MODE))
    }

    @Test
    fun `WHEN homepage as a new tab is enabled AND app is in normal mode THEN erasing private tabs does not reopen a homepage tab or open an activity`() {
        val selectedTab = createTab("https://mozilla.org", private = false)
        val store = BrowserStore(
            BrowserState(tabs = listOf(selectedTab), selectedTabId = selectedTab.id),
        )
        val fenixBrowserUseCases = mockk<FenixBrowserUseCases>(relaxed = true)

        every { testContext.components.core.store } returns store
        every { testContext.components.useCases.tabsUseCases } returns TabsUseCases(store)
        every { testContext.components.useCases.fenixBrowserUseCases } returns fenixBrowserUseCases
        every { testContext.components.settings.enableHomepageAsNewTab } returns true

        val service = shadowOf(controller.get())
        controller.startCommand(0, 0)

        verify(exactly = 0) { fenixBrowserUseCases.addNewHomepageTab(private = any()) }
        assertNull(service.nextStartedActivity)
    }

    @Test
    fun `service starts no activity if app is in normal mode`() {
        val selectedPrivateTab = createTab("https://mozilla.org", private = false)
        val store = BrowserStore(
            BrowserState(tabs = listOf(selectedPrivateTab), selectedTabId = selectedPrivateTab.id),
        )

        every { testContext.components.core.store } returns store
        every { testContext.components.useCases.tabsUseCases } returns TabsUseCases(store)
        every { testContext.components.settings.enableHomepageAsNewTab } returns false

        val service = shadowOf(controller.get())
        controller.startCommand(0, 0)

        assertNull(service.nextStartedActivity)
    }
}
