/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.collections

/**
 * The surface where the [CollectionsMigrationPromoCard] is shown.
 *
 * @property telemetryName The value recorded as the `source` in the Collections migration telemetry.
 */
enum class CollectionsMigrationPromoSource(val telemetryName: String) {
    HOMEPAGE("homepage"),
    TABS_TRAY("tabs_tray"),
}
