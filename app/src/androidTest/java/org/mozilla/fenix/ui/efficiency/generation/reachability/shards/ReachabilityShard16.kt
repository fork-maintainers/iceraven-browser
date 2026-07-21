package org.mozilla.fenix.ui.efficiency.generation.reachability.shards

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mozilla.fenix.ui.efficiency.generation.reachability.BaseReachabilityShardTest
import org.mozilla.fenix.ui.efficiency.generation.reachability.ReachabilityCase
import org.mozilla.fenix.ui.efficiency.generation.reachability.ReachabilityShardData

@RunWith(Parameterized::class)
class ReachabilityShard16(
    private val case: ReachabilityCase,
) : BaseReachabilityShardTest(case) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun data(): List<Array<Any>> = ReachabilityShardData.loadShard(
            shardIndex = 16,
            shardCount = 20,
        )
    }

    @Test
    fun verifyNavigationReachability() {
        runNavigationCase()
    }
}
