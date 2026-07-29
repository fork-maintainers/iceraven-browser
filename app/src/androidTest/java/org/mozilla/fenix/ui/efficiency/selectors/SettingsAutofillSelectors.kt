/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.settings.address.ui.edit.EditAddressTestTag
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

object SettingsAutofillSelectors {
    val SETTINGS_AUTOFILL_TITLE = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Autofill",
        description = "The Autofill Settings title",
        groups = listOf("requiredForPage"),
    )

    val AUTOFILL_ADDRESSES_TOGGLE = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_ID,
        value = "autofill_addresses_toggle",
        description = "Autofill Addresses Toggle",
        groups = listOf("autofillSettings"),
    )

    // "Add address" entry point on the Autofill settings screen (and inside Manage addresses).
    val ADD_ADDRESS_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = getStringResource(R.string.preferences_addresses_add_address),
        description = "The 'Add address' button",
        groups = listOf("addAddress"),
    )

    // Post-save anchor: after saving an address the screen returns to the Autofill list, which shows
    // the "Manage addresses" button. Used to confirm the save completed.
    val MANAGE_ADDRESSES_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = getStringResource(R.string.preferences_addresses_manage_addresses),
        description = "The 'Manage addresses' button",
        groups = listOf("manageAddresses"),
    )

    // --- Add/Edit address form fields (Compose; keyed off EditAddressTestTag in main source) ---

    val ADDRESS_NAME_FIELD = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = EditAddressTestTag.NAME_FIELD,
        description = "Address form: Name field",
        groups = listOf("addressForm"),
    )

    val ADDRESS_STREET_FIELD = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = EditAddressTestTag.STREET_ADDRESS_FIELD,
        description = "Address form: Street address field",
        groups = listOf("addressForm"),
    )

    val ADDRESS_CITY_FIELD = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = EditAddressTestTag.ADDRESS_LEVEL2_FIELD,
        description = "Address form: City field",
        groups = listOf("addressForm"),
    )

    val ADDRESS_STATE_DROPDOWN = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = EditAddressTestTag.ADDRESS_LEVEL1_FIELD,
        description = "Address form: State/sub-region dropdown",
        groups = listOf("addressForm"),
    )

    val ADDRESS_ZIP_FIELD = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = EditAddressTestTag.POSTAL_CODE_FIELD,
        description = "Address form: Zip code field",
        groups = listOf("addressForm"),
    )

    val ADDRESS_COUNTRY_DROPDOWN = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = EditAddressTestTag.COUNTRY_FIELD,
        description = "Address form: Country dropdown",
        groups = listOf("addressForm"),
    )

    val ADDRESS_PHONE_FIELD = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = EditAddressTestTag.TEL_FIELD,
        description = "Address form: Phone field",
        groups = listOf("addressForm"),
    )

    val ADDRESS_EMAIL_FIELD = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = EditAddressTestTag.EMAIL_FIELD,
        description = "Address form: Email field",
        groups = listOf("addressForm"),
    )

    val ADDRESS_SAVE_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = EditAddressTestTag.SAVE_BUTTON,
        description = "Address form: Save button",
        groups = listOf("addressForm"),
    )

    val all = listOf(
        SETTINGS_AUTOFILL_TITLE,
        AUTOFILL_ADDRESSES_TOGGLE,
        ADD_ADDRESS_BUTTON,
        MANAGE_ADDRESSES_BUTTON,
        ADDRESS_NAME_FIELD,
        ADDRESS_STREET_FIELD,
        ADDRESS_CITY_FIELD,
        ADDRESS_STATE_DROPDOWN,
        ADDRESS_ZIP_FIELD,
        ADDRESS_COUNTRY_DROPDOWN,
        ADDRESS_PHONE_FIELD,
        ADDRESS_EMAIL_FIELD,
        ADDRESS_SAVE_BUTTON,
    )
}
