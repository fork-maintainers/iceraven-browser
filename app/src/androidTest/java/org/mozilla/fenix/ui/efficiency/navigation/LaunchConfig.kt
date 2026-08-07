/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.navigation

/**
 * App launch/state a page is reached under. Mirrors the HomeActivityIntentTestRule flags on BaseTest,
 * with the same defaults — so LaunchConfig() equals the harness's normal launch. Declare a non-default
 * value on a page's AppEntry edge when the page only exists under a special launch (e.g. onboarding).
 */
data class LaunchConfig(
    val skipOnboarding: Boolean = true,
    val isPageLoadTranslationsPromptEnabled: Boolean = false,
    val isPocketEnabled: Boolean = true,
    val isRecentlyVisitedFeatureEnabled: Boolean = true,
    val shouldUseExpandedToolbar: Boolean = false,
    val isTabStripEnabled: Boolean = false,
    // Disabled by default because the "Add to Home screen" PWA prompt pops over the page on a
    // repeat visit to an installable site and covers the engine view, breaking navigation.
)
