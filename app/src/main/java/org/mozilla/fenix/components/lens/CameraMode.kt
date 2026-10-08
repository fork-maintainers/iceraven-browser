/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.lens

/**
 * Capture mode for the Lens camera screen. Lens mode captures or picks an image for upload to Google Lens; QR mode runs
 * continuous QR detection on the preview.
 */
enum class CameraMode {
    LENS,
    QR;

    companion object {
        /** Returns the [CameraMode] whose name is [name], or null if there is none. */
        fun fromNameOrNull(name: String?): CameraMode? = entries.firstOrNull { it.name == name }
    }
}
