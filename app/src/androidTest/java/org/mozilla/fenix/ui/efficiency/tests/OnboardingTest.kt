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
 * onboarding.fml.yaml, minus runtime-gated cards):
 *   Terms of Use → Default Browser → Add Search Widget → Sync → Toolbar
 * (the Notification-permission card is runtime-gated and does not appear, so it is not converted.)
 *
 * mozVerify requires the card to be *displayed*, so each test walks to its card. Advancing uses the
 * standard mozClick(...) on the shared "Continue"/"Not now" controls — resolve() clicks the match on
 * the currently-visible page (the cards share button text and sit in a HorizontalPager). We advance
 * the optional cards with "Not now" so we don't trigger their actions (set-default dialog, sync sign-in).
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

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3349495
    @SmokeTest
    @Test
    fun verifyTheFirefoxSearchWidgetOnboardingCardTest() {
        on.onboarding.navigateToPage()
            .mozVerify(OnboardingSelectors.TERMS_OF_USE_TITLE)
            .mozClick(OnboardingSelectors.CONTINUE_BUTTON)
            .mozVerify(OnboardingSelectors.DEFAULT_BROWSER_TITLE)
            .mozClick(OnboardingSelectors.NOT_NOW_BUTTON)
            .mozVerify(OnboardingSelectors.SEARCH_WIDGET_TITLE)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3349496
    @SmokeTest
    @Test
    fun verifyTheStartSyncingOnboardingCardTest() {
        on.onboarding.navigateToPage()
            .mozVerify(OnboardingSelectors.TERMS_OF_USE_TITLE)
            .mozClick(OnboardingSelectors.CONTINUE_BUTTON)
            .mozVerify(OnboardingSelectors.DEFAULT_BROWSER_TITLE)
            .mozClick(OnboardingSelectors.NOT_NOW_BUTTON)
            .mozVerify(OnboardingSelectors.SEARCH_WIDGET_TITLE)
            .mozClick(OnboardingSelectors.NOT_NOW_BUTTON)
            .mozVerify(OnboardingSelectors.SYNC_TITLE)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3349499
    @SmokeTest
    @Test
    fun verifyTheChooseYourAddressBarOnboardingCardTest() {
        on.onboarding.navigateToPage()
            .mozVerify(OnboardingSelectors.TERMS_OF_USE_TITLE)
            .mozClick(OnboardingSelectors.CONTINUE_BUTTON)
            .mozVerify(OnboardingSelectors.DEFAULT_BROWSER_TITLE)
            .mozClick(OnboardingSelectors.NOT_NOW_BUTTON)
            .mozVerify(OnboardingSelectors.SEARCH_WIDGET_TITLE)
            .mozClick(OnboardingSelectors.NOT_NOW_BUTTON)
            .mozVerify(OnboardingSelectors.SYNC_TITLE)
            .mozClick(OnboardingSelectors.NOT_NOW_BUTTON)
            .mozVerify(OnboardingSelectors.TOOLBAR_TITLE)
    }

    // NOTE: verifyTheNotificationsOnboardingCardTest (TestRail 3349498) is intentionally NOT converted —
    // the Notification-permission card is runtime-gated and does not appear in this flow (its title
    // never showed in the screen dumps). Revisit if/when that card is enabled for the test config.
}
