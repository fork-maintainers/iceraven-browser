/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class GrowthDataWorkerTest {

    @Test
    fun `isAfterFirstWeekFromInstall is false within the first week and true at or after it`() {
        val installTime = 1_000_000L
        shadowOf(testContext.packageManager)
            .getInternalMutablePackageInfo(testContext.packageName)
            .firstInstallTime = installTime
        val oneWeek = TimeUnit.DAYS.toMillis(7)

        assertFalse(isAfterFirstWeekFromInstall(testContext, now = installTime + oneWeek - 1))
        assertTrue(isAfterFirstWeekFromInstall(testContext, now = installTime + oneWeek))
    }
}
