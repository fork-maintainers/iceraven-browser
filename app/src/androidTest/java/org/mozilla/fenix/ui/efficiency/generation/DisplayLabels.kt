/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.generation

/**
 * Converts a page-object property name (e.g. "bookmarksPage") into its display label
 * (e.g. "BookmarksPage"), used for case labels and NavigationRegistry lookups.
 *
 * Shared across all four generator domains (P2b-3, 2026-07-17) — previously copy-pasted
 * identically into ReachabilityCaseFactory, PairCaseFactory, NavigationTestPlanner,
 * and InteractionTestPlanner.
 */
fun String.toDisplayLabel(): String {
    val name = replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase() else char.toString()
    }

    return if (name.endsWith("Page") || name.endsWith("Component")) {
        name
    } else {
        "${name}Page"
    }
}
