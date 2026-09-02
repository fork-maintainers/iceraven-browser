package org.mozilla.fenix.ui.efficiency.generation.reachability

import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.PageContext
import org.mozilla.fenix.ui.efficiency.navigation.LaunchConfig

data class ReachabilityCase(
    val label: String,
    val testRailId: String,
    val page: PageContext.() -> BasePage,
    val state: String = "",
    val launch: LaunchConfig = LaunchConfig(),
) {
    override fun toString(): String =
        "$label ($testRailId)${if (state.isNotBlank()) " — $state" else ""}"
}
