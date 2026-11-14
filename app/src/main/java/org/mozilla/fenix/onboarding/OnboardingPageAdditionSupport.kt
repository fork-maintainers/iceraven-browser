/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding

import android.content.SharedPreferences
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlin.coroutines.CoroutineContext
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
import org.mozilla.fenix.settings.OnSharedPreferenceChangeListener
import org.mozilla.fenix.utils.Settings

/**
 * Adds an onboarding page if the boolean preference for [prefKey] becomes true.
 *
 * Required for onboarding pages (cards) whose eligibility depends on the
 * [org.mozilla.fenix.components.metrics.InstallReferrerHandlingService], which may resolve after the initial onboarding
 * pages have been determined. This races with onboarding completion: if onboarding completes before the referrer
 * resolves, the page will not be shown.
 *
 * @param prefKey the pref key identifier for the boolean pref that gates the page.
 * @param pagesToDisplay the mutable list of onboarding pages we display.
 * @param settings settings class that holds shared preferences.
 * @param page the page to add, or null if it's not eligible to be shown.
 * @param mainContext the coroutine context for UI.
 * @param ioContext the coroutine context for IO.
 * @param lifecycleOwner the lifecycle owner.
 */
class OnboardingPageAdditionSupport(
    private val prefKey: String,
    private val pagesToDisplay: MutableList<OnboardingPageUiData>,
    private val settings: Settings,
    private val page: OnboardingPageUiData?,
    private val mainContext: CoroutineContext = Dispatchers.Main,
    private val ioContext: CoroutineContext = Dispatchers.IO,
    private val lifecycleOwner: LifecycleOwner,
) : LifecycleAwareFeature {

    private var job: Job? = null

    override fun start() {
        job =
            lifecycleOwner.lifecycleScope.launch(ioContext) {
                settings.preferences
                    .flowScopedBooleanPreference(
                        lifecycleOwner,
                        mainContext,
                        prefKey,
                        false,
                    )
                    .distinctUntilChanged()
                    .collect { shouldShowPage ->
                        if (shouldShowPage) {
                            page?.let { pagesToDisplay.addPageIfAbsent(it) }
                        }
                    }
            }
    }

    override fun stop() {
        job?.cancel()
    }
}

internal fun MutableList<OnboardingPageUiData>.addPageIfAbsent(page: OnboardingPageUiData) {
    if (none { it.type == page.type }) {
        add(page)
    }
}

internal fun SharedPreferences.flowScopedBooleanPreference(
    owner: LifecycleOwner,
    mainContext: CoroutineContext,
    key: String,
    defValue: Boolean,
) = channelFlow {
    val listener =
        OnSharedPreferenceChangeListener(this@flowScopedBooleanPreference) { pref, updatedKey ->
            if (key == updatedKey) {
                // The framework snapshots its listener set when the write is committed, so this can run after the
                // flow was cancelled or closed. trySend fails silently there, where send would throw the
                // cancellation out into the framework's callback and crash the app.
                trySend(pref.getBoolean(key, defValue))
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
}
    .buffer(Channel.CONFLATED)
