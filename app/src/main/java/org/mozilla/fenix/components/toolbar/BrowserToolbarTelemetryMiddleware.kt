/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import androidx.annotation.VisibleForTesting
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarAction
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarInteraction.BrowserToolbarEvent.Source
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarState
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.Store
import org.mozilla.fenix.GleanMetrics.Toolbar
import org.mozilla.fenix.components.toolbar.DisplayActions.AddBookmarkClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.EditBookmarkClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.EditShortcutClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.HomepageClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.MenuClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.NavigateBackClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.NavigateBackLongClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.NavigateForwardClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.NavigateForwardLongClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.RefreshClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.ShareClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.ShortcutLongClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.StopRefreshClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.SummarizeClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.TranslateClicked
import org.mozilla.fenix.components.toolbar.PageEndActionsInteractions.ReaderModeClicked
import org.mozilla.fenix.components.toolbar.StartPageActions.SiteInfoClicked
import org.mozilla.fenix.components.toolbar.TabCounterInteractions.AddNewPrivateTab
import org.mozilla.fenix.components.toolbar.TabCounterInteractions.AddNewTab
import org.mozilla.fenix.components.toolbar.TabCounterInteractions.TabCounterClicked
import org.mozilla.fenix.components.toolbar.TabCounterInteractions.TabCounterLongClicked
import org.mozilla.fenix.telemetry.ACTION_ADD_BOOKMARK_CLICKED
import org.mozilla.fenix.telemetry.ACTION_ADD_NEW_PRIVATE_TAB
import org.mozilla.fenix.telemetry.ACTION_ADD_NEW_TAB
import org.mozilla.fenix.telemetry.ACTION_EDIT_BOOKMARK_CLICKED
import org.mozilla.fenix.telemetry.ACTION_EDIT_SHORTCUT_CLICKED
import org.mozilla.fenix.telemetry.ACTION_HOME_CLICKED
import org.mozilla.fenix.telemetry.ACTION_MENU_CLICKED
import org.mozilla.fenix.telemetry.ACTION_NAVIGATE_BACK_CLICKED
import org.mozilla.fenix.telemetry.ACTION_NAVIGATE_BACK_LONG_CLICKED
import org.mozilla.fenix.telemetry.ACTION_NAVIGATE_FORWARD_CLICKED
import org.mozilla.fenix.telemetry.ACTION_NAVIGATE_FORWARD_LONG_CLICKED
import org.mozilla.fenix.telemetry.ACTION_READER_MODE_CLICKED
import org.mozilla.fenix.telemetry.ACTION_REFRESH_CLICKED
import org.mozilla.fenix.telemetry.ACTION_SECURITY_INDICATOR_CLICKED
import org.mozilla.fenix.telemetry.ACTION_SHARE_CLICKED
import org.mozilla.fenix.telemetry.ACTION_SHORTCUT_LONG_CLICKED
import org.mozilla.fenix.telemetry.ACTION_STOP_CLICKED
import org.mozilla.fenix.telemetry.ACTION_SUMMARIZE_CLICKED
import org.mozilla.fenix.telemetry.ACTION_TAB_COUNTER_CLICKED
import org.mozilla.fenix.telemetry.ACTION_TAB_COUNTER_LONG_CLICKED
import org.mozilla.fenix.telemetry.ACTION_TRANSLATE_CLICKED
import org.mozilla.fenix.telemetry.SOURCE_ADDRESS_BAR
import org.mozilla.fenix.telemetry.SOURCE_BROWSER_END
import org.mozilla.fenix.telemetry.SOURCE_BROWSER_START
import org.mozilla.fenix.telemetry.SOURCE_NAVIGATION_BAR
import org.mozilla.fenix.telemetry.SOURCE_PAGE_END
import org.mozilla.fenix.telemetry.SOURCE_PAGE_START
import org.mozilla.fenix.telemetry.SURFACE_BROWSER

/**
 * [Middleware] responsible for recording telemetry of actions triggered by compose toolbars.
 */
class BrowserToolbarTelemetryMiddleware : Middleware<BrowserToolbarState, BrowserToolbarAction> {
    @Suppress("CyclomaticComplexMethod")
    override fun invoke(
        store: Store<BrowserToolbarState, BrowserToolbarAction>,
        next: (BrowserToolbarAction) -> Unit,
        action: BrowserToolbarAction,
    ) {
        when (action) {
            is MenuClicked -> {
                trackToolbarEvent(ToolbarActionRecord.MenuClicked, action.source)
            }
            is TabCounterClicked -> {
                trackToolbarEvent(ToolbarActionRecord.TabCounterClicked, action.source)
            }
            is TabCounterLongClicked -> {
                trackToolbarEvent(ToolbarActionRecord.TabCounterLongClicked, action.source)
            }
            is AddNewTab -> {
                trackToolbarEvent(ToolbarActionRecord.AddNewTab, action.source)
            }
            is AddNewPrivateTab -> {
                trackToolbarEvent(ToolbarActionRecord.AddNewPrivateTab, action.source)
            }
            is NavigateBackClicked -> {
                trackToolbarEvent(ToolbarActionRecord.NavigateBackClicked, action.source)
            }
            is NavigateBackLongClicked -> {
                trackToolbarEvent(ToolbarActionRecord.NavigateBackLongClicked, action.source)
            }
            is NavigateForwardClicked -> {
                trackToolbarEvent(ToolbarActionRecord.NavigateForwardClicked, action.source)
            }
            is NavigateForwardLongClicked -> {
                trackToolbarEvent(ToolbarActionRecord.NavigateForwardLongClicked, action.source)
            }
            is RefreshClicked -> {
                trackToolbarEvent(ToolbarActionRecord.RefreshClicked, action.source)
            }
            is StopRefreshClicked -> {
                trackToolbarEvent(ToolbarActionRecord.StopRefreshClicked, action.source)
            }
            is AddBookmarkClicked -> {
                trackToolbarEvent(ToolbarActionRecord.AddBookmarkClicked, action.source)
            }
            is EditBookmarkClicked -> {
                trackToolbarEvent(ToolbarActionRecord.EditBookmarkClicked, action.source)
            }
            is ShareClicked -> {
                trackToolbarEvent(ToolbarActionRecord.ShareClicked, action.source)
            }
            is ReaderModeClicked -> {
                trackToolbarEvent(ToolbarActionRecord.ReaderModeClicked, action.source)
            }
            is TranslateClicked -> {
                trackToolbarEvent(ToolbarActionRecord.TranslateClicked, action.source)
            }
            is HomepageClicked -> {
                trackToolbarEvent(ToolbarActionRecord.HomepageClicked, action.source)
            }
            is SiteInfoClicked -> {
                trackToolbarEvent(ToolbarActionRecord.SecurityIndicatorClicked, action.source)
            }
            is SummarizeClicked -> {
                trackToolbarEvent(ToolbarActionRecord.SummarizeClicked, action.source)
            }
            is ShortcutLongClicked -> {
                trackToolbarEvent(ToolbarActionRecord.ShortcutLongClicked, action.source)
            }
            is EditShortcutClicked -> {
                trackToolbarEvent(ToolbarActionRecord.EditShortcutClicked, action.source)
            }
            else -> {}
        }

        next(action)
    }

    @VisibleForTesting
    internal sealed class ToolbarActionRecord(val action: String) {
        data object MenuClicked : ToolbarActionRecord(ACTION_MENU_CLICKED)
        data object TabCounterClicked : ToolbarActionRecord(ACTION_TAB_COUNTER_CLICKED)
        data object TabCounterLongClicked : ToolbarActionRecord(ACTION_TAB_COUNTER_LONG_CLICKED)
        data object AddNewTab : ToolbarActionRecord(ACTION_ADD_NEW_TAB)
        data object AddNewPrivateTab : ToolbarActionRecord(ACTION_ADD_NEW_PRIVATE_TAB)
        data object NavigateBackClicked : ToolbarActionRecord(ACTION_NAVIGATE_BACK_CLICKED)
        data object NavigateBackLongClicked : ToolbarActionRecord(ACTION_NAVIGATE_BACK_LONG_CLICKED)
        data object NavigateForwardClicked : ToolbarActionRecord(ACTION_NAVIGATE_FORWARD_CLICKED)
        data object NavigateForwardLongClicked : ToolbarActionRecord(ACTION_NAVIGATE_FORWARD_LONG_CLICKED)
        data object RefreshClicked : ToolbarActionRecord(ACTION_REFRESH_CLICKED)
        data object StopRefreshClicked : ToolbarActionRecord(ACTION_STOP_CLICKED)
        data object AddBookmarkClicked : ToolbarActionRecord(ACTION_ADD_BOOKMARK_CLICKED)
        data object EditBookmarkClicked : ToolbarActionRecord(ACTION_EDIT_BOOKMARK_CLICKED)
        data object ShareClicked : ToolbarActionRecord(ACTION_SHARE_CLICKED)
        data object ReaderModeClicked : ToolbarActionRecord(ACTION_READER_MODE_CLICKED)
        data object TranslateClicked : ToolbarActionRecord(ACTION_TRANSLATE_CLICKED)
        data object HomepageClicked : ToolbarActionRecord(ACTION_HOME_CLICKED)
        data object SecurityIndicatorClicked : ToolbarActionRecord(ACTION_SECURITY_INDICATOR_CLICKED)
        data object SummarizeClicked : ToolbarActionRecord(ACTION_SUMMARIZE_CLICKED)
        data object ShortcutLongClicked : ToolbarActionRecord(ACTION_SHORTCUT_LONG_CLICKED)
        data object EditShortcutClicked : ToolbarActionRecord(ACTION_EDIT_SHORTCUT_CLICKED)
    }

    private fun trackToolbarEvent(
        toolbarActionRecord: ToolbarActionRecord,
        source: Source = Source.Unknown,
    ) {
        when (source) {
            is Source.AddressBar ->
                Toolbar.buttonTapped.record(
                    Toolbar.ButtonTappedExtra(
                        source = SOURCE_ADDRESS_BAR,
                        item = toolbarActionRecord.action,
                        extra = source.telemetryName(),
                        surface = SURFACE_BROWSER,
                    ),
                )

            Source.NavigationBar ->
                Toolbar.buttonTapped.record(
                    Toolbar.ButtonTappedExtra(
                        source = SOURCE_NAVIGATION_BAR,
                        item = toolbarActionRecord.action,
                        surface = SURFACE_BROWSER,
                    ),
                )

            Source.Unknown -> return
        }
    }
}

internal fun Source.AddressBar.telemetryName(): String =
    when (this) {
        Source.AddressBar.BrowserStart -> SOURCE_BROWSER_START
        Source.AddressBar.PageStart -> SOURCE_PAGE_START
        Source.AddressBar.PageEnd -> SOURCE_PAGE_END
        Source.AddressBar.BrowserEnd -> SOURCE_BROWSER_END
    }
