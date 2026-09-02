/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.generation.behavior

/**
 * Applies runtime state after the app launches but before behavior capabilities run.
 *
 * For now this intentionally does almost nothing because BrowserMode=Default is implicit.
 *
 * Future examples:
 * - BrowserMode=Private may open private browsing mode.
 * - Account=SignedIn may seed or perform login setup.
 * - Network=Offline may invoke device/system helpers.
 */
object BehaviorRuntimeStateSetup {

    fun apply(
        runtime: BehaviorRuntime,
        context: BehaviorContextVariant,
    ) {
        val browserMode = context.values["BrowserMode"] ?: "Default"

        when (browserMode) {
            "Default" -> {
                // Normal launch path already gives us default browser mode.
            }

            else -> {
                throw AssertionError(
                    "Runtime setup for BrowserMode=$browserMode is not implemented. " +
                        "This context should have been filtered by BehaviorContextSupportRegistry.",
                )
            }
        }
    }
}
