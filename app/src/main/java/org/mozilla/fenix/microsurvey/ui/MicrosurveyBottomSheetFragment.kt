/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.microsurvey.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.compose.content
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import org.mozilla.fenix.ext.openToBrowser
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.messaging.MicrosurveyMessageController
import org.mozilla.fenix.microsurvey.ui.ext.MicrosurveyUIData
import org.mozilla.fenix.microsurvey.ui.ext.toMicrosurveyUIData
import org.mozilla.fenix.theme.FirefoxTheme
import com.google.android.material.R as materialR

/**
 * A bottom sheet fragment for displaying a microsurvey.
 */
class MicrosurveyBottomSheetFragment : BottomSheetDialogFragment() {

    private val args by navArgs<MicrosurveyBottomSheetFragmentArgs>()

    private val microsurveyMessageController by lazy {
        MicrosurveyMessageController(
            appStore = requireComponents.appStore,
            openUrlInBrowser = { url ->
                findNavController().openToBrowser()
                requireComponents.useCases.fenixBrowserUseCases.loadUrlOrSearch(
                    searchTermOrURL = url,
                    newTab = true,
                )
            },
        )
    }

    private var microsurveyUIData by mutableStateOf<MicrosurveyUIData?>(null)

    private val closeBottomSheet = { dismiss() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val messaging = requireComponents.nimbus.messaging
        val microsurveyId = args.microsurveyId

        lifecycleScope.launch {
            microsurveyUIData = messaging.getMessage(microsurveyId)?.toMicrosurveyUIData()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).apply {
            setOnShowListener {
                val bottomSheet = findViewById<View?>(materialR.id.design_bottom_sheet)
                bottomSheet?.let {
                    it.setBackgroundResource(android.R.color.transparent)
                    val behavior = BottomSheetBehavior.from(it)
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        FirefoxTheme {
            microsurveyUIData?.let {
                LaunchedEffect(it.id) {
                    microsurveyMessageController.onMicrosurveyShown(it.id)
                }
                MicrosurveyBottomSheet(
                    question = it.question,
                    icon = it.icon,
                    answers = it.answers,
                    maxLabelLines = it.maxNumberLines,
                    onPrivacyPolicyLinkClick = {
                        closeBottomSheet()
                        microsurveyMessageController.onPrivacyPolicyLinkClicked(
                            it.id,
                            it.utmContent,
                        )
                    },
                    onCloseButtonClicked = {
                        microsurveyMessageController.onMicrosurveyDismissed(it.id)
                        requireComponents.settings.shouldShowMicrosurveyPrompt = false
                        closeBottomSheet()
                    },
                    onSubmitButtonClicked = { answer ->
                        requireComponents.settings.shouldShowMicrosurveyPrompt = false
                        microsurveyMessageController.onSurveyCompleted(it.id, answer)
                    },
                )
            }
        }
    }
}
