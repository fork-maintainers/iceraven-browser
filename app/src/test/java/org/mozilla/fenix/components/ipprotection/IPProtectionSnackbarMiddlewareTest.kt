/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:OptIn(ExperimentalAndroidComponentsApi::class)

package org.mozilla.fenix.components.ipprotection

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mozilla.components.ExperimentalAndroidComponentsApi
import mozilla.components.feature.ipprotection.store.IPProtectionAction
import mozilla.components.feature.ipprotection.store.IPProtectionStore
import mozilla.components.feature.ipprotection.store.state.EligibilityStatus
import mozilla.components.support.test.middleware.CaptureActionsMiddleware
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.appstate.AppState

class IPProtectionSnackbarMiddlewareTest {
    private val connectionError = "Couldn't connect to VPN."
    private lateinit var captureMiddleware: CaptureActionsMiddleware<AppState, AppAction>
    private lateinit var appStore: AppStore
    private lateinit var ipProtectionStore: IPProtectionStore

    @Before
    fun setup() {
        captureMiddleware = CaptureActionsMiddleware()
        appStore = AppStore(middlewares = listOf(captureMiddleware))
        ipProtectionStore = IPProtectionStore(
            middleware = listOf(
                IPProtectionSnackbarMiddleware(
                    lazyAppStore = lazy { appStore },
                    messages = IPProtectionSnackbarMessages(connectionError = connectionError),
                ),
            ),
        )
    }

    @Test
    fun `WHEN ActivationFailed is dispatched THEN ConnectionError snackbar action is dispatched`() {
        ipProtectionStore.dispatch(IPProtectionAction.ToggleFailed)

        captureMiddleware.assertLastAction(AppAction.IPProtectionSnackbarAction.ConnectionError::class) { action ->
            assertEquals(connectionError, action.title)
        }
    }

    @Test
    fun `WHEN an unrelated action is dispatched THEN no snackbar action is dispatched`() {
        ipProtectionStore.dispatch(
            IPProtectionAction.EligibilityChanged(EligibilityStatus.Eligible),
        )

        captureMiddleware.assertNotDispatched(AppAction.IPProtectionSnackbarAction.ConnectionError::class)
    }

    @Test
    fun `GIVEN ActivationFailed was dispatched WHEN a new IPProtectionInfoPrompter attaches THEN no second snackbar action is dispatched`() =
        runTest(StandardTestDispatcher()) {
            // Simulates HomeActivity recreation: the same process-scoped IPProtectionStore now has
            // a new observer (a freshly-instantiated IPProtectionInfoPrompter). With the snackbar
            // owned by middleware, the new observer does not re-fire on already-set state.
            ipProtectionStore.dispatch(IPProtectionAction.ToggleFailed)
            captureMiddleware.assertLastAction(AppAction.IPProtectionSnackbarAction.ConnectionError::class)
            captureMiddleware.reset()

            val newPrompter = IPProtectionInfoPrompter(
                store = ipProtectionStore,
                appStore = appStore,
                errorMessages = ErrorMessages(dataLimitReached = "Data limit reached"),
                mainDispatcher = StandardTestDispatcher(testScheduler),
            )
            newPrompter.start()
            testScheduler.advanceUntilIdle()

            captureMiddleware.assertNotDispatched(AppAction.IPProtectionSnackbarAction.ConnectionError::class)
        }
}
