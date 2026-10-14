/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.search

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarStore
import mozilla.components.compose.browser.toolbar.store.Mode
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.Store
import mozilla.components.lib.state.ext.flow
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager
import org.mozilla.fenix.search.SearchFragmentAction.Init
import org.mozilla.fenix.search.SearchFragmentAction.SearchStarted

/**
 * [SearchFragmentStore] [Middleware] to synchronize search related details from [BrowserToolbarStore].
 *
 * @param toolbarStore The [BrowserToolbarStore] to sync from.
 * @param browsingModeManager [BrowsingModeManager] for querying the current browsing mode.
 * @param scope [CoroutineScope] used for running long running operations in background.
 * @param browserStore The [BrowserStore] to sync from.
 */
class BrowserToolbarToFenixSearchMapperMiddleware(
    private val toolbarStore: BrowserToolbarStore,
    private val browsingModeManager: BrowsingModeManager,
    private val scope: CoroutineScope,
    private val browserStore: BrowserStore? = null,
) : Middleware<SearchFragmentState, SearchFragmentAction> {
    private var syncSearchStartedJob: Job? = null
    private var syncSearchQueryJob: Job? = null

    override fun invoke(
        store: Store<SearchFragmentState, SearchFragmentAction>,
        next: (SearchFragmentAction) -> Unit,
        action: SearchFragmentAction,
    ) {
        next(action)

        if (action is Init) {
            syncSearchStatus(store)

            if (toolbarStore.state.isEditMode()) {
                syncUserQuery(store)
            }
        }
    }

    private fun syncSearchStatus(store: Store<SearchFragmentState, SearchFragmentAction>) {
        syncSearchStartedJob = scope.launch {
            toolbarStore
                .flow()
                .distinctUntilChangedBy { it.mode }
                .collect {
                    if (it.mode == Mode.EDIT) {
                        val editState = toolbarStore.state.editState
                        store.dispatch(
                            SearchStarted(
                                selectedSearchEngine = null,
                                isUserSelected = true,
                                inPrivateMode = browsingModeManager.mode.isPrivate,
                                searchStartedForCurrentUrl =
                                    editState.isQueryPrefilled &&
                                        isCurrentUrl(
                                            editState.query.current,
                                            browserStore?.state?.selectedTab?.content?.url,
                                        ),
                            )
                        )

                        syncUserQuery(store)
                    } else {
                        stopSyncingUserQuery()
                    }
                }
        }
    }

    private fun syncUserQuery(store: Store<SearchFragmentState, SearchFragmentAction>) {
        syncSearchQueryJob?.cancel()
        syncSearchQueryJob = scope.launch {
            toolbarStore
                .flow()
                .map { it.editState.query }
                .distinctUntilChanged()
                .collect { query ->
                    val isSearchStartedForCurrentUrl = store.state.searchStartedForCurrentUrl
                    val isQueryPrefilled = toolbarStore.state.editState.isQueryPrefilled
                    store.dispatch(
                        SearchFragmentAction.UpdateQuery(
                            when (isSearchStartedForCurrentUrl && isQueryPrefilled) {
                                true -> "" // consider a prefilled query for the current URL as not entered by user
                                false -> query.current
                            }
                        )
                    )
                }
        }
    }

    private fun stopSyncingUserQuery() {
        syncSearchQueryJob?.cancel()
    }
}
