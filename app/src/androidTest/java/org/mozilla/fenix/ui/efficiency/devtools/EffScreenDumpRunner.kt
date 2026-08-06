/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.devtools

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.mozilla.fenix.helpers.AppAndSystemHelper.isTestLab
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.helpers.ScreenDump
import org.mozilla.fenix.ui.efficiency.navigation.PageCatalog

/**
 * EffScreenDumpRunner — dev tool (not a smoke test).
 *
 * On-demand dump of a screen's live Compose UI tree, so selectors can be authored against ground truth
 * instead of inferred from legacy robots (which carry stale locators). Navigates to a page by its
 * PageContext property name (instrumentation arg `effdump.page`, default "home"), then emits an
 * EFF_SCREEN_DUMP via ScreenDump — read it back with effpretty like any run report.
 *
 * Run (effwatch queue): request `{ "test_class": "...devtools.EffScreenDumpRunner", "batch": "effdump",
 * "mach_args": "--stacktrace -Pandroid.testInstrumentationRunnerArguments.effdump.page=<property>" }`.
 * Only works for graph-navigable pages (launch-reached pages like customTabs have no nav edge).
 */
class EffScreenDumpRunner : BaseTest() {

    @Test
    fun dumpPage() {
        // The TAE flank config scans the whole devtools package, so gate on Firebase rather than @Ignore —
        // @Ignore would also block running this by hand, which is the only way it is meant to be used.
        assumeFalse("dev tool, not a CI test", isTestLab())
        val target = InstrumentationRegistry.getArguments().getString("effdump.page") ?: "home"
        val ref = PageCatalog.discoverPages().firstOrNull { it.propertyName == target }
            ?: error(
                "effdump: no PageContext page named '$target'. Options: " +
                    PageCatalog.discoverPages().joinToString(", ") { it.propertyName },
            )
        // Optional: dump a specific URL rather than the page's default landing state. Needed for
        // browserPage, which otherwise loads example.com — useless when the selectors you are
        // authoring belong to some other test page.
        val url = InstrumentationRegistry.getArguments().getString("effdump.url").orEmpty()
        ref.getter(on).navigateToPage(url = url)
        ScreenDump.dump(composeRule, label = "effdump:$target (compose)")
        ScreenDump.dumpEspresso(label = "effdump:$target (espresso/view-tree)")
        ScreenDump.dumpUiAutomator(label = "effdump:$target (native/uiautomator)")
    }
}
