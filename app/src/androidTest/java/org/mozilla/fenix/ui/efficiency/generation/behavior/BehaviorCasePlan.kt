/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.generation.behavior

/** Non-executing plan produced before runtime lambdas are resolved. */
data class BehaviorCasePlan(
    val label: String,
    val testRailId: String,
    val automationId: String,
    val feature: String,
    val entity: String,
    val templateId: String,
    val requiredOperations: List<BehaviorOperation>,
    val capabilityIds: List<String>,
    val assertionKind: AssertionKind,
    val assertionSelectorTemplateId: String?,
    val assertionPagePropertyName: String?,
    val data: BehaviorData,
    val context: BehaviorContextVariant,
    val navigationTransitions: List<String>,
    val missingRequirements: List<String> = emptyList(),
    val skippedReason: String? = null,
) {
    val isRunnable: Boolean
        get() = missingRequirements.isEmpty() && skippedReason == null

    override fun toString(): String =
        "$label automationId=$automationId runnable=$isRunnable missing=$missingRequirements"
}
