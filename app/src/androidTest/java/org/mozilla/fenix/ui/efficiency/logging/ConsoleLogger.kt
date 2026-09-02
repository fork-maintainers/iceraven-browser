/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.logging

import android.util.Log

/**
 * ConsoleLogger
 *
 * Purpose:
 * - Produce a human-optimized, structured log stream for local dev and CI artifacts.
 *
 * Why this exists (vs. plain logcat):
 * - logcat is powerful but noisy, and its signal-to-noise is especially poor when debugging
 *   complex automation harnesses and dynamic test factories.
 * - This logger produces a single, consistent "story" of what the test is doing at each layer:
 *      STEP  -> high-level intent (e.g., navigate to BookmarksPage)
 *      CMD   -> custom command / helper (e.g., click menu item)
 *      LOC   -> element lookup / verification (e.g., locate toolbar title)
 *
 * What encodes meaning:
 * - The level tag ([STEP]/[CMD]/[LOC]/[OK]/[ERR]/[INFO]), a per-level glyph, and indentation depth.
 *   These are readable on their own, including for developers with dyslexia or color blindness.
 * - Color is intentionally NOT emitted here. It is a *view-time* concern applied by the `effpretty`
 *   renderer (ui/efficiency/devtools/effpretty/), which uses a colorblind-safe palette. Keeping the
 *   emitted stream plain means the raw logcat artifact — including the one downloaded from Firebase —
 *   stays clean and machine-parseable, and there is a single structured source that many front-ends
 *   (the effpretty CLI today, a dev-tools GUI later) can render however they like.
 *
 * Consumption model:
 * - Lines are emitted via Logcat under the dedicated tag "Eff" (not the shared System.out tag, which
 *   is a junk drawer any app/library println lands in — the reason filtering used to be noisy).
 *   Filter with a clean allow list of dedicated tags (effpretty's efflog/efflast wrappers do this):
 *
 *     adb logcat -v time Eff:I EffScreenDump:I PageNavigation:I BaseTest:I TestRunner:I AndroidRuntime:E '*:S'
 */
object ConsoleLogger {
    /** Dedicated Logcat tag so the structured stream can be filtered without System.out noise. */
    private const val TAG = "Eff"

    private fun indent(level: Int): String = "    ".repeat(level.coerceAtLeast(0))

    /**
     * Low-level emission primitive. Emits one plain structured line on the "Eff" tag.
     *
     * Keep this dumb: the "meaning" lives in TimedReporter, and presentation lives in effpretty.
     * That makes it easy to change the output transport (logcat, file, JSON) without touching callers.
     */
    fun line(tag: String, icon: String, level: Int, msg: String) {
        Log.i(TAG, "${indent(level)}[$tag] $icon $msg")
    }

    // Start-of-scope messages: intentionally "Attempting..." and typically end with "...".
    fun step(level: Int, msg: String) = line("STEP", "★", level, msg)
    fun cmd(level: Int, msg: String) = line("CMD", "➤", level, msg)
    fun loc(level: Int, msg: String) = line("LOC", "🔎", level, msg)

    /**
     * SEL is reserved for "selector strategy / parsing" messages once we decide to surface them
     * (e.g. COMPOSE_BY_TAG vs ESPRESSO_BY_ID, resource-id parsing failures, etc.).
     */
    fun sel(level: Int, msg: String) = line("SEL", "→", level, msg)

    /**
     * Completion messages.
     *
     * Design rule: OK/ERR messages should contain *only* the outcome (confirmation/error), not a
     * repeat of the start message — the start message is already logged when the scope begins.
     */
    fun ok(level: Int, msg: String) = line("OK", "✔", level, msg)
    fun err(level: Int, msg: String) = line("ERR", "✖", level, msg)
    fun skip(level: Int, msg: String) = line("SKIP", "⊘", level, msg)

    fun info(level: Int, msg: String) = line("INFO", "•", level, msg)
}
