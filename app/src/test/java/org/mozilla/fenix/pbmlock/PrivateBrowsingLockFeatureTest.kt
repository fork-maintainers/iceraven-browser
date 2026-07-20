/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.pbmlock

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.selector.privateTabs
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.appstate.AppState

@RunWith(AndroidJUnit4::class)
class PrivateBrowsingLockFeatureTest {

    private val testDispatcher = StandardTestDispatcher()
    private val regularTabs: List<TabSessionState> = listOf(
        createTab("https://www.firefox.com", id = "firefox"),
        createTab("https://www.mozilla.org", id = "mozilla"),
    )
    private val mixedTabs: List<TabSessionState> = listOf(
        createTab("https://www.firefox.com", id = "firefox", private = true),
        createTab("https://www.mozilla.org", id = "mozilla"),
    )

    // zero tabs cases
    @Test
    fun `GIVEN feature is enabled and mode is normal WHEN number of private tabs reaches zero THEN we unlock private mode`() {
        val isFeatureEnabled = true
        val mode = BrowsingMode.Normal

        val appStore = createAppStore(mode, isPrivateScreenLocked = true)
        val browserStore = createBrowserStore(mixedTabs)
        createFeature(appStore, browserStore, createStorage(isFeatureEnabled))

        assertTrue(appStore.state.isPrivateScreenLocked)

        browserStore.dispatch(TabListAction.RemoveAllPrivateTabsAction)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(appStore.state.isPrivateScreenLocked)
    }

    @Test
    fun `GIVEN feature is enabled and mode is private WHEN authenticated and number of private tabs reaches zero THEN private mode is unchanged`() {
        val mode = BrowsingMode.Private

        val appStore = createAppStore(mode)
        val browserStore = createBrowserStore(mixedTabs)
        val useCase = PrivateBrowsingLockUseCases.AuthenticatedUseCase(appStore)

        useCase.invoke()

        assertFalse(appStore.state.isPrivateScreenLocked)

        browserStore.dispatch(TabListAction.RemoveAllPrivateTabsAction)

        assertFalse(appStore.state.isPrivateScreenLocked)
    }

    @Test
    fun `GIVEN the feature is disabled and mode is normal WHEN number of private tabs reaches zero THEN private mode is unchanged `() {
        val isFeatureEnabled = false
        val mode = BrowsingMode.Normal

        val appStore = createAppStore(mode)
        val browserStore = createBrowserStore(mixedTabs)
        createFeature(browserStore = browserStore, appStore = appStore, storage = createStorage(isFeatureEnabled = isFeatureEnabled))

        assertFalse(appStore.state.isPrivateScreenLocked)

        browserStore.dispatch(TabListAction.RemoveAllPrivateTabsAction)

        assertFalse(appStore.state.isPrivateScreenLocked)
    }

    @Test
    fun `GIVEN the feature is disabled and mode is private WHEN number of private tabs reaches zero THEN private mode is unchanged `() {
        val isFeatureEnabled = false
        val mode = BrowsingMode.Private

        val appStore = AppStore(initialState = AppState(mode = mode))
        val browserStore = createBrowserStore(mixedTabs)
        createFeature(appStore, browserStore, createStorage(isFeatureEnabled))

        assertFalse(appStore.state.isPrivateScreenLocked)

        browserStore.dispatch(TabListAction.RemoveAllPrivateTabsAction)

        assertFalse(appStore.state.isPrivateScreenLocked)
    }

    // initializing cases
    @Test
    fun `GIVEN feature is enabled and mode is private and there are private tabs WHEN initializing feature THEN we lock private mode`() {
        val isFeatureEnabled = true
        val mode = BrowsingMode.Private

        val appStore = createAppStore(mode, isPrivateScreenLocked = true)
        val browserStore = createBrowserStore(mixedTabs)
        createFeature(appStore, browserStore, createStorage(isFeatureEnabled))

        assertTrue(appStore.state.isPrivateScreenLocked)
    }

    @Test
    fun `GIVEN feature is enabled and mode is normal and there are private tabs WHEN initializing feature THEN we lock private mode`() {
        val isFeatureEnabled = true
        val mode = BrowsingMode.Normal

        val appStore = createAppStore(mode, isPrivateScreenLocked = true)
        val browserStore = createBrowserStore(mixedTabs)
        createFeature(appStore, browserStore, createStorage(isFeatureEnabled))

        assertTrue(appStore.state.isPrivateScreenLocked)
    }

    @Test
    fun `GIVEN feature is disabled and mode is private and there are private tabs WHEN initializing the app THEN we do not lock private mode`() {
        val isFeatureEnabled = false
        val mode = BrowsingMode.Private

        val appStore = createAppStore(mode)
        val browserStore = createBrowserStore(mixedTabs)
        createFeature(appStore, browserStore, createStorage(isFeatureEnabled))

        assertFalse(appStore.state.isPrivateScreenLocked)
    }

    @Test
    fun `GIVEN feature is disabled and mode is normal and there are private tabs WHEN initializing the app THEN we do not lock private mode`() {
        val isFeatureEnabled = false
        val mode = BrowsingMode.Normal

        val appStore = createAppStore(mode)
        val browserStore = createBrowserStore(mixedTabs)
        createFeature(appStore, browserStore, createStorage(isFeatureEnabled))

        assertFalse(appStore.state.isPrivateScreenLocked)
    }

    @Test
    fun `GIVEN feature is enabled and mode is private and there are no private tabs WHEN initializing feature THEN we do not lock private mode`() {
        val isFeatureEnabled = true
        val mode = BrowsingMode.Private

        val appStore = createAppStore(mode)
        val browserStore = createBrowserStore(regularTabs)
        createFeature(appStore, browserStore, createStorage(isFeatureEnabled))

        assertFalse(appStore.state.isPrivateScreenLocked)
    }

    @Test
    fun `GIVEN feature is enabled and mode is normal and there are no private tabs WHEN initializing feature THEN we do not lock private mode`() {
        val isFeatureEnabled = true
        val mode = BrowsingMode.Normal

        val appStore = createAppStore(mode)
        val browserStore = createBrowserStore(regularTabs)
        createFeature(appStore, browserStore, createStorage(isFeatureEnabled))

        assertFalse(appStore.state.isPrivateScreenLocked)
    }

    // observing private mode tests
    @Test
    fun `GIVEN normal mode and enabled lock WHEN observePrivateModeLock is triggered THEN observing lock doesn't trigger`() = runTest {
        var result = false
        val appState = AppState(mode = BrowsingMode.Normal, isPrivateScreenLocked = true)

        observePrivateModeLock(
            flow = flowOf(appState),
            onPrivateModeLocked = { result = true },
        )

        assertFalse(result)
    }

    @Test
    fun `GIVEN normal mode and disabled lock WHEN observePrivateModeLock is triggered THEN observing lock doesn't trigger`() = runTest {
        var result = false
        val appState = AppState(mode = BrowsingMode.Normal, isPrivateScreenLocked = false)

        observePrivateModeLock(
            flow = flowOf(appState),
            onPrivateModeLocked = { result = true },
        )

        assertFalse(result)
    }

    @Test
    fun `GIVEN private mode and enabled lock WHEN observePrivateModeLock is triggered THEN observing lock triggers`() = runTest {
        var result = false
        val appState = AppState(mode = BrowsingMode.Private, isPrivateScreenLocked = true)

        observePrivateModeLock(
            flow = flowOf(appState),
            onPrivateModeLocked = { result = true },
        )

        assertTrue(result)
    }

    @Test
    fun `GIVEN private mode and disabled lock WHEN observePrivateModeLock is triggered THEN observing lock doesn't trigger`() = runTest {
        var result = false
        val appState = AppState(mode = BrowsingMode.Private, isPrivateScreenLocked = false)

        observePrivateModeLock(
            flow = flowOf(appState),
            onPrivateModeLocked = { result = true },
        )

        assertFalse(result)
    }

    // on stop tests
    @Test
    fun `GIVEN feature is on and mode is private and there are unlocked private tabs WHEN Activity stops without config change THEN we lock private mode`() {
        val isFeatureEnabled = true
        val mode = BrowsingMode.Private

        val appStore = createAppStore(mode)
        val browserStore = createBrowserStore(mixedTabs)
        val feature = createFeature(appStore, browserStore, createStorage(isFeatureEnabled))

        // imitate user passing auth
        appStore.dispatch(AppAction.PrivateBrowsingLockAction.UpdatePrivateBrowsingLock(isLocked = false))

        assertTrue(appStore.state.mode == mode)
        assertFalse(appStore.state.isPrivateScreenLocked)

        val activity = mockk<AppCompatActivity>(relaxed = true)
        every { activity.isChangingConfigurations } returns false

        feature.onStop(activity)

        assertTrue(appStore.state.isPrivateScreenLocked)
    }

    @Test
    fun `GIVEN feature is on and mode is normal and there are unlocked private tabs WHEN Activity stops without config change THEN we lock private mode`() {
        val isFeatureEnabled = true
        val mode = BrowsingMode.Normal

        val appStore = createAppStore(mode)
        val browserStore = createBrowserStore(mixedTabs)
        val feature = createFeature(appStore, browserStore, createStorage(isFeatureEnabled))

        // imitate user passing auth
        appStore.dispatch(AppAction.PrivateBrowsingLockAction.UpdatePrivateBrowsingLock(isLocked = false))

        assertTrue(appStore.state.mode == mode)
        assertFalse(appStore.state.isPrivateScreenLocked)

        val activity = mockk<AppCompatActivity>(relaxed = true)
        every { activity.isChangingConfigurations } returns false

        feature.onStop(activity)

        assertTrue(appStore.state.mode == mode)
        assertTrue(appStore.state.isPrivateScreenLocked)
    }

    @Test
    fun `GIVEN feature is on and mode is private and there are no private tabs WHEN Activity stops without config change THEN we do not lock private mode`() {
        val isFeatureEnabled = true
        val mode = BrowsingMode.Private

        val appStore = createAppStore(mode)
        val browserStore = createBrowserStore(regularTabs)
        val feature = createFeature(appStore, browserStore, createStorage(isFeatureEnabled))

        assertTrue(appStore.state.mode == mode)
        assertFalse(appStore.state.isPrivateScreenLocked)

        val activity = mockk<AppCompatActivity>(relaxed = true)
        every { activity.isChangingConfigurations } returns false

        feature.onStop(activity)

        assertFalse(appStore.state.isPrivateScreenLocked)
    }

    @Test
    fun `GIVEN feature is on and mode is normal and there are no private tabs WHEN Activity stops without config change THEN we do not lock private mode`() {
        val isFeatureEnabled = true
        val mode = BrowsingMode.Normal

        val appStore = createAppStore(mode)
        val browserStore = createBrowserStore(regularTabs)
        val feature = createFeature(browserStore = browserStore, appStore = appStore, storage = createStorage(isFeatureEnabled = isFeatureEnabled))

        assertTrue(appStore.state.mode == mode)
        assertFalse(appStore.state.isPrivateScreenLocked)

        val activity = mockk<AppCompatActivity>(relaxed = true)
        every { activity.isChangingConfigurations } returns false

        feature.onStop(activity)

        assertFalse(appStore.state.isPrivateScreenLocked)
    }

    @Test
    fun `GIVEN feature is off and mode is private and there are private tabs WHEN Activity stops without config change THEN we do not lock private mode`() {
        val isFeatureEnabled = false
        val mode = BrowsingMode.Private

        val appStore = createAppStore(mode)
        val browserStore = createBrowserStore(mixedTabs)
        val feature = createFeature(appStore, browserStore, createStorage(isFeatureEnabled))

        assertTrue(appStore.state.mode == mode)
        assertFalse(appStore.state.isPrivateScreenLocked)

        val activity = mockk<AppCompatActivity>(relaxed = true)
        every { activity.isChangingConfigurations } returns false

        feature.onStop(activity)

        assertFalse(appStore.state.isPrivateScreenLocked)
    }

    @Test
    fun `GIVEN feature is off and mode is normal and there are private tabs WHEN Activity stops without config change THEN we do not lock private mode`() {
        val isFeatureEnabled = false
        val mode = BrowsingMode.Normal

        val appStore = createAppStore(mode)
        val browserStore = createBrowserStore(mixedTabs)
        val feature = createFeature(appStore, browserStore, createStorage(isFeatureEnabled))

        assertTrue(appStore.state.mode == mode)
        assertFalse(appStore.state.isPrivateScreenLocked)

        val activity = mockk<AppCompatActivity>(relaxed = true)
        every { activity.isChangingConfigurations } returns false

        feature.onStop(activity)

        assertFalse(appStore.state.isPrivateScreenLocked)
    }

    // NB: this is an important case: we don't want to lock the screen on config change if the user is already in
    // private mode because switching modes causes the activity to recreate; e.g., the user switches to private mode
    // tab through the tabstray. the app goes into private mode and then activity shuts due to configuration change.
    // if we lock the screen at that point, going into private mode will always lock it due to activity restart.
    @Test
    fun `GIVEN feature is on and mode is private and there are private tabs WHEN Activity stops with config change THEN we do not lock private mode`() {
        val isFeatureEnabled = true
        val mode = BrowsingMode.Private

        val appStore = AppStore(initialState = AppState(mode = mode))
        val browserStore = createBrowserStore(mixedTabs)
        val useCase = PrivateBrowsingLockUseCases.AuthenticatedUseCase(appStore)
        val feature = createFeature(browserStore = browserStore, appStore = appStore, storage = createStorage(isFeatureEnabled = isFeatureEnabled))

        useCase.invoke()

        val activity = mockk<AppCompatActivity>(relaxed = true)
        every { activity.isChangingConfigurations } returns true

        feature.onStop(activity)

        assertFalse(appStore.state.isPrivateScreenLocked)
    }

    @Test
    fun `GIVEN the feature is on and there are private tabs and we are in a custom tab WHEN we click on Open in Firefox THEN we don't lock PBM`() {
        val isFeatureEnabled = true

        val appStore = AppStore(initialState = AppState(openInFirefoxRequested = false))
        val browserStore = createBrowserStore(mixedTabs)
        val feature = createFeature(appStore, browserStore, createStorage(isFeatureEnabled))

        appStore.dispatch(AppAction.OpenInFirefoxStarted)
        testDispatcher.scheduler.advanceUntilIdle()

        val activity = mockk<AppCompatActivity>(relaxed = true)

        feature.onStop(activity)

        assertFalse(appStore.state.isPrivateScreenLocked)
    }

    // turning the feature on and off tests
    @Test
    fun `GIVEN the feature is on and there are private tabs WHEN we turn off the feature THEN we unlock private tabs and don't lock it`() {
        val isFeatureEnabled = true
        val mode = BrowsingMode.Private

        val appStore = createAppStore(mode, isPrivateScreenLocked = true)
        val browserStore = createBrowserStore(mixedTabs)
        val storage = createStorage(isFeatureEnabled)

        val feature = createFeature(appStore, browserStore, storage)

        assertTrue(appStore.state.isPrivateScreenLocked)

        // verify that disabled feature state unlocks private mode
        val sharedPrefUpdate = false
        storage.listener?.invoke(sharedPrefUpdate)

        assertTrue(browserStore.state.privateTabs.isNotEmpty())
        assertFalse(appStore.state.isPrivateScreenLocked)

        // verify that activity.onStop doesn't lock private mode
        val activity = mockk<AppCompatActivity>(relaxed = true)
        every { activity.isChangingConfigurations } returns false

        feature.onStop(activity)

        assertTrue(browserStore.state.privateTabs.isNotEmpty())
        assertFalse(appStore.state.isPrivateScreenLocked)

        // verify that going to normal mode doesn't lock private mode
        appStore.dispatch(AppAction.BrowsingModeManagerModeChanged(mode = BrowsingMode.Normal))

        assertTrue(browserStore.state.privateTabs.isNotEmpty())
        assertFalse(appStore.state.isPrivateScreenLocked)
    }

    @Test
    fun `GIVEN the feature is off and mode is normal and there are private tabs WHEN we turn on the feature THEN we lock private tabs`() {
        val isFeatureEnabled = false
        val mode = BrowsingMode.Normal

        val appStore = createAppStore(mode)
        val browserStore = createBrowserStore(mixedTabs)
        val storage = createStorage(isFeatureEnabled)

        createFeature(appStore, browserStore, storage)

        assertFalse(appStore.state.isPrivateScreenLocked)

        // verify that disabled feature state unlocks private mode
        val sharedPrefUpdate = true
        storage.listener?.invoke(sharedPrefUpdate)

        assertTrue(browserStore.state.privateTabs.isNotEmpty())
        assertTrue(appStore.state.isPrivateScreenLocked)
    }

    @Test
    fun `GIVEN the feature is off and mode is private and there are private tabs WHEN we turn on the feature THEN we do not lock private tabs`() {
        val isFeatureEnabled = false
        val mode = BrowsingMode.Private

        val appStore = createAppStore(mode)
        val browserStore = createBrowserStore(mixedTabs)
        val storage = createStorage(isFeatureEnabled)

        createFeature(appStore, browserStore, storage)

        assertFalse(appStore.state.isPrivateScreenLocked)

        // verify that disabled feature state unlocks private mode
        val sharedPrefUpdate = true
        storage.listener?.invoke(sharedPrefUpdate)

        assertTrue(browserStore.state.privateTabs.isNotEmpty())
        assertFalse(appStore.state.isPrivateScreenLocked)
    }

    // turning the feature on and off tests
    @Test
    fun `GIVEN the feature is on and there are private tabs WHEN switching to normal mode THEN we unlock private tabs and don't lock it`() {
        val isFeatureEnabled = true
        val mode = BrowsingMode.Private

        val appStore = createAppStore(mode, isPrivateScreenLocked = true)
        val browserStore = createBrowserStore(mixedTabs)

        createFeature(appStore, browserStore, createStorage(isFeatureEnabled))

        assertTrue(appStore.state.isPrivateScreenLocked)

        // imitate user passing auth
        appStore.dispatch(AppAction.PrivateBrowsingLockAction.UpdatePrivateBrowsingLock(isLocked = false))

        assertTrue(appStore.state.mode == BrowsingMode.Private)
        assertFalse(appStore.state.isPrivateScreenLocked)

        // verify that going to normal mode doesn't lock private mode
        appStore.dispatch(AppAction.BrowsingModeManagerModeChanged(mode = BrowsingMode.Normal))

        assertTrue(appStore.state.mode == BrowsingMode.Normal)
        assertFalse(appStore.state.isPrivateScreenLocked)
    }

    @Test
    fun `WHEN feature resumes THEN storage is listening to shared pref`() {
        val storage = MockedPrivateBrowsingLockStorage()
        val feature = createFeature(browserStore = BrowserStore(), appStore = AppStore(), storage = storage)
        val lifecycleOwner = MockedLifecycleOwner(Lifecycle.State.CREATED)

        assertFalse(storage.startCalled)

        feature.onResume(lifecycleOwner)
        assertTrue(storage.startCalled)
    }

    internal class MockedLifecycleOwner(initialState: Lifecycle.State) : LifecycleOwner {
        override val lifecycle: Lifecycle = LifecycleRegistry(this).apply {
            currentState = initialState
        }
    }

    internal class MockedPrivateBrowsingLockStorage(
        override val isFeatureEnabled: Boolean = true,
    ) : PrivateBrowsingLockStorage {
        var listener: ((Boolean) -> Unit)? = null

        var startCalled = false

        override fun addFeatureStateListener(listener: (Boolean) -> Unit) {
            this.listener = listener
        }

        override fun startObservingSharedPrefs() {
            startCalled = true
        }
    }

    private fun createFeature(
        appStore: AppStore,
        browserStore: BrowserStore,
        storage: PrivateBrowsingLockStorage,
    ) = PrivateBrowsingLockFeature(
        appStore = appStore,
        browserStore = browserStore,
        storage = storage,
        mainDispatcher = testDispatcher,
    )

    private fun createStorage(isFeatureEnabled: Boolean = true) = MockedPrivateBrowsingLockStorage(isFeatureEnabled)

    private fun createBrowserStore(tabs: List<TabSessionState>) = BrowserStore(
        BrowserState(
            tabs = tabs,
            selectedTabId = "mozilla",
        ),
    )

    private fun createAppStore(
        mode: BrowsingMode,
        isPrivateScreenLocked: Boolean = false,
    ) = AppStore(
        initialState = AppState(
            mode = mode,
            isPrivateScreenLocked = isPrivateScreenLocked,
        ),
    )
}
