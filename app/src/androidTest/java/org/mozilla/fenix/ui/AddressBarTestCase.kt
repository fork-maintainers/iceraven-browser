/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import android.content.Intent
import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.net.toUri
import java.util.concurrent.CopyOnWriteArrayList
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.selectedOrDefaultSearchEngine
import org.json.JSONArray
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.FenixTestRule
import org.mozilla.fenix.helpers.HomeActivityTestRule

/**
 * Shared setup for the address bar tests: a custom search engine backed by the test web server, suggestion settings
 * that are restored after each test and helpers for loading and editing a page in the tested browsing mode.
 *
 * @property focusMode Whether the address bar is displayed in focus mode.
 * @property privateMode Whether the tested pages are loaded in a private tab.
 * @property remoteSuggestions Whether search suggestions from the search engine are enabled.
 * @property privateSuggestions Whether search suggestions are also allowed while browsing privately.
 */
abstract class AddressBarTestCase(
    protected val focusMode: Boolean,
    protected val privateMode: Boolean = false,
    protected val remoteSuggestions: Boolean = false,
    protected val privateSuggestions: Boolean = false,
) {
    @get:Rule(order = 0) val fenixTestRule = FenixTestRule()

    @get:Rule(order = 1)
    val compose =
        AndroidComposeTestRuleV2(
            HomeActivityTestRule(
                skipOnboarding = true,
                isPocketEnabled = false,
                isWallpaperOnboardingEnabled = false,
                showAddressBarInFocusMode = focusMode,
            )
        ) {
            it.activity
        }

    protected val timeout = 15_000L
    protected val visible = SemanticsMatcher("has visible bounds") { !it.boundsInRoot.isEmpty }

    protected val requests = CopyOnWriteArrayList<String>()

    protected val server
        get() = fenixTestRule.mockWebServer

    protected val store
        get() = compose.activity.components.core.store

    protected val editor: SemanticsNodeInteraction
        get() = compose.onNode(hasSetTextAction() and visible)

    private val searchUseCases
        get() = compose.activity.components.useCases.searchUseCases

    private val changedPreferences = mutableMapOf<String, Boolean?>()
    private var testSearchEngine: SearchEngine? = null
    private var previousSearchEngine: SearchEngine? = null

    @Before
    fun setUp() {
        setPreference(R.string.pref_key_show_search_suggestions, remoteSuggestions)
        setPreference(R.string.pref_key_show_search_suggestions_in_private, privateSuggestions)
        setPreference(R.string.pref_key_show_search_suggestions_in_private_onboarding, true)
        setPreference(R.string.pref_key_enable_autocomplete_urls, false)
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    requests.add(request.target)
                    return if (request.target.startsWith("/suggest?")) {
                        val query = ("http://localhost" + request.target).toUri().getQueryParameter("q")!!
                        MockResponse.Builder()
                            .setHeader("Content-Type", "application/json")
                            .body(JSONArray().put(query).put(JSONArray().put("$query remote result")).toString())
                            .build()
                    } else {
                        MockResponse.Builder()
                            .setHeader("Content-Type", "text/html; charset=utf-8")
                            .setHeader("Cache-Control", "no-store")
                            .body("<title>Address bar test</title><p>Loaded</p>")
                            .build()
                    }
                }
            }
        val engine =
            SearchEngine(
                id = "url-privacy-test",
                name = "Local suggestions",
                type = SearchEngine.Type.CUSTOM,
                icon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                resultUrls = listOf(server.url("/search").toString() + "?q={searchTerms}"),
                suggestUrl = server.url("/suggest").toString() + "?q={searchTerms}",
            )
        previousSearchEngine = store.state.search.selectedOrDefaultSearchEngine
        testSearchEngine = engine
        searchUseCases.addSearchEngine(engine)
        searchUseCases.selectSearchEngine(engine)
    }

    @After
    fun tearDown() {
        testSearchEngine?.let { searchUseCases.removeSearchEngine(it) }
        previousSearchEngine?.let { searchUseCases.selectSearchEngine(it) }
        val preferencesEditor = compose.activity.components.settings.preferences.edit()
        changedPreferences.forEach { (key, value) ->
            if (value == null) preferencesEditor.remove(key) else preferencesEditor.putBoolean(key, value)
        }
        preferencesEditor.commit()
    }

    /** Applies a boolean preference for the duration of the test, remembering the value to restore in [tearDown]. */
    protected fun setPreference(@StringRes key: Int, value: Boolean) {
        val name = compose.activity.getString(key)
        val preferences = compose.activity.components.settings.preferences
        if (!changedPreferences.containsKey(name)) {
            changedPreferences[name] = if (preferences.contains(name)) preferences.getBoolean(name, false) else null
        }
        preferences.edit().putBoolean(name, value).commit()
    }

    protected fun loadPage(url: String) {
        compose.activity.startActivity(
            Intent(Intent.ACTION_VIEW, url.toUri())
                .setPackage(compose.activity.packageName)
                .putExtra(HomeActivity.PRIVATE_BROWSING_MODE, privateMode)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        compose.waitUntil(timeout) {
            store.state.selectedTab?.content?.let { it.url == url && !it.loading && it.private == privateMode } == true
        }
    }

    protected fun openAddressBar() {
        compose.waitUntil(timeout) {
            compose.onAllNodes(hasTestTag("ADDRESSBAR_URL_BOX") and visible).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasTestTag("ADDRESSBAR_URL_BOX") and visible).performClick()
    }

    protected fun editCurrentPage() {
        openAddressBar()
        if (focusMode) compose.onNodeWithTag("mozac.awesomebar.editCurrentSiteUrlButton").performClick()
        compose.waitUntil(timeout) {
            compose.onAllNodes(hasSetTextAction() and visible).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
