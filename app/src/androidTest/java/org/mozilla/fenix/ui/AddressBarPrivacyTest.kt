/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.KeyEvent
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import mozilla.components.browser.state.selector.selectedTab
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mozilla.fenix.R

/**
 * Tests that address bar editing keeps the browsing mode and the URL escaping of the page it was started from. Search
 * suggestions are kept off here so that only the address bar entry points vary; [AddressBarSuggestionsPrivacyTest]
 * covers the suggestion permissions.
 */
@RunWith(Parameterized::class)
class AddressBarPrivacyTest(focusMode: Boolean, privateMode: Boolean) : AddressBarTestCase(focusMode, privateMode) {
    private val clipboard
        get() = compose.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private val device
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun clipboardSuggestionPreservesEscapesAndBrowsingMode() {
        pasteAndNavigate(clipboardSuggestions = true)
    }

    @Test
    fun keyboardPasteWorksWithClipboardSuggestionsDisabled() {
        pasteAndNavigate(clipboardSuggestions = false)
    }

    @Test
    fun explicitSearchForAUrlPreservesBrowsingMode() {
        loadPage(server.url("/old%20page").toString())
        editCurrentPage()
        editor.performTextReplacement("example.com/a b")
        compose.waitUntil(timeout) { compose.onAllNodesWithText("example.com/a b").fetchSemanticsNodes().size > 1 }
        compose.onNode(hasText("example.com/a b") and !hasSetTextAction()).performClick()
        compose.waitUntil(timeout) { store.state.selectedTab?.content?.searchTerms == "example.com/a b" }
        assertEquals(privateMode, store.state.selectedTab?.content?.private)
    }

    private fun pasteAndNavigate(clipboardSuggestions: Boolean) {
        setPreference(R.string.pref_key_show_clipboard_suggestions, clipboardSuggestions)
        val pastedUrl = server.url("/pasted%20page?q=a%2Fb").toString()
        loadPage(server.url("/old%20page").toString())
        clipboard.setPrimaryClip(ClipData.newPlainText("URL", pastedUrl))
        editCurrentPage()
        editor.performTextReplacement("")
        val clipboardTitle = compose.activity.getString(R.string.awesomebar_clipboard_title)
        if (clipboardSuggestions) {
            compose.waitUntil(timeout) {
                compose.onAllNodesWithText(clipboardTitle).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(clipboardTitle).performClick()
        } else {
            compose.onNodeWithText(clipboardTitle).assertDoesNotExist()
            device.pressKeyCode(KeyEvent.KEYCODE_V, KeyEvent.META_CTRL_ON)
        }
        editor.assertTextEquals(pastedUrl)
        editor.performImeAction()
        compose.waitUntil(timeout) { store.state.selectedTab?.content?.url == pastedUrl }
        assertEquals(privateMode, store.state.selectedTab?.content?.private)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "focus-{0}_private-{1}")
        fun modes() =
            listOf(false, true).flatMap { focus ->
                listOf(false, true).map { private -> arrayOf(focus, private) }
            }
    }
}
