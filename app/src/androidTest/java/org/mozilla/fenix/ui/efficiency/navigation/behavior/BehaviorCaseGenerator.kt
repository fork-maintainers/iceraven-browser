/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.navigation.behavior

import android.util.Log
import org.mozilla.fenix.ui.efficiency.navigation.planning.NavigationGraphBootstrap

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

    fun logBehaviorMatrixSummary(
        plans: List<BehaviorCasePlan>,
    ) {
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

        if (skippedByReason.isNotEmpty()) {
            line("")
            line("Skipped by context support:")
            skippedByReason.entries
                .sortedBy { it.key }
                .forEach { (reason, automationIds) ->
                    val distinctAutomationIds = automationIds.distinct().sorted()

                    line("  - $reason")
                    distinctAutomationIds
                        .take(10)
                        .forEach { automationId ->
                            line("      used by $automationId")
                        }

                    if (distinctAutomationIds.size > 10) {
                        line("      ...and ${distinctAutomationIds.size - 10} more")
                    }
                }
        }

        line("Behavior matrix summary")
        line("--------------------------------------------------")
        line("Total plans=${plans.size}")
        line("Runnable=${plans.count { it.isRunnable }}")
        line("Skipped=${plans.count { !it.isRunnable }}")
        line("Contexts=${byContext.size}")
        line("Feature/entities=${byFeatureEntity.size}")
        line("Templates=${byTemplate.size}")

        line("")
        line("By context:")
        byContext.entries
            .sortedBy { it.key }
            .forEach { (context, contextPlans) ->
                line(
                    "  - $context: " +
                        "${contextPlans.count { it.isRunnable }}/${contextPlans.size} runnable",
                )
            }

        line("")
        line("By feature/entity:")
        byFeatureEntity.entries
            .sortedBy { it.key }
            .forEach { (key, featurePlans) ->
                line(
                    "  - $key: " +
                        "${featurePlans.count { it.isRunnable }}/${featurePlans.size} runnable",
                )
            }

        line("")
        line("By template:")
        byTemplate.entries
            .sortedBy { it.key }
            .forEach { (templateId, templatePlans) ->
                line(
                    "  - $templateId: " +
                        "${templatePlans.count { it.isRunnable }}/${templatePlans.size} runnable",
                )
            }

        val missingGroups = plans
            .flatMap { plan ->
                plan.missingRequirements.map { missing ->
                    missing to plan.automationId
                }
            }
            .groupBy({ it.first }, { it.second })

        if (missingGroups.isNotEmpty()) {
            line("")
            line("Missing requirements:")
            missingGroups.entries
                .sortedBy { it.key }
                .forEach { (missingRequirement, automationIds) ->
                    line("  - $missingRequirement")
                    automationIds
                        .distinct()
                        .sorted()
                        .take(10)
                        .forEach { automationId ->
                            line("      used by $automationId")
                        }

                    if (automationIds.distinct().size > 10) {
                        line("      ...and ${automationIds.distinct().size - 10} more")
                    }
                }
        }

        line("--------------------------------------------------")
    }

    fun logBehaviorPlanSummary(
        plans: List<BehaviorCasePlan>,
    ) {
        val grouped = plans.groupBy { it.feature to it.entity }

        line("Behavior plan summary")
        line("--------------------------------------------------")

        grouped.entries
            .sortedWith(
                compareBy(
                    { it.key.first },
                    { it.key.second },
                ),
            )
            .forEach { (featureEntity, featurePlans) ->
                val runnable = featurePlans.count { it.isRunnable }

                line(
                    "${featureEntity.first}.${featureEntity.second}: " +
                        "$runnable/${featurePlans.size} runnable",
                )

                featurePlans
                    .sortedWith(compareBy({ it.templateId }, { it.context.toString() }))
                    .forEach { plan ->
                        line(
                            "  - ${plan.automationId} " +
                                "runnable=${plan.isRunnable} " +
                                "context=${plan.context} " +
                                "missing=${plan.missingRequirements} " +
                                "skippedReason=${plan.skippedReason}",
                        )
                    }
            }

        line("--------------------------------------------------")
    }

    fun logBehaviorCaseBoilerplate(
        plans: List<BehaviorCasePlan>,
        includeSkipped: Boolean = true,
    ) {
        val runnable = plans.count { it.isRunnable }
        val skipped = plans.size - runnable

        line("Generated ${plans.size} behavior case plans: runnable=$runnable skipped=$skipped")
        line("--------------------------------------------------")

        plans
            .filter { includeSkipped || it.isRunnable }
            .sortedWith(compareBy({ it.feature }, { it.entity }, { it.templateId }, { it.context.toString() }))
            .forEach { plan ->
                line(plan.toBoilerplateBlock())
            }

        line("--------------------------------------------------")
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

    private fun line(message: String) {
        Log.i(TAG, message)
        println(message)
    }

    private fun String.escapeForKotlin(): String {
        return replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
    }

    fun logBehaviorContextManifest() {
        line("Behavior context manifest")
        line("--------------------------------------------------")

        BehaviorContextManifest.all
            .sortedWith(compareBy({ it.key }, { it.value }))
            .forEach { spec ->
                line(
                    "${spec.id} " +
                        "status=${spec.status} " +
                        "applicationMode=${spec.applicationMode} " +
                        "reason=${spec.reason}",
                )
            }

        line("--------------------------------------------------")
    }

    fun logBehaviorStateMachine() {
        line("Behavior state machine")
        line("--------------------------------------------------")

        BehaviorMatrixProfile.values().forEach { profile ->
            val contexts = BehaviorContextMatrix.variants(profile)
            val decisions = contexts.map { context ->
                context to BehaviorContextSupportRegistry.evaluate(context)
            }

            line("Profile=$profile")
            line("  contexts=${contexts.size}")
            line("  runnable=${decisions.count { it.second.isSupported }}")
            line("  todo=${decisions.count { !it.second.isSupported }}")

            decisions.forEach { (context, decision) ->
                line(
                    "    - $context " +
                        "supported=${decision.isSupported}" +
                        decision.skippedReason?.let { " reason=$it" }.orEmpty(),
                )
            }

            line("")
        }

        line("Manifest:")
        BehaviorContextManifest.all
            .sortedWith(compareBy({ it.key }, { it.value }))
            .forEach { spec ->
                line(
                    "  - ${spec.id} " +
                        "status=${spec.status} " +
                        "applicationMode=${spec.applicationMode} " +
                        "reason=${spec.reason}",
                )
            }

        line("--------------------------------------------------")
    }
}
