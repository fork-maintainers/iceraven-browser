/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.examples

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.navigation.NavigationRegistry
import org.mozilla.fenix.ui.efficiency.navigation.planning.NavigationTestPlannerLogger
import java.io.File

@RunWith(AndroidJUnit4::class)
class NavigationRegistryLoggingTest : BaseTest() {
    @Test
    fun logImportantNavigationPairs() {
        NavigationRegistry.logAllPaths("AppEntry", "BookmarksPage")
        NavigationRegistry.logAllPaths("AppEntry", "HistoryPage")
        NavigationRegistry.logAllPaths("SettingsPage", "ToolbarComponent")
        NavigationRegistry.logAllPaths("SettingsTabsPage", "ShareOverlayPage")
    }

    @Test
    fun logNavigationTestPlanner() {
        NavigationTestPlannerLogger.logReachabilityPlan(on)
    }

    @Test
    fun logNavigationPlanSummary() {
        // on initializes the navigation registry and must be called
        // first for all page pairs to be graphed and logged
        on
        NavigationRegistry.logGraph()
        NavigationRegistry.logPathSummary()
    }

    @Test
    fun exportNavigationGraphDotFile() {
        on

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val outputDir = context.getExternalFilesDir(null)!!
        val file = File(outputDir, "navigation-graph.dot")

        NavigationRegistry.exportDotToFile(file)

        Log.i("NavigationGraphExportTest", "DOT file written to ${file.absolutePath}")
    }
}
