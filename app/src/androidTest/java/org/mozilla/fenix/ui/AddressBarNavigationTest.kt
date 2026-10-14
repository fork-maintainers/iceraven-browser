/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import android.content.ClipboardManager
import android.content.Context
import android.view.KeyEvent
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextRange
import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import mozilla.components.browser.state.selector.selectedTab
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AddressBarNavigationTest(focusMode: Boolean) : AddressBarTestCase(focusMode) {
    private var pageSequence = 0

    @Test
    fun readablePrefillAndUnicodeEditsReachGeckoWithoutChangingEscapedData() {
        val origin = server.url("/").toString().removeSuffix("/")
        val cases =
            listOf(
                "/%D1%82%D0%B5%D1%81%D1%82%20file?q=hello%20world" to "/тест file?q=hello world",
                "/a%2Fb?q=a%26b%3Dc&x=&x=2" to "/a%2Fb?q=a%26b%3Dc&x=&x=2",
                "/%2520?q=a+b%2Bc%20d" to "/%2520?q=a+b%2Bc d",
                "/a%5Cb?q=x" to "/a%5Cb?q=x",
            )
        cases.forEach { (canonical, readable) ->
            loadPage(origin + canonical)
            editCurrentPage()
            editor.assertTextEquals(origin + readable)
            requests.clear()
            editor.performTextReplacement(origin + readable + "&edit=новые слова#раздел один")
            editor.performImeAction()
            val path = canonical + "&edit=%D0%BD%D0%BE%D0%B2%D1%8B%D0%B5%20%D1%81%D0%BB%D0%BE%D0%B2%D0%B0"
            val fragment = "#%D1%80%D0%B0%D0%B7%D0%B4%D0%B5%D0%BB%20%D0%BE%D0%B4%D0%B8%D0%BD"
            compose.waitUntil(timeout) {
                store.state.selectedTab?.content?.url == origin + path + fragment && path in requests
            }
        }
    }

    @Test
    fun desktopFormattingRulesSurviveEditingAndGeckoNavigation() {
        val origin = server.url("/").toString().removeSuffix("/")
        val cases =
            listOf(
                Triple("/a%2Eb?q=%21", "/a.b?q=!", "/a.b?q=!"),
                Triple("/a%20", "/a%20", "/a%20"),
                Triple("/a%20%20b", "/a%20 b", "/a%20%20b"),
                Triple("/a%E2%80%8Db", "/a\u200Db", "/a%E2%80%8Db"),
                Triple("/a%F0%9F%9B%A1", "/a%F0%9F%9B%A1", "/a%F0%9F%9B%A1"),
            )
        cases.forEach { (canonical, readable, destination) ->
            loadPage(origin + canonical)
            editCurrentPage()
            editor.assertTextEquals(origin + readable)
            requests.clear()
            editor.performTextReplacement(origin + readable + "?edited=1")
            editor.performImeAction()
            val target = destination + "?edited=1"
            compose.waitUntil(timeout) {
                store.state.selectedTab?.content?.url == origin + target && target in requests
            }
        }
    }

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

    @Test
    fun canonicalCopyAndSelectedTextClipboardStaySeparate() {
        val origin = server.url("/").toString().removeSuffix("/")
        val canonical = origin + "/%D1%82%D0%B5%D1%81%D1%82%20file?q=hello%20world"
        val readable = origin + "/тест file?q=hello world"
        loadPage(canonical)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val clipboard = compose.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (focusMode) {
            openAddressBar()
            compose.onNodeWithTag("mozac.awesomebar.copyCurrentSiteDetailsButton").performClick()
            compose.waitUntil(timeout) { clipboard.primaryClip?.getItemAt(0)?.text.toString() == canonical }
            compose.onNodeWithTag("mozac.awesomebar.editCurrentSiteUrlButton").performClick()
        } else {
            editCurrentPage()
        }
        editor.assertTextEquals(readable)
        device.pressKeyCode(KeyEvent.KEYCODE_A, KeyEvent.META_CTRL_ON)
        device.pressKeyCode(KeyEvent.KEYCODE_C, KeyEvent.META_CTRL_ON)
        compose.waitUntil(timeout) { clipboard.primaryClip?.getItemAt(0)?.text.toString() == readable }
        editor.performTextInputSelection(TextRange(readable.length - "world".length, readable.length))
        device.pressKeyCode(KeyEvent.KEYCODE_C, KeyEvent.META_CTRL_ON)
        compose.waitUntil(timeout) { clipboard.primaryClip?.getItemAt(0)?.text.toString() == "world" }
        device.pressKeyCode(KeyEvent.KEYCODE_X, KeyEvent.META_CTRL_ON)
        editor.assertTextEquals(readable.removeSuffix("world"))
        device.pressKeyCode(KeyEvent.KEYCODE_V, KeyEvent.META_CTRL_ON)
        editor.assertTextEquals(readable)
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
