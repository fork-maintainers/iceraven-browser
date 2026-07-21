/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.generation.behavior

data class BehaviorContextSupportDecision(
    val isSupported: Boolean,
    val skippedReason: String? = null,
) {
    companion object {
        val Supported = BehaviorContextSupportDecision(isSupported = true)
    }
}

object BehaviorContextSupportRegistry {

    fun evaluate(
        context: BehaviorContextVariant,
    ): BehaviorContextSupportDecision {
        val unsupported = context.values.entries.mapNotNull { (key, value) ->
            val spec = BehaviorContextManifest.find(key, value)

            when {
                spec == null -> {
                    "$key=$value is not declared in BehaviorContextManifest"
                }

                spec.status != BehaviorContextStatus.IMPLEMENTED -> {
                    "${spec.id} is ${spec.status}: ${spec.reason}"
                }

                else -> null
            }
        }

        if (unsupported.isEmpty()) {
            return BehaviorContextSupportDecision.Supported
        }

        return BehaviorContextSupportDecision(
            isSupported = false,
            skippedReason = unsupported.joinToString(separator = " | "),
        )
    }
}
