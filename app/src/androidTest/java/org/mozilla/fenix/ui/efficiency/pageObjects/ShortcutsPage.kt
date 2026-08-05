/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.selectors.ShortcutsSelectors

class ShortcutsPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "ShortcutsPage"

    // No navigation edge is registered on purpose. This page previously registered one with an empty
    // step list, which made navigateToPage() a silent no-op that reported success without going
    // anywhere. With no edge at all it fails loudly with "no navigation path found", which is the
    // honest answer until someone needs this page and can supply — and verify — real steps.

    override fun mozGetSelectorsByGroup(group: String): List<Selector> {
        return ShortcutsSelectors.all.filter { it.groups.contains(group) }
    }
}
