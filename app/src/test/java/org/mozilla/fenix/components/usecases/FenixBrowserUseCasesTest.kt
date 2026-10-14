/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.usecases

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.test.TestScope
import mozilla.components.browser.state.engine.EngineMiddleware
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.SearchState
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.base.profiler.Profiler
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.utils.ABOUT_HOME_URL
import mozilla.components.feature.search.SearchUseCases
import mozilla.components.feature.search.ext.createSearchEngine
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.support.test.robolectric.testContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppState

@RunWith(AndroidJUnit4::class)
class FenixBrowserUseCasesTest {

    private lateinit var searchEngine: SearchEngine
    private lateinit var browserStore: BrowserStore
    private lateinit var profiler: Profiler
    private lateinit var tabsUseCases: TabsUseCases
    private lateinit var loadUrlUseCase: SessionUseCases.DefaultLoadUrlUseCase
    private lateinit var searchUseCases: SearchUseCases
    private lateinit var defaultSearchUseCase: SearchUseCases.DefaultSearchUseCase
    private lateinit var useCases: FenixBrowserUseCases
    private lateinit var homepageTitle: String
    private lateinit var appStore: AppStore

    @Before
    fun setup() {
        tabsUseCases = mockk(relaxed = true)
        loadUrlUseCase = mockk(relaxed = true)
        searchUseCases = mockk(relaxed = true)

        profiler =
            mockk(relaxed = true) {
                every { getProfilerTime() } returns PROFILER_START_TIME
                every { isProfilerActive() } returns true
            }

        searchEngine =
            createSearchEngine(
                name = "name",
                url = "https://www.example.org/?q={searchTerms}",
                icon = mockk(relaxed = true),
            )
        browserStore =
            BrowserStore(
                initialState =
                    BrowserState(
                        tabs = listOf(createTab("https://www.mozilla.org", id = "1")),
                        selectedTabId = "1",
                        search = SearchState(regionSearchEngines = listOf(searchEngine)),
                    ),
                middleware =
                    EngineMiddleware.create(
                        mockk<Engine>(),
                        TestScope(),
                    ),
            )
        defaultSearchUseCase =
            spyk(
                SearchUseCases(
                        browserStore,
                        mockk(relaxed = true),
                        mockk(relaxed = true),
                    )
                    .defaultSearch
            )

        every { searchUseCases.defaultSearch } returns defaultSearchUseCase

        homepageTitle = testContext.getString(R.string.tab_tray_homepage_tab)

        appStore = AppStore(AppState())

        useCases =
            FenixBrowserUseCases(
                tabsUseCases = tabsUseCases,
                loadUrlUseCase = loadUrlUseCase,
                searchUseCases = searchUseCases,
                homepageTitle = homepageTitle,
                profiler = profiler,
                appStore = appStore,
            )
    }

    @Test
    fun `readable HTTP URL navigates with a configured default search engine`() {
        val input = "https://example.com/тест файл?q=hello world"
        useCases.loadUrlOrSearch(input, newTab = false, searchEngine = searchEngine)
        verify {
            loadUrlUseCase.invoke(
                url = "https://example.com/тест%20файл?q=hello%20world",
                originalInput = input,
            )
        }
    }

    @Test
    fun `without a search engine the entered text reaches Gecko unprepared`() {
        listOf("hello world", "https://exa mple.com/a", "site:example.com test words").forEach { input ->
            useCases.loadUrlOrSearch(input, newTab = false, searchEngine = null)

            verify(exactly = 1) { loadUrlUseCase.invoke(url = input, originalInput = input) }
        }
    }

    @Test
    fun `without a search engine a recognized address is still prepared`() {
        val input = "example.com/a b?q=c d"
        useCases.loadUrlOrSearch(input, newTab = false, searchEngine = null)

        verify {
            loadUrlUseCase.invoke(
                url = "http://example.com/a%20b?q=c%20d",
                originalInput = input,
            )
        }
    }

    @Test
    fun `non HTTP schemes retain their destination with a default search engine`() {
        listOf("mailto:user@example.com", "tel:12345", "custom:12345").forEach { input ->
            useCases.loadUrlOrSearch(input, newTab = false, searchEngine = searchEngine)
            verify(exactly = 1) { loadUrlUseCase.invoke(url = input, originalInput = input) }
        }
        verify(exactly = 0) { defaultSearchUseCase.invoke(searchTerms = any(), searchEngine = any(), flags = any()) }
    }

    @Test
    fun `explicit search keeps readable URL terms`() {
        val input = "https://example.com/a b"
        useCases.loadUrlOrSearch(input, newTab = false, forceSearch = true, searchEngine = searchEngine)
        verify {
            defaultSearchUseCase.invoke(
                searchTerms = input,
                searchEngine = searchEngine,
                flags = EngineSession.LoadUrlFlags.none(),
            )
        }
    }

    @Test
    fun `GIVEN an URL input WHEN the load url or search use case is invoked THEN load the URL`() {
        val url = "https://www.mozilla.org"

        useCases.loadUrlOrSearch(
            searchTermOrURL = url,
            newTab = false,
            forceSearch = false,
            private = false,
            searchEngine = mockk(relaxed = true),
        )

        verify {
            loadUrlUseCase.invoke(
                url = url,
                flags = EngineSession.LoadUrlFlags.none(),
                originalInput = url,
            )
        }
    }

    @Test
    fun `GIVEN an URL to load in a new tab WHEN the load url or search use case is invoked THEN load the URL in new tab`() {
        val url = "https://www.mozilla.org"
        val newTab = true
        val private = false

        useCases.loadUrlOrSearch(
            searchTermOrURL = url,
            newTab = newTab,
            forceSearch = false,
            private = private,
            searchEngine = mockk(relaxed = true),
        )

        verify {
            tabsUseCases.addTab.invoke(
                url = url,
                flags = EngineSession.LoadUrlFlags.none(),
                private = private,
                originalInput = url,
            )
        }
    }

    @Test
    fun `GIVEN an URL to load in a new private tab WHEN the load url or search use case is invoked THEN load the URL in new private tab`() {
        val url = "https://www.mozilla.org"
        val newTab = true
        val private = true

        useCases.loadUrlOrSearch(
            searchTermOrURL = url,
            newTab = newTab,
            forceSearch = false,
            private = private,
            searchEngine = mockk(relaxed = true),
        )

        verify {
            tabsUseCases.addTab.invoke(
                url = url,
                flags = EngineSession.LoadUrlFlags.none(),
                private = private,
                originalInput = url,
            )
        }
    }

    @Test
    fun `GIVEN an url input and no search engines WHEN the load url or search use case is invoked THEN load the URL`() {
        val url = "https://www.mozilla.org"
        val searchEngine = null

        useCases.loadUrlOrSearch(
            searchTermOrURL = url,
            newTab = false,
            forceSearch = false,
            private = false,
            searchEngine = searchEngine,
        )

        verify {
            loadUrlUseCase.invoke(
                url = url,
                flags = EngineSession.LoadUrlFlags.none(),
                originalInput = url,
            )
        }
    }

    @Test
    fun `GIVEN an url input to load on a new tab and no search engines WHEN the load url or search use case is invoked THEN load the URL in a new tab`() {
        val url = "https://www.mozilla.org"
        val newTab = true
        val searchEngine = null
        val private = false

        useCases.loadUrlOrSearch(
            searchTermOrURL = url,
            newTab = newTab,
            forceSearch = false,
            private = private,
            searchEngine = searchEngine,
        )

        verify {
            tabsUseCases.addTab.invoke(
                url = url,
                flags = EngineSession.LoadUrlFlags.none(),
                private = private,
                originalInput = url,
            )
        }
    }

    @Test
    fun `GIVEN a search term input WHEN the load url or search use case is invoked THEN perform a search`() {
        val searchTerm = "mozilla"
        val forceSearch = true

        useCases.loadUrlOrSearch(
            searchTermOrURL = searchTerm,
            newTab = false,
            forceSearch = forceSearch,
            private = false,
            searchEngine = searchEngine,
        )

        verify {
            defaultSearchUseCase.invoke(
                searchTerms = searchTerm,
                searchEngine = searchEngine,
                flags = EngineSession.LoadUrlFlags.none(),
            )
        }
    }

    @Test
    fun `GIVEN a search term input to load in a new tab WHEN the load url or search use case is invoked THEN perform a search in a new tab`() {
        val searchTerm = "mozilla"
        val forceSearch = true
        val newTab = true

        useCases.loadUrlOrSearch(
            searchTermOrURL = searchTerm,
            newTab = newTab,
            forceSearch = forceSearch,
            private = false,
            searchEngine = searchEngine,
        )

        verify {
            searchUseCases.newTabSearch.invoke(
                searchTerms = searchTerm,
                source = SessionState.Source.Internal.UserEntered,
                selected = true,
                searchEngine = searchEngine,
                flags = EngineSession.LoadUrlFlags.none(),
                additionalHeaders = null,
            )
        }
    }

    @Test
    fun `GIVEN a search term input to load in a new private tab WHEN the load url or search use case is invoked THEN perform a search in a new private tab`() {
        val searchTerm = "mozilla"
        val forceSearch = true
        val newTab = true
        val private = true

        useCases.loadUrlOrSearch(
            searchTermOrURL = searchTerm,
            newTab = newTab,
            forceSearch = forceSearch,
            private = private,
            searchEngine = searchEngine,
        )

        verify {
            searchUseCases.newPrivateTabSearch.invoke(
                searchTerms = searchTerm,
                source = SessionState.Source.Internal.UserEntered,
                selected = true,
                searchEngine = searchEngine,
                flags = EngineSession.LoadUrlFlags.none(),
                additionalHeaders = null,
            )
        }
    }

    @Test
    fun `GIVEN an URL input and profiler is active WHEN the load url or search use case is invoked THEN load the URL and add the profiler marker`() {
        val url = "https://www.mozilla.org"
        val newTab = false

        useCases.loadUrlOrSearch(
            searchTermOrURL = url,
            newTab = newTab,
            forceSearch = false,
            private = false,
            searchEngine = mockk(relaxed = true),
        )

        verifyOrder {
            profiler.getProfilerTime()

            loadUrlUseCase.invoke(
                url = url,
                flags = EngineSession.LoadUrlFlags.none(),
                originalInput = url,
            )

            profiler.addMarker(
                markerName = "FenixBrowserUseCases.loadUrlOrSearch",
                startTime = PROFILER_START_TIME,
                text = "newTab: $newTab, private: false",
            )
        }
    }

    @Test
    fun `WHEN add new homepage tab use case is invoked THEN create a new homepage tab`() {
        useCases.addNewHomepageTab(private = true)

        verify {
            tabsUseCases.addTab.invoke(
                url = ABOUT_HOME_URL,
                title = homepageTitle,
                private = true,
            )
        }

        useCases.addNewHomepageTab(private = false)

        verify {
            tabsUseCases.addTab.invoke(
                url = ABOUT_HOME_URL,
                title = homepageTitle,
                private = false,
            )
        }
    }

    @Test
    fun `WHEN navigate to homepage use case is invoked THEN load the ABOUT_HOME URL`() {
        useCases.navigateToHomepage()

        verify {
            loadUrlUseCase.invoke(
                url = ABOUT_HOME_URL,
                flags = EngineSession.LoadUrlFlags.none(),
            )
        }
    }

    @Test
    fun `GIVEN store is normal WHEN normal omitted and loading URL in new tab THEN open new normal tab`() {
        val url = "https://www.mozilla.org"
        appStore = AppStore(initialState = AppState(mode = BrowsingMode.Normal))
        useCases =
            FenixBrowserUseCases(
                appStore = appStore,
                tabsUseCases = tabsUseCases,
                loadUrlUseCase = loadUrlUseCase,
                searchUseCases = searchUseCases,
                homepageTitle = homepageTitle,
                profiler = profiler,
            )

        useCases.loadUrlOrSearch(
            searchTermOrURL = url,
            newTab = true,
            forceSearch = false,
            searchEngine = null,
        )

        verify {
            tabsUseCases.addTab.invoke(
                url = url,
                flags = EngineSession.LoadUrlFlags.none(),
                private = false,
                originalInput = url,
            )
        }
    }

    @Test
    fun `GIVEN store is private WHEN private omitted and loading URL in new tab THEN open new private tab`() {
        val url = "https://www.mozilla.org"
        appStore = AppStore(initialState = AppState(mode = BrowsingMode.Private))
        useCases =
            FenixBrowserUseCases(
                appStore = appStore,
                tabsUseCases = tabsUseCases,
                loadUrlUseCase = loadUrlUseCase,
                searchUseCases = searchUseCases,
                homepageTitle = homepageTitle,
                profiler = profiler,
            )

        useCases.loadUrlOrSearch(
            searchTermOrURL = url,
            newTab = true,
            forceSearch = false,
            searchEngine = null,
        )

        verify {
            tabsUseCases.addTab.invoke(
                url = url,
                flags = EngineSession.LoadUrlFlags.none(),
                private = true,
                originalInput = url,
            )
        }
    }

    companion object {
        private const val PROFILER_START_TIME = Double.MAX_VALUE
    }
}
