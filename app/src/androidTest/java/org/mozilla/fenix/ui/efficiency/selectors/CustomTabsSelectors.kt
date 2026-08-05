/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.helpers.TestHelper.appName
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy
import mozilla.components.feature.customtabs.R as customtabsR

object CustomTabsSelectors {

    // Custom tabs run in their own activity; all locators are device-level (UIAutomator) so they resolve
    // regardless of which activity is foreground.
    // Redesigned toolbar: the menu button is a content-description ("More options"), not the old view res-id.
    val MAIN_MENU_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
        value = getStringResource(R.string.content_description_menu),
        description = "Custom tabs main menu button",
        groups = listOf("requiredForPage"),
    )

    val CLOSE_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
        value = getStringResource(customtabsR.string.mozac_feature_customtabs_exit_button),
        description = "Custom tabs close button",
        groups = listOf("customTabToolbar"),
    )

    val SITE_INFO_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
        value = "Site information",
        description = "Custom tab site information button (opens the unified trust panel)",
        groups = listOf("customTabToolbar"),
    )

    // Main-menu items (visible after opening the custom-tab menu)
    val MENU_BACK = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = getStringResource(R.string.browser_menu_back),
        description = "Custom tab menu: Back",
        groups = listOf("customTabMainMenuItems"),
    )

    val MENU_FORWARD = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = getStringResource(R.string.browser_menu_forward),
        description = "Custom tab menu: Forward",
        groups = listOf("customTabMainMenuItems"),
    )

    val MENU_REFRESH = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = getStringResource(R.string.browser_menu_refresh),
        description = "Custom tab menu: Refresh",
        groups = listOf("customTabMainMenuItems"),
    )

    val MENU_SHARE = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = getStringResource(R.string.browser_menu_share),
        description = "Custom tab menu: Share",
        groups = listOf("customTabMainMenuItems"),
    )

    val MENU_OPEN_IN_APP = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
        value = "Open in $appName",
        description = "Custom tab menu: Open in app",
        groups = listOf("customTabMainMenuItems"),
    )

    val MENU_FIND_IN_PAGE = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
        value = getStringResource(R.string.browser_menu_find_in_page),
        description = "Custom tab menu: Find in page",
        groups = listOf("customTabMainMenuItems"),
    )

    val MENU_DESKTOP_SITE = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
        value = getStringResource(R.string.browser_menu_desktop_site),
        description = "Custom tab menu: Desktop site",
        groups = listOf("customTabMainMenuItems"),
    )

    val MENU_POWERED_BY = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = "Powered by $appName",
        description = "Custom tab menu: Powered-by branding",
        groups = listOf("customTabMainMenuItems"),
    )

    // Compose twin of MENU_BACK, required for long-press. mozLongClick dispatches on the resolved
    // element type: a UIAutomator UiObject gets UiObject.longClick() (the ~500ms system long-press
    // timeout), which this Compose button does not register as a long press — it lands as a plain
    // click and just navigates back. The Compose path instead does composeLongClick(5000ms), matching
    // the legacy robot's onNodeWithText("Back") + LONG_CLICK_DURATION.
    val MENU_BACK_COMPOSE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.browser_menu_back),
        description = "Custom tab menu: Back (Compose, for long-press)",
        groups = listOf(),
    )

    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun MENU_CUSTOM_ITEM(label: String = "") = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = label,
        description = "Custom tab menu: custom item '$label'",
        groups = listOf(),
    )

    val all = listOf(
        MAIN_MENU_BUTTON,
        CLOSE_BUTTON,
        SITE_INFO_BUTTON,
        MENU_BACK,
        MENU_BACK_COMPOSE,
        MENU_FORWARD,
        MENU_REFRESH,
        MENU_SHARE,
        MENU_OPEN_IN_APP,
        MENU_FIND_IN_PAGE,
        MENU_DESKTOP_SITE,
        MENU_POWERED_BY,
        MENU_CUSTOM_ITEM(),
    )
}
