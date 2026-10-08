/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.telemetry

import mozilla.telemetry.glean.private.NoExtras
import org.mozilla.fenix.GleanMetrics.Toolbar
import org.mozilla.fenix.GleanMetrics.ToolbarGoogleLensButton
import org.mozilla.fenix.components.lens.CameraMode

/**
 * Records a tap on a button shown in the address bar while it is in edit mode.
 *
 * @param item The telemetry identifier of the tapped button.
 * @param sourceTabId The ID of the tab from which the search was started, or `null` if started from home.
 */
internal fun recordAddressBarButtonTapped(item: String, sourceTabId: String?) {
    val surface = if (sourceTabId == null) SURFACE_HOME else SURFACE_BROWSER
    Toolbar.buttonTapped.record(
        Toolbar.ButtonTappedExtra(
            source = SOURCE_ADDRESS_BAR,
            item = item,
            surface = surface,
        )
    )
}

/**
 * Records a tap on the Lens button shown in the address bar, attributed to the camera mode it opens in.
 *
 * @param lensCameraLastMode The last selected Lens [CameraMode].
 * @param sourceTabId The ID of the tab from which the search was started, or `null` if started from home.
 */
internal fun recordLensButtonTapped(lensCameraLastMode: CameraMode, sourceTabId: String?) {
    if (lensCameraLastMode == CameraMode.QR) {
        recordAddressBarButtonTapped(ACTION_LENS_QR_CLICKED, sourceTabId)
    } else {
        recordAddressBarButtonTapped(ACTION_LENS_CLICKED, sourceTabId)
        ToolbarGoogleLensButton.tapped.record(NoExtras())
    }
}
