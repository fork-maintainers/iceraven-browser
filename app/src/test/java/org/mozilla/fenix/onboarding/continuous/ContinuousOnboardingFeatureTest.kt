/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding.continuous

import android.app.Activity
import android.content.Intent
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityOptionsCompat
import androidx.test.filters.SdkSuppress
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.utils.DateTimeProvider
import mozilla.components.support.utils.FakeDateTimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.GleanMetrics.Onboarding
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.FenixGleanTestRule
import org.mozilla.fenix.onboarding.OnboardingReason
import org.mozilla.fenix.onboarding.OnboardingTelemetryRecorder
import org.mozilla.fenix.onboarding.view.Action
import org.mozilla.fenix.onboarding.view.OnboardingPageState
import org.mozilla.fenix.onboarding.view.OnboardingPageUiData
import org.mozilla.fenix.utils.Settings
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertNotNull

private const val CONTINUOUS_ONBOARDING_DIALOG_TAG = "continuous_onboarding_dialog"

@RunWith(RobolectricTestRunner::class)
class ContinuousOnboardingFeatureTest {
    @get:Rule
    val gleanTestRule = FenixGleanTestRule(testContext)

    private lateinit var activity: Activity
    private lateinit var settings: Settings
    private lateinit var telemetryRecorder: OnboardingTelemetryRecorder
    private lateinit var stageProvider: ContinuousOnboardingStageProvider
    private lateinit var dateTimeProvider: DateTimeProvider
    private lateinit var feature: ContinuousOnboardingFeature

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(AppCompatActivity::class.java).create().get()
        settings = Settings(testContext)
        telemetryRecorder = OnboardingTelemetryRecorder(
            onboardingReason = OnboardingReason.NEW_USER,
            installSource = "test",
        )
        dateTimeProvider = FakeDateTimeProvider()
        stageProvider = FakeContinuousOnboardingStageProvider()
        feature = ContinuousOnboardingFeature(
            activity = activity,
            launcher = FakeActivityResultLauncher(),
            settings = settings,
            telemetryRecorder = telemetryRecorder,
            stageProvider = stageProvider,
            dateTimeProvider = dateTimeProvider,
            navigateToSyncSignIn = {},
        )
    }

    // shouldShowContinuousOnboarding

    @Test
    fun `WHEN feature disabled THEN shouldShowContinuousOnboarding returns false`() {
        settings.continuousOnboardingFeatureEnabled = false

        assertFalse(feature.shouldShowContinuousOnboarding())
    }

    @Test
    fun `WHEN feature enabled and seventh day completed THEN shouldShowContinuousOnboarding returns false`() {
        settings.continuousOnboardingFeatureEnabled = true
        settings.seventhDayOnboardingCompletedTimestamp = dateTimeProvider.currentTimeMillis()

        assertFalse(feature.shouldShowContinuousOnboarding())
    }

    @Test
    fun `WHEN feature enabled and seventh day not completed THEN shouldShowContinuousOnboarding returns true`() {
        settings.continuousOnboardingFeatureEnabled = true
        settings.seventhDayOnboardingCompletedTimestamp = -1

        assertTrue(feature.shouldShowContinuousOnboarding())
    }

    @Test
    fun `WHEN feature enabled and only second day completed THEN shouldShowContinuousOnboarding returns true`() {
        settings.continuousOnboardingFeatureEnabled = true
        settings.secondDayOnboardingCompletedTimestamp = dateTimeProvider.currentTimeMillis()

        assertTrue(feature.shouldShowContinuousOnboarding())
    }

    @Test
    fun `WHEN feature enabled and only third day completed THEN shouldShowContinuousOnboarding returns true`() {
        settings.continuousOnboardingFeatureEnabled = true
        settings.thirdDayOnboardingCompletedTimestamp = dateTimeProvider.currentTimeMillis()

        assertTrue(feature.shouldShowContinuousOnboarding())
    }

    // syncOnboardingPageState

    @Test
    fun `getSyncOnboardingPageState returns the expected state`() {
        val expectedState = OnboardingPageState(
            imageRes = R.drawable.nova_onboarding_sync,
            title = "Instantly pick up where you left off",
            description = "Grab bookmarks, passwords, and more on any device in a snap. Your personal data stays safe and secure with encryption.",
            primaryButton = Action(
                text = "Start syncing",
                onClick = {
                    telemetryRecorder.onSyncSignInClick(
                        sequenceId = OnboardingPageUiData.Type.SYNC_SIGN_IN.telemetryId,
                        sequencePosition = "0",
                    )
                },
            ),
            secondaryButton = Action(
                text = "Continue",
                onClick = {
                    telemetryRecorder.onSkipSignInClick(
                        sequenceId = OnboardingPageUiData.Type.SYNC_SIGN_IN.telemetryId,
                        sequencePosition = "0",
                    )
                },
            ),
            onRecordImpressionEvent = {
                telemetryRecorder.onImpression(
                    sequenceId = OnboardingPageUiData.Type.SYNC_SIGN_IN.telemetryId,
                    pageType = OnboardingPageUiData.Type.SYNC_SIGN_IN,
                    sequencePosition = "0",
                )
            },
        )
        val actualState = feature.getSyncOnboardingPageState(ContinuousOnboardingStage.DAY_7)

        assertEquals(expectedState.imageRes, actualState.imageRes)
        assertEquals(expectedState.title, actualState.title)
        assertEquals(expectedState.description, actualState.description)
        assertEquals(expectedState.primaryButton.text, actualState.primaryButton.text)
        assertEquals(expectedState.secondaryButton?.text, actualState.secondaryButton?.text)
    }

    @Test
    fun `WHEN sync primary button is clicked THEN sign-in telemetry is recorded`() {
        val pageState = feature.getSyncOnboardingPageState(ContinuousOnboardingStage.DAY_7)

        pageState.primaryButton.onClick()

        assertNotNull(Onboarding.signIn.testGetValue())
        val signInEvent = Onboarding.signIn.testGetValue()!!.single()
        assertEquals(OnboardingPageUiData.Type.SYNC_SIGN_IN.telemetryId, signInEvent.extra!!["sequence_id"])
        assertEquals("0", signInEvent.extra!!["sequence_position"])

        assertNotNull(Onboarding.completed.testGetValue())
        val completedEvent = Onboarding.completed.testGetValue()!!.single()
        assertEquals(OnboardingPageUiData.Type.SYNC_SIGN_IN.telemetryId, completedEvent.extra!!["sequence_id"])
        assertEquals("0", completedEvent.extra!!["sequence_position"])

        assertNotNull(Onboarding.dismissed.testGetValue())
        assertEquals("completed", Onboarding.dismissed.testGetValue()!!.single().extra!!["method"])

        assertEquals(-1L, settings.seventhDayOnboardingCompletedTimestamp)
    }

    @Test
    fun `WHEN sync primary button is clicked THEN navigateToSyncSignIn is invoked`() {
        var navigateToSyncSignInInvoked = false
        val navigateToSyncSignIn = { navigateToSyncSignInInvoked = true }
        val feature = ContinuousOnboardingFeature(
            activity = activity,
            launcher = FakeActivityResultLauncher(),
            settings = settings,
            telemetryRecorder = telemetryRecorder,
            stageProvider = stageProvider,
            dateTimeProvider = dateTimeProvider,
            navigateToSyncSignIn = navigateToSyncSignIn,
        )
        val pageState = feature.getSyncOnboardingPageState(ContinuousOnboardingStage.DAY_7)

        pageState.primaryButton.onClick()

        assertTrue(navigateToSyncSignInInvoked)
    }

    @Test
    fun `WHEN sync secondary button is clicked THEN skip sign-in telemetry is recorded`() {
        val pageState = feature.getSyncOnboardingPageState(ContinuousOnboardingStage.DAY_7)

        pageState.secondaryButton!!.onClick()

        assertNotNull(Onboarding.skipSignIn.testGetValue())
        val skipEvent = Onboarding.skipSignIn.testGetValue()!!.single()
        assertEquals(OnboardingPageUiData.Type.SYNC_SIGN_IN.telemetryId, skipEvent.extra!!["sequence_id"])
        assertEquals("0", skipEvent.extra!!["sequence_position"])

        assertNotNull(Onboarding.completed.testGetValue())
        val completedEvent = Onboarding.completed.testGetValue()!!.single()
        assertEquals(OnboardingPageUiData.Type.SYNC_SIGN_IN.telemetryId, completedEvent.extra!!["sequence_id"])
        assertEquals("0", completedEvent.extra!!["sequence_position"])

        assertNotNull(Onboarding.dismissed.testGetValue())
        assertEquals("skipped", Onboarding.dismissed.testGetValue()!!.single().extra!!["method"])

        assertEquals(dateTimeProvider.currentTimeMillis(), settings.seventhDayOnboardingCompletedTimestamp)
    }

    @Test
    fun `WHEN sync impression event fires THEN sign-in card telemetry is recorded`() {
        val pageState = feature.getSyncOnboardingPageState(ContinuousOnboardingStage.DAY_7)

        pageState.onRecordImpressionEvent()

        assertNotNull(Onboarding.signInCard.testGetValue())
        val event = Onboarding.signInCard.testGetValue()!!.single()
        assertEquals(OnboardingPageUiData.Type.SYNC_SIGN_IN.telemetryId, event.extra!!["sequence_id"])
        assertEquals("0", event.extra!!["sequence_position"])
    }

    @Test
    fun `WHEN no sync button is clicked THEN no sign-in telemetry is recorded`() {
        feature.getSyncOnboardingPageState(ContinuousOnboardingStage.DAY_7)

        assertNull(Onboarding.signIn.testGetValue())
        assertNull(Onboarding.skipSignIn.testGetValue())
        assertNull(Onboarding.signInCard.testGetValue())
    }

    // notificationOnboardingPageState

    @SdkSuppress(minSdkVersion = 33)
    @Test
    fun `getNotificationOnboardingPageState returns the expected state`() {
        val expectedState = OnboardingPageState(
            imageRes = R.drawable.nova_onboarding_notifications,
            title = "Notifications help you stay safer with Firefox",
            description = "Discover the latest privacy features in Firefox so you’re always up to date on how to stay protected.",
            primaryButton = Action(
                text = "Turn on notifications",
                onClick = {
                    telemetryRecorder.onNotificationPermissionClick(
                        sequenceId = OnboardingPageUiData.Type.NOTIFICATION_PERMISSION.telemetryId,
                        sequencePosition = "0",
                    )
                    activity.components.notificationsDelegate.requestNotificationPermission()
                },
            ),
            secondaryButton = Action(
                text = "Not now",
                onClick = {
                    telemetryRecorder.onSkipTurnOnNotificationsClick(
                        sequenceId = OnboardingPageUiData.Type.NOTIFICATION_PERMISSION.telemetryId,
                        sequencePosition = "0",
                    )
                },
            ),
            onRecordImpressionEvent = {
                telemetryRecorder.onImpression(
                    sequenceId = OnboardingPageUiData.Type.NOTIFICATION_PERMISSION.telemetryId,
                    pageType = OnboardingPageUiData.Type.NOTIFICATION_PERMISSION,
                    sequencePosition = "0",
                )
            },
        )

        val actualState = feature.getNotificationOnboardingPageState(ContinuousOnboardingStage.DAY_2)

        assertEquals(expectedState.imageRes, actualState.imageRes)
        assertEquals(expectedState.title, actualState.title)
        assertEquals(expectedState.description, actualState.description)
        assertEquals(expectedState.primaryButton.text, actualState.primaryButton.text)
        assertEquals(expectedState.secondaryButton?.text, actualState.secondaryButton?.text)
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    fun `WHEN notification primary button is clicked THEN turn on notification telemetry is recorded`() {
        val pageState = feature.getNotificationOnboardingPageState(ContinuousOnboardingStage.DAY_2)

        pageState.primaryButton.onClick()

        assertNotNull(Onboarding.turnOnNotifications.testGetValue())
        val event = Onboarding.turnOnNotifications.testGetValue()!!.single()
        assertEquals(OnboardingPageUiData.Type.NOTIFICATION_PERMISSION.telemetryId, event.extra!!["sequence_id"])
        assertEquals("0", event.extra!!["sequence_position"])

        assertEquals(dateTimeProvider.currentTimeMillis(), settings.secondDayOnboardingCompletedTimestamp)
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    fun `WHEN notification secondary button is clicked THEN skip notification telemetry is recorded`() {
        val pageState = feature.getNotificationOnboardingPageState(ContinuousOnboardingStage.DAY_2)

        pageState.secondaryButton!!.onClick()

        assertNotNull(Onboarding.skipTurnOnNotifications.testGetValue())
        val event = Onboarding.skipTurnOnNotifications.testGetValue()!!.single()
        assertEquals(OnboardingPageUiData.Type.NOTIFICATION_PERMISSION.telemetryId, event.extra!!["sequence_id"])
        assertEquals("0", event.extra!!["sequence_position"])

        assertEquals(dateTimeProvider.currentTimeMillis(), settings.secondDayOnboardingCompletedTimestamp)
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    fun `WHEN notification impression event fires THEN notification card telemetry is recorded`() {
        val pageState = feature.getNotificationOnboardingPageState(ContinuousOnboardingStage.DAY_2)

        pageState.onRecordImpressionEvent()

        assertNotNull(Onboarding.turnOnNotificationsCard.testGetValue())
        val event = Onboarding.turnOnNotificationsCard.testGetValue()!!.single()
        assertEquals(OnboardingPageUiData.Type.NOTIFICATION_PERMISSION.telemetryId, event.extra!!["sequence_id"])
        assertEquals("0", event.extra!!["sequence_position"])
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    fun `WHEN no notification button is clicked THEN no notification telemetry is recorded`() {
        feature.getNotificationOnboardingPageState(ContinuousOnboardingStage.DAY_2)

        assertNull(Onboarding.turnOnNotifications.testGetValue())
        assertNull(Onboarding.skipTurnOnNotifications.testGetValue())
        assertNull(Onboarding.turnOnNotificationsCard.testGetValue())
    }

    // markStageCompleted

    @Test
    fun `WHEN DAY_2 stage is completed THEN second day timestamp is saved`() {
        feature.markStageCompleted(ContinuousOnboardingStage.DAY_2)

        assertEquals(dateTimeProvider.currentTimeMillis(), settings.secondDayOnboardingCompletedTimestamp)
        assertEquals(-1L, settings.thirdDayOnboardingCompletedTimestamp)
        assertEquals(-1L, settings.seventhDayOnboardingCompletedTimestamp)
    }

    @Test
    fun `WHEN DAY_3 stage is completed THEN third day timestamp is saved`() {
        feature.markStageCompleted(ContinuousOnboardingStage.DAY_3)

        assertEquals(-1L, settings.secondDayOnboardingCompletedTimestamp)
        assertEquals(dateTimeProvider.currentTimeMillis(), settings.thirdDayOnboardingCompletedTimestamp)
        assertEquals(-1L, settings.seventhDayOnboardingCompletedTimestamp)
    }

    @Test
    fun `WHEN DAY_7 stage is completed THEN seventh day timestamp is saved`() {
        feature.markStageCompleted(ContinuousOnboardingStage.DAY_7)

        assertEquals(-1L, settings.secondDayOnboardingCompletedTimestamp)
        assertEquals(-1L, settings.thirdDayOnboardingCompletedTimestamp)
        assertEquals(dateTimeProvider.currentTimeMillis(), settings.seventhDayOnboardingCompletedTimestamp)
    }

    @Test
    fun `WHEN NONE stage is completed THEN no timestamps are changed`() {
        feature.markStageCompleted(ContinuousOnboardingStage.NONE)

        assertEquals(-1L, settings.secondDayOnboardingCompletedTimestamp)
        assertEquals(-1L, settings.thirdDayOnboardingCompletedTimestamp)
        assertEquals(-1L, settings.seventhDayOnboardingCompletedTimestamp)
    }

    // onDefaultBrowserStepCompleted

    @Test
    fun `WHEN default browser step completed with RESULT_OK THEN set-to-default telemetry is recorded`() {
        feature.onDefaultBrowserStepCompleted(Activity.RESULT_OK)

        assertNotNull(Onboarding.setToDefault.testGetValue())
        val event = Onboarding.setToDefault.testGetValue()!!.single()
        assertEquals(OnboardingPageUiData.Type.DEFAULT_BROWSER.telemetryId, event.extra!!["sequence_id"])
        assertEquals("0", event.extra!!["sequence_position"])
    }

    @Test
    fun `WHEN default browser step completed without RESULT_OK THEN no set-to-default telemetry is recorded`() {
        feature.onDefaultBrowserStepCompleted(Activity.RESULT_CANCELED)

        assertNull(Onboarding.setToDefault.testGetValue())
    }

    @Test
    fun `WHEN default browser step completed without RESULT_OK THEN pending stage is reset`() {
        feature.pendingStage = ContinuousOnboardingStage.DAY_2

        feature.onDefaultBrowserStepCompleted(Activity.RESULT_CANCELED)

        assertEquals(ContinuousOnboardingStage.NONE, feature.pendingStage)
    }

    @Test
    fun `WHEN default browser step completed with RESULT_OK THEN pending stage is reset`() {
        feature.pendingStage = ContinuousOnboardingStage.DAY_2

        feature.onDefaultBrowserStepCompleted(Activity.RESULT_OK)

        assertEquals(ContinuousOnboardingStage.NONE, feature.pendingStage)
    }

    // start() re-entrancy guard

    @Test
    fun `WHEN pendingStage is not none THEN start does not evaluate the stage`() {
        settings.continuousOnboardingFeatureEnabled = true
        val fakeStageProvider = FakeContinuousOnboardingStageProvider(ContinuousOnboardingStage.DAY_7)
        val feature = ContinuousOnboardingFeature(
            activity = activity,
            launcher = FakeActivityResultLauncher(),
            settings = settings,
            telemetryRecorder = telemetryRecorder,
            stageProvider = fakeStageProvider,
            dateTimeProvider = dateTimeProvider,
            navigateToSyncSignIn = {},
        )
        feature.pendingStage = ContinuousOnboardingStage.DAY_2

        feature.start()

        assertEquals(0, fakeStageProvider.callCount)
    }

    @Test
    fun `WHEN a continuous onboarding dialog is already showing THEN start does not evaluate the stage`() {
        settings.continuousOnboardingFeatureEnabled = true
        val fakeStageProvider = FakeContinuousOnboardingStageProvider(ContinuousOnboardingStage.DAY_7)
        val feature = ContinuousOnboardingFeature(
            activity = activity,
            launcher = FakeActivityResultLauncher(),
            settings = settings,
            telemetryRecorder = telemetryRecorder,
            stageProvider = fakeStageProvider,
            dateTimeProvider = dateTimeProvider,
            navigateToSyncSignIn = {},
        )
        val decorView = activity.window.decorView as ViewGroup
        decorView.addView(
            ComposeView(activity).apply {
                tag = CONTINUOUS_ONBOARDING_DIALOG_TAG
            },
        )

        feature.start()

        assertEquals(0, fakeStageProvider.callCount)
    }

    class FakeActivityResultLauncher : ActivityResultLauncher<Intent>() {
        override fun launch(input: Intent, options: ActivityOptionsCompat?) = Unit
        override fun unregister() = Unit
        override val contract: ActivityResultContract<Intent, *>
            get() = throw UnsupportedOperationException("Not used in tests.")
    }

    class FakeContinuousOnboardingStageProvider(
        private val stage: ContinuousOnboardingStage = ContinuousOnboardingStage.NONE,
    ) : ContinuousOnboardingStageProvider {
        var callCount = 0
            private set

        override fun getContinuousOnboardingStage(): ContinuousOnboardingStage {
            callCount++
            return stage
        }
    }
}
