/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

object ShareOverlaySelectors {

    // requiredForPage: this is the share sheet's root layout (fragment_share.xml), so it is what tells
    // the framework we actually arrived. Without it the page had no presence anchor at all and
    // navigateToPage() reported success while still sitting on the browser.
    val SHARING_LAYOUT = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "sharingLayout",
        description = "Share overlay layout",
        groups = listOf("requiredForPage", "shareTabLayout"),
    )

    val DEVICES_LIST = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "devicesList",
        description = "Send to device section",
        groups = listOf("shareTabLayout"),
    )

    val RECENT_APPS_CONTAINER = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "recentAppsContainer",
        description = "Recently used apps section",
        groups = listOf("shareTabLayout"),
    )

    val APPS_LIST = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "appsList",
        description = "All apps section",
        groups = listOf("shareTabLayout"),
    )

    val SEND_TO_DEVICE_HEADER = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = getStringResource(R.string.share_device_subheader),
        description = "Send to device header",
        groups = listOf("shareTabLayout"),
    )

    val RECENTLY_USED_HEADER = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = getStringResource(R.string.share_link_recent_apps_subheader),
        description = "Recently used apps header",
        groups = listOf("shareTabLayout"),
    )

    val ALL_ACTIONS_HEADER = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = getStringResource(R.string.share_link_all_apps_subheader),
        description = "All actions header",
        groups = listOf("shareTabLayout"),
    )

    val SAVE_AS_PDF_LABEL = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = getStringResource(R.string.share_save_to_pdf),
        description = "Save as PDF label",
        groups = listOf("shareTabLayout"),
    )

    val all = listOf(
        SHARING_LAYOUT,
        DEVICES_LIST,
        RECENT_APPS_CONTAINER,
        APPS_LIST,
        SEND_TO_DEVICE_HEADER,
        RECENTLY_USED_HEADER,
        ALL_ACTIONS_HEADER,
        SAVE_AS_PDF_LABEL,
    )
}
