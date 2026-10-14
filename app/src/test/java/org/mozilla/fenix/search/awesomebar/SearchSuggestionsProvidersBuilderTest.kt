/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.search.awesomebar

import androidx.core.net.toUri
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.TestScope
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.search.SearchEngineProvider
import mozilla.components.feature.awesomebar.provider.BookmarksStorageSuggestionProvider
import mozilla.components.feature.awesomebar.provider.CombinedHistorySuggestionProvider
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
import mozilla.components.feature.syncedtabs.SyncedTabsStorageSuggestionProvider
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager
import org.mozilla.fenix.components.Components
import org.mozilla.fenix.components.Core.Companion
import org.mozilla.fenix.search.SearchEngineSource
import org.mozilla.fenix.search.awesomebar.SearchSuggestionsProvidersBuilder.SearchProviderState
import org.mozilla.fenix.utils.Settings
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchSuggestionsProvidersBuilderTest {

    private lateinit var components: Components
    private lateinit var builder: SearchSuggestionsProvidersBuilder
    private val browsingModeManager: BrowsingModeManager = mockk(relaxed = true)
    private lateinit var searchEngineProvider: SearchEngineProvider
    private lateinit var suggestionsStringsProvider: SuggestionsStringsProvider

    @Before
    fun setup() {
        components = mockk(relaxed = true)
        every { components.core.store.state.search } returns mockk(relaxed = true)

        searchEngineProvider =
            mockk<SearchEngineProvider>(relaxed = true) {
                every { getDefaultSearchEngine() } returns
                    mockk {
                        every { name } returns "Google"
                    }
            }

        suggestionsStringsProvider = DefaultSuggestionsStringsProvider(testContext, searchEngineProvider)

        builder =
            SearchSuggestionsProvidersBuilder(
                components = components,
                scope = TestScope(),
                browsingModeManager = browsingModeManager,
                includeSelectedTab = false,
                loadUrlUseCase = mockk(),
                searchUseCase = mockk(),
                selectTabUseCase = mockk(),
                suggestionsStringsProvider = suggestionsStringsProvider,
                suggestionIconProvider = mockk(relaxed = true),
                onSearchEngineSuggestionSelected = {},
            )
    }

    @Test
    fun `GIVEN a search from history and history metadata is enabled and sponsored suggestions are enabled WHEN setting the providers THEN set less suggestions to be shown`() {
        val settings: Settings =
            mockk(relaxed = true) {
                every { historyMetadataUIFeature } returns true
            }
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                searchEngineSource = SearchEngineSource.History(mockk(relaxed = true)),
                showSponsoredSuggestions = true,
            )

        val result = builder.getProvidersToAdd(state)

        val historyProvider = result.firstOrNull { it is CombinedHistorySuggestionProvider }
        assertNotNull(historyProvider)
        assertEquals(
            SearchSuggestionsProvidersBuilder.METADATA_SUGGESTION_LIMIT,
            (historyProvider as CombinedHistorySuggestionProvider).getMaxNumberOfSuggestions(),
        )
    }

    @Test
    fun `GIVEN a search from history and history metadata is enabled and sponsored suggestions are disabled WHEN setting the providers THEN set more suggestions to be shown`() {
        val settings: Settings =
            mockk(relaxed = true) {
                every { historyMetadataUIFeature } returns true
            }
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                searchEngineSource = SearchEngineSource.History(mockk(relaxed = true)),
                showSponsoredSuggestions = false,
            )

        val result = builder.getProvidersToAdd(state)

        val historyProvider = result.firstOrNull { it is CombinedHistorySuggestionProvider }
        assertNotNull(historyProvider)
        assertEquals(
            Companion.METADATA_HISTORY_SUGGESTION_LIMIT,
            (historyProvider as CombinedHistorySuggestionProvider).getMaxNumberOfSuggestions(),
        )
    }

    @Test
    fun `GIVEN a search from history and history metadata is disabled and sponsored suggestions are enabled WHEN setting the providers THEN set less suggestions to be shown`() {
        val settings: Settings =
            mockk(relaxed = true) {
                every { historyMetadataUIFeature } returns false
            }
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                searchEngineSource = SearchEngineSource.History(mockk(relaxed = true)),
                showSponsoredSuggestions = true,
            )

        val result = builder.getProvidersToAdd(state)

        val historyProvider = result.firstOrNull { it is HistoryStorageSuggestionProvider }
        assertNotNull(historyProvider)
        assertEquals(
            SearchSuggestionsProvidersBuilder.METADATA_SUGGESTION_LIMIT,
            (historyProvider as HistoryStorageSuggestionProvider).getMaxNumberOfSuggestions(),
        )
    }

    @Test
    fun `GIVEN a search from history and history metadata is disabled and sponsored suggestions are disabled WHEN setting the providers THEN set more suggestions to be shown`() {
        val settings: Settings =
            mockk(relaxed = true) {
                every { historyMetadataUIFeature } returns false
            }
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                searchEngineSource = SearchEngineSource.History(mockk(relaxed = true)),
                showSponsoredSuggestions = false,
            )

        val result = builder.getProvidersToAdd(state)

        val historyProvider = result.firstOrNull { it is HistoryStorageSuggestionProvider }
        assertNotNull(historyProvider)
        assertEquals(
            Companion.METADATA_HISTORY_SUGGESTION_LIMIT,
            (historyProvider as HistoryStorageSuggestionProvider).getMaxNumberOfSuggestions(),
        )
    }

    @Test
    fun `GIVEN a search not from history and history metadata is enabled and sponsored suggestions are enabled WHEN setting the providers THEN set less suggestions to be shown`() {
        val settings: Settings =
            mockk(relaxed = true) {
                every { historyMetadataUIFeature } returns true
            }
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                searchEngineSource = SearchEngineSource.Shortcut(mockk(relaxed = true)),
                showSponsoredSuggestions = true,
            )

        val result = builder.getProvidersToAdd(state)

        val historyProvider = result.firstOrNull { it is CombinedHistorySuggestionProvider }
        assertNotNull(historyProvider)
        assertEquals(
            SearchSuggestionsProvidersBuilder.METADATA_SUGGESTION_LIMIT,
            (historyProvider as CombinedHistorySuggestionProvider).getMaxNumberOfSuggestions(),
        )
    }

    @Test
    fun `GIVEN a search not from history and history metadata is enabled and sponsored suggestions are disabled WHEN setting the providers THEN set less suggestions to be shown`() {
        val settings: Settings =
            mockk(relaxed = true) {
                every { historyMetadataUIFeature } returns true
            }
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                searchEngineSource = SearchEngineSource.Shortcut(mockk(relaxed = true)),
                showSponsoredSuggestions = false,
            )

        val result = builder.getProvidersToAdd(state)

        val historyProvider = result.firstOrNull { it is CombinedHistorySuggestionProvider }
        assertNotNull(historyProvider)
        assertEquals(
            SearchSuggestionsProvidersBuilder.METADATA_SUGGESTION_LIMIT,
            (historyProvider as CombinedHistorySuggestionProvider).getMaxNumberOfSuggestions(),
        )
    }

    @Test
    fun `GIVEN a search not from history and history metadata is disabled and sponsored suggestions are enabled WHEN setting the providers THEN set less suggestions to be shown`() {
        val settings: Settings =
            mockk(relaxed = true) {
                every { historyMetadataUIFeature } returns true
            }
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                searchEngineSource = SearchEngineSource.Bookmarks(mockk(relaxed = true)),
                showSponsoredSuggestions = true,
            )

        val result = builder.getProvidersToAdd(state)

        val historyProvider = result.firstOrNull { it is CombinedHistorySuggestionProvider }
        assertNotNull(historyProvider)
        assertEquals(
            SearchSuggestionsProvidersBuilder.METADATA_SUGGESTION_LIMIT,
            (historyProvider as CombinedHistorySuggestionProvider).getMaxNumberOfSuggestions(),
        )
    }

    @Test
    fun `GIVEN a search not from history and history metadata disabled WHEN setting the providers THEN set less suggestions to be shown`() {
        val settings: Settings =
            mockk(relaxed = true) {
                every { historyMetadataUIFeature } returns true
            }
        every { components.settings } returns settings
        val state = getSearchProviderState(searchEngineSource = SearchEngineSource.Bookmarks(mockk(relaxed = true)))

        val result = builder.getProvidersToAdd(state)

        val historyProvider = result.firstOrNull { it is CombinedHistorySuggestionProvider }
        assertNotNull(historyProvider)
        assertEquals(
            SearchSuggestionsProvidersBuilder.METADATA_SUGGESTION_LIMIT,
            (historyProvider as CombinedHistorySuggestionProvider).getMaxNumberOfSuggestions(),
        )
    }

    @Test
    fun `GIVEN a search that should show filtered history WHEN history metadata is enabled and sponsored suggestions are enabled THEN return a history metadata provider with an engine filter`() {
        val settings: Settings =
            mockk(relaxed = true) {
                every { historyMetadataUIFeature } returns true
            }
        val url = "test.com".toUri()
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                showAllHistorySuggestions = false,
                searchEngineSource =
                    SearchEngineSource.Shortcut(
                        mockk(relaxed = true) {
                            every { resultsUrl } returns url
                        }
                    ),
                showSponsoredSuggestions = true,
            )

        val result = builder.getProvidersToAdd(state)

        val historyProvider = result.firstOrNull { it is CombinedHistorySuggestionProvider }
        assertNotNull(historyProvider)
        assertNotNull((historyProvider as CombinedHistorySuggestionProvider).resultsUriFilter)
        assertEquals(
            SearchSuggestionsProvidersBuilder.METADATA_SUGGESTION_LIMIT,
            historyProvider.getMaxNumberOfSuggestions(),
        )
    }

    @Test
    fun `GIVEN a search that should show filtered history WHEN history metadata is enabled and sponsored suggestions are disabled THEN return a history metadata provider with an engine filter`() {
        val settings: Settings =
            mockk(relaxed = true) {
                every { historyMetadataUIFeature } returns true
            }
        val url = "test.com".toUri()
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                showAllHistorySuggestions = false,
                searchEngineSource =
                    SearchEngineSource.Shortcut(
                        mockk(relaxed = true) {
                            every { resultsUrl } returns url
                        }
                    ),
                showSponsoredSuggestions = false,
            )

        val result = builder.getProvidersToAdd(state)

        val historyProvider = result.firstOrNull { it is CombinedHistorySuggestionProvider }
        assertNotNull(historyProvider)
        assertNotNull((historyProvider as CombinedHistorySuggestionProvider).resultsUriFilter)
        assertEquals(
            SearchSuggestionsProvidersBuilder.METADATA_SUGGESTION_LIMIT,
            historyProvider.getMaxNumberOfSuggestions(),
        )
    }

    @Test
    fun `GIVEN the default engine is selected WHEN history metadata is enabled THEN suggestions are disabled in history and bookmark providers`() {
        val settings: Settings =
            mockk(relaxed = true) {
                every { historyMetadataUIFeature } returns true
            }
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                showHistorySuggestionsForCurrentEngine = false,
                showBookmarksSuggestionsForCurrentEngine = false,
                showSyncedTabsSuggestionsForCurrentEngine = false,
                showSessionSuggestionsForCurrentEngine = false,
                searchEngineSource = SearchEngineSource.Default(mockk(relaxed = true)),
            )

        val result = builder.getProvidersToAdd(state)

        val combinedHistoryProvider =
            result.firstOrNull { it is CombinedHistorySuggestionProvider } as CombinedHistorySuggestionProvider
        assertNotNull(combinedHistoryProvider)
        assertFalse(combinedHistoryProvider.showEditSuggestion)

        val bookmarkProvider =
            result.firstOrNull { it is BookmarksStorageSuggestionProvider } as BookmarksStorageSuggestionProvider
        assertNotNull(bookmarkProvider)
        assertFalse(bookmarkProvider.showEditSuggestion)
    }

    @Test
    fun `GIVEN the default engine is selected WHEN history metadata is disabled THEN suggestions are disabled in history and bookmark providers`() {
        val settings: Settings =
            mockk(relaxed = true) {
                every { historyMetadataUIFeature } returns false
            }
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                showHistorySuggestionsForCurrentEngine = false,
                showBookmarksSuggestionsForCurrentEngine = false,
                showSyncedTabsSuggestionsForCurrentEngine = false,
                showSessionSuggestionsForCurrentEngine = false,
                searchEngineSource = SearchEngineSource.Default(mockk(relaxed = true)),
            )

        val result = builder.getProvidersToAdd(state)

        val defaultHistoryProvider =
            result.firstOrNull { it is HistoryStorageSuggestionProvider } as HistoryStorageSuggestionProvider
        assertNotNull(defaultHistoryProvider)
        assertFalse(defaultHistoryProvider.showEditSuggestion)

        val bookmarkProvider =
            result.firstOrNull { it is BookmarksStorageSuggestionProvider } as BookmarksStorageSuggestionProvider
        assertNotNull(bookmarkProvider)
        assertFalse(bookmarkProvider.showEditSuggestion)
    }

    @Test
    fun `GIVEN the non default general engine is selected WHEN history metadata is enabled THEN history and bookmark providers are not set`() {
        val settings: Settings =
            mockk(relaxed = true) {
                every { historyMetadataUIFeature } returns true
            }
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                showHistorySuggestionsForCurrentEngine = false,
                showAllHistorySuggestions = false,
                showBookmarksSuggestionsForCurrentEngine = false,
                showAllBookmarkSuggestions = false,
                showSyncedTabsSuggestionsForCurrentEngine = false,
                showAllSyncedTabsSuggestions = false,
                showSessionSuggestionsForCurrentEngine = false,
                showAllSessionSuggestions = false,
                searchEngineSource =
                    SearchEngineSource.Shortcut(
                        mockk(relaxed = true) {
                            every { isGeneral } returns true
                        }
                    ),
            )

        val result = builder.getProvidersToAdd(state)

        val combinedHistoryProvider = result.firstOrNull { it is CombinedHistorySuggestionProvider }
        assertNull(combinedHistoryProvider)

        val bookmarkProvider = result.firstOrNull { it is BookmarksStorageSuggestionProvider }
        assertNull(bookmarkProvider)
    }

    @Test
    fun `GIVEN the non default general engine is selected WHEN history metadata is disabled THEN history and bookmark providers are not set`() {
        val settings: Settings =
            mockk(relaxed = true) {
                every { historyMetadataUIFeature } returns false
            }
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                showHistorySuggestionsForCurrentEngine = false,
                showAllHistorySuggestions = false,
                showBookmarksSuggestionsForCurrentEngine = false,
                showAllBookmarkSuggestions = false,
                showSyncedTabsSuggestionsForCurrentEngine = false,
                showAllSyncedTabsSuggestions = false,
                showSessionSuggestionsForCurrentEngine = false,
                showAllSessionSuggestions = false,
                searchEngineSource =
                    SearchEngineSource.Shortcut(
                        mockk(relaxed = true) {
                            every { isGeneral } returns true
                        }
                    ),
            )

        val result = builder.getProvidersToAdd(state)

        val defaultHistoryProvider = result.firstOrNull { it is HistoryStorageSuggestionProvider }
        assertNull(defaultHistoryProvider)

        val bookmarkProvider = result.firstOrNull { it is BookmarksStorageSuggestionProvider }
        assertNull(bookmarkProvider)
    }

    @Test
    fun `GIVEN the non default non general engine is selected WHEN history metadata is enabled THEN suggestions are disabled in history and bookmark providers`() {
        val settings: Settings =
            mockk(relaxed = true) {
                every { historyMetadataUIFeature } returns true
            }
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                showAllHistorySuggestions = false,
                showAllBookmarkSuggestions = false,
                showAllSyncedTabsSuggestions = false,
                showAllSessionSuggestions = false,
                searchEngineSource =
                    SearchEngineSource.Shortcut(
                        mockk(relaxed = true) {
                            every { isGeneral } returns false
                        }
                    ),
            )

        val result = builder.getProvidersToAdd(state)

        val combinedHistoryProvider =
            result.firstOrNull { it is CombinedHistorySuggestionProvider } as CombinedHistorySuggestionProvider
        assertNotNull(combinedHistoryProvider)
        assertFalse(combinedHistoryProvider.showEditSuggestion)

        val bookmarkProvider =
            result.firstOrNull { it is BookmarksStorageSuggestionProvider } as BookmarksStorageSuggestionProvider
        assertNotNull(bookmarkProvider)
        assertFalse(bookmarkProvider.showEditSuggestion)
    }

    @Test
    fun `GIVEN the non default non general engine is selected WHEN history metadata is disabled THEN suggestions are disabled in history and bookmark providers`() {
        val settings: Settings =
            mockk(relaxed = true) {
                every { historyMetadataUIFeature } returns false
            }
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                showAllHistorySuggestions = false,
                showAllBookmarkSuggestions = false,
                showAllSyncedTabsSuggestions = false,
                showAllSessionSuggestions = false,
                searchEngineSource =
                    SearchEngineSource.Shortcut(
                        mockk(relaxed = true) {
                            every { isGeneral } returns false
                        }
                    ),
            )

        val result = builder.getProvidersToAdd(state)

        val defaultHistoryProvider =
            result.firstOrNull { it is HistoryStorageSuggestionProvider } as HistoryStorageSuggestionProvider
        assertNotNull(defaultHistoryProvider)
        assertFalse(defaultHistoryProvider.showEditSuggestion)

        val bookmarkProvider =
            result.firstOrNull { it is BookmarksStorageSuggestionProvider } as BookmarksStorageSuggestionProvider
        assertNotNull(bookmarkProvider)
        assertFalse(bookmarkProvider.showEditSuggestion)
    }

    @Test
    fun `GIVEN history is selected WHEN history metadata is enabled THEN suggestions are disabled in history provider, bookmark provider is not set`() {
        val settings: Settings =
            mockk(relaxed = true) {
                every { historyMetadataUIFeature } returns true
            }
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                showSearchTermHistory = false,
                showHistorySuggestionsForCurrentEngine = false,
                showBookmarksSuggestionsForCurrentEngine = false,
                showAllBookmarkSuggestions = false,
                showSearchSuggestions = false,
                showSyncedTabsSuggestionsForCurrentEngine = false,
                showAllSyncedTabsSuggestions = false,
                showSessionSuggestionsForCurrentEngine = false,
                showAllSessionSuggestions = false,
                searchEngineSource = SearchEngineSource.History(mockk(relaxed = true)),
            )

        val result = builder.getProvidersToAdd(state)

        val combinedHistoryProvider =
            result.firstOrNull { it is CombinedHistorySuggestionProvider } as CombinedHistorySuggestionProvider
        assertNotNull(combinedHistoryProvider)
        assertFalse(combinedHistoryProvider.showEditSuggestion)

        val bookmarkProvider = result.firstOrNull { it is BookmarksStorageSuggestionProvider }
        assertNull(bookmarkProvider)
    }

    @Test
    fun `GIVEN history is selected WHEN history metadata is disabled THEN suggestions are disabled in history provider, bookmark provider is not set`() {
        val settings: Settings =
            mockk(relaxed = true) {
                every { historyMetadataUIFeature } returns false
            }
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                showSearchTermHistory = false,
                showHistorySuggestionsForCurrentEngine = false,
                showBookmarksSuggestionsForCurrentEngine = false,
                showAllBookmarkSuggestions = false,
                showSearchSuggestions = false,
                showSyncedTabsSuggestionsForCurrentEngine = false,
                showAllSyncedTabsSuggestions = false,
                showSessionSuggestionsForCurrentEngine = false,
                showAllSessionSuggestions = false,
                searchEngineSource = SearchEngineSource.History(mockk(relaxed = true)),
            )

        val result = builder.getProvidersToAdd(state)

        val defaultHistoryProvider =
            result.firstOrNull { it is HistoryStorageSuggestionProvider } as HistoryStorageSuggestionProvider
        assertNotNull(defaultHistoryProvider)
        assertFalse(defaultHistoryProvider.showEditSuggestion)

        val bookmarkProvider = result.firstOrNull { it is BookmarksStorageSuggestionProvider }
        assertNull(bookmarkProvider)
    }

    @Test
    fun `GIVEN tab engine is selected WHEN history metadata is enabled THEN history and bookmark providers are not set`() {
        val settings: Settings =
            mockk(relaxed = true) {
                every { historyMetadataUIFeature } returns true
            }
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                showSearchTermHistory = false,
                showHistorySuggestionsForCurrentEngine = false,
                showAllHistorySuggestions = false,
                showBookmarksSuggestionsForCurrentEngine = false,
                showAllBookmarkSuggestions = false,
                showSearchSuggestions = false,
                showSyncedTabsSuggestionsForCurrentEngine = false,
                showSessionSuggestionsForCurrentEngine = false,
                searchEngineSource = SearchEngineSource.Tabs(mockk(relaxed = true)),
            )

        val result = builder.getProvidersToAdd(state)

        val combinedHistoryProvider = result.firstOrNull { it is CombinedHistorySuggestionProvider }
        assertNull(combinedHistoryProvider)

        val bookmarkProvider = result.firstOrNull { it is BookmarksStorageSuggestionProvider }
        assertNull(bookmarkProvider)
    }

    @Test
    fun `GIVEN tab engine is selected WHEN history metadata is disabled THEN history and bookmark providers are not set`() {
        val settings: Settings =
            mockk(relaxed = true) {
                every { historyMetadataUIFeature } returns false
            }
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                showSearchTermHistory = false,
                showHistorySuggestionsForCurrentEngine = false,
                showAllHistorySuggestions = false,
                showBookmarksSuggestionsForCurrentEngine = false,
                showAllBookmarkSuggestions = false,
                showSearchSuggestions = false,
                showSyncedTabsSuggestionsForCurrentEngine = false,
                showSessionSuggestionsForCurrentEngine = false,
                searchEngineSource = SearchEngineSource.Tabs(mockk(relaxed = true)),
            )

        val result = builder.getProvidersToAdd(state)

        val defaultHistoryProvider = result.firstOrNull { it is HistoryStorageSuggestionProvider }
        assertNull(defaultHistoryProvider)

        val bookmarkProvider = result.firstOrNull { it is BookmarksStorageSuggestionProvider }
        assertNull(bookmarkProvider)
    }

    @Test
    fun `GIVEN bookmarks engine is selected WHEN history metadata is enabled THEN history provider is not set, suggestions are disabled in bookmark provider`() {
        val settings: Settings =
            mockk(relaxed = true) {
                every { historyMetadataUIFeature } returns true
            }
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                showSearchTermHistory = false,
                showHistorySuggestionsForCurrentEngine = false,
                showAllHistorySuggestions = false,
                showBookmarksSuggestionsForCurrentEngine = false,
                showSearchSuggestions = false,
                showSyncedTabsSuggestionsForCurrentEngine = false,
                showAllSyncedTabsSuggestions = false,
                showSessionSuggestionsForCurrentEngine = false,
                showAllSessionSuggestions = false,
                searchEngineSource = SearchEngineSource.Bookmarks(mockk(relaxed = true)),
            )

        val result = builder.getProvidersToAdd(state)

        val combinedHistoryProvider = result.firstOrNull { it is CombinedHistorySuggestionProvider }
        assertNull(combinedHistoryProvider)

        val bookmarkProvider =
            result.firstOrNull { it is BookmarksStorageSuggestionProvider } as BookmarksStorageSuggestionProvider
        assertNotNull(bookmarkProvider)
        assertFalse(bookmarkProvider.showEditSuggestion)
    }

    @Test
    fun `GIVEN bookmarks engine is selected WHEN history metadata is disabled THEN history provider is not set, suggestions are disabled in bookmark provider`() {
        val settings: Settings =
            mockk(relaxed = true) {
                every { historyMetadataUIFeature } returns false
            }
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                showSearchTermHistory = false,
                showHistorySuggestionsForCurrentEngine = false,
                showAllHistorySuggestions = false,
                showBookmarksSuggestionsForCurrentEngine = false,
                showSearchSuggestions = false,
                showSyncedTabsSuggestionsForCurrentEngine = false,
                showAllSyncedTabsSuggestions = false,
                showSessionSuggestionsForCurrentEngine = false,
                showAllSessionSuggestions = false,
                searchEngineSource = SearchEngineSource.Bookmarks(mockk(relaxed = true)),
            )

        val result = builder.getProvidersToAdd(state)

        val defaultHistoryProvider = result.firstOrNull { it is HistoryStorageSuggestionProvider }
        assertNull(defaultHistoryProvider)

        val bookmarkProvider =
            result.firstOrNull { it is BookmarksStorageSuggestionProvider } as BookmarksStorageSuggestionProvider
        assertNotNull(bookmarkProvider)
        assertFalse(bookmarkProvider.showEditSuggestion)
    }

    @Test
    fun `GIVEN a search that should show filtered history WHEN history metadata is disabled and sponsored suggestions are enabled THEN return a history provider with an engine filter`() {
        val settings: Settings =
            mockk(relaxed = true) {
                every { historyMetadataUIFeature } returns false
            }
        val url = "test.com".toUri()
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                showAllHistorySuggestions = false,
                searchEngineSource =
                    SearchEngineSource.Shortcut(
                        mockk(relaxed = true) {
                            every { resultsUrl } returns url
                        }
                    ),
                showSponsoredSuggestions = true,
            )

        val result = builder.getProvidersToAdd(state)

        val historyProvider = result.firstOrNull { it is HistoryStorageSuggestionProvider }
        assertNotNull(historyProvider)
        assertNotNull((historyProvider as HistoryStorageSuggestionProvider).resultsUriFilter)
        assertEquals(
            SearchSuggestionsProvidersBuilder.METADATA_SUGGESTION_LIMIT,
            historyProvider.getMaxNumberOfSuggestions(),
        )
    }

    @Test
    fun `GIVEN a search that should show filtered history WHEN history metadata is disabled and sponsored suggestions are disabled THEN return a history provider with an engine filter`() {
        val settings: Settings =
            mockk(relaxed = true) {
                every { historyMetadataUIFeature } returns false
            }
        val url = "test.com".toUri()
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                showAllHistorySuggestions = false,
                searchEngineSource =
                    SearchEngineSource.Shortcut(
                        mockk(relaxed = true) {
                            every { resultsUrl } returns url
                        }
                    ),
                showSponsoredSuggestions = false,
            )

        val result = builder.getProvidersToAdd(state)

        val historyProvider = result.firstOrNull { it is HistoryStorageSuggestionProvider }
        assertNotNull(historyProvider)
        assertNotNull((historyProvider as HistoryStorageSuggestionProvider).resultsUriFilter)
        assertEquals(
            SearchSuggestionsProvidersBuilder.METADATA_SUGGESTION_LIMIT,
            historyProvider.getMaxNumberOfSuggestions(),
        )
    }

    @Test
    fun `GIVEN a search from the default engine WHEN configuring providers THEN add search action and search suggestions providers`() {
        val settings: Settings = mockk(relaxed = true)
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                showAllHistorySuggestions = false,
                searchEngineSource = SearchEngineSource.Default(mockk(relaxed = true)),
            )

        val result = builder.getProvidersToAdd(state)

        assertEquals(1, result.filterIsInstance<SearchActionProvider>().size)
        assertEquals(1, result.filterIsInstance<SearchSuggestionProvider>().size)
    }

    @Test
    fun `GIVEN a search from a shortcut engine WHEN configuring providers THEN add search action and search suggestions providers`() {
        val settings: Settings = mockk(relaxed = true)
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                showAllHistorySuggestions = false,
                searchEngineSource = SearchEngineSource.Default(mockk(relaxed = true)),
            )

        val result = builder.getProvidersToAdd(state)

        assertEquals(1, result.filterIsInstance<SearchActionProvider>().size)
        assertEquals(1, result.filterIsInstance<SearchSuggestionProvider>().size)
    }

    @Test
    fun `GIVEN searches from other than default and shortcut engines WHEN configuring providers THEN don't add search action and search suggestion providers`() {
        val settings: Settings = mockk(relaxed = true)
        every { components.settings } returns settings

        val historyState =
            getSearchProviderState(searchEngineSource = SearchEngineSource.History(mockk(relaxed = true)))
        val bookmarksState =
            getSearchProviderState(searchEngineSource = SearchEngineSource.Bookmarks(mockk(relaxed = true)))
        val tabsState = getSearchProviderState(searchEngineSource = SearchEngineSource.Tabs(mockk(relaxed = true)))
        val noneState = getSearchProviderState()

        val historyResult = builder.getProvidersToAdd(historyState)
        val bookmarksResult = builder.getProvidersToAdd(bookmarksState)
        val tabsResult = builder.getProvidersToAdd(tabsState)
        val noneResult = builder.getProvidersToAdd(noneState)
        val allResults = historyResult + bookmarksResult + tabsResult + noneResult

        assertEquals(0, allResults.filterIsInstance<SearchActionProvider>().size)
        assertEquals(0, allResults.filterIsInstance<SearchSuggestionProvider>().size)
    }

    @Test
    fun `GIVEN normal browsing mode and needing to show all local tabs suggestions and sponsored suggestions are enabled WHEN configuring providers THEN add the tabs provider`() {
        val settings: Settings = mockk(relaxed = true)
        val url = "https://www.test.com".toUri()
        every { components.settings } returns settings
        every { browsingModeManager.mode } returns BrowsingMode.Normal
        val state =
            getSearchProviderState(
                showSessionSuggestionsForCurrentEngine = false,
                searchEngineSource =
                    SearchEngineSource.Shortcut(
                        mockk(relaxed = true) {
                            every { resultsUrl } returns url
                        }
                    ),
                showSponsoredSuggestions = true,
            )

        val result = builder.getProvidersToAdd(state)

        val localSessionsProviders = result.filterIsInstance<SessionSuggestionProvider>()
        assertEquals(1, localSessionsProviders.size)
        assertNull(localSessionsProviders[0].resultsUriFilter)
    }

    @Test
    fun `GIVEN normal browsing mode and needing to show all local tabs suggestions and sponsored suggestions are disabled WHEN configuring providers THEN add the tabs provider`() {
        val settings: Settings = mockk(relaxed = true)
        val url = "https://www.test.com".toUri()
        every { components.settings } returns settings
        every { browsingModeManager.mode } returns BrowsingMode.Normal
        val state =
            getSearchProviderState(
                showSessionSuggestionsForCurrentEngine = false,
                searchEngineSource =
                    SearchEngineSource.Shortcut(
                        mockk(relaxed = true) {
                            every { resultsUrl } returns url
                        }
                    ),
                showSponsoredSuggestions = false,
            )

        val result = builder.getProvidersToAdd(state)

        val localSessionsProviders = result.filterIsInstance<SessionSuggestionProvider>()
        assertEquals(1, localSessionsProviders.size)
        assertNull(localSessionsProviders[0].resultsUriFilter)
    }

    @Test
    fun `GIVEN normal browsing mode and needing to show filtered local tabs suggestions WHEN configuring providers THEN add the tabs provider with an engine filter`() {
        val settings: Settings = mockk(relaxed = true)
        val url = "https://www.test.com".toUri()
        every { components.settings } returns settings
        every { browsingModeManager.mode } returns BrowsingMode.Normal
        val state =
            getSearchProviderState(
                showAllSessionSuggestions = false,
                searchEngineSource =
                    SearchEngineSource.Shortcut(
                        mockk(relaxed = true) {
                            every { resultsUrl } returns url
                        }
                    ),
            )

        val result = builder.getProvidersToAdd(state)

        val localSessionsProviders = result.filterIsInstance<SessionSuggestionProvider>()
        assertEquals(1, localSessionsProviders.size)
        assertNotNull(localSessionsProviders[0].resultsUriFilter)
    }

    @Test
    fun `GIVEN private browsing mode and needing to show tabs suggestions WHEN configuring providers THEN don't add the tabs provider`() {
        val settings: Settings = mockk(relaxed = true)
        every { components.settings } returns settings
        every { browsingModeManager.mode } returns BrowsingMode.Private
        val state = getSearchProviderState(searchEngineSource = SearchEngineSource.Shortcut(mockk(relaxed = true)))

        val result = builder.getProvidersToAdd(state)

        assertEquals(0, result.filterIsInstance<SessionSuggestionProvider>().size)
    }

    @Test
    fun `GIVEN needing to show all synced tabs suggestions and sponsored suggestions are enabled WHEN configuring providers THEN add the synced tabs provider with a sponsored filter`() {
        val settings: Settings = mockk(relaxed = true)
        val url = "https://www.test.com".toUri()
        every { components.settings } returns settings
        every { browsingModeManager.mode } returns BrowsingMode.Normal
        val state =
            getSearchProviderState(
                showSyncedTabsSuggestionsForCurrentEngine = false,
                searchEngineSource =
                    SearchEngineSource.Shortcut(
                        mockk(relaxed = true) {
                            every { resultsUrl } returns url
                        }
                    ),
                showSponsoredSuggestions = true,
            )

        val result = builder.getProvidersToAdd(state)

        val localSessionsProviders = result.filterIsInstance<SyncedTabsStorageSuggestionProvider>()
        assertEquals(1, localSessionsProviders.size)
        assertNotNull(localSessionsProviders[0].resultsUrlFilter)
    }

    @Test
    fun `GIVEN needing to show filtered synced tabs suggestions and sponsored suggestions are enabled WHEN configuring providers THEN add the synced tabs provider with an engine filter`() {
        val settings: Settings = mockk(relaxed = true)
        val url = "https://www.test.com".toUri()
        every { components.settings } returns settings
        every { browsingModeManager.mode } returns BrowsingMode.Normal
        val state =
            getSearchProviderState(
                showAllSyncedTabsSuggestions = false,
                searchEngineSource =
                    SearchEngineSource.Shortcut(
                        mockk(relaxed = true) {
                            every { resultsUrl } returns url
                        }
                    ),
                showSponsoredSuggestions = true,
            )

        val result = builder.getProvidersToAdd(state)

        val localSessionsProviders = result.filterIsInstance<SyncedTabsStorageSuggestionProvider>()
        assertEquals(1, localSessionsProviders.size)
        assertNotNull(localSessionsProviders[0].resultsUrlFilter)
    }

    @Test
    fun `GIVEN needing to show all synced tabs suggestions and sponsored suggestions are disabled WHEN configuring providers THEN add the synced tabs provider`() {
        val settings: Settings = mockk(relaxed = true)
        val url = "https://www.test.com".toUri()
        every { components.settings } returns settings
        every { browsingModeManager.mode } returns BrowsingMode.Normal
        val state =
            getSearchProviderState(
                showSyncedTabsSuggestionsForCurrentEngine = false,
                searchEngineSource =
                    SearchEngineSource.Shortcut(
                        mockk(relaxed = true) {
                            every { resultsUrl } returns url
                        }
                    ),
                showSponsoredSuggestions = false,
            )

        val result = builder.getProvidersToAdd(state)

        val localSessionsProviders = result.filterIsInstance<SyncedTabsStorageSuggestionProvider>()
        assertEquals(1, localSessionsProviders.size)
        assertNull(localSessionsProviders[0].resultsUrlFilter)
    }

    @Test
    fun `GIVEN needing to show all bookmarks suggestions and sponsored suggestions are enabled WHEN configuring providers THEN add the bookmarks provider with a sponsored filter`() {
        val settings: Settings = mockk(relaxed = true)
        val url = "https://www.test.com".toUri()
        every { components.settings } returns settings
        every { browsingModeManager.mode } returns BrowsingMode.Normal
        val state =
            getSearchProviderState(
                showBookmarksSuggestionsForCurrentEngine = false,
                searchEngineSource =
                    SearchEngineSource.Shortcut(
                        mockk(relaxed = true) {
                            every { resultsUrl } returns url
                        }
                    ),
                showSponsoredSuggestions = true,
            )

        val result = builder.getProvidersToAdd(state)

        val localSessionsProviders = result.filterIsInstance<BookmarksStorageSuggestionProvider>()
        assertEquals(1, localSessionsProviders.size)
        assertNotNull(localSessionsProviders[0].resultsUriFilter)
    }

    @Test
    fun `GIVEN needing to show all bookmarks suggestions and sponsored suggestions are disabled WHEN configuring providers THEN add the bookmarks provider`() {
        val settings: Settings = mockk(relaxed = true)
        val url = "https://www.test.com".toUri()
        every { components.settings } returns settings
        every { browsingModeManager.mode } returns BrowsingMode.Normal
        val state =
            getSearchProviderState(
                showBookmarksSuggestionsForCurrentEngine = false,
                searchEngineSource =
                    SearchEngineSource.Shortcut(
                        mockk(relaxed = true) {
                            every { resultsUrl } returns url
                        }
                    ),
                showSponsoredSuggestions = false,
            )

        val result = builder.getProvidersToAdd(state)

        val localSessionsProviders = result.filterIsInstance<BookmarksStorageSuggestionProvider>()
        assertEquals(1, localSessionsProviders.size)
        assertNull(localSessionsProviders[0].resultsUriFilter)
    }

    @Test
    fun `GIVEN needing to show filtered bookmarks suggestions WHEN configuring providers THEN add the bookmarks provider with an engine filter`() {
        val settings: Settings = mockk(relaxed = true)
        val url = "https://www.test.com".toUri()
        every { components.settings } returns settings
        every { browsingModeManager.mode } returns BrowsingMode.Normal
        val state =
            getSearchProviderState(
                showAllBookmarkSuggestions = false,
                searchEngineSource =
                    SearchEngineSource.Shortcut(
                        mockk(relaxed = true) {
                            every { resultsUrl } returns url
                        }
                    ),
            )

        val result = builder.getProvidersToAdd(state)

        val localSessionsProviders = result.filterIsInstance<BookmarksStorageSuggestionProvider>()
        assertEquals(1, localSessionsProviders.size)
        assertNotNull(localSessionsProviders[0].resultsUriFilter)
    }

    @Test
    fun `GIVEN a search is made by the user WHEN configuring providers THEN search engine suggestion provider should always be added`() {
        val settings: Settings = mockk(relaxed = true)
        every { components.settings } returns settings
        val state = getSearchProviderState(searchEngineSource = SearchEngineSource.Default(mockk(relaxed = true)))

        val result = builder.getProvidersToAdd(state)

        assertEquals(1, result.filterIsInstance<SearchEngineSuggestionProvider>().size)
    }

    @Test
    fun `GIVEN a search from the default engine with all suggestions asked and sponsored suggestions are enabled WHEN configuring providers THEN add them all`() {
        val settings: Settings = mockk(relaxed = true)
        val url = "https://www.test.com".toUri()
        every { components.settings } returns settings
        every { browsingModeManager.mode } returns BrowsingMode.Normal
        val state =
            getSearchProviderState(
                searchEngineSource =
                    SearchEngineSource.Default(
                        mockk(relaxed = true) {
                            every { resultsUrl } returns url
                        }
                    ),
                showSponsoredSuggestions = true,
            )

        val result = builder.getProvidersToAdd(state)

        val historyProviders: List<HistoryStorageSuggestionProvider> =
            result.filterIsInstance<HistoryStorageSuggestionProvider>()
        assertEquals(2, historyProviders.size)
        assertNotNull(historyProviders[0].resultsUriFilter) // the general history provider
        assertNotNull(historyProviders[1].resultsUriFilter) // the filtered history provider
        val bookmarksProviders: List<BookmarksStorageSuggestionProvider> =
            result.filterIsInstance<BookmarksStorageSuggestionProvider>()
        assertEquals(2, bookmarksProviders.size)
        assertNotNull(bookmarksProviders[0].resultsUriFilter) // the general bookmarks provider
        assertNotNull(bookmarksProviders[1].resultsUriFilter) // the filtered bookmarks provider
        assertEquals(1, result.filterIsInstance<SearchActionProvider>().size)
        assertEquals(1, result.filterIsInstance<SearchSuggestionProvider>().size)
        val syncedTabsProviders: List<SyncedTabsStorageSuggestionProvider> =
            result.filterIsInstance<SyncedTabsStorageSuggestionProvider>()
        assertEquals(2, syncedTabsProviders.size)
        assertNotNull(syncedTabsProviders[0].resultsUrlFilter) // the general synced tabs provider
        assertNotNull(syncedTabsProviders[1].resultsUrlFilter) // the filtered synced tabs provider
        val localTabsProviders: List<SessionSuggestionProvider> = result.filterIsInstance<SessionSuggestionProvider>()
        assertEquals(2, localTabsProviders.size)
        assertNull(localTabsProviders[0].resultsUriFilter) // the general tabs provider
        assertNotNull(localTabsProviders[1].resultsUriFilter) // the filtered tabs provider
        assertEquals(1, result.filterIsInstance<SearchEngineSuggestionProvider>().size)
    }

    @Test
    fun `GIVEN a search from the default engine with all suggestions asked and sponsored suggestions are disabled WHEN configuring providers THEN add them all`() {
        val settings: Settings = mockk(relaxed = true)
        val url = "https://www.test.com".toUri()
        every { components.settings } returns settings
        every { browsingModeManager.mode } returns BrowsingMode.Normal
        val state =
            getSearchProviderState(
                searchEngineSource =
                    SearchEngineSource.Default(
                        mockk(relaxed = true) {
                            every { resultsUrl } returns url
                        }
                    ),
                showSponsoredSuggestions = false,
            )

        val result = builder.getProvidersToAdd(state)

        val historyProviders: List<HistoryStorageSuggestionProvider> =
            result.filterIsInstance<HistoryStorageSuggestionProvider>()
        assertEquals(2, historyProviders.size)
        assertNull(historyProviders[0].resultsUriFilter) // the general history provider
        assertNotNull(historyProviders[1].resultsUriFilter) // the filtered history provider
        val bookmarksProviders: List<BookmarksStorageSuggestionProvider> =
            result.filterIsInstance<BookmarksStorageSuggestionProvider>()
        assertEquals(2, bookmarksProviders.size)
        assertNull(bookmarksProviders[0].resultsUriFilter) // the general bookmarks provider
        assertNotNull(bookmarksProviders[1].resultsUriFilter) // the filtered bookmarks provider
        assertEquals(1, result.filterIsInstance<SearchActionProvider>().size)
        assertEquals(1, result.filterIsInstance<SearchSuggestionProvider>().size)
        val syncedTabsProviders: List<SyncedTabsStorageSuggestionProvider> =
            result.filterIsInstance<SyncedTabsStorageSuggestionProvider>()
        assertEquals(2, syncedTabsProviders.size)
        assertNull(syncedTabsProviders[0].resultsUrlFilter) // the general synced tabs provider
        assertNotNull(syncedTabsProviders[1].resultsUrlFilter) // the filtered synced tabs provider
        val localTabsProviders: List<SessionSuggestionProvider> = result.filterIsInstance<SessionSuggestionProvider>()
        assertEquals(2, localTabsProviders.size)
        assertNull(localTabsProviders[0].resultsUriFilter) // the general tabs provider
        assertNotNull(localTabsProviders[1].resultsUriFilter) // the filtered tabs provider
        assertEquals(1, result.filterIsInstance<SearchEngineSuggestionProvider>().size)
    }

    @Test
    fun `GIVEN a search from the default engine with no suggestions asked WHEN configuring providers THEN retain explicit search`() {
        val settings: Settings = mockk(relaxed = true)
        every { components.settings } returns settings
        every { browsingModeManager.mode } returns BrowsingMode.Normal
        val state =
            getSearchProviderState(
                showHistorySuggestionsForCurrentEngine = false,
                showAllHistorySuggestions = false,
                showBookmarksSuggestionsForCurrentEngine = false,
                showAllBookmarkSuggestions = false,
                showSearchSuggestions = false,
                showSyncedTabsSuggestionsForCurrentEngine = false,
                showAllSyncedTabsSuggestions = false,
                showSessionSuggestionsForCurrentEngine = false,
                showAllSessionSuggestions = false,
                searchEngineSource = SearchEngineSource.Default(mockk(relaxed = true)),
            )

        val result = builder.getProvidersToAdd(state)

        assertEquals(0, result.filterIsInstance<HistoryStorageSuggestionProvider>().size)
        assertEquals(0, result.filterIsInstance<BookmarksStorageSuggestionProvider>().size)
        assertEquals(1, result.filterIsInstance<SearchActionProvider>().size)
        assertEquals(0, result.filterIsInstance<SearchSuggestionProvider>().size)
        assertEquals(0, result.filterIsInstance<SyncedTabsStorageSuggestionProvider>().size)
        assertEquals(0, result.filterIsInstance<SessionSuggestionProvider>().size)
        assertEquals(1, result.filterIsInstance<SearchEngineSuggestionProvider>().size)
    }

    @Test
    fun `GIVEN sponsored suggestions are enabled WHEN configuring providers THEN add the Firefox Suggest suggestion provider`() {
        val settings: Settings = mockk(relaxed = true)
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                showSponsoredSuggestions = true,
                showNonSponsoredSuggestions = false,
            )

        val result = builder.getProvidersToAdd(state)

        val fxSuggestProvider = result.firstOrNull { it is FxSuggestSuggestionProvider }
        assertNotNull(fxSuggestProvider)
    }

    @Test
    fun `GIVEN non-sponsored suggestions are enabled WHEN configuring providers THEN add the Firefox Suggest suggestion provider`() {
        val settings: Settings = mockk(relaxed = true)
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                showSponsoredSuggestions = false,
                showNonSponsoredSuggestions = true,
            )

        val result = builder.getProvidersToAdd(state)

        val fxSuggestProvider = result.firstOrNull { it is FxSuggestSuggestionProvider }
        assertNotNull(fxSuggestProvider)
    }

    @Test
    fun `GIVEN sponsored and non-sponsored suggestions are enabled WHEN configuring providers THEN add the Firefox Suggest suggestion provider`() {
        val settings: Settings = mockk(relaxed = true)
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                showSponsoredSuggestions = true,
                showNonSponsoredSuggestions = true,
            )

        val result = builder.getProvidersToAdd(state)

        val fxSuggestProvider = result.firstOrNull { it is FxSuggestSuggestionProvider }
        assertNotNull(fxSuggestProvider)
    }

    @Test
    fun `GIVEN sponsored and non-sponsored suggestions are disabled WHEN configuring providers THEN don't add the Firefox Suggest suggestion provider`() {
        val settings: Settings = mockk(relaxed = true)
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                showSponsoredSuggestions = false,
                showNonSponsoredSuggestions = false,
            )

        val result = builder.getProvidersToAdd(state)

        val fxSuggestProvider = result.firstOrNull { it is FxSuggestSuggestionProvider }
        assertNull(fxSuggestProvider)
    }

    @Test
    fun `GIVEN a valid search engine and history metadata enabled WHEN creating a history provider for that engine THEN return a history metadata provider with engine filter`() {
        val settings: Settings = mockk {
            every { historyMetadataUIFeature } returns true
        }
        every { components.settings } returns settings
        val searchEngineSource = SearchEngineSource.Shortcut(mockk(relaxed = true))
        val state = getSearchProviderState(searchEngineSource = searchEngineSource)

        val result = builder.getHistoryProvider(filter = builder.getFilterForCurrentEngineResults(state))

        assertNotNull(result)
        assertIs<CombinedHistorySuggestionProvider>(result)
        assertNotNull(result.resultsUriFilter)
        assertEquals(SearchSuggestionsProvidersBuilder.METADATA_SUGGESTION_LIMIT, result.getMaxNumberOfSuggestions())
    }

    @Test
    fun `GIVEN a valid search engine and history metadata disabled WHEN creating a history provider for that engine THEN return a history metadata provider with an engine filter`() {
        val settings: Settings = mockk {
            every { historyMetadataUIFeature } returns false
        }
        every { components.settings } returns settings
        val searchEngineSource = SearchEngineSource.Shortcut(mockk(relaxed = true))
        val state = getSearchProviderState(searchEngineSource = searchEngineSource)

        val result = builder.getHistoryProvider(filter = builder.getFilterForCurrentEngineResults(state))

        assertNotNull(result)
        assertIs<HistoryStorageSuggestionProvider>(result)
        assertNotNull(result.resultsUriFilter)
        assertEquals(SearchSuggestionsProvidersBuilder.METADATA_SUGGESTION_LIMIT, result.getMaxNumberOfSuggestions())
    }

    @Test
    fun `GIVEN a search engine is not available WHEN asking for a search term provider THEN return null`() {
        val searchEngineSource: SearchEngineSource = SearchEngineSource.None

        val result = builder.getSearchTermSuggestionsProvider(searchEngineSource)

        assertNull(result)
    }

    @Test
    fun `GIVEN a search engine is available WHEN asking for a search term provider THEN return a valid provider`() {
        val searchEngineSource = SearchEngineSource.Default(mockk(relaxed = true))

        val result = builder.getSearchTermSuggestionsProvider(searchEngineSource)

        assertIs<SearchTermSuggestionsProvider>(result)
    }

    @Test
    fun `GIVEN the default search engine WHEN asking for a search term provider THEN the provider should have a suggestions header`() {
        val engine: SearchEngine = mockk {
            every { name } returns "Test"
        }
        every { searchEngineProvider.getDefaultSearchEngine() } returns engine

        val searchEngineSource = SearchEngineSource.Default(engine)

        val result = builder.getSearchTermSuggestionsProvider(searchEngineSource)

        assertIs<SearchTermSuggestionsProvider>(result)
        assertEquals("Test search", result.groupTitle())
    }

    @Test
    fun `GIVEN a shortcut search engine selected WHEN asking for a search term provider THEN the provider should not have a suggestions header`() {
        val otherEngine: SearchEngine = mockk {
            every { name } returns "Other"
        }
        val searchEngineSource = SearchEngineSource.Shortcut(otherEngine)

        val result = builder.getSearchTermSuggestionsProvider(searchEngineSource)

        assertIs<SearchTermSuggestionsProvider>(result)
        assertNull(result.groupTitle())
    }

    @Test
    fun `GIVEN the default search engine is unknown WHEN asking for a search term provider THEN the provider should not have a suggestions header`() {
        every { searchEngineProvider.getDefaultSearchEngine() } returns null

        val otherEngine: SearchEngine = mockk {
            every { name } returns "Other"
        }
        val searchEngineSource = SearchEngineSource.Shortcut(otherEngine)

        val result = builder.getSearchTermSuggestionsProvider(searchEngineSource)

        assertIs<SearchTermSuggestionsProvider>(result)
        assertNull(result.groupTitle())
    }

    @Test
    fun `GIVEN history search term suggestions disabled WHEN getting suggestions providers THEN don't search term provider of past searches`() {
        every { components.settings } returns mockk(relaxed = true)
        val state =
            getSearchProviderState(
                searchEngineSource = SearchEngineSource.Default(mockk(relaxed = true)),
                showSearchTermHistory = false,
                showRecentSearches = false,
            )
        val result = builder.getProvidersToAdd(state)

        assertEquals(0, result.filterIsInstance<SearchTermSuggestionsProvider>().size)
    }

    @Test
    fun `GIVEN history search term suggestions enabled WHEN getting suggestions providers THEN add a search term provider of past searches`() {
        every { components.settings } returns mockk(relaxed = true)
        val state =
            getSearchProviderState(
                searchEngineSource = SearchEngineSource.Default(mockk(relaxed = true)),
                showSearchTermHistory = true,
                showRecentSearches = false,
            )
        val result = builder.getProvidersToAdd(state)

        assertEquals(1, result.filterIsInstance<SearchTermSuggestionsProvider>().size)
    }

    @Test
    fun `GIVEN sponsored suggestions are enabled WHEN getting a filter to exclude sponsored suggestions THEN return the filter`() {
        every { components.settings } returns
            mockk(relaxed = true) {
                every { frecencyFilterQuery } returns "query=value"
            }
        val state =
            getSearchProviderState(
                searchEngineSource = SearchEngineSource.Default(mockk(relaxed = true)),
                showSponsoredSuggestions = true,
            )
        val filter = builder.getFilterToExcludeSponsoredResults(state)

        assertEquals(SearchSuggestionsProvidersBuilder.SearchResultFilter.ExcludeSponsored("query=value"), filter)
    }

    @Test
    fun `GIVEN sponsored suggestions are disabled WHEN getting a filter to exclude sponsored suggestions THEN return null`() {
        every { components.settings } returns
            mockk(relaxed = true) {
                every { frecencyFilterQuery } returns "query=value"
            }
        val state =
            getSearchProviderState(
                searchEngineSource = SearchEngineSource.Default(mockk(relaxed = true)),
                showSponsoredSuggestions = false,
            )
        val filter = builder.getFilterToExcludeSponsoredResults(state)

        assertNull(filter)
    }

    @Test
    fun `GIVEN a sponsored query parameter and a sponsored filter WHEN a URL contains the sponsored query parameter THEN that URL should be excluded`() {
        every { components.settings } returns
            mockk(relaxed = true) {
                every { frecencyFilterQuery } returns "query=value"
            }
        val state =
            getSearchProviderState(
                searchEngineSource = SearchEngineSource.Default(mockk(relaxed = true)),
                showSponsoredSuggestions = true,
            )
        val filter = requireNotNull(builder.getFilterToExcludeSponsoredResults(state))

        assertFalse(filter.shouldIncludeUri("http://example.com?query=value".toUri()))
        assertFalse(filter.shouldIncludeUri("http://example.com/a?query=value".toUri()))
        assertFalse(filter.shouldIncludeUri("http://example.com/a?b=c&query=value".toUri()))
        assertFalse(filter.shouldIncludeUri("http://example.com/a?b=c&query=value&d=e".toUri()))

        assertFalse(filter.shouldIncludeUrl("http://example.com?query=value"))
        assertFalse(filter.shouldIncludeUrl("http://example.com/a?query=value"))
        assertFalse(filter.shouldIncludeUrl("http://example.com/a?b=c&query=value"))
        assertFalse(filter.shouldIncludeUrl("http://example.com/a?b=c&query=value&d=e"))
    }

    @Test
    fun `GIVEN a sponsored query parameter and a sponsored filter WHEN a URL does not contain the sponsored query parameter THEN that URL should be included`() {
        every { components.settings } returns
            mockk(relaxed = true) {
                every { frecencyFilterQuery } returns "query=value"
            }
        val state =
            getSearchProviderState(
                searchEngineSource = SearchEngineSource.Default(mockk(relaxed = true)),
                showSponsoredSuggestions = true,
            )
        val filter = requireNotNull(builder.getFilterToExcludeSponsoredResults(state))

        assertTrue(filter.shouldIncludeUri("http://example.com".toUri()))
        assertTrue(filter.shouldIncludeUri("http://example.com?query".toUri()))
        assertTrue(filter.shouldIncludeUri("http://example.com/a".toUri()))
        assertTrue(filter.shouldIncludeUri("http://example.com/a?b".toUri()))
        assertTrue(filter.shouldIncludeUri("http://example.com/a?b&c=d".toUri()))

        assertTrue(filter.shouldIncludeUrl("http://example.com"))
        assertTrue(filter.shouldIncludeUrl("http://example.com?query"))
        assertTrue(filter.shouldIncludeUrl("http://example.com/a"))
        assertTrue(filter.shouldIncludeUrl("http://example.com/a?b"))
        assertTrue(filter.shouldIncludeUrl("http://example.com/a?b&c=d"))
    }

    @Test
    fun `GIVEN an engine with a results URL and an engine filter WHEN a URL matches the results URL THEN that URL should be included`() {
        val url = "http://test.com".toUri()
        val state =
            getSearchProviderState(
                searchEngineSource =
                    SearchEngineSource.Shortcut(
                        mockk(relaxed = true) {
                            every { resultsUrl } returns url
                        }
                    )
            )

        val filter = requireNotNull(builder.getFilterForCurrentEngineResults(state))

        assertTrue(filter.shouldIncludeUri("http://test.com".toUri()))
        assertTrue(filter.shouldIncludeUri("http://test.com/a".toUri()))
        assertTrue(filter.shouldIncludeUri("http://mobile.test.com".toUri()))
        assertTrue(filter.shouldIncludeUri("http://mobile.test.com/a".toUri()))

        assertTrue(filter.shouldIncludeUrl("http://test.com"))
        assertTrue(filter.shouldIncludeUrl("http://test.com/a"))
    }

    @Test
    fun `GIVEN an engine with a results URL and an engine filter WHEN a URL does not match the results URL THEN that URL should be excluded`() {
        val url = "http://test.com".toUri()
        val state =
            getSearchProviderState(
                searchEngineSource =
                    SearchEngineSource.Shortcut(
                        mockk(relaxed = true) {
                            every { resultsUrl } returns url
                        }
                    )
            )

        val filter = requireNotNull(builder.getFilterForCurrentEngineResults(state))

        assertFalse(filter.shouldIncludeUri("http://other.com".toUri()))
        assertFalse(filter.shouldIncludeUri("http://subdomain.test.com".toUri()))

        assertFalse(filter.shouldIncludeUrl("http://mobile.test.com"))
    }

    @Test
    fun `GIVEN should show trending searches WHEN configuring providers THEN add the trending search provider`() {
        val settings: Settings = mockk(relaxed = true)
        every { components.settings } returns settings
        val state = getSearchProviderState(showTrendingSearches = true)

        val result = builder.getProvidersToAdd(state)

        assertEquals(1, result.filterIsInstance<TrendingSearchProvider>().size)
    }

    @Test
    fun `GIVEN should not show trending searches WHEN configuring providers THEN don't add the trending search provider`() {
        val settings: Settings = mockk(relaxed = true)
        every { components.settings } returns settings
        val state = getSearchProviderState(showTrendingSearches = false)

        val result = builder.getProvidersToAdd(state)

        assertEquals(0, result.filterIsInstance<TrendingSearchProvider>().size)
    }

    @Test
    fun `GIVEN should show recent searches WHEN configuring providers THEN add the recent search suggestions provider`() {
        val settings: Settings = mockk(relaxed = true)
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                searchEngineSource = SearchEngineSource.Default(mockk(relaxed = true)),
                showSearchTermHistory = false,
                showRecentSearches = true,
            )

        val result = builder.getProvidersToAdd(state)

        assertEquals(1, result.filterIsInstance<RecentSearchSuggestionsProvider>().size)
    }

    @Test
    fun `GIVEN should not show recent searches WHEN configuring providers THEN don't add the recent search suggestions provider`() {
        val settings: Settings = mockk(relaxed = true)
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                searchEngineSource = SearchEngineSource.Default(mockk(relaxed = true)),
                showSearchTermHistory = false,
                showRecentSearches = false,
            )

        val result = builder.getProvidersToAdd(state)

        assertEquals(0, result.filterIsInstance<RecentSearchSuggestionsProvider>().size)
    }

    @Test
    fun `GIVEN should show stock cards WHEN configuring providers THEN add the stocks online suggestion provider`() {
        val settings: Settings = mockk(relaxed = true)
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                searchEngineSource = SearchEngineSource.Default(mockk(relaxed = true)),
                showStocksSuggestions = true,
            )

        val result = builder.getProvidersToAdd(state)

        assertEquals(1, result.filterIsInstance<StocksOnlineSuggestionProvider>().size)
    }

    @Test
    fun `GIVEN should not show stock cards WHEN configuring providers THEN don't add the stocks online suggestion provider`() {
        val settings: Settings = mockk(relaxed = true)
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                searchEngineSource = SearchEngineSource.Default(mockk(relaxed = true)),
                showStocksSuggestions = false,
            )

        val result = builder.getProvidersToAdd(state)

        assertEquals(0, result.filterIsInstance<StocksOnlineSuggestionProvider>().size)
    }

    @Test
    fun `GIVEN should show sport cards WHEN configuring providers THEN add the sports online suggestion provider`() {
        val settings: Settings = mockk(relaxed = true)
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                searchEngineSource = SearchEngineSource.Default(mockk(relaxed = true)),
                showSportsSuggestions = true,
            )

        val result = builder.getProvidersToAdd(state)

        assertEquals(1, result.filterIsInstance<SportsOnlineSuggestionProvider>().size)
    }

    @Test
    fun `GIVEN should not show sport cards WHEN configuring providers THEN don't add the sports online suggestion provider`() {
        val settings: Settings = mockk(relaxed = true)
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                searchEngineSource = SearchEngineSource.Default(mockk(relaxed = true)),
                showSportsSuggestions = false,
            )

        val result = builder.getProvidersToAdd(state)

        assertEquals(0, result.filterIsInstance<SportsOnlineSuggestionProvider>().size)
    }

    @Test
    fun `GIVEN should show flight cards WHEN configuring providers THEN add the flights online suggestion provider`() {
        val settings: Settings = mockk(relaxed = true)
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                searchEngineSource = SearchEngineSource.Default(mockk(relaxed = true)),
                showFlightsSuggestions = true,
            )

        val result = builder.getProvidersToAdd(state)

        assertEquals(1, result.filterIsInstance<FlightsOnlineSuggestionProvider>().size)
    }

    @Test
    fun `GIVEN should not show flight cards WHEN configuring providers THEN don't add the flights online suggestion provider`() {
        val settings: Settings = mockk(relaxed = true)
        every { components.settings } returns settings
        val state =
            getSearchProviderState(
                searchEngineSource = SearchEngineSource.Default(mockk(relaxed = true)),
                showFlightsSuggestions = false,
            )

        val result = builder.getProvidersToAdd(state)

        assertEquals(0, result.filterIsInstance<FlightsOnlineSuggestionProvider>().size)
    }
}

/** Get a default [SearchProviderState] that by default will ask for all types of suggestions. */
private fun getSearchProviderState(
    showSearchTermHistory: Boolean = true,
    showHistorySuggestionsForCurrentEngine: Boolean = true,
    showAllHistorySuggestions: Boolean = true,
    showBookmarksSuggestionsForCurrentEngine: Boolean = true,
    showAllBookmarkSuggestions: Boolean = true,
    showSearchSuggestions: Boolean = true,
    showSyncedTabsSuggestionsForCurrentEngine: Boolean = true,
    showAllSyncedTabsSuggestions: Boolean = true,
    showSessionSuggestionsForCurrentEngine: Boolean = true,
    showAllSessionSuggestions: Boolean = true,
    searchEngineSource: SearchEngineSource = SearchEngineSource.None,
    showSponsoredSuggestions: Boolean = true,
    showNonSponsoredSuggestions: Boolean = true,
    showStocksSuggestions: Boolean = true,
    showSportsSuggestions: Boolean = true,
    showFlightsSuggestions: Boolean = true,
    showTrendingSearches: Boolean = true,
    showRecentSearches: Boolean = true,
) =
    SearchProviderState(
        showSearchTermHistory = showSearchTermHistory,
        showHistorySuggestionsForCurrentEngine = showHistorySuggestionsForCurrentEngine,
        showAllHistorySuggestions = showAllHistorySuggestions,
        showBookmarksSuggestionsForCurrentEngine = showBookmarksSuggestionsForCurrentEngine,
        showAllBookmarkSuggestions = showAllBookmarkSuggestions,
        showSearchSuggestions = showSearchSuggestions,
        showSyncedTabsSuggestionsForCurrentEngine = showSyncedTabsSuggestionsForCurrentEngine,
        showAllSyncedTabsSuggestions = showAllSyncedTabsSuggestions,
        showSessionSuggestionsForCurrentEngine = showSessionSuggestionsForCurrentEngine,
        showAllSessionSuggestions = showAllSessionSuggestions,
        showSponsoredSuggestions = showSponsoredSuggestions,
        showNonSponsoredSuggestions = showNonSponsoredSuggestions,
        showStocksSuggestions = showStocksSuggestions,
        showSportsSuggestions = showSportsSuggestions,
        showFlightsSuggestions = showFlightsSuggestions,
        showTrendingSearches = showTrendingSearches,
        showRecentSearches = showRecentSearches,
        searchEngineSource = searchEngineSource,
    )
