/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ipprotection.store

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mozilla.components.feature.ipprotection.store.IPProtectionStore
import mozilla.components.feature.ipprotection.store.state.EligibilityStatus
import mozilla.components.feature.ipprotection.store.state.IPProtectionState
import mozilla.components.lib.state.helpers.AbstractBinding
import mozilla.components.support.utils.DateTimeProvider

/**
 * Triggers the IP protection onboarding bottom sheet when the user first becomes
 * eligible and meets our required heuristic provided by [IPProtectionPromptRepository].
 *
 * @param repository Source of truth for whether the onboarding prompt is still allowed to appear
 * (feature flag, install age, prior dismissals, prior VPN usage).
 * @param onShowOnboarding Callback invoked when the prompt should be presented to the user.
 * @param timeProvider Supplies the current time.
 * @param mainDispatcher [CoroutineDispatcher] on which [onShowOnboarding] is invoked.
 * @param store the singleton instance of [IPProtectionStore].
 */
class IPProtectionOnboardingPrompt(
    private val repository: IPProtectionPromptRepository,
    private val onShowOnboarding: () -> Unit,
    private val timeProvider: DateTimeProvider,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    store: IPProtectionStore,
) : AbstractBinding<IPProtectionState>(store, mainDispatcher) {
    override suspend fun onState(flow: Flow<IPProtectionState>) {
        flow.map { it.eligibilityStatus }
            .distinctUntilChanged()
            .collect { eligibilityStatus ->
                if (eligibilityStatus != EligibilityStatus.Eligible) {
                    return@collect
                }

                if (repository.canShowIPProtectionPrompt(timeProvider.currentTimeMillis())) {
                    onShowOnboarding()
                }
            }
    }
}
