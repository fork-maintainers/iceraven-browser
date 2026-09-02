/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding

import android.content.SharedPreferences
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.support.base.feature.LifecycleAwareFeature
import org.mozilla.fenix.onboarding.view.OnboardingPageUiData
import org.mozilla.fenix.perf.runBlockingIncrement
import org.mozilla.fenix.settings.OnSharedPreferenceChangeListener
import org.mozilla.fenix.utils.Settings
import kotlin.coroutines.CoroutineContext

/**
 * Handles adding the marketing page to onboarding if certain conditions are met.
 *
 * @param prefKey the pref key identifier for the "should show marketing page" pref
 * @param pagesToDisplay the mutable list of onboarding pages we display
 * @param marketingPage the marketing page to add, or null if it's not eligible to be shown
 * @param settings settings class that holds shared preferences
 * @param mainContext the coroutine context for UI
 * @param ioContext the coroutine context for IO
 * @param lifecycleOwner the lifecycle owner
 */
class MarketingPageAdditionSupport(
    private val prefKey: String,
    private val pagesToDisplay: MutableList<OnboardingPageUiData>,
    private val marketingPage: OnboardingPageUiData?,
    private val settings: Settings,
    private val mainContext: CoroutineContext = Dispatchers.Main,
    private val ioContext: CoroutineContext = Dispatchers.IO,
    private val lifecycleOwner: LifecycleOwner,
) : LifecycleAwareFeature {

    private var job: Job? = null

    override fun start() {
        job = lifecycleOwner.lifecycleScope.launch(ioContext) {
            settings.preferences.flowScopedBooleanPreference(
                lifecycleOwner,
                mainContext,
                prefKey,
                settings.shouldShowMarketingOnboarding,
            )
                .distinctUntilChanged()
                .collect { shouldShowMarketingOnboarding ->
                    if (shouldShowMarketingOnboarding) {
                        marketingPage?.let {
                            pagesToDisplay.addMarketingPageIfAbsent(it)
                        }
                    }
                }
        }
    }

    override fun stop() {
        job?.cancel()
    }
}

internal fun MutableList<OnboardingPageUiData>.addMarketingPageIfAbsent(marketingPage: OnboardingPageUiData) {
    if (none { it.type == OnboardingPageUiData.Type.MARKETING_DATA }) {
        add(marketingPage)
    }
}

internal fun SharedPreferences.flowScopedBooleanPreference(
    owner: LifecycleOwner,
    mainContext: CoroutineContext,
    key: String,
    defValue: Boolean,
) = channelFlow {
    val listener = OnSharedPreferenceChangeListener(
        this@flowScopedBooleanPreference,
    ) { pref, updatedKey ->
        if (key == updatedKey) {
            val result = pref.getBoolean(key, defValue)
            runBlockingIncrement {
                send(result)
            }
            this@channelFlow.close()
        }
    }

    withContext(mainContext) {
        owner.lifecycle.addObserver(listener)
    }

    val initValue = getBoolean(key, defValue)
    send(initValue)

    awaitClose {
        // On the off-chance that we close unexpectedly, let's clean up.
        unregisterOnSharedPreferenceChangeListener(listener)
    }
}.buffer(Channel.CONFLATED)
