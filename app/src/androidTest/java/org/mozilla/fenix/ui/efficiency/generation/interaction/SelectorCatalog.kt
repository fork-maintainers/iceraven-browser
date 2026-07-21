package org.mozilla.fenix.ui.efficiency.generation.interaction

import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.selectors.BookmarksSelectors

data class SelectorRef(
    val pagePropertyName: String,
    val selectorName: String,
    val selector: Selector,
)

object SelectorCatalog {

    private val registry: Map<String, List<SelectorRef>> = mapOf(
        "bookmarks" to buildRefs("bookmarks", BookmarksSelectors.all),
        // "home" to buildRefs("home", HomeSelectors.all),
        // "browserPage" to buildRefs("browserPage", BrowserSelectors.all),
    )

    fun discoverSelectorsForPage(pagePropertyName: String): List<SelectorRef> {
        return registry[normalize(pagePropertyName)] ?: emptyList()
    }

    private fun buildRefs(
        pagePropertyName: String,
        selectors: List<Selector>,
    ): List<SelectorRef> {
        return selectors.map { selector ->
            SelectorRef(
                pagePropertyName = pagePropertyName,
                selectorName = selector.name ?: inferName(selector),
                selector = selector,
            )
        }
    }

    private fun normalize(name: String): String {
        return name.removeSuffix("Page").lowercase()
    }

    /**
     * TEMP: name inference
     * Replace this later with a proper field-name capture (see below).
     */
    private fun inferName(selector: Selector): String {
        return selector.description
            .uppercase()
            .replace(" ", "_")
            .replace("[^A-Z0-9_]".toRegex(), "")
    }
}
