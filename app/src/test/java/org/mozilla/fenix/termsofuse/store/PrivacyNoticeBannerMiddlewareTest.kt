/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.termsofuse.store

import io.mockk.mockk
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import mozilla.components.lib.state.Store
import org.junit.Before
import org.junit.Test

class PrivacyNoticeBannerMiddlewareTest {

    private lateinit var repository: FakePrivacyNoticeBannerRepository

    private val store =
        mockk<Store<PrivacyNoticeBannerState, PrivacyNoticeBannerAction>>(
            relaxed = true,
        )

    private lateinit var middleware: PrivacyNoticeBannerMiddleware

    @Before
    fun setup() {
        repository = FakePrivacyNoticeBannerRepository()
        middleware = PrivacyNoticeBannerMiddleware(repository)
    }

    @Test
    fun `WHEN the action OnBannerDisplayed is received THEN we update the privacy notice banner preference`() {
        middleware.invoke(
            store = store,
            next = {},
            action = PrivacyNoticeBannerAction.OnBannerDisplayed,
        )

        assertTrue(repository.updatePrivacyNoticeBannerDisplayedPreferenceCalled)
    }

    @Test
    fun `WHEN a no-op action is received THEN we do not update the privacy notice banner preference`() {
        middleware.invoke(
            store = store,
            next = {},
            action = PrivacyNoticeBannerAction.OnPrivacyNoticeClicked,
        )

        middleware.invoke(
            store = store,
            next = {},
            action = PrivacyNoticeBannerAction.OnLearnMoreClicked,
        )

        middleware.invoke(
            store = store,
            next = {},
            action = PrivacyNoticeBannerAction.OnCloseClicked,
        )

        middleware.invoke(
            store = store,
            next = {},
            action = PrivacyNoticeBannerAction.OnNavigatedAwayFromHome,
        )

        assertFalse(repository.updatePrivacyNoticeBannerDisplayedPreferenceCalled)
    }

    class FakePrivacyNoticeBannerRepository : PrivacyNoticeBannerRepository {
        var updatePrivacyNoticeBannerDisplayedPreferenceCalled = false

        override fun updatePrivacyNoticeBannerDisplayedPreference() {
            updatePrivacyNoticeBannerDisplayedPreferenceCalled = true
        }

        override fun shouldShowPrivacyNoticeBanner(): Boolean = true
    }
}
