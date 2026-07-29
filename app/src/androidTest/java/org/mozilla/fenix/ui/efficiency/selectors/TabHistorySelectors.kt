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

    @Suppress("ktlint:standard:function-naming")
    fun TAB_HISTORY_ITEM(url: String = "") = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = url,
        description = "Tab history item with URL: $url",
        groups = listOf("tabHistoryItems"),
    )

    val all = listOf(
        TAB_HISTORY_LIST,
        TAB_HISTORY_ITEM(),
    )
}
