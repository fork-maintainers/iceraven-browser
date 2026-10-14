/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.core.net.toUri
import mozilla.components.browser.state.selector.selectedTab
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AddressBarNavigationTest(focusMode: Boolean) : AddressBarTestCase(focusMode) {
    private var pageSequence = 0

    @Test
    fun newSchemelessAddressWithSpacesNavigatesOnTheKeyboardAction() {
        val origin = server.url("/").toString().removeSuffix("/")
        loadPage(origin + "/start")
        editCurrentPage()
        editor.performTextReplacement(origin.removePrefix("http://") + "/new path?q=hello world")
        editor.performImeAction()
        val path = "/new%20path?q=hello%20world"
        compose.waitUntil(timeout) {
            store.state.selectedTab?.content?.url == origin + path && path in requests
        }
    }

    @Test
    fun newlyTypedUnicodeAndSpacesReachGeckoOnTheKeyboardAction() {
        val origin = "http://localhost:${server.port}"
        loadPage(origin + "/start")
        editCurrentPage()
        editor.performTextReplacement(origin + "/日本 語?q=한 글&keep=a%2Fb")
        editor.performImeAction()
        val path = "/%E6%97%A5%E6%9C%AC%20%E8%AA%9E?q=%ED%95%9C%20%EA%B8%80&keep=a%2Fb"
        compose.waitUntil(timeout) {
            store.state.selectedTab?.content?.url == origin + path && path in requests
        }
    }

    @Test
    fun hostAndPortAddressWithSpacesReachesTheServer() {
        submit("localhost:${server.port}/host port b")
        val path = "/host%20port%20b"
        compose.waitUntil(timeout) {
            store.state.selectedTab?.content?.url == "http://localhost:${server.port}$path" && path in requests
        }
    }

    @Test
    fun keyboardDecisionsUseTheAndroidClassifier() {
        val origin = server.url("/").toString().removeSuffix("/")
        val navigations = listOf("/a b" to "/a%20b", "/?q=hello world" to "/?q=hello%20world", "/a%20" to "/a%20")
        navigations.forEach { (input, path) ->
            submit(origin + input)
            compose.waitUntil(timeout) { loaded { it == origin + path } && path in requests }
        }
        listOf(
                "https://exa mple.com/a",
                "example.com/a b",
                "example.com some words",
                "node.js/express tutorial",
                "intranet/a b",
                "some ordinary words",
            )
            .forEach { input ->
                submit(input)
                val isSearch = { url: String -> url.toUri().getQueryParameter("q") == input }
                compose.waitUntil(timeout) {
                    loaded { it.startsWith("$origin/search?") && isSearch(it) } &&
                        requests.any { it.startsWith("/search?") && isSearch(origin + it) }
                }
            }
    }

    private fun loaded(url: (String) -> Boolean) =
        store.state.selectedTab?.content?.let { !it.loading && url(it.url) } == true

    private fun submit(input: String) {
        loadPage(server.url("/start/${pageSequence++}").toString())
        editCurrentPage()
        requests.clear()
        editor.performTextReplacement(input)
        editor.performImeAction()
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "focusMode={0}") fun modes() = listOf(true, false)
    }
}
