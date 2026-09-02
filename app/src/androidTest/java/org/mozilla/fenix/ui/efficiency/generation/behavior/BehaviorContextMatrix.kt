/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.generation.behavior

/**
 * Context variants are intentionally separate from behavior definitions.
 *
 * Behavior answers: what product rule is being checked?
 * Context answers: under which state/device/account/browser-mode should it run?
 */
data class BehaviorContextVariant(
    val values: Map<String, String>,
) {
    override fun toString(): String = values.entries
        .sortedBy { it.key }
        .joinToString(" | ") { "${it.key}=${it.value}" }
}

enum class BehaviorMatrixProfile {
    SMOKE,
    BASE_FLAGS,
    PAIRWISE_PREVIEW,
    EXHAUSTIVE_PREVIEW,
}

object BehaviorContextMatrix {

    fun variants(
        profile: BehaviorMatrixProfile = BehaviorRunArguments.matrixProfile(),
    ): List<BehaviorContextVariant> {
        return when (profile) {
            BehaviorMatrixProfile.SMOKE -> smoke()
            BehaviorMatrixProfile.BASE_FLAGS -> baseFlags()
            BehaviorMatrixProfile.PAIRWISE_PREVIEW -> pairwisePreview()
            BehaviorMatrixProfile.EXHAUSTIVE_PREVIEW -> exhaustivePreview()
        }
    }

    private fun smoke(): List<BehaviorContextVariant> = listOf(
        defaultContext(),
    )

    private fun baseFlags(): List<BehaviorContextVariant> = listOf(
        defaultContext(),
        defaultContext("PocketEnabled" to "false"),
        defaultContext("RecentlyVisitedEnabled" to "false"),
        defaultContext("UnifiedTrustPanelEnabled" to "false"),
    )

    private fun pairwisePreview(): List<BehaviorContextVariant> = listOf(
        defaultContext(),
        defaultContext("BrowserMode" to "Private"),
        defaultContext("Account" to "SignedIn"),
        defaultContext("DeviceClass" to "Tablet"),
        defaultContext(
            "PocketEnabled" to "false",
            "RecentlyVisitedEnabled" to "false",
            "UnifiedTrustPanelEnabled" to "false",
        ),
    )

    private fun exhaustivePreview(): List<BehaviorContextVariant> {
        val browserModes = listOf("Default", "Private")
        val accounts = listOf("SignedOut", "SignedIn")
        val deviceClasses = listOf("Phone", "Tablet")
        val pocketValues = listOf("true", "false")
        val recentlyVisitedValues = listOf("true", "false")
        val unifiedTrustPanelValues = listOf("true", "false")

        return buildList {
            for (browserMode in browserModes) {
                for (account in accounts) {
                    for (deviceClass in deviceClasses) {
                        for (pocket in pocketValues) {
                            for (recentlyVisited in recentlyVisitedValues) {
                                for (unifiedTrustPanel in unifiedTrustPanelValues) {
                                    add(
                                        defaultContext(
                                            "BrowserMode" to browserMode,
                                            "Account" to account,
                                            "DeviceClass" to deviceClass,
                                            "PocketEnabled" to pocket,
                                            "RecentlyVisitedEnabled" to recentlyVisited,
                                            "UnifiedTrustPanelEnabled" to unifiedTrustPanel,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }.distinctBy { it.toString() }
    }

    private fun defaultContext(
        vararg overrides: Pair<String, String>,
    ): BehaviorContextVariant {
        val values = mutableMapOf(
            "BrowserMode" to "Default",
        )

        overrides.forEach { (key, value) ->
            values[key] = value
        }

        return BehaviorContextVariant(values = values)
    }
}
