/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.devtools

import org.mozilla.fenix.ui.efficiency.generation.NavigationGraphBootstrap
import org.mozilla.fenix.ui.efficiency.generation.behavior.BehaviorCasePlan
import org.mozilla.fenix.ui.efficiency.generation.behavior.BehaviorContextManifest
import org.mozilla.fenix.ui.efficiency.generation.behavior.BehaviorContextMatrix
import org.mozilla.fenix.ui.efficiency.generation.behavior.BehaviorContextSupportRegistry
import org.mozilla.fenix.ui.efficiency.generation.behavior.BehaviorMatrixProfile
import org.mozilla.fenix.ui.efficiency.generation.behavior.BehaviorRunArguments
import org.mozilla.fenix.ui.efficiency.generation.behavior.BehaviorTestPlanner

object BehaviorCaseGenerator {
    private const val TAG = "BehaviorGenerator"

    /**
     * Existing API.
     *
     * Keep this because it matches the Navigation/Interaction generator pattern:
     * dump boilerplate/templates first, execute later.
     */
    fun logBehaviorCaseBoilerplate(
        includeSkipped: Boolean = true,
        profile: BehaviorMatrixProfile = BehaviorRunArguments.matrixProfile(),
    ) {
        val plans = buildPlans(profile)
        logBehaviorCaseBoilerplate(
            plans = plans,
            includeSkipped = includeSkipped,
        )
    }

    /**
     * Alias for readability in tests.
     *
     * Use this when the test name is "logBehaviorCasePlans".
     * Internally it still prints boilerplate.
     */
    fun logBehaviorCasePlans(
        profile: BehaviorMatrixProfile = BehaviorRunArguments.matrixProfile(),
        includeSkipped: Boolean = true,
    ) {
        logBehaviorCaseBoilerplate(
            includeSkipped = includeSkipped,
            profile = profile,
        )
    }

    fun logBehaviorPlanSummary(
        profile: BehaviorMatrixProfile = BehaviorRunArguments.matrixProfile(),
    ) {
        val plans = buildPlans(profile)
        logBehaviorPlanSummary(plans)
    }

    fun logBehaviorMatrixSummary(
        profile: BehaviorMatrixProfile = BehaviorRunArguments.matrixProfile(),
    ) {
        val plans = buildPlans(profile)
        logBehaviorMatrixSummary(plans)
    }

    /**
     * Builds the behavior matrix summary report without emitting it anywhere, so a future
     * consumer (a debug UI, an export, etc.) can reuse the same computation
     * [logBehaviorMatrixSummary] uses.
     */
    fun buildBehaviorMatrixSummaryReport(
        plans: List<BehaviorCasePlan>,
    ): DevToolReport {
        val byContext = plans.groupBy { it.context.toString() }
        val byFeatureEntity = plans.groupBy { "${it.feature}.${it.entity}" }
        val byTemplate = plans.groupBy { it.templateId }

        val skippedByReason = plans
            .mapNotNull { plan ->
                plan.skippedReason?.let { reason ->
                    reason to plan.automationId
                }
            }
            .groupBy({ it.first }, { it.second })

        val missingGroups = plans
            .flatMap { plan ->
                plan.missingRequirements.map { missing ->
                    missing to plan.automationId
                }
            }
            .groupBy({ it.first }, { it.second })

        val lines = buildList {
            if (skippedByReason.isNotEmpty()) {
                add("")
                add("Skipped by context support:")
                skippedByReason.entries
                    .sortedBy { it.key }
                    .forEach { (reason, automationIds) ->
                        val distinctAutomationIds = automationIds.distinct().sorted()

                        add("  - $reason")
                        distinctAutomationIds
                            .take(10)
                            .forEach { automationId ->
                                add("      used by $automationId")
                            }

                        if (distinctAutomationIds.size > 10) {
                            add("      ...and ${distinctAutomationIds.size - 10} more")
                        }
                    }
            }

            add("Behavior matrix summary")
            add("--------------------------------------------------")
            add("Total plans=${plans.size}")
            add("Runnable=${plans.count { it.isRunnable }}")
            add("Skipped=${plans.count { !it.isRunnable }}")
            add("Contexts=${byContext.size}")
            add("Feature/entities=${byFeatureEntity.size}")
            add("Templates=${byTemplate.size}")

            add("")
            add("By context:")
            byContext.entries
                .sortedBy { it.key }
                .forEach { (context, contextPlans) ->
                    add(
                        "  - $context: " +
                            "${contextPlans.count { it.isRunnable }}/${contextPlans.size} runnable",
                    )
                }

            add("")
            add("By feature/entity:")
            byFeatureEntity.entries
                .sortedBy { it.key }
                .forEach { (key, featurePlans) ->
                    add(
                        "  - $key: " +
                            "${featurePlans.count { it.isRunnable }}/${featurePlans.size} runnable",
                    )
                }

            add("")
            add("By template:")
            byTemplate.entries
                .sortedBy { it.key }
                .forEach { (templateId, templatePlans) ->
                    add(
                        "  - $templateId: " +
                            "${templatePlans.count { it.isRunnable }}/${templatePlans.size} runnable",
                    )
                }

            if (missingGroups.isNotEmpty()) {
                add("")
                add("Missing requirements:")
                missingGroups.entries
                    .sortedBy { it.key }
                    .forEach { (missingRequirement, automationIds) ->
                        add("  - $missingRequirement")
                        automationIds
                            .distinct()
                            .sorted()
                            .take(10)
                            .forEach { automationId ->
                                add("      used by $automationId")
                            }

                        if (automationIds.distinct().size > 10) {
                            add("      ...and ${automationIds.distinct().size - 10} more")
                        }
                    }
            }

            add("--------------------------------------------------")
        }

        return DevToolReport(lines = lines)
    }

    fun logBehaviorMatrixSummary(
        plans: List<BehaviorCasePlan>,
    ) {
        logReport(TAG, buildBehaviorMatrixSummaryReport(plans))
    }

    /**
     * Builds the behavior plan summary report without emitting it anywhere, so a future
     * consumer (a debug UI, an export, etc.) can reuse the same computation
     * [logBehaviorPlanSummary] uses.
     */
    fun buildBehaviorPlanSummaryReport(
        plans: List<BehaviorCasePlan>,
    ): DevToolReport {
        val grouped = plans.groupBy { it.feature to it.entity }

        val lines = buildList {
            add("Behavior plan summary")
            add("--------------------------------------------------")

            grouped.entries
                .sortedWith(
                    compareBy(
                        { it.key.first },
                        { it.key.second },
                    ),
                )
                .forEach { (featureEntity, featurePlans) ->
                    val runnable = featurePlans.count { it.isRunnable }

                    add(
                        "${featureEntity.first}.${featureEntity.second}: " +
                            "$runnable/${featurePlans.size} runnable",
                    )

                    featurePlans
                        .sortedWith(compareBy({ it.templateId }, { it.context.toString() }))
                        .forEach { plan ->
                            add(
                                "  - ${plan.automationId} " +
                                    "runnable=${plan.isRunnable} " +
                                    "context=${plan.context} " +
                                    "missing=${plan.missingRequirements} " +
                                    "skippedReason=${plan.skippedReason}",
                            )
                        }
                }

            add("--------------------------------------------------")
        }

        return DevToolReport(lines = lines)
    }

    fun logBehaviorPlanSummary(
        plans: List<BehaviorCasePlan>,
    ) {
        logReport(TAG, buildBehaviorPlanSummaryReport(plans))
    }

    /**
     * Builds the behavior case boilerplate report without emitting it anywhere, so a future
     * consumer (a debug UI, an export, etc.) can reuse the same computation
     * [logBehaviorCaseBoilerplate] uses.
     */
    fun buildBehaviorCaseBoilerplateReport(
        plans: List<BehaviorCasePlan>,
        includeSkipped: Boolean = true,
    ): DevToolReport {
        val runnable = plans.count { it.isRunnable }
        val skipped = plans.size - runnable

        val filteredPlans = plans
            .filter { includeSkipped || it.isRunnable }
            .sortedWith(compareBy({ it.feature }, { it.entity }, { it.templateId }, { it.context.toString() }))

        return buildBoilerplateReport(
            header = "Generated ${plans.size} behavior case plans: runnable=$runnable skipped=$skipped",
            items = filteredPlans,
        ) { plan -> plan.toBoilerplateBlock() }
    }

    fun logBehaviorCaseBoilerplate(
        plans: List<BehaviorCasePlan>,
        includeSkipped: Boolean = true,
    ) {
        logReport(TAG, buildBehaviorCaseBoilerplateReport(plans, includeSkipped))
    }

    private fun buildPlans(
        profile: BehaviorMatrixProfile,
    ): List<BehaviorCasePlan> {
        NavigationGraphBootstrap.ensureInitialized()

        return BehaviorTestPlanner.buildBehaviorCasePlans(
            contexts = BehaviorContextMatrix.variants(profile),
        )
    }

    private fun BehaviorCasePlan.toBoilerplateBlock(): String {
        val capabilityBlock = if (capabilityIds.isEmpty()) {
            "emptyList()"
        } else {
            capabilityIds.joinToString(
                separator = ",\n                    ",
                prefix = "listOf(\n                    ",
                postfix = ",\n                )",
            ) { "\"$it\"" }
        }

        val requiredOperationsBlock = if (requiredOperations.isEmpty()) {
            "emptyList()"
        } else {
            requiredOperations.joinToString(
                separator = ", ",
                prefix = "listOf(",
                postfix = ")",
            ) { "BehaviorOperation.$it" }
        }

        val dataBlock = data.values.entries
            .sortedBy { it.key }
            .joinToString(
                separator = ", ",
                prefix = "mapOf(",
                postfix = ")",
            ) { "\"${it.key}\" to \"${it.value}\"" }

        val contextBlock = context.values.entries
            .sortedBy { it.key }
            .joinToString(
                separator = ", ",
                prefix = "mapOf(",
                postfix = ")",
            ) { "\"${it.key}\" to \"${it.value}\"" }

        val navigationTransitionsBlock = navigationTransitions.joinToString(
            separator = ", ",
            prefix = "listOf(",
            postfix = ")",
        ) { "\"$it\"" }

        val missingRequirementsBlock = missingRequirements.joinToString(
            separator = ", ",
            prefix = "listOf(",
            postfix = ")",
        ) { "\"$it\"" }

        val skippedReasonBlock = skippedReason?.let {
            "\"${it.escapeForKotlin()}\""
        } ?: "null"

        return """
            // automationId=$automationId
            // feature=$feature entity=$entity template=$templateId
            // runnable=$isRunnable
            // missingRequirements=$missingRequirements
            // skippedReason=$skippedReason
            // requiredOperations=$requiredOperations
            // capabilities=$capabilityIds
            // assertion=$assertionKind selectorTemplate=$assertionSelectorTemplateId page=$assertionPagePropertyName
            // data=$data
            // context=$context
            // navigationTransitions=$navigationTransitions
            // TestRail draft:
            //   Title: $label
            //   Preconditions: generated data $data
            //   Steps: ${capabilityIds.joinToString(" -> ")}
            //   Expected: $assertionKind via $assertionSelectorTemplateId

            BehaviorCasePlan(
                label = "$label",
                testRailId = "TBD",
                automationId = "$automationId",
                feature = "$feature",
                entity = "$entity",
                templateId = "$templateId",
                requiredOperations = $requiredOperationsBlock,
                capabilityIds = $capabilityBlock,
                assertionKind = AssertionKind.$assertionKind,
                assertionSelectorTemplateId = ${assertionSelectorTemplateId?.let { "\"$it\"" }},
                assertionPagePropertyName = ${assertionPagePropertyName?.let { "\"$it\"" }},
                data = BehaviorData(values = $dataBlock),
                context = BehaviorContextVariant(values = $contextBlock),
                navigationTransitions = $navigationTransitionsBlock,
                missingRequirements = $missingRequirementsBlock,
                skippedReason = $skippedReasonBlock,
            )
        """.trimIndent()
    }

    private fun String.escapeForKotlin(): String {
        return replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
    }

    /**
     * Builds the behavior context manifest report without emitting it anywhere, so a future
     * consumer (a debug UI, an export, etc.) can reuse the same computation
     * [logBehaviorContextManifest] uses.
     */
    fun buildBehaviorContextManifestReport(): DevToolReport {
        val lines = buildList {
            add("Behavior context manifest")
            add("--------------------------------------------------")

            BehaviorContextManifest.all
                .sortedWith(compareBy({ it.key }, { it.value }))
                .forEach { spec ->
                    add(
                        "${spec.id} " +
                            "status=${spec.status} " +
                            "applicationMode=${spec.applicationMode} " +
                            "reason=${spec.reason}",
                    )
                }

            add("--------------------------------------------------")
        }

        return DevToolReport(lines = lines)
    }

    fun logBehaviorContextManifest() {
        logReport(TAG, buildBehaviorContextManifestReport())
    }

    /**
     * Builds the behavior state machine report without emitting it anywhere, so a future
     * consumer (a debug UI, an export, etc.) can reuse the same computation
     * [logBehaviorStateMachine] uses.
     */
    fun buildBehaviorStateMachineReport(): DevToolReport {
        val lines = buildList {
            add("Behavior state machine")
            add("--------------------------------------------------")

            BehaviorMatrixProfile.values().forEach { profile ->
                val contexts = BehaviorContextMatrix.variants(profile)
                val decisions = contexts.map { context ->
                    context to BehaviorContextSupportRegistry.evaluate(context)
                }

                add("Profile=$profile")
                add("  contexts=${contexts.size}")
                add("  runnable=${decisions.count { it.second.isSupported }}")
                add("  todo=${decisions.count { !it.second.isSupported }}")

                decisions.forEach { (context, decision) ->
                    add(
                        "    - $context " +
                            "supported=${decision.isSupported}" +
                            decision.skippedReason?.let { " reason=$it" }.orEmpty(),
                    )
                }

                add("")
            }

            add("Manifest:")
            BehaviorContextManifest.all
                .sortedWith(compareBy({ it.key }, { it.value }))
                .forEach { spec ->
                    add(
                        "  - ${spec.id} " +
                            "status=${spec.status} " +
                            "applicationMode=${spec.applicationMode} " +
                            "reason=${spec.reason}",
                    )
                }

            add("--------------------------------------------------")
        }

        return DevToolReport(lines = lines)
    }

    fun logBehaviorStateMachine() {
        logReport(TAG, buildBehaviorStateMachineReport())
    }
}
