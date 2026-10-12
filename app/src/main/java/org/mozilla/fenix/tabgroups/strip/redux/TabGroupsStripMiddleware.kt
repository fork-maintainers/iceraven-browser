/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabgroups.strip.redux

import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.Store

/**
 * A [Middleware] for the [TabGroupsStripStore] that reacts to [TabGroupsStripAction]s to drive navigation and other
 * side effects.
 */
class TabGroupsStripMiddleware : Middleware<TabGroupsStripState, TabGroupsStripAction> {
    override fun invoke(
        store: Store<TabGroupsStripState, TabGroupsStripAction>,
        next: (TabGroupsStripAction) -> Unit,
        action: TabGroupsStripAction,
    ) {
        // Bug 2072307 - React to TabClicked here by selecting and showing the tapped tab in the browser
        // Bug 2071335 - React to AddTabClicked here by opening and selecting a new homepage tab in this group
        // Bug 2069525 - React to ExpandGroupClicked here by opening the expanded tab group
        next(action)
    }
}
