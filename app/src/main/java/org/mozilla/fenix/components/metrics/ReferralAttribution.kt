/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import org.mozilla.fenix.GleanMetrics.Pings
import org.mozilla.fenix.GleanMetrics.Referrals
import org.mozilla.fenix.utils.Settings

/**
 * The preamble marking a `utm_content` value as a Firefox referral code.
 *
 * There is no separator between the preamble and the code: a referral `utm_content` is exactly
 * `fxrefer` followed by the code. Being unreserved ASCII, the preamble survives the single
 * URL-decode the Play Store applies to its `referrer` query parameter unchanged, whatever the
 * link's encoding; [UTMParams.parseInstallReferrer] does no further decoding.
 *
 * This exact spelling is a contract with whoever generates referral links. The match is
 * case-sensitive, so a link varying the case of the preamble will NOT be recognized, and the code
 * it carries will reach `client_info.attribution` and the Nimbus context, where it lives for the
 * lifespan of the install. That failure is silent, so correctness rests on link construction.
 *
 * Conversely, with no separator to anchor on, any `utm_content` that merely begins with `fxrefer`
 * is treated as a referral and dropped from attribution data. That direction fails safe.
 */
internal const val REFERRAL_UTM_CONTENT_PREFIX = "fxrefer"

private const val UTM_CONTENT_PARAM_PREFIX = "${UTMParams.UTM_CONTENT}="

/**
 * Extracts a Firefox referral code from the Play install referrer and reports it on the dedicated
 * `referrals` ping.
 *
 * A referral code is a quasi-identifier, so it must never reach lifespan-of-install telemetry.
 * Everything here exists to keep it out of [UTMParams], `client_info.attribution`, the
 * `play-store-attribution` ping and the Nimbus context. See bug 2062793, and bug 2055255 for the
 * desktop equivalent this mirrors.
 */
object ReferralAttribution {

    /**
     * The referral code carried by [utmContent], or `null` if it carries none.
     *
     * Whatever follows the preamble is returned unvalidated: codes are expected to be 16
     * characters, but neither platform checks, so a length change needs no client update.
     */
    fun referralCodeFrom(utmContent: String): String? {
        if (!utmContent.startsWith(REFERRAL_UTM_CONTENT_PREFIX)) {
            return null
        }

        return utmContent.removePrefix(REFERRAL_UTM_CONTENT_PREFIX).ifEmpty { null }
    }

    /**
     * Submits [referralCode] on the `referrals` ping, at most once per profile.
     *
     * @param referralCode The referral code, with the preamble already stripped.
     * @param settings Application settings, used to record that the ping has been submitted.
     */
    fun submit(referralCode: String, settings: Settings) {
        if (settings.referralPingSubmitted) {
            return
        }

        Referrals.code.set(referralCode)
        Pings.referrals.submit()
        settings.referralPingSubmitted = true
    }

    /**
     * [installReferrerResponse] with any referral-carrying `utm_content` parameter removed
     * entirely, so that a referral install is indistinguishable from one that carried no
     * `utm_content` at all. This matches the blanking of [UTMParams.content] in
     * `InstallReferrerWorker.handleSuccess`: referral installs are counted from the `referrals`
     * ping alone, never from attribution data. Returns the response unchanged when it carries no
     * referral code.
     *
     * Every matching occurrence is dropped, not just the one
     * [UTMParams.parseInstallReferrer] would have selected. That parser keeps the last value for a
     * repeated key, so a malformed referrer repeating `utm_content` could otherwise leave a code
     * behind in the raw response after the parsed copy was found clean.
     */
    fun redact(installReferrerResponse: String): String =
        installReferrerResponse.split("&")
            .filterNot { param ->
                param.startsWith(UTM_CONTENT_PARAM_PREFIX) &&
                    referralCodeFrom(param.removePrefix(UTM_CONTENT_PARAM_PREFIX)) != null
            }
            .joinToString("&")
}
