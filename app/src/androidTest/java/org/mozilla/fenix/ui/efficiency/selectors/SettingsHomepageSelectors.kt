/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

object SettingsHomepageSelectors {
    val SETTINGS_HOMEPAGE_TITLE = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Homepage",
        description = "The Homepage Settings menu item",
        groups = listOf("requiredForPage"),
    )

    val SHOW_TOP_SITES_TOGGLE = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_ID,
        value = "show_top_sites_toggle",
        description = "Show Top Sites Toggle",
        groups = listOf("homepageSettings"),
    )

    val SHORTCUTS_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Shortcuts",
        description = "the Shortcuts button",
        groups = listOf("homepageSettings", "requiredForPage"),
    )

    val JUMP_BACK_IN_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Jump back in",
        description = "the Jump Back In button",
        groups = listOf("homepageSettings"),
    )

    val RECENT_BOOKMARKS_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = getStringResource(R.string.customize_toggle_bookmarks),
        description = "the Recent bookmarks button",
        groups = listOf("homepageSettings"),
    )

    val RECENTLY_VISITED_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Recently visited",
        description = "the Recently visited button",
        groups = listOf("homepageSettings"),
    )

    val POCKET_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Pocket",
        description = "the Pocket button",
        groups = listOf("homepageSettings"),
    )

    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun OPENING_SCREEN_OPTION(openingScreenOption: String = "") = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_ID_WITH_SIBLING_TEXT,
        value = "radio_button",
        secondaryValue = openingScreenOption,
        description = "Opening screen option: $openingScreenOption",
        groups = listOf("homepageSettings"),
    )

    val all = listOf(
        SETTINGS_HOMEPAGE_TITLE,
        SHOW_TOP_SITES_TOGGLE,
        SHORTCUTS_BUTTON,
        JUMP_BACK_IN_BUTTON,
        RECENT_BOOKMARKS_BUTTON,
        RECENTLY_VISITED_BUTTON,
        POCKET_BUTTON,
        OPENING_SCREEN_OPTION(),
    )
}
