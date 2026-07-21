package org.mozilla.fenix.ui.efficiency.generation.reachability

import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.PageContext

data class ReachabilityCase(
    val label: String,
    val testRailId: String,
    val page: PageContext.() -> BasePage,
    val state: String = "",
) {
    override fun toString(): String =
        "$label ($testRailId)${if (state.isNotBlank()) " — $state" else ""}"
}
