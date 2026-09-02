/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

object FindInPageSelectors {

    // The bar container itself — used to assert find-in-page is gone after it is closed.
    val FIND_IN_PAGE_BAR = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "findInPageView",
        description = "Find in page bar",
        groups = listOf(),
    )

    val FIND_IN_PAGE_CLOSE_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "find_in_page_close_btn",
        description = "Find in page close button",
        groups = listOf("requiredForPage"),
    )

    val FIND_IN_PAGE_QUERY = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_RES_NAME,
        value = "find_in_page_query_text",
        description = "Find in page query input",
        groups = listOf("findInPage"),
    )

    val FIND_IN_PAGE_NEXT_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "find_in_page_next_btn",
        description = "Find in page next result button",
        groups = listOf("requiredForPage"),
    )

    val FIND_IN_PAGE_PREV_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "find_in_page_prev_btn",
        description = "Find in page previous result button",
        groups = listOf("requiredForPage"),
    )

    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun RESULT_COUNTER(text: String = "") = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = text,
        description = "Find in page result counter '$text'",
        groups = listOf("findInPage"),
    )

    val all = listOf(
        FIND_IN_PAGE_BAR,
        FIND_IN_PAGE_CLOSE_BUTTON,
        FIND_IN_PAGE_QUERY,
        FIND_IN_PAGE_NEXT_BUTTON,
        FIND_IN_PAGE_PREV_BUTTON,
        RESULT_COUNTER(),
    )
}
