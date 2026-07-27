/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import mozilla.components.support.utils.ext.packageManagerCompatHelper
import org.mozilla.fenix.ext.components
import java.util.concurrent.TimeUnit

/**
 * Worker that will send the User Activated event at the end of the first week.
 */
class GrowthDataWorker(
    context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val settings = applicationContext.components.settings
        val metrics = applicationContext.components.analytics.metrics

        if (!isAfterFirstWeekFromInstall(applicationContext) ||
            settings.growthUserActivatedSent
        ) {
            return Result.success()
        }

        metrics.track(Event.GrowthData.ConversionEvent7(fromSearch = false))

        return Result.success()
    }

    companion object {
        private const val GROWTH_USER_ACTIVATED_WORK_NAME = "org.mozilla.fenix.growth.work"

        /**
         * Schedules the Activated User event if needed.
         */
        fun sendActivatedSignalIfNeeded(context: Context) {
            val instanceWorkManager = WorkManager.getInstance(context)

            if (context.components.settings.growthUserActivatedSent) {
                return
            }

            val growthSignalWork = OneTimeWorkRequest.Builder(GrowthDataWorker::class.java)
                .setInitialDelay(FULL_WEEK_MILLIS, TimeUnit.MILLISECONDS)
                .build()

            instanceWorkManager.beginUniqueWork(
                GROWTH_USER_ACTIVATED_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                growthSignalWork,
            ).enqueue()
        }
    }
}

private const val DAY_MILLIS: Long = 1000 * 60 * 60 * 24
private const val FULL_WEEK_MILLIS: Long = DAY_MILLIS * 7

/**
 * Returns whether [now] is at least a full week after the application was first installed.
 */
internal fun isAfterFirstWeekFromInstall(
    context: Context,
    now: Long = System.currentTimeMillis(),
): Boolean = FULL_WEEK_MILLIS <= now - getInstalledTime(context)

private fun getInstalledTime(context: Context): Long = context.packageManagerCompatHelper
    .getPackageInfoCompat(context.packageName, 0)
    .firstInstallTime
