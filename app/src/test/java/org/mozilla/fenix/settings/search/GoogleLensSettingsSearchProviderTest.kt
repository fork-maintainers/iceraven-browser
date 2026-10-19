/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.search

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import mozilla.components.support.test.robolectric.testContext
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.settings.settingssearch.PreferenceFileInformation

@RunWith(AndroidJUnit4::class)
class GoogleLensSettingsSearchProviderTest {

    private val googleLensPreferenceKey
        get() = testContext.getString(R.string.pref_key_google_lens_integration_user_enabled)

    @Test
    fun `GIVEN the Google Lens integration is disabled WHEN getting search items THEN no items are returned`() {
        every { testContext.components.settings.googleLensIntegrationEnabled } returns false

        val items = GoogleLensSettingsSearchProvider.getSearchItems(testContext)

        assertTrue(actual = items.isEmpty(), message = "Expected no items when the Google Lens integration is disabled")
    }

    @Test
    fun `GIVEN the Google Lens integration is enabled WHEN getting search items THEN the Google Lens toggle of the search settings is returned`() {
        every { testContext.components.settings.googleLensIntegrationEnabled } returns true

        val items = GoogleLensSettingsSearchProvider.getSearchItems(testContext)

        assertEquals(
            expected = listOf(googleLensPreferenceKey to PreferenceFileInformation.SearchSettingsPreferences),
            actual = items.map { it.preferenceKey to it.preferenceFileInformation },
            message = "Expected a single item pointing at the Google Lens toggle of the search settings",
        )
    }
}
