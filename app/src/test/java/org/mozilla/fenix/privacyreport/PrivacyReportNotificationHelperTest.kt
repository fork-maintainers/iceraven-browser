/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.privacyreport

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import kotlin.test.assertNotNull
import mozilla.components.support.base.android.NotificationsDelegate
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.GleanMetrics.TrackingProtection
import org.mozilla.fenix.helpers.FenixGleanTestRule
import org.mozilla.fenix.privacyreport.PrivacyReportNotificationAvailability.APP_NOTIFICATIONS_DISABLED
import org.mozilla.fenix.privacyreport.PrivacyReportNotificationAvailability.AVAILABLE
import org.mozilla.fenix.privacyreport.PrivacyReportNotificationAvailability.CHANNEL_MISSING
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class PrivacyReportNotificationHelperTest {

    @get:Rule val gleanRule = FenixGleanTestRule(testContext)

    private lateinit var notificationsDelegate: NotificationsDelegate

    private val content = PrivacyReportNotificationContent(title = "A title", text = "Some text")

    @Before
    fun setUp() {
        notificationsDelegate = NotificationsDelegate(NotificationManagerCompat.from(testContext))
    }

    @Test
    fun `WHEN ensurePrivacyReportNotificationChannelExists is called THEN the channel is created`() {
        val notificationManager = testContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertNull(notificationManager.getNotificationChannel(PRIVACY_REPORT_NOTIFICATION_CHANNEL_ID))

        val channelId = ensurePrivacyReportNotificationChannelExists(testContext)

        assertEquals(PRIVACY_REPORT_NOTIFICATION_CHANNEL_ID, channelId)
        assertNotNull(notificationManager.getNotificationChannel(PRIVACY_REPORT_NOTIFICATION_CHANNEL_ID))
    }

    @Test
    fun `GIVEN the channel already exists WHEN ensurePrivacyReportNotificationChannelExists is called THEN it is not recreated`() {
        ensurePrivacyReportNotificationChannelExists(testContext)
        val notificationManager = testContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = notificationManager.getNotificationChannel(PRIVACY_REPORT_NOTIFICATION_CHANNEL_ID)

        ensurePrivacyReportNotificationChannelExists(testContext)

        assertEquals(channel, notificationManager.getNotificationChannel(PRIVACY_REPORT_NOTIFICATION_CHANNEL_ID))
    }

    @Test
    fun `WHEN showPrivacyReportNotification is called THEN a notification is shown with click and dismiss intents`() {
        showPrivacyReportNotification(testContext, notificationsDelegate, content)

        val notificationManager = testContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifications = shadowOf(notificationManager).allNotifications
        assertEquals(1, notifications.size)
        val notification = notifications.first()
        assertNotNull(notification.contentIntent)
        assertNotNull(notification.deleteIntent)
    }

    @Test
    fun `WHEN showPrivacyReportNotification is called THEN the notification shows the content title and text`() {
        showPrivacyReportNotification(testContext, notificationsDelegate, content)

        val notificationManager = testContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = shadowOf(notificationManager).allNotifications.first()

        assertEquals(content.title, shadowOf(notification).contentTitle)
        assertEquals(content.text, shadowOf(notification).contentText)
    }

    @Test
    fun `WHEN showPrivacyReportNotification is called THEN the click intent opens the deep link directly and the dismiss intent targets the receiver`() {
        showPrivacyReportNotification(testContext, notificationsDelegate, content)

        val notificationManager = testContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = shadowOf(notificationManager).allNotifications.first()

        val clickIntent = shadowOf(notification.contentIntent).savedIntent
        assertEquals(Intent.ACTION_VIEW, clickIntent.action)
        assertEquals("${BuildConfig.DEEP_LINK_SCHEME}://privacy_report".toUri(), clickIntent.data)

        val dismissIntent = shadowOf(notification.deleteIntent).savedIntent
        assertEquals(ComponentName(testContext, PrivacyReportNotificationReceiver::class.java), dismissIntent.component)
        assertEquals(ACTION_PRIVACY_REPORT_NOTIFICATION_DISMISSED, dismissIntent.action)
    }

    @Test
    fun `WHEN showPrivacyReportNotification sends the notification THEN the sent event is recorded`() {
        showPrivacyReportNotification(testContext, notificationsDelegate, content)

        assertEquals(1, TrackingProtection.privacyReportNotificationSent.testGetValue()!!.size)
    }

    @Test
    fun `GIVEN notifications are disabled WHEN showPrivacyReportNotification is called THEN no sent event is recorded`() {
        val notificationManager = testContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowOf(notificationManager).setNotificationsEnabled(false)

        showPrivacyReportNotification(testContext, notificationsDelegate, content)

        assertEquals(0, shadowOf(notificationManager).allNotifications.size)
        assertNull(TrackingProtection.privacyReportNotificationSent.testGetValue())
    }

    @Test
    fun `GIVEN the channel exists WHEN privacyReportNotificationAvailability is called THEN it is available`() {
        ensurePrivacyReportNotificationChannelExists(testContext)

        assertEquals(AVAILABLE, privacyReportNotificationAvailability(NotificationManagerCompat.from(testContext)))
    }

    @Test
    fun `GIVEN the channel was never created WHEN privacyReportNotificationAvailability is called THEN the channel is missing`() {
        assertEquals(
            CHANNEL_MISSING,
            privacyReportNotificationAvailability(NotificationManagerCompat.from(testContext)),
        )
    }

    @Test
    fun `GIVEN app notifications are disabled and the channel was never created WHEN privacyReportNotificationAvailability is called THEN app notifications are disabled`() {
        val notificationManager = testContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowOf(notificationManager).setNotificationsEnabled(false)

        assertEquals(
            APP_NOTIFICATIONS_DISABLED,
            privacyReportNotificationAvailability(NotificationManagerCompat.from(testContext)),
        )
    }
}
