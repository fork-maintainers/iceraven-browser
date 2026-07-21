package org.mozilla.fenix.ui.efficiency.generation.reachability

import org.mozilla.fenix.ui.efficiency.generation.ShardUtils

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
