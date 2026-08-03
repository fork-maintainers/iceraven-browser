/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.lens

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.setFragmentResult
import androidx.fragment.compose.content
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.mozilla.fenix.theme.FirefoxTheme
import com.google.android.material.R as materialR

/**
 * [BottomSheetDialogFragment] wrapper for the compose [GoogleLensOptOutBottomSheet]. Reports the
 * user's choice to the host through [RESULT_REQUEST_KEY]; a swipe, back press, tap outside or
 * accessibility dismissal is reported as [ACTION_NOT_NOW].
 */
class GoogleLensOptOutBottomSheetFragment : BottomSheetDialogFragment() {

    private var reportedAction: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reportedAction = savedInstanceState?.getString(STATE_REPORTED_ACTION)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            // Expanded before the dialog is shown rather than from the show listener, so the very
            // first layout pass already offsets the sheet to its final height instead of settling
            // there from a collapsed peek afterwards.
            behavior.apply {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }
            setOnShowListener {
                // Transparent so the rounded surface drawn by the composable is what the user sees.
                val bottomSheet = findViewById<View?>(materialR.id.design_bottom_sheet)
                bottomSheet?.setBackgroundResource(android.R.color.transparent)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return content {
            FirefoxTheme {
                GoogleLensOptOutBottomSheet(
                    onRequestDismiss = { reportAction(ACTION_NOT_NOW) },
                    onTryItNowClick = { reportAction(ACTION_TRY_IT_NOW) },
                    onNotNowClick = { reportAction(ACTION_NOT_NOW) },
                    onSettingsClick = { reportAction(ACTION_SETTINGS) },
                )
            }
        }.apply {
            // `content` leaves the layout params unset, which makes the hosting FrameLayout default
            // to MATCH_PARENT and the sheet's fit-to-contents height ambiguous on the first pass.
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    private fun reportAction(action: String) {
        if (reportedAction != null) return
        reportedAction = action
        setFragmentResult(RESULT_REQUEST_KEY, Bundle().apply { putString(RESULT_ACTION, action) })
        dismissAllowingStateLoss()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_REPORTED_ACTION, reportedAction)
    }

    // Not onDismiss: that also fires from onDestroyView on a configuration change, which would
    // report a choice the user never made. onCancel only covers swipe, back press and tap outside.
    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        // None of those leave an explicit choice; the user has not opted in, so treat it the same
        // as "Not now".
        reportAction(ACTION_NOT_NOW)
    }

    companion object {
        const val RESULT_REQUEST_KEY = "google_lens_opt_out_result_key"
        const val RESULT_ACTION = "google_lens_opt_out_action"

        const val ACTION_TRY_IT_NOW = "try_it_now"
        const val ACTION_NOT_NOW = "not_now"
        const val ACTION_SETTINGS = "settings"

        const val TAG = "GoogleLensOptOutBottomSheet"

        private const val STATE_REPORTED_ACTION = "reported_action"
    }
}
