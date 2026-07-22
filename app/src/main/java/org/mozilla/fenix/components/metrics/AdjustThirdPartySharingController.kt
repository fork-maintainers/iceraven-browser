/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustThirdPartySharing

/**
 * Controls third-party data sharing settings for distribution and attribution partners.
 */
interface ThirdPartySharingController {

    /**
     * Enables data sharing exclusively for the given partner, disabling it for all others.
     *
     * @param partnerId The Adjust partner ID to enable sharing for.
     */
    fun enableThirdPartySharingForPartner(partnerId: String)

    /**
     * Disables data sharing with all third-party partners globally.
     */
    fun disableAllThirdPartySharing()
}

/**
 * [ThirdPartySharingController] implementation that delegates to the Adjust SDK.
 */
class AdjustThirdPartySharingController : ThirdPartySharingController {

    override fun enableThirdPartySharingForPartner(partnerId: String) {
        Adjust.trackThirdPartySharing(
            AdjustThirdPartySharing(true).apply {
                addPartnerSharingSetting("all", "all", false)
                addPartnerSharingSetting(partnerId, "all", true)
            },
        )
    }

    override fun disableAllThirdPartySharing() {
        Adjust.trackThirdPartySharing(
            AdjustThirdPartySharing(false),
        )
    }

    companion object {
        /** Adjust partner ID for Meta. */
        const val META_PARTNER_ID = "34"

        /** Adjust partner ID for Aura. */
        const val AURA_PARTNER_ID = "802"

        /** Adjust partner ID for Google. */
        const val GOOGLE_PARTNER_ID = "254"

        /** Adjust partner ID for TikTok. */
        const val TIKTOK_PARTNER_ID = "2337"

        /** Adjust partner ID for Reddit. */
        const val REDDIT_PARTNER_ID = "375"

        /** Adjust partner ID for X (Twitter). */
        const val X_TWITTER_PARTNER_ID = "32"

        /** Adjust partner ID for Moloco. */
        const val MOLOCO_PARTNER_ID = "56"
    }
}
