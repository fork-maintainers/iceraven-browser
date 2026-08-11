/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.benchmark.baselineprofile

import androidx.test.platform.app.InstrumentationRegistry

private const val BASELINE_PROFILE_MAX_ITERATIONS_ARGUMENT = "baselineProfileMaxIterations"

/**
 * We keep the same default as @see BaselineProfileRule's maxIterations
 * @see androidx.benchmark.macro.junit4.BaselineProfileRule.collect
 */
private const val DEFAULT_BASELINE_PROFILE_MAX_ITERATIONS = 15

internal fun baselineProfileMaxIterations(): Int =
    InstrumentationRegistry.getArguments()
        .getString(
            BASELINE_PROFILE_MAX_ITERATIONS_ARGUMENT,
            DEFAULT_BASELINE_PROFILE_MAX_ITERATIONS.toString(),
        )
        ?.toIntOrNull()
        ?: DEFAULT_BASELINE_PROFILE_MAX_ITERATIONS
