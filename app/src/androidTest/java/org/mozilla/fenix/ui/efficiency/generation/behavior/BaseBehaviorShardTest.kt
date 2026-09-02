/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.generation.behavior

import android.util.Log
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest

abstract class BaseBehaviorShardTest(
    private val case: BehaviorCase,
) : BaseTest(
    skipOnboarding = case.context.toBaseTestConfig().skipOnboarding,
    isPageLoadTranslationsPromptEnabled =
        case.context.toBaseTestConfig().isPageLoadTranslationsPromptEnabled,
    isPocketEnabled =
        case.context.toBaseTestConfig().isPocketEnabled,
    isRecentlyVisitedFeatureEnabled =
        case.context.toBaseTestConfig().isRecentlyVisitedFeatureEnabled,
) {

    protected fun runBehaviorCase() {
        Log.i(
            TAG,
            "TestRail=${case.testRailId} " +
                "Behavior=${case.label} " +
                "State=${case.state} " +
                "Context=${case.context}",
        )

        println("--------------------------------------------------")
        println("Running behavior case: ${case.label}")
        println("Automation ID: ${case.automationId}")
        println("TestRail: ${case.testRailId}")
        println("State: ${case.state}")
        println("Template: ${case.templateId}")
        println("Feature: ${case.feature}")
        println("Entity: ${case.entity}")
        println("Context: ${case.context}")
        println("Data: ${case.data}")
        println("Capabilities: ${case.capabilities.map { it.id }}")
        println("Assertion: ${case.assertion.kind} ${case.assertion.selectorTemplate.id}")

        require(case.isRunnable) {
            "Behavior case is not runnable: ${case.missingRequirements}"
        }

        val runtime = BehaviorRuntime(
            on = on,
            data = case.data,
            context = case.context,
        )

        BehaviorRuntimeStateSetup.apply(
            runtime = runtime,
            context = case.context,
        )

        case.capabilities.forEach { capability ->
            println("Performing capability: ${capability.id} - ${capability.description}")
            capability.perform(runtime)
        }

        println(
            "Verifying assertion: " +
                "${case.assertion.kind} ${case.assertion.selectorTemplate.description}",
        )

        case.assertion.verify(runtime)
    }

    private companion object {
        const val TAG = "BehaviorFactoryTest"
    }
}
