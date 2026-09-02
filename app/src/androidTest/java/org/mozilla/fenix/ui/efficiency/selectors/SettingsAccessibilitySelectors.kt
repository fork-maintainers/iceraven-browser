/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

object SettingsAccessibilitySelectors {
    val SETTINGS_ACCESSIBILITY_TITLE = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = getStringResource(R.string.preferences_accessibility),
        description = "The Accessibility Settings header",
        groups = listOf("requiredForPage"),
    )

    val USE_SYSTEM_FONT_SIZE_TOGGLE = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_ID,
        value = "use_system_font_size_toggle",
        description = "Use System Font Size Toggle",
        groups = listOf("accessibilitySettings"),
    )

    val AUTOMATIC_FONT_SIZING_TITLE = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_TEXT,
        value = getStringResource(R.string.preference_accessibility_auto_size_2),
        description = "The Automatic font sizing title",
        groups = listOf("fontSizing"),
    )

    val AUTOMATIC_FONT_SIZING_SUMMARY = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_TEXT,
        value = getStringResource(R.string.preference_accessibility_auto_size_summary),
        description = "The Automatic font sizing summary",
        groups = listOf("fontSizing"),
    )

    val FONT_SIZE_TITLE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = "fontSizeTitle",
        description = "The Font Size title",
        groups = listOf("fontSizing"),
    )

    val FONT_SIZE_SUBTITLE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = "fontSizeSubtitle",
        description = "The Font Size summary",
        groups = listOf("fontSizing"),
    )

    val FONT_SIZE_SLIDER = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = "fontSizeSlider",
        description = "The Font Size slider",
        groups = listOf("fontSizing"),
    )

    val FONT_SIZE_SLIDER_VALUE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = "fontSizeSliderValue",
        description = "The Font Size slider percentage value",
        groups = listOf("fontSizing"),
    )

    val ZOOM_ON_ALL_WEBSITES_TITLE = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_TEXT,
        value = getStringResource(R.string.preference_accessibility_force_enable_zoom),
        description = "The Zoom on all websites title",
        groups = listOf("fontSizing"),
    )

    val ZOOM_ON_ALL_WEBSITES_SUMMARY = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_TEXT,
        value = getStringResource(R.string.preference_accessibility_force_enable_zoom_summary),
        description = "The Zoom on all websites summary",
        groups = listOf("fontSizing"),
    )

    val all = listOf(
        SETTINGS_ACCESSIBILITY_TITLE,
        USE_SYSTEM_FONT_SIZE_TOGGLE,
        AUTOMATIC_FONT_SIZING_TITLE,
        AUTOMATIC_FONT_SIZING_SUMMARY,
        FONT_SIZE_TITLE,
        FONT_SIZE_SUBTITLE,
        FONT_SIZE_SLIDER,
        FONT_SIZE_SLIDER_VALUE,
        ZOOM_ON_ALL_WEBSITES_TITLE,
        ZOOM_ON_ALL_WEBSITES_SUMMARY,
    )
}
