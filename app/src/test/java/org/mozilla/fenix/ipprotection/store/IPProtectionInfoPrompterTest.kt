/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:OptIn(ExperimentalAndroidComponentsApi::class)

package org.mozilla.fenix.ipprotection.store

import kotlin.test.assertIs
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mozilla.components.ExperimentalAndroidComponentsApi
import mozilla.components.feature.ipprotection.store.IPProtectionAction
import mozilla.components.feature.ipprotection.store.IPProtectionStore
import mozilla.components.feature.ipprotection.store.state.Authorized
import mozilla.components.feature.ipprotection.store.state.EligibilityStatus
import mozilla.components.feature.ipprotection.store.state.IPProtectionState
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.snackbar.SnackbarState

class IPProtectionInfoPrompterTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var appStore: AppStore
    private val errorMessages = ErrorMessages(dataLimitReached = "Data limit reached")

    @Before
    fun setup() {
        appStore = AppStore()
    }

    @Test
    fun `GIVEN eligible user with DataLimitReached proxy WHEN eligibility updates THEN shows data limit reached snackbar`() =
        runTest(testDispatcher) {
            val ipProtectionStore =
                IPProtectionStore(initialState = IPProtectionState(proxyStatus = Authorized.DataLimitReached))
            val prompter = IPProtectionInfoPrompter(ipProtectionStore, appStore, errorMessages, testDispatcher)

            prompter.start()
            testDispatcher.scheduler.advanceUntilIdle()

            ipProtectionStore.dispatch(IPProtectionAction.EligibilityChanged(EligibilityStatus.Eligible))
            testDispatcher.scheduler.advanceUntilIdle()

            val snackbarState = appStore.state.snackbarState
            assertIs<SnackbarState.IPProtectionShowActionSettingsSnackbar>(snackbarState)
            assertEquals(errorMessages.dataLimitReached, snackbarState.title)
        }

    @Test
    fun `GIVEN eligible user with Active proxy WHEN eligibility updates THEN no snackbar shown`() =
        runTest(testDispatcher) {
            val ipProtectionStore = IPProtectionStore(initialState = IPProtectionState(proxyStatus = Authorized.Active))
            val prompter = IPProtectionInfoPrompter(ipProtectionStore, appStore, errorMessages, testDispatcher)

            prompter.start()
            testDispatcher.scheduler.advanceUntilIdle()

            ipProtectionStore.dispatch(IPProtectionAction.EligibilityChanged(EligibilityStatus.Eligible))
            testDispatcher.scheduler.advanceUntilIdle()

            assertIs<SnackbarState.None>(appStore.state.snackbarState)
        }

    @Test
    fun `GIVEN ineligible user with ConnectionError proxy WHEN eligibility updates THEN no snackbar shown`() =
        runTest(testDispatcher) {
            val ipProtectionStore =
                IPProtectionStore(initialState = IPProtectionState(proxyStatus = Authorized.ConnectionError))
            val prompter = IPProtectionInfoPrompter(ipProtectionStore, appStore, errorMessages, testDispatcher)

            prompter.start()
            testDispatcher.scheduler.advanceUntilIdle()

            ipProtectionStore.dispatch(IPProtectionAction.EligibilityChanged(EligibilityStatus.Ineligible))
            testDispatcher.scheduler.advanceUntilIdle()

            assertIs<SnackbarState.None>(appStore.state.snackbarState)
        }
}
