/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adjust.sdk.AdjustConfig
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.components.metrics.AdjustThirdPartySharingController.Companion.AURA_PARTNER_ID
import org.mozilla.fenix.components.metrics.AdjustThirdPartySharingController.Companion.GOOGLE_PARTNER_ID
import org.mozilla.fenix.components.metrics.AdjustThirdPartySharingController.Companion.META_PARTNER_ID
import org.mozilla.fenix.components.metrics.AdjustThirdPartySharingController.Companion.MOLOCO_PARTNER_ID
import org.mozilla.fenix.components.metrics.AdjustThirdPartySharingController.Companion.REDDIT_PARTNER_ID
import org.mozilla.fenix.components.metrics.AdjustThirdPartySharingController.Companion.TIKTOK_PARTNER_ID
import org.mozilla.fenix.components.metrics.AdjustThirdPartySharingController.Companion.X_TWITTER_PARTNER_ID
import org.mozilla.fenix.distributions.DistributionAdjustStartupStrategy
import org.mozilla.fenix.distributions.DistributionIdManager
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.FenixGleanTestRule
import org.mozilla.fenix.utils.Settings
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
internal class AdjustMetricsServiceTest {
    val context: Context = ApplicationProvider.getApplicationContext()
    val thirdPartySharingController = mockk<ThirdPartySharingController>(relaxed = true)
    val conversionEventRecorder = mockk<ConversionEventRecorder>(relaxed = true)

    @get:Rule
    val gleanTestRule = FenixGleanTestRule(ApplicationProvider.getApplicationContext())

    @Before
    fun setUp() {
        every { testContext.components.settings } returns Settings(testContext)
        every { testContext.components.distributionIdManager } returns mockk<DistributionIdManager>(relaxed = true) {
            coEvery { getDistribution() } returns DistributionIdManager.Distribution.DEFAULT
            coEvery { getDistributionAdjustStartupStrategy() } returns DistributionAdjustStartupStrategy.NONE
        }
    }

    private fun TestScope.createAdjustMetricsService(
        adjustSdk: AdjustSdkController,
        storage: MetricsStorage = mockk(relaxed = true),
    ) = AdjustMetricsService(
        application = testContext as Application,
        storage = storage,
        crashReporter = mockk(relaxed = true),
        dispatcher = UnconfinedTestDispatcher(testScheduler),
        adjustSdk = adjustSdk,
        adjustToken = "fake-adjust-token",
        thirdPartySharingController = mockk(relaxed = true),
    )

    @Test
    fun `WHEN start is called AND attribution is already known THEN Adjust is not initialized`() =
        runTest {
            every { testContext.components.settings } returns
                Settings(testContext).apply { adjustCampaignId = "campaign" }
            val adjustSdk = mockk<AdjustSdkController>(relaxed = true)
            val service = createAdjustMetricsService(adjustSdk)

            service.start()
            advanceUntilIdle()

            verify(exactly = 0) { adjustSdk.initSdk(any()) }
            verify(exactly = 0) { adjustSdk.enable() }
        }

    @Test
    fun `WHEN attribution is unknown on start THEN Adjust is initialized with an attribution listener`() =
        runTest {
            val adjustSdk = mockk<AdjustSdkController>(relaxed = true)
            val configSlot = slot<AdjustConfig>()
            every { adjustSdk.initSdk(capture(configSlot)) } just Runs
            val service = createAdjustMetricsService(adjustSdk)

            service.start()
            advanceUntilIdle()

            verify { adjustSdk.initSdk(any()) }
            verify { adjustSdk.enable() }
            assertNotNull(configSlot.captured.onAttributionChangedListener)
        }

    @Test
    fun `WHEN stop is called THEN forget-me is sent before init without attribution tracking`() =
        runTest {
            val adjustSdk = mockk<AdjustSdkController>(relaxed = true)
            val configSlot = slot<AdjustConfig>()
            every { adjustSdk.initSdk(capture(configSlot)) } just Runs
            val service = createAdjustMetricsService(adjustSdk)

            service.stop()
            advanceUntilIdle()

            verifyOrder {
                adjustSdk.gdprForgetMe(any())
                adjustSdk.initSdk(any())
            }
            assertNull(configSlot.captured.onAttributionChangedListener)
        }

    @Test
    fun `WHEN stop is called with service already started THEN forget-me is sent without re-init`() =
        runTest {
            val adjustSdk = mockk<AdjustSdkController>(relaxed = true)
            val configSlot = slot<AdjustConfig>()
            every { adjustSdk.initSdk(capture(configSlot)) } just Runs
            val service = createAdjustMetricsService(adjustSdk)

            service.start()
            advanceUntilIdle()

            service.stop()
            advanceUntilIdle()

            verifyOrder {
                adjustSdk.initSdk(any())
                adjustSdk.enable()
                adjustSdk.gdprForgetMe(any())
            }
        }

    @Test
    fun `WHEN Adjust is used multiple times THEN it is initialized only once`() =
        runTest {
            val adjustSdk = mockk<AdjustSdkController>(relaxed = true)
            val storage = mockk<MetricsStorage> {
                coEvery { shouldTrack(any()) } returns true
                coEvery { updateSentState(any()) } just Runs
            }
            val service = createAdjustMetricsService(adjustSdk, storage)

            service.track(Event.GrowthData.ConversionEvent1)
            service.track(Event.GrowthData.ConversionEvent2)
            advanceUntilIdle()

            verify(exactly = 1) { adjustSdk.initSdk(any()) }
        }

    @Test
    fun `WHEN a growth event should be tracked THEN it is sent through the Adjust SDK and its sent state is updated`() =
        runTest {
            val adjustSdk = mockk<AdjustSdkController>(relaxed = true)
            val storage = mockk<MetricsStorage> {
                coEvery { shouldTrack(any()) } returns true
                coEvery { updateSentState(any()) } just Runs
            }
            val service = AdjustMetricsService(
                application = testContext as Application,
                storage = storage,
                crashReporter = mockk(relaxed = true),
                dispatcher = UnconfinedTestDispatcher(testScheduler),
                adjustSdk = adjustSdk,
            )

            service.track(Event.GrowthData.ConversionEvent1)
            advanceUntilIdle()

            verify { adjustSdk.trackEvent(any()) }
            coVerify { storage.updateSentState(Event.GrowthData.ConversionEvent1) }
        }

    @Test
    fun `WHEN a growth event should not be tracked THEN Adjust is neither started nor used`() =
        runTest {
            val adjustSdk = mockk<AdjustSdkController>(relaxed = true)
            val storage = mockk<MetricsStorage> {
                coEvery { shouldTrack(any()) } returns false
            }
            val service = AdjustMetricsService(
                application = testContext as Application,
                storage = storage,
                crashReporter = mockk(relaxed = true),
                dispatcher = UnconfinedTestDispatcher(testScheduler),
                adjustSdk = adjustSdk,
            )

            service.track(Event.GrowthData.ConversionEvent1)
            advanceUntilIdle()

            verify(exactly = 0) { adjustSdk.initSdk(any()) }
            verify(exactly = 0) { adjustSdk.trackEvent(any()) }
            coVerify(exactly = 0) { storage.updateSentState(any()) }
        }

    @Test
    fun `WHEN Adjust attribution data already exist THEN already known is true`() {
        val settings = Settings(context)
        assertFalse(AdjustMetricsService.alreadyKnown(settings))

        settings.adjustCampaignId = "campaign"
        assertTrue(AdjustMetricsService.alreadyKnown(settings))

        settings.adjustCampaignId = ""
        assertFalse(AdjustMetricsService.alreadyKnown(settings))

        settings.adjustNetwork = "network"
        assertTrue(AdjustMetricsService.alreadyKnown(settings))

        settings.adjustNetwork = ""
        assertFalse(AdjustMetricsService.alreadyKnown(settings))

        settings.adjustAdGroup = "ad group"
        assertTrue(AdjustMetricsService.alreadyKnown(settings))

        settings.adjustAdGroup = ""
        assertFalse(AdjustMetricsService.alreadyKnown(settings))

        settings.adjustCreative = "creative"
        assertTrue(AdjustMetricsService.alreadyKnown(settings))
    }

    @Test
    fun `WHEN the distribution is DEFAULT AND the user is meta attributed THEN sharing is enabled for META`() {
        AdjustMetricsService.applyThirdPartySharingSettings(
            distribution = DistributionIdManager.Distribution.DEFAULT,
            isUserMetaAttributed = true,
            isUserTikTokAttributed = false,
            isUserRedditAttributed = false,
            isUserXTwitterAttributed = false,
            isUserMolocoAttributed = false,
            controller = thirdPartySharingController,
        )

        verify { thirdPartySharingController.enableThirdPartySharingForPartner(META_PARTNER_ID) }
    }

    @Test
    fun `WHEN the distribution is DEFAULT AND the user has no Meta, TikTok, Reddit, or X attribution THEN sharing is enabled for Google`() {
        AdjustMetricsService.applyThirdPartySharingSettings(
            distribution = DistributionIdManager.Distribution.DEFAULT,
            isUserMetaAttributed = false,
            isUserTikTokAttributed = false,
            isUserRedditAttributed = false,
            isUserXTwitterAttributed = false,
            isUserMolocoAttributed = false,
            controller = thirdPartySharingController,
        )

        verify { thirdPartySharingController.enableThirdPartySharingForPartner(GOOGLE_PARTNER_ID) }
    }

    @Test
    fun `WHEN the distribution is DEFAULT AND the user is TikTok attributed THEN sharing is enabled for TikTok`() {
        AdjustMetricsService.applyThirdPartySharingSettings(
            distribution = DistributionIdManager.Distribution.DEFAULT,
            isUserMetaAttributed = false,
            isUserTikTokAttributed = true,
            isUserRedditAttributed = false,
            isUserXTwitterAttributed = false,
            isUserMolocoAttributed = false,
            controller = thirdPartySharingController,
        )

        verify { thirdPartySharingController.enableThirdPartySharingForPartner(TIKTOK_PARTNER_ID) }
    }

    @Test
    fun `WHEN the distribution is DEFAULT AND the user is Reddit attributed THEN sharing is enabled for Reddit`() {
        AdjustMetricsService.applyThirdPartySharingSettings(
            distribution = DistributionIdManager.Distribution.DEFAULT,
            isUserMetaAttributed = false,
            isUserTikTokAttributed = false,
            isUserRedditAttributed = true,
            isUserXTwitterAttributed = false,
            isUserMolocoAttributed = false,
            controller = thirdPartySharingController,
        )

        verify { thirdPartySharingController.enableThirdPartySharingForPartner(REDDIT_PARTNER_ID) }
    }

    @Test
    fun `WHEN the distribution is DEFAULT AND the user is X attributed THEN sharing is enabled for X`() {
        AdjustMetricsService.applyThirdPartySharingSettings(
            distribution = DistributionIdManager.Distribution.DEFAULT,
            isUserMetaAttributed = false,
            isUserTikTokAttributed = false,
            isUserRedditAttributed = false,
            isUserXTwitterAttributed = true,
            isUserMolocoAttributed = false,
            controller = thirdPartySharingController,
        )

        verify { thirdPartySharingController.enableThirdPartySharingForPartner(X_TWITTER_PARTNER_ID) }
    }

    @Test
    fun `WHEN the distribution is DEFAULT AND the user is Moloco attributed THEN sharing is enabled for Moloco`() {
        AdjustMetricsService.applyThirdPartySharingSettings(
            distribution = DistributionIdManager.Distribution.DEFAULT,
            isUserMetaAttributed = false,
            isUserTikTokAttributed = false,
            isUserRedditAttributed = false,
            isUserXTwitterAttributed = false,
            isUserMolocoAttributed = true,
            controller = thirdPartySharingController,
        )

        verify { thirdPartySharingController.enableThirdPartySharingForPartner(MOLOCO_PARTNER_ID) }
    }

    @Test
    fun `WHEN the distribution is AURA_001 THEN sharing is enabled for Aura`() {
        AdjustMetricsService.applyThirdPartySharingSettings(
            distribution = DistributionIdManager.Distribution.AURA_001,
            isUserMetaAttributed = false,
            isUserTikTokAttributed = false,
            isUserRedditAttributed = false,
            isUserXTwitterAttributed = false,
            isUserMolocoAttributed = false,
            controller = thirdPartySharingController,
        )

        verify { thirdPartySharingController.enableThirdPartySharingForPartner(AURA_PARTNER_ID) }
    }

    @Test
    fun `WHEN the distribution is VIVO_001 THEN all sharing is disabled`() {
        AdjustMetricsService.applyThirdPartySharingSettings(
            distribution = DistributionIdManager.Distribution.VIVO_001,
            isUserMetaAttributed = false,
            isUserTikTokAttributed = false,
            isUserRedditAttributed = false,
            isUserXTwitterAttributed = false,
            isUserMolocoAttributed = false,
            controller = thirdPartySharingController,
        )

        verify { thirdPartySharingController.disableAllThirdPartySharing() }
    }

    @Test
    fun `WHEN the distribution is DT_001 THEN all sharing is disabled`() {
        AdjustMetricsService.applyThirdPartySharingSettings(
            distribution = DistributionIdManager.Distribution.DT_001,
            isUserMetaAttributed = false,
            isUserTikTokAttributed = false,
            isUserRedditAttributed = false,
            isUserXTwitterAttributed = false,
            isUserMolocoAttributed = false,
            controller = thirdPartySharingController,
        )

        verify { thirdPartySharingController.disableAllThirdPartySharing() }
    }

    @Test
    fun `WHEN the distribution is DT_002 THEN all sharing is disabled`() {
        AdjustMetricsService.applyThirdPartySharingSettings(
            distribution = DistributionIdManager.Distribution.DT_002,
            isUserMetaAttributed = false,
            isUserTikTokAttributed = false,
            isUserRedditAttributed = false,
            isUserXTwitterAttributed = false,
            isUserMolocoAttributed = false,
            controller = thirdPartySharingController,
        )

        verify { thirdPartySharingController.disableAllThirdPartySharing() }
    }

    @Test
    fun `WHEN the distribution is DT_003 THEN all sharing is disabled`() {
        AdjustMetricsService.applyThirdPartySharingSettings(
            distribution = DistributionIdManager.Distribution.DT_003,
            isUserMetaAttributed = false,
            isUserTikTokAttributed = false,
            isUserRedditAttributed = false,
            isUserXTwitterAttributed = false,
            isUserMolocoAttributed = false,
            controller = thirdPartySharingController,
        )

        verify { thirdPartySharingController.disableAllThirdPartySharing() }
    }

    @Test
    fun `WHEN the distribution is XIAOMI_001 THEN all sharing is disabled`() {
        AdjustMetricsService.applyThirdPartySharingSettings(
            distribution = DistributionIdManager.Distribution.XIAOMI_001,
            isUserMetaAttributed = false,
            isUserTikTokAttributed = false,
            isUserRedditAttributed = false,
            isUserXTwitterAttributed = false,
            isUserMolocoAttributed = false,
            controller = thirdPartySharingController,
        )

        verify { thirdPartySharingController.disableAllThirdPartySharing() }
    }

    @Test
    fun `GIVEN a ConversionEvent1 event WHEN sendGleanEventAndPing is called THEN the event is recorded and the ping is submitted`() {
        AdjustMetricsService.sendGleanEventAndPing(
            Event.GrowthData.ConversionEvent1,
            conversionEventRecorder,
        )

        verify { conversionEventRecorder.recordConversionEvent(1) }
    }

    @Test
    fun `GIVEN a ConversionEvent2 event WHEN sendGleanEventAndPing is called THEN the event is recorded and the ping is submitted`() {
        AdjustMetricsService.sendGleanEventAndPing(
            Event.GrowthData.ConversionEvent2,
            conversionEventRecorder,
        )

        verify { conversionEventRecorder.recordConversionEvent(2) }
    }

    @Test
    fun `GIVEN a ConversionEvent3 event WHEN sendGleanEventAndPing is called THEN the event is recorded and the ping is submitted`() {
        AdjustMetricsService.sendGleanEventAndPing(
            Event.GrowthData.ConversionEvent3,
            conversionEventRecorder,
        )

        verify { conversionEventRecorder.recordConversionEvent(3) }
    }

    @Test
    fun `GIVEN a ConversionEvent4 event WHEN sendGleanEventAndPing is called THEN the event is recorded and the ping is submitted`() {
        AdjustMetricsService.sendGleanEventAndPing(
            Event.GrowthData.ConversionEvent4,
            conversionEventRecorder,
        )

        verify { conversionEventRecorder.recordConversionEvent(4) }
    }

    @Test
    fun `GIVEN a ConversionEvent5 event WHEN sendGleanEventAndPing is called THEN the event is recorded and the ping is submitted`() {
        AdjustMetricsService.sendGleanEventAndPing(
            Event.GrowthData.ConversionEvent5,
            conversionEventRecorder,
        )

        verify { conversionEventRecorder.recordConversionEvent(5) }
    }

    @Test
    fun `GIVEN a ConversionEvent6 event WHEN sendGleanEventAndPing is called THEN the event is recorded and the ping is submitted`() {
        AdjustMetricsService.sendGleanEventAndPing(
            Event.GrowthData.ConversionEvent6,
            conversionEventRecorder,
        )

        verify { conversionEventRecorder.recordConversionEvent(6) }
    }

    @Test
    fun `GIVEN a ConversionEvent7 event WHEN sendGleanEventAndPing is called THEN the event is recorded and the ping is submitted`() {
        AdjustMetricsService.sendGleanEventAndPing(
            Event.GrowthData.ConversionEvent7(fromSearch = true),
            conversionEventRecorder,
        )

        verify { conversionEventRecorder.recordConversionEvent(7) }
    }

    @Test
    fun `GIVEN a ConversionEvent8 event WHEN sendGleanEventAndPing is called THEN the event is recorded and the ping is submitted`() {
        AdjustMetricsService.sendGleanEventAndPing(
            Event.FirstWeekPostInstall.ConversionEvent8,
            conversionEventRecorder,
        )

        verify { conversionEventRecorder.recordConversionEvent(8) }
    }

    @Test
    fun `GIVEN a ConversionEvent9 event WHEN sendGleanEventAndPing is called THEN the event is recorded and the ping is submitted`() {
        AdjustMetricsService.sendGleanEventAndPing(
            Event.FirstWeekPostInstall.ConversionEvent9,
            conversionEventRecorder,
        )

        verify { conversionEventRecorder.recordConversionEvent(9) }
    }

    @Test
    fun `GIVEN a ConversionEvent10 event WHEN sendGleanEventAndPing is called THEN the event is recorded and the ping is submitted`() {
        AdjustMetricsService.sendGleanEventAndPing(
            Event.FirstWeekPostInstall.ConversionEvent10,
            conversionEventRecorder,
        )

        verify { conversionEventRecorder.recordConversionEvent(10) }
    }
}
