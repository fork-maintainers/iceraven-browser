/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.ExperimentalAndroidComponentsApi
import mozilla.components.concept.engine.ipprotection.ServiceState
import mozilla.components.feature.ipprotection.store.state.Authorized
import mozilla.components.feature.ipprotection.store.state.BYTES_PER_GB
import mozilla.components.feature.ipprotection.store.state.EligibilityStatus
import mozilla.components.feature.ipprotection.store.state.IPProtectionState
import mozilla.components.support.test.robolectric.testContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.Theme

private const val PROMO_DATE = "September 30"

@OptIn(ExperimentalAndroidComponentsApi::class)
@RunWith(AndroidJUnit4::class)
class IPProtectionScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `GIVEN the data limit is reached WHEN rendering the screen THEN the data limit UI is shown`() {
        val maxDataGb = 50f
        val state = IPProtectionState(
            eligibilityStatus = EligibilityStatus.Eligible,
            proxyStatus = Authorized.DataLimitReached,
            serviceStatus = ServiceState.Uninitialized,
            maxDataBytes = maxDataGb.toLong() * BYTES_PER_GB.toLong(),
            remainingDataBytes = 0L,
        )

        composeTestRule.setContent {
            FirefoxTheme(theme = Theme.Light) {
                IPProtectionScreen(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    readyToUse = true,
                    syncingData = false,
                    promoDate = null,
                    onVpnToggle = {},
                    onLearnMoreClick = {},
                    onGetStartedClick = {},
                    showDebugAction = false,
                    onDebugActionClick = {},
                    onNavigateBack = {},
                    onLocationClicked = {},
                )
            }
        }

        composeTestRule.onNodeWithText(
            testContext.getString(R.string.ip_protection_data_limit_label),
        ).assertExists()

        composeTestRule.onNodeWithText(
            testContext.getString(
                R.string.ip_protection_data_limit_reached_description,
                maxDataGb.toInt(),
            ),
        ).assertExists()

        composeTestRule.onNode(
            hasText(testContext.getString(R.string.ip_protection_toggle_label)) and isToggleable(),
        )
            .assertExists()
            .assertIsOff()
            .assertIsNotEnabled()

        composeTestRule.onNodeWithText(
            testContext.getString(R.string.ip_protection_location_section),
        ).assertExists()

        composeTestRule.onNodeWithText(
            testContext.getString(R.string.ip_protection_get_started),
        ).assertDoesNotExist()
    }

    @Test
    fun `GIVEN an unlimited plan and a promo date WHEN rendering the screen THEN the promo description is shown`() {
        val state = IPProtectionState(
            eligibilityStatus = EligibilityStatus.Eligible,
            proxyStatus = Authorized.Active,
            maxDataBytes = 0L,
            remainingDataBytes = 0L,
        )

        composeTestRule.setContent {
            FirefoxTheme(theme = Theme.Light) {
                IPProtectionScreen(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    readyToUse = true,
                    syncingData = false,
                    promoDate = PROMO_DATE,
                    onVpnToggle = {},
                    onLearnMoreClick = {},
                    onGetStartedClick = {},
                    showDebugAction = false,
                    onDebugActionClick = {},
                    onNavigateBack = {},
                    onLocationClicked = {},
                )
            }
        }

        composeTestRule.onNode(
            hasContentDescription("unlimited bandwidth through $PROMO_DATE", substring = true),
        ).assertExists()
        composeTestRule.onNode(
            hasContentDescription("Browse with extra protection", substring = true),
        ).assertDoesNotExist()
    }

    // Practically, we shouldn't need to rely on this behaviour - but this fallback is valuable in case of user-error.
    @Test
    fun `GIVEN an unlimited plan and a null promo date WHEN rendering the screen THEN the fallback description is shown`() {
        val state = IPProtectionState(
            eligibilityStatus = EligibilityStatus.Eligible,
            proxyStatus = Authorized.Active,
            maxDataBytes = 0L,
            remainingDataBytes = 0L,
        )

        composeTestRule.setContent {
            FirefoxTheme(theme = Theme.Light) {
                IPProtectionScreen(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    readyToUse = true,
                    syncingData = false,
                    promoDate = null,
                    onVpnToggle = {},
                    onLearnMoreClick = {},
                    onGetStartedClick = {},
                    showDebugAction = false,
                    onDebugActionClick = {},
                    onNavigateBack = {},
                    onLocationClicked = {},
                )
            }
        }

        composeTestRule.onNode(
            hasContentDescription("Browse with extra protection", substring = true),
        ).assertExists()
    }
}
