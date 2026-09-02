package org.mozilla.fenix.ui.efficiency.generation

/**
 * Utilities for deterministically splitting generated test cases into manual shards.
 */
object ShardUtils {

    /**
     * Returns only the cases assigned to the requested 1-based shard index.
     *
     * Example:
     * - shardCount = 10
     * - shardIndex = 1 returns cases at indices 0, 10, 20, ...
     * - shardIndex = 2 returns cases at indices 1, 11, 21, ...
     */
    fun <T> filterForShard(
        items: List<T>,
        shardIndex: Int,
        shardCount: Int,
    ): List<T> {
        require(shardCount > 0) { "shardCount must be > 0" }
        require(shardIndex in 1..shardCount) {
            "shardIndex must be between 1 and shardCount inclusive. " +
                "Received shardIndex=$shardIndex shardCount=$shardCount"
        }

        val zeroBasedShard = shardIndex - 1

        return items.filterIndexed { index, _ ->
            index % shardCount == zeroBasedShard
        }
    }

    /**
     * Shared shell for the four domains' `XShardData.loadShard()` entrypoints: resolves the
     * run-state override (falling back to the `testRunState` system property, then blank),
     * delegates to [buildForShard] to build that domain's shard, and wraps each case for
     * JUnit's `Parameterized` `data()` contract.
     *
     * Extracted (P2b-3, 2026-07-17) — this wrapper was previously identical, copy-pasted logic
     * in ReachabilityShardData, PairShardData, InteractionShardData, and
     * BehaviorShardData. Domain-specific setup (e.g. Pairs' graph bootstrap) stays in the
     * caller's [buildForShard] lambda rather than being forced into this shared shell.
     */
    fun <T> loadShard(
        shardIndex: Int,
        shardCount: Int,
        runStateOverride: String? = null,
        buildForShard: (runState: String, shardIndex: Int, shardCount: Int) -> List<T>,
    ): List<Array<Any>> {
        val runState = runStateOverride
            ?: System.getProperty("testRunState")?.takeIf { it.isNotBlank() }
            ?: ""

        return buildForShard(runState, shardIndex, shardCount).map { arrayOf(it as Any) }
    }
}
