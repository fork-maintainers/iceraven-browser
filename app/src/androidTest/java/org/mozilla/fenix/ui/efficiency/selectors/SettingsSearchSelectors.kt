/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

object SettingsSearchSelectors {
    val SETTINGS_SEARCH_TITLE = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_TEXT,
        value = "Search",
        description = "the Settings Search title",
        groups = listOf("requiredForPage"),
    )

    val DEFAULT_SEARCH_ENGINE_SETTING_OPTION = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_TEXT,
        value = "Default search engine",
        description = "Default search engine option",
        groups = listOf("requiredForPage"),
    )

    val MANAGE_SHORTCUTS_SETTING_OPTION = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_TEXT,
        value = getStringResource(R.string.preferences_manage_search_shortcuts_2),
        description = "Manage alternative search engines option",
        groups = listOf(),
    )

    // Below-fold preference row; clicking the title toggles the switch (mirrors the legacy
    // toggleShowSearchSuggestions). requiresScroll drives the harness to scroll it into view first.
    val SHOW_SEARCH_SUGGESTIONS_TOGGLE = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_TEXT,
        value = getStringResource(R.string.preferences_show_search_suggestions),
        description = "Show search suggestions toggle",
        groups = listOf("requiresScroll"),
    )

    val all = listOf(
        SETTINGS_SEARCH_TITLE,
        DEFAULT_SEARCH_ENGINE_SETTING_OPTION,
        MANAGE_SHORTCUTS_SETTING_OPTION,
        SHOW_SEARCH_SUGGESTIONS_TOGGLE,
    )
}
