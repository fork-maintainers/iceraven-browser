/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.search.awesomebar

import android.net.Uri
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.state.searchEngines
import mozilla.components.concept.awesomebar.AwesomeBar
import mozilla.components.concept.engine.Engine
import mozilla.components.feature.awesomebar.provider.BookmarksStorageSuggestionProvider
import mozilla.components.feature.awesomebar.provider.CombinedHistorySuggestionProvider
import mozilla.components.feature.awesomebar.provider.DEFAULT_RECENT_SEARCH_SUGGESTION_LIMIT
import mozilla.components.feature.awesomebar.provider.FlightsOnlineSuggestionProvider
import mozilla.components.feature.awesomebar.provider.HistoryStorageSuggestionProvider
import mozilla.components.feature.awesomebar.provider.RecentSearchSuggestionsProvider
import mozilla.components.feature.awesomebar.provider.SearchActionProvider
import mozilla.components.feature.awesomebar.provider.SearchEngineSuggestionProvider
import mozilla.components.feature.awesomebar.provider.SearchSuggestionProvider
import mozilla.components.feature.awesomebar.provider.SearchTermSuggestionsProvider
import mozilla.components.feature.awesomebar.provider.SessionSuggestionProvider
import mozilla.components.feature.awesomebar.provider.SportsOnlineSuggestionProvider
import mozilla.components.feature.awesomebar.provider.StocksOnlineSuggestionProvider
import mozilla.components.feature.awesomebar.provider.TrendingSearchProvider
import mozilla.components.feature.fxsuggest.FxSuggestSuggestionProvider
import mozilla.components.feature.fxsuggest.datasource.CombinedOnlineSuggestionDataSource
import mozilla.components.feature.search.SearchUseCases
import mozilla.components.feature.session.SessionUseCases.LoadUrlUseCase
import mozilla.components.feature.syncedtabs.DeviceIndicators
import mozilla.components.feature.syncedtabs.SyncedTabsStorageSuggestionProvider
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.support.ktx.android.net.sameHostWithoutMobileSubdomainAs
import mozilla.components.support.ktx.kotlin.tryGetHostFromUrl
import mozilla.components.support.ktx.kotlin.urlContainsQueryParameters
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager
import org.mozilla.fenix.components.Components
import org.mozilla.fenix.components.Core.Companion.METADATA_HISTORY_SUGGESTION_LIMIT
import org.mozilla.fenix.components.Core.Companion.METADATA_SHORTCUT_SUGGESTION_LIMIT
import org.mozilla.fenix.ext.containsQueryParameters
import org.mozilla.fenix.search.SearchEngineSource

/**
 * View that contains and configures the BrowserAwesomeBar
 */
@Suppress("LongParameterList")
class SearchSuggestionsProvidersBuilder(
    private val components: Components,
    private val scope: CoroutineScope,
    private val browsingModeManager: BrowsingModeManager,
    private val includeSelectedTab: Boolean,
    private val loadUrlUseCase: LoadUrlUseCase,
    private val searchUseCase: SearchUseCases.SearchUseCase,
    private val selectTabUseCase: TabsUseCases.SelectTabUseCase,
    private val suggestionsStringsProvider: SuggestionsStringsProvider,
    private val suggestionIconProvider: SuggestionIconProvider,
    onSearchEngineSuggestionSelected: (searchEngine: SearchEngine) -> Unit,
) {
    val engineForSpeculativeConnects: Engine?
    val defaultHistoryStorageProvider: HistoryStorageSuggestionProvider
    val defaultCombinedHistoryProvider: CombinedHistorySuggestionProvider
    val defaultSearchSuggestionProvider: SearchSuggestionProvider
    val defaultTrendingSearchProvider: TrendingSearchProvider
    val defaultSearchActionProvider: SearchActionProvider
    var searchEngineSuggestionProvider: SearchEngineSuggestionProvider?
    val searchSuggestionProviderMap: MutableMap<SearchEngine, List<AwesomeBar.SuggestionProvider>>
    val combinedOnlineDataSource: CombinedOnlineSuggestionDataSource = CombinedOnlineSuggestionDataSource(scope = scope)

    init {
        engineForSpeculativeConnects = when (browsingModeManager.mode) {
            BrowsingMode.Normal -> components.core.engine
            BrowsingMode.Private -> null
        }

        defaultHistoryStorageProvider =
            HistoryStorageSuggestionProvider(
                components.core.historyStorage,
                loadUrlUseCase,
                components.core.icons,
                engineForSpeculativeConnects,
                showEditSuggestion = false,
                suggestionsHeader = suggestionsStringsProvider.firefoxSuggestHeader,
            )

        defaultCombinedHistoryProvider =
            CombinedHistorySuggestionProvider(
                historyStorage = components.core.historyStorage,
                historyMetadataStorage = components.core.historyStorage,
                loadUrlUseCase = loadUrlUseCase,
                icons = components.core.icons,
                engine = engineForSpeculativeConnects,
                maxNumberOfSuggestions = METADATA_SUGGESTION_LIMIT,
                showEditSuggestion = false,
                suggestionsHeader = suggestionsStringsProvider.firefoxSuggestHeader,
            )

        val searchBitmap = suggestionIconProvider.getSearchIconBitmap()
        val searchWithBitmap = suggestionIconProvider.getSearchWithIconBitmap()

        val trendingSearchBitmap = suggestionIconProvider.getTrendingSearchIconBitmap()

        defaultSearchSuggestionProvider =
            SearchSuggestionProvider(
                store = components.core.store,
                searchUseCase = searchUseCase,
                fetchClient = components.core.client,
                mode = SearchSuggestionProvider.Mode.MULTIPLE_SUGGESTIONS,
                limit = 3,
                icon = searchBitmap,
                showDescription = false,
                engine = engineForSpeculativeConnects,
                filterExactMatch = true,
                private = browsingModeManager.mode.isPrivate,
                suggestionsHeader = suggestionsStringsProvider.forSearchEngineSuggestion(),
            )

        defaultTrendingSearchProvider =
            TrendingSearchProvider(
                fetchClient = components.core.client,
                privateMode = browsingModeManager.mode.isPrivate,
                searchUseCase = searchUseCase,
                icon = trendingSearchBitmap,
            )

        defaultSearchActionProvider =
            SearchActionProvider(
                store = components.core.store,
                searchUseCase = searchUseCase,
                icon = searchBitmap,
                showDescription = false,
                suggestionsHeader = suggestionsStringsProvider.forSearchEngineSuggestion(),
            )

        searchEngineSuggestionProvider =
            SearchEngineSuggestionProvider(
                searchEnginesList = components.core.store.state.search.searchEngines,
                selectShortcutEngine = onSearchEngineSuggestionSelected,
                description = suggestionsStringsProvider.forSearchEngineSuggestionDescription(),
                searchIcon = searchWithBitmap,
                titleFormatter = { searchEngineName ->
                    suggestionsStringsProvider.searchEngineSuggestionProviderTitle(searchEngineName)
                },
            )

        searchSuggestionProviderMap = HashMap()
    }

    @Suppress("CognitiveComplexMethod", "LongMethod", "CyclomaticComplexMethod")
    internal fun getProvidersToAdd(
        state: SearchProviderState,
    ): Set<AwesomeBar.SuggestionProvider> {
        val providersToAdd = mutableSetOf<AwesomeBar.SuggestionProvider>()

        when (state.searchEngineSource) {
            is SearchEngineSource.History -> {
                defaultCombinedHistoryProvider.setMaxNumberOfSuggestions(METADATA_HISTORY_SUGGESTION_LIMIT)
                defaultHistoryStorageProvider.setMaxNumberOfSuggestions(METADATA_HISTORY_SUGGESTION_LIMIT)
            }
            else -> {
                defaultCombinedHistoryProvider.setMaxNumberOfSuggestions(METADATA_SUGGESTION_LIMIT)
                defaultHistoryStorageProvider.setMaxNumberOfSuggestions(METADATA_SUGGESTION_LIMIT)
            }
        }

        if (state.showSearchTermHistory) {
            getSearchTermSuggestionsProvider(
                searchEngineSource = state.searchEngineSource,
            )?.let { providersToAdd.add(it) }
        }

        if (state.showRecentSearches) {
            getRecentSearchSuggestionsProvider(
                searchEngineSource = state.searchEngineSource,
            )?.let { providersToAdd.add(it) }
        }

        if (state.showAllHistorySuggestions) {
            providersToAdd.add(
                getHistoryProvider(
                    filter = getFilterToExcludeSponsoredResults(state),
                ),
            )
        }

        if (state.showHistorySuggestionsForCurrentEngine) {
            getFilterForCurrentEngineResults(state)?.let {
                providersToAdd.add(getHistoryProvider(it))
            }
        }

        if (state.showAllBookmarkSuggestions) {
            providersToAdd.add(
                getBookmarksProvider(
                    filter = getFilterToExcludeSponsoredResults(state),
                ),
            )
        }

        if (state.showBookmarksSuggestionsForCurrentEngine) {
            getFilterForCurrentEngineResults(state)?.let {
                providersToAdd.add(getBookmarksProvider(it))
            }
        }

        if (state.showSearchSuggestions) {
            providersToAdd.addAll(getSelectedSearchSuggestionProvider(state))
        }

        if (state.showAllSyncedTabsSuggestions) {
            providersToAdd.add(
                getSyncedTabsProvider(
                    filter = getFilterToExcludeSponsoredResults(state),
                ),
            )
        }

        if (state.showSyncedTabsSuggestionsForCurrentEngine) {
            getFilterForCurrentEngineResults(state)?.let {
                providersToAdd.add(getSyncedTabsProvider(it))
            }
        }

        if (!browsingModeManager.mode.isPrivate && state.showAllSessionSuggestions) {
            // Unlike other providers, we don't exclude sponsored suggestions for open tabs.
            providersToAdd.add(getLocalTabsProvider())
        }

        if (!browsingModeManager.mode.isPrivate && state.showSessionSuggestionsForCurrentEngine) {
            getFilterForCurrentEngineResults(state)?.let {
                providersToAdd.add(getLocalTabsProvider(it))
            }
        }

        if (state.showSponsoredSuggestions || state.showNonSponsoredSuggestions) {
            providersToAdd.add(
                if (components.settings.boostAmpWikiSuggestions) {
                    FxSuggestSuggestionProvider(
                        loadUrlUseCase = loadUrlUseCase,
                        includeSponsoredSuggestions = state.showSponsoredSuggestions,
                        includeNonSponsoredSuggestions = state.showNonSponsoredSuggestions,
                        suggestionsHeader = suggestionsStringsProvider.firefoxSuggestHeader,
                        sponsoredSuggestionDescription = suggestionsStringsProvider.getSponsoredSuggestionDescription(),
                        contextId = components.settings.contileContextId,
                        scorer = FxSuggestionExperimentScorer(),
                    )
                } else {
                    FxSuggestSuggestionProvider(
                        loadUrlUseCase = loadUrlUseCase,
                        includeSponsoredSuggestions = state.showSponsoredSuggestions,
                        includeNonSponsoredSuggestions = state.showNonSponsoredSuggestions,
                        suggestionsHeader = suggestionsStringsProvider.firefoxSuggestHeader,
                        sponsoredSuggestionDescription = suggestionsStringsProvider.getSponsoredSuggestionDescription(),
                        contextId = components.settings.contileContextId,
                    )
                },
            )
        }

        if (state.showStocksSuggestions) {
            providersToAdd.add(
                StocksOnlineSuggestionProvider(
                    searchUseCase = searchUseCase,
                    dataSource = combinedOnlineDataSource,
                    suggestionsHeader = suggestionsStringsProvider.firefoxSuggestOnlineHeader,
                ),
            )
        }

        if (state.showSportsSuggestions) {
            providersToAdd.add(
                SportsOnlineSuggestionProvider(
                    icons = components.core.icons,
                    searchUseCase = searchUseCase,
                    dataSource = combinedOnlineDataSource,
                    suggestionsHeader = suggestionsStringsProvider.firefoxSuggestOnlineHeader,
                ),
            )
        }

        if (state.showFlightsSuggestions) {
            providersToAdd.add(
                FlightsOnlineSuggestionProvider(
                    loadUrlUseCase = loadUrlUseCase,
                    dataSource = combinedOnlineDataSource,
                    suggestionsHeader = suggestionsStringsProvider.firefoxSuggestOnlineHeader,
                ),
            )
        }

        providersToAdd.add(requireNotNull(searchEngineSuggestionProvider))

        if (state.showTrendingSearches) {
            val suggestionHeader = state.searchEngineSource.searchEngine?.let { searchEngine ->
                suggestionsStringsProvider.forTrendingSearches(searchEngine)
            }

            defaultTrendingSearchProvider.setSearchEngine(
                state.searchEngineSource.searchEngine,
                suggestionHeader,
            )
            providersToAdd.add(defaultTrendingSearchProvider)
        }

        return providersToAdd
    }

    /**
     * Configure and return a provider of history suggestions.
     *
     * @param filter Optional filter to limit the returned history suggestions.
     *
     * @return A [CombinedHistorySuggestionProvider] or [HistoryStorageSuggestionProvider] depending
     * on if the history metadata feature is enabled.
     */
    @VisibleForTesting
    internal fun getHistoryProvider(
        filter: SearchResultFilter? = null,
    ): AwesomeBar.SuggestionProvider {
        return if (components.settings.historyMetadataUIFeature) {
            if (filter != null) {
                CombinedHistorySuggestionProvider(
                    historyStorage = components.core.historyStorage,
                    historyMetadataStorage = components.core.historyStorage,
                    loadUrlUseCase = loadUrlUseCase,
                    icons = components.core.icons,
                    engine = engineForSpeculativeConnects,
                    maxNumberOfSuggestions = METADATA_SUGGESTION_LIMIT,
                    showEditSuggestion = false,
                    suggestionsHeader = suggestionsStringsProvider.firefoxSuggestHeader,
                    resultsUriFilter = filter::shouldIncludeUri,
                )
            } else {
                defaultCombinedHistoryProvider
            }
        } else {
            if (filter != null) {
                HistoryStorageSuggestionProvider(
                    historyStorage = components.core.historyStorage,
                    loadUrlUseCase = loadUrlUseCase,
                    icons = components.core.icons,
                    engine = engineForSpeculativeConnects,
                    maxNumberOfSuggestions = METADATA_SUGGESTION_LIMIT,
                    showEditSuggestion = false,
                    suggestionsHeader = suggestionsStringsProvider.firefoxSuggestHeader,
                    resultsUriFilter = filter::shouldIncludeUri,
                )
            } else {
                defaultHistoryStorageProvider
            }
        }
    }

    private fun getSelectedSearchSuggestionProvider(state: SearchProviderState): List<AwesomeBar.SuggestionProvider> {
        return when (state.searchEngineSource) {
            is SearchEngineSource.Default -> listOf(
                defaultSearchActionProvider,
                defaultSearchSuggestionProvider,
            )
            is SearchEngineSource.Shortcut -> getSuggestionProviderForEngine(
                state.searchEngineSource.searchEngine,
            )
            is SearchEngineSource.History -> emptyList()
            is SearchEngineSource.Bookmarks -> emptyList()
            is SearchEngineSource.Tabs -> emptyList()
            is SearchEngineSource.None -> emptyList()
        }
    }

    @VisibleForTesting
    internal fun getSearchTermSuggestionsProvider(
        searchEngineSource: SearchEngineSource,
    ): AwesomeBar.SuggestionProvider? {
        val validSearchEngine = searchEngineSource.searchEngine ?: return null

        return SearchTermSuggestionsProvider(
            historyStorage = components.core.historyStorage,
            searchUseCase = searchUseCase,
            searchEngine = validSearchEngine,
            icon = suggestionIconProvider.getHistoryIconBitmap(),
            engine = engineForSpeculativeConnects,
            suggestionsHeader = suggestionsStringsProvider.forSearchEngineSuggestion(
                searchEngineSource.searchEngine,
            ),
        )
    }

    @VisibleForTesting
    internal fun getRecentSearchSuggestionsProvider(
        searchEngineSource: SearchEngineSource,
        maxNumberOfSuggestions: Int = DEFAULT_RECENT_SEARCH_SUGGESTION_LIMIT,
    ): AwesomeBar.SuggestionProvider? {
        val validSearchEngine = searchEngineSource.searchEngine ?: return null

        return RecentSearchSuggestionsProvider(
            historyStorage = components.core.historyStorage,
            searchUseCase = searchUseCase,
            searchEngine = validSearchEngine,
            maxNumberOfSuggestions = maxNumberOfSuggestions,
            icon = suggestionIconProvider.getHistoryIconBitmap(),
            engine = engineForSpeculativeConnects,
            suggestionsHeader = suggestionsStringsProvider.forRecentSearches(),
        )
    }

    private fun getSuggestionProviderForEngine(engine: SearchEngine): List<AwesomeBar.SuggestionProvider> {
        return searchSuggestionProviderMap.getOrPut(engine) {
            val searchBitmap = suggestionIconProvider.getSearchIconBitmap()

            val engineForSpeculativeConnects = when (browsingModeManager.mode.isPrivate) {
                true -> null
                else -> components.core.engine
            }

            listOf(
                SearchActionProvider(
                    searchEngine = engine,
                    store = components.core.store,
                    searchUseCase = searchUseCase,
                    icon = searchBitmap,
                ),
                SearchSuggestionProvider(
                    engine,
                    searchUseCase,
                    components.core.client,
                    limit = METADATA_SHORTCUT_SUGGESTION_LIMIT,
                    mode = SearchSuggestionProvider.Mode.MULTIPLE_SUGGESTIONS,
                    icon = searchBitmap,
                    engine = engineForSpeculativeConnects,
                    filterExactMatch = true,
                    private = browsingModeManager.mode.isPrivate,
                ),
            )
        }
    }

    /**
     * Configure and return a provider of synced tabs suggestions.
     *
     * @param filter Optional filter to limit the returned synced tab suggestions.
     *
     * @return [SyncedTabsStorageSuggestionProvider] providing suggestions for the [AwesomeBar].
     */
    @VisibleForTesting
    internal fun getSyncedTabsProvider(
        filter: SearchResultFilter? = null,
    ): SyncedTabsStorageSuggestionProvider {
        return SyncedTabsStorageSuggestionProvider(
            components.backgroundServices.syncedTabsStorage,
            loadUrlUseCase,
            components.core.icons,
            DeviceIndicators(
                suggestionIconProvider.getDesktopIconDrawable(),
                suggestionIconProvider.getMobileIconDrawable(),
                suggestionIconProvider.getTabletIconDrawable(),
            ),
            suggestionsHeader = suggestionsStringsProvider.firefoxSuggestHeader,
            resultsUrlFilter = filter?.let { it::shouldIncludeUrl },
        )
    }

    /**
     *  Configure and return a provider of local tabs suggestions.
     *
     * @param filter Optional filter to limit the returned local tab suggestions.
     *
     * @return [SessionSuggestionProvider] providing suggestions for the [AwesomeBar].
     */
    @VisibleForTesting
    internal fun getLocalTabsProvider(
        filter: SearchResultFilter? = null,
    ): SessionSuggestionProvider {
        return SessionSuggestionProvider(
            components.core.store,
            selectTabUseCase,
            components.core.icons,
            suggestionIconProvider.getLocalTabIconDrawable(),
            excludeSelectedSession = !includeSelectedTab,
            suggestionsHeader = suggestionsStringsProvider.firefoxSuggestHeader,
            switchToTabDescription = suggestionsStringsProvider.getSwitchToTabDescriptionString(),
            resultsUriFilter = filter?.let { it::shouldIncludeUri },
        )
    }

    /**
     * Configure and return a provider of bookmark suggestions.
     *
     * @param filter Optional filter to limit the returned bookmark suggestions.
     *
     * @return [BookmarksStorageSuggestionProvider] providing suggestions for the [AwesomeBar].
     */
    @VisibleForTesting
    internal fun getBookmarksProvider(
        filter: SearchResultFilter? = null,
    ): BookmarksStorageSuggestionProvider {
        return BookmarksStorageSuggestionProvider(
            bookmarksStorage = components.core.bookmarksStorage,
            loadUrlUseCase = loadUrlUseCase,
            icons = components.core.icons,
            indicatorIcon = suggestionIconProvider.getBookmarkIconDrawable(),
            engine = engineForSpeculativeConnects,
            showEditSuggestion = false,
            suggestionsHeader = suggestionsStringsProvider.firefoxSuggestHeader,
            resultsUriFilter = filter?.let { it::shouldIncludeUri },
        )
    }

    /**
     * Returns a [SearchResultFilter] that only includes results for the current search engine.
     */
    internal fun getFilterForCurrentEngineResults(state: SearchProviderState): SearchResultFilter? =
        state.searchEngineSource.searchEngine?.resultsUrl?.let {
            SearchResultFilter.CurrentEngine(it)
        }

    /**
     * Returns a [SearchResultFilter] that excludes sponsored results.
     */
    internal fun getFilterToExcludeSponsoredResults(state: SearchProviderState): SearchResultFilter? =
        if (state.showSponsoredSuggestions) {
            SearchResultFilter.ExcludeSponsored(components.settings.frecencyFilterQuery)
        } else {
            null
        }

    /**
     * Data based on which the search suggestions providers list should be built.
     *
     * @property showSearchTermHistory Whether to show the search term history.
     * @property showHistorySuggestionsForCurrentEngine Whether to show history suggestions
     * for the current search engine.
     * @property showAllHistorySuggestions Whether to show all history suggestions.
     * @property showBookmarksSuggestionsForCurrentEngine Whether to show bookmarks suggestions
     * for the current search engine.
     * @property showAllBookmarkSuggestions Whether to show all bookmark suggestions.
     * @property showSearchSuggestions Whether to show search suggestions.
     * @property showSyncedTabsSuggestionsForCurrentEngine Whether to show synced tabs suggestions
     * for the current search engine.
     * @property showAllSyncedTabsSuggestions Whether to show all synced tabs suggestions.
     * @property showSessionSuggestionsForCurrentEngine Whether to show session suggestions
     * for the current search engine.
     * @property showAllSessionSuggestions Whether to show all session suggestions.
     * @property showSponsoredSuggestions Whether to show sponsored suggestions.
     * @property showNonSponsoredSuggestions Whether to show non-sponsored suggestions.
     * @property showStocksSuggestions Whether to show optimized search suggestion stock cards.
     * @property showSportsSuggestions Whether to show optimized search suggestion sport cards.
     * @property showFlightsSuggestions Whether to show optimized search suggestion flight cards.
     * @property showTrendingSearches Whether to show trending searches.
     * @property showRecentSearches Whether to show recent searches.
     * @property searchEngineSource Hoe the current search engine was selected.
     */
    data class SearchProviderState(
        val showSearchTermHistory: Boolean,
        val showHistorySuggestionsForCurrentEngine: Boolean,
        val showAllHistorySuggestions: Boolean,
        val showBookmarksSuggestionsForCurrentEngine: Boolean,
        val showAllBookmarkSuggestions: Boolean,
        val showSearchSuggestions: Boolean,
        val showSyncedTabsSuggestionsForCurrentEngine: Boolean,
        val showAllSyncedTabsSuggestions: Boolean,
        val showSessionSuggestionsForCurrentEngine: Boolean,
        val showAllSessionSuggestions: Boolean,
        val showSponsoredSuggestions: Boolean,
        val showNonSponsoredSuggestions: Boolean,
        val showStocksSuggestions: Boolean,
        val showSportsSuggestions: Boolean,
        val showFlightsSuggestions: Boolean,
        val showTrendingSearches: Boolean,
        val showRecentSearches: Boolean,
        val searchEngineSource: SearchEngineSource,
    )

    /**
     * Filters to limit the suggestions returned from a suggestion provider.
     */
    sealed interface SearchResultFilter {
        /**
         * A filter for the currently selected search engine. This filter only includes suggestions
         * whose URLs have the same host as [resultsUri].
         */
        data class CurrentEngine(val resultsUri: Uri) : SearchResultFilter

        /**
         * A filter that excludes sponsored suggestions, whose URLs contain the given
         * [queryParameter].
         */
        data class ExcludeSponsored(val queryParameter: String) : SearchResultFilter

        /**
         * Returns `true` if the suggestion with the given [uri] should be included in the
         * suggestions returned from the provider.
         */
        fun shouldIncludeUri(uri: Uri): Boolean = when (this) {
            is CurrentEngine -> this.resultsUri.sameHostWithoutMobileSubdomainAs(uri)
            is ExcludeSponsored -> !uri.containsQueryParameters(queryParameter)
        }

        /**
         * Returns `true` if the suggestion with the given [url] string should be included in the
         * suggestions returned from the provider.
         */
        fun shouldIncludeUrl(url: String): Boolean = when (this) {
            is CurrentEngine -> resultsUri.host == url.tryGetHostFromUrl()
            is ExcludeSponsored -> !url.urlContainsQueryParameters(queryParameter)
        }
    }

    /**
     * Static configuration.
     */
    companion object {
        // Maximum number of suggestions returned.
        const val METADATA_SUGGESTION_LIMIT = 3

        const val GOOGLE_SEARCH_ENGINE_NAME = "Google"
    }
}
