/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

object PWASelectors {

    val PWA_SCREEN = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "engineView",
        description = "PWA screen",
        groups = listOf("requiredForPage"),
    )

    // The browser nav/URL bar. Present in the normal browser, absent when a PWA runs standalone — so its
    // absence is the "opened as an installed app" signal (mirrors legacy PwaTest.verifyNavURLBarHidden).
    val NAV_URL_BAR = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "toolbar",
        description = "Browser navigation URL bar",
        groups = listOf(),
    )

    val all = listOf(
        PWA_SCREEN,
        NAV_URL_BAR,
    )
}
