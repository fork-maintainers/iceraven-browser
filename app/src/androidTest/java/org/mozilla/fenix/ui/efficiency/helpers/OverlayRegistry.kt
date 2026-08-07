/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.helpers

/**
 * OverlayRegistry — known, dismissable overlays that can appear ON TOP of the app in a separate
 * window and silently block interaction (so a `moz*` verb reports "element not found" even though the
 * target is on screen, just covered).
 *
 * These are environment/system surfaces the test never asked for: OEM/system prompts, IME tutorials,
 * etc. The harness owns handling them so individual tests don't each re-implement the dismissal.
 * BasePage consults this registry on a locate miss (see [BasePage.dismissKnownOverlaysIfPresent]) and,
 * if it dismisses one, retries the failed step once.
 *
 * To add an overlay: give a [BlockingOverlay.presence] selector that uniquely detects it, and one or
 * more [BlockingOverlay.dismiss] selectors, which are tried in order until the overlay stops being
 * detected. Keep them UIAutomator-based — these windows are NOT in the app's Compose tree.
 *
 * Scope note: this is deliberately the simplest thing that handles the one overlay we have actually hit.
 * It has never run with several overlays registered, and the following are unresolved — settle them
 * before growing [known] much beyond a single entry:
 *  - Ordering/interaction when two overlays are stacked (dismissing one may reveal another; the current
 *    single pass over [known] won't re-check entries it already visited).
 *  - Cost: every registered overlay is presence-probed on every locate miss, on the failure path of
 *    every moz* verb.
 *  - Whether a dismiss that silently fails should escalate (throw / report) rather than let the caller
 *    re-probe and report a generic "element not found".
 */
object OverlayRegistry {

    data class BlockingOverlay(
        val name: String,
        val presence: Selector,
        val dismiss: List<Selector>,
    )

    /**
     * Android stylus-handwriting prompt ("Try out your stylus"): raised in a separate window when a
     * (web or native) text field is focused on devices with stylus handwriting enabled. It covers the
     * page and suppresses e.g. the address-autofill prompt. Its dismiss control is res-id `closeButton`
     * whose text/desc are both "Cancel" (its `button2` is "Next" — NOT the cancel), so we match on the
     * "Cancel" text, which is stable across the obfuscated system res-ids.
     */
    val STYLUS_HANDWRITING_PROMPT = BlockingOverlay(
        name = "Android stylus-handwriting prompt",
        presence = Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
            value = "Try out your stylus",
            description = "Stylus-handwriting prompt title",
        ),
        dismiss = listOf(
            Selector(
                strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
                value = "Cancel",
                description = "Stylus-handwriting prompt Cancel button",
            ),
        ),
    )

    /**
     * "Add to Home screen" prompt. Two triggers, one UI: the automatic PWA onboarding prompt shown on a
     * repeat visit to an installable site (PwaOnboardingObserver, fragment_pwa_onboarding.xml) and the
     * explicit menu "Add app to home screen" dialog (fragment_create_shortcut.xml). Both expose the same
     * res-ids (`add_button`/`cancel_button`), so one entry covers both. The automatic prompt pops over the
     * page uninvited and covers the engine view, breaking navigation in tests that never asked to install
     * anything (hit converting LoginsTest) — so it is dismissed here rather than suppressed per-test with a
     * launch flag. Dismiss is `cancel_button`, which is side-effect-free (it does NOT pin a shortcut); never
     * dismiss via `add_button`. A test that legitimately drives this prompt (e.g. installPWAFromTheMainMenuTest)
     * locates its control successfully, so this fallback — which only fires on a locate miss — never engages.
     */
    val ADD_TO_HOME_SCREEN_PROMPT = BlockingOverlay(
        name = "Add to Home screen prompt",
        presence = Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
            value = "cancel_button",
            description = "Add to Home screen prompt cancel button",
        ),
        dismiss = listOf(
            Selector(
                strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
                value = "cancel_button",
                description = "Add to Home screen prompt cancel button",
            ),
        ),
    )

    val known: List<BlockingOverlay> = listOf(
        STYLUS_HANDWRITING_PROMPT,
        ADD_TO_HOME_SCREEN_PROMPT,
    )
}
