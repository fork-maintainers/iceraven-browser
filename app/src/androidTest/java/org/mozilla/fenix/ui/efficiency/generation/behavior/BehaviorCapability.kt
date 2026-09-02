/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.generation.behavior

import org.mozilla.fenix.ui.efficiency.selectors.BookmarksSelectors

/**
 * A reusable, machine-readable page-object action.
 *
 * This is the unit the Behavior factory composes. It should map to a meaningful
 * user action/test step, not to every individual primitive command.
 */
data class BehaviorCapability(
    val id: String,
    val feature: String,
    val entity: String,
    val operation: BehaviorOperation,
    val pagePropertyName: String,
    val requiredDataKeys: Set<String> = emptySet(),
    val description: String,
    val action: BehaviorRuntime.() -> Unit,
) {
    fun perform(runtime: BehaviorRuntime) {
        runtime.action()
    }
}

object BehaviorCapabilityCatalog {
    val all: List<BehaviorCapability> = listOf(
        BehaviorCapability(
            id = "bookmarks.folder.create",
            feature = "bookmarks",
            entity = "folder",
            operation = BehaviorOperation.CREATE,
            pagePropertyName = "bookmarks",
            requiredDataKeys = setOf("folderName"),
            description = "Create a bookmark folder using the Bookmarks page UI",
            action = {
                on.bookmarks.navigateToPage()
                on.bookmarks.createFolder(data.require("folderName"))
            },
        ),
        BehaviorCapability(
            id = "bookmarks.folder.cancelDelete",
            feature = "bookmarks",
            entity = "folder",
            operation = BehaviorOperation.CANCEL_DELETE,
            pagePropertyName = "bookmarks",
            requiredDataKeys = setOf("folderName"),
            description = "Start deleting a bookmark folder, then cancel the confirmation dialog",
            action = {
                on.bookmarks.navigateToPage()
                on.bookmarks.openItemMenu(data.require("folderName"))
                on.bookmarks.mozClick(BookmarksSelectors.DELETE_BUTTON)
                on.bookmarks.mozClick(BookmarksSelectors.CANCEL_FOLDER_DELETION_BUTTON)
            },
        ),
        BehaviorCapability(
            id = "bookmarks.folder.delete",
            feature = "bookmarks",
            entity = "folder",
            operation = BehaviorOperation.DELETE,
            pagePropertyName = "bookmarks",
            requiredDataKeys = setOf("folderName"),
            description = "Delete a bookmark folder and confirm the deletion dialog",
            action = {
                on.bookmarks.navigateToPage()
                on.bookmarks.openItemMenu(data.require("folderName"))
                on.bookmarks.mozClick(BookmarksSelectors.DELETE_BUTTON)
                on.bookmarks.mozClick(BookmarksSelectors.DELETE_BUTTON)
            },
        ),
    )

    fun findById(id: String): BehaviorCapability? = all.firstOrNull { it.id == id }
}
