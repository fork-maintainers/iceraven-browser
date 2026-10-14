/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests that address bar searches only reach the search engine when both the normal and the private mode search
 * suggestion permissions allow it.
 */
@RunWith(Parameterized::class)
class AddressBarSuggestionsPrivacyTest(
    focusMode: Boolean,
    privateMode: Boolean,
    remoteSuggestions: Boolean,
    privateSuggestions: Boolean,
) : AddressBarTestCase(focusMode, privateMode, remoteSuggestions, privateSuggestions) {
    @Test
    fun remoteSuggestionsRespectNormalAndPrivateModePermissions() {
        loadPage(server.url("/old%20page").toString())
        editCurrentPage()
        val query = "privacy probe"
        requests.clear()
        editor.performTextReplacement(query)
        compose.waitUntil(timeout) {
            compose.onAllNodes(hasText(query) and !hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        val suggestionsAllowed = remoteSuggestions && (!privateMode || privateSuggestions)
        if (suggestionsAllowed) {
            compose.waitUntil(timeout) {
                compose.onAllNodesWithText("$query remote result").fetchSemanticsNodes().isNotEmpty()
            }
            assertTrue(requests.any { it.startsWith("/suggest?") })
        } else {
            assertFalse(requests.any { it.startsWith("/suggest?") })
            compose.onNodeWithText("$query remote result").assertDoesNotExist()
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "focus-{0}_private-{1}_remote-{2}_privateRemote-{3}")
        fun modes() =
            listOf(
                arrayOf(false, false, false, false),
                arrayOf(false, false, true, false),
                arrayOf(false, true, false, false),
                arrayOf(false, true, true, false),
                arrayOf(false, true, true, true),
            )
    }
}
