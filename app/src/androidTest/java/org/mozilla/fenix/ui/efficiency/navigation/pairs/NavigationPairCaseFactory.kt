package org.mozilla.fenix.ui.efficiency.navigation.pairs

import android.util.Log
import org.mozilla.fenix.ui.efficiency.navigation.planning.NavigationTestPlanner
import org.mozilla.fenix.ui.efficiency.navigation.planning.ShardUtils
import org.mozilla.fenix.ui.efficiency.navigation.planning.toDisplayLabel

object NavigationPairCaseFactory {

    private const val TAG = "NavigationPairCaseFactory"

    fun buildPairCases(
        runState: String,
    ): List<NavigationPairCase> {
        val generatedCases = NavigationTestPlanner.buildNavigationPairCases()

        val cases = generatedCases.map { generated ->
            NavigationPairCase(
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
    ): List<NavigationPairCase> {
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
