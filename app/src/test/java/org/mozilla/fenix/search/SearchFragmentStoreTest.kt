/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.search

import android.content.Intent
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import mozilla.components.browser.state.search.RegionState
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.ContentState
import mozilla.components.browser.state.state.SearchState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.compose.browser.awesomebar.internal.CurrentTabData
import mozilla.components.concept.awesomebar.AwesomeBar.SuggestionProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.Components
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.components.metrics.MetricsUtils
import org.mozilla.fenix.search.SearchFragmentAction.SearchProvidersUpdated
import org.mozilla.fenix.search.SearchFragmentAction.SearchStarted
import org.mozilla.fenix.search.SearchFragmentAction.SearchSuggestionsVisibilityUpdated
import org.mozilla.fenix.search.fixtures.EMPTY_SEARCH_FRAGMENT_STATE
import org.mozilla.fenix.utils.Settings
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchFragmentStoreTest {
    @MockK private lateinit var searchEngine: SearchEngine

    @MockK private lateinit var activity: HomeActivity

    @RelaxedMockK private lateinit var components: Components

    @RelaxedMockK private lateinit var settings: Settings

    @RelaxedMockK private lateinit var appStore: AppStore

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { activity.browsingModeManager } returns
            object : BrowsingModeManager {
                override var mode: BrowsingMode = BrowsingMode.Normal

                override fun updateMode(intent: Intent?) = Unit
            }
        every { components.settings } returns settings
        every { searchEngine.trendingUrl } returns null

        appStore = AppStore(initialState = AppState(mode = BrowsingMode.Normal))
        every { components.appStore } returns appStore
    }

    @Test
    fun `createInitialSearchFragmentState with no tab in normal browsing mode`() {
        appStore = AppStore(initialState = AppState(mode = BrowsingMode.Normal))
        every { components.appStore } returns appStore
        every { components.core.store.state } returns BrowserState()
        every { settings.shouldShowHistorySuggestions } returns true
        every { settings.shouldShowSearchSuggestions } returns true
        every { settings.shouldShowSearchSuggestionsInPrivate } returns false
        every { settings.enableFxSuggest } returns true
        every { settings.showSponsoredSuggestions } returns true
        every { settings.showNonSponsoredSuggestions } returns true
        every { settings.shouldUseBottomToolbar } returns true
        every { settings.isSearchOptimizationEnabled } returns true
        every { settings.shouldShowSearchOptimizationCards } returns true
        every { settings.shouldShowSearchOptimizationStockCard } returns true
        every { settings.shouldShowSearchOptimizationSportCard } returns true
        every { settings.shouldShowSearchOptimizationFlightCard } returns true

        val expected =
            EMPTY_SEARCH_FRAGMENT_STATE.copy(
                searchSuggestionsOrientedAtBottom = true,
                showSearchSuggestionsFromCurrentEngine = true,
                showSearchTermHistory = true,
                showAllHistorySuggestions = true,
                showAllSessionSuggestions = true,
                showSponsoredSuggestions = true,
                showNonSponsoredSuggestions = true,
                showStocksSuggestions = true,
                showSportsSuggestions = true,
                showFlightsSuggestions = true,
                showQrButton = true,
                pastedText = "pastedText",
                searchAccessPoint = MetricsUtils.Source.ACTION,
            )

        assertEquals(
            expected,
            createInitialSearchFragmentState(
                activity,
                components,
                tabId = null,
                pastedText = "pastedText",
                searchAccessPoint = MetricsUtils.Source.ACTION,
                isAndroidAutomotiveAvailable = false,
            ),
        )
        assertEquals(
            expected.copy(tabId = "tabId"),
            createInitialSearchFragmentState(
                activity,
                components,
                tabId = "tabId",
                pastedText = "pastedText",
                searchAccessPoint = MetricsUtils.Source.ACTION,
                isAndroidAutomotiveAvailable = false,
            ),
        )
        assertEquals(
            expected.copy(showQrButton = false),
            createInitialSearchFragmentState(
                activity,
                components,
                tabId = null,
                pastedText = "pastedText",
                searchAccessPoint = MetricsUtils.Source.ACTION,
                isAndroidAutomotiveAvailable = true,
            ),
        )

        verify(exactly = 3) { shouldShowSearchSuggestions(BrowsingMode.Normal, settings) }
    }

    @Test
    fun `createInitialSearchFragmentState with no tab in private browsing mode`() {
        appStore = AppStore(initialState = AppState(mode = BrowsingMode.Private))
        every { components.appStore } returns appStore
        every { components.core.store.state } returns BrowserState()
        every { settings.shouldShowHistorySuggestions } returns true
        every { settings.shouldShowSearchSuggestions } returns true
        every { settings.shouldShowSearchSuggestionsInPrivate } returns false
        every { settings.enableFxSuggest } returns true
        every { settings.showSponsoredSuggestions } returns true
        every { settings.showNonSponsoredSuggestions } returns true
        every { settings.shouldUseBottomToolbar } returns true
        every { settings.isSearchOptimizationEnabled } returns true
        every { settings.shouldShowSearchOptimizationCards } returns true
        every { settings.shouldShowSearchOptimizationStockCard } returns true
        every { settings.shouldShowSearchOptimizationSportCard } returns true
        every { settings.shouldShowSearchOptimizationFlightCard } returns true

        val expected =
            EMPTY_SEARCH_FRAGMENT_STATE.copy(
                searchSuggestionsOrientedAtBottom = true,
                showSearchTermHistory = true,
                showAllHistorySuggestions = true,
                showAllSessionSuggestions = true,
                showStocksSuggestions = true,
                showSportsSuggestions = true,
                showFlightsSuggestions = true,
                showQrButton = true,
                pastedText = "pastedText",
                searchAccessPoint = MetricsUtils.Source.ACTION,
            )

        assertEquals(
            expected,
            createInitialSearchFragmentState(
                activity,
                components,
                tabId = null,
                pastedText = "pastedText",
                searchAccessPoint = MetricsUtils.Source.ACTION,
                isAndroidAutomotiveAvailable = false,
            ),
        )
    }

    @Test
    fun `createInitialSearchFragmentState with tab`() {
        every { settings.shouldUseBottomToolbar } returns true
        appStore = AppStore(initialState = AppState(mode = BrowsingMode.Private))
        every { components.appStore } returns appStore
        every { components.core.store.state } returns
            BrowserState(
                tabs =
                    listOf(
                        TabSessionState(
                            id = "tabId",
                            content =
                                ContentState(
                                    url = "https://example.com",
                                    searchTerms = "search terms",
                                ),
                        )
                    )
            )

        assertEquals(
            EMPTY_SEARCH_FRAGMENT_STATE.copy(
                query = "https://example.com",
                currentTabData = CurrentTabData("search terms", "https://example.com", null),
                searchTerms = "search terms",
                searchSuggestionsOrientedAtBottom = true,
                showAllSessionSuggestions = true,
                showQrButton = true,
                tabId = "tabId",
                pastedText = "",
                searchAccessPoint = MetricsUtils.Source.SHORTCUT,
            ),
            createInitialSearchFragmentState(
                activity,
                components,
                tabId = "tabId",
                pastedText = "",
                searchAccessPoint = MetricsUtils.Source.SHORTCUT,
                isAndroidAutomotiveAvailable = false,
            ),
        )
    }

    @Test
    fun `GIVEN sponsored and non-sponsored suggestions are enabled and Firefox Suggest is disabled WHEN the initial state is created THEN neither are displayed`() {
        appStore = AppStore(initialState = AppState(mode = BrowsingMode.Normal))
        every { components.appStore } returns appStore
        every { components.core.store.state } returns BrowserState()
        every { settings.enableFxSuggest } returns false
        every { settings.showSponsoredSuggestions } returns true
        every { settings.showNonSponsoredSuggestions } returns true

        val initialState =
            createInitialSearchFragmentState(
                activity,
                components,
                tabId = null,
                pastedText = "pastedText",
                searchAccessPoint = MetricsUtils.Source.ACTION,
                isAndroidAutomotiveAvailable = false,
            )
        assertFalse(initialState.showSponsoredSuggestions)
        assertFalse(initialState.showNonSponsoredSuggestions)
    }

    @Test
    fun `GIVEN non-sponsored suggestions is disabled WHEN the initial state is created THEN all suggestion cards are not displayed`() {
        every { components.core.store.state } returns BrowserState()
        every { settings.enableFxSuggest } returns true
        every { settings.showNonSponsoredSuggestions } returns false
        every { settings.isSearchOptimizationEnabled } returns true
        every { settings.shouldShowSearchOptimizationCards } returns true
        every { settings.shouldShowSearchOptimizationStockCard } returns true
        every { settings.shouldShowSearchOptimizationSportCard } returns true
        every { settings.shouldShowSearchOptimizationFlightCard } returns true

        val initialState =
            createInitialSearchFragmentState(
                activity,
                components,
                tabId = null,
                pastedText = "pastedText",
                searchAccessPoint = MetricsUtils.Source.ACTION,
                isAndroidAutomotiveAvailable = false,
            )
        assertFalse(initialState.showStocksSuggestions)
        assertFalse(initialState.showSportsSuggestions)
        assertFalse(initialState.showFlightsSuggestions)
    }

    @Test
    fun `GIVEN search optimization is disabled WHEN the initial state is created THEN all suggestion cards are not displayed`() {
        every { components.core.store.state } returns BrowserState()
        every { settings.enableFxSuggest } returns true
        every { settings.showNonSponsoredSuggestions } returns true
        every { settings.isSearchOptimizationEnabled } returns false
        every { settings.shouldShowSearchOptimizationCards } returns true
        every { settings.shouldShowSearchOptimizationStockCard } returns true
        every { settings.shouldShowSearchOptimizationSportCard } returns true
        every { settings.shouldShowSearchOptimizationFlightCard } returns true

        val initialState =
            createInitialSearchFragmentState(
                activity,
                components,
                tabId = null,
                pastedText = "pastedText",
                searchAccessPoint = MetricsUtils.Source.ACTION,
                isAndroidAutomotiveAvailable = false,
            )
        assertFalse(initialState.showStocksSuggestions)
        assertFalse(initialState.showSportsSuggestions)
        assertFalse(initialState.showFlightsSuggestions)
    }

    @Test
    fun `GIVEN suggestion cards is disabled WHEN the initial state is created THEN all suggestion cards are not displayed`() {
        every { components.core.store.state } returns BrowserState()
        every { settings.enableFxSuggest } returns true
        every { settings.showNonSponsoredSuggestions } returns true
        every { settings.isSearchOptimizationEnabled } returns true
        every { settings.shouldShowSearchOptimizationCards } returns false
        every { settings.shouldShowSearchOptimizationStockCard } returns true
        every { settings.shouldShowSearchOptimizationSportCard } returns true
        every { settings.shouldShowSearchOptimizationFlightCard } returns true

        val initialState =
            createInitialSearchFragmentState(
                activity,
                components,
                tabId = null,
                pastedText = "pastedText",
                searchAccessPoint = MetricsUtils.Source.ACTION,
                isAndroidAutomotiveAvailable = false,
            )
        assertFalse(initialState.showStocksSuggestions)
        assertFalse(initialState.showSportsSuggestions)
        assertFalse(initialState.showFlightsSuggestions)
    }

    @Test
    fun `GIVEN stock cards is disabled WHEN the initial state is created THEN the stock card is not displayed`() {
        every { components.core.store.state } returns BrowserState()
        every { settings.enableFxSuggest } returns true
        every { settings.showNonSponsoredSuggestions } returns true
        every { settings.isSearchOptimizationEnabled } returns true
        every { settings.shouldShowSearchOptimizationCards } returns true
        every { settings.shouldShowSearchOptimizationStockCard } returns false

        val initialState =
            createInitialSearchFragmentState(
                activity,
                components,
                tabId = null,
                pastedText = "pastedText",
                searchAccessPoint = MetricsUtils.Source.ACTION,
                isAndroidAutomotiveAvailable = false,
            )
        assertFalse(initialState.showStocksSuggestions)
    }

    @Test
    fun `GIVEN sport cards is disabled WHEN the initial state is created THEN the sport card is not displayed`() {
        every { components.core.store.state } returns BrowserState()
        every { settings.enableFxSuggest } returns true
        every { settings.showNonSponsoredSuggestions } returns true
        every { settings.isSearchOptimizationEnabled } returns true
        every { settings.shouldShowSearchOptimizationCards } returns true
        every { settings.shouldShowSearchOptimizationSportCard } returns false

        val initialState =
            createInitialSearchFragmentState(
                activity,
                components,
                tabId = null,
                pastedText = "pastedText",
                searchAccessPoint = MetricsUtils.Source.ACTION,
                isAndroidAutomotiveAvailable = false,
            )
        assertFalse(initialState.showSportsSuggestions)
    }

    @Test
    fun `GIVEN flight cards is disabled WHEN the initial state is created THEN the flight card is not displayed`() {
        every { components.core.store.state } returns BrowserState()
        every { settings.enableFxSuggest } returns true
        every { settings.showNonSponsoredSuggestions } returns true
        every { settings.isSearchOptimizationEnabled } returns true
        every { settings.shouldShowSearchOptimizationCards } returns true
        every { settings.shouldShowSearchOptimizationFlightCard } returns false

        val initialState =
            createInitialSearchFragmentState(
                activity,
                components,
                tabId = null,
                pastedText = "pastedText",
                searchAccessPoint = MetricsUtils.Source.ACTION,
                isAndroidAutomotiveAvailable = false,
            )
        assertFalse(initialState.showFlightsSuggestions)
    }

    @Test
    fun updateQuery() = runTest {
        val initialState = emptyDefaultState()
        val store = SearchFragmentStore(initialState)
        val query = "test query"

        store.dispatch(SearchFragmentAction.UpdateQuery(query))
        assertNotSame(initialState, store.state)
        assertEquals(query, store.state.query)
    }

    @Test
    fun `GIVEN search shortcuts are disabled in settings WHEN the default search engine is selected THEN search shortcuts are not displayed`() =
        runTest {
            val initialState = emptyDefaultState(showHistorySuggestionsForCurrentEngine = false)
            val store = SearchFragmentStore(initialState)

            store.dispatch(
                SearchFragmentAction.SearchDefaultEngineSelected(
                    engine = searchEngine,
                    browsingMode = BrowsingMode.Normal,
                    settings = settings,
                )
            )

            assertNotSame(initialState, store.state)
        }

    @Test
    fun `GIVEN search shortcuts are enabled in settings WHEN the default search engine is selected THEN search shortcuts are not displayed`() =
        runTest {
            val initialState = emptyDefaultState(showHistorySuggestionsForCurrentEngine = false)
            val store = SearchFragmentStore(initialState)

            store.dispatch(
                SearchFragmentAction.SearchDefaultEngineSelected(
                    engine = searchEngine,
                    browsingMode = BrowsingMode.Normal,
                    settings = settings,
                )
            )

            assertNotSame(initialState, store.state)
        }

    @Test
    fun `WHEN the default search engine is selected THEN search shortcuts are not displayed`() = runTest {
        val initialState = emptyDefaultState(showHistorySuggestionsForCurrentEngine = false)
        val store = SearchFragmentStore(initialState)

        store.dispatch(
            SearchFragmentAction.SearchDefaultEngineSelected(
                engine = searchEngine,
                browsingMode = BrowsingMode.Normal,
                settings = settings,
            )
        )

        assertNotSame(initialState, store.state)
    }

    // non default tests

    @Test
    fun `GIVEN search shortcuts are disabled in settings WHEN the search engine shortcut is selected THEN search shortcuts are not displayed`() =
        runTest {
            val initialState = emptyDefaultState(showHistorySuggestionsForCurrentEngine = false)
            val store = SearchFragmentStore(initialState)

            val newEngine: SearchEngine = mockk {
                every { id } returns "DuckDuckGo"
                every { isGeneral } returns true
                every { trendingUrl } returns null
            }

            store.dispatch(
                SearchFragmentAction.SearchShortcutEngineSelected(
                    engine = newEngine,
                    browsingMode = BrowsingMode.Normal,
                    settings = settings,
                )
            )

            assertNotSame(initialState, store.state)
        }

    @Test
    fun `GIVEN search shortcuts are enabled in settings WHEN the search engine shortcut is selected THEN search shortcuts are not displayed`() =
        runTest {
            val initialState = emptyDefaultState(showHistorySuggestionsForCurrentEngine = false)
            val store = SearchFragmentStore(initialState)

            val newEngine: SearchEngine = mockk {
                every { id } returns "DuckDuckGo"
                every { isGeneral } returns true
                every { trendingUrl } returns null
            }

            store.dispatch(
                SearchFragmentAction.SearchDefaultEngineSelected(
                    engine = newEngine,
                    browsingMode = BrowsingMode.Normal,
                    settings = settings,
                )
            )

            assertNotSame(initialState, store.state)
        }

    @Test
    fun `GIVEN the search engine shortcut is selected THEN search shortcuts are not displayed`() = runTest {
        val initialState = emptyDefaultState(showHistorySuggestionsForCurrentEngine = false)
        val store = SearchFragmentStore(initialState)

        val newEngine: SearchEngine = mockk {
            every { id } returns "DuckDuckGo"
            every { isGeneral } returns true
            every { trendingUrl } returns null
        }

        store.dispatch(
            SearchFragmentAction.SearchDefaultEngineSelected(
                engine = newEngine,
                browsingMode = BrowsingMode.Normal,
                settings = settings,
            )
        )

        assertNotSame(initialState, store.state)
    }

    @Test
    fun `GIVEN sponsored suggestions are enabled WHEN the default search engine is selected THEN sponsored suggestions are displayed`() =
        runTest {
            val initialState = emptyDefaultState(showSponsoredSuggestions = false, showNonSponsoredSuggestions = false)
            val store = SearchFragmentStore(initialState)
            every { settings.enableFxSuggest } returns true
            every { settings.showSponsoredSuggestions } returns true
            every { settings.showNonSponsoredSuggestions } returns false

            store.dispatch(
                SearchFragmentAction.SearchDefaultEngineSelected(
                    engine = searchEngine,
                    browsingMode = BrowsingMode.Normal,
                    settings = settings,
                )
            )

            assertNotSame(initialState, store.state)
            assertTrue(store.state.showSponsoredSuggestions)
            assertFalse(store.state.showNonSponsoredSuggestions)
        }

    @Test
    fun `GIVEN non-sponsored suggestions are enabled WHEN the default search engine is selected THEN non-sponsored suggestions are displayed`() =
        runTest {
            val initialState = emptyDefaultState(showSponsoredSuggestions = false, showNonSponsoredSuggestions = false)
            val store = SearchFragmentStore(initialState)
            every { settings.enableFxSuggest } returns true
            every { settings.showSponsoredSuggestions } returns false
            every { settings.showNonSponsoredSuggestions } returns true

            store.dispatch(
                SearchFragmentAction.SearchDefaultEngineSelected(
                    engine = searchEngine,
                    browsingMode = BrowsingMode.Normal,
                    settings = settings,
                )
            )

            assertNotSame(initialState, store.state)
            assertFalse(store.state.showSponsoredSuggestions)
            assertTrue(store.state.showNonSponsoredSuggestions)
        }

    @Test
    fun `GIVEN sponsored and non-sponsored suggestions are enabled and Firefox Suggest is enabled WHEN the default search engine is selected THEN both are displayed`() =
        runTest {
            val initialState = emptyDefaultState(showSponsoredSuggestions = false, showNonSponsoredSuggestions = false)
            val store = SearchFragmentStore(initialState)
            every { settings.enableFxSuggest } returns true
            every { settings.showSponsoredSuggestions } returns true
            every { settings.showNonSponsoredSuggestions } returns true

            store.dispatch(
                SearchFragmentAction.SearchDefaultEngineSelected(
                    engine = searchEngine,
                    browsingMode = BrowsingMode.Normal,
                    settings = settings,
                )
            )

            assertNotSame(initialState, store.state)
            assertTrue(store.state.showSponsoredSuggestions)
            assertTrue(store.state.showNonSponsoredSuggestions)
        }

    @Test
    fun `GIVEN sponsored and non-sponsored suggestions are enabled and Firefox Suggest is disabled WHEN the default search engine is selected THEN neither are displayed`() =
        runTest {
            val initialState = emptyDefaultState(showSponsoredSuggestions = true, showNonSponsoredSuggestions = true)
            val store = SearchFragmentStore(initialState)
            every { settings.enableFxSuggest } returns false
            every { settings.showSponsoredSuggestions } returns true
            every { settings.showNonSponsoredSuggestions } returns true

            store.dispatch(
                SearchFragmentAction.SearchDefaultEngineSelected(
                    engine = searchEngine,
                    browsingMode = BrowsingMode.Normal,
                    settings = settings,
                )
            )

            assertNotSame(initialState, store.state)
            assertFalse(store.state.showSponsoredSuggestions)
            assertFalse(store.state.showNonSponsoredSuggestions)
        }

    @Test
    fun `GIVEN sponsored and non-sponsored suggestions are disabled WHEN the default search engine is selected THEN neither are displayed`() =
        runTest {
            val initialState = emptyDefaultState(showSponsoredSuggestions = true, showNonSponsoredSuggestions = true)
            val store = SearchFragmentStore(initialState)
            every { settings.enableFxSuggest } returns true
            every { settings.showSponsoredSuggestions } returns false
            every { settings.showNonSponsoredSuggestions } returns false

            store.dispatch(
                SearchFragmentAction.SearchDefaultEngineSelected(
                    engine = searchEngine,
                    browsingMode = BrowsingMode.Normal,
                    settings = settings,
                )
            )

            assertNotSame(initialState, store.state)
            assertFalse(store.state.showSponsoredSuggestions)
            assertFalse(store.state.showNonSponsoredSuggestions)
        }

    @Test
    fun `GIVEN sponsored and non-sponsored suggestions are enabled WHEN a shortcut is selected THEN neither are displayed`() =
        runTest {
            val initialState = emptyDefaultState(showSponsoredSuggestions = true, showNonSponsoredSuggestions = true)
            val store = SearchFragmentStore(initialState)
            every { settings.enableFxSuggest } returns true
            every { settings.showSponsoredSuggestions } returns true
            every { settings.showNonSponsoredSuggestions } returns true
            every { searchEngine.isGeneral } returns true

            store.dispatch(
                SearchFragmentAction.SearchShortcutEngineSelected(
                    engine = searchEngine,
                    browsingMode = BrowsingMode.Normal,
                    settings = settings,
                )
            )

            assertNotSame(initialState, store.state)
            assertFalse(store.state.showSponsoredSuggestions)
            assertFalse(store.state.showNonSponsoredSuggestions)
        }

    @Test
    fun `GIVEN sponsored and non-sponsored suggestions are enabled WHEN the history engine is selected THEN neither are displayed`() =
        runTest {
            val initialState = emptyDefaultState(showSponsoredSuggestions = true, showNonSponsoredSuggestions = true)
            val store = SearchFragmentStore(initialState)
            every { settings.enableFxSuggest } returns true
            every { settings.showSponsoredSuggestions } returns true
            every { settings.showNonSponsoredSuggestions } returns true

            store.dispatch(SearchFragmentAction.SearchHistoryEngineSelected(searchEngine))

            assertNotSame(initialState, store.state)
            assertFalse(store.state.showSponsoredSuggestions)
            assertFalse(store.state.showNonSponsoredSuggestions)
        }

    @Test
    fun `GIVEN sponsored and non-sponsored suggestions are enabled WHEN the bookmarks engine is selected THEN neither are displayed`() =
        runTest {
            val initialState = emptyDefaultState(showSponsoredSuggestions = true, showNonSponsoredSuggestions = true)
            val store = SearchFragmentStore(initialState)
            every { settings.enableFxSuggest } returns true
            every { settings.showSponsoredSuggestions } returns true
            every { settings.showNonSponsoredSuggestions } returns true

            store.dispatch(SearchFragmentAction.SearchBookmarksEngineSelected(searchEngine))

            assertNotSame(initialState, store.state)
            assertFalse(store.state.showSponsoredSuggestions)
            assertFalse(store.state.showNonSponsoredSuggestions)
        }

    @Test
    fun `GIVEN sponsored and non-sponsored suggestions are enabled WHEN the tabs engine is selected THEN neither are displayed`() =
        runTest {
            val initialState = emptyDefaultState(showSponsoredSuggestions = true, showNonSponsoredSuggestions = true)
            val store = SearchFragmentStore(initialState)
            every { settings.enableFxSuggest } returns true
            every { settings.showSponsoredSuggestions } returns true
            every { settings.showNonSponsoredSuggestions } returns true

            store.dispatch(SearchFragmentAction.SearchTabsEngineSelected(searchEngine))

            assertNotSame(initialState, store.state)
            assertFalse(store.state.showSponsoredSuggestions)
            assertFalse(store.state.showNonSponsoredSuggestions)
        }

    @Test
    fun `GIVEN non-sponsored suggestion is disabled WHEN the default search engine is selected THEN all suggestion cards are not displayed`() =
        runTest {
            val initialState = emptyDefaultState(showStocksSuggestions = true)
            val store = SearchFragmentStore(initialState)
            every { settings.enableFxSuggest } returns true
            every { settings.showNonSponsoredSuggestions } returns false
            every { settings.isSearchOptimizationEnabled } returns true
            every { settings.shouldShowSearchOptimizationCards } returns true
            every { settings.shouldShowSearchOptimizationStockCard } returns true
            every { settings.shouldShowSearchOptimizationSportCard } returns true
            every { settings.shouldShowSearchOptimizationFlightCard } returns true

            store.dispatch(
                SearchFragmentAction.SearchDefaultEngineSelected(
                    engine = searchEngine,
                    browsingMode = BrowsingMode.Normal,
                    settings = settings,
                )
            )

            assertNotSame(initialState, store.state)
            assertFalse(store.state.showStocksSuggestions)
            assertFalse(store.state.showSportsSuggestions)
            assertFalse(store.state.showFlightsSuggestions)
        }

    @Test
    fun `GIVEN search optimization is disabled WHEN the default search engine is selected THEN all suggestion cards are not displayed`() =
        runTest {
            val initialState = emptyDefaultState(showStocksSuggestions = true)
            val store = SearchFragmentStore(initialState)
            every { settings.enableFxSuggest } returns true
            every { settings.showNonSponsoredSuggestions } returns true
            every { settings.isSearchOptimizationEnabled } returns false
            every { settings.shouldShowSearchOptimizationCards } returns true
            every { settings.shouldShowSearchOptimizationStockCard } returns true
            every { settings.shouldShowSearchOptimizationSportCard } returns true
            every { settings.shouldShowSearchOptimizationFlightCard } returns true

            store.dispatch(
                SearchFragmentAction.SearchDefaultEngineSelected(
                    engine = searchEngine,
                    browsingMode = BrowsingMode.Normal,
                    settings = settings,
                )
            )

            assertNotSame(initialState, store.state)
            assertFalse(store.state.showStocksSuggestions)
            assertFalse(store.state.showSportsSuggestions)
            assertFalse(store.state.showFlightsSuggestions)
        }

    @Test
    fun `GIVEN suggestion cards is disabled WHEN the default search engine is selected THEN all suggestion cards are not displayed`() =
        runTest {
            val initialState = emptyDefaultState(showStocksSuggestions = true)
            val store = SearchFragmentStore(initialState)
            every { settings.enableFxSuggest } returns true
            every { settings.showNonSponsoredSuggestions } returns true
            every { settings.isSearchOptimizationEnabled } returns true
            every { settings.shouldShowSearchOptimizationCards } returns false
            every { settings.shouldShowSearchOptimizationStockCard } returns true
            every { settings.shouldShowSearchOptimizationSportCard } returns true
            every { settings.shouldShowSearchOptimizationFlightCard } returns true

            store.dispatch(
                SearchFragmentAction.SearchDefaultEngineSelected(
                    engine = searchEngine,
                    browsingMode = BrowsingMode.Normal,
                    settings = settings,
                )
            )

            assertNotSame(initialState, store.state)
            assertFalse(store.state.showStocksSuggestions)
            assertFalse(store.state.showSportsSuggestions)
            assertFalse(store.state.showFlightsSuggestions)
        }

    @Test
    fun `GIVEN stock cards is disabled WHEN the default search engine is selected THEN stock suggestions are not displayed`() =
        runTest {
            val initialState = emptyDefaultState(showStocksSuggestions = true)
            val store = SearchFragmentStore(initialState)
            every { settings.enableFxSuggest } returns true
            every { settings.showNonSponsoredSuggestions } returns true
            every { settings.isSearchOptimizationEnabled } returns true
            every { settings.shouldShowSearchOptimizationCards } returns true
            every { settings.shouldShowSearchOptimizationStockCard } returns false

            store.dispatch(
                SearchFragmentAction.SearchDefaultEngineSelected(
                    engine = searchEngine,
                    browsingMode = BrowsingMode.Normal,
                    settings = settings,
                )
            )

            assertNotSame(initialState, store.state)
            assertFalse(store.state.showStocksSuggestions)
        }

    @Test
    fun `GIVEN sport cards is disabled WHEN the default search engine is selected THEN sport suggestions are not displayed`() =
        runTest {
            val initialState = emptyDefaultState(showStocksSuggestions = true)
            val store = SearchFragmentStore(initialState)
            every { settings.enableFxSuggest } returns true
            every { settings.showNonSponsoredSuggestions } returns true
            every { settings.isSearchOptimizationEnabled } returns true
            every { settings.shouldShowSearchOptimizationCards } returns true
            every { settings.shouldShowSearchOptimizationSportCard } returns false

            store.dispatch(
                SearchFragmentAction.SearchDefaultEngineSelected(
                    engine = searchEngine,
                    browsingMode = BrowsingMode.Normal,
                    settings = settings,
                )
            )

            assertNotSame(initialState, store.state)
            assertFalse(store.state.showSportsSuggestions)
        }

    @Test
    fun `GIVEN flight cards is disabled WHEN the default search engine is selected THEN flight suggestions are not displayed`() =
        runTest {
            val initialState = emptyDefaultState(showStocksSuggestions = true)
            val store = SearchFragmentStore(initialState)
            every { settings.enableFxSuggest } returns true
            every { settings.showNonSponsoredSuggestions } returns true
            every { settings.isSearchOptimizationEnabled } returns true
            every { settings.shouldShowSearchOptimizationCards } returns true
            every { settings.shouldShowSearchOptimizationFlightCard } returns false

            store.dispatch(
                SearchFragmentAction.SearchDefaultEngineSelected(
                    engine = searchEngine,
                    browsingMode = BrowsingMode.Normal,
                    settings = settings,
                )
            )

            assertNotSame(initialState, store.state)
            assertFalse(store.state.showFlightsSuggestions)
        }

    @Test
    fun `GIVEN private browsing mode WHEN the search engine is the default one THEN search suggestions providers are updated`() =
        runTest {
            val initialState = emptyDefaultState(showHistorySuggestionsForCurrentEngine = false)
            val store = SearchFragmentStore(initialState)

            every { settings.shouldShowSearchSuggestions } returns true
            every { settings.shouldShowClipboardSuggestions } returns true
            every { settings.shouldShowHistorySuggestions } returns true
            every { settings.shouldShowBookmarkSuggestions } returns false
            every { settings.shouldShowSyncedTabsSuggestions } returns false
            every { settings.shouldShowSearchSuggestions } returns true
            every { settings.shouldShowSearchSuggestionsInPrivate } returns true
            every { settings.enableFxSuggest } returns true
            every { settings.showSponsoredSuggestions } returns true
            every { settings.showNonSponsoredSuggestions } returns true
            every { settings.isSearchOptimizationEnabled } returns true
            every { settings.shouldShowSearchOptimizationCards } returns true
            every { settings.shouldShowSearchOptimizationStockCard } returns true
            every { settings.shouldShowSearchOptimizationSportCard } returns true
            every { settings.shouldShowSearchOptimizationFlightCard } returns true

            store.dispatch(
                SearchFragmentAction.SearchDefaultEngineSelected(
                    engine = searchEngine,
                    browsingMode = BrowsingMode.Private,
                    settings = settings,
                )
            )

            assertNotSame(initialState, store.state)
            assertEquals(SearchEngineSource.Default(searchEngine), store.state.searchEngineSource)

            assertTrue(store.state.showSearchSuggestionsFromCurrentEngine)
            assertTrue(store.state.showClipboardSuggestions)
            assertTrue(store.state.showSearchTermHistory)
            assertFalse(store.state.showHistorySuggestionsForCurrentEngine)
            assertTrue(store.state.showAllHistorySuggestions)
            assertFalse(store.state.showAllBookmarkSuggestions)
            assertFalse(store.state.showAllSyncedTabsSuggestions)
            assertTrue(store.state.showAllSessionSuggestions)
            assertFalse(store.state.showSponsoredSuggestions)
            assertFalse(store.state.showNonSponsoredSuggestions)
            assertTrue(store.state.showStocksSuggestions)
            assertTrue(store.state.showSportsSuggestions)
            assertTrue(store.state.showFlightsSuggestions)
            verify { shouldShowSearchSuggestions(BrowsingMode.Private, settings) }
        }

    @Test
    fun `GIVEN normal browsing mode WHEN the search engine is the default one THEN search suggestions providers are updated`() =
        runTest {
            val initialState = emptyDefaultState(showHistorySuggestionsForCurrentEngine = false)
            val store = SearchFragmentStore(initialState)

            every { settings.shouldShowSearchSuggestions } returns true
            every { settings.shouldShowClipboardSuggestions } returns true
            every { settings.shouldShowHistorySuggestions } returns true
            every { settings.shouldShowBookmarkSuggestions } returns false
            every { settings.shouldShowSyncedTabsSuggestions } returns false
            every { settings.shouldShowSearchSuggestions } returns true
            every { settings.shouldShowSearchSuggestionsInPrivate } returns true
            every { settings.enableFxSuggest } returns true
            every { settings.showSponsoredSuggestions } returns true
            every { settings.showNonSponsoredSuggestions } returns true
            every { settings.isSearchOptimizationEnabled } returns true
            every { settings.shouldShowSearchOptimizationCards } returns true
            every { settings.shouldShowSearchOptimizationStockCard } returns true
            every { settings.shouldShowSearchOptimizationSportCard } returns true
            every { settings.shouldShowSearchOptimizationFlightCard } returns true

            store.dispatch(
                SearchFragmentAction.SearchDefaultEngineSelected(
                    engine = searchEngine,
                    browsingMode = BrowsingMode.Normal,
                    settings = settings,
                )
            )

            assertNotSame(initialState, store.state)
            assertEquals(SearchEngineSource.Default(searchEngine), store.state.searchEngineSource)

            assertTrue(store.state.showSearchSuggestionsFromCurrentEngine)
            assertTrue(store.state.showClipboardSuggestions)
            assertTrue(store.state.showSearchTermHistory)
            assertFalse(store.state.showHistorySuggestionsForCurrentEngine)
            assertTrue(store.state.showAllHistorySuggestions)
            assertFalse(store.state.showAllBookmarkSuggestions)
            assertFalse(store.state.showAllSyncedTabsSuggestions)
            assertTrue(store.state.showAllSessionSuggestions)
            assertTrue(store.state.showSponsoredSuggestions)
            assertTrue(store.state.showNonSponsoredSuggestions)
            assertTrue(store.state.showStocksSuggestions)
            assertTrue(store.state.showSportsSuggestions)
            assertTrue(store.state.showFlightsSuggestions)
        }

    @Test
    fun `GIVEN the search engine is updated to a general engine shortcut THEN search suggestions providers are updated`() =
        runTest {
            val initialState = emptyDefaultState(showHistorySuggestionsForCurrentEngine = false)
            val store = SearchFragmentStore(initialState)
            val topicSpecificEngine: SearchEngine = mockk {
                every { isGeneral } returns false
                every { trendingUrl } returns null
            }

            every { settings.shouldShowClipboardSuggestions } returns true
            every { settings.shouldShowHistorySuggestions } returns true
            every { settings.shouldShowBookmarkSuggestions } returns true
            every { settings.shouldShowSyncedTabsSuggestions } returns true
            every { settings.shouldShowSearchSuggestions } returns true
            every { settings.enableFxSuggest } returns true
            every { settings.showSponsoredSuggestions } returns true
            every { settings.showNonSponsoredSuggestions } returns true
            every { settings.isSearchOptimizationEnabled } returns true
            every { settings.shouldShowSearchOptimizationCards } returns true
            every { settings.shouldShowSearchOptimizationStockCard } returns true
            every { settings.shouldShowSearchOptimizationSportCard } returns true
            every { settings.shouldShowSearchOptimizationFlightCard } returns true

            store.dispatch(
                SearchFragmentAction.SearchShortcutEngineSelected(
                    engine = topicSpecificEngine,
                    browsingMode = BrowsingMode.Normal,
                    settings = settings,
                )
            )

            assertNotSame(initialState, store.state)
            assertEquals(SearchEngineSource.Shortcut(topicSpecificEngine), store.state.searchEngineSource)
            assertTrue(store.state.showSearchSuggestionsFromCurrentEngine)
            assertTrue(store.state.showClipboardSuggestions)
            assertTrue(store.state.showSearchTermHistory)
            assertTrue(store.state.showHistorySuggestionsForCurrentEngine)
            assertFalse(store.state.showAllHistorySuggestions)
            assertTrue(store.state.showBookmarksSuggestionsForCurrentEngine)
            assertFalse(store.state.showAllBookmarkSuggestions)
            assertTrue(store.state.showSyncedTabsSuggestionsForCurrentEngine)
            assertFalse(store.state.showAllSyncedTabsSuggestions)
            assertTrue(store.state.showSessionSuggestionsForCurrentEngine)
            assertFalse(store.state.showAllSessionSuggestions)
            assertFalse(store.state.showSponsoredSuggestions)
            assertFalse(store.state.showNonSponsoredSuggestions)
            assertFalse(store.state.showStocksSuggestions)
            assertFalse(store.state.showSportsSuggestions)
            assertFalse(store.state.showFlightsSuggestions)

            every { settings.shouldShowSearchSuggestions } returns false
            val generalEngine: SearchEngine = mockk {
                every { isGeneral } returns true
                every { trendingUrl } returns null
            }
            store.dispatch(
                SearchFragmentAction.SearchShortcutEngineSelected(
                    engine = generalEngine,
                    browsingMode = BrowsingMode.Normal,
                    settings = settings,
                )
            )
            assertNotSame(initialState, store.state)
            assertEquals(SearchEngineSource.Shortcut(generalEngine), store.state.searchEngineSource)
            assertFalse(store.state.showSearchSuggestionsFromCurrentEngine)
            assertTrue(store.state.showClipboardSuggestions)
            assertTrue(store.state.showSearchTermHistory)
            assertFalse(store.state.showHistorySuggestionsForCurrentEngine)
            assertFalse(store.state.showAllHistorySuggestions)
            assertFalse(store.state.showBookmarksSuggestionsForCurrentEngine)
            assertFalse(store.state.showAllBookmarkSuggestions)
            assertFalse(store.state.showSyncedTabsSuggestionsForCurrentEngine)
            assertFalse(store.state.showAllSyncedTabsSuggestions)
            assertFalse(store.state.showSessionSuggestionsForCurrentEngine)
            assertFalse(store.state.showAllSessionSuggestions)
            assertFalse(store.state.showSponsoredSuggestions)
            assertFalse(store.state.showNonSponsoredSuggestions)
            assertFalse(store.state.showStocksSuggestions)
            assertFalse(store.state.showSportsSuggestions)
            assertFalse(store.state.showFlightsSuggestions)
        }

    @Test
    fun `GIVEN the search engine is updated to a topic specific engine shortcut THEN search suggestions providers are updated`() =
        runTest {
            val initialState = emptyDefaultState(showHistorySuggestionsForCurrentEngine = false)
            val store = SearchFragmentStore(initialState)
            every { searchEngine.isGeneral } returns false
            every { searchEngine.trendingUrl } returns "https://mozilla.org"
            every { settings.shouldShowSearchSuggestions } returns false
            every { settings.shouldShowClipboardSuggestions } returns false
            every { settings.shouldShowHistorySuggestions } returns true
            every { settings.shouldShowBookmarkSuggestions } returns false
            every { settings.shouldShowSyncedTabsSuggestions } returns false
            every { settings.enableFxSuggest } returns true
            every { settings.showSponsoredSuggestions } returns true
            every { settings.showNonSponsoredSuggestions } returns true
            every { settings.trendingSearchSuggestionsEnabled } returns true
            every { settings.shouldShowRecentSearchSuggestions } returns true
            every { settings.isSearchOptimizationEnabled } returns true
            every { settings.shouldShowSearchOptimizationCards } returns true
            every { settings.shouldShowSearchOptimizationStockCard } returns true
            every { settings.shouldShowSearchOptimizationSportCard } returns true
            every { settings.shouldShowSearchOptimizationFlightCard } returns true

            store.dispatch(
                SearchFragmentAction.SearchShortcutEngineSelected(
                    engine = searchEngine,
                    browsingMode = BrowsingMode.Normal,
                    settings = settings,
                )
            )

            assertNotSame(initialState, store.state)
            assertEquals(SearchEngineSource.Shortcut(searchEngine), store.state.searchEngineSource)
            assertFalse(store.state.showSearchSuggestionsFromCurrentEngine)
            assertFalse(store.state.showClipboardSuggestions)
            assertTrue(store.state.showSearchTermHistory)
            assertTrue(store.state.showHistorySuggestionsForCurrentEngine)
            assertFalse(store.state.showAllHistorySuggestions)
            assertFalse(store.state.showBookmarksSuggestionsForCurrentEngine)
            assertFalse(store.state.showAllBookmarkSuggestions)
            assertFalse(store.state.showSyncedTabsSuggestionsForCurrentEngine)
            assertFalse(store.state.showAllSyncedTabsSuggestions)
            assertTrue(store.state.showSessionSuggestionsForCurrentEngine)
            assertFalse(store.state.showAllSessionSuggestions)
            assertFalse(store.state.showSponsoredSuggestions)
            assertFalse(store.state.showNonSponsoredSuggestions)
            assertFalse(store.state.showStocksSuggestions)
            assertFalse(store.state.showSportsSuggestions)
            assertFalse(store.state.showFlightsSuggestions)
            assertFalse(store.state.showTrendingSearches)
            assertTrue(store.state.showRecentSearches)
        }

    @Test
    fun `GIVEN updating the search engine to a topic specific one THEN enable filtered bookmarks, history and tabs suggestions`() =
        runTest {
            val initialState = emptyDefaultState()
            val store = SearchFragmentStore(initialState)
            val topicSpecificEngine1: SearchEngine =
                mockk(relaxed = true) {
                    every { name } returns "1"
                    every { isGeneral } returns false
                }

            every { settings.shouldShowBookmarkSuggestions } returns false
            every { settings.shouldShowSyncedTabsSuggestions } returns false
            store.dispatch(
                SearchFragmentAction.SearchShortcutEngineSelected(
                    engine = topicSpecificEngine1,
                    browsingMode = BrowsingMode.Private,
                    settings = settings,
                )
            )
            assertNotSame(initialState, store.state)
            assertEquals(SearchEngineSource.Shortcut(topicSpecificEngine1), store.state.searchEngineSource)
            assertFalse(store.state.showBookmarksSuggestionsForCurrentEngine)
            assertFalse(store.state.showAllBookmarkSuggestions)
            assertFalse(store.state.showSyncedTabsSuggestionsForCurrentEngine)
            assertFalse(store.state.showAllSyncedTabsSuggestions)
            assertTrue(store.state.showSessionSuggestionsForCurrentEngine)
            assertFalse(store.state.showAllSessionSuggestions)

            val topicSpecificEngine2 = topicSpecificEngine1.copy(name = "2")
            every { settings.shouldShowBookmarkSuggestions } returns true
            every { settings.shouldShowSyncedTabsSuggestions } returns true
            store.dispatch(
                SearchFragmentAction.SearchShortcutEngineSelected(
                    engine = topicSpecificEngine2,
                    browsingMode = BrowsingMode.Private,
                    settings = settings,
                )
            )
            assertNotSame(initialState, store.state)
            assertEquals(SearchEngineSource.Shortcut(topicSpecificEngine2), store.state.searchEngineSource)
            assertTrue(store.state.showBookmarksSuggestionsForCurrentEngine)
            assertFalse(store.state.showAllBookmarkSuggestions)
            assertTrue(store.state.showSyncedTabsSuggestionsForCurrentEngine)
            assertFalse(store.state.showAllSyncedTabsSuggestions)
            assertTrue(store.state.showSessionSuggestionsForCurrentEngine)
            assertFalse(store.state.showAllSessionSuggestions)
        }

    @Test
    fun `GIVEN updating the search engine to a topic specific one THEN enable bookmarks and tabs suggestions if user enabled`() =
        runTest {
            val initialState = emptyDefaultState()
            val store = SearchFragmentStore(initialState)
            val topicSpecificEngine1: SearchEngine =
                mockk(relaxed = true) {
                    every { id } returns "1"
                    every { isGeneral } returns false
                }

            every { settings.shouldShowBookmarkSuggestions } returns false
            every { settings.shouldShowSyncedTabsSuggestions } returns true
            store.dispatch(
                SearchFragmentAction.SearchShortcutEngineSelected(
                    engine = topicSpecificEngine1,
                    browsingMode = BrowsingMode.Private,
                    settings = settings,
                )
            )
            assertNotSame(initialState, store.state)
            assertEquals(SearchEngineSource.Shortcut(topicSpecificEngine1), store.state.searchEngineSource)
            assertFalse(store.state.showAllBookmarkSuggestions)
            assertTrue(store.state.showSyncedTabsSuggestionsForCurrentEngine)
            assertFalse(store.state.showAllSyncedTabsSuggestions)
            assertTrue(store.state.showSessionSuggestionsForCurrentEngine)
            assertFalse(store.state.showAllSessionSuggestions)

            val topicSpecificEngine2 = topicSpecificEngine1.copy(id = "2")
            every { settings.shouldShowBookmarkSuggestions } returns true
            every { settings.shouldShowSyncedTabsSuggestions } returns false
            store.dispatch(
                SearchFragmentAction.SearchShortcutEngineSelected(
                    engine = topicSpecificEngine2,
                    browsingMode = BrowsingMode.Private,
                    settings = settings,
                )
            )
            assertNotSame(initialState, store.state)
            assertEquals(SearchEngineSource.Shortcut(topicSpecificEngine2), store.state.searchEngineSource)
            assertTrue(store.state.showBookmarksSuggestionsForCurrentEngine)
            assertFalse(store.state.showAllBookmarkSuggestions)
            assertFalse(store.state.showSyncedTabsSuggestionsForCurrentEngine)
            assertFalse(store.state.showAllSyncedTabsSuggestions)
            assertTrue(store.state.showSessionSuggestionsForCurrentEngine)
            assertFalse(store.state.showAllSessionSuggestions)
        }

    @Test
    fun `WHEN doing a history search THEN search suggestions providers are updated`() = runTest {
        val initialState = emptyDefaultState(showHistorySuggestionsForCurrentEngine = true)
        val store = SearchFragmentStore(initialState)

        store.dispatch(SearchFragmentAction.SearchHistoryEngineSelected(searchEngine))

        assertNotSame(initialState, store.state)
        assertEquals(SearchEngineSource.History(searchEngine), store.state.searchEngineSource)
        assertFalse(store.state.showSearchSuggestionsFromCurrentEngine)
        assertFalse(store.state.showClipboardSuggestions)
        assertFalse(store.state.showSearchTermHistory)
        assertFalse(store.state.showHistorySuggestionsForCurrentEngine)
        assertTrue(store.state.showAllHistorySuggestions)
        assertFalse(store.state.showAllBookmarkSuggestions)
        assertFalse(store.state.showAllSyncedTabsSuggestions)
        assertFalse(store.state.showAllSessionSuggestions)
        assertFalse(store.state.showSponsoredSuggestions)
        assertFalse(store.state.showNonSponsoredSuggestions)
        assertFalse(store.state.showStocksSuggestions)
        assertFalse(store.state.showSportsSuggestions)
        assertFalse(store.state.showFlightsSuggestions)
    }

    @Test
    fun `WHEN doing a bookmarks search THEN search suggestions providers are updated`() = runTest {
        val initialState = emptyDefaultState(showHistorySuggestionsForCurrentEngine = true)
        val store = SearchFragmentStore(initialState)

        store.dispatch(SearchFragmentAction.SearchBookmarksEngineSelected(searchEngine))

        assertNotSame(initialState, store.state)
        assertEquals(SearchEngineSource.Bookmarks(searchEngine), store.state.searchEngineSource)
        assertFalse(store.state.showSearchSuggestionsFromCurrentEngine)
        assertFalse(store.state.showClipboardSuggestions)
        assertFalse(store.state.showSearchTermHistory)
        assertFalse(store.state.showHistorySuggestionsForCurrentEngine)
        assertFalse(store.state.showAllHistorySuggestions)
        assertTrue(store.state.showAllBookmarkSuggestions)
        assertFalse(store.state.showAllSyncedTabsSuggestions)
        assertFalse(store.state.showAllSessionSuggestions)
        assertFalse(store.state.showSponsoredSuggestions)
        assertFalse(store.state.showNonSponsoredSuggestions)
        assertFalse(store.state.showStocksSuggestions)
        assertFalse(store.state.showSportsSuggestions)
        assertFalse(store.state.showFlightsSuggestions)
    }

    @Test
    fun `WHEN doing a tabs search THEN search suggestions providers are updated`() = runTest {
        val initialState = emptyDefaultState(showHistorySuggestionsForCurrentEngine = true)
        val store = SearchFragmentStore(initialState)

        store.dispatch(SearchFragmentAction.SearchTabsEngineSelected(searchEngine))

        assertNotSame(initialState, store.state)
        assertEquals(SearchEngineSource.Tabs(searchEngine), store.state.searchEngineSource)
        assertFalse(store.state.showSearchSuggestionsFromCurrentEngine)
        assertFalse(store.state.showClipboardSuggestions)
        assertFalse(store.state.showSearchTermHistory)
        assertFalse(store.state.showHistorySuggestionsForCurrentEngine)
        assertFalse(store.state.showAllHistorySuggestions)
        assertFalse(store.state.showAllBookmarkSuggestions)
        assertTrue(store.state.showAllSyncedTabsSuggestions)
        assertTrue(store.state.showAllSessionSuggestions)
        assertFalse(store.state.showSponsoredSuggestions)
        assertFalse(store.state.showNonSponsoredSuggestions)
        assertFalse(store.state.showStocksSuggestions)
        assertFalse(store.state.showSportsSuggestions)
        assertFalse(store.state.showFlightsSuggestions)
    }

    @Test
    fun `WHEN tabs engine selected action dispatched THEN update search engine source`() = runTest {
        val initialState = emptyDefaultState()
        val store = SearchFragmentStore(initialState)

        store.dispatch(SearchFragmentAction.SearchTabsEngineSelected(searchEngine))
        assertNotSame(initialState, store.state)
        assertEquals(SearchEngineSource.Tabs(searchEngine), store.state.searchEngineSource)
    }

    @Test
    fun showSearchSuggestions() = runTest {
        val initialState = emptyDefaultState()
        val store = SearchFragmentStore(initialState)

        store.dispatch(SearchFragmentAction.SetShowSearchSuggestions(true))
        assertNotSame(initialState, store.state)
        assertTrue(store.state.showSearchSuggestionsFromCurrentEngine)

        store.dispatch(SearchFragmentAction.SetShowSearchSuggestions(false))
        assertFalse(store.state.showSearchSuggestionsFromCurrentEngine)
    }

    @Test
    fun allowSearchInPrivateMode() = runTest {
        val initialState = emptyDefaultState()
        val store = SearchFragmentStore(initialState)

        store.dispatch(SearchFragmentAction.AllowSearchSuggestionsInPrivateModePrompt(true))
        assertNotSame(initialState, store.state)
        assertTrue(store.state.showSearchSuggestionsHint)

        store.dispatch(SearchFragmentAction.AllowSearchSuggestionsInPrivateModePrompt(false))
        assertFalse(store.state.showSearchSuggestionsHint)
    }

    @Test
    fun updatingClipboardUrl() {
        val initialState = emptyDefaultState()
        val store = SearchFragmentStore(initialState)

        assertFalse(store.state.clipboardHasUrl)

        store.dispatch(SearchFragmentAction.UpdateClipboardHasUrl(true))

        assertTrue(store.state.clipboardHasUrl)
    }

    @Test
    fun `Updating SearchFragmentState from SearchState`() {
        val store =
            SearchFragmentStore(
                emptyDefaultState(
                    searchEngineSource = SearchEngineSource.None,
                    defaultEngine = null,
                )
            )

        assertNull(store.state.defaultEngine)
        assertEquals(SearchEngineSource.None, store.state.searchEngineSource)

        store.dispatch(
            SearchFragmentAction.UpdateSearchState(
                search =
                    SearchState(
                        region = RegionState("US", "US"),
                        regionSearchEngines =
                            listOf(
                                SearchEngine("engine-a", "Engine A", mockk(), type = SearchEngine.Type.BUNDLED),
                                SearchEngine("engine-b", "Engine B", mockk(), type = SearchEngine.Type.BUNDLED),
                                SearchEngine("engine-c", "Engine C", mockk(), type = SearchEngine.Type.BUNDLED),
                            ),
                        customSearchEngines =
                            listOf(
                                SearchEngine("engine-d", "Engine D", mockk(), type = SearchEngine.Type.CUSTOM),
                                SearchEngine("engine-e", "Engine E", mockk(), type = SearchEngine.Type.CUSTOM),
                            ),
                        additionalSearchEngines =
                            listOf(
                                SearchEngine(
                                    "engine-f",
                                    "Engine F",
                                    mockk(),
                                    type = SearchEngine.Type.BUNDLED_ADDITIONAL,
                                )
                            ),
                        additionalAvailableSearchEngines =
                            listOf(
                                SearchEngine(
                                    "engine-g",
                                    "Engine G",
                                    mockk(),
                                    type = SearchEngine.Type.BUNDLED_ADDITIONAL,
                                ),
                                SearchEngine(
                                    "engine-h",
                                    "Engine H",
                                    mockk(),
                                    type = SearchEngine.Type.BUNDLED_ADDITIONAL,
                                ),
                            ),
                        hiddenSearchEngines =
                            listOf(SearchEngine("engine-i", "Engine I", mockk(), type = SearchEngine.Type.BUNDLED)),
                        regionDefaultSearchEngineId = "engine-b",
                        userSelectedSearchEngineId = null,
                        userSelectedSearchEngineName = null,
                    )
            )
        )

        assertNotNull(store.state.defaultEngine)
        assertEquals("Engine B", store.state.defaultEngine!!.name)

        assertIs<SearchEngineSource.Default>(store.state.searchEngineSource)
        assertNotNull(store.state.searchEngineSource.searchEngine)
        assertEquals("Engine B", store.state.searchEngineSource.searchEngine!!.name)
    }

    @Test
    fun `Updating SearchFragmentState from SearchState - shortcuts disabled`() {
        val store =
            SearchFragmentStore(
                emptyDefaultState(
                    searchEngineSource = SearchEngineSource.None,
                    defaultEngine = null,
                )
            )

        assertNull(store.state.defaultEngine)
        assertEquals(SearchEngineSource.None, store.state.searchEngineSource)

        store.dispatch(
            SearchFragmentAction.UpdateSearchState(
                search =
                    SearchState(
                        region = RegionState("US", "US"),
                        regionSearchEngines =
                            listOf(
                                SearchEngine("engine-a", "Engine A", mockk(), type = SearchEngine.Type.BUNDLED),
                                SearchEngine("engine-b", "Engine B", mockk(), type = SearchEngine.Type.BUNDLED),
                                SearchEngine("engine-c", "Engine C", mockk(), type = SearchEngine.Type.BUNDLED),
                            ),
                        customSearchEngines =
                            listOf(
                                SearchEngine("engine-d", "Engine D", mockk(), type = SearchEngine.Type.CUSTOM),
                                SearchEngine("engine-e", "Engine E", mockk(), type = SearchEngine.Type.CUSTOM),
                            ),
                        additionalSearchEngines =
                            listOf(
                                SearchEngine(
                                    "engine-f",
                                    "Engine F",
                                    mockk(),
                                    type = SearchEngine.Type.BUNDLED_ADDITIONAL,
                                )
                            ),
                        additionalAvailableSearchEngines =
                            listOf(
                                SearchEngine(
                                    "engine-g",
                                    "Engine G",
                                    mockk(),
                                    type = SearchEngine.Type.BUNDLED_ADDITIONAL,
                                ),
                                SearchEngine(
                                    "engine-h",
                                    "Engine H",
                                    mockk(),
                                    type = SearchEngine.Type.BUNDLED_ADDITIONAL,
                                ),
                            ),
                        hiddenSearchEngines =
                            listOf(SearchEngine("engine-i", "Engine I", mockk(), type = SearchEngine.Type.BUNDLED)),
                        regionDefaultSearchEngineId = "engine-b",
                        userSelectedSearchEngineId = null,
                        userSelectedSearchEngineName = null,
                    )
            )
        )

        assertNotNull(store.state.defaultEngine)
        assertEquals("Engine B", store.state.defaultEngine!!.name)

        assertIs<SearchEngineSource.Default>(store.state.searchEngineSource)
        assertNotNull(store.state.searchEngineSource.searchEngine)
        assertEquals("Engine B", store.state.searchEngineSource.searchEngine!!.name)
    }

    @Test
    fun `WHEN updating the SearchFragmentState from SearchState THEN search shortcuts are disabled`() {
        val store =
            SearchFragmentStore(
                emptyDefaultState(
                    searchEngineSource = SearchEngineSource.None,
                    defaultEngine = null,
                )
            )

        store.dispatch(
            SearchFragmentAction.UpdateSearchState(
                search =
                    SearchState(
                        region = RegionState("US", "US"),
                        regionSearchEngines =
                            listOf(
                                SearchEngine("engine-a", "Engine A", mockk(), type = SearchEngine.Type.BUNDLED),
                                SearchEngine("engine-b", "Engine B", mockk(), type = SearchEngine.Type.BUNDLED),
                            ),
                        customSearchEngines = listOf(),
                        additionalSearchEngines = listOf(),
                        additionalAvailableSearchEngines = listOf(),
                        hiddenSearchEngines = listOf(),
                        regionDefaultSearchEngineId = "engine-b",
                        userSelectedSearchEngineId = null,
                        userSelectedSearchEngineName = null,
                    )
            )
        )
    }

    @Test
    fun `GIVEN private browsing mode WHEN updating SearchFragmentState from SearchState THEN use private default search engine`() {
        val store =
            SearchFragmentStore(
                emptyDefaultState(
                    searchEngineSource = SearchEngineSource.None,
                    defaultEngine = null,
                )
            )

        assertNull(store.state.defaultEngine)

        store.dispatch(
            SearchFragmentAction.UpdateSearchState(
                search =
                    SearchState(
                        region = RegionState("US", "US"),
                        regionSearchEngines =
                            listOf(
                                SearchEngine("engine-a", "Engine A", mockk(), type = SearchEngine.Type.BUNDLED),
                                SearchEngine("engine-b", "Engine B", mockk(), type = SearchEngine.Type.BUNDLED),
                            ),
                        regionDefaultSearchEngineId = "engine-b",
                        userSelectedSearchEngineId = null,
                        userSelectedSearchEngineName = null,
                        userSelectedPrivateSearchEngineId = "engine-a",
                        userSelectedPrivateSearchEngineName = null,
                    ),
                isPrivate = true,
            )
        )

        assertNotNull(store.state.defaultEngine)
        assertEquals("Engine A", store.state.defaultEngine!!.name)
        assertIs<SearchEngineSource.Default>(store.state.searchEngineSource)
        assertEquals("Engine A", store.state.searchEngineSource.searchEngine!!.name)
    }

    @Test
    fun `GIVEN normal browsing mode WHEN updating SearchFragmentState from SearchState with private engine set THEN use normal default search engine`() {
        val store =
            SearchFragmentStore(
                emptyDefaultState(
                    searchEngineSource = SearchEngineSource.None,
                    defaultEngine = null,
                )
            )

        assertNull(store.state.defaultEngine)

        store.dispatch(
            SearchFragmentAction.UpdateSearchState(
                search =
                    SearchState(
                        region = RegionState("US", "US"),
                        regionSearchEngines =
                            listOf(
                                SearchEngine("engine-a", "Engine A", mockk(), type = SearchEngine.Type.BUNDLED),
                                SearchEngine("engine-b", "Engine B", mockk(), type = SearchEngine.Type.BUNDLED),
                            ),
                        regionDefaultSearchEngineId = "engine-b",
                        userSelectedSearchEngineId = null,
                        userSelectedSearchEngineName = null,
                        userSelectedPrivateSearchEngineId = "engine-a",
                        userSelectedPrivateSearchEngineName = null,
                    ),
                isPrivate = false,
            )
        )

        assertNotNull(store.state.defaultEngine)
        assertEquals("Engine B", store.state.defaultEngine!!.name)
        assertIs<SearchEngineSource.Default>(store.state.searchEngineSource)
        assertEquals("Engine B", store.state.searchEngineSource.searchEngine!!.name)
    }

    @Test
    fun `GIVEN private mode with no private engine set WHEN updating SearchFragmentState THEN fall back to normal default`() {
        val store =
            SearchFragmentStore(
                emptyDefaultState(
                    searchEngineSource = SearchEngineSource.None,
                    defaultEngine = null,
                )
            )

        store.dispatch(
            SearchFragmentAction.UpdateSearchState(
                search =
                    SearchState(
                        region = RegionState("US", "US"),
                        regionSearchEngines =
                            listOf(
                                SearchEngine("engine-a", "Engine A", mockk(), type = SearchEngine.Type.BUNDLED),
                                SearchEngine("engine-b", "Engine B", mockk(), type = SearchEngine.Type.BUNDLED),
                            ),
                        regionDefaultSearchEngineId = "engine-b",
                        userSelectedSearchEngineId = null,
                        userSelectedSearchEngineName = null,
                        userSelectedPrivateSearchEngineId = null,
                        userSelectedPrivateSearchEngineName = null,
                    ),
                isPrivate = true,
            )
        )

        assertNotNull(store.state.defaultEngine)
        assertEquals("Engine B", store.state.defaultEngine!!.name)
    }

    @Test
    fun `GIVEN normal browsing mode and search suggestions enabled WHEN checking if search suggestions should be shown THEN return true`() {
        var settings: Settings = mockk {
            every { shouldShowSearchSuggestions } returns false
            every { shouldShowSearchSuggestionsInPrivate } returns false
        }
        assertFalse(shouldShowSearchSuggestions(BrowsingMode.Normal, settings))

        settings = mockk {
            every { shouldShowSearchSuggestions } returns true
            every { shouldShowSearchSuggestionsInPrivate } returns false
        }
        assertTrue(shouldShowSearchSuggestions(BrowsingMode.Normal, settings))
    }

    @Test
    fun `GIVEN private browsing mode and search suggestions enabled WHEN checking if search suggestions should be shown THEN return true`() {
        var settings: Settings = mockk {
            every { shouldShowSearchSuggestions } returns false
            every { shouldShowSearchSuggestionsInPrivate } returns false
        }
        assertFalse(shouldShowSearchSuggestions(BrowsingMode.Private, settings))

        settings = mockk {
            every { shouldShowSearchSuggestions } returns false
            every { shouldShowSearchSuggestionsInPrivate } returns true
        }
        assertFalse(shouldShowSearchSuggestions(BrowsingMode.Private, settings))

        settings = mockk {
            every { shouldShowSearchSuggestions } returns true
            every { shouldShowSearchSuggestionsInPrivate } returns true
        }
        assertTrue(shouldShowSearchSuggestions(BrowsingMode.Private, settings))
    }

    @Test
    fun `GIVEN trending searches is enabled and search engine supports it THEN should show trending searches`() {
        var settings: Settings = mockk {
            every { trendingSearchSuggestionsEnabled } returns true
            every { shouldShowSearchSuggestions } returns true
            every { shouldShowSearchSuggestionsInPrivate } returns true
        }

        assertTrue(shouldShowTrendingSearchSuggestions(BrowsingMode.Private, settings, true))
        assertTrue(shouldShowTrendingSearchSuggestions(BrowsingMode.Normal, settings, true))

        settings = mockk {
            every { trendingSearchSuggestionsEnabled } returns false
            every { shouldShowSearchSuggestions } returns true
            every { shouldShowSearchSuggestionsInPrivate } returns true
        }

        assertFalse(shouldShowTrendingSearchSuggestions(BrowsingMode.Private, settings, true))
        assertFalse(shouldShowTrendingSearchSuggestions(BrowsingMode.Normal, settings, true))

        settings = mockk {
            every { trendingSearchSuggestionsEnabled } returns true
            every { shouldShowSearchSuggestions } returns false
            every { shouldShowSearchSuggestionsInPrivate } returns true
        }

        assertFalse(shouldShowTrendingSearchSuggestions(BrowsingMode.Private, settings, true))
        assertFalse(shouldShowTrendingSearchSuggestions(BrowsingMode.Normal, settings, true))
    }

    @Test
    fun `GIVEN search engine does not supports trending search THEN should not show trending searches`() {
        val settings: Settings = mockk {
            every { trendingSearchSuggestionsEnabled } returns true
            every { shouldShowSearchSuggestions } returns true
            every { shouldShowSearchSuggestionsInPrivate } returns true
        }

        assertFalse(shouldShowTrendingSearchSuggestions(BrowsingMode.Private, settings, false))
        assertFalse(shouldShowTrendingSearchSuggestions(BrowsingMode.Normal, settings, false))
    }

    @Test
    fun `GIVEN is private tab THEN should show trending searches only if allowed`() {
        var settings: Settings = mockk {
            every { trendingSearchSuggestionsEnabled } returns true
            every { shouldShowSearchSuggestions } returns true
            every { shouldShowSearchSuggestionsInPrivate } returns false
        }

        assertFalse(shouldShowTrendingSearchSuggestions(BrowsingMode.Private, settings, true))
        assertTrue(shouldShowTrendingSearchSuggestions(BrowsingMode.Normal, settings, true))

        settings = mockk {
            every { trendingSearchSuggestionsEnabled } returns true
            every { shouldShowSearchSuggestions } returns true
            every { shouldShowSearchSuggestionsInPrivate } returns true
        }

        assertTrue(shouldShowTrendingSearchSuggestions(BrowsingMode.Private, settings, true))
        assertTrue(shouldShowTrendingSearchSuggestions(BrowsingMode.Normal, settings, true))
    }

    @Test
    fun `WHEN search providers are updated THEN persist this in state`() {
        val newSearchProviders = listOf(mockk<SuggestionProvider>())
        val store = SearchFragmentStore(emptyDefaultState())

        store.dispatch(SearchProvidersUpdated(newSearchProviders))

        assertEquals(newSearchProviders, store.state.searchSuggestionsProviders)
    }

    @Test
    fun `WHEN search is started THEN don't update any state`() {
        val selectedSearchEngine = mockk<SearchEngine>()
        val initialState = emptyDefaultState()
        val store = SearchFragmentStore(initialState)

        store.dispatch(
            SearchStarted(
                selectedSearchEngine,
                isUserSelected = false,
                inPrivateMode = true,
                searchStartedForCurrentUrl = false,
            )
        )

        assertEquals(initialState, store.state)
    }

    @Test
    fun `WHEN the search UX changes visibility THEN persist this in state`() {
        val store = SearchFragmentStore(emptyDefaultState())

        store.dispatch(SearchSuggestionsVisibilityUpdated(true))
        assertTrue(store.state.shouldShowSearchSuggestions)

        store.dispatch(SearchSuggestionsVisibilityUpdated(false))
        assertFalse(store.state.shouldShowSearchSuggestions)
    }

    private fun emptyDefaultState(
        searchEngineSource: SearchEngineSource = mockk(),
        defaultEngine: SearchEngine? = mockk(),
        showHistorySuggestionsForCurrentEngine: Boolean = true,
        showSponsoredSuggestions: Boolean = true,
        showNonSponsoredSuggestions: Boolean = true,
        showStocksSuggestions: Boolean = true,
        showSportsSuggestions: Boolean = true,
        showFlightsSuggestions: Boolean = true,
    ): SearchFragmentState =
        EMPTY_SEARCH_FRAGMENT_STATE.copy(
            searchEngineSource = searchEngineSource,
            defaultEngine = defaultEngine,
            showSearchTermHistory = true,
            showHistorySuggestionsForCurrentEngine = showHistorySuggestionsForCurrentEngine,
            showSponsoredSuggestions = showSponsoredSuggestions,
            showNonSponsoredSuggestions = showNonSponsoredSuggestions,
            showStocksSuggestions = showStocksSuggestions,
            showSportsSuggestions = showSportsSuggestions,
            showFlightsSuggestions = showFlightsSuggestions,
            showQrButton = true,
        )
}
