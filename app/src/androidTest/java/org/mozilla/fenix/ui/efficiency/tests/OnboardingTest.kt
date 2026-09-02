/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.selectors.OnboardingSelectors

/**
 * Converted Onboarding card smoke tests (Phase A).
 *
 * Launches with onboarding enabled via BaseTest(skipOnboarding = false). Live card order (from
 * onboarding.fml.yaml) is: Terms of Use → Default Browser.
 *
 * mozVerify requires the card to be *displayed*, so each test walks to its card. Advancing uses the
 * standard mozClick(...) on the shared "Continue"/"Not now" controls — resolve() clicks the match on
 * the currently-visible page (the cards share button text and sit in a HorizontalPager). We advance
 * the optional card with "Not now" so we don't trigger its action (set-default dialog).
 */
class OnboardingTest : BaseTest(skipOnboarding = false) {

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3349493
    @SmokeTest
    @Test
    fun verifyTheTermsOfUseOnboardingCardTest() {
        on.onboarding.navigateToPage()
            .mozVerify(OnboardingSelectors.TERMS_OF_USE_TITLE)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3349494
    // Verifies the default-browser card is shown (its title renders as "Open all your links…").
    @SmokeTest
    @Test
    fun verifyTheSetAsDefaultBrowserOnboardingCardTest() {
        on.onboarding.navigateToPage()
            .mozVerify(OnboardingSelectors.TERMS_OF_USE_TITLE)
            .mozClick(OnboardingSelectors.CONTINUE_BUTTON)
            .mozVerify(OnboardingSelectors.DEFAULT_BROWSER_TITLE)
    }
}
