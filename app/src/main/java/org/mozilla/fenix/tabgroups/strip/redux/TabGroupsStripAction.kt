/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabgroups.strip.redux

import mozilla.components.lib.state.Action

/** [Action]s dispatched to the [TabGroupsStripStore] to change its [TabGroupsStripState]. */
sealed interface TabGroupsStripAction : Action {
    /** The store was created and should start observing tab data. */
    data object InitializeDataObservation : TabGroupsStripAction

    /**
     * The active tab's group changed.
     *
     * @property groupId The ID of the active tab's group.
     * @property groupTitle The title of the active tab's group.
     * @property tabs The tabs of the active tab's group.
     */
    data class TabGroupChanged(
        val groupId: String,
        val groupTitle: String,
        val tabs: List<TabGroupsStripState.Tab>,
    ) : TabGroupsStripAction

    /** The active tab is no longer in a group. */
    data object TabGroupCleared : TabGroupsStripAction

    /**
     * A tab in the strip was clicked.
     *
     * @property tabId The ID of the clicked tab.
     */
    data class TabClicked(val tabId: String) : TabGroupsStripAction

    /** The add tab button in the strip was clicked. */
    data object AddTabClicked : TabGroupsStripAction

    /** The expand button in the strip was clicked. */
    data object ExpandGroupClicked : TabGroupsStripAction
}
