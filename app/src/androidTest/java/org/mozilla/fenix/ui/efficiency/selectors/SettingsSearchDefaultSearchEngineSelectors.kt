package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

object SettingsSearchDefaultSearchEngineSelectors {

    val DEFAULT_SEARCH_ENGINE_TITLE = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_TEXT,
        value = "Default search engine",
        description = "Default search engine title",
        groups = listOf("requiredForPage"),
    )

    @Suppress("ktlint:standard:function-naming")
    fun DEFAULT_SEARCH_ENGINE_OPTION(engineName: String = "") = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID_AND_TEXT,
        value = "engine_text",
        secondaryValue = engineName,
        description = "Default search engine option: $engineName",
        groups = listOf("defaultSearchEngines"),
    )

    // The "Add search engine" row at the bottom of the default-engine list; opens the custom-engine form.
    val ADD_SEARCH_ENGINE_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = getStringResource(R.string.search_engine_add_custom_search_engine_title),
        description = "Add search engine button",
        groups = listOf(),
    )

    // Overflow (three-dot) button on a custom engine's row. Only custom engines expose it on this
    // screen, so a plain content-description match is unique when a single custom engine is present.
    val ENGINE_OVERFLOW_MENU = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
        value = getStringResource(R.string.content_description_menu),
        description = "Custom engine overflow menu button",
        groups = listOf(),
    )

    val OVERFLOW_EDIT_ITEM = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = getStringResource(R.string.search_engine_edit),
        description = "Edit item in the custom engine overflow menu",
        groups = listOf(),
    )

    val all = listOf(
        DEFAULT_SEARCH_ENGINE_TITLE,
        DEFAULT_SEARCH_ENGINE_OPTION(),
        ADD_SEARCH_ENGINE_BUTTON,
        ENGINE_OVERFLOW_MENU,
        OVERFLOW_EDIT_ITEM,
    )
}
