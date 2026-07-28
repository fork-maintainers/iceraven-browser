/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.share

import android.os.Bundle
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.concept.sync.TabPrivacy
import mozilla.components.service.fxa.manager.FxaAccountManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SendToDevicesDialogFragmentTest {

    private val mockAccountManager = mockk<FxaAccountManager>(relaxed = true)
    private lateinit var fragment: SendToDevicesDialogFragment

    @Before
    fun setUp() {
        fragment = spyk(SendToDevicesDialogFragment.newInstance("https://example.com", "Title", false))
        every { fragment.navigateToSignIn() } just runs
        every { fragment.onAuthenticated() } just runs
    }

    // region loadTabData

    @Test
    fun `GIVEN bundle with PRIVATE privacy WHEN loadTabData is called THEN tabPrivacy is Private`() {
        val bundle = Bundle().apply { putString("privacy", "PRIVATE") }

        fragment.loadTabData(bundle)

        assertEquals(TabPrivacy.Private, fragment.tabPrivacyForTest)
    }

    @Test
    fun `GIVEN bundle without privacy extra WHEN loadTabData is called THEN tabPrivacy defaults to Normal`() {
        val bundle = Bundle().apply { putString("url", "https://example.com") }

        fragment.loadTabData(bundle)

        assertEquals(TabPrivacy.Normal, fragment.tabPrivacyForTest)
    }

    @Test
    fun `GIVEN bundle with url and title WHEN loadTabData is called THEN tabUrl and tabTitle are updated`() {
        val bundle = Bundle().apply {
            putString("url", "https://mozilla.org")
            putString("title", "Mozilla")
        }

        fragment.loadTabData(bundle)

        assertEquals("https://mozilla.org", fragment.tabUrlForTest)
        assertEquals("Mozilla", fragment.tabTitleForTest)
    }

    @Test
    fun `GIVEN null bundle WHEN loadTabData is called THEN fields are null and privacy defaults to Normal`() {
        fragment.loadTabData(null)

        assertNull(fragment.tabUrlForTest)
        assertNull(fragment.tabTitleForTest)
        assertEquals(TabPrivacy.Normal, fragment.tabPrivacyForTest)
    }

    // endregion

    // region checkAuthAndNavigate

    @Test
    fun `GIVEN unauthenticated account WHEN checkAuthAndNavigate is called THEN navigateToSignIn is called`() {
        every { mockAccountManager.authenticatedAccount() } returns null

        fragment.checkAuthAndNavigate(mockAccountManager)

        verify { fragment.navigateToSignIn() }
    }

    @Test
    fun `GIVEN authenticated account WHEN checkAuthAndNavigate is called THEN navigateToSignIn is not called`() {
        every { mockAccountManager.authenticatedAccount() } returns mockk<OAuthAccount>()

        fragment.checkAuthAndNavigate(mockAccountManager)

        verify(exactly = 0) { fragment.navigateToSignIn() }
    }

    @Test
    fun `GIVEN unauthenticated account WHEN checkAuthAndNavigate is called twice THEN navigateToSignIn is called only once`() {
        every { mockAccountManager.authenticatedAccount() } returns null

        fragment.checkAuthAndNavigate(mockAccountManager)
        fragment.checkAuthAndNavigate(mockAccountManager)

        verify(exactly = 1) { fragment.navigateToSignIn() }
    }

    // endregion
}

private val SendToDevicesDialogFragment.tabPrivacyForTest: TabPrivacy
    get() = javaClass.getDeclaredField("tabPrivacy").apply { isAccessible = true }.get(this) as TabPrivacy

private val SendToDevicesDialogFragment.tabUrlForTest: String?
    get() = javaClass.getDeclaredField("tabUrl").apply { isAccessible = true }.get(this) as String?

private val SendToDevicesDialogFragment.tabTitleForTest: String?
    get() = javaClass.getDeclaredField("tabTitle").apply { isAccessible = true }.get(this) as String?
