/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.benchmark.utils.TARGET_PACKAGE
import org.mozilla.fenix.benchmark.utils.clearPackageData
import org.mozilla.fenix.benchmark.utils.measureRepeatedDefault
import org.mozilla.fenix.benchmark.utils.onboardingJourney

/**
 * This test class benchmarks the speed of completing onboarding. Run this benchmark to verify how effective
 * a Baseline Profile is. It does this by comparing [CompilationMode.None], which represents the
 * app with no Baseline Profiles optimizations, and [CompilationMode.Partial], which uses Baseline Profiles.
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
@RunWith(AndroidJUnit4::class)
@BaselineProfileMacrobenchmark
class BaselineProfilesOnboardingBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Ignore("Failing due to new nightly config: https://bugzilla.mozilla.org/show_bug.cgi?id=2031266")
    @Test
    fun onboardingNone() = onboardingBenchmark(CompilationMode.None())

    @Ignore("Failing due to new nightly config: https://bugzilla.mozilla.org/show_bug.cgi?id=2031266")
    @Test
    fun onboarding() =
        onboardingBenchmark(CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require))

    private fun onboardingBenchmark(compilationMode: CompilationMode) =
        benchmarkRule.measureRepeatedDefault(
            packageName = TARGET_PACKAGE,
            metrics = listOf(StartupTimingMetric()),
            startupMode = StartupMode.COLD,
            compilationMode = compilationMode,
            setupBlock = {
                pressHome()
                device.clearPackageData(packageName = packageName)
                killProcess()
            },
        ) {
            onboardingJourney()
            killProcess()
        }
}
