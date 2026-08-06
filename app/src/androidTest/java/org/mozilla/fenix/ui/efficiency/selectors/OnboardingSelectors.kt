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
 * Verified against onboarding.fml.yaml + on-device dumps. The live card order (by `ordering`, minus
 * runtime-gated cards) is:
 *   Terms of Use → Default Browser → Add Search Widget → Sync → [Notification: gated/absent] → Toolbar
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

    val SEARCH_WIDGET_TITLE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.nova_onboarding_add_search_widget_title), // "Make every search more private"
        description = "Add search widget onboarding card title",
        groups = listOf("searchWidgetCard"),
    )

    val SYNC_TITLE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.nova_onboarding_sync_title), // "Instantly pick up where you left off"
        description = "Sync (sign-in) onboarding card title",
        groups = listOf("syncCard"),
    )

    val TOOLBAR_TITLE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.nova_onboarding_toolbar_selection_title), // "Choose your address bar"
        description = "Toolbar placement onboarding card title",
        groups = listOf("toolbarCard"),
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
        SEARCH_WIDGET_TITLE,
        SYNC_TITLE,
        TOOLBAR_TITLE,
        CONTINUE_BUTTON,
        NOT_NOW_BUTTON,
    )
}
