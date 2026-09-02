/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.generation.behavior

enum class BehaviorOperation {
    CREATE,
    DELETE,
    CANCEL_DELETE,
}

enum class AssertionKind {
    PRESENT,
    ABSENT,
}

enum class AssertionTarget {
    CREATED_ENTITY,
}

/**
 * Generic behavior pattern that can be applied to any feature/entity pair with
 * matching capabilities and selector templates.
 */
data class BehaviorTemplate(
    val id: String,
    val automationSuffix: String,
    val titlePattern: String,
    val requiredOperations: List<BehaviorOperation>,
    val assertionKind: AssertionKind,
    val assertionTarget: AssertionTarget = AssertionTarget.CREATED_ENTITY,
) {
    fun title(feature: String, entity: String): String = titlePattern
        .replace("{feature}", feature.toDisplayName())
        .replace("{entity}", entity.toDisplayName())

    fun automationId(feature: String, entity: String): String =
        "$feature.$entity.$automationSuffix"

    private fun String.toDisplayName(): String =
        split(".", "_", "-")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
}

object BehaviorTemplateCatalog {
    val all: List<BehaviorTemplate> = listOf(
        BehaviorTemplate(
            id = "entity.create.visible",
            automationSuffix = "create.visible",
            titlePattern = "{feature} - created {entity} is visible",
            requiredOperations = listOf(BehaviorOperation.CREATE),
            assertionKind = AssertionKind.PRESENT,
        ),
        BehaviorTemplate(
            id = "entity.delete.cancel.preservesEntity",
            automationSuffix = "delete.cancel.preserves",
            titlePattern = "{feature} - cancel delete preserves {entity}",
            requiredOperations = listOf(
                BehaviorOperation.CREATE,
                BehaviorOperation.CANCEL_DELETE,
            ),
            assertionKind = AssertionKind.PRESENT,
        ),
        BehaviorTemplate(
            id = "entity.delete.absent",
            automationSuffix = "delete.absent",
            titlePattern = "{feature} - deleted {entity} is absent",
            requiredOperations = listOf(
                BehaviorOperation.CREATE,
                BehaviorOperation.DELETE,
            ),
            assertionKind = AssertionKind.ABSENT,
        ),
    )
}
