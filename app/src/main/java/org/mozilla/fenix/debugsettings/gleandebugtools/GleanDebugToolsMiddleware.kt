/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.debugsettings.gleandebugtools

import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.Store
import mozilla.components.support.ktx.kotlin.urlEncode
import mozilla.components.support.utils.ClipboardHandler

internal const val PING_PREVIEW_URL = "https://debug-ping-preview.firebaseapp.com/"

/**
 * [Middleware] that reacts to various [GleanDebugToolsAction]s.
 *
 * @param gleanDebugToolsStorage [GleanDebugToolsStorage] used to dispatch calls to the Glean API.
 * @param clipboardHandler [ClipboardHandler] used to add the debug view link to the clipboard.
 * @param openDebugView Invoked when the user clicks on the open debug view button.
 * @param showToast Invoked when the user sends a test ping.
 */
class GleanDebugToolsMiddleware(
    private val gleanDebugToolsStorage: GleanDebugToolsStorage,
    private val clipboardHandler: ClipboardHandler,
    private val openDebugView: (String) -> Unit,
    private val showToast: (String) -> Unit,
) : Middleware<GleanDebugToolsState, GleanDebugToolsAction> {
    override fun invoke(
        store: Store<GleanDebugToolsState, GleanDebugToolsAction>,
        next: (GleanDebugToolsAction) -> Unit,
        action: GleanDebugToolsAction,
    ) {
        next(action)
        when (action) {
            is GleanDebugToolsAction.LogPingsToConsoleToggled -> {
                gleanDebugToolsStorage.setLogPings(store.state.logPingsToConsoleEnabled)
            }
            is GleanDebugToolsAction.OpenDebugView -> {
                val debugViewLink = getDebugViewLink(
                    debugViewTag = store.state.debugViewTag,
                    useDebugViewTag = action.useDebugViewTag,
                )
                openDebugView(debugViewLink)
            }
            is GleanDebugToolsAction.CopyDebugViewLink -> {
                val debugViewLink = getDebugViewLink(
                    debugViewTag = store.state.debugViewTag,
                    useDebugViewTag = action.useDebugViewTag,
                )
                clipboardHandler.text = debugViewLink
            }
            is GleanDebugToolsAction.DebugViewTagChanged -> {
                // A tag can only be persisted through the adb persistDebugViewTag intent,
                // so changing it here should also clear this persisted tag.
                gleanDebugToolsStorage.clearPersistedDebugViewTag()
            }
            is GleanDebugToolsAction.SendPing -> {
                gleanDebugToolsStorage.sendPing(
                    pingType = store.state.pingType,
                    debugViewTag = store.state.debugViewTag,
                )
                showToast(store.state.pingType)
            }
            is GleanDebugToolsAction.ChangePingType -> {} // No-op
        }
    }

    /**
     * Get the debug view link for the given debug view tag.
     *
     * @param debugViewTag The debug view tag to use to get the link to the debug view.
     * @param useDebugViewTag Whether to use the given debug view tag to get the debug view link.
     */
    private fun getDebugViewLink(debugViewTag: String, useDebugViewTag: Boolean): String =
        if (useDebugViewTag) {
            "${PING_PREVIEW_URL}pings/${debugViewTag.urlEncode()}"
        } else {
            PING_PREVIEW_URL
        }
}
