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
 * ElementLoadParameterizedTest
 *
 * - Each Case carries a label, a TestRail id, a page selector (PageContext.() -> BasePage),
 *   and an optional human-readable `state` string.
 *
 * - The `data()` companion provides two ways to control the `state`:
 *   1) Hard-coded per-case `state` values for quick local clarity.
 *   2) A run-level stamp read from `-DtestRunState="..."` (System property) so CI/CLI
 *      can stamp an entire run without modifying source.
 *
 * Design notes (why this shape helps future automation):
 * - Keeping `page` as a lambda `(PageContext) -> BasePage` lets us avoid per-page
 *   boilerplate wrappers. We can later generate these lambdas automatically by
 *   reflecting over PageContext properties.
 *
 * - The simple `state: String` is intentionally small and serializable to the test name
 *   so it appears directly in the JUnit XML. Later we can replace `state: String` with
 *   a richer object (flags/enum) and still keep the same instrumentation for reporting.
 *
 * - For CI-driven permutations:
 *     * A helper can enumerate "pages" (via reflection on PageContext or scanning page classes)
 *     * Another helper can enumerate "states" (feature flags, browser modes, user types)
 *     * A permutation generator produces Cases = Cartesian(product(pages, states))
 *     * Add pruning rules to avoid impossible or low-value combos
 *
 * - Because the test name includes the state, TestRail/reporting mapping is straightforward:
 *   each test case name will include the Testrail ID and the exact state string used.
 */
@RunWith(Parameterized::class)
class ElementLoadParameterizedTest(
    private val case: Case,
) : BaseTest() {

    data class Case(
        val label: String,
        val testRailId: String,
        val page: PageContext.() -> BasePage,
        val state: String = "",
    ) {
        // toString() is used in parameterized test naming and therefore shows up in XML reports.
        override fun toString(): String = "$label ($testRailId)${if (state.isNotBlank()) " — $state" else ""}"
    }

    companion object {
        /**
         * Parameter provider.
         *
         * Behavior:
         *  - If a run-level system property `testRunState` is provided when running Gradle,
         *    it will be used as the `state` for cases that do not specify an explicit state.
         *
         * Usage:
         *  ./gradlew connectedDebugAndroidTest \
         *    -Pandroid.testInstrumentationRunnerArguments.class=org.mozilla.fenix.ui.efficiency.devtools.ElementLoadParameterizedTest \
         *    -DtestRunState="Compose Redesign | Browser=Default"
         *
         * Notes for future dynamic generation:
         *  - Replace the static `listOf(...)` below with a generator function that:
         *      1) Reflects PageContext properties (e.g., PageContext::home, PageContext::bookmarks)
         *      2) Reflects available states (list of feature flags / browser modes / user types)
         *      3) Produces permutations and returns them as List<Case>
         *  - Add a pruning function to drop low-value or impossible permutations.
         */
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun data(): List<Any> {
            // Optional run-level stamp (CLI/CI can inject this)
            val runState = System.getProperty("testRunState")?.takeIf { it.isNotBlank() } ?: ""

            // Hard-coded cases for now — simple and explicit
            // Note: page lambdas refer to PageContext properties and avoid wrappers.
            val cases = listOf(
                Case(
                    label = "HomePage",
                    testRailId = "T1234",
                    page = { home },
                    state = runState.ifBlank { "Compose Classic | Browser=Default" },
                ),
                Case(
                    label = "BookmarksPage",
                    testRailId = "T1235",
                    page = { bookmarks },
                    state = runState.ifBlank { "Compose Classic | Browser=Default" },
                ),
                Case(
                    label = "HistoryPage",
                    testRailId = "T1236",
                    page = { history },
                    state = runState.ifBlank { "Compose Classic | Browser=Default" },
                ),
                // Example: explicit hard-coded state for just this case (overrides runState)
                Case(
                    label = "SearchBarComponent",
                    testRailId = "T12347",
                    page = { searchBar },
                    state = "Compose Redesign | Browser=Default", // explicit per-case state
                ),
                // FUTURE: swap the above static list with a generator that reflects PageContext
            )

            // The Parameterized runner expects a raw List/Collection of Any (static method),
            // so we ensure the type matches by mapping to Any.
            return cases.map { it as Any }
        }
    }

    @Test
    fun verifyElementLoadTest() {
        // Include the state in logs so it appears in stdout which may be embedded in XML
        Log.i("ElementLoadTest", "TestRail=${case.testRailId} Page=${case.label} State=${case.state}")
        println("TestRail=${case.testRailId} Page=${case.label} State=${case.state}")

        // 1) If you later need to apply the state to the app (prefs/flags/onboarding flows),
        //    call a small helper here before navigation. For the simple string approach we
        //    only report the chosen state for now.
        //
        //    e.g. applyState(case.state)

        // 2) Navigate to the page using the PageContext -> BasePage lambda
        val pageObj: BasePage = case.page(on) // `on` is the PageContext provided by BaseTest
        pageObj.navigateToPage()

        // 3) Add element checks/assertions here...
        //    Keep them small and focused so each permutation surfaces a single meaningful failure.
    }

    // Example helper stub for future richer state application:
    // private fun applyState(state: String) { ... }
}
