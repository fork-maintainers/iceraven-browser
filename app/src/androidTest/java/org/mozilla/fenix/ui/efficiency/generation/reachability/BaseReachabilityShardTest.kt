package org.mozilla.fenix.ui.efficiency.generation.reachability

import android.util.Log
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.navigation.LaunchConfig

/**
 * Shared logic for all generated/manual navigation shard entrypoint classes.
 */
abstract class BaseReachabilityShardTest(
    private val case: ReachabilityCase,
) : BaseTest() {

    override fun launchConfig(): LaunchConfig = case.launch

    protected fun runNavigationCase() {
        Log.i(
            "NavigationReachabilityTest",
            "TestRail=${case.testRailId} Page=${case.label} State=${case.state}",
        )
        println("TestRail=${case.testRailId} Page=${case.label} State=${case.state}")

        val pageObj: BasePage = case.page(on)
        pageObj.navigateToPage()

        // Add optional page-specific assertions later if needed.
    }
}
