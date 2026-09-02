/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy
import mozilla.components.feature.addons.R as addonsR

object SettingsAddonsManagerSelectors {

    val NAVIGATE_BACK_TOOLBAR_BUTTON = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_CONTENT_DESC,
        value = "Navigate up",
        description = "Navigate back toolbar button",
        groups = listOf("requiredForPage"),
    )

    val ENABLE_OR_DISABLE_EXTENSION_TOGGLE = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "enable_switch",
        description = "Enable or disable an extension toggle",
        groups = listOf("extensionDetails"),
    )

    val ADD_ONS_LIST = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "add_ons_list",
        description = "Add-ons List",
        groups = listOf("addOns"),
    )

    // The progress bar shown while the add-ons manager list loads. Mirrors the legacy
    // waitForAddonsListProgressBarToBeGone (itemWithResId("add_ons_progress_bar")).
    val ADD_ONS_PROGRESS_BAR = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "add_ons_progress_bar",
        description = "Add-ons list loading progress bar",
        groups = listOf(),
    )

    // The "Install <addon>" button on a recommended add-on row in the add-ons manager list, keyed on
    // the addon name via its content description. requiresScroll so the framework scrolls the row into
    // view before clicking (mirrors the legacy addonsList().scrollIntoView) — needed for add-ons below
    // the fold. Mirrors the legacy installButtonForAddon (withContentDescription("Install <addon>")).
    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun INSTALL_ADDON_BUTTON(addonTitle: String = "") = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
        value = "Install $addonTitle",
        description = "Install add-on '$addonTitle' button",
        groups = listOf("requiresScroll"),
    )

    // The "Allow extension to run in private browsing" checkbox on the add-on install permission
    // dialog. Mirrors the legacy selectAllowInPrivateBrowsing (onView(withId(allow_in_private_browsing))).
    val ALLOW_IN_PRIVATE_BROWSING_CHECKBOX = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "allow_in_private_browsing",
        description = "Add-on permission dialog Allow in private browsing checkbox",
        groups = listOf(),
    )

    // The "Add" button on the add-on install permission dialog. Mirrors the legacy
    // allowPermissionToInstall (By.res("$packageName:id/allow_button")).
    val ADDON_PERMISSION_ALLOW_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "allow_button",
        description = "Add-on install permission dialog Add button",
        groups = listOf(),
    )

    // Title of the add-on install permission dialog ("Add <addon>"). Keyed on the addon name via a
    // text-contains match on the shared dialog "title" id, mirroring the legacy verifyAddonPermissionPrompt.
    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun ADDON_PERMISSION_PROMPT_TITLE(addonTitle: String = "") = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID_CONTAINING_TEXT,
        value = "title",
        secondaryValue = addonTitle,
        description = "Add-on install permission dialog title for '$addonTitle'",
        groups = listOf(),
    )

    // Title of the install-completed dialog ("<addon> was added"). The addon's display name may be
    // longer than the recommended-list name (e.g. "Bitwarden" -> "Bitwarden Password Manager"), so a
    // text-contains match on the short name matches both. Mirrors legacy verifyAddonInstallCompletedPrompt.
    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun ADDON_INSTALL_COMPLETED_TITLE(addonTitle: String = "") = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID_CONTAINING_TEXT,
        value = "title",
        secondaryValue = addonTitle,
        description = "Add-on install completed prompt for '$addonTitle'",
        groups = listOf(),
    )

    // The "OK" button on the install-completed dialog. Matched by its unique id (only that dialog has
    // confirm_button), which also keeps it language-independent. Mirrors legacy closeAddonInstallCompletePrompt.
    val ADDON_INSTALL_COMPLETED_OK_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "confirm_button",
        description = "Add-on install completed prompt OK button",
        groups = listOf(),
    )

    // An installed add-on row in the add-ons manager list, keyed on its name label. A text-contains
    // match on the short name tolerates the longer display name shown in the list.
    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun INSTALLED_ADDON_ITEM(addonTitle: String = "") = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID_CONTAINING_TEXT,
        value = "add_on_name",
        secondaryValue = addonTitle,
        description = "Installed add-on '$addonTitle' row",
        groups = listOf(),
    )

    // The "Remove" button on an add-on's detail screen. Mirrors the legacy removeAddon
    // (onView(withId(R.id.remove_add_on))).
    val REMOVE_ADDON_BUTTON = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_ID,
        value = "remove_add_on",
        description = "Remove add-on button",
        groups = listOf(),
    )

    // The "Enabled" section header in the add-ons manager, shown once an installed extension is
    // enabled. Mirrors the legacy verifyEnabledTitleDisplayed.
    val ENABLED_SECTION_TITLE = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = getStringResource(addonsR.string.mozac_feature_addons_enabled),
        description = "Add-ons manager Enabled section title",
        groups = listOf(),
    )

    val all = listOf(
        NAVIGATE_BACK_TOOLBAR_BUTTON,
        ENABLE_OR_DISABLE_EXTENSION_TOGGLE,
        ADD_ONS_LIST,
        ADD_ONS_PROGRESS_BAR,
        INSTALL_ADDON_BUTTON(),
        ALLOW_IN_PRIVATE_BROWSING_CHECKBOX,
        ADDON_PERMISSION_ALLOW_BUTTON,
        ADDON_PERMISSION_PROMPT_TITLE(),
        ADDON_INSTALL_COMPLETED_TITLE(),
        ADDON_INSTALL_COMPLETED_OK_BUTTON,
        INSTALLED_ADDON_ITEM(),
        REMOVE_ADDON_BUTTON,
        ENABLED_SECTION_TITLE,
    )
}
