/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.data

/**
 * A saved address, as entered in Settings > Autofill and then offered on a web address form.
 *
 * Immutable on purpose: the legacy equivalents (AddressAutofillTest.FirstAddressAutofillDetails) are
 * `object`s of `var`s, so one test mutating a field silently changes what every other test fills in.
 * Use [copy] for a variation instead.
 */
data class AddressDetails(
    val name: String,
    val streetAddress: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String,
    val phoneNumber: String,
    val emailAddress: String,
)

/**
 * Shared address fixtures. Tests that fill, edit or assert an autofill address should take their values
 * from here rather than inlining literals, so the same street/city/zip can be changed in one place —
 * the same values are typed into the settings form, matched in the suggestion list, and asserted in the
 * autofilled web field, and those three uses must agree.
 */
object AddressTestData {

    val FIRST = AddressDetails(
        name = "Mozilla Fenix Firefox",
        streetAddress = "Harrison Street",
        city = "San Francisco",
        state = "Alaska",
        zipCode = "94105",
        country = "United States",
        phoneNumber = "555-5555",
        emailAddress = "foo@bar.com",
    )

    val SECOND = AddressDetails(
        name = "Android Test Name",
        streetAddress = "Fort Street",
        city = "Alberta",
        state = "Alberta",
        zipCode = "95141",
        country = "Canada",
        phoneNumber = "777-7777",
        emailAddress = "fuu@bar.org",
    )
}
