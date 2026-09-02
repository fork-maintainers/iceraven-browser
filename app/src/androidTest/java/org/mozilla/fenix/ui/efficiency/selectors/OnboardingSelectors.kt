/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

/**
 * Selectors for the first-run Onboarding flow (Compose cards shown when the app launches with
 * onboarding enabled — BaseTest(skipOnboarding = false)).
 *
 * Verified against onboarding.fml.yaml + on-device dumps. The live card order (by `ordering`) is:
 *   Terms of Use → Default Browser.
 *
 * Titles are unique text, matched by COMPOSE_BY_TEXT (the title nodes expose no testTag/id). NOTE the
 * resource names are misleading: `set_to_default_title_2` renders as "Open all your links with built-in
 * privacy" (the default-browser card's title); "Set as default browser" is that card's *button*.
 *
 * The advance buttons ("Continue" / "Not now") share text across cards, and the cards sit in a
 * HorizontalPager (all composed at once), so they can't be matched by a single text or a merged-tree
 * testTag. Instead the tests advance via BasePage.mozClickDisplayed(...), which clicks the one
 * currently-*displayed* match.
 */
object OnboardingSelectors {

    // --- Card titles (unique text; verified visible only when on that page) ---
    val TERMS_OF_USE_TITLE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.onboarding_welcome_to_firefox), // "Welcome to Firefox"
        description = "Terms of Use onboarding card title",
        groups = listOf("requiredForPage", "termsOfUseCard"),
    )

    val DEFAULT_BROWSER_TITLE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.nova_onboarding_set_to_default_title_2), // "Open all your links with built-in privacy"
        description = "Default browser onboarding card title",
        groups = listOf("defaultBrowserCard"),
    )

    // --- Advance controls (shared text across cards; click via mozClickDisplayed) ---
    val CONTINUE_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.nova_onboarding_continue_button), // "Continue"
        description = "Onboarding Continue button (current card)",
        groups = listOf("continueButton"),
    )

    val NOT_NOW_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.nova_onboarding_negative_button), // "Not now"
        description = "Onboarding 'Not now' skip button (current card)",
        groups = listOf("notNowButton"),
    )

    val all = listOf(
        TERMS_OF_USE_TITLE,
        DEFAULT_BROWSER_TITLE,
        CONTINUE_BUTTON,
        NOT_NOW_BUTTON,
    )
}
