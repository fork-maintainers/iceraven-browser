/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.devtools

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeFalse
import org.junit.runner.RunWith
import org.mozilla.fenix.helpers.AppAndSystemHelper.isTestLab
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.helpers.ScreenDump
import java.io.File
import kotlin.test.Test

// DEV ONLY. Selector-discovery helpers around ScreenDump.dumpAll().
// The TAE flank config scans this whole package, so both methods gate on assumeFalse(isTestLab()) —
// @Ignore would also block running them by hand, which is the only way they are meant to be used.
// Artifacts + the exact `adb shell run-as` retrieval commands are logged under the "EffScreenDump" tag.
@RunWith(AndroidJUnit4::class)
class InteractiveInspectTest : BaseTest() {

    // START HERE. Navigate somewhere, dump ONCE, done — use this to confirm the whole pipeline works.
    // Run: ./mach gradle fenix:connectedDebugAndroidTest \
    //   -Pandroid.testInstrumentationRunnerArguments.class=org.mozilla.fenix.ui.efficiency.devtools.InteractiveInspectTest#dumpOnce
    // Then: adb logcat -d -s EffScreenDump:I     (prints element count + the run-as commands to retrieve the files)
    @Test
    fun dumpOnce() {
        assumeFalse("dev tool, not a CI test", isTestLab())
        on.home.navigateToPage()
        ScreenDump.dumpAll(composeRule, "dumpOnce (home)")
    }

    // Interactive: run it, drive the app BY HAND, and trigger a dump whenever you're on the screen you want.
    // Trigger from your machine (pkg is logged; Fenix debug = org.mozilla.fenix.debug):
    //   adb shell run-as org.mozilla.fenix.debug touch files/eff-scan
    @Test
    fun inspect() {
        // Without this gate the 30-minute poll below burns a Firebase device slot on every TAE run.
        assumeFalse("dev tool, not a CI test", isTestLab())
        on.home.navigateToPage()
        val trigger = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "eff-scan")
        val end = System.currentTimeMillis() + 30 * 60_000 // 30-min budget so it can't hang forever
        while (System.currentTimeMillis() < end) {
            if (trigger.exists()) {
                trigger.delete()
                // Call on the test thread — NOT runOnMainSync; the Espresso layer must run off the main thread.
                ScreenDump.dumpAll(composeRule, "inspect scan")
            }
            Thread.sleep(500)
        }
    }
}
