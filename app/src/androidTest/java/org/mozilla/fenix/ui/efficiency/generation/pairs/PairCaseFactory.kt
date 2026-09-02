package org.mozilla.fenix.ui.efficiency.generation.pairs

import android.util.Log
import org.mozilla.fenix.ui.efficiency.generation.NavigationTestPlanner
import org.mozilla.fenix.ui.efficiency.generation.ShardUtils
import org.mozilla.fenix.ui.efficiency.generation.toDisplayLabel

object PairCaseFactory {

    private const val TAG = "PairCaseFactory"

    fun buildPairCases(
        runState: String,
    ): List<PairCase> {
        val generatedCases = NavigationTestPlanner.buildNavigationPairCases()

        val cases = generatedCases.map { generated ->
            PairCase(
                label = "${generated.firstPropertyName.toDisplayLabel()} -> " +
                    generated.secondPropertyName.toDisplayLabel(),
                testRailId = "TBD",
                firstPage = generated.firstPage,
                secondPage = generated.secondPage,
                state = runState.ifBlank { "Navigation Pair Reachability" },
            )
        }

        Log.i(TAG, "Built ${cases.size} navigation pair cases.")
        return cases
    }

    fun buildPairCasesForShard(
        runState: String,
        shardIndex: Int,
        shardCount: Int,
    ): List<PairCase> {
        val allCases = buildPairCases(runState)
        val shardCases = ShardUtils.filterForShard(
            items = allCases,
            shardIndex = shardIndex,
            shardCount = shardCount,
        )

        Log.i(
            TAG,
            "Shard $shardIndex/$shardCount contains ${shardCases.size} " +
                "of ${allCases.size} total navigation pair cases.",
        )

        return shardCases
    }
}
