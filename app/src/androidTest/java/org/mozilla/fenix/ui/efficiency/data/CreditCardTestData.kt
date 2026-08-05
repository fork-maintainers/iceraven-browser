/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.data

import java.time.LocalDate

/**
 * A saved credit card, as entered in Settings > Autofill and then offered on a web payment form.
 *
 * Immutable for the same reason as [AddressDetails]: the legacy equivalent
 * (CreditCardAutofillTest.MockCreditCard1) is an `object` of constants that tests reach into directly.
 */
data class CreditCardDetails(
    val number: String,
    val lastDigits: String,
    val nameOnCard: String,
    val expiryMonth: String,
    val expiryYear: String,
)

object CreditCardTestData {

    // Expiry year is relative to today: a hardcoded year eventually goes into the past, and the editor
    // does not offer past years, so the dropdown selection would start failing on a fixed date.
    val FIRST = CreditCardDetails(
        number = "5555555555554444",
        lastDigits = "4444",
        nameOnCard = "Mastercard",
        expiryMonth = "February",
        expiryYear = (LocalDate.now().year + 1).toString(),
    )
}
