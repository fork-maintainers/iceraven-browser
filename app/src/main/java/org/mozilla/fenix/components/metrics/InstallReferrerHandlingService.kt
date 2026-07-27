/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import android.content.Context
import android.os.RemoteException
import androidx.annotation.VisibleForTesting
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.fenix.distributions.DistributionIdManager
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.nimbus.FxNimbus
import java.net.URLDecoder

private const val GCLID_PREFIX = "gclid="
private const val ADJUST_REFTAG_PREFIX = "adjust_reftag="

/**
 * A service that fetches the install referrer and stores it for use after the user accepts
 * the Terms of Service.
 *
 * **WARNING:** This service IS started before the user accepts the Terms of Service.
 * Do NOT use the stored [InstallReferrerHandlingService.response] in any code path that fires
 * telemetry or makes network calls until after ToS is accepted.
 *
 * @param context The application context.
 * @param scope Coroutine scope used to launch background work.
 */
class InstallReferrerHandlingService(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) {
    private val logger = Logger("InstallReferrerHandlingService")

    @VisibleForTesting
    internal var clientFactory: (Context) -> InstallReferrerClientWrapper = ::DefaultInstallReferrerClient

    /**
     * Starts the connection with the install referrer and handle the response.
     */
    fun start() {
        val client = clientFactory(context)

        client.startConnection(
            object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    when (responseCode) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            // Connection established.
                            val installReferrerResponse = try {
                                client.getInstallReferrer()
                            } catch (e: RemoteException) {
                                // We can't do anything about this.
                                logger.error("Failed to retrieve install referrer response", e)
                                null
                            } catch (e: SecurityException) {
                                // https://issuetracker.google.com/issues/72926755
                                logger.error("Failed to retrieve install referrer response", e)
                                null
                            }

                            val distributionIdManager = context.components.distributionIdManager

                            if (!installReferrerResponse.isNullOrBlank()) {
                                response = installReferrerResponse
                                context.components.settings.isUserMetaAttributed =
                                    isMetaAttribution(installReferrerResponse)
                                context.components.settings.isUserTikTokAttributed =
                                    isTikTokAttribution(installReferrerResponse)
                                context.components.settings.isUserRedditAttributed =
                                    isRedditAttribution(installReferrerResponse)
                                context.components.settings.isUserXTwitterAttributed =
                                    isXTwitterAttribution(installReferrerResponse)
                                context.components.settings.isUserMolocoAttributed =
                                    isMolocoAttribution(installReferrerResponse)
                                context.components.settings.isUserRakutenAttributed =
                                    isRakutenAttribution(installReferrerResponse)
                                distributionIdManager.updateDistributionIdFromUtmParams(
                                    UTMParams.parseUTMParameters(installReferrerResponse),
                                )
                                scope.launch {
                                    distributionIdManager.startAdjustIfSkippingConsentScreen()
                                }
                            }

                            scope.launch {
                                context.components.settings.shouldShowMarketingOnboarding =
                                    shouldShowMarketingOnboarding(
                                        installReferrerResponse,
                                        distributionIdManager,
                                    )
                            }

                            safeEndConnection(client)
                            return
                        }

                        InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED,
                        InstallReferrerClient.InstallReferrerResponse.DEVELOPER_ERROR,
                        InstallReferrerClient.InstallReferrerResponse.PERMISSION_ERROR,
                        InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE,
                        InstallReferrerClient.InstallReferrerResponse.SERVICE_DISCONNECTED,
                            -> {
                            context.components.settings.shouldShowMarketingOnboarding = false
                            safeEndConnection(client)
                            return
                        }
                    }
                }

                override fun onInstallReferrerServiceDisconnected() {
                    context.components.settings.shouldShowMarketingOnboarding = false
                    safeEndConnection(client)
                }
            },
        )
    }

    /**
     * Companion object responsible for determine if a install referrer response should result in
     * showing the marketing onboarding flow.
     */
    companion object {
        private val marketingPrefixes = listOf(GCLID_PREFIX, ADJUST_REFTAG_PREFIX)

        @Suppress("TooGenericExceptionCaught")
        private fun safeEndConnection(client: InstallReferrerClientWrapper) {
            try {
                client.endConnection()
            } catch (e: Exception) {
                // endConnection can throw if the binding is already dead.
            }
        }

        /**
         * The raw install referrer string. Only read this after the user has accepted ToS —
         * do not use it to trigger telemetry or network calls before consent is given.
         */
        var response: String? = null

        @VisibleForTesting
        internal fun isMetaAttribution(installReferrerResponse: String?): Boolean {
            if (installReferrerResponse.isNullOrBlank()) {
                return false
            }

            val utmParams = UTMParams.parseUTMParameters(installReferrerResponse)
            return MetaParams.extractMetaAttribution(utmParams.content) != null
        }

        private const val ADJUST_EXTERNAL_CLICK_ID = "adjust_external_click_id"
        private val TIKTOK_EXTERNAL_CLICK_ID_PREFIXES = listOf("E.C.P.C", "E_C_P_C")
        private const val REDDIT_EXTERNAL_CLICK_ID_PREFIX = "reddit_"
        private const val REDDIT_UTM_SOURCE = "reddit"
        private const val X_TWITTER_UTM_SOURCE = "x"
        private const val MOLOCO_EXTERNAL_CLICK_ID_PREFIX = "moloco_"
        private const val RAKUTEN_UTM_SOURCE = "rakuten"

        private fun decodeInstallReferrer(installReferrerResponse: String): String =
            try {
                URLDecoder.decode(installReferrerResponse, "UTF-8")
            } catch (e: IllegalArgumentException) {
                Logger.error("decodeInstallReferrer() - bad installReferrerResponse", e)

                installReferrerResponse
            }

        @VisibleForTesting
        internal fun isTikTokAttribution(installReferrerResponse: String?): Boolean {
            if (installReferrerResponse.isNullOrBlank()) return false
            val decoded = decodeInstallReferrer(installReferrerResponse)

            val clickId = UTMParams.parseInstallReferrer(decoded)[ADJUST_EXTERNAL_CLICK_ID]
                ?: return false

            return TIKTOK_EXTERNAL_CLICK_ID_PREFIXES.any { clickId.startsWith(it, ignoreCase = true) }
        }

        @VisibleForTesting
        internal fun isRedditAttribution(installReferrerResponse: String?): Boolean {
            if (installReferrerResponse.isNullOrBlank()) return false
            val decoded = decodeInstallReferrer(installReferrerResponse)

            if (UTMParams.parseUTMParameters(decoded).source.equals(REDDIT_UTM_SOURCE, ignoreCase = true)) {
                return true
            }

            val clickId = UTMParams.parseInstallReferrer(decoded)[ADJUST_EXTERNAL_CLICK_ID]
                ?: return false

            return clickId.startsWith(REDDIT_EXTERNAL_CLICK_ID_PREFIX, ignoreCase = true)
        }

        @VisibleForTesting
        internal fun isXTwitterAttribution(installReferrerResponse: String?): Boolean {
            if (installReferrerResponse.isNullOrBlank()) return false
            val decoded = decodeInstallReferrer(installReferrerResponse)

            return UTMParams.parseUTMParameters(decoded).source.equals(X_TWITTER_UTM_SOURCE, ignoreCase = true)
        }

        @VisibleForTesting
        internal fun isMolocoAttribution(installReferrerResponse: String?): Boolean {
            if (installReferrerResponse.isNullOrBlank()) return false
            val decoded = decodeInstallReferrer(installReferrerResponse)

            val clickId = UTMParams.parseInstallReferrer(decoded)[ADJUST_EXTERNAL_CLICK_ID]
                ?: return false

            return clickId.startsWith(MOLOCO_EXTERNAL_CLICK_ID_PREFIX, ignoreCase = true)
        }

        @VisibleForTesting
        internal fun isRakutenAttribution(installReferrerResponse: String?): Boolean {
            if (installReferrerResponse.isNullOrBlank()) return false
            val decoded = decodeInstallReferrer(installReferrerResponse)

            return UTMParams.parseUTMParameters(decoded).source.equals(RAKUTEN_UTM_SOURCE, ignoreCase = true)
        }

        @Suppress("ReturnCount")
        @VisibleForTesting
        internal suspend fun shouldShowMarketingOnboarding(
            installReferrerResponse: String?,
            distributionIdManager: DistributionIdManager,
        ): Boolean {
            if (distributionIdManager.isPartnershipDistribution()) {
                return !distributionIdManager.shouldSkipMarketingConsentScreen()
            }

            if (installReferrerResponse.isNullOrBlank()) {
                return false
            }

            if (!FxNimbus.features.marketingOnboardingCard.value().enabled) {
                return false
            }

            if (isMetaAttribution(installReferrerResponse)) {
                return true
            }

            if (isTikTokAttribution(installReferrerResponse)) {
                return true
            }

            if (isRedditAttribution(installReferrerResponse)) {
                return true
            }

            if (isXTwitterAttribution(installReferrerResponse)) {
                return true
            }

            if (isMolocoAttribution(installReferrerResponse)) {
                return true
            }

            if (isRakutenAttribution(installReferrerResponse)) {
                return true
            }

            return marketingPrefixes.any { installReferrerResponse.startsWith(it, ignoreCase = true) }
        }
    }
}
