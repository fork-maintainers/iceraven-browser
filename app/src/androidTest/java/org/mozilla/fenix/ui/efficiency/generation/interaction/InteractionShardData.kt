package org.mozilla.fenix.ui.efficiency.generation.interaction

import org.mozilla.fenix.ui.efficiency.generation.ShardUtils

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
