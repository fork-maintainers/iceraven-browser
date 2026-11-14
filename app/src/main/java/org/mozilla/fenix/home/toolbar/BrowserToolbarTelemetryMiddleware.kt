/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.toolbar

import androidx.annotation.VisibleForTesting
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarAction
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarInteraction.BrowserToolbarEvent.Source
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarState
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.Store
import org.mozilla.fenix.GleanMetrics.Toolbar
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.home.toolbar.DisplayActions.MenuClicked
import org.mozilla.fenix.home.toolbar.TabCounterInteractions.AddNewPrivateTab
import org.mozilla.fenix.home.toolbar.TabCounterInteractions.AddNewTab
import org.mozilla.fenix.home.toolbar.TabCounterInteractions.TabCounterClicked
import org.mozilla.fenix.home.toolbar.TabCounterInteractions.TabCounterLongClicked
import org.mozilla.fenix.search.EditPageEndActionsInteractions.LensButtonClicked
import org.mozilla.fenix.search.EditPageEndActionsInteractions.QrScannerClicked
import org.mozilla.fenix.telemetry.ACTION_ADD_NEW_PRIVATE_TAB
import org.mozilla.fenix.telemetry.ACTION_ADD_NEW_TAB
import org.mozilla.fenix.telemetry.ACTION_MENU_CLICKED
import org.mozilla.fenix.telemetry.ACTION_QR_CLICKED
import org.mozilla.fenix.telemetry.ACTION_TAB_COUNTER_CLICKED
import org.mozilla.fenix.telemetry.ACTION_TAB_COUNTER_LONG_CLICKED
import org.mozilla.fenix.telemetry.SOURCE_ADDRESS_BAR
import org.mozilla.fenix.telemetry.SOURCE_BROWSER_END
import org.mozilla.fenix.telemetry.SOURCE_BROWSER_START
import org.mozilla.fenix.telemetry.SOURCE_NAVIGATION_BAR
import org.mozilla.fenix.telemetry.SOURCE_PAGE_END
import org.mozilla.fenix.telemetry.SOURCE_PAGE_START
import org.mozilla.fenix.telemetry.SURFACE_HOME
import org.mozilla.fenix.telemetry.recordAddressBarButtonTapped
import org.mozilla.fenix.telemetry.recordLensButtonTapped
import org.mozilla.fenix.utils.Settings

/**
 * [Middleware] responsible for recording telemetry of actions triggered by compose toolbars.
 *
 * @param appStore [AppStore] used to determine the surface on which the search was started.
 * @param settings [Settings] used to determine the last selected Lens camera mode.
 */
class BrowserToolbarTelemetryMiddleware(
    private val appStore: AppStore,
    private val settings: Settings,
) : Middleware<BrowserToolbarState, BrowserToolbarAction> {
    private val sourceTabId: String?
        get() = appStore.state.searchState.sourceTabId

    override fun invoke(
        store: Store<BrowserToolbarState, BrowserToolbarAction>,
        next: (BrowserToolbarAction) -> Unit,
        action: BrowserToolbarAction,
    ) {
        when (action) {
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
            is MenuClicked -> {
                trackToolbarEvent(ToolbarActionRecord.MenuClicked, action.source)
            }
            is QrScannerClicked -> recordAddressBarButtonTapped(ACTION_QR_CLICKED, sourceTabId)
            is LensButtonClicked -> recordLensButtonTapped(settings.lensCameraLastMode, sourceTabId)
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
                        surface = SURFACE_HOME,
                    )
                )

            Source.NavigationBar ->
                Toolbar.buttonTapped.record(
                    Toolbar.ButtonTappedExtra(
                        source = SOURCE_NAVIGATION_BAR,
                        item = toolbarActionRecord.action,
                        surface = SURFACE_HOME,
                    )
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
