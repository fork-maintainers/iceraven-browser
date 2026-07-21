package org.mozilla.fenix.ui.efficiency.navigation.interaction

import org.mozilla.fenix.ui.efficiency.navigation.planning.ShardUtils

object InteractionShardData {

    fun loadShard(
        shardIndex: Int,
        shardCount: Int,
        runStateOverride: String? = null,
    ): List<Array<Any>> = ShardUtils.loadShard(
        shardIndex = shardIndex,
        shardCount = shardCount,
        runStateOverride = runStateOverride,
        buildForShard = InteractionCaseFactory::buildInteractionCasesForShard,
    )
}
