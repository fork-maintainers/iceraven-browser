/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.benchmark.utils.HtmlAsset
import org.mozilla.fenix.benchmark.utils.MockWebServerRule
import org.mozilla.fenix.benchmark.utils.TARGET_PACKAGE
import org.mozilla.fenix.benchmark.utils.measureRepeatedDefault
import org.mozilla.fenix.benchmark.utils.privateBrowsingJourney
import org.mozilla.fenix.benchmark.utils.url

/**
 * This test class benchmarks the speed of loading a website from the awesome bar in private
 * browsing mode. Run this benchmark to verify how effective a Baseline Profile is. It does this by comparing
 * [CompilationMode.None], which represents the app with no Baseline Profiles optimizations, and
 * [CompilationMode.Partial], which uses Baseline Profiles.
 *
 * Run this benchmark to see startup measurements and captured system traces for verifying
 * the effectiveness of your Baseline Profiles. You can run it directly from Android
 * Studio as an instrumentation test that logs the benchmark metrics with links to the Perfetto traces,
 *
 * or using the gradle command:
 *
 * ```
 * ./gradlew :benchmark:connectedBenchmarkAndroidTest -P android.testInstrumentationRunnerArguments.annotation=org.mozilla.fenix.benchmark.baselineprofile -P benchmarkTest -P disableOptimization
 * ```
 *
 * The metric results will be in `benchmark/build/outputs/connected_android_test_additional_output` folder.
 *
 * Run the benchmarks on a physical device, not an emulator because the emulator doesn't represent
 * real world performance and shares system resources with its host.
 *
 * For more information, see the [Macrobenchmark documentation](https://d.android.com/macrobenchmark#create-macrobenchmark)
 * and the [instrumentation arguments documentation](https://d.android.com/topic/performance/benchmarking/macrobenchmark-instrumentation-args).
 **/
@BaselineProfileMacrobenchmark
class BaselineProfilesPrivateBrowsingBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @get:Rule
    val mockRule = MockWebServerRule()

    @Test
    fun privateBrowsingNone() = privateBrowsingBenchmark(CompilationMode.None())

    @Test
    fun privateBrowsing() =
        privateBrowsingBenchmark(
            CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require),
        )

    private fun privateBrowsingBenchmark(compilationMode: CompilationMode) =
        benchmarkRule.measureRepeatedDefault(
            packageName = TARGET_PACKAGE,
            metrics = listOf(StartupTimingMetric()),
            startupMode = StartupMode.COLD,
            compilationMode = compilationMode,
            setupBlock = {
                pressHome()
            },
        ) {
            privateBrowsingJourney(url = mockRule.url(HtmlAsset.SIMPLE))
            killProcess()
        }
}
