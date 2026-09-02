package org.mozilla.fenix.ui.efficiency.generation.pairs

import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.PageContext

data class PairCase(
    val label: String,
    val testRailId: String,
    val firstPage: PageContext.() -> BasePage,
    val secondPage: PageContext.() -> BasePage,
    val state: String,
) {
    override fun toString(): String = label
}
