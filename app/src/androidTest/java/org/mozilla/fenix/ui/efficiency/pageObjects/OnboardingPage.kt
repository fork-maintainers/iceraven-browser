/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.navigation.LaunchConfig
import org.mozilla.fenix.ui.efficiency.navigation.NavigationRegistry
import org.mozilla.fenix.ui.efficiency.selectors.OnboardingSelectors

/**
 * The first-run Onboarding flow.
 *
 * Only reachable when the app launches with onboarding enabled — declare the test class as
 * BaseTest(skipOnboarding = false). The AppEntry -> OnboardingPage edge has no steps because the
 * flow is already on screen at launch; navigateToPage() confirms arrival via the `requiredForPage`
 * selector group (the Terms of Use card title).
 */
class OnboardingPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {

    override val pageName = "OnboardingPage"

    init {
        NavigationRegistry.register(
            from = "AppEntry",
            to = pageName,
            steps = listOf(),
            launch = LaunchConfig(skipOnboarding = false),
        )
    }

    override fun mozGetSelectorsByGroup(group: String): List<Selector> {
        return OnboardingSelectors.all.filter { it.groups.contains(group) }
    }
}
