/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabgroups.strip.redux

import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.Store

/**
 * A [Store] that holds the [TabGroupsStripState] and reduces [TabGroupsStripAction]s dispatched to it.
 *
 * @param initialState The initial [TabGroupsStripState].
 * @param middleware The list of [Middleware] to apply to dispatched [TabGroupsStripAction]s.
 */
class TabGroupsStripStore(
    initialState: TabGroupsStripState = TabGroupsStripState.TabGroupStripHidden,
    middleware: List<Middleware<TabGroupsStripState, TabGroupsStripAction>> = emptyList(),
) :
    Store<TabGroupsStripState, TabGroupsStripAction>(
        initialState,
        TabGroupsStripReducer::reduce,
        middleware,
    )
