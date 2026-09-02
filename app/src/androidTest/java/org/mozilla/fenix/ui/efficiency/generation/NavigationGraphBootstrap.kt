/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.generation

import android.util.Log
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.ui.efficiency.helpers.PageContext
import org.mozilla.fenix.ui.efficiency.navigation.NavigationRegistry
import org.mozilla.fenix.ui.efficiency.navigation.PageCatalog
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2

/**
 * Initializes page objects once so their init blocks register navigation edges before
 * planning runs from static JUnit parameter providers (`data()` methods).
 *
 * Shared across domains that need `NavigationRegistry` populated ahead of a `Parameterized`
 * `data()` call (P2b-3 follow-up, 2026-07-17) — previously duplicated identically as
 * `NavigationPairGraphBootstrap` and `BehaviorGraphBootstrap`.
 */
object NavigationGraphBootstrap {

    private const val TAG = "NavigationGraphBootstrap"

    private var initialized = false

    fun ensureInitialized() {
        if (initialized) return

        val composeRule = AndroidComposeTestRuleV2(
            HomeActivityIntentTestRule(
                skipOnboarding = true,
                isPageLoadTranslationsPromptEnabled = false,
            ),
        ) { it.activity }

        val pageContext = PageContext(composeRule)

        PageCatalog.discoverPages().forEach { pageRef ->
            val page = pageRef.getter(pageContext)
            Log.i(
                TAG,
                "Initialized page ${page.pageName} from property ${pageRef.propertyName}",
            )
        }

        NavigationRegistry.logPathSummary()
        initialized = true
    }
}
