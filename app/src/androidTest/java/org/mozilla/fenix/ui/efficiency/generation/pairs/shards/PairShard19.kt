package org.mozilla.fenix.ui.efficiency.generation.pairs.shards

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mozilla.fenix.ui.efficiency.generation.pairs.BasePairShardTest
import org.mozilla.fenix.ui.efficiency.generation.pairs.PairCase
import org.mozilla.fenix.ui.efficiency.generation.pairs.PairShardData

@RunWith(Parameterized::class)
class PairShard19(
    private val case: PairCase,
) : BasePairShardTest(case) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun data(): List<Array<Any>> = PairShardData.loadShard(
            shardIndex = 19,
            shardCount = 20,
        )
    }

    @Test
    fun verifyNavigationPairReachability() {
        runNavigationPairCase()
    }
}
