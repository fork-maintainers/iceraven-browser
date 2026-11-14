/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.privacyreport

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mozilla.components.feature.session.TrackingProtectionUseCases
import mozilla.components.support.base.android.NotificationsDelegate
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.utils.DateTimeProvider
import mozilla.components.support.utils.DefaultDateTimeProvider
import org.mozilla.fenix.GleanMetrics.Pings
import org.mozilla.fenix.GleanMetrics.TrackingProtection
import org.mozilla.fenix.utils.Settings

private const val PRIVACY_REPORT_NOTIFICATION_WORK_NAME = "org.mozilla.fenix.privacyreport.work"
private const val NOTIFICATION_TIME_MILLIS_KEY = "privacy_report_notification_time_millis"
private val WEEK_IN_MILLIS = TimeUnit.DAYS.toMillis(7)
private val logger = Logger("PrivacyReportNotificationWorker")

/** Weekly blocked-tracker counts over this show the "trackers blocked" notification variant. */
private const val SHOW_NOTIFICATION_THRESHOLD = 47
private const val PRIVACY_REPORT_INTERVAL_DAYS = 7L
/** Start of the notification window, at noon in the user's local time. */
private const val NOTIFICATION_WINDOW_START_HOUR = 12
/** End of the notification window, at 5:00 p.m. in the user's local time. */
private const val NOTIFICATION_WINDOW_END_HOUR = 17

/**
 * Background [CoroutineWorker] that shows the weekly privacy report notification, summarizing how many trackers were
 * blocked over the past week.
 *
 * Instantiated by [PrivacyReportWorkerFactory], not WorkManager's default reflective factory.
 *
 * @param applicationContext Setup parameter for [CoroutineWorker].
 * @param workerParameters Setup parameter for a [CoroutineWorker].
 * @param settings Used to check whether tracking protection is enabled.
 * @param trackingProtectionUseCases Used to fetch the past week's blocked trackers.
 * @param notificationsDelegate Used to request notification permission and post the notification.
 * @param dateTimeProvider Used to access the current time.
 */
class PrivacyReportNotificationWorker(
    applicationContext: Context,
    workerParameters: WorkerParameters,
    private val settings: Settings,
    private val trackingProtectionUseCases: TrackingProtectionUseCases,
    private val notificationsDelegate: NotificationsDelegate,
    private val dateTimeProvider: DateTimeProvider = DefaultDateTimeProvider(),
) : CoroutineWorker(applicationContext, workerParameters) {
    override suspend fun doWork(): Result {
        // The intended target timestamp, used as an anchor for the next notification time.
        val scheduledNotificationTimeMillis = inputData.getLong(NOTIFICATION_TIME_MILLIS_KEY, -1L)

        if (scheduledNotificationTimeMillis < 0) {
            logger.error("Missing scheduled notification timestamp. The worker cannot maintain the weekly cadence.")
            return Result.failure()
        }

        try {
            recordWorkerRun()
            ensurePrivacyReportNotificationChannelExists(applicationContext)

            val notSentReason = notSentReason()
            if (notSentReason != null) {
                logger.info("Not sending the privacy report notification. The reason is : $notSentReason")
                TrackingProtection.privacyReportNotificationNotSent.record(
                    TrackingProtection.PrivacyReportNotificationNotSentExtra(reason = notSentReason.telemetryId)
                )
                return Result.success()
            }

            // Tracking protection could have been disabled since the worker was scheduled —
            // updatePrivacyReportNotificationWorker() only re-evaluates this on HomeActivity.onResume, so re-check here
            // before doing any work.
            if (settings.shouldUseTrackingProtection) {
                val trackersBlockedCount = fetchTrackersBlockedThisWeek()
                logger.info("trackersBlockedCount is $trackersBlockedCount")

                val content =
                    if (trackersBlockedCount > SHOW_NOTIFICATION_THRESHOLD) {
                        logger.info("trackersBlockedCount is above the notification threshold")
                        PrivacyReportNotificationContent.trackersBlocked(applicationContext, trackersBlockedCount)
                    } else {
                        logger.info("trackersBlockedCount is below the notification threshold")
                        PrivacyReportNotificationContent.noTrackersBlocked(applicationContext)
                    }

                showPrivacyReportNotification(applicationContext, notificationsDelegate, content)
            }
        } finally {
            Pings.privacyReportNotification.submit(Pings.privacyReportNotificationReasonCodes.workerRun)

            if (!isStopped && settings.weeklyPrivacyNotificationFeatureFlagEnabled) {
                // Reschedule based on the intended time (not actual execution time) to avoid drift from worker delays.
                scheduleNext(
                    context = applicationContext,
                    dateTimeProvider = dateTimeProvider,
                    previousNotificationTimeMillis = scheduledNotificationTimeMillis,
                )
            }
        }

        return Result.success()
    }

    /**
     * Report that the worker ran. The run count is incremented before any work so that runs cancelled before the ping
     * is submitted show up as gaps in the submitted run counts.
     */
    private fun recordWorkerRun() {
        TrackingProtection.privacyReportNotificationWorkerRunCount.add()
        TrackingProtection.privacyReportNotificationWorkerRan.record()
    }

    /**
     * Returns why the app could not send a privacy report notification, or `null` if there are no reasons preventing it
     * from doing so.
     */
    private fun notSentReason(): PrivacyReportNotificationAvailability? {
        // Tracking protection could have been disabled since the worker was scheduled —
        // updatePrivacyReportNotificationWorker() only re-evaluates this on HomeActivity.onResume, so re-check here
        // before doing any work.
        if (!settings.shouldUseTrackingProtection) {
            return PrivacyReportNotificationAvailability.TRACKING_PROTECTION_DISABLED
        }

        return privacyReportNotificationAvailability(notificationsDelegate.notificationManagerCompat).takeIf {
            it != PrivacyReportNotificationAvailability.AVAILABLE
        }
    }

    private suspend fun fetchTrackersBlockedThisWeek(): Int {
        if (settings.debugForceWeeklyPrivacyReportNotification) {
            return SHOW_NOTIFICATION_THRESHOLD + 1
        }

        val now = dateTimeProvider.currentTimeMillis()
        val oneWeekAgo = now - WEEK_IN_MILLIS

        // fetchTrackingEvents queries Gecko's content-blocking database, which needs a Looper.
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                trackingProtectionUseCases.fetchTrackingEvents(
                    dateFrom = oneWeekAgo,
                    dateTo = now,
                    onSuccess = { events ->
                        logger.info(
                            "Successfully fetched tracking events for the privacy report notification. " +
                                "Event count: ${events.size}"
                        )
                        continuation.resume(events.sumOf { it.count })
                    },
                    onError = { error ->
                        logger.error("Failed to fetch tracking events for the privacy report notification", error)
                        continuation.resume(0)
                    },
                )
            }
        }
    }

    companion object {
        /**
         * Schedule the first privacy report notification. The first occurrence is anchored to onboarding completion.
         *
         * Callers must ensure that onboarding has been completed before calling this function.
         */
        fun schedule(
            context: Context,
            settings: Settings,
            dateTimeProvider: DateTimeProvider = DefaultDateTimeProvider(),
        ) {
            val onboardingCompletedTimestamp = settings.onboardingCompletedTimestamp

            val zoneId = dateTimeProvider.currentZoneId()
            val now = dateTimeProvider.currentTimeMillis()

            val scheduledNotificationTimeMillis =
                nextClampedNotificationTimeMillis(
                    anchorMillis = onboardingCompletedTimestamp,
                    nowMillis = now,
                    zoneId = zoneId,
                )

            enqueueNotificationWork(
                context = context,
                policy = ExistingWorkPolicy.KEEP,
                notificationTimeMillis = scheduledNotificationTimeMillis,
                nowMillis = now,
            )

            logger.info("Registered the privacy report notification worker.")

            recordWorkerScheduled(now)
        }

        /**
         * Schedules the next occurrence. [previousNotificationTimeMillis] is the intended notification time of the
         * previous occurrence, not the actual execution time, to prevent schedule drift.
         */
        private fun scheduleNext(
            context: Context,
            dateTimeProvider: DateTimeProvider,
            previousNotificationTimeMillis: Long,
        ) {
            val zoneId = dateTimeProvider.currentZoneId()
            val now = dateTimeProvider.currentTimeMillis()

            val scheduledNotificationTimeMillis =
                nextClampedNotificationTimeMillis(
                    anchorMillis = previousNotificationTimeMillis,
                    nowMillis = now,
                    zoneId = zoneId,
                )

            enqueueNotificationWork(
                context = context,
                policy = ExistingWorkPolicy.REPLACE,
                notificationTimeMillis = scheduledNotificationTimeMillis,
                nowMillis = now,
            )

            logger.info("Rescheduled the privacy report notification worker for its next occurrence.")
        }

        /**
         * Creates the OneTimeWorkRequest.
         *
         * The intended notification time is stored in the WorkRequest's input data. This is important because
         * [setInitialDelay] alone does not preserve the absolute target timestamp for us.
         */
        private fun enqueueNotificationWork(
            context: Context,
            policy: ExistingWorkPolicy,
            notificationTimeMillis: Long,
            nowMillis: Long,
        ) {
            val initialDelay = (notificationTimeMillis - nowMillis).coerceAtLeast(0L)

            val inputData = workDataOf(NOTIFICATION_TIME_MILLIS_KEY to notificationTimeMillis)

            val request =
                OneTimeWorkRequestBuilder<PrivacyReportNotificationWorker>()
                    .setInputData(inputData)
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .build()

            WorkManager.getInstance(context).enqueueUniqueWork(PRIVACY_REPORT_NOTIFICATION_WORK_NAME, policy, request)
        }

        /**
         * Calculates the next notification time [PRIVACY_REPORT_INTERVAL_DAYS] days after the anchor. The anchor should
         * be the intended notification time, never the actual worker execution time.
         */
        @VisibleForTesting
        internal fun nextNotificationTimeMillis(
            anchorMillis: Long,
            zoneId: ZoneId,
        ): Long =
            clampToNotificationWindow(
                Instant.ofEpochMilli(anchorMillis).atZone(zoneId).plusDays(PRIVACY_REPORT_INTERVAL_DAYS)
            )

        /**
         * Computes the next notification time from [anchorMillis], falling back to the next valid window starting from
         * [nowMillis] if that time has already passed. This guards both the first scheduling (anchored to onboarding
         * completion) and every subsequent occurrence (anchored to the previous intended time) against a long-overdue
         * anchor — e.g. after being idle for more than [PRIVACY_REPORT_INTERVAL_DAYS] days — which would otherwise fire
         * immediately, outside the notification window.
         */
        @VisibleForTesting
        internal fun nextClampedNotificationTimeMillis(
            anchorMillis: Long,
            nowMillis: Long,
            zoneId: ZoneId,
        ): Long {
            val nextNotificationTimeMillis = nextNotificationTimeMillis(anchorMillis, zoneId)
            if (nextNotificationTimeMillis > nowMillis) {
                return nextNotificationTimeMillis
            }

            return clampToNotificationWindow(Instant.ofEpochMilli(nowMillis).atZone(zoneId))
        }

        /**
         * Clamps [dateTime] into the notification time window, moving it forward to the nearest window start if needed.
         */
        private fun clampToNotificationWindow(dateTime: ZonedDateTime): Long {
            val clampedDateTime =
                when {
                    dateTime.hour < NOTIFICATION_WINDOW_START_HOUR ->
                        dateTime.withHour(NOTIFICATION_WINDOW_START_HOUR).withMinute(0).withSecond(0).withNano(0)

                    dateTime.hour < NOTIFICATION_WINDOW_END_HOUR -> dateTime

                    else ->
                        dateTime
                            .plusDays(1)
                            .withHour(NOTIFICATION_WINDOW_START_HOUR)
                            .withMinute(0)
                            .withSecond(0)
                            .withNano(0)
                }

            return clampedDateTime.toInstant().toEpochMilli()
        }

        /** Cancel the [PrivacyReportNotificationWorker]. */
        fun cancel(context: Context) {
            logger.info("Cancelling the privacy report notification worker...")

            try {
                WorkManager.getInstance(context).cancelUniqueWork(PRIVACY_REPORT_NOTIFICATION_WORK_NAME)
                logger.info("Privacy report notification worker cancellation completed.")
            } catch (e: CancellationException) {
                logger.debug(
                    "Stopped waiting for privacy report notification worker cancellation because" +
                        " the coroutine was cancelled."
                )
            }

            TrackingProtection.privacyReportNotificationWorkerScheduled.set(false)
        }

        /** Report that the worker is scheduled. */
        private fun recordWorkerScheduled(nowMillis: Long) {
            TrackingProtection.privacyReportNotificationScheduledAt.set(Date(nowMillis))
            TrackingProtection.privacyReportNotificationWorkerScheduled.set(true)
        }
    }
}
