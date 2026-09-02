/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.perf

import android.app.ApplicationExitInfo
import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ApplicationExitInfoMetricsTest {

    @Test
    fun `TRACKED_REASONS includes system-driven reasons the user cannot control`() {
        val expected = listOf(
            ApplicationExitInfo.REASON_ANR,
            ApplicationExitInfo.REASON_CRASH,
            ApplicationExitInfo.REASON_CRASH_NATIVE,
            ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE,
            ApplicationExitInfo.REASON_LOW_MEMORY,
            ApplicationExitInfo.REASON_SIGNALED,
            ApplicationExitInfo.REASON_OTHER,
            ApplicationExitInfo.REASON_DEPENDENCY_DIED,
            ApplicationExitInfo.REASON_INITIALIZATION_FAILURE,
        )
        expected.forEach {
            assertTrue(
                "TRACKED_REASONS is missing reason code $it",
                ApplicationExitInfoMetrics.TRACKED_REASONS.contains(it),
            )
        }
    }

    @Test
    fun `TRACKED_REASONS includes REASON_FREEZER when running on A13+`() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            assertTrue(
                ApplicationExitInfoMetrics.TRACKED_REASONS.contains(ApplicationExitInfo.REASON_FREEZER),
            )
        }
    }

    @Test
    fun `TRACKED_REASONS excludes user-triggered reasons`() {
        val userTriggered = listOf(
            ApplicationExitInfo.REASON_USER_REQUESTED,
            ApplicationExitInfo.REASON_USER_STOPPED,
            ApplicationExitInfo.REASON_PERMISSION_CHANGE,
            ApplicationExitInfo.REASON_PACKAGE_UPDATED,
            ApplicationExitInfo.REASON_PACKAGE_STATE_CHANGE,
        )
        userTriggered.forEach {
            assertFalse(
                "TRACKED_REASONS should not include user-triggered reason $it",
                ApplicationExitInfoMetrics.TRACKED_REASONS.contains(it),
            )
        }
    }
}
