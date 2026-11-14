/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ipprotection.store

import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mozilla.components.browser.state.search.RegionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.ipprotection.storage.IPProtectionEligibilityStorage
import mozilla.components.feature.ipprotection.store.state.EligibilityStatus
import org.mozilla.fenix.nimbus.FxNimbus
import org.mozilla.fenix.settings.registerOnSharedPreferenceChangeListener

/**
 * Fenix's implementation of [IPProtectionEligibilityStorage].
 *
 * @param browserStore [BrowserStore] to access [RegionState].
 * @param sharedPref [SharedPreferences] to observe settings for changes.
 * @param prefKey key to observe [SharedPreferences] for.
 * @param lifecycleOwner A [LifecycleOwner] for observing [SharedPreferences].
 */
class FenixIPProtectionEligibilityStorage(
    private val browserStore: BrowserStore,
    private val sharedPref: SharedPreferences,
    private val prefKey: String,
    private val lifecycleOwner: LifecycleOwner,
) : IPProtectionEligibilityStorage {

    private val secretEnabled = MutableStateFlow(sharedPref.getBoolean(prefKey, false))

    override val eligibilityStatus: Flow<EligibilityStatus> =
        combine(
                browserStore.stateFlow.map { it.search.region },
                secretEnabled,
            ) { region, secretOverride ->
                val nimbus = FxNimbus.features.ipProtection.value()
                val status =
                    when {
                        secretOverride -> EligibilityStatus.Eligible
                        !nimbus.enabled -> EligibilityStatus.Ineligible
                        region == null -> EligibilityStatus.Unknown
                        region.home in nimbus.allowedRegions -> EligibilityStatus.Eligible
                        else -> EligibilityStatus.UnsupportedRegion
                    }
                status
            }
            .distinctUntilChanged()

    @VisibleForTesting
    internal fun onPreferenceChange(
        sharedPreferences: SharedPreferences,
        key: String?,
    ) {
        if (key == prefKey) {
            secretEnabled.value = sharedPreferences.getBoolean(prefKey, false)
        }
    }

    override fun init() {
        sharedPref.registerOnSharedPreferenceChangeListener(owner = lifecycleOwner) { sharedPreferences, key ->
            onPreferenceChange(sharedPreferences, key)
        }
    }
}
