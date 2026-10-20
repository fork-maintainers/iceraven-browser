/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabgroups.strip.redux

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TabGroupsStripReducerTest {
    @Test
    fun `WHEN InitializeDataObservation is dispatched THEN the state is unchanged`() {
        val state = stateWith(selectedId = "1")

        val result = TabGroupsStripReducer.reduce(state, TabGroupsStripAction.InitializeDataObservation)

        assertEquals(state, result)
    }

    @Test
    fun `WHEN TabGroupChanged with a group is dispatched THEN the group and its tabs replace the state`() {
        val tabs =
            listOf(
                TabGroupsStripState.Tab(id = "1", url = "https://example.com/1", title = "Tab 1", isFocused = true),
                TabGroupsStripState.Tab(id = "2", url = "https://example.com/2", title = "Tab 2", isFocused = false),
            )

        val result =
            TabGroupsStripReducer.reduce(
                TabGroupsStripState.TabGroupStripHidden,
                TabGroupsStripAction.TabGroupChanged(groupId = "group", groupTitle = "Group", tabs = tabs),
            )

        assertEquals(
            TabGroupsStripState.TabGroupStripVisible(groupId = "group", groupTitle = "Group", tabs = tabs),
            result,
        )
    }

    @Test
    fun `WHEN TabGroupCleared is dispatched THEN the state is cleared`() {
        val result = TabGroupsStripReducer.reduce(stateWith(selectedId = "1"), TabGroupsStripAction.TabGroupCleared)

        assertEquals(TabGroupsStripState.TabGroupStripHidden, result)
    }

    @Test
    fun `WHEN TabClicked is dispatched THEN only the matching tab is selected`() {
        val result = TabGroupsStripReducer.reduce(stateWith(selectedId = "1"), TabGroupsStripAction.TabClicked("2"))

        assertEquals(listOf("2"), focusedIds(result))
    }

    @Test
    fun `WHEN TabClicked targets the already selected tab THEN it stays selected`() {
        val result = TabGroupsStripReducer.reduce(stateWith(selectedId = "2"), TabGroupsStripAction.TabClicked("2"))

        assertEquals(listOf("2"), focusedIds(result))
    }

    @Test
    fun `WHEN TabClicked targets an unknown tab THEN no tab is selected`() {
        val result =
            TabGroupsStripReducer.reduce(stateWith(selectedId = "1"), TabGroupsStripAction.TabClicked("missing"))

        assertTrue(focusedIds(result).isEmpty())
    }

    @Test
    fun `WHEN TabClicked is dispatched while not in a group THEN the state is unchanged`() {
        val result =
            TabGroupsStripReducer.reduce(TabGroupsStripState.TabGroupStripHidden, TabGroupsStripAction.TabClicked("1"))

        assertEquals(TabGroupsStripState.TabGroupStripHidden, result)
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

    private fun focusedIds(state: TabGroupsStripState) =
        (state as TabGroupsStripState.TabGroupStripVisible).tabs.filter { it.isFocused }.map { it.id }

    private fun stateWith(selectedId: String) =
        TabGroupsStripState.TabGroupStripVisible(
            groupId = "group",
            groupTitle = "Group",
            tabs =
                listOf("1", "2", "3").map { id ->
                    TabGroupsStripState.Tab(
                        id = id,
                        url = "https://example.com/$id",
                        title = "Tab $id",
                        isFocused = id == selectedId,
                    )
                },
        )
}
