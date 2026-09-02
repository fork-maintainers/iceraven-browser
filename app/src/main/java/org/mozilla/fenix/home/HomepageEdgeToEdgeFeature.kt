/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarState
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarStore
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.base.feature.LifecycleAwareFeature
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.utils.Settings
import org.mozilla.fenix.wallpapers.Wallpaper
import com.google.android.material.R as materialR

/**
 * Feature responsible for managing window insets, background styling, and toolbar visibility
 * during the home screen lifecycle, when edge to edge background is enabled.
 *
 * @param appStore [AppStore] used for querying and updating application state.
 * @param activity The activity containing the window to manage.
 * @param settings The [Settings] used to determine the current position of the toolbar.
 * @param browsingModeManager The [BrowsingModeManager] used to determine the current browsing mode.
 * @param toolbarStore The [BrowserToolbarStore] which state is observed to manage status bar background in edit mode.
 * @param mainDispatcher The [CoroutineDispatcher] used for main thread operations.
 */
class HomepageEdgeToEdgeFeature(
    private val appStore: AppStore,
    private val activity: Activity,
    private val settings: Settings,
    private val browsingModeManager: BrowsingModeManager,
    private val toolbarStore: BrowserToolbarStore,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) : LifecycleAwareFeature {

    private var backgroundView: View? = null
    private var statusBarHeight: Int = 0
    private var shouldApplyEdgeToEdgeBackgroundOnNextInsets = false
    private var toolbarScope: CoroutineScope? = null
    private var wallpaperScope: CoroutineScope? = null

    override fun start() {
        if (!settings.enableHomepageEdgeToEdgeBackgroundFeature) {
            return
        }

        observeWallpaperUpdates()
    }

    private fun observeWallpaperUpdates() {
        wallpaperScope = appStore.flowScoped(dispatcher = mainDispatcher) { flow ->
            flow.map { state -> state.wallpaperState.currentWallpaper }
                .distinctUntilChanged()
                .collect { wallpaper ->
                    setWallpaper(wallpaper)
                }
        }
    }

    override fun stop() {
        removeEdgeToEdgeComponents()
        wallpaperScope?.cancel()
        wallpaperScope = null
    }

    private fun setWallpaper(wallpaper: Wallpaper) {
        if (wallpaper == Wallpaper.EdgeToEdge) {
            if (setupStatusBarBackground()) {
                setBackground(Background.HomeEdgeToEdge)
            }
        } else {
            removeEdgeToEdgeComponents()
        }
    }

    private fun removeEdgeToEdgeComponents() {
        setBackground(Background.Regular)
        activity.window?.decorView?.let { decorView ->
            (decorView as? ViewGroup)?.apply {
                backgroundView?.let { view ->
                    this@apply.removeView(view)
                }
            }
        }
        shouldApplyEdgeToEdgeBackgroundOnNextInsets = false
        toolbarScope?.cancel()
        toolbarScope = null
        backgroundView = null
    }

    private fun setBackground(background: Background) {
        val isPrivateMode = browsingModeManager.mode == BrowsingMode.Private
        activity.window?.setBackgroundDrawableResource(
            if (isPrivateMode) R.color.fx_mobile_private_surface else background.resourceId,
        )
    }

    /**
     * Sets up a dynamic status bar background view that changes color based on toolbar state.
     *
     * This view is added directly to the DecorView (the root view of the activity window) to ensure
     * it sits behind all other content but can still receive window insets.
     *
     * @return true if the status bar height is already known and the edge-to-edge background
     * can be applied immediately, false if it must be deferred until window insets are received.
     */
    private fun setupStatusBarBackground(): Boolean {
        val rootView = activity.window?.decorView as? ViewGroup ?: return false

        val rootInsetsTop = ViewCompat.getRootWindowInsets(rootView)
            ?.getInsets(WindowInsetsCompat.Type.statusBars())
            ?.top
        if (rootInsetsTop != null) {
            statusBarHeight = rootInsetsTop
        }

        val shouldApplyBackgroundImmediately = statusBarHeight > 0
        shouldApplyEdgeToEdgeBackgroundOnNextInsets = !shouldApplyBackgroundImmediately

        backgroundView = View(activity).apply {
            id = View.generateViewId()
            setBackgroundColor(getStatusBarColor(settings, toolbarStore.state))

            val params = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                gravity = Gravity.TOP
                height = statusBarHeight
            }
            rootView.addView(this, params)

            ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
                applyStatusBarInsetsAndBackground(this, insets)
                insets
            }
        }
        if (shouldApplyEdgeToEdgeBackgroundOnNextInsets) {
            backgroundView?.let(ViewCompat::requestApplyInsets)
        }

        if (toolbarScope == null) {
            toolbarScope = toolbarStore.flowScoped(dispatcher = mainDispatcher) { flow ->
                flow.collect { toolbarState ->
                    backgroundView?.setBackgroundColor(getStatusBarColor(settings, toolbarState))
                }
            }
        }
        return shouldApplyBackgroundImmediately
    }

    private fun applyStatusBarInsetsAndBackground(view: View, insets: WindowInsetsCompat) {
        statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        view.layoutParams.height = statusBarHeight
        view.requestLayout()
        if (shouldApplyEdgeToEdgeBackgroundOnNextInsets && statusBarHeight > 0) {
            setBackground(Background.HomeEdgeToEdge)
            shouldApplyEdgeToEdgeBackgroundOnNextInsets = false
        }
    }

    private fun getStatusBarColor(settings: Settings, toolbarState: BrowserToolbarState): Int {
        val shouldShow = !settings.shouldUseBottomToolbar || toolbarState.isShowingResultsScreen
        val isPrivateMode = browsingModeManager.mode == BrowsingMode.Private

        return when {
            !shouldShow -> Color.TRANSPARENT
            isPrivateMode -> ContextCompat.getColor(activity, R.color.fx_mobile_private_surface)
            !settings.isTabStripEnabled &&
                toolbarState.isShowingResultsScreen && browsingModeManager.mode == BrowsingMode.Normal &&
                (
                    settings.enableHomepageTrendingRecentSearch ||
                    toolbarState.editState.query.current.isNotEmpty() ||
                    toolbarState.editState.queryWasPrefilled
                ) ->
                MaterialColors.getColor(
                    activity,
                    materialR.attr.colorSurface,
                    "Could not resolve color",
                )
            else -> ContextCompat.getColor(activity, R.color.homepage_tab_edge_to_edge_toolbar_background)
        }
    }

    private val BrowserToolbarState.isShowingResultsScreen: Boolean
        get() = isEditMode()

    /**
     * Enum representing the available background drawable resources.
     */
    enum class Background(val resourceId: Int) {
        Regular(R.color.fx_mobile_surface),
        HomeEdgeToEdge(R.drawable.home_background_gradient),
    }
}
