/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser

import android.content.Context
import android.content.res.Resources
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.navigation.NavController
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.runTest
import mozilla.components.browser.state.action.RestoreCompleteAction
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.FenixApplication
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.components.toolbar.BrowserToolbarComposable
import org.mozilla.fenix.ext.application
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.onboarding.FenixOnboarding
import org.mozilla.fenix.utils.Settings
import org.robolectric.RobolectricTestRunner
import kotlin.coroutines.ContinuationInterceptor

@RunWith(RobolectricTestRunner::class)
class BrowserFragmentTest {

    private lateinit var store: BrowserStore
    private lateinit var testTab: TabSessionState
    private lateinit var browserFragment: BrowserFragment
    private lateinit var view: View
    private lateinit var homeActivity: HomeActivity
    private lateinit var fenixApplication: FenixApplication
    private lateinit var context: Context

    private lateinit var resources: Resources
    private lateinit var lifecycleOwner: MockedLifecycleOwner
    private lateinit var navController: NavController
    private lateinit var onboarding: FenixOnboarding
    private lateinit var settings: Settings
    private lateinit var appStore: AppStore

    @Before
    fun setup() {
        context = spyk(testContext)
        resources = spyk(testContext.resources)
        every { context.resources } returns resources

        fenixApplication = mockk(relaxed = true)
        every { context.application } returns fenixApplication

        homeActivity = mockk(relaxed = true)
        every { homeActivity.resources } returns testContext.resources

        view = mockk(relaxed = true)
        lifecycleOwner = MockedLifecycleOwner(Lifecycle.State.STARTED)
        navController = mockk(relaxed = true)
        onboarding = mockk(relaxed = true)
        settings = mockk(relaxed = true)
        appStore = AppStore(initialState = AppState())

        browserFragment = spyk(BrowserFragment())
        every { browserFragment.view } returns view
        every { browserFragment.isAdded } returns true
        every { browserFragment.browserToolbar } returns mockk<BrowserToolbarComposable>(relaxed = true)
        every { browserFragment.childFragmentManager } returns mockk(relaxed = true)
        every { browserFragment.activity } returns homeActivity
        every { browserFragment.lifecycle } returns lifecycleOwner.lifecycle
        every { browserFragment.viewLifecycleOwner } returns lifecycleOwner
        every { context.components.fenixOnboarding } returns onboarding
        every { context.components.settings } returns settings

        every { context.components.appStore } returns appStore
        every { browserFragment.requireContext() } returns context
        every { browserFragment.initializeUI(any(), any()) } returns mockk()
        every { browserFragment.fullScreenChanged(any()) } just Runs

        testTab = createTab(url = "https://mozilla.org")
        store = BrowserStore()
        every { context.components.core.store } returns store
    }

    @Test
    fun `GIVEN fragment is added WHEN selected tab changes THEN theme is updated`() = runTest {
        browserFragment.observeTabSelection(
            store,
            false,
            coroutineContext[ContinuationInterceptor] as CoroutineDispatcher,
        )
        verify(exactly = 0) { browserFragment.updateThemeForSession(testTab) }

        addAndSelectTab(testTab)
        testScheduler.advanceUntilIdle()

        verify(exactly = 1) { browserFragment.updateThemeForSession(testTab) }
    }

    @Test
    fun `GIVEN fragment is added WHEN selected tab is customTab THEN theme is not updated`() = runTest {
        browserFragment.observeTabSelection(
            store,
            true,
            coroutineContext[ContinuationInterceptor] as CoroutineDispatcher,
        )
        testScheduler.advanceUntilIdle()

        verify(exactly = 0) { browserFragment.updateThemeForSession(testTab) }

        addAndSelectTab(testTab)
        testScheduler.advanceUntilIdle()

        verify(exactly = 0) { browserFragment.updateThemeForSession(testTab) }
    }

    @Test
    fun `GIVEN fragment is removing WHEN selected tab changes THEN theme is not updated`() = runTest {
        every { browserFragment.isRemoving } returns true
        browserFragment.observeTabSelection(
            store,
            false,
            coroutineContext[ContinuationInterceptor] as CoroutineDispatcher,
        )

        addAndSelectTab(testTab)
        testScheduler.advanceUntilIdle()

        verify(exactly = 0) { browserFragment.updateThemeForSession(testTab) }
    }

    @Test
    fun `GIVEN browser UI is not initialized WHEN selected tab changes THEN browser UI is initialized`() = runTest {
        browserFragment.observeTabSelection(
            store,
            false,
            coroutineContext[ContinuationInterceptor] as CoroutineDispatcher,
        )

        verify(exactly = 0) { browserFragment.initializeUI(view, testTab) }

        addAndSelectTab(testTab)
        testScheduler.advanceUntilIdle()

        verify(exactly = 1) { browserFragment.initializeUI(view, testTab) }
    }

    @Test
    fun `GIVEN browser UI is initialized WHEN selected tab changes THEN toolbar is expanded`() = runTest {
        browserFragment.browserInitialized = true
        browserFragment.observeTabSelection(
            store,
            false,
            coroutineContext[ContinuationInterceptor] as CoroutineDispatcher,
        )

        val toolbar: BrowserToolbarComposable = mockk(relaxed = true)
        every { browserFragment.browserToolbar } returns toolbar

        val newSelectedTab = createTab("https://firefox.com")
        addAndSelectTab(newSelectedTab)
        testScheduler.advanceUntilIdle()

        verify(exactly = 1) { toolbar.expand() }
    }

    @Test
    fun `GIVEN browser UI is initialized WHEN selected tab changes THEN full screen mode is exited`() = runTest {
        browserFragment.browserInitialized = true
        browserFragment.observeTabSelection(
            store,
            false,
            coroutineContext[ContinuationInterceptor] as CoroutineDispatcher,
        )

        val newSelectedTab = createTab("https://firefox.com")
        addAndSelectTab(newSelectedTab)
        testScheduler.advanceUntilIdle()

        verify(exactly = 1) { browserFragment.fullScreenChanged(false) }
    }

    @Test
    fun `GIVEN tabs are restored WHEN there are no tabs THEN navigate to home`() = runTest {
        store = BrowserStore(initialState = BrowserState(tabs = listOf(testTab)))
        every { context.components.core.store } returns store

        browserFragment.observeRestoreComplete(
            store,
            navController,
            coroutineContext[ContinuationInterceptor] as CoroutineDispatcher,
        )
        store.dispatch(RestoreCompleteAction)
        testScheduler.advanceUntilIdle()

        verify(exactly = 1) { navController.popBackStack(R.id.homeFragment, false) }
    }

    @Test
    fun `GIVEN tabs are restored WHEN there are tabs THEN do not navigate`() = runTest {
        addAndSelectTab(testTab)
        browserFragment.observeRestoreComplete(
            store,
            navController,
            coroutineContext[ContinuationInterceptor] as CoroutineDispatcher,
        )
        store.dispatch(RestoreCompleteAction)
        testScheduler.advanceUntilIdle()

        verify(exactly = 0) { navController.popBackStack(R.id.homeFragment, false) }
    }

    @Test
    fun `GIVEN tabs are restored WHEN there is no selected tab THEN navigate to home`() = runTest {
        store = BrowserStore(initialState = BrowserState(tabs = listOf(testTab)))
        every { context.components.core.store } returns store

        browserFragment.observeRestoreComplete(
            store,
            navController,
            coroutineContext[ContinuationInterceptor] as CoroutineDispatcher,
        )
        store.dispatch(RestoreCompleteAction)
        testScheduler.advanceUntilIdle()

        verify(exactly = 1) { navController.popBackStack(R.id.homeFragment, false) }
    }

    @Test
    fun `GIVEN the onboarding is finished WHEN visiting any link THEN the onboarding is not dismissed `() = runTest {
        every { onboarding.userHasBeenOnboarded() } returns true

        browserFragment.observeTabSource(
            store,
            coroutineContext[ContinuationInterceptor] as CoroutineDispatcher,
        )

        val newSelectedTab = createTab("any-tab.org")
        addAndSelectTab(newSelectedTab)
        testScheduler.advanceUntilIdle()

        verify(exactly = 0) { onboarding.finish() }
    }

    @Test
    fun `GIVEN the onboarding is not finished WHEN visiting a link THEN the onboarding is dismissed `() = runTest {
        every { onboarding.userHasBeenOnboarded() } returns false

        browserFragment.observeTabSource(
            store,
            coroutineContext[ContinuationInterceptor] as CoroutineDispatcher,
        )

        val newSelectedTab = createTab("any-tab.org")
        addAndSelectTab(newSelectedTab)
        testScheduler.advanceUntilIdle()

        verify(exactly = 1) { onboarding.finish() }
    }

    @Test
    fun `GIVEN the onboarding is not finished WHEN visiting an onboarding link THEN the onboarding is not dismissed `() = runTest {
        every { onboarding.userHasBeenOnboarded() } returns false

        browserFragment.observeTabSource(
            store,
            coroutineContext[ContinuationInterceptor] as CoroutineDispatcher,
        )

        val newSelectedTab = createTab(BaseBrowserFragment.onboardingLinksList[0])
        addAndSelectTab(newSelectedTab)
        testScheduler.advanceUntilIdle()

        verify(exactly = 0) { onboarding.finish() }
    }

    @Test
    fun `GIVEN the onboarding is not finished WHEN opening a page from another app THEN the onboarding is not dismissed `() = runTest {
        every { onboarding.userHasBeenOnboarded() } returns false

        browserFragment.observeTabSource(
            store,
            coroutineContext[ContinuationInterceptor] as CoroutineDispatcher,
        )

        val newSelectedTab1 = createTab("any-tab-1.org", source = SessionState.Source.External.ActionSearch(mockk()))
        val newSelectedTab2 = createTab("any-tab-2.org", source = SessionState.Source.External.ActionView(mockk()))
        val newSelectedTab3 = createTab("any-tab-3.org", source = SessionState.Source.External.ActionSend(mockk()))
        val newSelectedTab4 = createTab("any-tab-4.org", source = SessionState.Source.External.CustomTab(mockk()))

        addAndSelectTab(newSelectedTab1)
        testScheduler.advanceUntilIdle()

        verify(exactly = 0) { onboarding.finish() }

        addAndSelectTab(newSelectedTab2)
        testScheduler.advanceUntilIdle()

        verify(exactly = 0) { onboarding.finish() }

        addAndSelectTab(newSelectedTab3)
        testScheduler.advanceUntilIdle()

        verify(exactly = 0) { onboarding.finish() }

        addAndSelectTab(newSelectedTab4)
        testScheduler.advanceUntilIdle()

        verify(exactly = 0) { onboarding.finish() }
    }

    @Test
    fun `GIVEN the onboarding is not finished WHEN visiting an link after redirect THEN the onboarding is not dismissed `() = runTest {
        every { onboarding.userHasBeenOnboarded() } returns false

        val newSelectedTab: TabSessionState = mockk(relaxed = true)
        every { newSelectedTab.content.loadRequest?.triggeredByRedirect } returns true
        every { newSelectedTab.parentId } returns null

        browserFragment.observeTabSource(
            store,
            coroutineContext[ContinuationInterceptor] as CoroutineDispatcher,
        )
        addAndSelectTab(newSelectedTab)
        testScheduler.advanceUntilIdle()

        verify(exactly = 0) { onboarding.finish() }
    }

    @Test
    fun `WHEN isPullToRefreshEnabledInBrowser is disabled THEN pull down refresh is disabled`() {
        every { context.components.settings.isPullToRefreshEnabledInBrowser } returns true
        assertTrue(browserFragment.shouldPullToRefreshBeEnabled(false))

        every { context.components.settings.isPullToRefreshEnabledInBrowser } returns false
        assertTrue(!browserFragment.shouldPullToRefreshBeEnabled(false))
    }

    @Test
    fun `WHEN in fullscreen THEN pull down refresh is disabled`() {
        every { context.components.settings.isPullToRefreshEnabledInBrowser } returns true
        assertTrue(browserFragment.shouldPullToRefreshBeEnabled(false))
        assertTrue(!browserFragment.shouldPullToRefreshBeEnabled(true))
    }

    private fun addAndSelectTab(tab: TabSessionState) {
        store.dispatch(TabListAction.AddTabAction(tab))
        store.dispatch(TabListAction.SelectTabAction(tab.id))
    }

    internal class MockedLifecycleOwner(initialState: Lifecycle.State) : LifecycleOwner {
        override val lifecycle: Lifecycle = LifecycleRegistry(this).apply {
            currentState = initialState
        }
    }

    @Test
    fun `WHEN updating the last browse activity THEN update the associated preference`() {
        val settings: Settings = mockk(relaxed = true)

        every { browserFragment.context } returns context
        every { context.components.settings } returns settings

        browserFragment.updateLastBrowseActivity()

        verify(exactly = 1) { settings.recordLastBrowseActivity() }
    }
}
