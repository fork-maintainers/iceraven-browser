package org.mozilla.fenix.ui.efficiency.devtools

import android.util.Log
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mozilla.fenix.ui.efficiency.generation.interaction.InteractionCase
import org.mozilla.fenix.ui.efficiency.generation.interaction.InteractionCaseFactory
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest

@RunWith(Parameterized::class)
class InteractionGeneratedCasePrintTest(
    private val case: InteractionCase,
) : BaseTest() {

    companion object {
        private const val TAG = "InteractionCasePrint"

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun data(): List<Array<Any>> {
            val runState = System.getProperty("testRunState")
                ?.takeIf { it.isNotBlank() }
                ?: ""

            return InteractionCaseFactory
                .buildInteractionCases(runState = runState)
                .map { arrayOf(it as Any) }
        }
    }

    @Test
    fun printGeneratedInteractionCase() {
        Log.i(TAG, "--------------------------------------------------")
        Log.i(TAG, "Interaction case: ${case.label}")
        Log.i(TAG, "TestRail: ${case.testRailId}")
        Log.i(TAG, "State: ${case.state}")
        Log.i(TAG, "Interaction selector: ${case.interactionSelectorName}")
        Log.i(TAG, "Expected selectors: ${case.expectedSelectorNames}")

        println("--------------------------------------------------")
        println("Interaction case: ${case.label}")
        println("TestRail: ${case.testRailId}")
        println("State: ${case.state}")
        println("Interaction selector: ${case.interactionSelectorName}")
        println("Expected selectors: ${case.expectedSelectorNames}")
    }
}
