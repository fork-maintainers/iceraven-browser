package org.mozilla.fenix.ui.efficiency.generation.interaction

import org.mozilla.fenix.ui.efficiency.generation.NavigationTestPlanner
import org.mozilla.fenix.ui.efficiency.generation.toDisplayLabel
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.PageContext
import org.mozilla.fenix.ui.efficiency.navigation.NavigationRegistry
import kotlin.text.contains

object InteractionTestPlanner {

    data class InteractionCasePlan(
        val pagePropertyName: String,
        val pageName: String,
        val page: PageContext.() -> BasePage,
        val interactionSelectorName: String,
        val interactionDescription: String,
        val expectedGroup: String,
        val expectedSelectorNames: List<String>,
        val pathCount: Int,
        val isRunnable: Boolean,
    )

    fun buildInteractionCases(): List<InteractionCasePlan> {
        return NavigationTestPlanner.buildReachabilityCases()
            .filter { it.propertyName.contains("bookmark", ignoreCase = true) }
            .flatMap { pageCase ->
                val pageName = pageCase.propertyName.toDisplayLabel()
                val pathCount = NavigationRegistry.findAllPaths("AppEntry", pageName).size
                val selectorRefs = SelectorCatalog.discoverSelectorsForPage(pageCase.propertyName)

                selectorRefs
                    .filter { it.selectorName.endsWith("_BUTTON") }
                    .map { button ->
                        val expectedGroup = "resultOf:${button.selectorName}"

                        val expectedSelectors = selectorRefs
                            .filter { expectedGroup in it.selector.groups }
                            .map { it.selectorName }
                            .sorted()

                        InteractionCasePlan(
                            pagePropertyName = pageCase.propertyName,
                            pageName = pageName,
                            page = pageCase.page,
                            interactionSelectorName = button.selectorName,
                            interactionDescription = button.selector.description,
                            expectedGroup = expectedGroup,
                            expectedSelectorNames = expectedSelectors,
                            pathCount = pathCount,
                            isRunnable = expectedSelectors.isNotEmpty(),
                        )
                    }
            }
            .sortedWith(compareBy({ it.pagePropertyName }, { it.interactionSelectorName }))
    }
}
