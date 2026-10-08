/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.search

import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mozilla.components.browser.state.action.ContentAction.UpdateSearchTermsAction
import mozilla.components.browser.state.action.SearchAction.ApplicationSearchEnginesLoaded
import mozilla.components.browser.state.action.TabListAction.RemoveTabAction
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.SearchState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.compose.browser.awesomebar.internal.CurrentTabData
import mozilla.components.support.test.middleware.CaptureActionsMiddleware
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction.SearchAction.SearchEnded
import org.mozilla.fenix.components.appstate.AppAction.SearchAction.SearchStarted
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.components.appstate.search.SearchState as AppSearchState
import org.mozilla.fenix.search.fixtures.EMPTY_SEARCH_FRAGMENT_STATE
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrowserStoreToFenixSearchMapperMiddlewareTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `GIVEN a store initialized without a tab WHEN the search source changes THEN synchronize current tab data without changing the query`() =
        runTest(UnconfinedTestDispatcher()) {
            val firstTab = createTab("https://example.com", title = "First page", searchTerms = "first search")
            val secondTab = createTab("https://mozilla.org", title = "Second page", searchTerms = "second search")
            val browserStore = BrowserStore(BrowserState(tabs = listOf(firstTab, secondTab)))
            val appStore = AppStore()
            val middleware = BrowserStoreToFenixSearchMapperMiddleware(browserStore, backgroundScope, appStore)
            val searchStore = buildStore(middleware)

            assertNull(searchStore.state.currentTabData)

            appStore.dispatch(SearchStarted(firstTab.id))

            assertEquals(firstTab.id, searchStore.state.tabId)
            assertEquals(
                CurrentTabData("first search", firstTab.content.url, null),
                searchStore.state.currentTabData,
            )
            assertEquals("", searchStore.state.searchTerms)
            assertEquals("", searchStore.state.query)

            appStore.dispatch(SearchStarted(secondTab.id))

            assertEquals(secondTab.id, searchStore.state.tabId)
            assertEquals(
                CurrentTabData("second search", secondTab.content.url, null),
                searchStore.state.currentTabData,
            )
            assertEquals("", searchStore.state.searchTerms)
            assertEquals("", searchStore.state.query)

            appStore.dispatch(SearchStarted())

            assertNull(searchStore.state.tabId)
            assertNull(searchStore.state.currentTabData)

            appStore.dispatch(SearchEnded)

            assertNull(searchStore.state.tabId)
            assertNull(searchStore.state.currentTabData)
            assertEquals("", searchStore.state.searchTerms)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `GIVEN an inactive search with a source tab WHEN browser state changes THEN leave tab details cleared until search starts`() =
        runTest(UnconfinedTestDispatcher()) {
            val tab = createTab("https://example.com", searchTerms = "initial search")
            val browserStore = BrowserStore(BrowserState(tabs = listOf(tab)))
            val appStore = AppStore(AppState(searchState = AppSearchState.EMPTY.copy(sourceTabId = tab.id)))
            val middleware = BrowserStoreToFenixSearchMapperMiddleware(browserStore, backgroundScope, appStore)
            val searchStore = buildStore(middleware)

            assertNull(searchStore.state.tabId)
            assertNull(searchStore.state.currentTabData)

            browserStore.dispatch(UpdateSearchTermsAction(tab.id, "updated while inactive"))

            assertNull(searchStore.state.currentTabData)

            appStore.dispatch(SearchStarted(tab.id))

            assertEquals(tab.id, searchStore.state.tabId)
            assertEquals(
                CurrentTabData("updated while inactive", tab.content.url, null),
                searchStore.state.currentTabData,
            )

            appStore.dispatch(SearchEnded)
            browserStore.dispatch(UpdateSearchTermsAction(tab.id, "updated after search ended"))

            assertNull(searchStore.state.tabId)
            assertNull(searchStore.state.currentTabData)

            appStore.dispatch(SearchStarted(tab.id))

            assertEquals(
                CurrentTabData("updated after search ended", tab.content.url, null),
                searchStore.state.currentTabData,
            )
            assertEquals("", searchStore.state.query)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `WHEN the source tab search terms change or the tab is removed THEN synchronize current tab data`() =
        runTest(UnconfinedTestDispatcher()) {
            val tab = createTab("https://example.com", title = "Page title", searchTerms = "initial search")
            val browserStore = BrowserStore(BrowserState(tabs = listOf(tab)))
            val appStore = AppStore()
            appStore.dispatch(SearchStarted(tab.id))
            val middleware = BrowserStoreToFenixSearchMapperMiddleware(browserStore, backgroundScope, appStore)
            val searchStore = buildStore(middleware)

            assertEquals(
                CurrentTabData("initial search", tab.content.url, null),
                searchStore.state.currentTabData,
            )

            browserStore.dispatch(UpdateSearchTermsAction(tab.id, "updated search"))

            assertEquals(
                CurrentTabData("updated search", tab.content.url, null),
                searchStore.state.currentTabData,
            )
            assertEquals("", searchStore.state.searchTerms)
            assertEquals("", searchStore.state.query)

            for (searchTerms in listOf("", "   ")) {
                browserStore.dispatch(UpdateSearchTermsAction(tab.id, searchTerms))

                assertEquals(
                    CurrentTabData("Page title", tab.content.url, null),
                    searchStore.state.currentTabData,
                )
                assertEquals("", searchStore.state.searchTerms)
                assertEquals("", searchStore.state.query)
            }

            browserStore.dispatch(RemoveTabAction(tab.id))

            assertNull(searchStore.state.tabId)
            assertNull(searchStore.state.currentTabData)
            assertEquals("", searchStore.state.searchTerms)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `WHEN the browser search state changes THEN update the application search state`() =
        runTest(UnconfinedTestDispatcher()) {
            val defaultSearchEngine: SearchEngine = mockk()
            val newSearchEngines: List<SearchEngine> = listOf(defaultSearchEngine, mockk())
            val browserStore =
                BrowserStore(BrowserState(search = SearchState(applicationSearchEngines = newSearchEngines)))
            val middleware = BrowserStoreToFenixSearchMapperMiddleware(browserStore, backgroundScope)
            val searchStore = buildStore(middleware)

            browserStore.dispatch(ApplicationSearchEnginesLoaded(newSearchEngines))

            assertEquals(defaultSearchEngine, searchStore.state.defaultEngine)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `GIVEN no appStore WHEN the browser search state changes THEN isPrivate defaults to false`() =
        runTest(UnconfinedTestDispatcher()) {
            val newSearchEngines: List<SearchEngine> = listOf(mockk(), mockk())
            val browserStore =
                BrowserStore(BrowserState(search = SearchState(applicationSearchEngines = newSearchEngines)))
            val actionsCaptor = CaptureActionsMiddleware<SearchFragmentState, SearchFragmentAction>()
            val middleware = BrowserStoreToFenixSearchMapperMiddleware(browserStore, backgroundScope)
            buildStore(middleware, actionsCaptor)

            browserStore.dispatch(ApplicationSearchEnginesLoaded(newSearchEngines))

            actionsCaptor.assertLastAction(SearchFragmentAction.UpdateSearchState::class) {
                assertFalse(it.isPrivate)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `GIVEN appStore in private mode WHEN the browser search state changes THEN isPrivate is true`() =
        runTest(UnconfinedTestDispatcher()) {
            val newSearchEngines: List<SearchEngine> = listOf(mockk(), mockk())
            val browserStore =
                BrowserStore(BrowserState(search = SearchState(applicationSearchEngines = newSearchEngines)))
            val appStore = AppStore(AppState(mode = BrowsingMode.Private))
            val actionsCaptor = CaptureActionsMiddleware<SearchFragmentState, SearchFragmentAction>()
            val middleware = BrowserStoreToFenixSearchMapperMiddleware(browserStore, backgroundScope, appStore)
            buildStore(middleware, actionsCaptor)

            browserStore.dispatch(ApplicationSearchEnginesLoaded(newSearchEngines))

            actionsCaptor.assertLastAction(SearchFragmentAction.UpdateSearchState::class) {
                assertTrue(it.isPrivate)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `GIVEN appStore in normal mode WHEN the browser search state changes THEN isPrivate is false`() =
        runTest(UnconfinedTestDispatcher()) {
            val newSearchEngines: List<SearchEngine> = listOf(mockk(), mockk())
            val browserStore =
                BrowserStore(BrowserState(search = SearchState(applicationSearchEngines = newSearchEngines)))
            val appStore = AppStore(AppState(mode = BrowsingMode.Normal))
            val actionsCaptor = CaptureActionsMiddleware<SearchFragmentState, SearchFragmentAction>()
            val middleware = BrowserStoreToFenixSearchMapperMiddleware(browserStore, backgroundScope, appStore)
            buildStore(middleware, actionsCaptor)

            browserStore.dispatch(ApplicationSearchEnginesLoaded(newSearchEngines))

            actionsCaptor.assertLastAction(SearchFragmentAction.UpdateSearchState::class) {
                assertFalse(it.isPrivate)
            }
        }

    private fun buildStore(
        middleware: BrowserStoreToFenixSearchMapperMiddleware,
        vararg additional: CaptureActionsMiddleware<SearchFragmentState, SearchFragmentAction>,
    ) =
        SearchFragmentStore(
            initialState = EMPTY_SEARCH_FRAGMENT_STATE,
            middleware = listOf(middleware) + additional.toList(),
        )
}
