/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.devtools

import org.mozilla.fenix.ui.efficiency.generation.NavigationTestPlanner
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.PageContext
import org.mozilla.fenix.ui.efficiency.navigation.NavigationRegistry

// TODO (Jackie J. 3/23/2026): fix all of these horrible names, they're temporary.
object ReachabilityCaseGenerator {

    private const val TAG = "NavCaseGenerator"

    /**
     * Builds the reachability boilerplate report without emitting it anywhere, so a future
     * consumer (a debug UI, an export, etc.) can reuse the same computation
     * [logNavigationCaseBoilerplate] uses.
     */
    fun buildNavigationCaseBoilerplateReport(context: PageContext): DevToolReport {
        val cases = NavigationTestPlanner.buildReachabilityCases()

        return buildBoilerplateReport(
            header = "Generated ${cases.size} reachability case templates:",
            items = cases,
        ) { case ->
            val pageObj: BasePage = case.page(context)
            val pageName = pageObj.pageName
            val pathCount = NavigationRegistry.findAllPaths("AppEntry", pageName).size

            """
            // pageName=$pageName, property=${case.propertyName}, paths=$pathCount
            Case(
                label = "$pageName",
                testRailId = "TBD",
                page = { ${case.propertyName} },
                state = runState.ifBlank { "Navigation Reachability" },
            ),
            """.trimIndent()
        }
    }

    fun logNavigationCaseBoilerplate(context: PageContext) {
        logReport(TAG, buildNavigationCaseBoilerplateReport(context))
    }
}
