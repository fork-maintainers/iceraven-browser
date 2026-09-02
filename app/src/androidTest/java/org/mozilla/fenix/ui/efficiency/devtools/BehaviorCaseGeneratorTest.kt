/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.devtools

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.ui.efficiency.generation.behavior.BehaviorMatrixProfile
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest

@RunWith(AndroidJUnit4::class)
class BehaviorCaseGeneratorTest : BaseTest() {

    @Test
    fun logBehaviorCasePlans() {
        // Touching `on` initializes PageContext/page objects in the normal test lifecycle.
        on

        BehaviorCaseGenerator.logBehaviorCaseBoilerplate(
            includeSkipped = true,
        )
    }

    @Test
    fun logBehaviorPlanSummary() {
        on

        BehaviorCaseGenerator.logBehaviorPlanSummary()
    }

    @Test
    fun logSmokeBehaviorMatrixSummary() {
        on

        BehaviorCaseGenerator.logBehaviorMatrixSummary(
            profile = BehaviorMatrixProfile.SMOKE,
        )
    }

    @Test
    fun logBaseFlagsBehaviorMatrixSummary() {
        on

        BehaviorCaseGenerator.logBehaviorMatrixSummary(
            profile = BehaviorMatrixProfile.BASE_FLAGS,
        )
    }

    @Test
    fun logPairwisePreviewBehaviorMatrixSummary() {
        on

        BehaviorCaseGenerator.logBehaviorMatrixSummary(
            profile = BehaviorMatrixProfile.PAIRWISE_PREVIEW,
        )
    }

    @Test
    fun logExhaustivePreviewBehaviorMatrixSummary() {
        on

        BehaviorCaseGenerator.logBehaviorMatrixSummary(
            profile = BehaviorMatrixProfile.EXHAUSTIVE_PREVIEW,
        )
    }

    @Test
    fun logBaseFlagsBehaviorCasePlans() {
        on

        BehaviorCaseGenerator.logBehaviorCaseBoilerplate(
            includeSkipped = true,
            profile = BehaviorMatrixProfile.BASE_FLAGS,
        )
    }

    @Test
    fun logBehaviorContextManifest() {
        on

        BehaviorCaseGenerator.logBehaviorContextManifest()
    }

    @Test
    fun logBehaviorStateMachine() {
        on

        BehaviorCaseGenerator.logBehaviorStateMachine()
    }
}
