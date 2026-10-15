/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import mozilla.components.support.utils.ext.pixelSizeFor
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.BaseBrowserFragment
import org.mozilla.fenix.components.toolbar.ToolbarPosition

/** Returns true if the microsurvey prompt is shown above the bottom toolbar. */
internal fun BaseBrowserFragment.isMicrosurveyPromptVisible(): Boolean =
    requireComponents.settings.microsurveyFeatureEnabled && requireComponents.appStore.state.microsurvey.current != null

/**
 * Returns the height of the bottom toolbar.
 *
 * The bottom toolbar can consist of:
 * - a combination of address bar, navigation bar & a microsurvey.
 * - be absent.
 *
 * @param includeTabStripIfAvailable If true and the tab strip feature is enabled it's height will be included in the
 *   calculation.
 * @param includeNavBarIfEnabled If true and the navigation bar feature is enabled it's height will be included in the
 *   calculation.
 * @param includeTabGroupsStrip If true and the tab groups strip is enabled its height will be included in the
 *   calculation.
 */
internal fun BaseBrowserFragment.getBottomToolbarHeight(
    includeTabStripIfAvailable: Boolean = true,
    includeNavBarIfEnabled: Boolean = true,
    includeTabGroupsStrip: Boolean = true,
): Int {
    val settings = requireComponents.settings

    val isMicrosurveyVisible = isMicrosurveyPromptVisible()
    val isToolbarAtBottom = settings.toolbarPosition == ToolbarPosition.BOTTOM
    val isNavBarEnabled = settings.shouldUseExpandedToolbar && isTallWindow() && !isWideWindow()
    val shouldShowTabStrip = includeTabStripIfAvailable && settings.shouldShowTabStripAtBottom
    val shouldShowTabGroupsStrip = includeTabGroupsStrip && settings.shouldShowTabGroupsStrip
    val navBarDimen = if (isToolbarAtBottom) R.dimen.browser_navbar_height_small else R.dimen.browser_navbar_height

    val microsurveyHeight =
        if (isMicrosurveyVisible) {
            pixelSizeFor(R.dimen.browser_microsurvey_height)
        } else {
            0
        }

    val toolbarHeight =
        if (isToolbarAtBottom) {
            settings.getBrowserToolbarHeight(requireContext())
        } else {
            0
        }

    val tabstripHeight =
        when (shouldShowTabStrip) {
            true -> pixelSizeFor(R.dimen.tab_strip_height)
            else -> 0
        }

    val navBarHeight =
        if (includeNavBarIfEnabled && isNavBarEnabled) {
            pixelSizeFor(navBarDimen)
        } else {
            0
        }

    val tabGroupsStripHeight =
        when (shouldShowTabGroupsStrip) {
            true -> pixelSizeFor(R.dimen.tab_groups_strip_height)
            else -> 0
        }

    return microsurveyHeight + toolbarHeight + navBarHeight + tabstripHeight + tabGroupsStripHeight
}

/**
 * Returns the height of the top toolbar.
 *
 * @param includeTabStripIfAvailable If true and the tab strip feature is enabled it's height will be included in the
 *   calculation.
 */
internal fun BaseBrowserFragment.getTopToolbarHeight(includeTabStripIfAvailable: Boolean = true): Int {
    val settings = requireComponents.settings
    val isToolbarAtTop = settings.toolbarPosition == ToolbarPosition.TOP
    val shouldShowTabStrip = includeTabStripIfAvailable && settings.shouldShowTabStripAtTop

    val toolbarHeight =
        when (isToolbarAtTop) {
            true -> settings.getBrowserToolbarHeight(requireContext())
            else -> 0
        }

    val tabStripHeight =
        when (shouldShowTabStrip) {
            true -> pixelSizeFor(R.dimen.tab_strip_height)
            else -> 0
        }

    return toolbarHeight + tabStripHeight
}
