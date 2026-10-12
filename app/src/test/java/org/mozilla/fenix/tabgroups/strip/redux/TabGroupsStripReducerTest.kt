/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabgroups.strip.redux

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TabGroupsStripReducerTest {
    @Test
    fun `WHEN TabClicked is dispatched THEN only the matching tab is selected`() {
        val state = stateWith(selectedId = "1")

        val result = TabGroupsStripReducer.reduce(state, TabGroupsStripAction.TabClicked("2"))

        assertEquals(listOf("2"), result.tabs.filter { it.isFocused }.map { it.id })
    }

    @Test
    fun `WHEN TabClicked targets the already selected tab THEN it stays selected`() {
        val state = stateWith(selectedId = "2")

        val result = TabGroupsStripReducer.reduce(state, TabGroupsStripAction.TabClicked("2"))

        assertEquals(listOf("2"), result.tabs.filter { it.isFocused }.map { it.id })
    }

    @Test
    fun `WHEN TabClicked targets an unknown tab THEN no tab is selected`() {
        val state = stateWith(selectedId = "1")

        val result = TabGroupsStripReducer.reduce(state, TabGroupsStripAction.TabClicked("missing"))

        assertTrue(result.tabs.none { it.isFocused })
    }

    @Test
    fun `WHEN AddTabClicked is dispatched THEN the state is unchanged`() {
        val state = stateWith(selectedId = "1")

        val result = TabGroupsStripReducer.reduce(state, TabGroupsStripAction.AddTabClicked)

        assertEquals(state, result)
    }

    @Test
    fun `WHEN ExpandGroupClicked is dispatched THEN the state is unchanged`() {
        val state = stateWith(selectedId = "1")

        val result = TabGroupsStripReducer.reduce(state, TabGroupsStripAction.ExpandGroupClicked)

        assertEquals(state, result)
    }

    private fun stateWith(selectedId: String) =
        TabGroupsStripState(
            tabs =
                listOf("1", "2", "3").map { id ->
                    TabGroupsStripState.Tab(
                        id = id,
                        url = "https://example.com/$id",
                        title = "Tab $id",
                        isFocused = id == selectedId,
                    )
                }
        )
}
