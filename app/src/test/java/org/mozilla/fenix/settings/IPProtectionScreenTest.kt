/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.ExperimentalAndroidComponentsApi
import mozilla.components.compose.base.theme.Theme
import mozilla.components.concept.engine.ipprotection.ServiceState
import mozilla.components.feature.ipprotection.store.state.AccountState
import mozilla.components.feature.ipprotection.store.state.AccountStatus
import mozilla.components.feature.ipprotection.store.state.Authorized
import mozilla.components.feature.ipprotection.store.state.BYTES_PER_GB
import mozilla.components.feature.ipprotection.store.state.EligibilityStatus
import mozilla.components.feature.ipprotection.store.state.IPProtectionState
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.mozilla.fenix.theme.FirefoxTheme

private const val PROMO_DATE = "September 30"

@OptIn(ExperimentalAndroidComponentsApi::class)
@RunWith(AndroidJUnit4::class)
class IPProtectionScreenTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun `GIVEN the data limit is reached WHEN rendering the screen THEN the data limit UI is shown`() {
        val maxDataGb = 50f
        val state =
            IPProtectionState(
                eligibilityStatus = EligibilityStatus.Eligible,
                proxyStatus = Authorized.DataLimitReached,
                serviceStatus = ServiceState.Ready,
                maxDataBytes = maxDataGb.toLong() * BYTES_PER_GB.toLong(),
                remainingDataBytes = 0L,
            )

        setScreen(state = state)

        composeTestRule.onNodeWithText(testContext.getString(R.string.ip_protection_data_limit_label)).assertExists()

        composeTestRule
            .onNodeWithText(
                testContext.getString(
                    R.string.ip_protection_data_limit_reached_description,
                    maxDataGb.toInt(),
                )
            )
            .assertExists()

        composeTestRule
            .onNode(hasText(testContext.getString(R.string.ip_protection_toggle_label)) and isToggleable())
            .assertExists()
            .assertIsOff()
            .assertIsNotEnabled()

        composeTestRule.onNodeWithText(testContext.getString(R.string.ip_protection_location_section)).assertExists()

        composeTestRule.onNodeWithText(testContext.getString(R.string.ip_protection_get_started)).assertDoesNotExist()
    }

    @Test
    fun `GIVEN no promo is running WHEN rendering the screen THEN the description is shown instead of the promo card`() {
        val state =
            IPProtectionState(
                eligibilityStatus = EligibilityStatus.Eligible,
                proxyStatus = Authorized.Active,
                serviceStatus = ServiceState.Ready,
                maxDataBytes = 50L * BYTES_PER_GB.toLong(),
                remainingDataBytes = 40L * BYTES_PER_GB.toLong(),
            )

        setScreen(state = state)

        composeTestRule.onNode(hasVpnDescription()).assertExists()

        composeTestRule.onNode(hasPromoCardTitle()).assertDoesNotExist()
    }

    @Test
    fun `GIVEN no promo is running WHEN the description learn more link is clicked THEN the callback is invoked`() {
        var learnMoreClicks = 0
        val state =
            IPProtectionState(
                eligibilityStatus = EligibilityStatus.Eligible,
                proxyStatus = Authorized.Active,
                serviceStatus = ServiceState.Ready,
                maxDataBytes = 50L * BYTES_PER_GB.toLong(),
                remainingDataBytes = 40L * BYTES_PER_GB.toLong(),
            )

        setScreen(state = state, onLearnMoreClick = { learnMoreClicks++ })

        composeTestRule.onNode(hasVpnDescription()).performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(1, learnMoreClicks)
    }

    @Test
    fun `GIVEN an unlimited plan and a promo date WHEN rendering the screen THEN the promo description is shown`() {
        val state =
            IPProtectionState(
                eligibilityStatus = EligibilityStatus.Eligible,
                proxyStatus = Authorized.Active,
                maxDataBytes = 0L,
                remainingDataBytes = 0L,
            )

        setScreen(state = state, promoDate = PROMO_DATE)

        composeTestRule
            .onNode(hasContentDescription("unlimited bandwidth through $PROMO_DATE", substring = true))
            .assertExists()
        composeTestRule.onNode(hasVpnDescription()).assertDoesNotExist()
    }

    // The promo card advertises unlimited bandwidth, which must not run alongside a data cap - bug 2045899.
    @Test
    fun `GIVEN a metered plan and a promo date WHEN rendering the screen THEN the description is shown`() {
        val state =
            IPProtectionState(
                eligibilityStatus = EligibilityStatus.Eligible,
                proxyStatus = Authorized.Active,
                serviceStatus = ServiceState.Ready,
                maxDataBytes = 50L * BYTES_PER_GB.toLong(),
                remainingDataBytes = 40L * BYTES_PER_GB.toLong(),
            )

        setScreen(state = state, promoDate = PROMO_DATE)

        composeTestRule.onNode(hasVpnDescription()).assertExists()

        composeTestRule.onNode(hasPromoCardTitle()).assertDoesNotExist()
    }

    @Test
    fun `GIVEN an unlimited plan and a null promo date WHEN rendering the screen THEN the description is shown`() {
        val state =
            IPProtectionState(
                eligibilityStatus = EligibilityStatus.Eligible,
                proxyStatus = Authorized.Active,
                maxDataBytes = 0L,
                remainingDataBytes = 0L,
            )

        setScreen(state = state)

        composeTestRule.onNode(hasVpnDescription()).assertExists()

        composeTestRule.onNode(hasPromoCardTitle()).assertDoesNotExist()
    }

    // maxDataBytes is -1 until the service reports usage, which must not be mistaken for the uncapped promo plan.
    @Test
    fun `GIVEN usage data has not arrived and no promo is running WHEN rendering the screen THEN the description is shown`() {
        setScreen(state = IPProtectionState(eligibilityStatus = EligibilityStatus.Eligible))

        composeTestRule.onNode(hasVpnDescription()).assertExists()

        composeTestRule.onNode(hasPromoCardTitle()).assertDoesNotExist()
    }

    @Test
    fun `GIVEN usage data has not arrived and a promo is running WHEN rendering the screen THEN the promo card is shown`() {
        setScreen(state = IPProtectionState(eligibilityStatus = EligibilityStatus.Eligible), promoDate = PROMO_DATE)

        composeTestRule.onNode(hasPromoCardTitle()).assertExists()
    }

    @Test
    fun `GIVEN the proxy is activating WHEN rendering the screen THEN the location row is not selectable`() {
        val state =
            IPProtectionState(
                eligibilityStatus = EligibilityStatus.Eligible,
                proxyStatus = Authorized.Activating,
                serviceStatus = ServiceState.Ready,
                maxDataBytes = 0L,
                remainingDataBytes = 0L,
            )

        setScreen(state = state, isLocationSelectionEnabled = true)

        composeTestRule
            .onNode(hasText(testContext.getString(R.string.ip_protection_toggle_label)) and isToggleable())
            .assertIsNotEnabled()

        composeTestRule
            .onNodeWithText(testContext.getString(R.string.ip_protection_location_recommended_label))
            .assertHasNoClickAction()
    }

    @Test
    fun `GIVEN the user has to sign in WHEN rendering the screen THEN an enabled Get started button is shown`() {
        setScreen(
            state =
                IPProtectionState(
                    eligibilityStatus = EligibilityStatus.Eligible,
                    serviceStatus = ServiceState.Unauthenticated,
                    accountState = AccountState(AccountStatus.NoAccount),
                )
        )

        composeTestRule.onNodeWithText(testContext.getString(R.string.ip_protection_get_started)).assertIsEnabled()
        composeTestRule
            .onNodeWithText(testContext.getString(R.string.ip_protection_location_section))
            .assertDoesNotExist()
    }

    @Test
    fun `GIVEN the service has not accepted the signed-in account yet WHEN rendering the screen THEN a disabled Connecting button is shown`() {
        setScreen(
            state =
                IPProtectionState(
                    eligibilityStatus = EligibilityStatus.Eligible,
                    serviceStatus = ServiceState.Unauthenticated,
                    accountState = AccountState(AccountStatus.Authenticated),
                )
        )

        composeTestRule.onNodeWithText(testContext.getString(R.string.ip_protection_connecting)).assertIsNotEnabled()
        composeTestRule.onNodeWithText(testContext.getString(R.string.ip_protection_get_started)).assertDoesNotExist()
    }

    @Test
    fun `GIVEN the service is unavailable WHEN rendering the screen THEN the Get started button is disabled`() {
        setScreen(
            state =
                IPProtectionState(
                    eligibilityStatus = EligibilityStatus.Eligible,
                    serviceStatus = ServiceState.Unavailable,
                )
        )

        composeTestRule.onNodeWithText(testContext.getString(R.string.ip_protection_get_started)).assertIsNotEnabled()
    }

    private fun setScreen(
        state: IPProtectionState,
        promoDate: String? = null,
        onLearnMoreClick: () -> Unit = {},
        isLocationSelectionEnabled: Boolean = false,
    ) {
        composeTestRule.setContent {
            FirefoxTheme(theme = Theme.Light) {
                IPProtectionScreen(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    promoDate = promoDate,
                    onVpnToggle = {},
                    onLearnMoreClick = onLearnMoreClick,
                    onGetStartedClick = {},
                    showDebugAction = false,
                    onDebugActionClick = {},
                    onNavigateBack = {},
                    onLocationClicked = {},
                    isLocationSelectionEnabled = isLocationSelectionEnabled,
                )
            }
        }
    }
}

private fun hasPromoCardTitle() =
    hasText(testContext.getString(R.string.ip_protection_promo_headline, testContext.getString(R.string.firefox)))

private fun hasVpnDescription() =
    hasContentDescription(
        testContext.getString(
            R.string.ip_protection_promo_body_2,
            testContext.getString(R.string.ip_protection_learn_more),
        ),
        substring = true,
    )
