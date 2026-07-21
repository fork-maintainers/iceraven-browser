/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.generation.behavior

import androidx.test.platform.app.InstrumentationRegistry

object BehaviorRunArguments {

    fun matrixProfile(
        default: BehaviorMatrixProfile = BehaviorMatrixProfile.SMOKE,
    ): BehaviorMatrixProfile {
        val raw = string(
            key = "behaviorMatrixProfile",
            default = default.name,
        )

        return BehaviorMatrixProfile.values()
            .firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
            ?: default
    }

    fun runState(
        default: String = "",
    ): String {
        return string(
            key = "testRunState",
            default = default,
        )
    }

    fun string(
        key: String,
        default: String = "",
    ): String {
        val instrumentationValue = runCatching {
            InstrumentationRegistry
                .getArguments()
                .getString(key)
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()

        if (!instrumentationValue.isNullOrBlank()) {
            return instrumentationValue
        }

        return System.getProperty(key)
            ?.takeIf { it.isNotBlank() }
            ?: default
    }
}
