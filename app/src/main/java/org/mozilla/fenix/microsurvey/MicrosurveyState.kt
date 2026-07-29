/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.microsurvey

import org.mozilla.fenix.microsurvey.ui.ext.MicrosurveyUIData

/**
 * State of the microsurvey currently eligible to be shown, derived from the MICROSURVEY message surface.
 *
 * @property current The microsurvey to display, or null if none is eligible.
 */
data class MicrosurveyState(
    val current: MicrosurveyUIData? = null,
)
