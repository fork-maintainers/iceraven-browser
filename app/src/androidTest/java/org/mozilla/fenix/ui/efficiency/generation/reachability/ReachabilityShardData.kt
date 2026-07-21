package org.mozilla.fenix.ui.efficiency.generation.reachability

import org.mozilla.fenix.ui.efficiency.navigation.planning.ShardUtils

object ReachabilityShardData {

    fun loadShard(
        shardIndex: Int,
        shardCount: Int,
        runStateOverride: String? = null,
    ): List<Array<Any>> = ShardUtils.loadShard(
        shardIndex = shardIndex,
        shardCount = shardCount,
        runStateOverride = runStateOverride,
        buildForShard = ReachabilityCaseFactory::buildReachabilityCasesForShard,
    )
}
