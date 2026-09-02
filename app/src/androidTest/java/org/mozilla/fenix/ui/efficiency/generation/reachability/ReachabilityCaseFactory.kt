package org.mozilla.fenix.ui.efficiency.generation.reachability

import android.util.Log
import org.mozilla.fenix.ui.efficiency.generation.NavigationGraphBootstrap
import org.mozilla.fenix.ui.efficiency.generation.NavigationTestPlanner
import org.mozilla.fenix.ui.efficiency.generation.ShardUtils
import org.mozilla.fenix.ui.efficiency.generation.toDisplayLabel

object ReachabilityCaseFactory {

    private const val TAG = "ReachabilityCaseFactory"

    fun buildReachabilityCases(
        runState: String,
    ): List<ReachabilityCase> {
        NavigationGraphBootstrap.ensureInitialized()
        val generatedCases = NavigationTestPlanner.buildReachabilityCases()

        val cases = generatedCases.map { generated ->
            ReachabilityCase(
                label = generated.propertyName.toDisplayLabel(),
                testRailId = "TBD",
                page = generated.page,
                state = runState.ifBlank { "Navigation Reachability" },
                launch = generated.launch,
            )
        }

        Log.i(TAG, "Built ${cases.size} navigation reachability cases.")
        return cases
    }

    fun buildReachabilityCasesForShard(
        runState: String,
        shardIndex: Int,
        shardCount: Int,
    ): List<ReachabilityCase> {
        val allCases = buildReachabilityCases(runState)
        val shardCases = ShardUtils.filterForShard(
            items = allCases,
            shardIndex = shardIndex,
            shardCount = shardCount,
        )

        Log.i(
            TAG,
            "Shard $shardIndex/$shardCount contains ${shardCases.size} " +
                "of ${allCases.size} total navigation reachability cases.",
        )

        return shardCases
    }
}
