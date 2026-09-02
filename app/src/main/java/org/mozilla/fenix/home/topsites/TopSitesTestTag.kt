/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.topsites

internal object TopSitesTestTag {
    const val TOP_SITES = "top_sites_list"

    const val TOP_SITE_ITEM_ROOT = "$TOP_SITES.top_site_item"
    const val TOP_SITE_TITLE = "$TOP_SITE_ITEM_ROOT.top_site_title"
    const val TOP_SITE_CARD_FAVICON = "$TOP_SITE_ITEM_ROOT.top_site_card_favicon"

    const val ADD_SHORTCUT_ROOT = "$TOP_SITES.add_shortcut_item"
    const val ADD_SHORTCUT_TITLE = "$ADD_SHORTCUT_ROOT.add_shortcut_title"

    const val ADD_WEBSITE = "$TOP_SITES.add_website"

    const val TOP_SITES_PAGER_INDICATOR = "$TOP_SITES.pager_indicator"

    // Contextual/DropDown menu
    const val TOP_SITE_CONTEXTUAL_MENU = "$TOP_SITES.top_site_contextual_menu"
    const val OPEN_IN_PRIVATE_TAB = "$TOP_SITE_CONTEXTUAL_MENU.open_in_private_tab"
    const val EDIT = "$TOP_SITE_CONTEXTUAL_MENU.edit"
    const val REMOVE = "$TOP_SITE_CONTEXTUAL_MENU.remove"
}
