package org.mozilla.fenix.ui.efficiency.generation.interaction

import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.PageContext
import org.mozilla.fenix.ui.efficiency.helpers.Selector

data class InteractionCase(
    val label: String,
    val testRailId: String,
    val page: PageContext.() -> BasePage,
    val interactionSelectorName: String,
    val interactionSelector: Selector,
    val expectedSelectorNames: List<String>,
    val expectedSelectors: List<Selector>,
    val state: String,
) {
    override fun toString(): String = label
}
