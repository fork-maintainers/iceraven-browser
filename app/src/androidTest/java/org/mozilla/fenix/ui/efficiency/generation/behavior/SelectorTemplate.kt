/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.generation.behavior

import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.selectors.BookmarksSelectors

/**
 * Dynamic selector factory used when behavior assertions depend on runtime data.
 */
data class SelectorTemplate(
    val id: String,
    val feature: String,
    val entity: String,
    val target: AssertionTarget,
    val pagePropertyName: String,
    val requiredDataKeys: Set<String>,
    val description: String,
    val build: (BehaviorData) -> Selector,
)

object SelectorTemplateCatalog {
    val all: List<SelectorTemplate> = listOf(
        SelectorTemplate(
            id = "bookmarks.folder.item",
            feature = "bookmarks",
            entity = "folder",
            target = AssertionTarget.CREATED_ENTITY,
            pagePropertyName = "bookmarks",
            requiredDataKeys = setOf("folderName"),
            description = "Bookmark folder row matching the generated folder name",
            build = { data -> BookmarksSelectors.BOOKMARK_ITEM(data.require("folderName")) },
        ),
    )

    fun find(
        feature: String,
        entity: String,
        target: AssertionTarget,
    ): SelectorTemplate? = all.firstOrNull {
        it.feature == feature && it.entity == entity && it.target == target
    }

    fun findById(id: String): SelectorTemplate? = all.firstOrNull { it.id == id }
}
