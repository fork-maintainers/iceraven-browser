/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.ui.efficiency.helpers.PageReadinessProfile
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorContainer
import org.mozilla.fenix.ui.efficiency.helpers.SelectorGroup
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy
import org.mozilla.fenix.ui.efficiency.helpers.SwipeDirection

object SettingsCustomizeSelectors : SelectorContainer {
    enum class Group : SelectorGroup {
        CUSTOMIZE_SETTINGS,
        APP_ICON_DEFAULT,
        TOOLBAR_LAYOUT,
    }

    val SETTINGS_CUSTOMIZE_TITLE =
        navigationToolbarTitle(
            title = getStringResource(R.string.preferences_customize),
            description = "Customize toolbar title",
        )

    val SHOW_TAB_BAR_TOGGLE =
        Selector(
            strategy = SelectorStrategy.ESPRESSO_BY_TEXT,
            value = getStringResource(R.string.preference_tab_strip_show),
            description = "Show tab bar toggle",
            groups = setOf(Group.CUSTOMIZE_SETTINGS),
            // INTERACTIVE only (not NAVIGATION_READY): this toggle sits above the lower gesture options, so
            // interacting with one (e.g. "Scroll to hide toolbar") scrolls it off the top. The always-visible
            // navigation-toolbar title is the page's NAVIGATION_READY anchor; gating navigation on this toggle too
            // would fail the source-page readiness check when leaving the screen from a scrolled position.
            readiness = setOf(PageReadinessProfile.INTERACTIVE),
            scrollDirection = SwipeDirection.UP,
        )

    val SELECT_APP_ICON_TITLE =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
            value = getStringResource(R.string.preference_select_app_icon_title),
            description = "Select App Icon title",
            groups = setOf(Group.APP_ICON_DEFAULT),
        )

    val APP_ICON_DEFAULT =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
            value = "Default",
            description = "Default app icon option",
            groups = setOf(Group.APP_ICON_DEFAULT),
        )

    val TOOLBAR_LAYOUT_SIMPLE =
        Selector(
            strategy = SelectorStrategy.ESPRESSO_BY_TEXT,
            value = getStringResource(R.string.preference_simple_toolbar),
            description = "Simple toolbar layout option",
            groups = setOf(Group.TOOLBAR_LAYOUT),
            scrollDirection = SwipeDirection.DOWN,
        )

    val TOOLBAR_LAYOUT_EXPANDED =
        Selector(
            strategy = SelectorStrategy.ESPRESSO_BY_TEXT,
            value = getStringResource(R.string.preference_expanded_toolbar),
            description = "Expanded toolbar layout option",
            groups = setOf(Group.TOOLBAR_LAYOUT),
            scrollDirection = SwipeDirection.UP,
        )

    val TOOLBAR_POSITION_BOTTOM =
        Selector(
            strategy = SelectorStrategy.ESPRESSO_BY_TEXT,
            value = getStringResource(R.string.preference_bottom_toolbar),
            description = "Bottom toolbar position option",
            scrollDirection = SwipeDirection.UP,
        )

    val NAVIGATE_BACK_TOOLBAR_BUTTON =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
            value = "Navigate up",
            description = "Navigate back toolbar button",
        )

    val SCROLL_TO_HIDE_TOOLBAR_OPTION =
        Selector(
            strategy = SelectorStrategy.ESPRESSO_BY_TEXT,
            value = getStringResource(R.string.preference_gestures_dynamic_toolbar),
            description = "Scroll to hide toolbar option",
            scrollDirection = SwipeDirection.UP,
        )

    override val scrollTraversalOrder: Map<SelectorGroup, List<Selector>> =
        mapOf(Group.TOOLBAR_LAYOUT to listOf(TOOLBAR_LAYOUT_SIMPLE, TOOLBAR_LAYOUT_EXPANDED))
}
