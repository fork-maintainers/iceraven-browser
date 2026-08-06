/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:OptIn(ExperimentalAndroidComponentsApi::class)

package org.mozilla.fenix.components.ipprotection

import mozilla.components.ExperimentalAndroidComponentsApi
import mozilla.components.concept.engine.ipprotection.ServiceState
import mozilla.components.feature.ipprotection.store.IPProtectionAction
import mozilla.components.feature.ipprotection.store.IPProtectionStore
import mozilla.components.feature.ipprotection.store.state.AccountState
import mozilla.components.feature.ipprotection.store.state.AccountStatus
import mozilla.components.feature.ipprotection.store.state.IPProtectionState
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.GleanMetrics.Vpn
import org.mozilla.fenix.helpers.FenixGleanTestRule
import org.mozilla.geckoview.IPProtectionController.IPProxyException
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class IPProtectionTelemetryMiddlewareTest {

    @get:Rule
    val gleanTestRule = FenixGleanTestRule(testContext)

    private var now = 0L
    private val middleware = IPProtectionTelemetryMiddleware(currentTimeInMillis = { now })

    @Test
    fun `GIVEN the authentication flow started WHEN it completes successfully THEN fxAccountFlowCompleted is recorded with the duration`() {
        assertNull(Vpn.fxAccountFlowCompleted.testGetValue())

        val store = createStore(initialStatus = AccountStatus.NeedsAuthentication)

        now = 1_000L
        store.transitionTo(AccountStatus.RequestingAuthentication)
        store.transitionTo(AccountStatus.AwaitingAuthentication)
        now = 3_500L
        store.transitionTo(AccountStatus.Authenticated)

        val events = Vpn.fxAccountFlowCompleted.testGetValue()
        assertNotNull(events)
        assertEquals(1, events.size)
        assertEquals("2500", events.single().extra?.get("duration_ms"))
        assertNull(Vpn.fxAuthorizationFlowCompleted.testGetValue())
    }

    @Test
    fun `GIVEN the authorization flow started WHEN it completes successfully THEN fxAuthorizationFlowCompleted is recorded with the duration`() {
        assertNull(Vpn.fxAuthorizationFlowCompleted.testGetValue())

        val store = createStore(initialStatus = AccountStatus.NeedsAuthorization)

        now = 1_000L
        store.transitionTo(AccountStatus.RequestingAuthorization)
        store.transitionTo(AccountStatus.AwaitingAuthorization)
        now = 2_000L
        store.transitionTo(AccountStatus.Authenticated)

        val events = Vpn.fxAuthorizationFlowCompleted.testGetValue()
        assertNotNull(events)
        assertEquals(1, events.size)
        assertEquals("1000", events.single().extra?.get("duration_ms"))
        assertNull(Vpn.fxAccountFlowCompleted.testGetValue())
    }

    @Test
    fun `GIVEN an auth flow WHEN it is cancelled THEN no telemetry is recorded`() {
        val store = createStore(initialStatus = AccountStatus.AwaitingAuthentication)

        store.transitionTo(AccountStatus.NeedsAuthentication)

        assertNull(Vpn.fxAccountFlowCompleted.testGetValue())
        assertNull(Vpn.fxAuthorizationFlowCompleted.testGetValue())
    }

    @Test
    fun `GIVEN the account status does not change THEN no telemetry is recorded`() {
        val store = createStore(initialStatus = AccountStatus.AwaitingAuthentication)

        store.transitionTo(AccountStatus.AwaitingAuthentication)

        assertNull(Vpn.fxAccountFlowCompleted.testGetValue())
        assertNull(Vpn.fxAuthorizationFlowCompleted.testGetValue())
    }

    @Test
    fun `GIVEN the VPN toggle failed with no or a non-proxy error THEN no error code is recorded`() {
        assertNull(Vpn.errorEncountered.testGetValue())

        val store = createStore(initialStatus = AccountStatus.EnrolledAndEntitled)

        store.dispatch(IPProtectionAction.ToggleFailed())
        store.dispatch(IPProtectionAction.ToggleFailed(RuntimeException("boom")))

        val events = Vpn.errorEncountered.testGetValue()
        assertNotNull(events)
        assertEquals(2, events.size)
        assertTrue(events.all { it.extra?.get("error_code") == "null" })
    }

    @Test
    fun `GIVEN user has already finished auth flow successfully but service is still unauthenticated WHEN the VPN toggle failed THEN a generic network error telemetry is recorded`() {
        assertNull(Vpn.entitledAccountUnauthenticated.testGetValue())

        val store = createStore(
            initialStatus = AccountStatus.EnrolledAndEntitled,
            serviceStatus = ServiceState.Unauthenticated,
        )

        store.dispatch(IPProtectionAction.ToggleFailed())

        val events = Vpn.entitledAccountUnauthenticated.testGetValue()
        assertNotNull(events)
        assertEquals(1, events.size)
    }

    private fun createStore(
        initialStatus: AccountStatus,
        serviceStatus: ServiceState = ServiceState.Uninitialized,
    ) = IPProtectionStore(
        initialState = IPProtectionState(
            accountState = AccountState(status = initialStatus),
            serviceStatus = serviceStatus,
        ),
        middleware = listOf(middleware),
    )

    private fun IPProtectionStore.transitionTo(status: AccountStatus) {
        dispatch(IPProtectionAction.AccountStateChanged(status))
    }
}
