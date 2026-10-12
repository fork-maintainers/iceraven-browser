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
            is TabGroupsStripAction.TabClicked ->
                state.copy(tabs = state.tabs.map { it.copy(isFocused = it.id == action.tabId) })
            TabGroupsStripAction.AddTabClicked -> state
            TabGroupsStripAction.ExpandGroupClicked -> state
        }
}
