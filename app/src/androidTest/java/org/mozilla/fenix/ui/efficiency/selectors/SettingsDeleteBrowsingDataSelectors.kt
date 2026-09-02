/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

object SettingsDeleteBrowsingDataSelectors {

    val DELETE_BROWSING_DATA_BUTTON = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_ID,
        value = "delete_data",
        description = "Delete browsing data button",
        groups = listOf("requiredForPage"),
    )

    // Each data-type row is a CheckBox (R.id.checkbox) beside its title TextView; match the checkbox by
    // its id together with the sibling title text, mirroring the legacy robot's matchers.
    private fun checkBox(labelResId: Int, description: String) = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_ID_WITH_SIBLING_TEXT,
        value = "checkbox",
        secondaryValue = getStringResource(labelResId),
        description = description,
        groups = listOf("dataTypeCheckBoxes"),
    )

    val OPEN_TABS_CHECKBOX =
        checkBox(R.string.preferences_delete_browsing_data_tabs_title_2, "Open tabs check box")
    val BROWSING_HISTORY_CHECKBOX =
        checkBox(R.string.preferences_delete_browsing_data_browsing_history_title, "Browsing history check box")
    val COOKIES_CHECKBOX =
        checkBox(R.string.preferences_delete_browsing_data_cookies_and_site_data, "Cookies and site data check box")
    val CACHED_FILES_CHECKBOX =
        checkBox(R.string.preferences_delete_browsing_data_cached_files, "Cached images and files check box")
    val SITE_PERMISSIONS_CHECKBOX =
        checkBox(R.string.preferences_delete_browsing_data_site_permissions, "Site permissions check box")
    val DOWNLOADS_CHECKBOX =
        checkBox(R.string.preferences_delete_browsing_data_downloads, "Downloads check box")

    // Confirmation dialog. UIAutomator matches across windows, so it sees the dialog that a plain
    // Espresso onView (default root) would miss without an inRoot(isDialog()) qualifier.
    val DELETE_DIALOG_MESSAGE = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = getStringResource(R.string.delete_browsing_data_prompt_message_3),
        description = "Delete browsing data dialog message",
        groups = listOf("deleteBrowsingDataDialog"),
    )

    // UiObject2 (not UiObject): tapping these dismisses the dialog, a window change that UiObject's
    // clickAndSync reports as a failed click even when it lands (see the note on
    // UIAUTOMATOR2_BY_DESCRIPTION_CONTAINS in SelectorStrategy). UiObject2.click() just injects the tap.
    val DELETE_DIALOG_CANCEL_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR2_BY_TEXT,
        value = getStringResource(R.string.delete_browsing_data_prompt_cancel),
        description = "Delete browsing data dialog Cancel button",
        groups = listOf("deleteBrowsingDataDialog"),
    )

    val DELETE_DIALOG_DELETE_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR2_BY_TEXT,
        value = getStringResource(R.string.delete_browsing_data_prompt_allow),
        description = "Delete browsing data dialog Delete button",
        groups = listOf("deleteBrowsingDataDialog"),
    )

    val DELETION_SNACKBAR = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = getStringResource(R.string.preferences_delete_browsing_data_snackbar),
        description = "Browsing data deleted snackbar",
        groups = listOf(),
    )

    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun OPEN_TABS_DETAILS(count: String = "0") = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "$count tabs",
        description = "Open tabs summary: $count tabs",
        groups = listOf(),
    )

    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun BROWSING_HISTORY_DETAILS(addresses: String = "0") = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = "$addresses addresses",
        description = "Browsing history summary: $addresses addresses",
        groups = listOf(),
    )

    val all = listOf(
        DELETE_BROWSING_DATA_BUTTON,
        OPEN_TABS_CHECKBOX,
        BROWSING_HISTORY_CHECKBOX,
        COOKIES_CHECKBOX,
        CACHED_FILES_CHECKBOX,
        SITE_PERMISSIONS_CHECKBOX,
        DOWNLOADS_CHECKBOX,
        DELETE_DIALOG_MESSAGE,
        DELETE_DIALOG_CANCEL_BUTTON,
        DELETE_DIALOG_DELETE_BUTTON,
        DELETION_SNACKBAR,
        OPEN_TABS_DETAILS(),
        BROWSING_HISTORY_DETAILS(),
    )
}
