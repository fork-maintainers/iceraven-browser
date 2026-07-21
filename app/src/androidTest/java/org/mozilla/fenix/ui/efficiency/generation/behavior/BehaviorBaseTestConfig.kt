/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.generation.behavior

/**
 * Launch-time config that can be passed into BaseTest/HomeActivityIntentTestRule.
 *
 * These values must be known before the Compose rule creates the activity.
 */
data class BehaviorBaseTestConfig(
    val skipOnboarding: Boolean = true,
    val isPageLoadTranslationsPromptEnabled: Boolean = false,
    val isPocketEnabled: Boolean = true,
    val isRecentlyVisitedFeatureEnabled: Boolean = true,
    val isUnifiedTrustPanelEnabled: Boolean = true,
)

/**
 * Converts a behavior context into BaseTest constructor flags.
 *
 * Missing values mean "use BaseTest default".
 */
fun BehaviorContextVariant.toBaseTestConfig(): BehaviorBaseTestConfig {
    return BehaviorBaseTestConfig(
        skipOnboarding = booleanValue(
            key = "SkipOnboarding",
            default = true,
        ),
        isPageLoadTranslationsPromptEnabled = booleanValue(
            key = "PageLoadTranslationsPromptEnabled",
            default = false,
        ),
        isPocketEnabled = booleanValue(
            key = "PocketEnabled",
            default = true,
        ),
        isRecentlyVisitedFeatureEnabled = booleanValue(
            key = "RecentlyVisitedEnabled",
            default = true,
        ),
        isUnifiedTrustPanelEnabled = booleanValue(
            key = "UnifiedTrustPanelEnabled",
            default = true,
        ),
    )
}

private fun BehaviorContextVariant.booleanValue(
    key: String,
    default: Boolean,
): Boolean {
    return values[key]
        ?.toBooleanStrictOrNull()
        ?: default
}
