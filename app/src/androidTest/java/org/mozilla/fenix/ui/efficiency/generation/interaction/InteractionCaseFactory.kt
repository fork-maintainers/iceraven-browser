package org.mozilla.fenix.ui.efficiency.generation.interaction

import android.util.Log
import org.mozilla.fenix.ui.efficiency.generation.ShardUtils

object InteractionCaseFactory {

    private const val TAG = "InteractionCaseFactory"

    fun buildInteractionCases(
        runState: String,
    ): List<InteractionCase> {
        val generatedCases = InteractionTestPlanner
            .buildInteractionCases()
            .filter { it.isRunnable }

        val cases = generatedCases.map { generated ->
            val selectorRefs = SelectorCatalog.discoverSelectorsForPage(generated.pagePropertyName)

            val interactionSelector = selectorRefs
                .first { it.selectorName == generated.interactionSelectorName }
                .selector

            val expectedSelectors = selectorRefs
                .filter { it.selectorName in generated.expectedSelectorNames }
                .map { it.selector }

            InteractionCase(
                label = "${generated.pageName} - ${generated.interactionSelectorName}",
                testRailId = "TBD",
                page = generated.page,
                interactionSelectorName = generated.interactionSelectorName,
                interactionSelector = interactionSelector,
                expectedSelectorNames = generated.expectedSelectorNames,
                expectedSelectors = expectedSelectors,
                state = runState.ifBlank { "Interaction Factory" },
            )
        }

        Log.i(TAG, "Built ${cases.size} runnable interaction cases.")
        return cases
    }

    fun buildInteractionCasesForShard(
        runState: String,
        shardIndex: Int,
        shardCount: Int,
    ): List<InteractionCase> {
        val allCases = buildInteractionCases(runState)

        val shardCases = ShardUtils.filterForShard(
            items = allCases,
            shardIndex = shardIndex,
            shardCount = shardCount,
        )

        Log.i(
            TAG,
            "Shard $shardIndex/$shardCount contains ${shardCases.size} " +
                "of ${allCases.size} total interaction cases.",
        )

        return shardCases
    }
}
