/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.GleanMetrics.Pings
import org.mozilla.fenix.GleanMetrics.Referrals
import org.mozilla.fenix.helpers.FenixGleanTestRule
import org.mozilla.fenix.utils.Settings
import org.robolectric.RobolectricTestRunner

private const val REFERRAL_CODE = "0123456789ABCXYZ"

@RunWith(RobolectricTestRunner::class)
class ReferralAttributionTest {

    @get:Rule
    val gleanTestRule = FenixGleanTestRule(testContext)

    @Test
    fun `GIVEN a referral utm_content WHEN extracting THEN the code after the preamble is returned`() {
        assertEquals(
            REFERRAL_CODE,
            ReferralAttribution.referralCodeFrom("$REFERRAL_UTM_CONTENT_PREFIX$REFERRAL_CODE"),
        )
    }

    @Test
    fun `GIVEN a referral code of unexpected length WHEN extracting THEN the code is returned unchanged`() {
        assertEquals("AB", ReferralAttribution.referralCodeFrom("${REFERRAL_UTM_CONTENT_PREFIX}AB"))
    }

    @Test
    fun `GIVEN a bare referral preamble WHEN extracting THEN null is returned`() {
        assertNull(ReferralAttribution.referralCodeFrom(REFERRAL_UTM_CONTENT_PREFIX))
    }

    // The preamble carries no separator, so a link generator still emitting the old `fxrefer:`
    // spelling is recognized, but folds the leftover colon into the code and reports a value the
    // referral service will not match. Pinned because the miss is silent on both sides.
    @Test
    fun `GIVEN a referral preamble followed by a separator WHEN extracting THEN the separator is part of the code`() {
        assertEquals(
            ":$REFERRAL_CODE",
            ReferralAttribution.referralCodeFrom("fxrefer:$REFERRAL_CODE"),
        )
    }

    // Pins the case-sensitive contract described on REFERRAL_UTM_CONTENT_PREFIX: an unrecognized
    // preamble means the code reaches attribution data. Changing this to expect a match is a
    // deliberate widening of the contract, not a bug fix.
    @Test
    fun `GIVEN a mixed case referral preamble WHEN extracting THEN null is returned`() {
        assertNull(ReferralAttribution.referralCodeFrom("FxRefer$REFERRAL_CODE"))
    }

    @Test
    fun `GIVEN an ordinary utm_content WHEN extracting THEN null is returned`() {
        assertNull(ReferralAttribution.referralCodeFrom("CONTENT"))
        assertNull(ReferralAttribution.referralCodeFrom(""))
    }

    @Test
    fun `GIVEN an RTAMO utm_content WHEN extracting THEN null is returned`() {
        assertNull(ReferralAttribution.referralCodeFrom("rta%3ASOME_ADDON_GUID"))
    }

    @Test
    fun `GIVEN a referral referrer WHEN redacting THEN utm_content is removed and other params are preserved`() {
        assertEquals(
            "utm_source=SOURCE&utm_term=TERM",
            ReferralAttribution.redact(
                "utm_source=SOURCE&utm_content=$REFERRAL_UTM_CONTENT_PREFIX$REFERRAL_CODE&utm_term=TERM",
            ),
        )
    }

    @Test
    fun `GIVEN a referral-only referrer WHEN redacting THEN an empty response is returned`() {
        assertEquals(
            "",
            ReferralAttribution.redact("utm_content=$REFERRAL_UTM_CONTENT_PREFIX$REFERRAL_CODE"),
        )
    }

    @Test
    fun `GIVEN a referrer repeating utm_content WHEN redacting THEN every referral occurrence is removed`() {
        assertEquals(
            "utm_content=CONTENT",
            ReferralAttribution.redact(
                "utm_content=$REFERRAL_UTM_CONTENT_PREFIX$REFERRAL_CODE&utm_content=CONTENT",
            ),
        )
    }

    @Test
    fun `GIVEN a referrer with no referral code WHEN redacting THEN the response is returned unchanged`() {
        val referrer = "utm_source=SOURCE&utm_medium=MEDIUM&utm_content=CONTENT"

        assertEquals(referrer, ReferralAttribution.redact(referrer))
    }

    @Test
    fun `GIVEN a referral preamble in utm_campaign WHEN redacting THEN the response is returned unchanged`() {
        val referrer = "utm_campaign=$REFERRAL_UTM_CONTENT_PREFIX$REFERRAL_CODE&utm_source=SOURCE"

        assertEquals(referrer, ReferralAttribution.redact(referrer))
    }

    @Test
    fun `WHEN submitting a referral code THEN it is sent on the referrals ping and the profile is marked as submitted`() {
        val settings = Settings(testContext)
        val job = Pings.referrals.testBeforeNextSubmit {
            assertEquals(REFERRAL_CODE, Referrals.code.testGetValue())
        }

        ReferralAttribution.submit(REFERRAL_CODE, settings)

        job.join()
        assertTrue(settings.referralPingSubmitted)
    }

    @Test
    fun `GIVEN the ping was already submitted WHEN submitting THEN the code is never recorded`() {
        val settings = Settings(testContext)
        settings.referralPingSubmitted = true

        ReferralAttribution.submit(REFERRAL_CODE, settings)

        assertNull(Referrals.code.testGetValue())
    }
}
