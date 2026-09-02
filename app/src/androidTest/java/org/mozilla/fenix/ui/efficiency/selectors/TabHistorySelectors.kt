/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

object TabHistorySelectors {
    val TAB_HISTORY_LIST = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_ID,
        value = "tabHistoryRecyclerView",
        description = "Tab history list",
        groups = listOf("requiredForPage"),
    )

    // UIAutomator twin of TAB_HISTORY_LIST, for reaching the sheet from a custom tab. Espresso resolves
    // against the resumed activity's hierarchy, which is CustomTabActivity there while the test rule's
    // context is HomeActivity — the ESPRESSO_BY_ID lookup misses and the harness then tries to launch
    // HomeActivity, timing out. UIAutomator matches device-wide, so it is activity-agnostic. This is
    // what the legacy assertion used (itemWithResId("$packageName:id/tabHistoryRecyclerView")).
    val TAB_HISTORY_LIST_UIAUTOMATOR = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "tabHistoryRecyclerView",
        description = "Tab history list (UIAutomator)",
        groups = listOf(),
    )

    @Suppress("ktlint:standard:function-naming")
    fun TAB_HISTORY_ITEM(url: String = "") = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = url,
        description = "Tab history item with URL: $url",
        groups = listOf("tabHistoryItems"),
    )

    val all = listOf(
        TAB_HISTORY_LIST,
        TAB_HISTORY_LIST_UIAUTOMATOR,
        TAB_HISTORY_ITEM(),
    )
}
