/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.devtools

import android.util.Log
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.helpers.PageContext

/**
 * NavigationTest
 *
 * Purpose:
 * - Verify that each selected page/component can be reached via `navigateToPage()`
 *   from a fresh test setup.
 *
 * Why this shape:
 * - Matches the same parameterized structure already proven to work in this project.
 * - Keeps each navigation target as its own test case.
 * - Allows the case list to be pasted in from a helper/generator so maintenance stays low.
 *
 * Future direction:
 * - The static `listOf(...)` below can be replaced or regenerated from a helper that reflects
 *   over PageContext and prints `Case(...)` boilerplate.
 */
@RunWith(Parameterized::class)
class NavigationTest(
    private val case: Case,
) : BaseTest() {

    data class Case(
        val label: String,
        val testRailId: String,
        val page: PageContext.() -> BasePage,
        val state: String = "",
    ) {
        override fun toString(): String =
            "$label ($testRailId)${if (state.isNotBlank()) " — $state" else ""}"
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun data(): List<Any> {
            val runState = System.getProperty("testRunState")?.takeIf { it.isNotBlank() } ?: ""

            // The test cases below are generated from navigation.NavigationTestPlanner
            val cases = listOf(
                // pageName=BookmarksPage, property=bookmarks, paths=9
                Case(
                    label = "BookmarksPage",
                    testRailId = "TBD",
                    page = { bookmarks },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
                // pageName=BrowserPage, property=browserPage, paths=2
                Case(
                    label = "BrowserPage",
                    testRailId = "TBD",
                    page = { browserPage },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
                // pageName=CollectionsPage, property=collections, paths=1
                Case(
                    label = "CollectionsPage",
                    testRailId = "TBD",
                    page = { collections },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
            )

            return cases.map { it as Any }
        }
    }

    @Test
    fun verifyNavigationReachability() {
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
