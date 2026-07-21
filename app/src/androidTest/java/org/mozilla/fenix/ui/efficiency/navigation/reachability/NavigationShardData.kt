package org.mozilla.fenix.ui.efficiency.navigation.reachability

import org.mozilla.fenix.ui.efficiency.navigation.planning.ShardUtils

object NavigationShardData {

    fun loadShard(
        shardIndex: Int,
        shardCount: Int,
        runStateOverride: String? = null,
    ): List<Array<Any>> = ShardUtils.loadShard(
        shardIndex = shardIndex,
        shardCount = shardCount,
        runStateOverride = runStateOverride,
        buildForShard = NavigationCaseFactory::buildReachabilityCasesForShard,
    )
}
