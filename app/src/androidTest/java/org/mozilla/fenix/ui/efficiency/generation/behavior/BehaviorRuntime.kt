/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.generation.behavior

import org.mozilla.fenix.ui.efficiency.helpers.PageContext

/** Runtime object passed to behavior capabilities and assertions. */
data class BehaviorRuntime(
    val on: PageContext,
    val data: BehaviorData,
    val context: BehaviorContextVariant,
)
