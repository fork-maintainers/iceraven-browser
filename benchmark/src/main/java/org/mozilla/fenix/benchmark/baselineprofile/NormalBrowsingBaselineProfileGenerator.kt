/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.benchmark.baselineprofile

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.benchmark.utils.HtmlAsset
import org.mozilla.fenix.benchmark.utils.MockWebServerRule
import org.mozilla.fenix.benchmark.utils.TARGET_PACKAGE
import org.mozilla.fenix.benchmark.utils.normalBrowsingJourney
import org.mozilla.fenix.benchmark.utils.url

/**
 * This test class generates a baseline profile on a critical user journey, that loads a website from the
 * awesome bar in normal browsing mode, for the target package.
 *
 * Refer to the [baseline profile documentation](https://d.android.com/topic/performance/baselineprofiles)
 * for more information.
 *
 * Generate the baseline profile using this gradle task:
 * ```
 *  ./gradlew :benchmark:pixel6Api34BenchmarkAndroidTest -P android.testInstrumentationRunnerArguments.annotation=org.mozilla.fenix.benchmark.baselineprofile -P benchmarkTest -P disableOptimization
 * ```
 *
 * Check [documentation](https://d.android.com/topic/performance/benchmarking/macrobenchmark-instrumentation-args)
 * for more information about available instrumentation arguments.
 *
 * Then, copy the profiles to app/src/benchmark/baselineProfiles to verify the improvements by running
 * the [org.mozilla.fenix.benchmark.BaselineProfilesNormalBrowsingBenchmark] benchmark.
 * Notice that when we run the benchmark, we run the benchmark variant and not the nightly so
 * copying the profiles here is important.
 * These shouldn't be pushed to version control.
 *
 * When using this class to generate a baseline profile, only API 33+ or rooted API 28+ are supported.
 */
@RequiresApi(Build.VERSION_CODES.P)
@BaselineProfileGenerator
class NormalBrowsingBaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @get:Rule
    val mockRule = MockWebServerRule()

    @Test
    fun generateBaselineProfile() {
        rule.collect(
            packageName = TARGET_PACKAGE,
            maxIterations = baselineProfileMaxIterations(),
        ) {
            normalBrowsingJourney(url = mockRule.url(HtmlAsset.SIMPLE))
        }
    }
}
