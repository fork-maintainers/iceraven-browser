/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
import androidx.core.view.isVisible
import mozilla.components.compose.browser.toolbar.NavigationBar
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarStore
import mozilla.components.compose.browser.toolbar.store.ToolbarGravity.Bottom
import mozilla.components.compose.browser.toolbar.store.ToolbarGravity.Top
import mozilla.components.support.utils.KeyboardState
import mozilla.components.support.utils.keyboardAsState
import org.mozilla.fenix.R
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.utils.Settings

private const val NAVBAR_VISIBILITY_ANIMATION_MS = 150

/**
 * A wrapper over the [NavigationBar] composable that provides enhanced customization and lifecycle-aware integration.
 *
 * @param context [Context] used to access resources and other application-level operations.
 * @param container [ViewGroup] which will serve as parent of this View.
 * @param toolbarStore [BrowserToolbarStore] containing the navigation bar state.
 * @param settings [Settings] object to get the toolbar position and other settings.
 * @param customTabSessionId session ID of the custom tab in which the navigation bar is shown.
 * @param hideWhenKeyboardShown If true, navigation bar will be hidden when the keyboard is visible.
 * @param tabStripContent Composable content for the tab strip when shown together with the navigation bar.
 * @param tabGroupsStripContent Composable content for the tab groups strip when shown together with the navigation bar.
 */
@Suppress("LongParameterList")
class BrowserNavigationBar(
    private val context: Context,
    private val container: ViewGroup,
    private val toolbarStore: BrowserToolbarStore,
    private val settings: Settings,
    private val customTabSessionId: String? = null,
    private val hideWhenKeyboardShown: Boolean,
    private val tabStripContent: () -> @Composable () -> Unit,
    private val tabGroupsStripContent: () -> @Composable () -> Unit,
) {
    val layout =
        NavigationBarComposeView(context) {
                val shouldShowTabStrip = remember {
                    customTabSessionId == null && settings.shouldShowTabStripAtBottom
                }
                val shouldShowTabGroupsStrip = remember {
                    customTabSessionId == null && settings.shouldShowTabGroupsStrip
                }
                val hasBottomStrips = remember { hasBottomStrip() }

                FirefoxTheme {
                    TopDividerColumn(
                        showDivider = hasBottomStrips,
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                    ) {
                        if (shouldShowTabStrip) {
                            tabStripContent().invoke()
                        }
                        if (shouldShowTabGroupsStrip) {
                            tabGroupsStripContent().invoke()
                        }
                        DefaultNavigationBarContent()
                    }
                }
            }
            .apply {
                id = R.id.navigation_bar
                // Add this to the container only if it is to be shown on it's own, at the bottom of the screen.
                // If the toolbar is at the bottom the navigation bar is composed inside the toolbar's own View
                // - see [asComposable] - so this must never become a child of the container.
                if (!settings.shouldUseBottomToolbar) {
                    addToParent(this)
                    setNavbarDynamicBehavior(this)
                }
            }

    /** Returns a [Composable] function that renders the default navigation bar content. */
    fun asComposable(): @Composable () -> Unit = {
        DefaultNavigationBarContent()
    }

    internal fun gone() {
        layout.isVisible = false
    }

    internal fun visible() {
        layout.isVisible = true
    }

    @Composable
    private fun DefaultNavigationBarContent() {
        val uiState by toolbarStore.stateFlow.collectAsState()
        val toolbarGravity =
            remember(settings) {
                when (settings.shouldUseBottomToolbar) {
                    true -> Bottom
                    false -> Top
                }
            }
        val hasBottomStrip = remember { hasBottomStrip() }
        val isKeyboardVisible =
            if (hideWhenKeyboardShown) {
                val keyboardState by keyboardAsState()
                keyboardState == KeyboardState.Opened
            } else {
                false
            }

        if (uiState.displayState.navigationActions.isNotEmpty()) {
            AnimatedVisibility(
                visible = !isKeyboardVisible,
                enter =
                    expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = tween(durationMillis = NAVBAR_VISIBILITY_ANIMATION_MS),
                    ),
                exit =
                    shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = tween(durationMillis = NAVBAR_VISIBILITY_ANIMATION_MS),
                    ),
            ) {
                FirefoxTheme {
                    NavigationBar(
                        actions = uiState.displayState.navigationActions,
                        toolbarGravity = toolbarGravity,
                        showDivider = toolbarGravity == Top && !hasBottomStrip,
                        onInteraction = { toolbarStore.dispatch(it) },
                    )
                }
            }
        }
    }

    private fun hasBottomStrip() =
        customTabSessionId == null && (settings.shouldShowTabStripAtBottom || settings.shouldShowTabGroupsStrip)

    private fun addToParent(view: NavigationBarComposeView) {
        container.addView(
            view,
            LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT,
                )
                .apply {
                    gravity = Gravity.BOTTOM
                },
        )
    }

    private fun setNavbarDynamicBehavior(view: NavigationBarComposeView) {
        if (settings.isDynamicToolbarEnabled) {
            (view.layoutParams as LayoutParams).apply {
                behavior = NavbarToolbarSyncBehavior(context)
            }
        }
    }
}
