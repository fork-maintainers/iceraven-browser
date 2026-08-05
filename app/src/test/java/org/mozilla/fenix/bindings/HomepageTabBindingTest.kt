/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.bindings

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager
import org.mozilla.fenix.components.HomepageAsANewTabPreferencesRepository
import org.mozilla.fenix.components.usecases.FenixBrowserUseCases

@RunWith(AndroidJUnit4::class)
class HomepageTabBindingTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fenixBrowserUseCases: FenixBrowserUseCases
    private lateinit var repository: HomepageAsANewTabPreferencesRepository

    @Before
    fun setUp() {
        fenixBrowserUseCases = mockk(relaxed = true)
        repository = mockk(relaxed = true)

        every { repository.getHomepageAsANewTabEnabled() } returns true
    }

    @Test
    fun `GIVEN restore is complete and no tabs on private mode startup WHEN the binding starts THEN add a private homepage tab`() = runTest(testDispatcher) {
        val browserStore = createStore(restoreComplete = true)

        createBinding(browserStore, mode = BrowsingMode.Private).start()
        testDispatcher.scheduler.advanceUntilIdle()

        verify { fenixBrowserUseCases.addNewHomepageTab(private = true) }
    }

    @Test
    fun `GIVEN restore is complete and no tabs on normal mode startup WHEN the binding starts THEN add a homepage tab`() = runTest(testDispatcher) {
        val browserStore = createStore(restoreComplete = true)

        createBinding(browserStore, mode = BrowsingMode.Normal).start()
        testDispatcher.scheduler.advanceUntilIdle()

        verify { fenixBrowserUseCases.addNewHomepageTab(private = false) }
    }

    @Test
    fun `GIVEN restore is not complete WHEN the binding starts THEN do not add a homepage tab`() = runTest(testDispatcher) {
        val browserStore = createStore(restoreComplete = false)

        createBinding(browserStore, mode = BrowsingMode.Private).start()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 0) { fenixBrowserUseCases.addNewHomepageTab(private = any()) }
    }

    @Test
    fun `GIVEN normal tabs on private mode startup WHEN the binding starts THEN add a private homepage tab`() = runTest(testDispatcher) {
        val browserStore = createStore(
            restoreComplete = true,
            tabs = listOf(createTab(url = "https://www.mozilla.org")),
        )

        createBinding(browserStore, BrowsingMode.Private).start()
        testDispatcher.scheduler.advanceUntilIdle()

        verify { fenixBrowserUseCases.addNewHomepageTab(private = true) }
    }

    @Test
    fun `GIVEN homepage as a new tab is disabled WHEN the binding starts THEN do not add a homepage tab`() = runTest(testDispatcher) {
        every { repository.getHomepageAsANewTabEnabled() } returns false
        val browserStore = createStore(restoreComplete = true)

        createBinding(browserStore, BrowsingMode.Normal).start()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 0) { fenixBrowserUseCases.addNewHomepageTab(private = any()) }
    }

    private fun createStore(
        restoreComplete: Boolean,
        tabs: List<TabSessionState> = emptyList(),
    ) = BrowserStore(
        initialState = BrowserState(tabs = tabs, restoreComplete = restoreComplete),
    )

    private fun createBinding(
        browserStore: BrowserStore,
        mode: BrowsingMode,
    ) = HomepageTabBinding(
        browserStore = browserStore,
        browsingModeManager = mockk<BrowsingModeManager> { every { this@mockk.mode } returns mode },
        fenixBrowserUseCases = fenixBrowserUseCases,
        repository = repository,
        mainDispatcher = testDispatcher,
    )
}
