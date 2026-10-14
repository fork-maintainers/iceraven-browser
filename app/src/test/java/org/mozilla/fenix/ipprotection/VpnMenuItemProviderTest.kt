/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ipprotection

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.Role.Companion.Button
import androidx.compose.ui.semantics.Role.Companion.Switch
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.spyk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mozilla.components.ExperimentalAndroidComponentsApi
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.MenuItemActionButton
import mozilla.components.compose.menu.data.MenuItemBadge
import mozilla.components.compose.menu.data.MenuItemSummary
import mozilla.components.compose.menu.data.MenuItemTitle
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.compose.menu.ui.MenuItemState
import mozilla.components.concept.engine.ipprotection.ServiceState
import mozilla.components.feature.ipprotection.store.IPProtectionAction
import mozilla.components.feature.ipprotection.store.IPProtectionStore
import mozilla.components.feature.ipprotection.store.state.Authorized
import mozilla.components.feature.ipprotection.store.state.BYTES_PER_GB
import mozilla.components.feature.ipprotection.store.state.EligibilityStatus
import mozilla.components.feature.ipprotection.store.state.IPProtectionState
import mozilla.components.feature.ipprotection.store.state.ProxyStatus
import mozilla.components.feature.ipprotection.store.state.Uninitialized
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.ui.icons.R as iconsR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.GleanMetrics.Vpn
import org.mozilla.fenix.NavGraphDirections
import org.mozilla.fenix.R
import org.mozilla.fenix.components.accounts.FenixFxAEntryPoint
import org.mozilla.fenix.components.menu.fake.FakeMenuHost
import org.mozilla.fenix.components.menu.fake.reachableEvents
import org.mozilla.fenix.components.menu.store.MenuAction
import org.mozilla.fenix.helpers.FenixGleanTestRule

@OptIn(ExperimentalAndroidComponentsApi::class)
@RunWith(AndroidJUnit4::class)
class VpnMenuItemProviderTest {
    @get:Rule val gleanRule = FenixGleanTestRule(testContext)

    private val menu = FakeMenuHost()

    @Test
    fun `GIVEN the user is not eligible WHEN building the menu item THEN don't show it`() = runTest {
        val provider = provider(IPProtectionStore(IPProtectionState()))

        assertNull(provider.itemFlow.value)
    }

    @Test
    fun `GIVEN IP protection is off WHEN building the menu item THEN show it as a switch that is off`() = runTest {
        val provider = provider(eligibleStore(Uninitialized))

        assertEquals(expectedItem(), provider.itemFlow.value)
    }

    @Test
    fun `GIVEN IP protection is on WHEN building the menu item THEN show it as active`() = runTest {
        val provider = provider(eligibleStore(Authorized.Active))

        assertEquals(
            expectedItem(
                badgeTextRes = R.string.preferences_ip_protection_on,
                badgeState = MenuItemState.ACTIVE,
            ),
            provider.itemFlow.value,
        )
    }

    @Test
    fun `GIVEN authentication is needed WHEN building the menu item THEN show it as a button rather than a switch`() =
        runTest {
            val store =
                IPProtectionStore(
                    IPProtectionState(
                        eligibilityStatus = EligibilityStatus.Eligible,
                        serviceStatus = ServiceState.Unauthenticated,
                    )
                )
            val provider = provider(store)

            assertEquals(
                expectedItem(badgeTextRes = R.string.ip_protection_menu_try_vpn_cta, role = Button),
                provider.itemFlow.value,
            )
        }

    @Test
    fun `GIVEN the data limit was reached WHEN building the menu item THEN explain it in the summary`() = runTest {
        val store =
            IPProtectionStore(
                IPProtectionState(
                    eligibilityStatus = EligibilityStatus.Eligible,
                    proxyStatus = Authorized.DataLimitReached,
                    maxDataBytes = (5 * BYTES_PER_GB).toLong(),
                )
            )
        val provider = provider(store)

        assertEquals(
            MenuItemSummary(
                text = Text.Resource(R.string.ip_protection_menu_limit_reached, listOf(5)),
                state = MenuItemState.WARNING,
            ),
            (provider.itemFlow.value as StandardMenuItem).summary,
        )
    }

    @Test
    fun `GIVEN the data limit is unknown WHEN building the menu item THEN don't show a summary`() = runTest {
        val provider = provider(eligibleStore(Uninitialized))

        assertNull((provider.itemFlow.value as StandardMenuItem).summary)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `WHEN the user stops being eligible THEN stop showing the menu item`() = runTest {
        val store = eligibleStore(Uninitialized)
        val provider = provider(store)

        assertEquals(expectedItem(), provider.itemFlow.value)

        store.dispatch(IPProtectionAction.EligibilityChanged(EligibilityStatus.Ineligible))
        runCurrent()

        assertNull(provider.itemFlow.value)
    }

    @Test
    fun `GIVEN IP protection is off WHEN toggling it THEN turn it on and keep the menu open`() = runTest {
        val store = spyk(eligibleStore(Authorized.Idle))

        provider(store).onEvent(MenuAction.IPProtectionToggle, menu)

        verify { store.dispatch(IPProtectionAction.Toggle) }
        assertFalse(menu.isUsed)
        assertNotNull(Vpn.menuTurnedOn.testGetValue())
    }

    @Test
    fun `GIVEN IP protection is on WHEN toggling it THEN turn it off and keep the menu open`() = runTest {
        val store = spyk(eligibleStore(Authorized.Active))

        provider(store).onEvent(MenuAction.IPProtectionToggle, menu)

        verify { store.dispatch(IPProtectionAction.Toggle) }
        assertFalse(menu.isUsed)
        assertNotNull(Vpn.menuTurnedOff.testGetValue())
    }

    @Test
    fun `GIVEN signing in is needed WHEN toggling IP protection THEN show its settings instead`() = runTest {
        val store = IPProtectionStore(IPProtectionState(serviceStatus = ServiceState.Unauthenticated))

        provider(store).onEvent(MenuAction.IPProtectionToggle, menu)

        assertEquals(settingsDirections, menu.directions)
        assertNotNull(Vpn.menuTryItTapped.testGetValue())
    }

    @Test
    fun `WHEN asking for the IP protection settings THEN show them in place of the menu`() = runTest {
        provider(eligibleStore(Authorized.Idle)).onEvent(MenuAction.Navigate.IPProtectionSettings, menu)

        assertEquals(settingsDirections, menu.directions)
        assertEquals("Menu", Vpn.settingsPageTapped.testGetValue()?.last()?.extra?.get("entrypoint"))
    }

    @Test
    fun `WHEN IP protection is on or off THEN handle all events the item can dispatch and no others`() = runTest {
        listOf(Authorized.Idle, Authorized.Active).forEach { status ->
            val provider = provider(eligibleStore(status))

            val events = requireNotNull(provider.itemFlow.value).reachableEvents()

            assertTrue(events.all { provider.handles(it) }, "Not all of $events are handled")
            assertFalse(provider.handles(MenuAction.Navigate.Settings))
        }
    }

    private val settingsDirections =
        NavGraphDirections.actionGlobalIpProtectionFragment(entrypoint = FenixFxAEntryPoint.IPProtectionMainMenu)

    // The item is kept up to date on a scope that runTest cancels at the end of each test.
    private fun TestScope.provider(ipProtectionStore: IPProtectionStore) =
        VpnMenuItemProvider(ipProtectionStore = ipProtectionStore, scope = backgroundScope)

    private fun eligibleStore(proxyStatus: ProxyStatus) =
        IPProtectionStore(IPProtectionState(eligibilityStatus = EligibilityStatus.Eligible, proxyStatus = proxyStatus))

    private fun expectedItem(
        badgeTextRes: Int = R.string.preferences_ip_protection_off,
        badgeState: MenuItemState = MenuItemState.DEFAULT,
        role: Role = Switch,
    ) =
        StandardMenuItem(
            title = MenuItemTitle(Text.Resource(R.string.ip_protection_toggle_label)),
            icon = MenuItemIconRes(iconsR.drawable.mozac_ic_globe_24),
            role = role,
            onClickEvent = MenuAction.IPProtectionToggle,
            badge = MenuItemBadge(text = Text.Resource(badgeTextRes), state = badgeState),
            actionButton =
                MenuItemActionButton(
                    icon = iconsR.drawable.mozac_ic_chevron_right_24,
                    contentDescription = Text.Resource(R.string.ip_protection_navigate_settings),
                    onClickEvent = MenuAction.Navigate.IPProtectionSettings,
                    showDivider = true,
                ),
        )
}
