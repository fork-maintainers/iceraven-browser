/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.TestAssetHelper.addressFormAsset
import org.mozilla.fenix.ui.efficiency.data.AddressTestData
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest

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
}
