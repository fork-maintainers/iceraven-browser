/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.historymetadata.controller

import android.content.Context
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import mozilla.components.browser.state.action.HistoryMetadataAction
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import mozilla.components.concept.engine.prompt.ShareData
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.telemetry.glean.private.NoExtras
import org.mozilla.fenix.R
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.share.ShareSheetChooserAction
import org.mozilla.fenix.components.share.ShareSource
import org.mozilla.fenix.components.usecases.FenixBrowserUseCases
import org.mozilla.fenix.components.usecases.ShareUseCases
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.library.history.History
import org.mozilla.fenix.library.history.toPendingDeletionHistory
import org.mozilla.fenix.library.historymetadata.HistoryMetadataGroupFragment.DeleteAllConfirmationDialogFragment
import org.mozilla.fenix.library.historymetadata.HistoryMetadataGroupFragmentAction
import org.mozilla.fenix.library.historymetadata.HistoryMetadataGroupFragmentDirections
import org.mozilla.fenix.library.historymetadata.HistoryMetadataGroupFragmentStore
import org.mozilla.fenix.utils.Settings
import org.mozilla.fenix.GleanMetrics.History as GleanHistory

/**
 * An interface that handles the view manipulation of the history metadata group in the History
 * metadata group screen.
 */
interface HistoryMetadataGroupController {

    /**
     * Opens the given history [item] in a new tab.
     *
     * @param item The [History] to open in a new tab.
     */
    fun handleOpen(item: History.Metadata)

    /**
     * Toggles the given history [item] to be selected in multi-select mode.
     *
     * @param item The [History] to select.
     */
    fun handleSelect(item: History.Metadata)

    /**
     * Toggles the given history [item] to be deselected in multi-select mode.
     *
     * @param item The [History] to deselect.
     */
    fun handleDeselect(item: History.Metadata)

    /**
     * Called on backpressed to deselect all the given [items].
     *
     * @param items The set of [History]s to deselect.
     */
    fun handleBackPressed(items: Set<History.Metadata>): Boolean

    /**
     * Opens the share sheet for a set of history [items].
     *
     * @param items The set of [History]s to share.
     */
    fun handleShare(items: Set<History.Metadata>)

    /**
     * Deletes the given history metadata [items] from storage.
     */
    fun handleDelete(items: Set<History.Metadata>)

    /**
     * Displays a [DeleteAllConfirmationDialogFragment] prompt.
     */
    fun handleDeleteAll()

    /**
     * Deletes history metadata items in this group.
     */
    fun handleDeleteAllConfirmed()
}

/**
 * The default implementation of [HistoryMetadataGroupController].
 */
@Suppress("LongParameterList")
class DefaultHistoryMetadataGroupController(
    private val historyStorage: PlacesHistoryStorage,
    private val browserStore: BrowserStore,
    private val appStore: AppStore,
    private val store: HistoryMetadataGroupFragmentStore,
    private val selectOrAddUseCase: TabsUseCases.SelectOrAddUseCase,
    private val fenixBrowserUseCases: FenixBrowserUseCases,
    private val navController: NavController,
    private val settings: Settings,
    private val shareUseCases: ShareUseCases,
    private val scope: CoroutineScope,
    private val searchTerm: String,
    private val deleteSnackbar: (
        items: Set<History.Metadata>,
        undo: suspend (Set<History.Metadata>) -> Unit,
        delete: (Set<History.Metadata>) -> suspend (context: Context) -> Unit,
    ) -> Unit,
    private val promptDeleteAll: () -> Unit,
    private val allDeletedSnackbar: () -> Unit,
) : HistoryMetadataGroupController {

    override fun handleOpen(item: History.Metadata) {
        if (settings.enableHomepageAsNewTab) {
            fenixBrowserUseCases.loadUrlOrSearch(
                searchTermOrURL = item.url,
                newTab = false,
                private = appStore.state.mode.isPrivate,
            )
        } else {
            selectOrAddUseCase.invoke(item.url, item.historyMetadataKey)
        }

        navController.navigate(R.id.browserFragment)
        GleanHistory.searchTermGroupOpenTab.record(NoExtras())
    }

    override fun handleSelect(item: History.Metadata) {
        store.dispatch(HistoryMetadataGroupFragmentAction.Select(item))
    }

    override fun handleDeselect(item: History.Metadata) {
        store.dispatch(HistoryMetadataGroupFragmentAction.Deselect(item))
    }

    override fun handleBackPressed(items: Set<History.Metadata>): Boolean {
        return if (items.isNotEmpty()) {
            store.dispatch(HistoryMetadataGroupFragmentAction.DeselectAll)
            true
        } else {
            false
        }
    }

    override fun handleShare(items: Set<History.Metadata>) {
        val shareData = items.map { ShareData(url = it.url, title = it.title) }

        shareUseCases.shareItems(
            items = shareData,
            source = ShareSource.HISTORY_METADATA_GROUP,
            chooserActions = if (items.size == 1) {
                listOf(ShareSheetChooserAction.SEND_TO_DEVICES, ShareSheetChooserAction.QR_CODE)
            } else {
                listOf(ShareSheetChooserAction.SEND_TO_DEVICES)
            },
            navigateToShareFragment = {
                navController.navigate(
                    HistoryMetadataGroupFragmentDirections.actionGlobalShareFragment(
                        data = shareData.toTypedArray(),
                    ),
                )
            },
        )
    }

    override fun handleDelete(items: Set<History.Metadata>) {
        val pendingDeletionItems = items.map { it.toPendingDeletionHistory() }.toSet()
        appStore.dispatch(AppAction.AddPendingDeletionSet(pendingDeletionItems))
        deleteSnackbar.invoke(items, ::undo, ::delete)
    }

    private fun undo(items: Set<History.Metadata>) {
        val pendingDeletionItems = items.map { it.toPendingDeletionHistory() }.toSet()
        appStore.dispatch(AppAction.UndoPendingDeletionSet(pendingDeletionItems))
    }

    private fun delete(items: Set<History.Metadata>): suspend (context: Context) -> Unit {
        return { context ->
            scope.launch {
                val isDeletingLastItem = items.containsAll(store.state.items)
                items.forEach {
                    store.dispatch(HistoryMetadataGroupFragmentAction.Delete(it))
                    context.components.core.historyStorage.deleteVisitsFor(it.url)
                    GleanHistory.searchTermGroupRemoveTab.record(NoExtras())
                }
                // The method is called for both single and multiple items.
                // In case all items have been deleted, we have to disband the search group.
                if (isDeletingLastItem) {
                    context.components.core.store.dispatch(
                        HistoryMetadataAction.DisbandSearchGroupAction(searchTerm = searchTerm),
                    )
                }
            }
        }
    }

    override fun handleDeleteAll() {
        promptDeleteAll.invoke()
    }

    override fun handleDeleteAllConfirmed() {
        scope.launch {
            store.state.items.forEach {
                historyStorage.deleteVisitsFor(it.url)
            }
            store.dispatch(HistoryMetadataGroupFragmentAction.DeleteAll)
            browserStore.dispatch(
                HistoryMetadataAction.DisbandSearchGroupAction(searchTerm = searchTerm),
            )
            GleanHistory.searchTermGroupRemoveAll.record(NoExtras())
            allDeletedSnackbar.invoke()
            launch(Main) {
                navController.popBackStack(R.id.historyFragment, false)
            }
        }
    }
}
