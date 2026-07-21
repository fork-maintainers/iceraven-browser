/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.generation.behavior

/**
 * Runtime data shared across a generated behavior case.
 *
 * Keep this intentionally tiny for now. Behavior tests usually need one or two
 * dynamic values, such as a folder name, bookmark title, URL, or count.
 */
data class BehaviorData(
    val values: Map<String, String>,
) {
    operator fun get(key: String): String = require(key)

    fun require(key: String): String = values[key]
        ?: throw IllegalArgumentException("Missing behavior data key '$key'. Available keys=${values.keys.sorted()}")

    override fun toString(): String = values.entries
        .sortedBy { it.key }
        .joinToString(prefix = "{", postfix = "}") { "${it.key}=${it.value}" }
}

object BehaviorDataCatalog {
    fun buildData(
        feature: String,
        entity: String,
        templateId: String,
        requiredKeys: Set<String>,
    ): BehaviorData {
        val values = requiredKeys.associateWith { key ->
            defaultValueFor(
                key = key,
                feature = feature,
                entity = entity,
                templateId = templateId,
            )
        }

        return BehaviorData(values)
    }

    private fun defaultValueFor(
        key: String,
        feature: String,
        entity: String,
        templateId: String,
    ): String = when (key) {
        "folderName" -> "TAE Folder ${templateId.stableSuffix()}"
        "bookmarkTitle" -> "TAE Bookmark ${templateId.stableSuffix()}"
        "bookmarkUrl" -> "https://example.com/$feature/$entity/${templateId.stableSuffix()}"
        "itemTitle" -> "TAE Item ${templateId.stableSuffix()}"
        else -> "TAE ${key.toDisplayName()} ${templateId.stableSuffix()}"
    }

    private fun String.stableSuffix(): String =
        replace("entity.", "")
            .replace(".", "-")
            .take(32)

    private fun String.toDisplayName(): String =
        replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
