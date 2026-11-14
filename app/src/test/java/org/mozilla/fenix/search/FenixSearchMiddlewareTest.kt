/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.search

import android.os.Build
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import mozilla.components.browser.state.action.AwesomeBarAction
import mozilla.components.browser.state.action.AwesomeBarAction.EngagementFinished
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.search.RegionState
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.ReaderState
import mozilla.components.browser.state.state.SearchState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.state.selectedOrDefaultSearchEngine
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.compose.browser.awesomebar.internal.CurrentTabData
import mozilla.components.compose.browser.toolbar.store.BrowserEditToolbarAction
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarStore
import mozilla.components.compose.browser.toolbar.ui.BrowserToolbarQuery
import mozilla.components.concept.awesomebar.AwesomeBar.Suggestion
import mozilla.components.concept.awesomebar.AwesomeBar.SuggestionProvider
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession.LoadUrlFlags
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.support.ktx.util.URLStringUtils
import mozilla.components.support.test.middleware.CaptureActionsMiddleware
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.utils.ClipboardHandler
import mozilla.telemetry.glean.testing.GleanTestRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.experiments.nimbus.NimbusEventStore
import org.mozilla.fenix.GleanMetrics.BookmarksManagement
import org.mozilla.fenix.GleanMetrics.Events
import org.mozilla.fenix.GleanMetrics.History
import org.mozilla.fenix.GleanMetrics.Toolbar
import org.mozilla.fenix.NavGraphDirections
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.NimbusComponents
import org.mozilla.fenix.components.UseCases
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.appstate.AppAction.SearchAction.SearchEngineSelected
import org.mozilla.fenix.components.appstate.AppAction.URLCopiedToClipboard
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.components.appstate.search.SearchState as AppSearchState
import org.mozilla.fenix.components.appstate.search.SelectedSearchEngine
import org.mozilla.fenix.components.search.BOOKMARKS_SEARCH_ENGINE_ID
import org.mozilla.fenix.components.share.ShareSource
import org.mozilla.fenix.components.usecases.FenixBrowserUseCases
import org.mozilla.fenix.components.usecases.ShareUseCases
import org.mozilla.fenix.ext.telemetryName
import org.mozilla.fenix.search.SearchEngineSource.Bookmarks
import org.mozilla.fenix.search.SearchEngineSource.Shortcut
import org.mozilla.fenix.search.SearchFragmentAction.CopyCurrentWebsiteDetailsClicked
import org.mozilla.fenix.search.SearchFragmentAction.EditCurrentWebsiteDetailsClicked
import org.mozilla.fenix.search.SearchFragmentAction.ReloadCurrentWebsiteClicked
import org.mozilla.fenix.search.SearchFragmentAction.SearchProvidersUpdated
import org.mozilla.fenix.search.SearchFragmentAction.SearchShortcutEngineSelected
import org.mozilla.fenix.search.SearchFragmentAction.SearchStarted
import org.mozilla.fenix.search.SearchFragmentAction.SearchSuggestionsVisibilityUpdated
import org.mozilla.fenix.search.SearchFragmentAction.ShareCurrentWebsiteDetailsClicked
import org.mozilla.fenix.search.SearchFragmentAction.SuggestionClicked
import org.mozilla.fenix.search.SearchFragmentAction.SuggestionSelected
import org.mozilla.fenix.search.awesomebar.SearchSuggestionsProvidersBuilder
import org.mozilla.fenix.search.fixtures.EMPTY_SEARCH_FRAGMENT_STATE
import org.mozilla.fenix.telemetry.ACTION_SEARCH_ENGINE_SELECTED
import org.mozilla.fenix.telemetry.SOURCE_ADDRESS_BAR
import org.mozilla.fenix.telemetry.SURFACE_BROWSER
import org.mozilla.fenix.telemetry.SURFACE_HOME
import org.mozilla.fenix.utils.Settings
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class FenixSearchMiddlewareTest {
    @get:Rule val gleanTestRule = GleanTestRule(testContext)

    private val engine: Engine = mockk {
        every { speculativeCreateSession(any(), any()) } just Runs
    }
    private val fenixBrowserUseCasesMock: FenixBrowserUseCases = mockk(relaxed = true)
    private val browsingModeManager: BrowsingModeManager = mockk(relaxed = true)
    private val useCases: UseCases = mockk {
        every { fenixBrowserUseCases } returns fenixBrowserUseCasesMock
        every { tabsUseCases } returns mockk()
    }
    private val nimbusComponents: NimbusComponents = mockk()
    private val settings: Settings = mockk(relaxed = true)
    private val browserActionsCaptor = CaptureActionsMiddleware<BrowserState, BrowserAction>()
    private val searchActionsCaptor = CaptureActionsMiddleware<SearchFragmentState, SearchFragmentAction>()
    private val appStore: AppStore = mockk(relaxed = true)
    private var browserStore =
        BrowserStore(
            initialState = BrowserState(search = fakeSearchEnginesState()),
            middleware = listOf(browserActionsCaptor),
        )
    private val toolbarStore: BrowserToolbarStore = mockk(relaxed = true)
    private val navController: NavController = mockk(relaxed = true)
    private val shareUseCases: ShareUseCases = mockk(relaxed = true)
    private val clipboardHandler: ClipboardHandler = mockk(relaxed = true)

    @Test
    fun `WHEN the store is created THEN update the search engines configuration`() {
        val (_, store) = buildMiddlewareAndAddToSearchStore()

        assertNotNull(store.state.defaultEngine)
        assertEquals("Engine B", store.state.defaultEngine!!.name)
        assertIs<SearchEngineSource.Default>(store.state.searchEngineSource)
        assertNotNull(store.state.searchEngineSource.searchEngine)
        assertEquals("Engine B", store.state.searchEngineSource.searchEngine!!.name)
    }

    @Test
    fun `WHEN search is started THEN warmup http engine and configure search providers`() {
        val defaultSearchEngine = fakeSearchEnginesState().selectedOrDefaultSearchEngine
        val preselectedSearchEngine = SearchEngine("engine-a", "Engine A", mockk(), type = SearchEngine.Type.BUNDLED)
        val wasEngineSelectedByUser = false
        val isSearchInPrivateMode = true
        val (middleware, store) = buildMiddlewareAndAddToSearchStore()
        val expectedSuggestionProviders = setOf(mockk<SuggestionProvider>(), mockk<SuggestionProvider>())
        val expectedSearchSuggestionsProvider: SearchSuggestionsProvidersBuilder = mockk {
            every { getProvidersToAdd(any()) } returns expectedSuggestionProviders
        }
        every { middleware.buildSearchSuggestionsProvider(any()) } returns expectedSearchSuggestionsProvider

        store.dispatch(SearchStarted(preselectedSearchEngine, wasEngineSelectedByUser, isSearchInPrivateMode, false))

        verify { engine.speculativeCreateSession(isSearchInPrivateMode) }
        assertEquals(expectedSearchSuggestionsProvider, middleware.suggestionsProvidersBuilder)
        assertEquals(expectedSuggestionProviders.toList(), store.state.searchSuggestionsProviders.toList())
        assertEquals(Shortcut(preselectedSearchEngine), store.state.searchEngineSource)
        assertNotNull(store.state.defaultEngine)
        assertEquals(defaultSearchEngine?.id, store.state.defaultEngine?.id)
        assertFalse(store.state.shouldShowSearchSuggestions)
    }

    @Test
    fun `GIVEN the user preselects a search engine WHEN search is started THEN record telemetry`() {
        val preselectedSearchEngine = SearchEngine("engine-a", "Engine A", mockk(), type = SearchEngine.Type.BUNDLED)
        val (middleware, store) = buildMiddlewareAndAddToSearchStore()
        every { middleware.buildSearchSuggestionsProvider(any()) } returns mockk(relaxed = true)

        store.dispatch(
            SearchStarted(
                selectedSearchEngine = preselectedSearchEngine,
                isUserSelected = true,
                inPrivateMode = false,
                searchStartedForCurrentUrl = false,
            )
        )

        assertSearchEngineSelectedTelemetryRecorded(preselectedSearchEngine.telemetryName())
    }

    @Test
    fun `GIVEN a search started from a browser tab WHEN the user preselects a search engine THEN record telemetry with the browser surface`() {
        val preselectedSearchEngine = SearchEngine("engine-a", "Engine A", mockk(), type = SearchEngine.Type.BUNDLED)
        val middleware = buildMiddleware()
        val store = buildStore(middleware, initialState = buildEmptySearchState(tabId = "test"))
        every { middleware.buildSearchSuggestionsProvider(any()) } returns mockk(relaxed = true)

        store.dispatch(
            SearchStarted(
                selectedSearchEngine = preselectedSearchEngine,
                isUserSelected = true,
                inPrivateMode = false,
                searchStartedForCurrentUrl = false,
            )
        )

        assertSearchEngineSelectedTelemetryRecorded(
            preselectedSearchEngine.telemetryName(),
            surface = SURFACE_BROWSER,
        )
    }

    @Test
    fun `GIVEN the app preselects a search engine WHEN search is started THEN don't record telemetry`() {
        val preselectedSearchEngine = SearchEngine("engine-a", "Engine A", mockk(), type = SearchEngine.Type.BUNDLED)
        val (middleware, store) = buildMiddlewareAndAddToSearchStore()
        every { middleware.buildSearchSuggestionsProvider(any()) } returns mockk(relaxed = true)

        store.dispatch(
            SearchStarted(
                selectedSearchEngine = preselectedSearchEngine,
                isUserSelected = false,
                inPrivateMode = false,
                searchStartedForCurrentUrl = false,
            )
        )

        assertNull(Toolbar.buttonTapped.testGetValue())
    }

    @Test
    fun `WHEN search is started with the default search engine then don't record telemetry`() {
        val (middleware, store) = buildMiddlewareAndAddToSearchStore()
        every { middleware.buildSearchSuggestionsProvider(any()) } returns mockk(relaxed = true)

        store.dispatch(
            SearchStarted(
                null,
                isUserSelected = false,
                inPrivateMode = false,
                searchStartedForCurrentUrl = false,
            )
        )

        assertNull(Toolbar.buttonTapped.testGetValue())
    }

    @Test
    fun `GIVEN should show recent searches and a query is set WHEN search is started THEN show search suggestions`() {
        val defaultSearchEngine = fakeSearchEnginesState().selectedOrDefaultSearchEngine
        val preselectedSearchEngine = SearchEngine("engine-a", "Engine A", mockk(), type = SearchEngine.Type.BUNDLED)
        val wasEngineSelectedByUser = false
        val isSearchInPrivateMode = false
        every { settings.shouldShowRecentSearchSuggestions } returns true
        every { settings.shouldShowSearchSuggestions } returns true
        val (middleware, store) = buildMiddlewareAndAddToSearchStore()
        val expectedSuggestionProviders = setOf(mockk<SuggestionProvider>(), mockk<SuggestionProvider>())
        val expectedSearchSuggestionsProvider: SearchSuggestionsProvidersBuilder = mockk {
            every { getProvidersToAdd(any()) } returns expectedSuggestionProviders
        }
        every { middleware.buildSearchSuggestionsProvider(any()) } returns expectedSearchSuggestionsProvider

        store.dispatch(SearchFragmentAction.UpdateQuery("test"))
        store.dispatch(SearchStarted(preselectedSearchEngine, wasEngineSelectedByUser, isSearchInPrivateMode, false))

        verify { engine.speculativeCreateSession(isSearchInPrivateMode) }
        assertEquals(expectedSearchSuggestionsProvider, middleware.suggestionsProvidersBuilder)
        assertEquals(expectedSuggestionProviders.toList(), store.state.searchSuggestionsProviders.toList())
        assertEquals(Shortcut(preselectedSearchEngine), store.state.searchEngineSource)
        assertNotNull(store.state.defaultEngine)
        assertEquals(defaultSearchEngine?.id, store.state.defaultEngine?.id)
        assertTrue(store.state.shouldShowSearchSuggestions)
    }

    @Test
    fun `GIVEN the search query is updated WHEN it is different than the current URL and not empty THEN show search suggestions`() {
        val (_, store) = buildMiddlewareAndAddToSearchStore()
        every { settings.shouldShowSearchSuggestions } returns true

        store.dispatch(SearchFragmentAction.UpdateQuery(store.state.currentTabData!!.url))
        assertFalse(store.state.shouldShowSearchSuggestions)

        store.dispatch(SearchFragmentAction.UpdateQuery("test"))
        assertTrue(store.state.shouldShowSearchSuggestions)

        store.dispatch(SearchFragmentAction.UpdateQuery(""))
        assertFalse(store.state.shouldShowSearchSuggestions)
    }

    @Test
    fun `GIVEN addressbar focus mode is enabled and search started from a browser tab WHEN the query is empty THEN show search suggestions`() {
        every { settings.showAddressBarInFocusMode } returns true
        val currentTab = createTab("https://mozilla.com")
        stubSearchSourceTab(currentTab.id)
        val (_, store) = buildMiddlewareAndAddToSearchStore(browserStore = buildBrowserStore(currentTab))
        every { settings.shouldShowSearchSuggestions } returns false

        store.dispatch(SearchFragmentAction.UpdateQuery(""))

        assertTrue(store.state.shouldShowSearchSuggestions)
    }

    @Test
    fun `GIVEN addressbar focus mode is enabled and search started from home WHEN the query is empty THEN don't show search suggestions`() {
        every { settings.showAddressBarInFocusMode } returns true
        stubSearchSourceTab(null)
        val (_, store) = buildMiddlewareAndAddToSearchStore()
        every { settings.shouldShowSearchSuggestions } returns false

        store.dispatch(SearchFragmentAction.UpdateQuery(""))

        assertFalse(store.state.shouldShowSearchSuggestions)
    }

    @Test
    fun `GIVEN a search query already exists WHEN the search providers are updated THEN show new search suggestions`() {
        val (_, store) = buildMiddlewareAndAddToSearchStore()
        store.dispatch(SearchFragmentAction.UpdateQuery("test"))
        every { settings.shouldShowSearchSuggestions } returns true

        store.dispatch(SearchProvidersUpdated(listOf(mockk())))

        searchActionsCaptor.assertLastAction(SearchSuggestionsVisibilityUpdated::class) {
            assertTrue(it.visible)
        }
    }

    @Test
    fun `GIVEN trending searches are enabled WHEN search starts starts for the current webpage THEN show new search suggestions`() {
        val (_, store) = buildMiddlewareAndAddToSearchStore()
        every { settings.trendingSearchSuggestionsEnabled } returns true
        every { settings.shouldShowSearchSuggestions } returns true
        val defaultSearchEngine = fakeSearchEnginesState().selectedOrDefaultSearchEngine

        store.dispatch(
            SearchStarted(
                defaultSearchEngine,
                isUserSelected = false,
                inPrivateMode = false,
                searchStartedForCurrentUrl = true,
            )
        )

        searchActionsCaptor.assertLastAction(SearchSuggestionsVisibilityUpdated::class) {
            assertTrue(it.visible)
        }
    }

    @Test
    fun `GIVEN trending searches and trending recent search on homepage are enabled WHEN search starts on the homepage THEN show new search suggestions`() {
        val (_, store) = buildMiddlewareAndAddToSearchStore()
        every { settings.trendingSearchSuggestionsEnabled } returns true
        every { settings.shouldShowSearchSuggestions } returns true
        every { settings.enableHomepageTrendingRecentSearch } returns true
        val defaultSearchEngine = fakeSearchEnginesState().selectedOrDefaultSearchEngine

        store.dispatch(
            SearchStarted(
                defaultSearchEngine,
                isUserSelected = false,
                inPrivateMode = false,
                searchStartedForCurrentUrl = false,
            )
        )

        searchActionsCaptor.assertLastAction(SearchSuggestionsVisibilityUpdated::class) {
            assertTrue(it.visible)
        }
    }

    @Test
    fun `GIVEN trending searches are enabled and trending and recent search on homepage is disabled WHEN search starts on the homepage THEN don't show new search suggestions`() {
        val (_, store) = buildMiddlewareAndAddToSearchStore()
        every { settings.trendingSearchSuggestionsEnabled } returns true
        every { settings.shouldShowSearchSuggestions } returns true
        every { settings.enableHomepageTrendingRecentSearch } returns false
        val defaultSearchEngine = fakeSearchEnginesState().selectedOrDefaultSearchEngine

        store.dispatch(
            SearchStarted(
                defaultSearchEngine,
                isUserSelected = false,
                inPrivateMode = false,
                searchStartedForCurrentUrl = false,
            )
        )

        searchActionsCaptor.assertLastAction(SearchSuggestionsVisibilityUpdated::class) {
            assertFalse(it.visible)
        }
    }

    @Test
    fun `GIVEN addressbar focus mode is enabled and no other suggestions are enabled WHEN search starts THEN show new search suggestions`() {
        every { settings.showAddressBarInFocusMode } returns true
        val currentTab = createTab("https://mozilla.com")
        stubSearchSourceTab(currentTab.id)
        val (_, store) = buildMiddlewareAndAddToSearchStore(browserStore = buildBrowserStore(currentTab))
        every { settings.trendingSearchSuggestionsEnabled } returns false
        every { settings.shouldShowRecentSearchSuggestions } returns false
        every { settings.shouldShowSearchSuggestions } returns false
        val defaultSearchEngine = fakeSearchEnginesState().selectedOrDefaultSearchEngine

        store.dispatch(
            SearchStarted(
                defaultSearchEngine,
                isUserSelected = false,
                inPrivateMode = false,
                searchStartedForCurrentUrl = true,
            )
        )

        searchActionsCaptor.assertLastAction(SearchSuggestionsVisibilityUpdated::class) {
            assertTrue(it.visible)
        }
    }

    @Test
    fun `GIVEN recent search suggestions are enabled WHEN search starts starts for the current webpage THEN show new search suggestions`() {
        val (_, store) = buildMiddlewareAndAddToSearchStore()
        every { settings.shouldShowRecentSearchSuggestions } returns true
        every { settings.shouldShowSearchSuggestions } returns true
        val defaultSearchEngine = fakeSearchEnginesState().selectedOrDefaultSearchEngine

        store.dispatch(
            SearchStarted(
                defaultSearchEngine,
                isUserSelected = false,
                inPrivateMode = false,
                searchStartedForCurrentUrl = true,
            )
        )

        searchActionsCaptor.assertLastAction(SearchSuggestionsVisibilityUpdated::class) {
            assertTrue(it.visible)
        }
    }

    @Test
    fun `GIVEN browsing mode is private and suggestions in private mode not allowed WHEN the query is empty THEN don't show the private suggestions banner`() {
        val (_, store) = buildMiddlewareAndAddToSearchStore()
        every { settings.showSearchSuggestionsInPrivateOnboardingFinished } returns false
        every { settings.shouldShowSearchSuggestions } returns true
        every { settings.shouldShowSearchSuggestionsInPrivate } returns false
        every { browsingModeManager.mode } returns BrowsingMode.Private

        store.dispatch(SearchFragmentAction.UpdateQuery(""))

        assertFalse(store.state.showSearchSuggestionsHint)
    }

    @Test
    fun `GIVEN browsing mode is private and search suggestions not allowed WHEN the query is not empty THEN don't show the private suggestions banner`() {
        val privateAppStore = AppStore(AppState(mode = BrowsingMode.Private))
        val (_, store) = buildMiddlewareAndAddToSearchStore(appStore = privateAppStore)
        every { settings.showSearchSuggestionsInPrivateOnboardingFinished } returns false
        every { settings.shouldShowSearchSuggestions } returns false
        every { settings.shouldShowSearchSuggestionsInPrivate } returns false

        store.dispatch(SearchFragmentAction.UpdateQuery("test"))

        assertFalse(store.state.showSearchSuggestionsHint)
    }

    @Test
    fun `GIVEN browsing mode is private and suggestions in private mode not allowed WHEN the query is not empty THEN show the private suggestions banner`() {
        val (_, store) = buildMiddlewareAndAddToSearchStore()
        every { settings.showSearchSuggestionsInPrivateOnboardingFinished } returns false
        every { settings.shouldShowSearchSuggestions } returns true
        every { settings.shouldShowSearchSuggestionsInPrivate } returns false
        every { browsingModeManager.mode } returns BrowsingMode.Private

        store.dispatch(SearchFragmentAction.UpdateQuery("test"))

        assertTrue(store.state.showSearchSuggestionsHint)
    }

    @Test
    fun `GIVEN browsing mode is private and user has allowed suggestions in private mode WHEN search query is different than the current URL and not empty THEN show search suggestions`() {
        val (_, store) = buildMiddlewareAndAddToSearchStore()
        every { settings.shouldShowSearchSuggestions } returns true
        every { settings.shouldShowSearchSuggestionsInPrivate } returns true
        every { browsingModeManager.mode } returns BrowsingMode.Private

        store.dispatch(SearchFragmentAction.UpdateQuery(store.state.currentTabData!!.url))
        assertFalse(store.state.shouldShowSearchSuggestions)

        store.dispatch(SearchFragmentAction.UpdateQuery("test"))
        assertTrue(store.state.shouldShowSearchSuggestions)
    }

    @Test
    fun `WHEN a new search engine is selected THEN update it in search state and record telemetry`() {
        val newSearchEngineSelection =
            SearchEngine(
                "engine-f",
                "Engine F",
                mockk(),
                type = SearchEngine.Type.BUNDLED_ADDITIONAL,
            )
        val appStore =
            AppStore(
                AppState(
                    searchState =
                        AppSearchState.EMPTY.copy(
                            selectedSearchEngine = SelectedSearchEngine(newSearchEngineSelection, true)
                        )
                )
            )
        val (middleware, store) = buildMiddlewareAndAddToSearchStore(appStore = appStore)
        val expectedSuggestionProviders = setOf(mockk<SuggestionProvider>(), mockk<SuggestionProvider>())
        val expectedSearchSuggestionsProvider: SearchSuggestionsProvidersBuilder = mockk {
            every { getProvidersToAdd(any()) } returns expectedSuggestionProviders
        }
        every { middleware.buildSearchSuggestionsProvider(any()) } returns expectedSearchSuggestionsProvider

        // this triggers observing the search engine updates
        store.dispatch(
            SearchStarted(
                null,
                isUserSelected = false,
                inPrivateMode = false,
                searchStartedForCurrentUrl = false,
            )
        )

        searchActionsCaptor.assertLastAction(SearchShortcutEngineSelected::class) {
            assertEquals(newSearchEngineSelection, it.engine)
            assertFalse(it.browsingMode.isPrivate)
            assertEquals(settings, it.settings)
        }
        assertSearchEngineSelectedTelemetryRecorded(newSearchEngineSelection.telemetryName())
    }

    @Test
    fun `WHEN needing to load an URL THEN open it in browser, record search ended and record telemetry`() {
        val url = "https://mozilla.com"
        val flags = LoadUrlFlags.all()
        every { settings.enableHomepageAsNewTab } returns true
        val middleware = buildMiddleware(useCases = useCases)
        val store = buildStore(middleware)

        middleware.loadUrlUseCase(store).invoke(url, flags, null, null)

        verify { navController.navigate(R.id.browserFragment) }
        verify {
            fenixBrowserUseCasesMock.loadUrlOrSearch(
                searchTermOrURL = url,
                newTab = false,
                private = false,
                flags = flags,
            )
        }
        browserActionsCaptor.assertLastAction(EngagementFinished::class) {
            assertEquals(false, it.abandoned)
        }
        val telemetry = Events.enteredUrl.testGetValue()
        assertEquals("entered_url", telemetry?.get(0)?.name)
        assertEquals("false", telemetry?.get(0)?.extra?.get("autocomplete"))
    }

    @Test
    fun `WHEN needing to search for specific terms THEN open them in browser, record search ended and record telemetry`() =
        runTest {
            val searchTerm = "test"
            every { settings.enableHomepageAsNewTab } returns true
            val nimbusEventsStore: NimbusEventStore = mockk {
                every { recordEvent(any()) } just Runs
            }
            every { nimbusComponents.events } returns nimbusEventsStore
            val middleware = buildMiddleware()
            val store = buildStore(middleware)

            middleware.searchUseCase(store).invoke(searchTerm, null, null)

            verify { navController.navigate(R.id.browserFragment) }
            verify {
                fenixBrowserUseCasesMock.loadUrlOrSearch(
                    searchTermOrURL = searchTerm,
                    newTab = false,
                    private = false,
                    forceSearch = true,
                    searchEngine = store.state.searchEngineSource.searchEngine,
                    flags = LoadUrlFlags.none(),
                )
            }
            browserActionsCaptor.assertLastAction(EngagementFinished::class) {
                assertEquals(false, it.abandoned)
            }
            verify { nimbusEventsStore.recordEvent("performed_search") }
            val telemetry = Events.performedSearch.testGetValue()
            assertEquals("performed_search", telemetry?.get(0)?.name)
            assertEquals("default.suggestion", telemetry?.get(0)?.extra?.get("source"))
        }

    @Test
    fun `WHEN needing to select a specific tab THEN open it in browser and record search ended`() {
        val selectedTabId = "tab2"
        val tabsUseCases: TabsUseCases = mockk(relaxed = true)
        every { useCases.tabsUseCases } returns tabsUseCases
        val middleware = buildMiddleware(useCases = useCases)

        middleware.selectTabUseCase().invoke(selectedTabId)

        verify { tabsUseCases.selectTab(selectedTabId) }
        verify { navController.navigate(R.id.browserFragment) }
        browserActionsCaptor.assertLastAction(EngagementFinished::class) {
            assertEquals(false, it.abandoned)
        }
    }

    @Test
    fun `WHEN the user selects a specific search engine THEN update the search engine to be used for future searches and record telemetry`() {
        val defaultSearchEngine = fakeSearchEnginesState().selectedOrDefaultSearchEngine
        val searchEngineClicked =
            SearchEngine(
                id = BOOKMARKS_SEARCH_ENGINE_ID,
                name = "Bookmarks",
                icon = mockk(),
                type = SearchEngine.Type.APPLICATION,
            )
        val appStore = AppStore()
        val (_, store) = buildMiddlewareAndAddToSearchStore(appStore = appStore)

        appStore.dispatch(AppAction.SearchAction.SearchStarted())
        store.dispatch(
            SearchStarted(
                defaultSearchEngine,
                isUserSelected = false,
                inPrivateMode = false,
                searchStartedForCurrentUrl = false,
            )
        )
        appStore.dispatch(SearchEngineSelected(searchEngineClicked, true))

        assertEquals(Bookmarks(searchEngineClicked), store.state.searchEngineSource)
        assertNotNull(store.state.defaultEngine)
        assertEquals(defaultSearchEngine?.id, store.state.defaultEngine?.id)
        browserActionsCaptor.assertNotDispatched(EngagementFinished::class)
        assertSearchEngineSelectedTelemetryRecorded("bookmarks")
    }

    @Test
    fun `WHEN a search suggestion is clicked THEN exit search mode and execute the custom actions for it`() {
        var wasSuggestionClickHandled = false
        val customSuggestionClickedAction = { wasSuggestionClickHandled = true }
        val clickedSuggestion = Suggestion(provider = mockk(), onSuggestionClicked = customSuggestionClickedAction)
        val (_, store) = buildMiddlewareAndAddToSearchStore()

        store.dispatch(SuggestionClicked(clickedSuggestion))

        assertTrue(wasSuggestionClickHandled)
        verify { toolbarStore.dispatch(BrowserEditToolbarAction.SearchQueryUpdated(BrowserToolbarQuery(""))) }
        browserActionsCaptor.assertLastAction(AwesomeBarAction.SuggestionClicked::class) {
            assertEquals(clickedSuggestion, it.suggestion)
        }
    }

    @Test
    fun `GIVEN the search selector menu is opened WHEN the history search engine item is clicked THEN record telemetry`() {
        val historySuggestion: Suggestion =
            mockk(relaxed = true) {
                every { flags } returns setOf(Suggestion.Flag.HISTORY)
            }
        val (_, store) = buildMiddlewareAndAddToSearchStore()

        store.dispatch(SuggestionClicked(historySuggestion))

        assertNotNull(History.searchResultTapped.testGetValue())
    }

    @Test
    fun `GIVEN the search selector menu is opened WHEN the bookmarks search engine item is clicked THEN record telemetry`() {
        val bookmarksSuggestion: Suggestion =
            mockk(relaxed = true) {
                every { flags } returns setOf(Suggestion.Flag.BOOKMARK)
            }
        val (_, store) = buildMiddlewareAndAddToSearchStore()

        store.dispatch(SuggestionClicked(bookmarksSuggestion))

        assertNotNull(BookmarksManagement.searchResultTapped.testGetValue())
    }

    @Test
    fun `WHEN a search suggestion is selected for edit THEN update the current search query with the search suggestion text`() {
        val selectedSuggestion = Suggestion(provider = mockk(), editSuggestion = "test")
        val (_, store) = buildMiddlewareAndAddToSearchStore()

        store.dispatch(SuggestionSelected(selectedSuggestion))

        verify {
            toolbarStore.dispatch(
                BrowserEditToolbarAction.SearchQueryUpdated(
                    query = BrowserToolbarQuery("test"),
                    isQueryPrefilled = true,
                )
            )
        }
    }

    @Test
    fun `WHEN a search or clipboard suggestion is selected for edit THEN keep its text literal`() {
        val text = "https://example.com/%D1%82%20file?q=%2520"
        val (_, store) = buildMiddlewareAndAddToSearchStore()

        listOf(emptySet(), setOf(Suggestion.Flag.CLIPBOARD)).forEach { flags ->
            store.dispatch(SuggestionSelected(Suggestion(provider = mockk(), editSuggestion = text, flags = flags)))
        }

        verify(exactly = 2) {
            toolbarStore.dispatch(
                BrowserEditToolbarAction.SearchQueryUpdated(
                    query = BrowserToolbarQuery(text),
                    isQueryPrefilled = true,
                )
            )
        }
    }

    @Test
    fun `WHEN a suggestion holding a browser URL is selected for edit THEN make the URL readable`() {
        val url = "https://example.com/%D1%82%D0%B5%D1%81%D1%82%20file?q=hello%20world"
        val (_, store) = buildMiddlewareAndAddToSearchStore()

        listOf(Suggestion.Flag.HISTORY, Suggestion.Flag.BOOKMARK, Suggestion.Flag.OPEN_TAB, Suggestion.Flag.SYNC_TAB)
            .forEach { flag ->
                store.dispatch(
                    SuggestionSelected(Suggestion(provider = mockk(), editSuggestion = url, flags = setOf(flag)))
                )
            }

        verify(exactly = 4) {
            toolbarStore.dispatch(
                BrowserEditToolbarAction.SearchQueryUpdated(
                    query = BrowserToolbarQuery("https://example.com/тест file?q=hello world"),
                    isQueryPrefilled = true,
                )
            )
        }
    }

    @Test
    fun `WHEN choosing to share the current website details THEN share the details of the tab search started for`() {
        stubCurrentNavDestination()
        val currentTab = createTab(url = "https://mozilla.com", title = "Mozilla", private = false)
        stubSearchSourceTab(currentTab.id)
        val (_, store) = buildMiddlewareAndAddToSearchStore(browserStore = buildBrowserStore(currentTab))

        store.dispatch(ShareCurrentWebsiteDetailsClicked)

        verify {
            shareUseCases.shareUrl(
                id = currentTab.id,
                url = currentTab.content.url,
                title = currentTab.content.title,
                source = ShareSource.BROWSER_TOOLBAR,
                isPrivate = false,
                isCustomTab = false,
                navigateToShareFragment = any(),
            )
        }
    }

    @Test
    fun `GIVEN the current tab is in reader mode WHEN choosing to share the current website details THEN share the original URL`() {
        stubCurrentNavDestination()
        val currentTab =
            createTab(
                url = "moz-extension://1234/readerview.html?url=https%3A%2F%2Fmozilla.com",
                title = "Mozilla",
                readerState = ReaderState(active = true, activeUrl = "https://mozilla.com"),
            )
        stubSearchSourceTab(currentTab.id)
        val (_, store) = buildMiddlewareAndAddToSearchStore(browserStore = buildBrowserStore(currentTab))

        store.dispatch(ShareCurrentWebsiteDetailsClicked)

        verify {
            shareUseCases.shareUrl(
                id = currentTab.id,
                url = "https://mozilla.com",
                title = currentTab.content.title,
                source = ShareSource.BROWSER_TOOLBAR,
                isPrivate = false,
                isCustomTab = false,
                navigateToShareFragment = any(),
            )
        }
    }

    @Test
    fun `WHEN choosing to copy the current website details THEN copy the URL of the tab search started for`() {
        val currentTab = createTab(url = "https://mozilla.com", title = "Mozilla", private = false)
        stubSearchSourceTab(currentTab.id)
        val (_, store) = buildMiddlewareAndAddToSearchStore(browserStore = buildBrowserStore(currentTab))

        store.dispatch(CopyCurrentWebsiteDetailsClicked)

        verify { clipboardHandler.text = currentTab.content.url }
        verify(exactly = 0) { clipboardHandler.sensitiveText = any() }
    }

    @Test
    fun `GIVEN a private tab WHEN choosing to copy the current website details THEN copy the URL as sensitive text`() {
        val currentTab = createTab(url = "https://mozilla.com", title = "Mozilla", private = true)
        stubSearchSourceTab(currentTab.id)
        val (_, store) = buildMiddlewareAndAddToSearchStore(browserStore = buildBrowserStore(currentTab))

        store.dispatch(CopyCurrentWebsiteDetailsClicked)

        verify { clipboardHandler.sensitiveText = currentTab.content.url }
        verify(exactly = 0) { clipboardHandler.text = any() }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `GIVEN on Android 12 WHEN choosing to copy the current website details THEN show a snackbar to inform about the operation`() {
        val currentTab = createTab(url = "https://mozilla.com", title = "Mozilla", private = false)
        stubSearchSourceTab(currentTab.id)
        val (_, store) = buildMiddlewareAndAddToSearchStore(browserStore = buildBrowserStore(currentTab))

        store.dispatch(CopyCurrentWebsiteDetailsClicked)

        verify { appStore.dispatch(URLCopiedToClipboard) }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `GIVEN on Android 13 WHEN choosing to copy the current website details THEN don't show a snackbar to inform about the operation`() {
        val currentTab = createTab(url = "https://mozilla.com", title = "Mozilla", private = false)
        stubSearchSourceTab(currentTab.id)
        val (_, store) = buildMiddlewareAndAddToSearchStore(browserStore = buildBrowserStore(currentTab))

        store.dispatch(CopyCurrentWebsiteDetailsClicked)

        verify(exactly = 0) { appStore.dispatch(URLCopiedToClipboard) }
    }

    @Test
    fun `editing an encoded page prefills readable text and leaves copying canonical`() {
        val url = "https://example.com/%D1%82%D0%B5%D1%81%D1%82%20file?q=hello%20world"
        val tab = createTab(url = url)
        stubSearchSourceTab(tab.id)
        val (_, store) = buildMiddlewareAndAddToSearchStore(browserStore = buildBrowserStore(tab))
        store.dispatch(EditCurrentWebsiteDetailsClicked)
        verify {
            toolbarStore.dispatch(
                BrowserEditToolbarAction.SearchQueryUpdated(
                    BrowserToolbarQuery("https://example.com/тест file?q=hello world"),
                    true,
                )
            )
        }
        store.dispatch(CopyCurrentWebsiteDetailsClicked)
        verify { clipboardHandler.text = url }
    }

    @Test
    fun `readable current page prefill does not enable search suggestions`() {
        val url = "https://example.com/a%20b?token=secret%20value"
        val store =
            buildStore(initialState = buildEmptySearchState().copy(currentTabData = CurrentTabData("", url, null)))
        store.dispatch(SearchFragmentAction.UpdateQuery(URLStringUtils.toLosslessEditableUrl(url)))
        assertFalse(store.state.shouldShowSearchSuggestions)
        assertFalse(store.state.showSearchSuggestionsHint)
    }

    @Test
    fun `WHEN choosing to edit the current website URL THEN add in the addressbar the URL of the tab search started for`() {
        val currentTab = createTab(url = "https://mozilla.com", title = "Mozilla", private = false)
        stubSearchSourceTab(currentTab.id)
        val (_, store) = buildMiddlewareAndAddToSearchStore(browserStore = buildBrowserStore(currentTab))

        store.dispatch(EditCurrentWebsiteDetailsClicked)

        verify {
            toolbarStore.dispatch(
                BrowserEditToolbarAction.SearchQueryUpdated(
                    query = BrowserToolbarQuery(currentTab.content.url),
                    isQueryPrefilled = true,
                )
            )
        }
    }

    @Test
    fun `WHEN choosing to edit the current search terms THEN add in the addressbar the search terms of the tab search started for`() {
        val currentTab =
            createTab(url = "https://mozilla.com", title = "Mozilla", searchTerms = "test", private = false)
        stubSearchSourceTab(currentTab.id)
        val (_, store) = buildMiddlewareAndAddToSearchStore(browserStore = buildBrowserStore(currentTab))

        store.dispatch(EditCurrentWebsiteDetailsClicked)

        verify {
            toolbarStore.dispatch(
                BrowserEditToolbarAction.SearchQueryUpdated(
                    query = BrowserToolbarQuery(currentTab.content.searchTerms),
                    isQueryPrefilled = true,
                )
            )
        }
    }

    @Test
    fun `GIVEN the current tab is in reader mode WHEN choosing to edit the current website URL THEN add in the addressbar the original URL`() {
        val currentTab =
            createTab(
                url = "moz-extension://1234/readerview.html?url=https%3A%2F%2Fmozilla.com",
                title = "Mozilla",
                readerState = ReaderState(active = true, activeUrl = "https://mozilla.com"),
            )
        stubSearchSourceTab(currentTab.id)
        val (_, store) = buildMiddlewareAndAddToSearchStore(browserStore = buildBrowserStore(currentTab))

        store.dispatch(EditCurrentWebsiteDetailsClicked)

        verify {
            toolbarStore.dispatch(
                BrowserEditToolbarAction.SearchQueryUpdated(
                    query = BrowserToolbarQuery("https://mozilla.com"),
                    isQueryPrefilled = true,
                )
            )
        }
    }

    @Test
    fun `WHEN needing to reload the current website THEN navigate to the browser screen and reload the tab the search was started from`() {
        val tab1 = createTab(url = "https://mozilla.com", title = "1", private = false)
        val tab2 = createTab(url = "https://mozilla.com", title = "2", private = false)
        val tab3 = createTab(url = "https://mozilla.com", title = "3", private = false)
        stubSearchSourceTab(tab2.id)
        val tabsUseCases: TabsUseCases = mockk(relaxed = true)
        val sessionUseCases: SessionUseCases = mockk(relaxed = true)
        val useCases: UseCases = mockk {
            every { this@mockk.tabsUseCases } returns tabsUseCases
            every { this@mockk.sessionUseCases } returns sessionUseCases
        }
        val browserActionsCaptor = CaptureActionsMiddleware<BrowserState, BrowserAction>()
        val browserStore =
            BrowserStore(
                BrowserState(tabs = listOf(tab1, tab2, tab3)),
                middleware = listOf(browserActionsCaptor),
            )
        val (_, store) =
            buildMiddlewareAndAddToSearchStore(
                useCases = useCases,
                browserStore = browserStore,
            )

        store.dispatch(ReloadCurrentWebsiteClicked)

        verifyOrder {
            tabsUseCases.selectTab(tab2.id)
            navController.navigate(NavGraphDirections.actionGlobalBrowser(), null)
            sessionUseCases.reload(tab2.id)
        }
        browserActionsCaptor.assertLastAction(EngagementFinished::class) {
            assertEquals(true, it.abandoned)
        }
    }

    private fun buildMiddlewareAndAddToSearchStore(
        engine: Engine = this.engine,
        useCases: UseCases = this.useCases,
        settings: Settings = this.settings,
        appStore: AppStore = this.appStore,
        browserStore: BrowserStore = this.browserStore,
        toolbarStore: BrowserToolbarStore = this.toolbarStore,
        shareUseCases: ShareUseCases = this.shareUseCases,
        clipboardHandler: ClipboardHandler = this.clipboardHandler,
    ): Pair<FenixSearchMiddleware, SearchFragmentStore> {
        val middleware =
            buildMiddleware(
                engine = engine,
                useCases = useCases,
                nimbusComponents = nimbusComponents,
                settings = settings,
                appStore = appStore,
                browserStore = browserStore,
                toolbarStore = toolbarStore,
                shareUseCases = shareUseCases,
                clipboardHandler = clipboardHandler,
            )
        every { middleware.buildSearchSuggestionsProvider(any()) } returns mockk(relaxed = true)

        val store = buildStore(middleware)

        return middleware to store
    }

    private fun buildMiddleware(
        engine: Engine = this.engine,
        useCases: UseCases = this.useCases,
        nimbusComponents: NimbusComponents = this.nimbusComponents,
        settings: Settings = this.settings,
        appStore: AppStore = this.appStore,
        browserStore: BrowserStore = this.browserStore,
        toolbarStore: BrowserToolbarStore = this.toolbarStore,
        navController: NavController = this.navController,
        browsingModeManager: BrowsingModeManager = this.browsingModeManager,
        shareUseCases: ShareUseCases = this.shareUseCases,
        clipboardHandler: ClipboardHandler = this.clipboardHandler,
    ): FenixSearchMiddleware {
        val middleware =
            spyk(
                FenixSearchMiddleware(
                    fragment =
                        spyk(Fragment()) {
                            every { viewLifecycleOwner } returns FakeLifecycleOwner(Lifecycle.State.RESUMED)
                        },
                    engine = engine,
                    useCases = useCases,
                    nimbusComponents = nimbusComponents,
                    settings = settings,
                    appStore = appStore,
                    browserStore = browserStore,
                    toolbarStore = toolbarStore,
                    navController = navController,
                    browsingModeManager = browsingModeManager,
                    shareUseCases = shareUseCases,
                    clipboardHandler = clipboardHandler,
                )
            )
        every { middleware.buildSearchSuggestionsProvider(any()) } returns mockk(relaxed = true)

        return middleware
    }

    private fun buildStore(
        middleware: FenixSearchMiddleware = buildMiddleware(),
        initialState: SearchFragmentState = buildEmptySearchState(),
    ) =
        SearchFragmentStore(
            initialState = initialState,
            middleware = listOf(middleware, searchActionsCaptor),
        )

    private fun buildEmptySearchState(
        searchEngineSource: SearchEngineSource = SearchEngineSource.Default(searchEngine = mockk()),
        defaultEngine: SearchEngine? = mockk(),
        showHistorySuggestionsForCurrentEngine: Boolean = true,
        showSponsoredSuggestions: Boolean = true,
        showNonSponsoredSuggestions: Boolean = true,
        tabId: String? = null,
    ): SearchFragmentState =
        EMPTY_SEARCH_FRAGMENT_STATE.copy(
            currentTabData = CurrentTabData("", "", null),
            searchEngineSource = searchEngineSource,
            defaultEngine = defaultEngine,
            showSearchTermHistory = true,
            showHistorySuggestionsForCurrentEngine = showHistorySuggestionsForCurrentEngine,
            showSponsoredSuggestions = showSponsoredSuggestions,
            showNonSponsoredSuggestions = showNonSponsoredSuggestions,
            showQrButton = true,
            tabId = tabId,
        )

    private fun buildBrowserStore(vararg tabs: TabSessionState) =
        BrowserStore(
            initialState =
                BrowserState(
                    tabs = tabs.toList(),
                    selectedTabId = tabs.firstOrNull()?.id,
                    search = fakeSearchEnginesState(),
                )
        )

    private fun stubCurrentNavDestination(destinationId: Int? = R.id.browserFragment) {
        every { navController.currentDestination } returns
            destinationId?.let { mockk<NavDestination> { every { id } returns it } }
    }

    private fun stubSearchSourceTab(tabId: String?) {
        every { appStore.state } returns AppState(searchState = AppSearchState.EMPTY.copy(sourceTabId = tabId))
    }

    private fun fakeSearchEnginesState() =
        SearchState(
            region = RegionState("US", "US"),
            regionSearchEngines =
                listOf(
                    SearchEngine("engine-a", "Engine A", mockk(), type = SearchEngine.Type.BUNDLED),
                    SearchEngine(
                        "engine-b",
                        "Engine B",
                        mockk(),
                        type = SearchEngine.Type.BUNDLED,
                        trendingUrl = "test",
                    ),
                    SearchEngine("engine-c", "Engine C", mockk(), type = SearchEngine.Type.BUNDLED),
                ),
            customSearchEngines =
                listOf(
                    SearchEngine("engine-d", "Engine D", mockk(), type = SearchEngine.Type.CUSTOM),
                    SearchEngine("engine-e", "Engine E", mockk(), type = SearchEngine.Type.CUSTOM),
                ),
            additionalSearchEngines =
                listOf(SearchEngine("engine-f", "Engine F", mockk(), type = SearchEngine.Type.BUNDLED_ADDITIONAL)),
            additionalAvailableSearchEngines =
                listOf(
                    SearchEngine("engine-g", "Engine G", mockk(), type = SearchEngine.Type.BUNDLED_ADDITIONAL),
                    SearchEngine("engine-h", "Engine H", mockk(), type = SearchEngine.Type.BUNDLED_ADDITIONAL),
                ),
            hiddenSearchEngines =
                listOf(SearchEngine("engine-i", "Engine I", mockk(), type = SearchEngine.Type.BUNDLED)),
            regionDefaultSearchEngineId = "engine-b",
            userSelectedSearchEngineId = null,
            userSelectedSearchEngineName = null,
        )

    private fun assertSearchEngineSelectedTelemetryRecorded(
        extra: String,
        surface: String = SURFACE_HOME,
    ) {
        val values = Toolbar.buttonTapped.testGetValue()
        assertNotNull(values)
        val last = values.last()
        assertEquals(ACTION_SEARCH_ENGINE_SELECTED, last.extra?.get("item"))
        assertEquals(SOURCE_ADDRESS_BAR, last.extra?.get("source"))
        assertEquals(extra, last.extra?.get("extra"))
        assertEquals(surface, last.extra?.get("surface"))
    }
}

private class FakeLifecycleOwner(initialState: Lifecycle.State) : LifecycleOwner {
    override val lifecycle: Lifecycle =
        LifecycleRegistry(this).apply {
            currentState = initialState
        }
}
