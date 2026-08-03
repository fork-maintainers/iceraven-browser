/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.TestAssetHelper.addressFormAsset
import org.mozilla.fenix.ui.efficiency.data.AddressTestData
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.selectors.SettingsAutofillSelectors

class AddressAutofillTest : BaseTest() {
    private val mockWebServer get() = fenixTestRule.mockWebServer

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3205329
    @SmokeTest
    @Test
    fun verifyAddressAutofillTest() {
        val addressFormPage = mockWebServer.addressFormAsset
        val address = AddressTestData.FIRST

        on.settingsAutofill.navigateToPage()
            .fillAndSaveAddress(address)

        on.browserPage.navigateToPage(addressFormPage.url.toString())
            .clickAddressFormStreetField()
            .clickSelectAddressButton()
            .clickAddressSuggestion(address.streetAddress)
            .verifyAutofilledAddress(address.streetAddress)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3205332
    @SmokeTest
    @Test
    fun deleteSavedAddressTest() {
        val address = AddressTestData.FIRST

        on.settingsAutofill.navigateToPage()
            .fillAndSaveAddress(address)
            .mozClick(SettingsAutofillSelectors.MANAGE_ADDRESSES_BUTTON)
            .mozVerify(SettingsAutofillSelectors.SAVED_ADDRESS(address.name))
            .mozClick(SettingsAutofillSelectors.SAVED_ADDRESS(address.name))
            .mozVerify(SettingsAutofillSelectors.DELETE_ADDRESS_TOOLBAR_BUTTON)
            .mozClick(SettingsAutofillSelectors.DELETE_ADDRESS_TOOLBAR_BUTTON)
            .mozClick(SettingsAutofillSelectors.CANCEL_DELETE_ADDRESS_BUTTON)
            .mozClick(SettingsAutofillSelectors.DELETE_ADDRESS_TOOLBAR_BUTTON)
            .mozClick(SettingsAutofillSelectors.CONFIRM_DELETE_ADDRESS_BUTTON)
            .mozVerify(SettingsAutofillSelectors.ADD_ADDRESS_BUTTON)
    }
}
