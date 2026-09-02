/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.ipprotection

import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mozilla.components.browser.state.action.SearchAction
import mozilla.components.browser.state.search.RegionState
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.SearchState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.ipprotection.store.state.EligibilityStatus
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.nimbus.FxNimbus
import org.mozilla.fenix.nimbus.IpProtection

@RunWith(AndroidJUnit4::class)
class FenixIPProtectionEligibilityStorageTest {
    private val prefKey = "test_ip_protection_enabled"

    private lateinit var sharedPreferences: SharedPreferences

    @Before
    fun setup() {
        sharedPreferences = testContext.getSharedPreferences("test", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
    }

    @Test
    fun `WHEN nimbus disabled and secret settings disabled THEN status is Ineligible`() = runTest {
        FxNimbus.features.ipProtection.withCachedValue(IpProtection(enabled = false))
        val browserStore = BrowserStore(
            initialState = BrowserState(
                search = SearchState(region = RegionState("CA", "CA")),
            ),
        )

        val storage = FenixIPProtectionEligibilityStorage(
            browserStore = browserStore,
            sharedPref = sharedPreferences,
            prefKey = prefKey,
            lifecycleOwner = mockk(relaxed = true),
        )

        assertEquals(EligibilityStatus.Ineligible, storage.eligibilityStatus.first())
    }

    @Test
    fun `WHEN nimbus disabled but region in allowed list THEN status is Ineligible`() = runTest {
        FxNimbus.features.ipProtection.withCachedValue(
            IpProtection(enabled = false, allowedRegions = listOf("US", "CA")),
        )
        val browserStore = BrowserStore(
            initialState = BrowserState(
                search = SearchState(region = RegionState("US", "US")),
            ),
        )

        val storage = FenixIPProtectionEligibilityStorage(
            browserStore = browserStore,
            sharedPref = sharedPreferences,
            prefKey = prefKey,
            lifecycleOwner = mockk(relaxed = true),
        )

        assertEquals(EligibilityStatus.Ineligible, storage.eligibilityStatus.first())
    }

    @Test
    fun `WHEN nimbus enabled and region in allowed list THEN status is Eligible`() = runTest {
        FxNimbus.features.ipProtection.withCachedValue(
            IpProtection(enabled = true, allowedRegions = listOf("US", "CA")),
        )
        val browserStore = BrowserStore(
            initialState = BrowserState(
                search = SearchState(region = RegionState("US", "US")),
            ),
        )

        val storage = FenixIPProtectionEligibilityStorage(
            browserStore = browserStore,
            sharedPref = sharedPreferences,
            prefKey = prefKey,
            lifecycleOwner = mockk(relaxed = true),
        )

        assertEquals(EligibilityStatus.Eligible, storage.eligibilityStatus.first())
    }

    @Test
    fun `WHEN nimbus enabled and region not in allowed list THEN status is UnsupportedRegion`() = runTest {
        FxNimbus.features.ipProtection.withCachedValue(
            IpProtection(enabled = true, allowedRegions = listOf("US", "CA")),
        )
        val browserStore = BrowserStore(
            initialState = BrowserState(
                search = SearchState(region = RegionState("JP", "JP")),
            ),
        )

        val storage = FenixIPProtectionEligibilityStorage(
            browserStore = browserStore,
            sharedPref = sharedPreferences,
            prefKey = prefKey,
            lifecycleOwner = mockk(relaxed = true),
        )

        assertEquals(EligibilityStatus.UnsupportedRegion, storage.eligibilityStatus.first())
    }

    @Test
    fun `WHEN secret toggle is enabled THEN status is Eligible regardless of nimbus`() = runTest {
        FxNimbus.features.ipProtection.withCachedValue(IpProtection(enabled = false))
        sharedPreferences.edit().putBoolean(prefKey, true).apply()

        val browserStore = BrowserStore(
            initialState = BrowserState(
                search = SearchState(region = RegionState("JP", "JP")),
            ),
        )

        val storage = FenixIPProtectionEligibilityStorage(
            browserStore = browserStore,
            sharedPref = sharedPreferences,
            prefKey = prefKey,
            lifecycleOwner = mockk(relaxed = true),
        )

        assertEquals(EligibilityStatus.Eligible, storage.eligibilityStatus.first())
    }

    @Test
    fun `WHEN secret preference changes to enabled THEN status is updated to Eligible`() = runTest {
        FxNimbus.features.ipProtection.withCachedValue(IpProtection(enabled = false))
        val browserStore = BrowserStore(
            initialState = BrowserState(
                search = SearchState(region = RegionState("JP", "JP")),
            ),
        )

        val storage = FenixIPProtectionEligibilityStorage(
            browserStore = browserStore,
            sharedPref = sharedPreferences,
            prefKey = prefKey,
            lifecycleOwner = mockk(relaxed = true),
        )

        assertEquals(EligibilityStatus.Ineligible, storage.eligibilityStatus.first())

        sharedPreferences.edit().putBoolean(prefKey, true).apply()
        storage.onPreferenceChange(sharedPreferences, prefKey)

        assertEquals(EligibilityStatus.Eligible, storage.eligibilityStatus.first())
    }

    @Test
    fun `WHEN secret preference changes to disabled THEN status reverts`() = runTest {
        FxNimbus.features.ipProtection.withCachedValue(IpProtection(enabled = false))
        sharedPreferences.edit().putBoolean(prefKey, true).apply()

        val browserStore = BrowserStore(
            initialState = BrowserState(
                search = SearchState(region = RegionState("JP", "JP")),
            ),
        )

        val storage = FenixIPProtectionEligibilityStorage(
            browserStore = browserStore,
            sharedPref = sharedPreferences,
            prefKey = prefKey,
            lifecycleOwner = mockk(relaxed = true),
        )

        assertEquals(EligibilityStatus.Eligible, storage.eligibilityStatus.first())

        sharedPreferences.edit().putBoolean(prefKey, false).apply()
        storage.onPreferenceChange(sharedPreferences, prefKey)

        assertEquals(EligibilityStatus.Ineligible, storage.eligibilityStatus.first())
    }

    @Test
    fun `WHEN onPreferenceChange is called with a different key THEN status is not updated`() = runTest {
        FxNimbus.features.ipProtection.withCachedValue(IpProtection(enabled = false))
        val browserStore = BrowserStore(
            initialState = BrowserState(
                search = SearchState(region = RegionState("JP", "JP")),
            ),
        )

        val storage = FenixIPProtectionEligibilityStorage(
            browserStore = browserStore,
            sharedPref = sharedPreferences,
            prefKey = prefKey,
            lifecycleOwner = mockk(relaxed = true),
        )

        assertEquals(EligibilityStatus.Ineligible, storage.eligibilityStatus.first())

        sharedPreferences.edit().putBoolean("some_other_key", true).apply()
        storage.onPreferenceChange(sharedPreferences, "some_other_key")

        assertEquals(EligibilityStatus.Ineligible, storage.eligibilityStatus.first())
    }

    @Test
    fun `WHEN region changes to an allowed region THEN status is updated to Eligible`() = runTest {
        FxNimbus.features.ipProtection.withCachedValue(
            IpProtection(enabled = true, allowedRegions = listOf("US")),
        )
        val browserStore = BrowserStore(
            initialState = BrowserState(
                search = SearchState(region = RegionState("JP", "JP")),
            ),
        )

        val storage = FenixIPProtectionEligibilityStorage(
            browserStore = browserStore,
            sharedPref = sharedPreferences,
            prefKey = prefKey,
            lifecycleOwner = mockk(relaxed = true),
        )

        assertEquals(EligibilityStatus.UnsupportedRegion, storage.eligibilityStatus.first())

        browserStore.dispatch(
            SearchAction.SetRegionAction(RegionState("US", "US")),
        )

        assertEquals(EligibilityStatus.Eligible, storage.eligibilityStatus.first())
    }

    @Test
    fun `GIVEN null region WHEN nimbus is enabled THEN status is UnsupportedRegion`() = runTest {
        FxNimbus.features.ipProtection.withCachedValue(
            IpProtection(enabled = true, allowedRegions = listOf("US")),
        )
        val browserStore = BrowserStore()

        val storage = FenixIPProtectionEligibilityStorage(
            browserStore = browserStore,
            sharedPref = sharedPreferences,
            prefKey = prefKey,
            lifecycleOwner = mockk(relaxed = true),
        )

        assertEquals(EligibilityStatus.UnsupportedRegion, storage.eligibilityStatus.first())
    }
}
