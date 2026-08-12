/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

object SettingsSearchManageShortcutsSelectors {

    // The screen is Compose (SearchEngineShortcuts): its category header is the only stable,
    // always-present arrival signal. The individual engine rows depend on the region's engine set.
    val ENGINES_LIST_HEADER = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.preferences_category_engines_in_search_menu),
        description = "Engines visible on the search menu header",
        groups = listOf("requiredForPage"),
    )

    // A single engine row in the shortcuts list, keyed by the engine name (the row's Text node).
    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun SEARCH_ENGINE_SHORTCUT(engineName: String = "") = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = engineName,
        description = "Search engine shortcut row: $engineName",
        groups = listOf(),
    )

    val all = listOf(
        ENGINES_LIST_HEADER,
        SEARCH_ENGINE_SHORTCUT(),
    )
}
