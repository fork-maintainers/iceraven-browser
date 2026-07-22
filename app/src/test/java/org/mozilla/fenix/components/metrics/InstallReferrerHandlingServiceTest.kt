/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.utils.ext.packageManagerWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.components.fake.FakeMetricController
import org.mozilla.fenix.distributions.DistributionBrowserStoreProvider
import org.mozilla.fenix.distributions.DistributionIdManager
import org.mozilla.fenix.distributions.DistributionProviderChecker
import org.mozilla.fenix.distributions.DistributionSettings
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.nimbus.FxNimbus
import org.mozilla.fenix.nimbus.MarketingOnboardingCard
import org.mozilla.fenix.utils.Settings

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
internal class InstallReferrerHandlingServiceTest {

    private var providerValue: String? = null
    private var storedId: String? = null
    private var savedId: String = ""

    private val testDistributionProviderChecker = object : DistributionProviderChecker {
        override suspend fun queryProvider(): String? = providerValue
    }

    private val testBrowserStoreProvider = object : DistributionBrowserStoreProvider {
        override fun getDistributionId(): String? = storedId

        override fun updateDistributionId(id: String) {
            storedId = id
        }
    }

    private val testDistributionSettings = object : DistributionSettings {
        override fun getDistributionId(): String = savedId

        override fun saveDistributionId(id: String) {
            savedId = id
        }

        override fun setMarketingTelemetryPreferences() = Unit
    }

    val distributionIdManager = DistributionIdManager(
        packageManager = testContext.packageManagerWrapper,
        testBrowserStoreProvider,
        distributionProviderChecker = testDistributionProviderChecker,
        distributionSettings = testDistributionSettings,
        metricController = FakeMetricController(),
        appPreinstalledOnVivoDevice = { true },
    )

    @Before
    fun setUp() {
        every { testContext.components.settings } returns Settings(testContext)
        InstallReferrerHandlingService.response = null
        testContext.components.settings.shouldShowMarketingOnboarding = true
        FxNimbus.features.marketingOnboardingCard.withCachedValue(MarketingOnboardingCard(enabled = true))
    }

    @Test
    fun `GIVEN a null referrer on OK response WHEN start is called THEN response is not stored and shouldShowMarketingOnboarding is false`() =
        runTest {
            val service = fakeService(
                responseCode = InstallReferrerClient.InstallReferrerResponse.OK,
                referrerResponse = null,
                scope = this,
            )

            service.start()
            advanceUntilIdle()

            assertNull(InstallReferrerHandlingService.response)
            assertFalse(testContext.components.settings.shouldShowMarketingOnboarding)
        }

    @Test
    fun `GIVEN a non-null referrer on OK response WHEN start is called THEN response is stored`() {
        val referrer = "utm_source=addons.mozilla.org&utm_medium=referral&utm_content=rta%3Atest"
        val service = fakeService(responseCode = InstallReferrerClient.InstallReferrerResponse.OK, referrerResponse = referrer)

        service.start()

        assertEquals(referrer, InstallReferrerHandlingService.response)
    }

    @Test
    fun `GIVEN FEATURE_NOT_SUPPORTED WHEN start is called THEN shouldShowMarketingOnboarding is false`() {
        val service = fakeService(responseCode = InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED)

        service.start()

        assertNull(InstallReferrerHandlingService.response)
        assertFalse(testContext.components.settings.shouldShowMarketingOnboarding)
    }

    @Test
    fun `GIVEN DEVELOPER_ERROR WHEN start is called THEN shouldShowMarketingOnboarding is false`() {
        val service = fakeService(responseCode = InstallReferrerClient.InstallReferrerResponse.DEVELOPER_ERROR)

        service.start()

        assertFalse(testContext.components.settings.shouldShowMarketingOnboarding)
    }

    @Test
    fun `GIVEN SERVICE_UNAVAILABLE WHEN start is called THEN shouldShowMarketingOnboarding is false`() {
        val service = fakeService(responseCode = InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE)

        service.start()

        assertFalse(testContext.components.settings.shouldShowMarketingOnboarding)
    }

    @Test
    fun `GIVEN PERMISSION_ERROR WHEN start is called THEN shouldShowMarketingOnboarding is false`() {
        val service = fakeService(responseCode = InstallReferrerClient.InstallReferrerResponse.PERMISSION_ERROR)

        service.start()

        assertFalse(testContext.components.settings.shouldShowMarketingOnboarding)
    }

    @Test
    fun `GIVEN a service disconnect WHEN start is called THEN shouldShowMarketingOnboarding is false`() {
        val service = fakeService(simulateDisconnect = true)

        service.start()

        assertFalse(testContext.components.settings.shouldShowMarketingOnboarding)
    }

    private fun fakeService(
        responseCode: Int = InstallReferrerClient.InstallReferrerResponse.OK,
        referrerResponse: String? = null,
        simulateDisconnect: Boolean = false,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    ) = InstallReferrerHandlingService(testContext, scope = scope).apply {
        clientFactory = {
            FakeReferrerClient(
                responseCode = responseCode,
                referrerResponse = referrerResponse,
                simulateDisconnect = simulateDisconnect,
            )
        }
    }

    @Test
    fun `WHEN the marketing onboarding Nimbus flag is disabled THEN we should not show marketing onboarding`() =
        runBlocking {
            FxNimbus.features.marketingOnboardingCard.withCachedValue(MarketingOnboardingCard(enabled = false))
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclid=12345", distributionIdManager))
        }

    @Test
    fun `WHEN the marketing onboarding Nimbus flag is enabled THEN we should show marketing onboarding`() =
        runBlocking {
            assertTrue(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclid=12345", distributionIdManager))
        }

    @Test
    fun `WHEN installReferrerResponse is empty or null THEN we should not show marketing onboarding`() =
        runBlocking {
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding(null, distributionIdManager))
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("", distributionIdManager))
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding(" ", distributionIdManager))
        }

    @Test
    fun `WHEN installReferrerResponse is in the marketing prefixes THEN we should show marketing onboarding`() =
        runBlocking {
            assertTrue(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclid=", distributionIdManager))
            assertTrue(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclid=12345", distributionIdManager))
            assertTrue(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclid=CjwKCAjw&utm_source=google&utm_medium=cpc&utm_campaign=Search_Brand&utm_content=ad_variation_1&utm_term=firefox+browser", distributionIdManager))
            assertTrue(InstallReferrerHandlingService.shouldShowMarketingOnboarding("adjust_reftag=", distributionIdManager))
            assertTrue(InstallReferrerHandlingService.shouldShowMarketingOnboarding("adjust_reftag=test", distributionIdManager))
            assertTrue(InstallReferrerHandlingService.shouldShowMarketingOnboarding("adjust_reftag=abc123&utm_source=adjust&utm_medium=paid&utm_campaign=winter_promo&utm_content=banner_1&utm_term=", distributionIdManager))
        }

    @Test
    fun `WHEN installReferrerResponse is not in the marketing prefixes THEN we should not show marketing onboarding`() =
        runBlocking {
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding(" gclid=12345", distributionIdManager))
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("utm_source=google&utm_medium=cpc&utm_campaign=brand&utm_content=gclid%3D12345&utm_term=", distributionIdManager))
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("utm_source=google-play&utm_medium=organic", distributionIdManager))
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("utm_source=(not%20set)&utm_medium=(not%20set)", distributionIdManager))
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("utm_source=eea-browser-choice&utm_medium=preload", distributionIdManager))
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("utm_source=addons.mozilla.org&utm_medium=referral&utm_campaign=amo-fx-cta-869140&utm_content=rta%3AezU4YzMyYWM0LTBkNmMtNGQ2Zi1hZTJjLTk2YWFmOGZmY2I2Nn0&utm_term=", distributionIdManager))
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclida=", distributionIdManager))
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("adjust_reftag_test", distributionIdManager))
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("test", distributionIdManager))
        }

    @Test
    fun `GIVEN a partnership distribution that skips the consent screen WHEN referrer is present THEN we should not show marketing onboarding`() =
        runBlocking {
            distributionIdManager.setDistribution(DistributionIdManager.Distribution.VIVO_001)
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclid=12345", distributionIdManager))

            distributionIdManager.setDistribution(DistributionIdManager.Distribution.DT_001)
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclid=12345", distributionIdManager))

            distributionIdManager.setDistribution(DistributionIdManager.Distribution.DT_002)
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclid=12345", distributionIdManager))

            distributionIdManager.setDistribution(DistributionIdManager.Distribution.DT_003)
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclid=12345", distributionIdManager))

            distributionIdManager.setDistribution(DistributionIdManager.Distribution.XIAOMI_001)
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclid=12345", distributionIdManager))

            distributionIdManager.setDistribution(DistributionIdManager.Distribution.AURA_001)
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclid=12345", distributionIdManager))
        }

    @Test
    fun `GIVEN a partnership distribution that should show the consent screen THEN we should show marketing onboarding`() =
        runBlocking {
            val mockedDistributionIdManager = mockk<DistributionIdManager> {
                coEvery { isPartnershipDistribution() } returns true
                coEvery { shouldSkipMarketingConsentScreen() } returns false
            }
            assertTrue(InstallReferrerHandlingService.shouldShowMarketingOnboarding(null, mockedDistributionIdManager))
            assertTrue(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclid=12345", mockedDistributionIdManager))
        }

    @Test
    fun `WHEN installReferrerResponse is a Meta attribution THEN we should show marketing onboarding`() =
        runBlocking {
            val metaReferrer = """utm_source=apps.facebook.com&utm_medium=paid&utm_content={"app":12345,"t":1234567890,"source":{"data":"DATA","nonce":"NONCE"}}"""
            assertTrue(InstallReferrerHandlingService.shouldShowMarketingOnboarding(metaReferrer, distributionIdManager))
        }

    @Test
    fun `WHEN installReferrerResponse is null or blank or malformed THEN isMetaAttribution returns false`() {
        assertFalse(InstallReferrerHandlingService.isMetaAttribution(null))
        assertFalse(InstallReferrerHandlingService.isMetaAttribution(""))
        assertFalse(InstallReferrerHandlingService.isMetaAttribution(" "))

        val malformedReferrer = """utm_content={"app":12345,"t":1234567890,"source":{"data":"DATA","nonce":"NONCE"}"""
        assertFalse(InstallReferrerHandlingService.isMetaAttribution(malformedReferrer))
    }

    @Test
    fun `WHEN installReferrerResponse contains Meta utm_content params THEN isMetaAttribution returns true`() {
        val metaReferrer = """utm_content={"app":12345,"t":1234567890,"source":{"data":"DATA","nonce":"NONCE"}}"""
        assertTrue(InstallReferrerHandlingService.isMetaAttribution(metaReferrer))
    }

    @Test
    fun `WHEN installReferrerResponse missing Meta data or nonce THEN isMetaAttribution returns false`() {
        var metaReferrer = """utm_content={"app":12345,"t":1234567890,"source":{"nonce":"NONCE"}}"""
        assertFalse(InstallReferrerHandlingService.isMetaAttribution(metaReferrer))

        metaReferrer = """utm_content={"app":12345,"t":1234567890,"source":{"data":"DATA"}}"""
        assertFalse(InstallReferrerHandlingService.isMetaAttribution(metaReferrer))
    }

    @Test
    fun `WHEN installReferrerResponse does not contain Meta params THEN isMetaAttribution returns false`() {
        assertFalse(InstallReferrerHandlingService.isMetaAttribution("utm_source=google&utm_medium=cpc"))
        assertFalse(InstallReferrerHandlingService.isMetaAttribution("gclid=12345"))
        assertFalse(InstallReferrerHandlingService.isMetaAttribution("adjust_reftag=test"))
    }

    @Test
    fun `GIVEN a Meta-attributed referrer on OK response WHEN start is called THEN isUserMetaAttributed is true`() {
        val referrer =
            """utm_source=apps.facebook.com&utm_medium=paid&utm_content={"app":12345,"t":1234567890,"source":{"data":"DATA","nonce":"NONCE"}}"""
        val service = fakeService(
            responseCode = InstallReferrerClient.InstallReferrerResponse.OK,
            referrerResponse = referrer,
        )

        service.start()

        assertTrue(testContext.components.settings.isUserMetaAttributed)
    }

    @Test
    fun `GIVEN a non-Meta referrer on OK response WHEN start is called THEN isUserMetaAttributed is false`() {
        testContext.components.settings.isUserMetaAttributed = true
        val service = fakeService(
            responseCode = InstallReferrerClient.InstallReferrerResponse.OK,
            referrerResponse = "utm_source=google&utm_medium=cpc",
        )

        service.start()

        assertFalse(testContext.components.settings.isUserMetaAttributed)
    }

    @Test
    fun `WHEN installReferrerResponse is null or blank THEN isTikTokAttribution returns false`() {
        assertFalse(InstallReferrerHandlingService.isTikTokAttribution(null))
        assertFalse(InstallReferrerHandlingService.isTikTokAttribution(""))
        assertFalse(InstallReferrerHandlingService.isTikTokAttribution(" "))
    }

    @Test
    fun `WHEN installReferrerResponse has a dotted TikTok adjust_external_click_id THEN isTikTokAttribution returns true`() {
        assertTrue(
            InstallReferrerHandlingService.isTikTokAttribution(
                "adjust_external_click_id=E.C.P.C.04.AAAQzv8mYx",
            ),
        )
    }

    @Test
    fun `WHEN installReferrerResponse has an underscored TikTok adjust_external_click_id THEN isTikTokAttribution returns true`() {
        assertTrue(
            InstallReferrerHandlingService.isTikTokAttribution(
                "adjust_external_click_id=E_C_P_C_12_AAAQzv8mYx",
            ),
        )
    }

    @Test
    fun `WHEN installReferrerResponse has a lowercase TikTok adjust_external_click_id THEN isTikTokAttribution returns true`() {
        assertTrue(InstallReferrerHandlingService.isTikTokAttribution("adjust_external_click_id=e_c_p_c_abc_aaaqzv8myx"))
        assertTrue(InstallReferrerHandlingService.isTikTokAttribution("adjust_external_click_id%3De_c_p_c_08aaaBBB8myx"))
        assertTrue(InstallReferrerHandlingService.isTikTokAttribution("adjust_external_click_id%3DE_c_p_c_14a"))
        assertTrue(InstallReferrerHandlingService.isTikTokAttribution("adjust_external_click_id%3DE.c.P.c_24bbbCCc"))
    }

    @Test
    fun `WHEN installReferrerResponse has a malformed percent escape THEN isTikTokAttribution falls back to raw parsing`() {
        // The lone trailing % causes URLDecoder to throw IllegalArgumentException
        assertTrue(
            InstallReferrerHandlingService.isTikTokAttribution(
                "adjust_external_click_id=E_C_P_C_04_AAA&malformed=%",
            ),
        )
    }

    @Test
    fun `WHEN installReferrerResponse has a non-TikTok adjust_external_click_id THEN isTikTokAttribution returns false`() {
        assertFalse(
            InstallReferrerHandlingService.isTikTokAttribution(
                "adjust_external_click_id=EAIaIQobChMI4t7Y8KOM_wIVDpRoCR1RAQ7t",
            ),
        )
    }

    @Test
    fun `WHEN installReferrerResponse has no adjust_external_click_id THEN isTikTokAttribution returns false`() {
        assertFalse(InstallReferrerHandlingService.isTikTokAttribution("utm_source=google&utm_medium=cpc"))
    }

    @Test
    fun `GIVEN a TikTok-attributed referrer on OK response WHEN start is called THEN isUserTikTokAttributed is true`() {
        val referrer = "adjust_external_click_id=E.C.P.C.04.AAA&utm_medium=paid"
        val service = fakeService(
            responseCode = InstallReferrerClient.InstallReferrerResponse.OK,
            referrerResponse = referrer,
        )

        service.start()

        assertTrue(testContext.components.settings.isUserTikTokAttributed)
    }

    @Test
    fun `GIVEN a non-TikTok referrer on OK response WHEN start is called THEN isUserTikTokAttributed is false`() {
        testContext.components.settings.isUserTikTokAttributed = true
        val service = fakeService(
            responseCode = InstallReferrerClient.InstallReferrerResponse.OK,
            referrerResponse = "utm_source=google&utm_medium=cpc",
        )

        service.start()

        assertFalse(testContext.components.settings.isUserTikTokAttributed)
    }

    @Test
    fun `WHEN installReferrerResponse is a TikTok attribution THEN we should show marketing onboarding`() =
        runBlocking {
            val tiktokReferrer = "adjust_external_click_id=E.C.P.C.04.AAA&utm_medium=paid"
            assertTrue(InstallReferrerHandlingService.shouldShowMarketingOnboarding(tiktokReferrer, distributionIdManager))
        }

    @Test
    fun `WHEN installReferrerResponse is null or blank THEN isRedditAttribution returns false`() {
        assertFalse(InstallReferrerHandlingService.isRedditAttribution(null))
        assertFalse(InstallReferrerHandlingService.isRedditAttribution(""))
        assertFalse(InstallReferrerHandlingService.isRedditAttribution(" "))
    }

    @Test
    fun `WHEN installReferrerResponse has a Reddit adjust_external_click_id THEN isRedditAttribution returns true`() {
        assertTrue(
            InstallReferrerHandlingService.isRedditAttribution(
                "adjust_external_click_id=reddit_abc123XYZ",
            ),
        )
    }

    @Test
    fun `WHEN installReferrerResponse has a mixed-case Reddit adjust_external_click_id THEN isRedditAttribution returns true`() {
        assertTrue(InstallReferrerHandlingService.isRedditAttribution("adjust_external_click_id=Reddit_abc"))
        assertTrue(InstallReferrerHandlingService.isRedditAttribution("adjust_external_click_id=REDDIT_abc"))
        assertTrue(InstallReferrerHandlingService.isRedditAttribution("adjust_external_click_id%3Dreddit_abc"))
        assertTrue(InstallReferrerHandlingService.isRedditAttribution("adjust_external_click_id%3DReDdIt_abc"))
    }

    @Test
    fun `WHEN installReferrerResponse has utm_source reddit THEN isRedditAttribution returns true`() {
        assertTrue(InstallReferrerHandlingService.isRedditAttribution("utm_source=reddit&utm_medium=paid"))
    }

    @Test
    fun `WHEN installReferrerResponse has a mixed-case utm_source reddit THEN isRedditAttribution returns true`() {
        assertTrue(InstallReferrerHandlingService.isRedditAttribution("utm_source=Reddit&utm_medium=paid"))
        assertTrue(InstallReferrerHandlingService.isRedditAttribution("utm_source%3Dreddit&utm_medium=paid"))
    }

    @Test
    fun `WHEN installReferrerResponse has a utm_source that merely starts with reddit THEN isRedditAttribution returns false`() {
        assertFalse(InstallReferrerHandlingService.isRedditAttribution("utm_source=reddities&utm_medium=cpc"))
        assertFalse(InstallReferrerHandlingService.isRedditAttribution("utm_source=reddit_news&utm_medium=cpc"))
        assertFalse(InstallReferrerHandlingService.isRedditAttribution("utm_source=reddit news&utm_medium=cpc"))
    }

    @Test
    fun `WHEN installReferrerResponse has a malformed percent escape THEN isRedditAttribution falls back to raw parsing`() {
        assertTrue(
            InstallReferrerHandlingService.isRedditAttribution(
                "adjust_external_click_id=reddit_abc123&malformed=%",
            ),
        )
    }

    @Test
    fun `WHEN installReferrerResponse has a non-Reddit adjust_external_click_id THEN isRedditAttribution returns false`() {
        assertFalse(
            InstallReferrerHandlingService.isRedditAttribution(
                "adjust_external_click_id=E.C.P.C.04.AAAQzv8mYx",
            ),
        )
    }

    @Test
    fun `WHEN installReferrerResponse has no adjust_external_click_id THEN isRedditAttribution returns false`() {
        assertFalse(InstallReferrerHandlingService.isRedditAttribution("utm_source=google&utm_medium=cpc"))
    }

    @Test
    fun `GIVEN a Reddit-attributed referrer on OK response WHEN start is called THEN isUserRedditAttributed is true`() {
        val referrer = "adjust_external_click_id=reddit_abc123&utm_medium=paid"
        val service = fakeService(
            responseCode = InstallReferrerClient.InstallReferrerResponse.OK,
            referrerResponse = referrer,
        )

        service.start()

        assertTrue(testContext.components.settings.isUserRedditAttributed)
    }

    @Test
    fun `GIVEN a non-Reddit referrer on OK response WHEN start is called THEN isUserRedditAttributed is false`() {
        testContext.components.settings.isUserRedditAttributed = true
        val service = fakeService(
            responseCode = InstallReferrerClient.InstallReferrerResponse.OK,
            referrerResponse = "utm_source=google&utm_medium=cpc",
        )

        service.start()

        assertFalse(testContext.components.settings.isUserRedditAttributed)
    }

    @Test
    fun `WHEN installReferrerResponse is a Reddit attribution THEN we should show marketing onboarding`() =
        runBlocking {
            val redditReferrer = "adjust_external_click_id=reddit_abc123&utm_medium=paid"
            assertTrue(InstallReferrerHandlingService.shouldShowMarketingOnboarding(redditReferrer, distributionIdManager))
        }

    @Test
    fun `WHEN installReferrerResponse is null or blank THEN isXTwitterAttribution returns false`() {
        assertFalse(InstallReferrerHandlingService.isXTwitterAttribution(null))
        assertFalse(InstallReferrerHandlingService.isXTwitterAttribution(""))
        assertFalse(InstallReferrerHandlingService.isXTwitterAttribution(" "))
    }

    @Test
    fun `WHEN installReferrerResponse has utm_source x THEN isXTwitterAttribution returns true`() {
        assertTrue(InstallReferrerHandlingService.isXTwitterAttribution("utm_source=x&utm_medium=paid"))
    }

    @Test
    fun `WHEN installReferrerResponse has a mixed-case utm_source x THEN isXTwitterAttribution returns true`() {
        assertTrue(InstallReferrerHandlingService.isXTwitterAttribution("utm_source=X&utm_medium=paid"))
        assertTrue(InstallReferrerHandlingService.isXTwitterAttribution("utm_source%3Dx&utm_medium=paid"))
    }

    @Test
    fun `WHEN installReferrerResponse has a malformed percent escape THEN isXTwitterAttribution falls back to raw parsing`() {
        assertTrue(InstallReferrerHandlingService.isXTwitterAttribution("utm_source=x&malformed=%"))
    }

    @Test
    fun `WHEN installReferrerResponse has a non-X utm_source THEN isXTwitterAttribution returns false`() {
        assertFalse(InstallReferrerHandlingService.isXTwitterAttribution("utm_source=google&utm_medium=cpc"))
    }

    @Test
    fun `WHEN installReferrerResponse has a utm_source that merely starts with x THEN isXTwitterAttribution returns false`() {
        assertFalse(InstallReferrerHandlingService.isXTwitterAttribution("utm_source=xyz&utm_medium=cpc"))
        assertFalse(InstallReferrerHandlingService.isXTwitterAttribution("utm_source=x_yz&utm_medium=cpc"))
        assertFalse(InstallReferrerHandlingService.isXTwitterAttribution("utm_source=x yz&utm_medium=cpc"))
    }

    @Test
    fun `WHEN installReferrerResponse has no utm_source THEN isXTwitterAttribution returns false`() {
        assertFalse(InstallReferrerHandlingService.isXTwitterAttribution("adjust_external_click_id=reddit_abc"))
    }

    @Test
    fun `GIVEN an X-attributed referrer on OK response WHEN start is called THEN isUserXTwitterAttributed is true`() {
        val referrer = "utm_source=x&utm_medium=paid"
        val service = fakeService(
            responseCode = InstallReferrerClient.InstallReferrerResponse.OK,
            referrerResponse = referrer,
        )

        service.start()

        assertTrue(testContext.components.settings.isUserXTwitterAttributed)
    }

    @Test
    fun `GIVEN a non-X referrer on OK response WHEN start is called THEN isUserXTwitterAttributed is false`() {
        testContext.components.settings.isUserXTwitterAttributed = true
        val service = fakeService(
            responseCode = InstallReferrerClient.InstallReferrerResponse.OK,
            referrerResponse = "utm_source=google&utm_medium=cpc",
        )

        service.start()

        assertFalse(testContext.components.settings.isUserXTwitterAttributed)
    }

    @Test
    fun `WHEN installReferrerResponse is an X attribution THEN we should show marketing onboarding`() =
        runBlocking {
            val xReferrer = "utm_source=x&utm_medium=paid"
            assertTrue(InstallReferrerHandlingService.shouldShowMarketingOnboarding(xReferrer, distributionIdManager))
        }

    @Test
    fun `WHEN installReferrerResponse is null or blank THEN isMolocoAttribution returns false`() {
        assertFalse(InstallReferrerHandlingService.isMolocoAttribution(null))
        assertFalse(InstallReferrerHandlingService.isMolocoAttribution(""))
        assertFalse(InstallReferrerHandlingService.isMolocoAttribution(" "))
    }

    @Test
    fun `WHEN installReferrerResponse has a Moloco adjust_external_click_id THEN isMolocoAttribution returns true`() {
        assertTrue(
            InstallReferrerHandlingService.isMolocoAttribution(
                "adjust_external_click_id=moloco_9b1deb4d-3b7d-4bad-9bdd-2b0d7b3d41a6",
            ),
        )
    }

    @Test
    fun `WHEN installReferrerResponse has a mixed-case Moloco adjust_external_click_id THEN isMolocoAttribution returns true`() {
        assertTrue(InstallReferrerHandlingService.isMolocoAttribution("adjust_external_click_id=Moloco_abc"))
        assertTrue(InstallReferrerHandlingService.isMolocoAttribution("adjust_external_click_id=MOLOCO_abc"))
        assertTrue(InstallReferrerHandlingService.isMolocoAttribution("adjust_external_click_id%3Dmoloco_abc"))
        assertTrue(InstallReferrerHandlingService.isMolocoAttribution("adjust_external_click_id%3DMoLoCo_abc"))
    }

    @Test
    fun `WHEN installReferrerResponse has a malformed percent escape THEN isMolocoAttribution falls back to raw parsing`() {
        assertTrue(
            InstallReferrerHandlingService.isMolocoAttribution(
                "adjust_external_click_id=moloco_abc123&malformed=%",
            ),
        )
    }

    @Test
    fun `WHEN installReferrerResponse has a non-Moloco adjust_external_click_id THEN isMolocoAttribution returns false`() {
        assertFalse(
            InstallReferrerHandlingService.isMolocoAttribution(
                "adjust_external_click_id=reddit_abc123",
            ),
        )
    }

    @Test
    fun `WHEN installReferrerResponse has no adjust_external_click_id THEN isMolocoAttribution returns false`() {
        assertFalse(InstallReferrerHandlingService.isMolocoAttribution("utm_source=google&utm_medium=cpc"))
    }

    @Test
    fun `GIVEN a Moloco-attributed referrer on OK response WHEN start is called THEN isUserMolocoAttributed is true`() {
        val referrer = "adjust_external_click_id=moloco_9b1deb4d-3b7d-4bad-9bdd-2b0d7b3d41a6&utm_medium=paid"
        val service = fakeService(
            responseCode = InstallReferrerClient.InstallReferrerResponse.OK,
            referrerResponse = referrer,
        )

        service.start()

        assertTrue(testContext.components.settings.isUserMolocoAttributed)
    }

    @Test
    fun `GIVEN a non-Moloco referrer on OK response WHEN start is called THEN isUserMolocoAttributed is false`() {
        testContext.components.settings.isUserMolocoAttributed = true
        val service = fakeService(
            responseCode = InstallReferrerClient.InstallReferrerResponse.OK,
            referrerResponse = "utm_source=google&utm_medium=cpc",
        )

        service.start()

        assertFalse(testContext.components.settings.isUserMolocoAttributed)
    }

    @Test
    fun `WHEN installReferrerResponse is a Moloco attribution THEN we should show marketing onboarding`() =
        runBlocking {
            val molocoReferrer = "adjust_external_click_id=moloco_9b1deb4d-3b7d-4bad-9bdd-2b0d7b3d41a6&utm_medium=paid"
            assertTrue(
                InstallReferrerHandlingService.shouldShowMarketingOnboarding(molocoReferrer, distributionIdManager),
            )
        }
}

private class FakeReferrerClient(
    private val responseCode: Int = InstallReferrerClient.InstallReferrerResponse.OK,
    private val referrerResponse: String? = null,
    private val simulateDisconnect: Boolean = false,
) : InstallReferrerClientWrapper {

    override fun startConnection(listener: InstallReferrerStateListener) {
        if (simulateDisconnect) {
            listener.onInstallReferrerServiceDisconnected()
        } else {
            listener.onInstallReferrerSetupFinished(responseCode)
        }
    }

    override fun getInstallReferrer(): String? = referrerResponse

    override fun endConnection() = Unit
}
