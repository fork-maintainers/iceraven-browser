/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabgroups.strip.redux

/** Reduces [TabGroupsStripAction]s dispatched to the [TabGroupsStripStore] into a new [TabGroupsStripState]. */
internal object TabGroupsStripReducer {
    fun reduce(
        state: TabGroupsStripState,
        action: TabGroupsStripAction,
    ): TabGroupsStripState =
        when (action) {
            is TabGroupsStripAction.InitializeDataObservation -> state
            is TabGroupsStripAction.TabGroupChanged ->
                TabGroupsStripState.TabGroupStripVisible(
                    groupId = action.groupId,
                    groupTitle = action.groupTitle,
                    tabs = action.tabs,
                )
            TabGroupsStripAction.TabGroupCleared -> TabGroupsStripState.TabGroupStripHidden
            is TabGroupsStripAction.TabClicked ->
                if (state is TabGroupsStripState.TabGroupStripVisible) {
                    state.copy(tabs = state.tabs.map { it.copy(isFocused = it.id == action.tabId) })
                } else {
                    state
                }
            TabGroupsStripAction.AddTabClicked -> state
            TabGroupsStripAction.ExpandGroupClicked -> state
        }
}
