/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.devtools

import org.mozilla.fenix.ui.efficiency.generation.NavigationTestPlanner
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.PageContext
import org.mozilla.fenix.ui.efficiency.navigation.NavigationRegistry

// TODO (Jackie J. 3/23/2026): fix all of these horrible names, they're temporary.
object ReachabilityPlanLogger {

    private const val TAG = "NavigationPlanner"

    /**
     * Builds the reachability plan report without emitting it anywhere, so a future consumer
     * (a debug UI, an export, etc.) can reuse the same computation [logReachabilityPlan] uses.
     */
    fun buildReachabilityPlanReport(context: PageContext): DevToolReport {
        val cases = NavigationTestPlanner.buildReachabilityCases()

        val lines = buildList {
            add("Built ${cases.size} reachability cases")

            cases.forEachIndexed { index, case ->
                val page: BasePage = case.page(context)
                val pageName = page.pageName
                val pathCount = NavigationRegistry.findAllPaths("AppEntry", pageName).size

                add(" ${index + 1}. $pageName (property=${case.propertyName}, paths=$pathCount)")
            }
        }

        return DevToolReport(lines = lines)
    }

    fun logReachabilityPlan(context: PageContext) {
        logReport(TAG, buildReachabilityPlanReport(context))
    }
}
