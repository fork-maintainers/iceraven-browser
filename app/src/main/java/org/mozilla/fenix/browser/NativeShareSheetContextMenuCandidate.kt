/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser

import android.content.Context
import android.view.View
import mozilla.components.browser.state.state.SessionState
import mozilla.components.concept.engine.prompt.ShareData
import mozilla.components.feature.contextmenu.ContextMenuCandidate
import mozilla.components.feature.contextmenu.ContextMenuCandidate.Companion.createAddContactCandidate
import mozilla.components.feature.contextmenu.ContextMenuCandidate.Companion.createCopyEmailAddressCandidate
import mozilla.components.feature.contextmenu.ContextMenuCandidate.Companion.createCopyImageCandidate
import mozilla.components.feature.contextmenu.ContextMenuCandidate.Companion.createCopyImageLocationCandidate
import mozilla.components.feature.contextmenu.ContextMenuCandidate.Companion.createCopyLinkCandidate
import mozilla.components.feature.contextmenu.ContextMenuCandidate.Companion.createCopyLinkTextCandidate
import mozilla.components.feature.contextmenu.ContextMenuCandidate.Companion.createDownloadLinkCandidate
import mozilla.components.feature.contextmenu.ContextMenuCandidate.Companion.createOpenImageInNewTabCandidate
import mozilla.components.feature.contextmenu.ContextMenuCandidate.Companion.createOpenInNewTabCandidate
import mozilla.components.feature.contextmenu.ContextMenuCandidate.Companion.createOpenInPrivateTabCandidate
import mozilla.components.feature.contextmenu.ContextMenuCandidate.Companion.createSaveImageCandidate
import mozilla.components.feature.contextmenu.ContextMenuCandidate.Companion.createSaveVideoAudioCandidate
import mozilla.components.feature.contextmenu.ContextMenuCandidate.Companion.createShareEmailAddressCandidate
import mozilla.components.feature.contextmenu.ContextMenuCandidate.Companion.createShareImageCandidate
import mozilla.components.feature.contextmenu.ContextMenuCandidate.Companion.createShareLinkCandidate
import mozilla.components.feature.contextmenu.ContextMenuUseCases
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.ui.widgets.DefaultSnackbarDelegate
import mozilla.components.ui.widgets.SnackbarDelegate
import org.mozilla.fenix.components.share.ShareSheetChooserAction
import org.mozilla.fenix.components.share.ShareSource
import org.mozilla.fenix.components.usecases.ShareUseCases

/**
 * Provides the context menu candidates for [BrowserFragment] when native share sheet is enabled.
 */
object NativeShareSheetContextMenuCandidate {

    /**
     * Returns the default list of context menu candidates for [BrowserFragment] when the native share sheet is enabled.
     */
    @Suppress("LongParameterList")
    fun defaultCandidates(
        context: Context,
        tabsUseCases: TabsUseCases,
        contextMenuUseCases: ContextMenuUseCases,
        shareUseCases: ShareUseCases,
        getShareItems: (String) -> List<ShareData>,
        snackBarParentView: View,
        snackbarDelegate: SnackbarDelegate = DefaultSnackbarDelegate(),
        downloadsLocation: () -> String,
        navigateToShareFragment: (currentTab: SessionState, hitTabUrl: String) -> Unit,
    ): List<ContextMenuCandidate> = listOf(
        createOpenInNewTabCandidate(
            context,
            tabsUseCases,
            snackBarParentView,
            snackbarDelegate,
        ),
        createOpenInPrivateTabCandidate(
            context,
            tabsUseCases,
            snackBarParentView,
            snackbarDelegate,
        ),
        createCopyLinkCandidate(context, snackBarParentView, snackbarDelegate),
        createCopyLinkTextCandidate(context, snackBarParentView, snackbarDelegate),
        createDownloadLinkCandidate(context, contextMenuUseCases, downloadsLocation),
        createShareLinkCandidate(
            context = context,
            shareUseCases = shareUseCases,
            shareItems = getShareItems,
            navigateToShareFragment = navigateToShareFragment,
        ),
        createShareImageCandidate(context, contextMenuUseCases),
        createOpenImageInNewTabCandidate(
            context,
            tabsUseCases,
            snackBarParentView,
            snackbarDelegate,
        ),
        createCopyImageCandidate(
            context,
            contextMenuUseCases,
        ),
        createSaveImageCandidate(context, contextMenuUseCases, downloadsLocation),
        createSaveVideoAudioCandidate(context, contextMenuUseCases, downloadsLocation),
        createCopyImageLocationCandidate(context, snackBarParentView, snackbarDelegate),
        createAddContactCandidate(context),
        createShareEmailAddressCandidate(context),
        createCopyEmailAddressCandidate(context, snackBarParentView, snackbarDelegate),
    )

    private fun createShareLinkCandidate(
        context: Context,
        shareUseCases: ShareUseCases,
        shareItems: (hitTab: String) -> List<ShareData>,
        navigateToShareFragment: (currentTab: SessionState, hitTabUrl: String) -> Unit,
    ) = createShareLinkCandidate(
        context = context,
        action = { currentTab, hitTab ->
            val url = hitTab.getUrl()
            shareUseCases.shareItems(
                items = shareItems(url),
                source = ShareSource.CONTEXT_MENU_LINK,
                subject = null,
                chooserActions = listOf(
                    ShareSheetChooserAction.SEND_TO_DEVICES,
                    ShareSheetChooserAction.QR_CODE,
                ),
                navigateToShareFragment = { navigateToShareFragment(currentTab, url) },
            )
        },
    )
}
