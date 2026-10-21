/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:OptIn(ExperimentalAndroidComponentsApi::class)

package org.mozilla.fenix.components.menu

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mozilla.components.ExperimentalAndroidComponentsApi
import mozilla.components.concept.engine.ipprotection.ProxyState
import mozilla.components.concept.engine.ipprotection.ServiceState
import mozilla.components.feature.ipprotection.store.IPProtectionAction
import mozilla.components.feature.ipprotection.store.IPProtectionStore
import mozilla.components.feature.ipprotection.store.state.Authorized
import mozilla.components.feature.ipprotection.store.state.BYTES_PER_GB
import mozilla.components.feature.ipprotection.store.state.EligibilityStatus
import mozilla.components.feature.ipprotection.store.state.IPProtectionState
import mozilla.components.feature.ipprotection.store.state.IPProtectionStatus
import mozilla.components.feature.ipprotection.store.state.ProxyStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mozilla.fenix.components.menu.store.IPProtectionMenuState

class IPProtectionMenuBindingTest {
    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun `GIVEN IP protection has not reported its state WHEN binding starts THEN dispatch Uninitialized status`() =
        runTest {
            var result: IPProtectionMenuState? = null
            val ipProtectionStore = IPProtectionStore()

            startBinding(ipProtectionStore) { result = it }

            assertEquals(IPProtectionStatus.Uninitialized, result?.status)
        }

    @Test
    fun `WHEN proxy status changes THEN dispatch updated menu state`() = runTest {
        var result: IPProtectionMenuState? = null
        val ipProtectionStore = IPProtectionStore(initialState = readyState(Authorized.Idle))

        startBinding(ipProtectionStore) { result = it }

        assertEquals(IPProtectionStatus.Disabled, result?.status)

        ipProtectionStore.dispatch(IPProtectionAction.ProxyStateChanged(ProxyState.ACTIVE))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(IPProtectionStatus.Enabled, result?.status)
    }

    @Test
    fun `GIVEN dataMaxBytes is set WHEN binding starts THEN dispatch correct dataLimitGb`() = runTest {
        var result: IPProtectionMenuState? = null
        val ipProtectionStore =
            IPProtectionStore(
                initialState =
                    IPProtectionState(
                        proxyStatus = Authorized.Active,
                        maxDataBytes = (5 * BYTES_PER_GB).toLong(),
                    )
            )

        startBinding(ipProtectionStore) { result = it }

        assertEquals(5, result?.dataLimitGb)
    }

    @Test
    fun `GIVEN dataMaxBytes is unavailable WHEN binding starts THEN dispatch dataLimitGb as -1`() = runTest {
        var result: IPProtectionMenuState? = null
        val ipProtectionStore =
            IPProtectionStore(initialState = IPProtectionState(proxyStatus = Authorized.Active, maxDataBytes = -1L))

        startBinding(ipProtectionStore) { result = it }

        assertEquals(-1, result?.dataLimitGb)
    }

    private fun readyState(proxyStatus: ProxyStatus) =
        IPProtectionState(
            eligibilityStatus = EligibilityStatus.Eligible,
            serviceStatus = ServiceState.Ready,
            proxyStatus = proxyStatus,
        )

    private fun startBinding(
        ipProtectionStore: IPProtectionStore,
        onUpdate: (IPProtectionMenuState) -> Unit,
    ) {
        val binding =
            IPProtectionMenuBinding(
                ipProtectionStore = ipProtectionStore,
                onIPProtectionStatusUpdate = onUpdate,
                mainDispatcher = testDispatcher,
            )
        binding.start()
        testDispatcher.scheduler.advanceUntilIdle()
    }
}
