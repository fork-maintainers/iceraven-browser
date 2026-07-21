/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.generation.behavior.shards

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mozilla.fenix.ui.efficiency.generation.behavior.BaseBehaviorShardTest
import org.mozilla.fenix.ui.efficiency.generation.behavior.BehaviorCase
import org.mozilla.fenix.ui.efficiency.generation.behavior.BehaviorShardData

@RunWith(Parameterized::class)
class BehaviorShard01(
    private val case: BehaviorCase,
) : BaseBehaviorShardTest(case) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun data(): List<Array<Any>> = BehaviorShardData.loadShard(
            shardIndex = 1,
            shardCount = 1,
        )
    }

    @Test
    fun verifyBehaviorCase() {
        runBehaviorCase()
    }
}
