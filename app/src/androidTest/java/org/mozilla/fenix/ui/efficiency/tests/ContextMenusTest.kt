/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import mozilla.components.feature.contextmenu.R as contextMenuR
import org.junit.Test
import org.mozilla.fenix.customannotations.Critical
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.helpers.TestAssetHelper.imageAsset
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.selectors.BrowserPageSelectors
import org.mozilla.fenix.ui.efficiency.selectors.TabDrawerSelectors

class ContextMenusTest : BaseTest() {

    private val openLinkInNewTab = getStringResource(contextMenuR.string.mozac_feature_contextmenu_open_link_in_new_tab)
    private val openImageInNewTab =
        getStringResource(contextMenuR.string.mozac_feature_contextmenu_open_image_in_new_tab)

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/243837
    @Critical
    @Test
    fun verifyOpenLinkNewTabContextMenuOptionTest() {
        val pageLinks = mockWebServer.getGenericAsset(4)
        val genericURL = mockWebServer.getGenericAsset(1)

        on.browserPage
            .navigateToPage(pageLinks.url.toString())
            .longClickPageObjectUntilContextMenu("Link 1", openLinkInNewTab)
            .verifyContextMenuForLocalHostLinks(genericURL.url.toString())
            .mozClick(BrowserPageSelectors.CONTEXT_MENU_ITEM(openLinkInNewTab))

        on.browserPage.verifySnackbarText("New tab opened").mozClick(BrowserPageSelectors.SNACKBAR_ACTION_BUTTON)

        on.browserPage.verifyUrl(genericURL.url.toString())

        on.tabDrawer
            .navigateToPage()
            .mozVerifyElementIsSelected(TabDrawerSelectors.NORMAL_BROWSING_OPEN_TABS_BUTTON)
            .mozVerify(TabDrawerSelectors.TAB_ITEM_WITH_TITLE(pageLinks.title))
            .mozVerify(TabDrawerSelectors.TAB_ITEM_WITH_TITLE(genericURL.title))
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/352050
    @Critical
    @Test
    fun verifyContextMenuLinkVariationsTest() {
        val pageLinks = mockWebServer.getGenericAsset(4)
        val genericURL = mockWebServer.getGenericAsset(1)
        val imageResource = mockWebServer.imageAsset

        on.browserPage
            .navigateToPage(pageLinks.url.toString())
            .longClickPageObjectUntilContextMenu("Link 1", openLinkInNewTab)
            .verifyContextMenuForLocalHostLinks(genericURL.url.toString())
            .dismissContextMenuByTappingOutside()
            .longClickPageObjectUntilContextMenu("test_link_image", openLinkInNewTab)
            .verifyLinkImageContextMenuItems(imageResource.url.toString())
            .dismissContextMenuByTappingOutside()
            .longClickPageObjectUntilContextMenu("test_no_link_image", openImageInNewTab)
            .verifyNoLinkImageContextMenuItems(imageResource.url.toString())
    }
}
