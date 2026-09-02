/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser.store

import android.content.Context
import android.view.Gravity
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.FragmentManager
import mozilla.components.concept.base.crash.Breadcrumb
import mozilla.components.feature.downloads.ui.DownloadCancelDialogFragment
import mozilla.components.lib.crash.CrashReporter
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.Store
import mozilla.components.support.ktx.android.content.pixelSizeFor
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.store.BrowserScreenAction.CancelPrivateDownloadsOnPrivateTabsClosedAccepted
import org.mozilla.fenix.browser.store.BrowserScreenAction.ClosingLastPrivateTab
import org.mozilla.fenix.theme.ThemeManager

/**
 * [Middleware] responsible for handling actions related to the browser screen.
 *
 * @param uiContext [Context] used for various system interactions.
 * @param crashReporter [CrashReporter] for recording crashes.
 * @param fragmentManager [FragmentManager] to use for showing other fragments.
 */
class BrowserScreenMiddleware(
    private val uiContext: Context,
    private val crashReporter: CrashReporter,
    private val fragmentManager: FragmentManager,
) : Middleware<BrowserScreenState, BrowserScreenAction> {

    override fun invoke(
        store: Store<BrowserScreenState, BrowserScreenAction>,
        next: (BrowserScreenAction) -> Unit,
        action: BrowserScreenAction,
    ) {
        when (action) {
            is ClosingLastPrivateTab -> {
                next(action)

                showCancelledDownloadWarning(
                    store = store,
                    downloadCount = action.inProgressPrivateDownloads,
                    tabId = action.tabId,
                )
            }

            else -> next(action)
        }
    }

    private fun showCancelledDownloadWarning(
        store: Store<BrowserScreenState, BrowserScreenAction>,
        downloadCount: Int,
        tabId: String?,
    ) {
        crashReporter.recordCrashBreadcrumb(
            Breadcrumb("DownloadCancelDialogFragment shown in browser screen"),
        )
        val dialog = createDownloadCancelDialog(uiContext, store, downloadCount, tabId)

        dialog.show(fragmentManager, CANCEL_PRIVATE_DOWNLOADS_DIALOG_FRAGMENT_TAG)
    }

    /**
     * Creates and configures a new instance of [DownloadCancelDialogFragment].
     */
    @VisibleForTesting
    internal fun createDownloadCancelDialog(
        context: Context,
        store: Store<BrowserScreenState, BrowserScreenAction>,
        downloadCount: Int,
        tabId: String?,
    ): DownloadCancelDialogFragment {
        return DownloadCancelDialogFragment.newInstance(
            downloadCount = downloadCount,
            tabId = tabId,
            source = null,
            promptStyling = createDialogPromptStyling(context),
            onPositiveButtonClicked = { _, _ ->
                store.dispatch(CancelPrivateDownloadsOnPrivateTabsClosedAccepted)
            },
        )
    }

    /**
     * Creates the specific styling configuration for the download cancellation prompt.
     */
    fun createDialogPromptStyling(context: Context): DownloadCancelDialogFragment.PromptStyling {
        return DownloadCancelDialogFragment.PromptStyling(
            gravity = Gravity.BOTTOM,
            shouldWidthMatchParent = true,
            positiveButtonBackgroundColor = ThemeManager.resolveAttribute(
                R.attr.accent,
                context,
            ),
            positiveButtonTextColor = ThemeManager.resolveAttribute(
                R.attr.textOnColorPrimary,
                context,
            ),
            positiveButtonRadius = context.pixelSizeFor(
                R.dimen.tab_corner_radius,
            ).toFloat(),
        )
    }

    /**
     * Static functionalities of the [BrowserScreenMiddleware].
     */
    companion object {
        @VisibleForTesting
        internal const val CANCEL_PRIVATE_DOWNLOADS_DIALOG_FRAGMENT_TAG = "CANCEL_PRIVATE_DOWNLOADS_DIALOG_FRAGMENT_TAG"
    }
}
