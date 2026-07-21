package org.mozilla.fenix.ui.efficiency.generation.pairs

import android.util.Log
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest

abstract class BasePairShardTest(
    private val case: PairCase,
) : BaseTest() {

    protected fun runNavigationPairCase() {
        Log.i(
            "NavigationPairTest",
            "TestRail=${case.testRailId} Pair=${case.label} State=${case.state}",
        )
        println("TestRail=${case.testRailId} Pair=${case.label} State=${case.state}")

        val firstPageObj: BasePage = case.firstPage(on)
        firstPageObj.navigateToPage()

        val secondPageObj: BasePage = case.secondPage(on)
        secondPageObj.navigateToPage()
    }
}
