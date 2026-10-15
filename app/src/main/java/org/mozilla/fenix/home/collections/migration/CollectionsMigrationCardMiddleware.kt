/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.collections.migration

import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.Store
import mozilla.telemetry.glean.private.NoExtras
import org.mozilla.fenix.GleanMetrics.CollectionsMigration

/**
 * [Middleware] that reacts to various [CollectionsMigrationCardAction]s.
 *
 * @param repository The repository for the Collections migration.
 */
class CollectionsMigrationCardMiddleware(private val repository: CollectionsMigrationRepository) :
    Middleware<CollectionsMigrationCardState, CollectionsMigrationCardAction> {

    override fun invoke(
        store: Store<CollectionsMigrationCardState, CollectionsMigrationCardAction>,
        next: (CollectionsMigrationCardAction) -> Unit,
        action: CollectionsMigrationCardAction,
    ) {
        when (action) {
            is CollectionsMigrationCardAction.ViewTabGroupsClicked -> {
                CollectionsMigration.promoClick.record(NoExtras())
                repository.updateCollectionsMigrationCardVisibility(visible = false)
            }
        }

        next(action)
    }
}
