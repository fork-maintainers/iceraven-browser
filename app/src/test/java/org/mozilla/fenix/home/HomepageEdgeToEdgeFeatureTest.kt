/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.color.MaterialColors
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mozilla.components.compose.browser.toolbar.store.BrowserEditToolbarAction
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarAction
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarStore
import mozilla.components.compose.browser.toolbar.ui.BrowserToolbarQuery
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.utils.Settings
import org.mozilla.fenix.wallpapers.Wallpaper
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class HomepageEdgeToEdgeFeatureTest {

    private val testDispatcher = StandardTestDispatcher()
    private val settings: Settings = mockk(relaxed = true)
    private val browsingModeManager: BrowsingModeManager = mockk(relaxed = true) {
        every { mode } returns BrowsingMode.Normal
    }

    @Test
    fun `GIVEN feature is disabled WHEN feature starts THEN wallpaper updates are not observed`() = runTest(testDispatcher) {
        every { settings.enableHomepageEdgeToEdgeBackgroundFeature } returns false
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val decorView = activity.window.decorView as ViewGroup
        val initialChildCount = decorView.childCount
        val appStore = AppStore()

        HomepageEdgeToEdgeFeature(
            appStore = appStore,
            activity = activity,
            settings = settings,
            browsingModeManager = browsingModeManager,
            toolbarStore = BrowserToolbarStore(),
            mainDispatcher = testDispatcher,
        ).start()
        testScheduler.advanceUntilIdle()
        appStore.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(Wallpaper.EdgeToEdge))
        testScheduler.advanceUntilIdle()

        assertEquals(initialChildCount, decorView.childCount)
    }

    @Test
    fun `GIVEN feature is enabled WHEN EdgeToEdge wallpaper is selected THEN status bar background is added`() = runTest(testDispatcher) {
        every { settings.enableHomepageEdgeToEdgeBackgroundFeature } returns true
        every { settings.shouldUseBottomToolbar } returns false
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val decorView = activity.window.decorView as ViewGroup
        val initialChildCount = decorView.childCount
        val appStore = AppStore()
        appStore.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(Wallpaper.EdgeToEdge))

        HomepageEdgeToEdgeFeature(
            appStore = appStore,
            activity = activity,
            settings = settings,
            browsingModeManager = browsingModeManager,
            toolbarStore = BrowserToolbarStore(),
            mainDispatcher = testDispatcher,
        ).start()
        testScheduler.advanceUntilIdle()

        assertEquals(initialChildCount + 1, decorView.childCount)
        ViewCompat.dispatchApplyWindowInsets(
            decorView.getChildAt(initialChildCount),
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, 1, 0, 0))
                .build(),
        )
        assertEquals(
            R.drawable.home_background_gradient,
            shadowOf(shadowOf(activity.window).backgroundDrawable).createdFromResId,
        )
    }

    @Test
    fun `GIVEN EdgeToEdge wallpaper is active WHEN default wallpaper is selected THEN status bar background is removed`() = runTest(testDispatcher) {
        every { settings.enableHomepageEdgeToEdgeBackgroundFeature } returns true
        every { settings.shouldUseBottomToolbar } returns false
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val decorView = activity.window.decorView as ViewGroup
        val initialChildCount = decorView.childCount
        val appStore = AppStore()
        appStore.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(Wallpaper.EdgeToEdge))

        HomepageEdgeToEdgeFeature(
            appStore = appStore,
            activity = activity,
            settings = settings,
            browsingModeManager = browsingModeManager,
            toolbarStore = BrowserToolbarStore(),
            mainDispatcher = testDispatcher,
        ).start()
        testScheduler.advanceUntilIdle()

        assertEquals(initialChildCount + 1, decorView.childCount)
        ViewCompat.dispatchApplyWindowInsets(
            decorView.getChildAt(initialChildCount),
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, 1, 0, 0))
                .build(),
        )
        assertEquals(
            R.drawable.home_background_gradient,
            shadowOf(shadowOf(activity.window).backgroundDrawable).createdFromResId,
        )

        appStore.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(Wallpaper.Default))
        testScheduler.advanceUntilIdle()

        assertEquals(initialChildCount, decorView.childCount)
        assertEquals(
            R.color.fx_mobile_surface,
            shadowOf(shadowOf(activity.window).backgroundDrawable).createdFromResId,
        )
    }

    @Test
    fun `GIVEN EdgeToEdge wallpaper is active WHEN feature stops THEN status bar background is removed`() = runTest(testDispatcher) {
        every { settings.enableHomepageEdgeToEdgeBackgroundFeature } returns true
        every { settings.shouldUseBottomToolbar } returns false
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val decorView = activity.window.decorView as ViewGroup
        val initialChildCount = decorView.childCount
        val appStore = AppStore()
        appStore.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(Wallpaper.EdgeToEdge))
        val feature = HomepageEdgeToEdgeFeature(
            appStore = appStore,
            activity = activity,
            settings = settings,
            browsingModeManager = browsingModeManager,
            toolbarStore = BrowserToolbarStore(),
            mainDispatcher = testDispatcher,
        )
        feature.start()
        testScheduler.advanceUntilIdle()
        assertEquals(initialChildCount + 1, decorView.childCount)

        feature.stop()

        assertEquals(initialChildCount, decorView.childCount)
        assertEquals(
            R.color.fx_mobile_surface,
            shadowOf(shadowOf(activity.window).backgroundDrawable).createdFromResId,
        )
    }

    @Test
    fun `GIVEN feature is stopped WHEN EdgeToEdge wallpaper is selected THEN wallpaper updates are not observed`() = runTest(testDispatcher) {
        every { settings.enableHomepageEdgeToEdgeBackgroundFeature } returns true
        every { settings.shouldUseBottomToolbar } returns false
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val decorView = activity.window.decorView as ViewGroup
        val initialChildCount = decorView.childCount
        val appStore = AppStore()
        val feature = HomepageEdgeToEdgeFeature(
            appStore = appStore,
            activity = activity,
            settings = settings,
            browsingModeManager = browsingModeManager,
            toolbarStore = BrowserToolbarStore(),
            mainDispatcher = testDispatcher,
        )
        feature.start()
        testScheduler.advanceUntilIdle()

        feature.stop()
        appStore.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(Wallpaper.EdgeToEdge))
        testScheduler.advanceUntilIdle()

        assertEquals(initialChildCount, decorView.childCount)
    }

    @Test
    fun `GIVEN private mode WHEN EdgeToEdge wallpaper is selected THEN private background is shown`() = runTest(testDispatcher) {
        every { settings.enableHomepageEdgeToEdgeBackgroundFeature } returns true
        every { settings.shouldUseBottomToolbar } returns false
        every { browsingModeManager.mode } returns BrowsingMode.Private
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val decorView = activity.window.decorView as ViewGroup
        val initialChildCount = decorView.childCount
        val appStore = AppStore()
        appStore.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(Wallpaper.EdgeToEdge))

        HomepageEdgeToEdgeFeature(
            appStore = appStore,
            activity = activity,
            settings = settings,
            browsingModeManager = browsingModeManager,
            toolbarStore = BrowserToolbarStore(),
            mainDispatcher = testDispatcher,
        ).start()
        testScheduler.advanceUntilIdle()

        ViewCompat.dispatchApplyWindowInsets(
            decorView.getChildAt(initialChildCount),
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, 1, 0, 0))
                .build(),
        )
        assertEquals(
            R.color.fx_mobile_private_surface,
            shadowOf(shadowOf(activity.window).backgroundDrawable).createdFromResId,
        )
    }

    @Test
    fun `GIVEN search query is shown WHEN EdgeToEdge wallpaper is active THEN status bar background uses surface color`() = runTest(testDispatcher) {
        every { settings.enableHomepageEdgeToEdgeBackgroundFeature } returns true
        every { settings.shouldUseBottomToolbar } returns false
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val decorView = activity.window.decorView as ViewGroup
        val initialChildCount = decorView.childCount
        val appStore = AppStore()
        val toolbarStore = BrowserToolbarStore()
        appStore.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(Wallpaper.EdgeToEdge))

        HomepageEdgeToEdgeFeature(
            appStore = appStore,
            activity = activity,
            settings = settings,
            browsingModeManager = browsingModeManager,
            toolbarStore = toolbarStore,
            mainDispatcher = testDispatcher,
        ).start()
        testScheduler.advanceUntilIdle()

        toolbarStore.dispatch(BrowserToolbarAction.EnterEditMode(isPrivate = false))
        toolbarStore.dispatch(BrowserEditToolbarAction.SearchQueryUpdated(BrowserToolbarQuery("test")))
        testScheduler.advanceUntilIdle()

        assertEquals(
            MaterialColors.getColor(
                activity,
                com.google.android.material.R.attr.colorSurface,
                "Could not resolve color",
            ),
            (decorView.getChildAt(initialChildCount).background as ColorDrawable).color,
        )
    }

    @Test
    fun `GIVEN homepage trending and recent searches are enabled WHEN the toolbar is selected with no query on EdgeToEdge wallpaper THEN status bar background uses surface color`() = runTest(testDispatcher) {
        every { settings.enableHomepageEdgeToEdgeBackgroundFeature } returns true
        every { settings.shouldUseBottomToolbar } returns false
        every { settings.enableHomepageTrendingRecentSearch } returns true
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val decorView = activity.window.decorView as ViewGroup
        val initialChildCount = decorView.childCount
        val appStore = AppStore()
        val toolbarStore = BrowserToolbarStore()
        appStore.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(Wallpaper.EdgeToEdge))

        HomepageEdgeToEdgeFeature(
            appStore = appStore,
            activity = activity,
            settings = settings,
            browsingModeManager = browsingModeManager,
            toolbarStore = toolbarStore,
            mainDispatcher = testDispatcher,
        ).start()
        testScheduler.advanceUntilIdle()

        toolbarStore.dispatch(BrowserToolbarAction.EnterEditMode(isPrivate = false))
        testScheduler.advanceUntilIdle()

        assertEquals(
            MaterialColors.getColor(
                activity,
                com.google.android.material.R.attr.colorSurface,
                "Could not resolve color",
            ),
            (decorView.getChildAt(initialChildCount).background as ColorDrawable).color,
        )
    }

    @Test
    fun `GIVEN tab strip is enabled WHEN search query is shown on EdgeToEdge wallpaper THEN status bar background is shown`() = runTest(testDispatcher) {
        every { settings.enableHomepageEdgeToEdgeBackgroundFeature } returns true
        every { settings.shouldUseBottomToolbar } returns false
        every { settings.isTabStripEnabled } returns true
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val decorView = activity.window.decorView as ViewGroup
        val initialChildCount = decorView.childCount
        val appStore = AppStore()
        val toolbarStore = BrowserToolbarStore()
        appStore.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(Wallpaper.EdgeToEdge))

        HomepageEdgeToEdgeFeature(
            appStore = appStore,
            activity = activity,
            settings = settings,
            browsingModeManager = browsingModeManager,
            toolbarStore = toolbarStore,
            mainDispatcher = testDispatcher,
        ).start()
        testScheduler.advanceUntilIdle()

        toolbarStore.dispatch(BrowserToolbarAction.EnterEditMode(isPrivate = false))
        toolbarStore.dispatch(BrowserEditToolbarAction.SearchQueryUpdated(BrowserToolbarQuery("test")))
        testScheduler.advanceUntilIdle()

        assertEquals(
            ContextCompat.getColor(activity, R.color.homepage_tab_edge_to_edge_toolbar_background),
            (decorView.getChildAt(initialChildCount).background as ColorDrawable).color,
        )
    }

    @Test
    fun `GIVEN EdgeToEdge wallpaper is active WHEN EdgeToEdge wallpaper is selected again THEN status bar background is not duplicated`() = runTest(testDispatcher) {
        every { settings.enableHomepageEdgeToEdgeBackgroundFeature } returns true
        every { settings.shouldUseBottomToolbar } returns false
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val decorView = activity.window.decorView as ViewGroup
        val initialChildCount = decorView.childCount
        val appStore = AppStore()
        appStore.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(Wallpaper.EdgeToEdge))

        HomepageEdgeToEdgeFeature(
            appStore = appStore,
            activity = activity,
            settings = settings,
            browsingModeManager = browsingModeManager,
            toolbarStore = BrowserToolbarStore(),
            mainDispatcher = testDispatcher,
        ).start()
        testScheduler.advanceUntilIdle()

        appStore.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(Wallpaper.EdgeToEdge))
        testScheduler.advanceUntilIdle()

        assertEquals(initialChildCount + 1, decorView.childCount)
    }

    @Test
    fun `GIVEN EdgeToEdge wallpaper was removed WHEN EdgeToEdge wallpaper is selected again THEN status bar background is restored`() = runTest(testDispatcher) {
        every { settings.enableHomepageEdgeToEdgeBackgroundFeature } returns true
        every { settings.shouldUseBottomToolbar } returns false
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val decorView = activity.window.decorView as ViewGroup
        val initialChildCount = decorView.childCount
        val appStore = AppStore()
        appStore.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(Wallpaper.EdgeToEdge))

        HomepageEdgeToEdgeFeature(
            appStore = appStore,
            activity = activity,
            settings = settings,
            browsingModeManager = browsingModeManager,
            toolbarStore = BrowserToolbarStore(),
            mainDispatcher = testDispatcher,
        ).start()
        testScheduler.advanceUntilIdle()
        appStore.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(Wallpaper.Default))
        testScheduler.advanceUntilIdle()

        appStore.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(Wallpaper.EdgeToEdge))
        testScheduler.advanceUntilIdle()

        assertEquals(initialChildCount + 1, decorView.childCount)
        ViewCompat.dispatchApplyWindowInsets(
            decorView.getChildAt(initialChildCount),
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, 1, 0, 0))
                .build(),
        )
        assertEquals(
            R.drawable.home_background_gradient,
            shadowOf(shadowOf(activity.window).backgroundDrawable).createdFromResId,
        )
    }

    @Test
    fun `GIVEN bottom toolbar is shown WHEN toolbar is not in edit mode THEN status bar background is transparent`() = runTest(testDispatcher) {
        every { settings.enableHomepageEdgeToEdgeBackgroundFeature } returns true
        every { settings.shouldUseBottomToolbar } returns true
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val decorView = activity.window.decorView as ViewGroup
        val initialChildCount = decorView.childCount
        val appStore = AppStore()
        appStore.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(Wallpaper.EdgeToEdge))

        HomepageEdgeToEdgeFeature(
            appStore = appStore,
            activity = activity,
            settings = settings,
            browsingModeManager = browsingModeManager,
            toolbarStore = BrowserToolbarStore(),
            mainDispatcher = testDispatcher,
        ).start()
        testScheduler.advanceUntilIdle()

        assertEquals(
            Color.TRANSPARENT,
            (decorView.getChildAt(initialChildCount).background as ColorDrawable).color,
        )
    }

    @Test
    fun `GIVEN bottom toolbar is shown WHEN toolbar enters edit mode THEN status bar background is shown`() = runTest(testDispatcher) {
        every { settings.enableHomepageEdgeToEdgeBackgroundFeature } returns true
        every { settings.shouldUseBottomToolbar } returns true
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val decorView = activity.window.decorView as ViewGroup
        val initialChildCount = decorView.childCount
        val appStore = AppStore()
        val toolbarStore = BrowserToolbarStore()
        appStore.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(Wallpaper.EdgeToEdge))

        HomepageEdgeToEdgeFeature(
            appStore = appStore,
            activity = activity,
            settings = settings,
            browsingModeManager = browsingModeManager,
            toolbarStore = toolbarStore,
            mainDispatcher = testDispatcher,
        ).start()
        testScheduler.advanceUntilIdle()

        toolbarStore.dispatch(BrowserToolbarAction.EnterEditMode(isPrivate = false))
        testScheduler.advanceUntilIdle()

        assertEquals(
            ContextCompat.getColor(activity, R.color.homepage_tab_edge_to_edge_toolbar_background),
            (decorView.getChildAt(initialChildCount).background as ColorDrawable).color,
        )
    }
}
