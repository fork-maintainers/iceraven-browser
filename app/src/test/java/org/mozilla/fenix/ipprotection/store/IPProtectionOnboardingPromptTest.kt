/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:OptIn(ExperimentalAndroidComponentsApi::class)

package org.mozilla.fenix.ipprotection.store

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mozilla.components.ExperimentalAndroidComponentsApi
import mozilla.components.feature.ipprotection.store.IPProtectionAction
import mozilla.components.feature.ipprotection.store.IPProtectionStore
import mozilla.components.feature.ipprotection.store.state.EligibilityStatus
import mozilla.components.support.utils.FakeDateTimeProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mozilla.fenix.ipprotection.FakeIPProtectionPromptRepository

class IPProtectionOnboardingPromptTest {
    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun `GIVEN repository allows the prompt WHEN eligibility becomes Eligible THEN onShowOnboarding is invoked`() =
        runTest(testDispatcher) {
            val repository = FakeIPProtectionPromptRepository(canShowIPProtectionPrompt = true)
            var shownCount = 0
            val store = IPProtectionStore()

            startBinding(repository, store) { shownCount++ }

            store.dispatch(IPProtectionAction.EligibilityChanged(EligibilityStatus.Eligible))
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, shownCount)
        }

    @Test
    fun `GIVEN repository does not allow the prompt WHEN eligibility becomes Eligible THEN onShowOnboarding is not invoked`() =
        runTest(testDispatcher) {
            val repository = FakeIPProtectionPromptRepository(canShowIPProtectionPrompt = false)
            var shownCount = 0
            val store = IPProtectionStore()

            startBinding(repository, store) { shownCount++ }

            store.dispatch(IPProtectionAction.EligibilityChanged(EligibilityStatus.Eligible))
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(0, shownCount)
        }

    @Test
    fun `WHEN eligibility is not Eligible THEN onShowOnboarding is not invoked`() =
        runTest(testDispatcher) {
            val repository = FakeIPProtectionPromptRepository(canShowIPProtectionPrompt = true)
            var shownCount = 0
            val store = IPProtectionStore()

            startBinding(repository, store) { shownCount++ }

            store.dispatch(IPProtectionAction.EligibilityChanged(EligibilityStatus.Ineligible))
            store.dispatch(IPProtectionAction.EligibilityChanged(EligibilityStatus.UnsupportedRegion))
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(0, shownCount)
        }

    @Test
    fun `GIVEN showOnboardingBottomSheet is true WHEN eligibility becomes Eligible THEN onShowOnboarding is invoked`() =
        runTest(testDispatcher) {
            val repository = FakeIPProtectionPromptRepository(
                canShowIPProtectionPrompt = true,
                showOnboardingBottomSheet = true,
            )
            var shownCount = 0
            val store = IPProtectionStore()

            startBinding(repository, store) { shownCount++ }

            store.dispatch(IPProtectionAction.EligibilityChanged(EligibilityStatus.Eligible))
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, shownCount)
        }

    @Test
    fun `GIVEN showOnboardingBottomSheet is false WHEN eligibility becomes Eligible THEN onShowOnboarding is not invoked`() =
        runTest(testDispatcher) {
            val repository = FakeIPProtectionPromptRepository(
                canShowIPProtectionPrompt = false,
                showOnboardingBottomSheet = false,
            )
            var shownCount = 0
            val store = IPProtectionStore()

            startBinding(repository, store) { shownCount++ }

            store.dispatch(IPProtectionAction.EligibilityChanged(EligibilityStatus.Eligible))
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(0, shownCount)
        }

    private fun startBinding(
        repository: FakeIPProtectionPromptRepository,
        store: IPProtectionStore,
        onShowOnboarding: () -> Unit,
    ) {
        IPProtectionOnboardingPrompt(
            repository = repository,
            onShowOnboarding = onShowOnboarding,
            timeProvider = FakeDateTimeProvider(),
            mainDispatcher = testDispatcher,
            store = store,
        ).start()
        testDispatcher.scheduler.advanceUntilIdle()
    }
}
