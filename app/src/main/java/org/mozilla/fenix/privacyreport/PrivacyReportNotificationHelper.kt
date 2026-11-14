/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.privacyreport

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import mozilla.components.support.base.android.NotificationsDelegate
import mozilla.components.support.base.ext.areNotificationsEnabledSafe
import mozilla.components.support.base.ids.SharedIdsHelper
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.GleanMetrics.TrackingProtection
import org.mozilla.fenix.R
import org.mozilla.fenix.utils.IntentUtils
import org.mozilla.fenix.utils.createBaseNotification

const val PRIVACY_REPORT_NOTIFICATION_CHANNEL_ID = "org.mozilla.fenix.privacyreport.channel"

private const val PRIVACY_REPORT_NOTIFICATION_TAG = "org.mozilla.fenix.privacyreport.tag"
private const val PRIVACY_REPORT_PENDING_INTENT_TAG = "org.mozilla.fenix.privacyreport.pending_intent"

/**
 * The privacy report notification availability states.
 *
 * @property telemetryId The value recorded in telemetry for this state.
 */
internal enum class PrivacyReportNotificationAvailability(val telemetryId: String) {
    /** Nothing stops the privacy report notification from being posted. */
    AVAILABLE("available"),

    /** Tracking protection was turned off after the worker was scheduled. */
    TRACKING_PROTECTION_DISABLED("tracking_protection_disabled"),

    /** The user turned off notifications for the whole app, so no notification can be posted. */
    APP_NOTIFICATIONS_DISABLED("app_notifications_disabled"),

    /** App notifications are on, but the user turned off the privacy report channel. */
    CHANNEL_DISABLED("channel_disabled"),

    /**
     * App notifications are on, but the privacy report channel does not exist, so no notification can be posted. The
     * channel is always created before this is checked, so this is likely caused by a programmer error.
     */
    CHANNEL_MISSING("channel_missing"),
}

/**
 * Ensures that the notification channel for the weekly privacy report exists, creating it if necessary, and returns its
 * ID.
 *
 * @param context Used to access the [NotificationManager] system service and required string resources.
 *
 * **Note:** If the privacy report becomes a permanent feature, this will be refactored to inject the notification
 * manager through an abstraction, removing the direct dependency on [NotificationManager].
 *
 * See https://bugzilla.mozilla.org/show_bug.cgi?id=2062560.
 *
 * @return the channel ID to be used for notifications.
 */
fun ensurePrivacyReportNotificationChannelExists(context: Context): String {
    val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val channel =
        notificationManager.getNotificationChannel(PRIVACY_REPORT_NOTIFICATION_CHANNEL_ID)
            ?: NotificationChannel(
                    PRIVACY_REPORT_NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.notification_privacy_report_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
                .apply {
                    description =
                        context.getString(
                            R.string.notification_privacy_report_channel_description,
                            context.getString(R.string.app_name),
                        )
                }

    notificationManager.createNotificationChannel(channel)

    return PRIVACY_REPORT_NOTIFICATION_CHANNEL_ID
}

/**
 * Whether the weekly privacy report notification can currently be sent. If notification cannot be posted, then return
 * the reason.
 *
 * @param notificationManager The [NotificationManagerCompat] used to check app-level notification settings.
 */
internal fun privacyReportNotificationAvailability(
    notificationManager: NotificationManagerCompat
): PrivacyReportNotificationAvailability {
    val channel = notificationManager.getNotificationChannelCompat(PRIVACY_REPORT_NOTIFICATION_CHANNEL_ID)

    return when {
        !notificationManager.areNotificationsEnabledSafe() ->
            PrivacyReportNotificationAvailability.APP_NOTIFICATIONS_DISABLED
        channel == null -> PrivacyReportNotificationAvailability.CHANNEL_MISSING
        channel.importance == NotificationManagerCompat.IMPORTANCE_NONE ->
            PrivacyReportNotificationAvailability.CHANNEL_DISABLED
        else -> PrivacyReportNotificationAvailability.AVAILABLE
    }
}

/**
 * Build and display the weekly privacy report notification.
 *
 * @param context Used to look up the [NotificationManager] system service and required string resources.
 * @param notificationsDelegate Used to request notification permission and post the notification.
 * @param content The content of the notification.
 */
fun showPrivacyReportNotification(
    context: Context,
    notificationsDelegate: NotificationsDelegate,
    content: PrivacyReportNotificationContent,
) {
    notificationsDelegate.notify(
        PRIVACY_REPORT_NOTIFICATION_TAG,
        SharedIdsHelper.getIdForTag(context, PRIVACY_REPORT_NOTIFICATION_TAG),
        buildPrivacyReportNotification(context, content),
        onPermissionGranted = { TrackingProtection.privacyReportNotificationSent.record() },
    )
}

private fun buildPrivacyReportNotification(
    context: Context,
    content: PrivacyReportNotificationContent,
): Notification =
    createBaseNotification(
        context = context,
        channelId = ensurePrivacyReportNotificationChannelExists(context),
        title = content.title,
        text = content.text,
        onClick = createClickPendingIntent(context),
        onDismiss = createDismissPendingIntent(context),
    )

/**
 * The click action launches the deep link activity directly, via [PendingIntent.getActivity], rather than routing
 * through [PrivacyReportNotificationReceiver]. A broadcast receiver that calls `startActivity` in response to a
 * notification tap is a "notification trampoline", which Android discourages as background-activity-launch territory
 * and may stop working in a future release. See https://developer.android.com/guide/components/activities/secure-bal.
 */
private fun createClickPendingIntent(context: Context): PendingIntent {
    val intent =
        Intent(
                Intent.ACTION_VIEW,
                "${BuildConfig.DEEP_LINK_SCHEME}://privacy_report".toUri(),
            )
            .apply {
                setPackage(context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

    return PendingIntent.getActivity(
        context,
        SharedIdsHelper.getNextIdForTag(context, PRIVACY_REPORT_PENDING_INTENT_TAG),
        intent,
        IntentUtils.DEFAULT_PENDING_INTENT_FLAGS,
    )
}

private fun createDismissPendingIntent(context: Context): PendingIntent {
    val intent =
        Intent(context, PrivacyReportNotificationReceiver::class.java)
            .setAction(ACTION_PRIVACY_REPORT_NOTIFICATION_DISMISSED)

    return PendingIntent.getBroadcast(
        context,
        SharedIdsHelper.getNextIdForTag(context, PRIVACY_REPORT_PENDING_INTENT_TAG),
        intent,
        IntentUtils.DEFAULT_PENDING_INTENT_FLAGS,
    )
}
