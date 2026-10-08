/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.search

import android.content.Context
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlin.test.assertNotNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mozilla.components.browser.domains.autocomplete.BaseDomainAutocompleteProvider
import mozilla.components.browser.state.action.AwesomeBarAction.EngagementFinished
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.SearchAction.ApplicationSearchEnginesLoaded
import mozilla.components.browser.state.search.RegionState
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.SearchState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.state.selectedOrDefaultSearchEngine
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.storage.sync.PlacesBookmarksStorage
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import mozilla.components.browser.toolbar.R as toolbarR
import mozilla.components.compose.browser.toolbar.concept.Action.ActionButton
import mozilla.components.compose.browser.toolbar.concept.Action.ActionButtonRes
import mozilla.components.compose.browser.toolbar.concept.Action.SearchSelectorAction
import mozilla.components.compose.browser.toolbar.store.BrowserEditToolbarAction.SearchQueryUpdated
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarAction.CommitUrl
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarAction.EnterEditMode
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarAction.ExitEditMode
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarInteraction.BrowserToolbarEvent
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarStore
import mozilla.components.compose.browser.toolbar.ui.BrowserToolbarQuery
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.toolbar.AutocompleteProvider
import mozilla.components.concept.toolbar.AutocompleteResult
import mozilla.components.feature.awesomebar.provider.SessionAutocompleteProvider
import mozilla.components.feature.qr.R as qrR
import mozilla.components.feature.syncedtabs.SyncedTabsAutocompleteProvider
import mozilla.components.support.test.middleware.CaptureActionsMiddleware
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.ui.icons.R as iconsR
import mozilla.telemetry.glean.testing.GleanTestRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.GleanMetrics.Events
import org.mozilla.fenix.GleanMetrics.Toolbar
import org.mozilla.fenix.GleanMetrics.ToolbarGoogleLensButton
import org.mozilla.fenix.NavGraphDirections
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.BrowserFragmentDirections
import org.mozilla.fenix.browser.browsingmode.BrowsingMode.Normal
import org.mozilla.fenix.browser.browsingmode.BrowsingMode.Private
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.Components
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.appstate.AppAction.LensAction.LensDismissed
import org.mozilla.fenix.components.appstate.AppAction.LensAction.LensRequestConsumed
import org.mozilla.fenix.components.appstate.AppAction.LensAction.LensRequested
import org.mozilla.fenix.components.appstate.AppAction.LensAction.LensResultAvailable
import org.mozilla.fenix.components.appstate.AppAction.QrScannerAction.QrScannerInputAvailable
import org.mozilla.fenix.components.appstate.AppAction.QrScannerAction.QrScannerInputConsumed
import org.mozilla.fenix.components.appstate.AppAction.QrScannerAction.QrScannerRequested
import org.mozilla.fenix.components.appstate.AppAction.SearchAction.SearchEnded
import org.mozilla.fenix.components.appstate.AppAction.SearchAction.SearchStarted
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.components.appstate.VoiceSearchAction.VoiceInputRequested
import org.mozilla.fenix.components.appstate.search.SearchState as AppSearchState
import org.mozilla.fenix.components.appstate.search.SelectedSearchEngine
import org.mozilla.fenix.components.lens.CameraMode
import org.mozilla.fenix.components.search.BOOKMARKS_SEARCH_ENGINE_ID
import org.mozilla.fenix.components.search.HISTORY_SEARCH_ENGINE_ID
import org.mozilla.fenix.components.search.TABS_SEARCH_ENGINE_ID
import org.mozilla.fenix.components.usecases.FenixBrowserUseCases
import org.mozilla.fenix.search.EditPageEndActionsInteractions.ClearSearchClicked
import org.mozilla.fenix.search.EditPageEndActionsInteractions.LensButtonClicked
import org.mozilla.fenix.search.EditPageEndActionsInteractions.QrScannerClicked
import org.mozilla.fenix.search.EditPageEndActionsInteractions.VoiceSearchButtonClicked
import org.mozilla.fenix.search.SearchSelectorEvents.SearchSelectorClicked
import org.mozilla.fenix.search.SearchSelectorEvents.SearchSelectorItemClicked
import org.mozilla.fenix.search.SearchSelectorEvents.SearchSettingsItemClicked
import org.mozilla.fenix.search.ext.searchEngineShortcuts
import org.mozilla.fenix.search.fixtures.assertSearchSelectorEquals
import org.mozilla.fenix.search.fixtures.buildExpectedSearchSelector
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.telemetry.ACTION_CLEAR_CLICKED
import org.mozilla.fenix.telemetry.ACTION_LENS_CLICKED
import org.mozilla.fenix.telemetry.ACTION_MICROPHONE_CLICKED
import org.mozilla.fenix.telemetry.ACTION_QR_CLICKED
import org.mozilla.fenix.telemetry.ACTION_SEARCH_ENGINE_SELECTOR_CLICKED
import org.mozilla.fenix.telemetry.SOURCE_ADDRESS_BAR
import org.mozilla.fenix.telemetry.SURFACE_BROWSER
import org.mozilla.fenix.telemetry.SURFACE_HOME
import org.mozilla.fenix.utils.Settings
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrowserToolbarSearchMiddlewareTest {
    @get:Rule val gleanTestRule = GleanTestRule(testContext)

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = CoroutineScope(testDispatcher)
    private val captureBrowserActionsMiddleware = CaptureActionsMiddleware<BrowserState, BrowserAction>()

    val appStore = AppStore()
    val browserStore =
        BrowserStore(
            BrowserState(search = fakeSearchState()),
            middleware = listOf(captureBrowserActionsMiddleware),
        )
    val components: Components = mockk()
    val settings: Settings =
        mockk(relaxed = true) {
            every { lensCameraLastMode } returns CameraMode.LENS
        }
    val navController: NavController = mockk {
        every { navigate(any<NavDirections>()) } just Runs
        every { navigate(any<Int>()) } just Runs
    }
    val browsingModeManager: BrowsingModeManager = mockk {
        every { mode } returns Normal
    }

    @Test
    fun `WHEN the toolbar enters in edit mode THEN a new search selector button is added`() {
        val (_, store) = buildMiddlewareAndAddToStore()

        store.dispatch(EnterEditMode(false))

        assertSearchSelectorEquals(
            expectedSearchSelector(),
            store.state.editState.editActionsStart[0] as SearchSelectorAction,
        )
    }

    @Test
    fun `WHEN the toolbar enters in edit mode with non-blank query THEN a clear button is shown`() {
        val (_, store) = buildMiddlewareAndAddToStore()

        store.dispatch(EnterEditMode(false))
        store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("test")))

        assertEquals(
            expectedClearButton,
            store.state.editState.editActionsEnd.last() as ActionButtonRes,
        )
    }

    @Test
    fun `WHEN the toolbar enters in edit mode with blank query THEN a qr scanner button is shown`() {
        val (_, store) = buildMiddlewareAndAddToStore()

        store.dispatch(EnterEditMode(false))
        store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("")))

        assertEquals(
            expectedQrButton,
            store.state.editState.editActionsEnd.last() as ActionButtonRes,
        )
    }

    @Test
    fun `WHEN the toolbar enters in edit mode with blank query AND user starts typing THEN qr button is replaced by clear button`() {
        val (_, store) = buildMiddlewareAndAddToStore()

        store.dispatch(EnterEditMode(false))
        store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("")))

        assertEquals(
            expectedQrButton,
            store.state.editState.editActionsEnd.last() as ActionButtonRes,
        )

        store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("a")))

        assertEquals(
            expectedClearButton,
            store.state.editState.editActionsEnd.last() as ActionButtonRes,
        )
    }

    @Test
    fun `WHEN the toolbar enters in edit mode with non-blank query AND the clear button is clicked THEN text is cleared and telemetry is recorded`() {
        val (_, store) = buildMiddlewareAndAddToStore()
        store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("test")))
        store.dispatch(EnterEditMode(false))

        val clearButton = store.state.editState.editActionsEnd.last() as ActionButtonRes
        assertEquals(expectedClearButton, clearButton)

        store.dispatch(clearButton.onClick as BrowserToolbarEvent)
        assertEquals(store.state.editState.query.current, "")
        assertTelemetryRecorded(ACTION_CLEAR_CLICKED)
    }

    @Test
    fun `GIVEN a custom search engine WHEN the qr button is clicked THEN start qr recognition and record telemetry`() {
        val appStore: AppStore =
            mockk(relaxed = true) {
                every { state.searchState } returns
                    AppSearchState.EMPTY.copy(
                        selectedSearchEngine =
                            SelectedSearchEngine(
                                searchEngine = fakeSearchState().customSearchEngines.first(),
                                isUserSelected = true,
                            )
                    )
            }
        val (_, store) = buildMiddlewareAndAddToStore(appStore = appStore)
        store.dispatch(EnterEditMode(false))

        val qrButton = store.state.editState.editActionsEnd.last() as ActionButtonRes
        assertEquals(expectedQrButton, qrButton)

        store.dispatch(qrButton.onClick as BrowserToolbarEvent)
        assertTelemetryRecorded(ACTION_QR_CLICKED)
        verify { appStore.dispatch(QrScannerRequested) }
    }

    @Test
    fun `WHEN the voice search button is clicked THEN start voice recognition and record telemetry`() {
        val appStore: AppStore =
            mockk(relaxed = true) {
                every { state.searchState } returns
                    AppSearchState.EMPTY.copy(selectedSearchEngine = mockk(relaxed = true))
            }
        every { settings.shouldShowVoiceSearch } returns true
        val middleware = spyk(buildMiddleware(appStore = appStore))
        every { middleware.isSpeechRecognitionAvailable() } returns true
        val store = buildStore(middleware)
        store.dispatch(EnterEditMode(false))

        val voiceSearchButton = store.state.editState.editActionsEnd.last() as ActionButtonRes
        assertEquals(expectedVoiceSearchButton, voiceSearchButton)

        store.dispatch(voiceSearchButton.onClick as BrowserToolbarEvent)
        assertTelemetryRecorded(ACTION_MICROPHONE_CLICKED)
        verify { appStore.dispatch(VoiceInputRequested) }
    }

    @Test
    fun `WHEN the search selector button is clicked THEN record a telemetry event`() {
        val (_, store) = buildMiddlewareAndAddToStore()

        store.dispatch(SearchSelectorClicked)

        assertTelemetryRecorded(ACTION_SEARCH_ENGINE_SELECTOR_CLICKED)
    }

    @Test
    fun `GIVEN a search started from a browser tab WHEN the search selector button is clicked THEN record telemetry with the browser surface`() {
        val appStore = AppStore(initialState = AppState(searchState = AppSearchState.EMPTY.copy(sourceTabId = "test")))
        val (_, store) = buildMiddlewareAndAddToStore(appStore = appStore)

        store.dispatch(SearchSelectorClicked)

        assertTelemetryRecorded(ACTION_SEARCH_ENGINE_SELECTOR_CLICKED, surface = SURFACE_BROWSER)
    }

    @Test
    fun `GIVEN the search selector menu is open WHEN the search settings button is clicked THEN exit edit mode and open search settings`() {
        val captorMiddleware = CaptureActionsMiddleware<AppState, AppAction>()
        val appStore = AppStore(middlewares = listOf(captorMiddleware))
        val (_, store) = buildMiddlewareAndAddToStore(appStore = appStore)
        appStore.dispatch(SearchStarted())
        store.dispatch(EnterEditMode(false))
        store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("test")))
        assertTrue(store.state.isEditMode())
        assertTrue(appStore.state.searchState.isSearchActive)
        assertEquals("test", store.state.editState.query.current)

        store.dispatch(SearchSettingsItemClicked)

        assertFalse(appStore.state.searchState.isSearchActive)
        assertEquals("", store.state.editState.query.current)
        captorMiddleware.assertLastAction(SearchEnded::class) {}
        captureBrowserActionsMiddleware.assertFirstAction(EngagementFinished::class) { action ->
            assertTrue(action.abandoned)
        }

        verify {
            navController.navigate(BrowserFragmentDirections.actionGlobalSearchEngineFragment())
        }
    }

    @Test
    fun `GIVEN the search selector menu is open WHEN a menu item is clicked THEN update the selected search engine and rebuild the menu`() {
        val (_, store) = buildMiddlewareAndAddToStore()
        val newEngineSelection = fakeSearchState().searchEngineShortcuts.last()
        store.dispatch(EnterEditMode(false))
        assertSearchSelectorEquals(
            expectedSearchSelector(),
            store.state.editState.editActionsStart[0] as SearchSelectorAction,
        )

        store.dispatch(SearchSelectorItemClicked(newEngineSelection))

        assertSearchSelectorEquals(
            expectedSearchSelector(newEngineSelection),
            store.state.editState.editActionsStart[0] as SearchSelectorAction,
        )
    }

    @Test
    fun `GIVEN a search was started from a tab WHEN the search engine is changed THEN keep the source tab so the search loads in it`() {
        val captorMiddleware = CaptureActionsMiddleware<AppState, AppAction>()
        val appStore =
            AppStore(
                initialState =
                    AppState(
                        searchState =
                            AppSearchState.EMPTY.copy(
                                isSearchActive = true,
                                sourceTabId = "test",
                            )
                    ),
                middlewares = listOf(captorMiddleware),
            )
        val (_, store) = buildMiddlewareAndAddToStore(appStore = appStore)
        val newEngineSelection = fakeSearchState().searchEngineShortcuts.last()

        store.dispatch(SearchSelectorItemClicked(newEngineSelection))

        assertEquals("test", appStore.state.searchState.sourceTabId)
        captorMiddleware.assertLastAction(SearchStarted::class) { action ->
            assertEquals("test", action.tabId)
        }
    }

    @Test
    fun `GIVEN the search selector menu is open while in display mode WHEN a menu item is clicked THEN enter edit mode`() {
        val (_, store) = buildMiddlewareAndAddToStore()
        val newEngineSelection = fakeSearchState().searchEngineShortcuts.last()
        store.dispatch(ExitEditMode)
        assertFalse(store.state.isEditMode())

        store.dispatch(SearchSelectorItemClicked(newEngineSelection))

        assertTrue(appStore.state.searchState.isSearchActive)
    }

    @Test
    fun `GIVEN default engine selected WHEN entering in edit mode THEN set autocomplete suggestions and page end buttons`() =
        runTest(testDispatcher) {
            every { settings.shouldAutocompleteInAwesomebar } returns true
            every { settings.shouldShowHistorySuggestions } returns true
            every { settings.shouldShowBookmarkSuggestions } returns true
            every { settings.shouldShowVoiceSearch } returns true
            val engine: Engine = mockk {
                every { speculativeConnect(any()) } just Runs
            }
            every { components.core.engine } returns engine
            configureAutocompleteProvidersInComponents()
            val middleware = spyk(buildMiddleware())
            every { middleware.isSpeechRecognitionAvailable() } returns true
            val store = buildStore(middleware)
            val autocompleteProvidersSlot = slot<List<AutocompleteProvider>>()

            store.dispatch(EnterEditMode(false))
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify(exactly = 0) {
                middleware.fetchAutocomplete(
                    autocompleteProviders = any(),
                    input = "",
                )
            }
            assertNull(store.state.editState.suggestion)
            assertEquals(2, store.state.editState.editActionsEnd.size)
            assertEquals(expectedVoiceSearchButton, store.state.editState.editActionsEnd.first())
            assertEquals(expectedQrButton, store.state.editState.editActionsEnd.last())

            store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("test")))
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify {
                middleware.fetchAutocomplete(
                    autocompleteProviders = capture(autocompleteProvidersSlot),
                    input = "test",
                )
            }
            assertEquals(
                autocompleteProvidersSlot.captured.map { it.javaClass::getSimpleName },
                listOfNotNull(
                        components.core.historyStorage,
                        components.core.bookmarksStorage,
                        components.core.domainsAutocompleteProvider,
                    )
                    .map { it.javaClass::getSimpleName },
            )
            assertEquals(store.state.editState.suggestion?.text, "history")
            verify { engine.speculativeConnect("history.com") }
            assertEquals(2, store.state.editState.editActionsEnd.size)
            assertEquals(expectedVoiceSearchButton, store.state.editState.editActionsEnd.first())
            assertEquals(expectedClearButton, store.state.editState.editActionsEnd.last())
        }

    @Test
    fun `GIVEN default engine selected WHEN new query is prefilled THEN update end buttons but don't autocomplete`() =
        runTest(testDispatcher) {
            every { settings.shouldAutocompleteInAwesomebar } returns true
            every { settings.shouldShowHistorySuggestions } returns true
            every { settings.shouldShowBookmarkSuggestions } returns true
            every { settings.shouldShowVoiceSearch } returns true
            val engine: Engine = mockk {
                every { speculativeConnect(any()) } just Runs
            }
            every { components.core.engine } returns engine
            configureAutocompleteProvidersInComponents()
            val middleware = spyk(buildMiddleware())
            every { middleware.isSpeechRecognitionAvailable() } returns true
            val store = buildStore(middleware)
            val autocompleteProvidersSlot = slot<List<AutocompleteProvider>>()

            store.dispatch(EnterEditMode(false))
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify(exactly = 0) {
                middleware.fetchAutocomplete(
                    autocompleteProviders = any(),
                    input = "",
                )
            }
            assertNull(store.state.editState.suggestion)
            assertEquals(2, store.state.editState.editActionsEnd.size)
            assertEquals(expectedVoiceSearchButton, store.state.editState.editActionsEnd.first())
            assertEquals(expectedQrButton, store.state.editState.editActionsEnd.last())

            store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("test"), true))
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify(exactly = 0) {
                middleware.fetchAutocomplete(
                    autocompleteProviders = capture(autocompleteProvidersSlot),
                    input = "test",
                )
            }
            assertNull(store.state.editState.suggestion)
            assertEquals(2, store.state.editState.editActionsEnd.size)
            assertEquals(expectedVoiceSearchButton, store.state.editState.editActionsEnd.first())
            assertEquals(expectedClearButton, store.state.editState.editActionsEnd.last())
        }

    @Test
    fun `GIVEN default engine selected and history suggestions disabled WHEN entering in edit mode THEN set autocomplete suggestions`() =
        runTest(testDispatcher) {
            every { settings.shouldAutocompleteInAwesomebar } returns true
            every { settings.shouldShowHistorySuggestions } returns false
            every { settings.shouldShowBookmarkSuggestions } returns true
            val engine: Engine = mockk {
                every { speculativeConnect(any()) } just Runs
            }
            every { components.core.engine } returns engine
            configureAutocompleteProvidersInComponents()
            val middleware = spyk(buildMiddleware())
            val store = buildStore(middleware)
            val autocompleteProvidersSlot = slot<List<AutocompleteProvider>>()

            store.dispatch(EnterEditMode(false))
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify(exactly = 0) {
                middleware.fetchAutocomplete(
                    autocompleteProviders = any(),
                    input = "",
                )
            }
            assertNull(store.state.editState.suggestion)

            store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("test")))
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify {
                middleware.fetchAutocomplete(
                    autocompleteProviders = capture(autocompleteProvidersSlot),
                    input = "test",
                )
            }
            assertEquals(
                autocompleteProvidersSlot.captured.map { it.javaClass::getSimpleName },
                listOfNotNull(
                        components.core.bookmarksStorage,
                        components.core.domainsAutocompleteProvider,
                    )
                    .map { it.javaClass::getSimpleName },
            )
            assertEquals(store.state.editState.suggestion?.text, "bookmarks")
            verify { engine.speculativeConnect("bookmarks.com") }
        }

    @Test
    fun `GIVEN default engine selected and bookmarks suggestions disabled WHEN entering in edit mode THEN set autocomplete suggestions`() =
        runTest(testDispatcher) {
            every { settings.shouldAutocompleteInAwesomebar } returns true
            every { settings.shouldShowHistorySuggestions } returns true
            every { settings.shouldShowBookmarkSuggestions } returns false
            val engine: Engine = mockk {
                every { speculativeConnect(any()) } just Runs
            }
            every { components.core.engine } returns engine
            configureAutocompleteProvidersInComponents()
            val middleware = spyk(buildMiddleware())
            val store = buildStore(middleware)
            val autocompleteProvidersSlot = slot<List<AutocompleteProvider>>()

            store.dispatch(EnterEditMode(false))
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify(exactly = 0) {
                middleware.fetchAutocomplete(
                    autocompleteProviders = any(),
                    input = "",
                )
            }
            assertNull(store.state.editState.suggestion)

            store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("test")))
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify {
                middleware.fetchAutocomplete(
                    autocompleteProviders = capture(autocompleteProvidersSlot),
                    input = "test",
                )
            }

            assertEquals(
                autocompleteProvidersSlot.captured.map { it.javaClass::getSimpleName },
                listOfNotNull(
                        components.core.historyStorage,
                        components.core.domainsAutocompleteProvider,
                    )
                    .map { it.javaClass::getSimpleName },
            )
            assertEquals(store.state.editState.suggestion?.text, "history")
            verify { engine.speculativeConnect("history.com") }
        }

    @Test
    fun `GIVEN default engine selected and history + bookmarks suggestions disabled WHEN entering in edit mode THEN set autocomplete suggestions`() =
        runTest(testDispatcher) {
            every { settings.shouldAutocompleteInAwesomebar } returns true
            every { settings.shouldShowHistorySuggestions } returns false
            every { settings.shouldShowBookmarkSuggestions } returns false
            val engine: Engine = mockk {
                every { speculativeConnect(any()) } just Runs
            }
            every { components.core.engine } returns engine
            configureAutocompleteProvidersInComponents()
            val middleware = spyk(buildMiddleware())
            val store = buildStore(middleware)
            val autocompleteProvidersSlot = slot<List<AutocompleteProvider>>()

            store.dispatch(EnterEditMode(false))
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify(exactly = 0) {
                middleware.fetchAutocomplete(
                    autocompleteProviders = any(),
                    input = "",
                )
            }
            assertNull(store.state.editState.suggestion)

            store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("test")))
            testDispatcher.scheduler.advanceUntilIdle()
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify {
                middleware.fetchAutocomplete(
                    autocompleteProviders = capture(autocompleteProvidersSlot),
                    input = "test",
                )
            }
            assertEquals(
                autocompleteProvidersSlot.captured.map { it.javaClass::getSimpleName },
                listOfNotNull(components.core.domainsAutocompleteProvider).map { it.javaClass::getSimpleName },
            )
            assertEquals(store.state.editState.suggestion?.text, "domains")
            verify { engine.speculativeConnect("domains.com") }
        }

    @Test
    fun `GIVEN tabs engine selected WHEN entering in edit mode THEN set autocomplete suggestions and page end buttons`() =
        runTest(testDispatcher) {
            every { settings.shouldAutocompleteInAwesomebar } returns true
            every { settings.shouldShowHistorySuggestions } returns true
            every { settings.shouldShowBookmarkSuggestions } returns true
            every { settings.shouldShowVoiceSearch } returns true
            val appStore = AppStore()
            val middleware = spyk(buildMiddleware(appStore = appStore))
            every { middleware.isSpeechRecognitionAvailable() } returns true
            val engine: Engine = mockk {
                every { speculativeConnect(any()) } just Runs
            }
            every { components.core.engine } returns engine
            configureAutocompleteProvidersInComponents()
            val store = buildStore(middleware)
            val autocompleteProvidersSlot = slot<List<AutocompleteProvider>>()

            store.dispatch(
                SearchSelectorItemClicked(
                    fakeSearchState().applicationSearchEngines.first { it.id == TABS_SEARCH_ENGINE_ID }
                )
            )
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify(exactly = 0) {
                middleware.fetchAutocomplete(
                    autocompleteProviders = any(),
                    input = "",
                )
            }
            assertNull(store.state.editState.suggestion)
            assertEquals(1, store.state.editState.editActionsEnd.size)
            assertEquals(expectedVoiceSearchButton, store.state.editState.editActionsEnd.first())

            store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("test")))
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify {
                middleware.fetchAutocomplete(
                    autocompleteProviders = capture(autocompleteProvidersSlot),
                    input = "test",
                )
            }
            assertEquals(
                autocompleteProvidersSlot.captured.map { it.javaClass::getSimpleName },
                listOfNotNull(
                        components.core.sessionAutocompleteProvider,
                        components.backgroundServices.syncedTabsAutocompleteProvider,
                    )
                    .map { it.javaClass::getSimpleName },
            )
            assertEquals(store.state.editState.suggestion?.text, "session")
            verify { engine.speculativeConnect("session.com") }
            assertEquals(2, store.state.editState.editActionsEnd.size)
            assertEquals(expectedVoiceSearchButton, store.state.editState.editActionsEnd.first())
            assertEquals(expectedClearButton, store.state.editState.editActionsEnd.last())
        }

    @Test
    fun `GIVEN bookmarks engine selected WHEN entering in edit mode THEN set autocomplete suggestions`() =
        runTest(testDispatcher) {
            every { settings.shouldAutocompleteInAwesomebar } returns true
            every { settings.shouldShowHistorySuggestions } returns true
            every { settings.shouldShowBookmarkSuggestions } returns true
            every { settings.shouldShowVoiceSearch } returns true
            val appStore = AppStore()
            val middleware = spyk(buildMiddleware(appStore = appStore))
            every { middleware.isSpeechRecognitionAvailable() } returns true
            val engine: Engine = mockk {
                every { speculativeConnect(any()) } just Runs
            }
            every { components.core.engine } returns engine
            configureAutocompleteProvidersInComponents()
            val store = buildStore(middleware)
            val autocompleteProvidersSlot = slot<List<AutocompleteProvider>>()

            store.dispatch(
                SearchSelectorItemClicked(
                    fakeSearchState().applicationSearchEngines.first { it.id == BOOKMARKS_SEARCH_ENGINE_ID }
                )
            )
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify(exactly = 0) {
                middleware.fetchAutocomplete(
                    autocompleteProviders = any(),
                    input = "",
                )
            }
            assertNull(store.state.editState.suggestion)
            assertEquals(1, store.state.editState.editActionsEnd.size)
            assertEquals(expectedVoiceSearchButton, store.state.editState.editActionsEnd.first())

            store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("test")))
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify {
                middleware.fetchAutocomplete(
                    autocompleteProviders = capture(autocompleteProvidersSlot),
                    input = "test",
                )
            }
            assertEquals(
                autocompleteProvidersSlot.captured.map { it.javaClass::getSimpleName },
                listOfNotNull(components.core.bookmarksStorage).map { it.javaClass::getSimpleName },
            )
            assertEquals(store.state.editState.suggestion?.text, "bookmarks")
            verify { engine.speculativeConnect("bookmarks.com") }
            assertEquals(2, store.state.editState.editActionsEnd.size)
            assertEquals(expectedVoiceSearchButton, store.state.editState.editActionsEnd.first())
            assertEquals(expectedClearButton, store.state.editState.editActionsEnd.last())
        }

    @Test
    fun `GIVEN history engine selected WHEN entering in edit mode THEN set autocomplete suggestions`() =
        runTest(testDispatcher) {
            every { settings.shouldAutocompleteInAwesomebar } returns true
            every { settings.shouldShowHistorySuggestions } returns true
            every { settings.shouldShowBookmarkSuggestions } returns true
            every { settings.shouldShowVoiceSearch } returns true
            val appStore = AppStore()
            val middleware = spyk(buildMiddleware(appStore = appStore))
            every { middleware.isSpeechRecognitionAvailable() } returns true
            val engine: Engine = mockk {
                every { speculativeConnect(any()) } just Runs
            }
            every { components.core.engine } returns engine
            configureAutocompleteProvidersInComponents()
            val store = buildStore(middleware)
            val autocompleteProvidersSlot = slot<List<AutocompleteProvider>>()

            store.dispatch(
                SearchSelectorItemClicked(
                    fakeSearchState().applicationSearchEngines.first { it.id == HISTORY_SEARCH_ENGINE_ID }
                )
            )

            coVerify(exactly = 0) {
                middleware.fetchAutocomplete(
                    autocompleteProviders = any(),
                    input = "",
                )
            }
            assertNull(store.state.editState.suggestion)
            assertEquals(1, store.state.editState.editActionsEnd.size)
            assertEquals(expectedVoiceSearchButton, store.state.editState.editActionsEnd.first())

            store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("test")))
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify {
                middleware.fetchAutocomplete(
                    autocompleteProviders = capture(autocompleteProvidersSlot),
                    input = "test",
                )
            }
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                autocompleteProvidersSlot.captured.map { it.javaClass::getSimpleName },
                listOfNotNull(components.core.historyStorage).map { it.javaClass::getSimpleName },
            )
            assertEquals(store.state.editState.suggestion?.text, "history")
            verify { engine.speculativeConnect("history.com") }
            assertEquals(2, store.state.editState.editActionsEnd.size)
            assertEquals(expectedVoiceSearchButton, store.state.editState.editActionsEnd.first())
            assertEquals(expectedClearButton, store.state.editState.editActionsEnd.last())
        }

    @Test
    fun `GIVEN other search engine selected WHEN entering in edit mode THEN set autocomplete suggestions`() {
        every { settings.shouldAutocompleteInAwesomebar } returns true
        every { settings.shouldShowVoiceSearch } returns true
        val middleware = spyk(buildMiddleware(appStore = appStore))
        every { middleware.isSpeechRecognitionAvailable() } returns true
        val engine: Engine = mockk {
            every { speculativeConnect(any()) } just Runs
        }
        every { components.core.engine } returns engine
        configureAutocompleteProvidersInComponents()
        val store = buildStore(middleware)

        store.dispatch(SearchSelectorItemClicked(mockk(relaxed = true)))
        store.dispatch(EnterEditMode(false))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) {
            middleware.fetchAutocomplete(
                autocompleteProviders = any(),
                input = "",
            )
        }
        assertNull(store.state.editState.suggestion)
        verify(exactly = 0) { engine.speculativeConnect(any()) }
        assertEquals(1, store.state.editState.editActionsEnd.size)
        assertEquals(expectedVoiceSearchButton, store.state.editState.editActionsEnd.first())
    }

    @Test
    fun `WHEN the search engines are updated in BrowserStore THEN update the search selector and search providers`() {
        val browserStore = BrowserStore()
        val (_, store) = buildMiddlewareAndAddToStore(browserStore = browserStore)
        store.dispatch(EnterEditMode(false))
        val newSearchEngines = fakeSearchState().applicationSearchEngines

        browserStore.dispatch(ApplicationSearchEnginesLoaded(newSearchEngines))
        testDispatcher.scheduler.advanceUntilIdle() // wait for observing and processing the search engines update

        assertSearchSelectorEquals(
            expectedSearchSelector(newSearchEngines[0], newSearchEngines),
            store.state.editState.editActionsStart[0] as SearchSelectorAction,
        )
    }

    @Test
    fun `GIVEN a user typed search query WHEN the search engines are updated in BrowserStore THEN update the search selector, search providers and autocompletions`() {
        every { settings.shouldAutocompleteInAwesomebar } returns true
        every { settings.shouldShowHistorySuggestions } returns true
        every { settings.shouldShowBookmarkSuggestions } returns true
        every { settings.shouldShowVoiceSearch } returns true
        val engine: Engine = mockk {
            every { speculativeConnect(any()) } just Runs
        }
        every { components.core.engine } returns engine
        configureAutocompleteProvidersInComponents()
        val browserStore = BrowserStore()
        val middleware = spyk(buildMiddleware(browserStore = browserStore))
        val store = buildStore(middleware)
        val autocompleteProvidersSlots = mutableListOf<List<AutocompleteProvider>>()
        val newSearchEngines = fakeSearchState().applicationSearchEngines
        store.dispatch(EnterEditMode(false))
        store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("test")))

        browserStore.dispatch(ApplicationSearchEnginesLoaded(newSearchEngines))
        testDispatcher.scheduler.advanceUntilIdle() // wait for observing and processing the search engines update

        assertSearchSelectorEquals(
            expectedSearchSelector(newSearchEngines[0], newSearchEngines),
            store.state.editState.editActionsStart[0] as SearchSelectorAction,
        )
        coVerify {
            middleware.fetchAutocomplete(
                autocompleteProviders = capture(autocompleteProvidersSlots),
                input = "test",
            )
        }
        assertEquals(
            autocompleteProvidersSlots.last().map { it.javaClass::getSimpleName },
            listOfNotNull(
                    components.core.historyStorage,
                    components.core.bookmarksStorage,
                    components.core.domainsAutocompleteProvider,
                )
                .map { it.javaClass::getSimpleName },
        )
        assertEquals(store.state.editState.suggestion?.text, "history")
        verify { engine.speculativeConnect("history.com") }
    }

    @Test
    fun `GIVEN a search engine is already selected WHEN the search engines are updated in BrowserStore THEN don't change the selected search engine`() {
        val selectedSearchEngine = fakeSearchState().applicationSearchEngines.first().copy(id = "test")
        val appStore =
            AppStore(
                AppState(
                    searchState =
                        AppSearchState.EMPTY.copy(
                            selectedSearchEngine = SelectedSearchEngine(selectedSearchEngine, true)
                        )
                )
            )
        val browserStore = BrowserStore()
        val (_, store) = buildMiddlewareAndAddToStore(testContext, appStore, browserStore)
        store.dispatch(EnterEditMode(false))
        val newSearchEngines = fakeSearchState().applicationSearchEngines

        browserStore.dispatch(ApplicationSearchEnginesLoaded(newSearchEngines))
        testDispatcher.scheduler.advanceUntilIdle()

        assertSearchSelectorEquals(
            expectedSearchSelector(selectedSearchEngine, newSearchEngines),
            store.state.editState.editActionsStart[0] as SearchSelectorAction,
        )
    }

    @Test
    fun `WHEN the search engine is added by the application THEN do not load URL`() {
        val captorMiddleware = CaptureActionsMiddleware<AppState, AppAction>()
        val appStore = AppStore(middlewares = listOf(captorMiddleware))
        val browserStore =
            BrowserStore(
                BrowserState(search = fakeSearchState().copy(userSelectedSearchEngineId = TABS_SEARCH_ENGINE_ID))
            )

        val browserUseCases: FenixBrowserUseCases = mockk(relaxed = true)
        val components: Components =
            mockk(relaxed = true) {
                every { useCases.fenixBrowserUseCases } returns browserUseCases
            }
        val (_, store) =
            buildMiddlewareAndAddToStore(
                appStore = appStore,
                browserStore = browserStore,
                components = components,
            )

        store.dispatch(CommitUrl("test"))

        verify(exactly = 0) {
            browserUseCases.loadUrlOrSearch(
                searchTermOrURL = any(),
                newTab = any(),
                forceSearch = any(),
                private = any(),
                searchEngine = any(),
            )
        }
        captorMiddleware.assertNotDispatched(SearchEnded::class)
    }

    @Test
    fun `WHEN about crashes is searched THEN navigate to crash list fragment`() {
        val captorMiddleware = CaptureActionsMiddleware<AppState, AppAction>()
        val appStore = AppStore(middlewares = listOf(captorMiddleware))
        val (_, store) = buildMiddlewareAndAddToStore(appStore = appStore)

        store.dispatch(CommitUrl("about:crashes"))

        verify { navController.navigate(NavGraphDirections.actionGlobalCrashListFragment()) }
        captorMiddleware.assertLastAction(SearchEnded::class) {}
    }

    @Test
    fun `WHEN about addons is searched THEN navigate to addons management fragment`() {
        val captorMiddleware = CaptureActionsMiddleware<AppState, AppAction>()
        val appStore = AppStore(middlewares = listOf(captorMiddleware))
        val (_, store) = buildMiddlewareAndAddToStore(appStore = appStore)

        store.dispatch(CommitUrl("about:addons"))

        verify { navController.navigate(NavGraphDirections.actionGlobalAddonsManagementFragment()) }
        captureBrowserActionsMiddleware.assertFirstAction(EngagementFinished::class) { action ->
            assertFalse(action.abandoned)
        }
        captorMiddleware.assertLastAction(SearchEnded::class) {}
    }

    @Test
    fun `WHEN about glean is searched THEN navigate to glean debug tools fragment`() {
        val captorMiddleware = CaptureActionsMiddleware<AppState, AppAction>()
        val appStore = AppStore(middlewares = listOf(captorMiddleware))
        val (_, store) = buildMiddlewareAndAddToStore(appStore = appStore)

        store.dispatch(CommitUrl("about:glean"))

        verify { navController.navigate(NavGraphDirections.actionGlobalGleanDebugToolsFragment()) }
        captorMiddleware.assertLastAction(SearchEnded::class) {}
    }

    @Test
    fun `WHEN mozilla manifesto URL is searched THEN navigate to mozilla manifesto page`() {
        val manifestoUrl = SupportUtils.getMozillaPageUrl(SupportUtils.MozillaPage.MANIFESTO)
        val captorMiddleware = CaptureActionsMiddleware<AppState, AppAction>()
        val appStore = AppStore(middlewares = listOf(captorMiddleware))
        val browserUseCases: FenixBrowserUseCases = mockk(relaxed = true)
        val components: Components =
            mockk(relaxed = true) {
                every { useCases.fenixBrowserUseCases } returns browserUseCases
            }
        val browsingModeManager: BrowsingModeManager =
            mockk(relaxed = true) {
                every { mode } returns Normal
            }
        val (_, store) =
            buildMiddlewareAndAddToStore(
                appStore = appStore,
                components = components,
                browsingModeManager = browsingModeManager,
            )

        assertNull(Events.enteredUrl.testGetValue())

        store.dispatch(CommitUrl("moz://a"))

        verifyOrder {
            navController.navigate(NavGraphDirections.actionGlobalBrowser())
            browserUseCases.loadUrlOrSearch(
                searchTermOrURL = manifestoUrl,
                newTab = true,
                forceSearch = false,
                private = false,
                searchEngine = any(),
            )
        }
        assertNotNull(Events.enteredUrl.testGetValue())
        assertEquals(1, Events.enteredUrl.testGetValue()!!.size)
        assertEquals(
            "false",
            Events.enteredUrl.testGetValue()!!.single().extra?.getValue("autocomplete"),
        )
        captureBrowserActionsMiddleware.assertFirstAction(EngagementFinished::class) { action ->
            assertFalse(action.abandoned)
        }
        captorMiddleware.assertLastAction(SearchEnded::class) {}
    }

    @Test
    fun `WHEN empty text is searched THEN finish engagement as abandoned`() {
        val captorMiddleware = CaptureActionsMiddleware<AppState, AppAction>()
        val appStore = AppStore(middlewares = listOf(captorMiddleware))
        val (_, store) = buildMiddlewareAndAddToStore(appStore = appStore)

        store.dispatch(CommitUrl(""))

        captureBrowserActionsMiddleware.assertFirstAction(EngagementFinished::class) { action ->
            assertTrue(action.abandoned)
        }
        captorMiddleware.assertLastAction(SearchEnded::class) {}
    }

    @Test
    fun `GIVEN homepage as a new tab is enabled WHEN url is committed THEN perform search in the existing tab`() {
        val url = "https://www.mozilla.org"
        val captorMiddleware = CaptureActionsMiddleware<AppState, AppAction>()
        val appStore = AppStore(middlewares = listOf(captorMiddleware))
        val browserUseCases: FenixBrowserUseCases = mockk(relaxed = true)
        val components: Components =
            mockk(relaxed = true) {
                every { useCases.fenixBrowserUseCases } returns browserUseCases
            }
        val settings: Settings =
            mockk(relaxed = true) {
                every { enableHomepageAsNewTab } returns true
            }
        val browsingModeManager: BrowsingModeManager =
            mockk(relaxed = true) {
                every { mode } returns Normal
            }
        val (_, store) =
            buildMiddlewareAndAddToStore(
                appStore = appStore,
                components = components,
                settings = settings,
                browsingModeManager = browsingModeManager,
            )

        assertNull(Events.enteredUrl.testGetValue())

        store.dispatch(CommitUrl(url))

        verifyOrder {
            navController.navigate(NavGraphDirections.actionGlobalBrowser())
            browserUseCases.loadUrlOrSearch(
                searchTermOrURL = url,
                newTab = false,
                forceSearch = false,
                private = false,
                searchEngine = any(),
            )
        }
        assertNotNull(Events.enteredUrl.testGetValue())
        assertEquals(1, Events.enteredUrl.testGetValue()!!.size)
        assertEquals(
            "false",
            Events.enteredUrl.testGetValue()!!.single().extra?.getValue("autocomplete"),
        )
        captureBrowserActionsMiddleware.assertFirstAction(EngagementFinished::class) { action ->
            assertFalse(action.abandoned)
        }
        captorMiddleware.assertLastAction(SearchEnded::class) {}
    }

    @Test
    fun `GIVEN homepage as a new tab is enabled WHEN search term is committed THEN perform search in the existing tab`() {
        val searchTerm = "Firefox"
        val captorMiddleware = CaptureActionsMiddleware<AppState, AppAction>()
        val appStore = AppStore(middlewares = listOf(captorMiddleware))
        val browserUseCases: FenixBrowserUseCases = mockk(relaxed = true)
        val components: Components =
            mockk(relaxed = true) {
                every { useCases.fenixBrowserUseCases } returns browserUseCases
            }
        val settings: Settings =
            mockk(relaxed = true) {
                every { enableHomepageAsNewTab } returns true
            }
        val browsingModeManager: BrowsingModeManager =
            mockk(relaxed = true) {
                every { mode } returns Normal
            }
        val (_, store) =
            buildMiddlewareAndAddToStore(
                appStore = appStore,
                components = components,
                settings = settings,
                browsingModeManager = browsingModeManager,
            )

        store.dispatch(CommitUrl(searchTerm))

        verifyOrder {
            navController.navigate(NavGraphDirections.actionGlobalBrowser())
            browserUseCases.loadUrlOrSearch(
                searchTermOrURL = searchTerm,
                newTab = false,
                forceSearch = false,
                private = false,
                searchEngine = any(),
            )
        }
        captureBrowserActionsMiddleware.assertFirstAction(EngagementFinished::class) { action ->
            assertFalse(action.abandoned)
        }
        captorMiddleware.assertLastAction(SearchEnded::class) {}
    }

    @Test
    fun `GIVEN homepage as a new tab is disabled and the source tab is available WHEN search term is committed THEN perform search in the source tab`() {
        val searchTerm = "Firefox"
        val captorMiddleware = CaptureActionsMiddleware<AppState, AppAction>()
        val browserUseCases: FenixBrowserUseCases = mockk(relaxed = true)
        val components: Components =
            mockk(relaxed = true) {
                every { useCases.fenixBrowserUseCases } returns browserUseCases
            }
        val settings: Settings =
            mockk(relaxed = true) {
                every { enableHomepageAsNewTab } returns false
            }
        val browsingModeManager: BrowsingModeManager =
            mockk(relaxed = true) {
                every { mode } returns Normal
            }
        val appStore =
            AppStore(
                initialState = AppState(searchState = AppSearchState.EMPTY.copy(sourceTabId = "test")),
                middlewares = listOf(captorMiddleware),
            )
        val browserStore =
            BrowserStore(
                BrowserState(
                    tabs = listOf(createTab("https://test.com", id = "test")),
                    search = fakeSearchState(),
                ),
                middleware = listOf(captureBrowserActionsMiddleware),
            )
        val (_, store) =
            buildMiddlewareAndAddToStore(
                appStore = appStore,
                browserStore = browserStore,
                components = components,
                settings = settings,
                browsingModeManager = browsingModeManager,
            )

        store.dispatch(CommitUrl(searchTerm))

        verifyOrder {
            navController.navigate(NavGraphDirections.actionGlobalBrowser())
            browserUseCases.loadUrlOrSearch(
                searchTermOrURL = searchTerm,
                newTab = false,
                forceSearch = false,
                private = false,
                searchEngine = any(),
            )
        }
        captureBrowserActionsMiddleware.assertFirstAction(EngagementFinished::class) { action ->
            assertFalse(action.abandoned)
        }
        captorMiddleware.assertLastAction(SearchEnded::class) {}
    }

    @Test
    fun `GIVEN homepage as a new tab is disabled and the source tab is not available WHEN search term is committed THEN perform search in a new tab`() {
        val searchTerm = "Firefox"
        val captorMiddleware = CaptureActionsMiddleware<AppState, AppAction>()
        val browserUseCases: FenixBrowserUseCases = mockk(relaxed = true)
        val components: Components =
            mockk(relaxed = true) {
                every { useCases.fenixBrowserUseCases } returns browserUseCases
            }
        val settings: Settings =
            mockk(relaxed = true) {
                every { enableHomepageAsNewTab } returns false
            }
        val browsingModeManager: BrowsingModeManager =
            mockk(relaxed = true) {
                every { mode } returns Normal
            }
        val appStore =
            AppStore(
                initialState = AppState(searchState = AppSearchState.EMPTY.copy(sourceTabId = "test")),
                middlewares = listOf(captorMiddleware),
            )
        val (_, store) =
            buildMiddlewareAndAddToStore(
                appStore = appStore,
                components = components,
                settings = settings,
                browsingModeManager = browsingModeManager,
            )

        store.dispatch(CommitUrl(searchTerm))

        verifyOrder {
            navController.navigate(NavGraphDirections.actionGlobalBrowser())
            browserUseCases.loadUrlOrSearch(
                searchTermOrURL = searchTerm,
                newTab = true,
                forceSearch = false,
                private = false,
                searchEngine = any(),
            )
        }
        captureBrowserActionsMiddleware.assertFirstAction(EngagementFinished::class) { action ->
            assertFalse(action.abandoned)
        }
        captorMiddleware.assertLastAction(SearchEnded::class) {}
    }

    @Test
    fun `GIVEN the toolbar is in edit mode WHEN updateSearchActionsEnd is triggered via ToggleEditMode THEN a voice search action button is added to the end actions`() {
        every { settings.shouldShowVoiceSearch } returns true
        val middleware = spyk(buildMiddleware(appStore = appStore))
        every { middleware.isSpeechRecognitionAvailable() } returns true
        val store = buildStore(middleware)
        store.dispatch(EnterEditMode(false))

        val actions = store.state.editState.editActionsEnd
        assertEquals(2, actions.size)
        val voiceAction = actions.first() as ActionButtonRes
        assertEquals(expectedVoiceSearchButton, voiceAction)
    }

    @Test
    fun `GIVEN the toolbar is in edit mode but speech recognition is not available THEN don't show a voice search action button`() {
        val middleware = spyk(buildMiddleware(appStore = appStore))
        every { middleware.isSpeechRecognitionAvailable() } returns false
        val store = buildStore(middleware)
        store.dispatch(EnterEditMode(false))
        store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("")))

        val actions = store.state.editState.editActionsEnd
        assertTrue(actions.size == 1)
        assertEquals(expectedQrButton, actions.last())
    }

    @Test
    fun `GIVEN QR scan while in normal browsing mode WHEN receiving a result THEN open it as a new normal tab`() {
        val appStoreActionsCaptor = CaptureActionsMiddleware<AppState, AppAction>()
        val appStore = AppStore(middlewares = listOf(appStoreActionsCaptor))
        val browserUseCases: FenixBrowserUseCases = mockk(relaxed = true)
        every { components.useCases.fenixBrowserUseCases } returns browserUseCases
        val browsingModeManager: BrowsingModeManager =
            mockk(relaxed = true) {
                every { mode } returns Normal
            }
        val (_, store) =
            buildMiddlewareAndAddToStore(
                appStore = appStore,
                components = components,
                browsingModeManager = browsingModeManager,
            )
        store.dispatch(EnterEditMode(false))
        val qrScannerButton = store.state.editState.editActionsEnd.last() as ActionButtonRes

        store.dispatch(qrScannerButton.onClick as BrowserToolbarEvent)
        appStore.dispatch(QrScannerInputAvailable("mozilla.test"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("mozilla.test", store.state.editState.query.current)
        assertTrue(store.state.editState.isQueryPrefilled)
        appStoreActionsCaptor.assertLastAction(QrScannerInputConsumed::class)
        verify {
            browserUseCases.loadUrlOrSearch(
                searchTermOrURL = "mozilla.test",
                newTab = true,
                flags = EngineSession.LoadUrlFlags.external(),
                private = false,
            )
        }
        verify { navController.navigate(R.id.action_global_browser) }
    }

    @Test
    fun `GIVEN QR scan while in private browsing mode WHEN receiving a result THEN open it as a new private tab`() {
        val appStoreActionsCaptor = CaptureActionsMiddleware<AppState, AppAction>()
        val appStore = AppStore(middlewares = listOf(appStoreActionsCaptor))
        val browserUseCases: FenixBrowserUseCases = mockk(relaxed = true)
        every { components.useCases.fenixBrowserUseCases } returns browserUseCases
        val browsingModeManager: BrowsingModeManager =
            mockk(relaxed = true) {
                every { mode } returns Private
            }
        val (_, store) =
            buildMiddlewareAndAddToStore(
                appStore = appStore,
                components = components,
                browsingModeManager = browsingModeManager,
            )
        store.dispatch(EnterEditMode(true))
        val qrScannerButton = store.state.editState.editActionsEnd.last() as ActionButtonRes

        store.dispatch(qrScannerButton.onClick as BrowserToolbarEvent)
        appStore.dispatch(QrScannerInputAvailable("test.mozilla"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("test.mozilla", store.state.editState.query.current)
        assertTrue(store.state.editState.isQueryPrefilled)
        appStoreActionsCaptor.assertLastAction(QrScannerInputConsumed::class)
        verify {
            browserUseCases.loadUrlOrSearch(
                searchTermOrURL = "test.mozilla",
                newTab = true,
                flags = EngineSession.LoadUrlFlags.external(),
                private = true,
            )
        }
        verify { navController.navigate(R.id.action_global_browser) }
    }

    @Test
    fun `GIVEN QR scan started from browser WHEN receiving a result THEN open it in the same tab`() {
        val appStoreActionsCaptor = CaptureActionsMiddleware<AppState, AppAction>()
        val appStore =
            AppStore(
                initialState = AppState(searchState = AppSearchState.EMPTY.copy(sourceTabId = "test")),
                middlewares = listOf(appStoreActionsCaptor),
            )
        val browserUseCases: FenixBrowserUseCases = mockk(relaxed = true)
        every { components.useCases.fenixBrowserUseCases } returns browserUseCases
        val settings: Settings =
            mockk(relaxed = true) {
                every { enableHomepageAsNewTab } returns false
            }
        val browsingModeManager: BrowsingModeManager =
            mockk(relaxed = true) {
                every { mode } returns Normal
            }
        val (_, store) =
            buildMiddlewareAndAddToStore(
                appStore = appStore,
                components = components,
                settings = settings,
                browsingModeManager = browsingModeManager,
            )
        store.dispatch(EnterEditMode(false))
        val qrScannerButton = store.state.editState.editActionsEnd.last() as ActionButtonRes

        store.dispatch(qrScannerButton.onClick as BrowserToolbarEvent)
        appStore.dispatch(QrScannerInputAvailable("test.com"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("test.com", store.state.editState.query.current)
        assertTrue(store.state.editState.isQueryPrefilled)
        appStoreActionsCaptor.assertLastAction(QrScannerInputConsumed::class)
        verify {
            browserUseCases.loadUrlOrSearch(
                searchTermOrURL = "test.com",
                newTab = false,
                flags = EngineSession.LoadUrlFlags.external(),
                private = false,
            )
        }
        verify { navController.navigate(R.id.action_global_browser) }
    }

    @Test
    fun `GIVEN QR scan did not start from browser WHEN receiving a result THEN open it in a new tab`() {
        val appStoreActionsCaptor = CaptureActionsMiddleware<AppState, AppAction>()
        val appStore =
            AppStore(
                initialState = AppState(searchState = AppSearchState.EMPTY),
                middlewares = listOf(appStoreActionsCaptor),
            )
        val browserUseCases: FenixBrowserUseCases = mockk(relaxed = true)
        every { components.useCases.fenixBrowserUseCases } returns browserUseCases
        val settings: Settings =
            mockk(relaxed = true) {
                every { enableHomepageAsNewTab } returns false
            }
        val browsingModeManager: BrowsingModeManager =
            mockk(relaxed = true) {
                every { mode } returns Normal
            }
        val (_, store) =
            buildMiddlewareAndAddToStore(
                appStore = appStore,
                components = components,
                settings = settings,
                browsingModeManager = browsingModeManager,
            )
        store.dispatch(EnterEditMode(false))
        val qrScannerButton = store.state.editState.editActionsEnd.last() as ActionButtonRes

        store.dispatch(qrScannerButton.onClick as BrowserToolbarEvent)
        appStore.dispatch(QrScannerInputAvailable("test.com"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("test.com", store.state.editState.query.current)
        assertTrue(store.state.editState.isQueryPrefilled)
        appStoreActionsCaptor.assertLastAction(QrScannerInputConsumed::class)
        verify {
            browserUseCases.loadUrlOrSearch(
                searchTermOrURL = "test.com",
                newTab = true,
                flags = EngineSession.LoadUrlFlags.external(),
                private = false,
            )
        }
        verify { navController.navigate(R.id.action_global_browser) }
    }

    @Test
    fun `GIVEN homepage as a new tab is enabled and QR scan started from browser WHEN receiving a QR scan result THEN open it in the current tab`() {
        val appStoreActionsCaptor = CaptureActionsMiddleware<AppState, AppAction>()
        val appStore =
            AppStore(
                initialState = AppState(searchState = AppSearchState.EMPTY.copy(sourceTabId = "test")),
                middlewares = listOf(appStoreActionsCaptor),
            )
        val browserUseCases: FenixBrowserUseCases = mockk(relaxed = true)
        every { components.useCases.fenixBrowserUseCases } returns browserUseCases
        val settings: Settings =
            mockk(relaxed = true) {
                every { enableHomepageAsNewTab } returns true
            }
        val browsingModeManager: BrowsingModeManager =
            mockk(relaxed = true) {
                every { mode } returns Normal
            }
        val (_, store) =
            buildMiddlewareAndAddToStore(
                appStore = appStore,
                components = components,
                settings = settings,
                browsingModeManager = browsingModeManager,
            )
        store.dispatch(EnterEditMode(false))
        val qrScannerButton = store.state.editState.editActionsEnd.last() as ActionButtonRes

        store.dispatch(qrScannerButton.onClick as BrowserToolbarEvent)
        appStore.dispatch(QrScannerInputAvailable("test.com"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("test.com", store.state.editState.query.current)
        assertTrue(store.state.editState.isQueryPrefilled)
        appStoreActionsCaptor.assertLastAction(QrScannerInputConsumed::class)
        verify {
            browserUseCases.loadUrlOrSearch(
                searchTermOrURL = "test.com",
                newTab = false,
                flags = EngineSession.LoadUrlFlags.external(),
                private = false,
            )
        }
        verify { navController.navigate(R.id.action_global_browser) }
    }

    @Test
    fun `GIVEN homepage as a new tab is enabled and QR scan did not start from browser WHEN receiving a QR scan result THEN open it in the current tab`() {
        val appStoreActionsCaptor = CaptureActionsMiddleware<AppState, AppAction>()
        val appStore =
            AppStore(
                initialState = AppState(searchState = AppSearchState.EMPTY),
                middlewares = listOf(appStoreActionsCaptor),
            )
        val browserUseCases: FenixBrowserUseCases = mockk(relaxed = true)
        every { components.useCases.fenixBrowserUseCases } returns browserUseCases
        val settings: Settings =
            mockk(relaxed = true) {
                every { enableHomepageAsNewTab } returns true
            }
        val browsingModeManager: BrowsingModeManager =
            mockk(relaxed = true) {
                every { mode } returns Normal
            }
        val (_, store) =
            buildMiddlewareAndAddToStore(
                appStore = appStore,
                components = components,
                settings = settings,
                browsingModeManager = browsingModeManager,
            )
        store.dispatch(EnterEditMode(false))
        val qrScannerButton = store.state.editState.editActionsEnd.last() as ActionButtonRes

        store.dispatch(qrScannerButton.onClick as BrowserToolbarEvent)
        appStore.dispatch(QrScannerInputAvailable("test.com"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("test.com", store.state.editState.query.current)
        assertTrue(store.state.editState.isQueryPrefilled)
        appStoreActionsCaptor.assertLastAction(QrScannerInputConsumed::class)
        verify {
            browserUseCases.loadUrlOrSearch(
                searchTermOrURL = "test.com",
                newTab = false,
                flags = EngineSession.LoadUrlFlags.external(),
                private = false,
            )
        }
        verify { navController.navigate(R.id.action_global_browser) }
    }

    @Test
    fun `GIVEN Google search engine and Lens enabled WHEN toolbar enters edit mode with blank query THEN a Lens button is shown`() {
        every { settings.googleLensIntegrationEnabled } returns true
        every { settings.googleLensIntegrationUserEnabled } returns true
        val appStore: AppStore =
            mockk(relaxed = true) {
                every { state.searchState.selectedSearchEngine?.searchEngine } returns googleSearchEngine()
            }
        val (_, store) = buildMiddlewareAndAddToStore(appStore = appStore)

        store.dispatch(EnterEditMode(false))
        store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("")))

        val actions = store.state.editState.editActionsEnd
        val lensButton = actions.filterIsInstance<ActionButtonRes>().find { it.onClick == LensButtonClicked }
        assertEquals(expectedLensButton, lensButton)
    }

    @Test
    fun `GIVEN Google search engine, Lens enabled and QR as the last camera mode WHEN toolbar enters edit mode with blank query THEN a QR button opening the Lens camera is shown`() {
        every { settings.googleLensIntegrationEnabled } returns true
        every { settings.googleLensIntegrationUserEnabled } returns true
        every { settings.lensCameraLastMode } returns CameraMode.QR
        val appStore: AppStore =
            mockk(relaxed = true) {
                every { state.searchState.selectedSearchEngine?.searchEngine } returns googleSearchEngine()
            }
        val (_, store) = buildMiddlewareAndAddToStore(appStore = appStore)

        store.dispatch(EnterEditMode(false))
        store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("")))

        val actions = store.state.editState.editActionsEnd.filterIsInstance<ActionButtonRes>()
        assertEquals(expectedLensQrButton, actions.find { it.onClick == LensButtonClicked })
        assertNull(actions.find { it.onClick == QrScannerClicked })
    }

    @Test
    fun `GIVEN non-Google search engine WHEN toolbar enters edit mode THEN no Lens button is shown`() {
        every { settings.googleLensIntegrationEnabled } returns true
        val (_, store) = buildMiddlewareAndAddToStore()

        store.dispatch(EnterEditMode(false))
        store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("")))

        val actions = store.state.editState.editActionsEnd
        val lensButton = actions.filterIsInstance<ActionButtonRes>().find { it.onClick == LensButtonClicked }
        assertEquals(null, lensButton)
    }

    @Test
    fun `GIVEN Lens disabled WHEN toolbar enters edit mode with Google engine THEN no Lens button is shown`() {
        every { settings.googleLensIntegrationEnabled } returns false
        val appStore: AppStore =
            mockk(relaxed = true) {
                every { state.searchState.selectedSearchEngine?.searchEngine } returns googleSearchEngine()
            }
        val (_, store) = buildMiddlewareAndAddToStore(appStore = appStore)

        store.dispatch(EnterEditMode(false))
        store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("")))

        val actions = store.state.editState.editActionsEnd
        val lensButton = actions.filterIsInstance<ActionButtonRes>().find { it.onClick == LensButtonClicked }
        assertEquals(null, lensButton)
    }

    @Test
    fun `GIVEN Lens enabled but user disabled WHEN toolbar enters edit mode with Google engine THEN no Lens button is shown`() {
        every { settings.googleLensIntegrationEnabled } returns true
        every { settings.googleLensIntegrationUserEnabled } returns false
        val appStore: AppStore =
            mockk(relaxed = true) {
                every { state.searchState.selectedSearchEngine?.searchEngine } returns googleSearchEngine()
            }
        val (_, store) = buildMiddlewareAndAddToStore(appStore = appStore)

        store.dispatch(EnterEditMode(false))
        store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("")))

        val actions = store.state.editState.editActionsEnd
        val lensButton = actions.filterIsInstance<ActionButtonRes>().find { it.onClick == LensButtonClicked }
        assertEquals(null, lensButton)
    }

    @Test
    fun `GIVEN Google search engine and Lens enabled WHEN in private mode THEN the QR scanner button is shown instead of the Lens button`() {
        every { settings.googleLensIntegrationEnabled } returns true
        every { settings.googleLensIntegrationUserEnabled } returns true
        val browsingModeManager: BrowsingModeManager = mockk {
            every { mode } returns Private
        }
        val appStore: AppStore =
            mockk(relaxed = true) {
                every { state.searchState.selectedSearchEngine?.searchEngine } returns googleSearchEngine()
            }
        val (_, store) =
            buildMiddlewareAndAddToStore(
                appStore = appStore,
                browsingModeManager = browsingModeManager,
            )

        store.dispatch(EnterEditMode(false))
        store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("")))

        val actions = store.state.editState.editActionsEnd.filterIsInstance<ActionButtonRes>()
        assertEquals(null, actions.find { it.onClick == LensButtonClicked })
        assertEquals(expectedQrButton, actions.find { it.onClick == QrScannerClicked })
    }

    @Test
    fun `WHEN the Lens button is clicked THEN dispatch LensRequested and record telemetry`() {
        every { settings.googleLensIntegrationEnabled } returns true
        every { settings.googleLensIntegrationUserEnabled } returns true
        val appStore: AppStore =
            mockk(relaxed = true) {
                every { state.searchState } returns
                    AppSearchState.EMPTY.copy(
                        selectedSearchEngine =
                            SelectedSearchEngine(
                                searchEngine = googleSearchEngine(),
                                isUserSelected = true,
                            )
                    )
            }
        val (_, store) = buildMiddlewareAndAddToStore(appStore = appStore)
        store.dispatch(EnterEditMode(false))
        store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("")))

        val lensButton =
            store.state.editState.editActionsEnd.filterIsInstance<ActionButtonRes>().find {
                it.onClick == LensButtonClicked
            }!!

        store.dispatch(lensButton.onClick as BrowserToolbarEvent)
        assertTelemetryRecorded(ACTION_LENS_CLICKED)
        assertNotNull(ToolbarGoogleLensButton.tapped.testGetValue())
        verify { appStore.dispatch(LensRequested) }
    }

    @Test
    fun `GIVEN QR as the last camera mode WHEN the Lens button is clicked THEN dispatch LensRequested and don't record Lens telemetry`() {
        every { settings.googleLensIntegrationEnabled } returns true
        every { settings.googleLensIntegrationUserEnabled } returns true
        every { settings.lensCameraLastMode } returns CameraMode.QR
        val appStore: AppStore =
            mockk(relaxed = true) {
                every { state.searchState } returns
                    AppSearchState.EMPTY.copy(
                        selectedSearchEngine =
                            SelectedSearchEngine(
                                searchEngine = googleSearchEngine(),
                                isUserSelected = true,
                            )
                    )
            }
        val (_, store) = buildMiddlewareAndAddToStore(appStore = appStore)
        store.dispatch(EnterEditMode(false))
        store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("")))

        val lensButton =
            store.state.editState.editActionsEnd.filterIsInstance<ActionButtonRes>().find {
                it.onClick == LensButtonClicked
            }!!

        store.dispatch(lensButton.onClick as BrowserToolbarEvent)
        assertNull(Toolbar.buttonTapped.testGetValue())
        assertNull(ToolbarGoogleLensButton.tapped.testGetValue())
        verify { appStore.dispatch(LensRequested) }
    }

    @Test
    fun `GIVEN the Lens camera was opened WHEN it is dismissed after switching to QR mode THEN the Lens button shows the QR icon`() {
        val appStore =
            AppStore(
                initialState =
                    AppState(
                        searchState =
                            AppSearchState.EMPTY.copy(
                                selectedSearchEngine =
                                    SelectedSearchEngine(
                                        searchEngine = googleSearchEngine(),
                                        isUserSelected = false,
                                    )
                            )
                    )
            )
        every { settings.googleLensIntegrationEnabled } returns true
        every { settings.googleLensIntegrationUserEnabled } returns true
        val (_, store) = buildMiddlewareAndAddToStore(appStore = appStore, components = components)
        store.dispatch(EnterEditMode(false))
        store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("")))
        val lensButton =
            store.state.editState.editActionsEnd.filterIsInstance<ActionButtonRes>().find {
                it.onClick == LensButtonClicked
            }!!
        assertEquals(expectedLensButton, lensButton)

        store.dispatch(lensButton.onClick as BrowserToolbarEvent)
        appStore.dispatch(LensRequestConsumed)
        testDispatcher.scheduler.advanceUntilIdle()
        every { settings.lensCameraLastMode } returns CameraMode.QR
        appStore.dispatch(LensDismissed)
        testDispatcher.scheduler.advanceUntilIdle()

        val actions = store.state.editState.editActionsEnd.filterIsInstance<ActionButtonRes>()
        assertEquals(expectedLensQrButton, actions.find { it.onClick == LensButtonClicked })
    }

    @Test
    fun `GIVEN the Lens button was clicked WHEN a QR scanner result arrives THEN the URL bar is populated`() {
        val appStoreActionsCaptor = CaptureActionsMiddleware<AppState, AppAction>()
        val appStore =
            AppStore(
                initialState =
                    AppState(
                        searchState =
                            AppSearchState.EMPTY.copy(
                                selectedSearchEngine =
                                    SelectedSearchEngine(
                                        searchEngine = googleSearchEngine(),
                                        isUserSelected = false,
                                    )
                            )
                    ),
                middlewares = listOf(appStoreActionsCaptor),
            )
        val browserUseCases: FenixBrowserUseCases = mockk(relaxed = true)
        every { components.useCases.fenixBrowserUseCases } returns browserUseCases
        every { settings.googleLensIntegrationEnabled } returns true
        every { settings.googleLensIntegrationUserEnabled } returns true
        val browsingModeManager: BrowsingModeManager =
            mockk(relaxed = true) {
                every { mode } returns Normal
            }
        val (_, store) =
            buildMiddlewareAndAddToStore(
                appStore = appStore,
                components = components,
                browsingModeManager = browsingModeManager,
            )
        store.dispatch(EnterEditMode(false))
        store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("")))

        val lensButton =
            store.state.editState.editActionsEnd.filterIsInstance<ActionButtonRes>().find {
                it.onClick == LensButtonClicked
            }!!

        store.dispatch(lensButton.onClick as BrowserToolbarEvent)
        appStore.dispatch(QrScannerInputAvailable("scanned.example"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("scanned.example", store.state.editState.query.current)
        assertTrue(store.state.editState.isQueryPrefilled)
        appStoreActionsCaptor.assertLastAction(QrScannerInputConsumed::class)
    }

    @Test
    fun `GIVEN the Lens button was clicked WHEN a Lens result becomes available THEN it navigates to the browser without loading the url`() {
        val appStore =
            AppStore(
                initialState =
                    AppState(
                        searchState =
                            AppSearchState.EMPTY.copy(
                                selectedSearchEngine =
                                    SelectedSearchEngine(
                                        searchEngine = googleSearchEngine(),
                                        isUserSelected = false,
                                    )
                            )
                    )
            )
        val browserUseCases: FenixBrowserUseCases = mockk(relaxed = true)
        every { components.useCases.fenixBrowserUseCases } returns browserUseCases
        every { settings.googleLensIntegrationEnabled } returns true
        every { settings.googleLensIntegrationUserEnabled } returns true
        val browsingModeManager: BrowsingModeManager =
            mockk(relaxed = true) {
                every { mode } returns Normal
            }
        val (_, store) =
            buildMiddlewareAndAddToStore(
                appStore = appStore,
                components = components,
                browsingModeManager = browsingModeManager,
            )
        store.dispatch(EnterEditMode(false))
        store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("")))

        val lensButton =
            store.state.editState.editActionsEnd.filterIsInstance<ActionButtonRes>().find {
                it.onClick == LensButtonClicked
            }!!

        store.dispatch(lensButton.onClick as BrowserToolbarEvent)
        appStore.dispatch(LensResultAvailable("https://lens.google.com/results"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Opening the tab is LensImageSearch's job now, so the middleware only navigates.
        verify(exactly = 0) {
            browserUseCases.loadUrlOrSearch(
                searchTermOrURL = any(),
                newTab = any(),
                private = any(),
                flags = any(),
            )
        }
        verify { navController.navigate(R.id.action_global_browser) }
    }

    @Test
    fun `GIVEN the Lens button was clicked WHEN edit mode is exited THEN LensDismissed is not dispatched`() {
        val appStoreActionsCaptor = CaptureActionsMiddleware<AppState, AppAction>()
        val appStore =
            AppStore(
                initialState =
                    AppState(
                        searchState =
                            AppSearchState.EMPTY.copy(
                                selectedSearchEngine =
                                    SelectedSearchEngine(
                                        searchEngine = googleSearchEngine(),
                                        isUserSelected = false,
                                    )
                            )
                    ),
                middlewares = listOf(appStoreActionsCaptor),
            )
        every { settings.googleLensIntegrationEnabled } returns true
        every { settings.googleLensIntegrationUserEnabled } returns true
        val (_, store) = buildMiddlewareAndAddToStore(appStore = appStore, components = components)
        store.dispatch(EnterEditMode(false))
        store.dispatch(SearchQueryUpdated(BrowserToolbarQuery("")))
        val lensButton =
            store.state.editState.editActionsEnd.filterIsInstance<ActionButtonRes>().find {
                it.onClick == LensButtonClicked
            }!!
        store.dispatch(lensButton.onClick as BrowserToolbarEvent)

        store.dispatch(ExitEditMode)
        testDispatcher.scheduler.advanceUntilIdle()

        appStoreActionsCaptor.assertNotDispatched(LensDismissed::class)
    }

    @Test
    fun `WHEN the voice action is tapped THEN add a new voice input request to the AppStore`() {
        val appStore: AppStore =
            mockk(relaxed = true) {
                every { state } returns mockk(relaxed = true)
            }
        every { settings.shouldShowVoiceSearch } returns true
        val middleware = spyk(buildMiddleware(appStore = appStore))
        every { middleware.isSpeechRecognitionAvailable() } returns true
        val store = buildStore(middleware)
        store.dispatch(EnterEditMode(false))
        val voiceAction = store.state.editState.editActionsEnd.first() as ActionButtonRes

        store.dispatch(voiceAction.onClick as BrowserToolbarEvent)

        verify { appStore.dispatch(VoiceInputRequested) }
    }

    private fun expectedSearchSelector(
        defaultOrSelectedSearchEngine: SearchEngine = fakeSearchState().selectedOrDefaultSearchEngine!!,
        searchEngineShortcuts: List<SearchEngine> = fakeSearchState().searchEngineShortcuts,
    ) =
        buildExpectedSearchSelector(
            defaultOrSelectedSearchEngine,
            searchEngineShortcuts,
            testContext.resources,
        )

    private val expectedClearButton =
        ActionButtonRes(
            drawableResId = iconsR.drawable.mozac_ic_cross_circle_fill_24,
            contentDescription = toolbarR.string.mozac_clear_button_description,
            state = ActionButton.State.DEFAULT,
            onClick = ClearSearchClicked,
        )

    private val expectedQrButton =
        ActionButtonRes(
            drawableResId = iconsR.drawable.mozac_ic_qr_code_24,
            contentDescription = qrR.string.mozac_feature_qr_scanner,
            state = ActionButton.State.DEFAULT,
            onClick = QrScannerClicked,
        )

    private val expectedLensButton =
        ActionButtonRes(
            drawableResId = iconsR.drawable.mozac_ic_logo_google_lens_24,
            contentDescription = R.string.lens_search_content_description,
            state = ActionButton.State.DEFAULT,
            onClick = LensButtonClicked,
        )

    private val expectedLensQrButton =
        ActionButtonRes(
            drawableResId = iconsR.drawable.mozac_ic_qr_code_24,
            contentDescription = qrR.string.mozac_feature_qr_scanner,
            state = ActionButton.State.DEFAULT,
            onClick = LensButtonClicked,
        )

    private val expectedVoiceSearchButton =
        ActionButtonRes(
            drawableResId = iconsR.drawable.mozac_ic_microphone_24,
            contentDescription = R.string.voice_search_content_description,
            onClick = VoiceSearchButtonClicked,
        )

    private fun buildMiddlewareAndAddToStore(
        uiContext: Context = testContext,
        appStore: AppStore = this.appStore,
        browserStore: BrowserStore = this.browserStore,
        components: Components = this.components,
        navController: NavController = this.navController,
        browsingModeManager: BrowsingModeManager = this.browsingModeManager,
        settings: Settings = this.settings,
        scope: CoroutineScope = testScope,
    ): Pair<BrowserToolbarSearchMiddleware, BrowserToolbarStore> {
        val middleware =
            buildMiddleware(
                uiContext = uiContext,
                appStore = appStore,
                browserStore = browserStore,
                components = components,
                navController = navController,
                browsingModeManager = browsingModeManager,
                settings = settings,
                scope = scope,
            )
        val store = buildStore(middleware)

        return middleware to store
    }

    private fun buildStore(middleware: BrowserToolbarSearchMiddleware = buildMiddleware()) =
        BrowserToolbarStore(middleware = listOf(middleware))

    private fun buildMiddleware(
        uiContext: Context = testContext,
        appStore: AppStore = this.appStore,
        browserStore: BrowserStore = this.browserStore,
        components: Components = this.components,
        navController: NavController = this.navController,
        browsingModeManager: BrowsingModeManager = this.browsingModeManager,
        settings: Settings = this.settings,
        scope: CoroutineScope = testScope,
    ) =
        BrowserToolbarSearchMiddleware(
            uiContext = uiContext,
            appStore = appStore,
            browserStore = browserStore,
            components = components,
            navController = navController,
            browsingModeManager = browsingModeManager,
            settings = settings,
            scope = scope,
            autocompleteDispatcher = testDispatcher,
        )

    private fun configureAutocompleteProvidersInComponents() {
        val autocompleteSuggestion =
            AutocompleteResult(
                text = "",
                url = "",
                input = "",
                source = "t",
                totalItems = 1,
            )
        val historyStorage: PlacesHistoryStorage = mockk {
            coEvery { getAutocompleteSuggestion(any()) } returns
                autocompleteSuggestion.copy(
                    text = "history",
                    url = "history.com",
                )
        }
        val bookmarksStorage: PlacesBookmarksStorage = mockk {
            coEvery { getAutocompleteSuggestion(any()) } returns
                autocompleteSuggestion.copy(
                    text = "bookmarks",
                    url = "bookmarks.com",
                )
        }
        val domainsProvider: BaseDomainAutocompleteProvider = mockk {
            coEvery { getAutocompleteSuggestion(any()) } returns
                autocompleteSuggestion.copy(
                    text = "domains",
                    url = "domains.com",
                )
        }
        val sessionsProvider: SessionAutocompleteProvider = mockk {
            coEvery { getAutocompleteSuggestion(any()) } returns
                autocompleteSuggestion.copy(
                    text = "session",
                    url = "session.com",
                )
        }
        val syncedTabsProvider: SyncedTabsAutocompleteProvider = mockk {
            coEvery { getAutocompleteSuggestion(any()) } returns
                autocompleteSuggestion.copy(
                    text = "synced tabs",
                    url = "synced-tabs.com",
                )
        }

        every { components.core.historyStorage } returns historyStorage
        every { components.core.bookmarksStorage } returns bookmarksStorage
        every { components.core.domainsAutocompleteProvider } returns domainsProvider
        every { components.core.sessionAutocompleteProvider } returns sessionsProvider
        every { components.backgroundServices.syncedTabsAutocompleteProvider } returns syncedTabsProvider
    }

    private fun fakeSearchState() =
        SearchState(
            region = RegionState("US", "US"),
            regionSearchEngines =
                listOf(
                    SearchEngine("engine-a", "Engine A", mockk(relaxed = true), type = SearchEngine.Type.BUNDLED),
                    SearchEngine("engine-b", "Engine B", mockk(relaxed = true), type = SearchEngine.Type.BUNDLED),
                ),
            customSearchEngines =
                listOf(SearchEngine("engine-c", "Engine C", mockk(relaxed = true), type = SearchEngine.Type.CUSTOM)),
            applicationSearchEngines =
                listOf(
                    SearchEngine(
                        TABS_SEARCH_ENGINE_ID,
                        "Tabs",
                        mockk(relaxed = true),
                        type = SearchEngine.Type.APPLICATION,
                    ),
                    SearchEngine(
                        BOOKMARKS_SEARCH_ENGINE_ID,
                        "Bookmarks",
                        mockk(relaxed = true),
                        type = SearchEngine.Type.APPLICATION,
                    ),
                    SearchEngine(
                        HISTORY_SEARCH_ENGINE_ID,
                        "History",
                        mockk(relaxed = true),
                        type = SearchEngine.Type.APPLICATION,
                    ),
                ),
            additionalSearchEngines =
                listOf(
                    SearchEngine(
                        "engine-e",
                        "Engine E",
                        mockk(relaxed = true),
                        type = SearchEngine.Type.BUNDLED_ADDITIONAL,
                    )
                ),
            additionalAvailableSearchEngines =
                listOf(
                    SearchEngine(
                        "engine-f",
                        "Engine F",
                        mockk(relaxed = true),
                        type = SearchEngine.Type.BUNDLED_ADDITIONAL,
                    )
                ),
            hiddenSearchEngines =
                listOf(SearchEngine("engine-g", "Engine G", mockk(relaxed = true), type = SearchEngine.Type.BUNDLED)),
            regionDefaultSearchEngineId = null,
            userSelectedSearchEngineId = "engine-c",
            userSelectedSearchEngineName = null,
        )

    private fun googleSearchEngine() =
        SearchEngine(
            id = "google",
            name = "Google",
            icon = mockk(relaxed = true),
            type = SearchEngine.Type.BUNDLED,
            isGeneral = true,
        )

    private fun assertTelemetryRecorded(item: String, surface: String = SURFACE_HOME) {
        val values = Toolbar.buttonTapped.testGetValue()
        assertNotNull(values)
        val last = values.last()
        assertEquals(item, last.extra?.get("item"))
        assertEquals(SOURCE_ADDRESS_BAR, last.extra?.get("source"))
        assertEquals(surface, last.extra?.get("surface"))
    }
}
