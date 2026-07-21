/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.generation.behavior

import android.util.Log
import org.mozilla.fenix.ui.efficiency.generation.NavigationGraphBootstrap
import org.mozilla.fenix.ui.efficiency.generation.ShardUtils
import org.mozilla.fenix.ui.efficiency.navigation.PageCatalog

object BehaviorCaseFactory {
    private const val TAG = "BehaviorCaseFactory"

    fun buildBehaviorCases(
        runState: String,
        includeNonRunnable: Boolean = false,
    ): List<BehaviorCase> {
        NavigationGraphBootstrap.ensureInitialized()

        val plans = BehaviorTestPlanner.buildBehaviorCasePlans()
            .filter { it.isRunnable }

        val pageRefsByProperty = PageCatalog.discoverPages().associateBy { it.propertyName }

        val cases = plans.mapNotNull { plan ->
            if (!plan.isRunnable && !includeNonRunnable) return@mapNotNull null

            val capabilities = plan.capabilityIds.mapNotNull { BehaviorCapabilityCatalog.findById(it) }
            val assertionTemplate = plan.assertionSelectorTemplateId
                ?.let { SelectorTemplateCatalog.findById(it) }

            val assertionPageProperty = plan.assertionPagePropertyName
            val assertionPage = assertionPageProperty
                ?.let { pageRefsByProperty[it]?.getter }

            if (assertionTemplate == null || assertionPageProperty == null || assertionPage == null) {
                Log.i(TAG, "Skipping unresolved behavior plan: $plan")
                return@mapNotNull null
            }

            BehaviorCase(
                label = plan.label,
                testRailId = plan.testRailId,
                automationId = plan.automationId,
                feature = plan.feature,
                entity = plan.entity,
                templateId = plan.templateId,
                capabilities = capabilities,
                assertion = ResolvedBehaviorAssertion(
                    pagePropertyName = assertionPageProperty,
                    page = assertionPage,
                    selectorTemplate = assertionTemplate,
                    kind = plan.assertionKind,
                ),
                data = plan.data,
                context = plan.context,
                state = runState.ifBlank { "Behavior Factory | ${plan.context}" },
                missingRequirements = plan.missingRequirements,
            )
        }

        Log.i(TAG, "Built ${cases.size} behavior cases from ${plans.size} plans.")
        return cases
    }

    fun buildBehaviorCasesForShard(
        runState: String,
        shardIndex: Int,
        shardCount: Int,
    ): List<BehaviorCase> {
        val allCases = buildBehaviorCases(runState = runState)
        val shardCases = ShardUtils.filterForShard(
            items = allCases,
            shardIndex = shardIndex,
            shardCount = shardCount,
        )

        Log.i(
            TAG,
            "Shard $shardIndex/$shardCount contains ${shardCases.size} " +
                "of ${allCases.size} total behavior cases.",
        )

        return shardCases
    }
}
