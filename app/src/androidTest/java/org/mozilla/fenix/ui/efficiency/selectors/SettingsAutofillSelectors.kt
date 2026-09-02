/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.settings.address.ui.edit.EditAddressTestTag
import org.mozilla.fenix.settings.creditcards.ui.CreditCardEditorTestTags
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

    val DELETE_ADDRESS_TOOLBAR_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = EditAddressTestTag.TOPBAR_DELETE_BUTTON,
        description = "Edit address screen: toolbar delete button",
        groups = listOf("deleteAddress"),
    )

    val CANCEL_DELETE_ADDRESS_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = EditAddressTestTag.DIALOG_CANCEL_BUTTON,
        description = "Delete address dialog: Cancel button",
        groups = listOf("deleteAddress"),
    )

    val CONFIRM_DELETE_ADDRESS_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = EditAddressTestTag.DIALOG_DELETE_BUTTON,
        description = "Delete address dialog: Delete (confirm) button",
        groups = listOf("deleteAddress"),
    )

    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun SAVED_ADDRESS(name: String = "") = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = name,
        description = "Saved address row: $name",
        groups = listOf("manageAddresses"),
    )

    // --- Credit cards ---

    val ADD_CREDIT_CARD_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = getStringResource(R.string.preferences_credit_cards_add_credit_card_2),
        description = "The 'Add card' button",
        groups = listOf("addCreditCard"),
    )

    // Post-save anchor, mirroring MANAGE_ADDRESSES_BUTTON: saving returns to the Autofill list, which
    // only offers "Manage cards" once a card exists.
    val MANAGE_SAVED_CREDIT_CARDS_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = getStringResource(R.string.preferences_credit_cards_manage_saved_cards_2),
        description = "The 'Manage cards' button",
        groups = listOf("manageCreditCards"),
    )

    // "Later" on the system prompt offering to secure saved cards behind a device lock. It is a
    // platform AlertDialog, so the handle is the framework's negative-button id used verbatim — an
    // app-scoped strategy would look it up in the app's R class and fail to resolve it at all.
    val SECURED_CREDIT_CARDS_LATER_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RAW_RES_ID,
        value = "android:id/button2",
        description = "The 'Later' button on the secure-your-cards prompt",
        groups = listOf(),
    )

    // A saved card row in Manage cards, matched on the card-type logo — the number itself is masked.
    val SAVED_CREDIT_CARD = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "credit_card_logo",
        description = "A saved credit card row",
        groups = listOf(),
    )

    val EDIT_CREDIT_CARD_TOOLBAR_TITLE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.credit_cards_edit_card),
        description = "The 'Edit card' toolbar title",
        groups = listOf("editCreditCard"),
    )

    // --- Add/Edit card form (Compose; keyed off CreditCardEditorTestTags in main source) ---

    val CREDIT_CARD_NUMBER_FIELD = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = CreditCardEditorTestTags.CARD_NUMBER_FIELD,
        description = "Card form: Card number field",
        groups = listOf("creditCardForm"),
    )

    val CREDIT_CARD_NAME_FIELD = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = CreditCardEditorTestTags.NAME_ON_CARD_FIELD,
        description = "Card form: Name on card field",
        groups = listOf("creditCardForm"),
    )

    val CREDIT_CARD_SAVE_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = CreditCardEditorTestTags.SAVE_BUTTON,
        description = "Card form: Save button",
        groups = listOf("creditCardForm"),
    )

    // Delete lives in the editor's body, distinct from the toolbar delete action.
    val DELETE_CREDIT_CARD_MENU_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = CreditCardEditorTestTags.DELETE_BUTTON,
        description = "Card form: Delete card button",
        groups = listOf("editCreditCard"),
    )

    val DELETE_CREDIT_CARD_DIALOG_CANCEL_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = CreditCardEditorTestTags.DELETE_DIALOG_CANCEL_BUTTON,
        description = "Delete card dialog: Cancel button",
        groups = listOf(),
    )

    val DELETE_CREDIT_CARD_DIALOG_DELETE_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = CreditCardEditorTestTags.DELETE_DIALOG_DELETE_BUTTON,
        description = "Delete card dialog: Delete button",
        groups = listOf(),
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
        DELETE_ADDRESS_TOOLBAR_BUTTON,
        CANCEL_DELETE_ADDRESS_BUTTON,
        CONFIRM_DELETE_ADDRESS_BUTTON,
        ADD_CREDIT_CARD_BUTTON,
        MANAGE_SAVED_CREDIT_CARDS_BUTTON,
        SECURED_CREDIT_CARDS_LATER_BUTTON,
        SAVED_CREDIT_CARD,
        EDIT_CREDIT_CARD_TOOLBAR_TITLE,
        CREDIT_CARD_NUMBER_FIELD,
        CREDIT_CARD_NAME_FIELD,
        CREDIT_CARD_SAVE_BUTTON,
        DELETE_CREDIT_CARD_MENU_BUTTON,
        DELETE_CREDIT_CARD_DIALOG_CANCEL_BUTTON,
        DELETE_CREDIT_CARD_DIALOG_DELETE_BUTTON,
    )
}
