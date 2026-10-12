/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabgroups.strip.redux

import mozilla.components.lib.state.Action

/** [Action]s dispatched to the [TabGroupsStripStore] to change its [TabGroupsStripState]. */
sealed interface TabGroupsStripAction : Action {
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
