/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.pbmlock

import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.launch
import mozilla.components.browser.state.selector.normalTabs
import mozilla.components.feature.customtabs.isCustomTabIntent
import mozilla.components.support.base.feature.UserInteractionHandler
import org.mozilla.fenix.GleanMetrics.PrivateBrowsingLocked
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.e2e.SystemInsetsPaddedFragment
import org.mozilla.fenix.ext.registerForActivityResult
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.home.HomeFragmentDirections
import org.mozilla.fenix.settings.biometric.DefaultBiometricUtils
import org.mozilla.fenix.tabstray.redux.state.Page
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * Fragment used to display biometric authentication when the app is locked.
 */
class UnlockPrivateTabsFragment : Fragment(), UserInteractionHandler, SystemInsetsPaddedFragment {
    private lateinit var startForResult: ActivityResultLauncher<Intent>
    private val args: UnlockPrivateTabsFragmentArgs by navArgs()
    private val navigationOrigin by lazy { args.navigationOrigin }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        startForResult = registerForActivityResult(
            onSuccess = { onAuthSuccess() },
            onFailure = { onAuthFailure() },
        )

        return ComposeView(requireContext()).apply {
            isTransitionGroup = true
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        PrivateBrowsingLocked.promptShown.record()

        val appStore = requireComponents.appStore
        val isCustomPrivateTab =
            isCustomTabIntent(requireActivity().intent) && appStore.state.mode.isPrivate

        (view as ComposeView).setContent {
            FirefoxTheme {
                UnlockPrivateTabsScreen(
                    onUnlockClicked = { requestPrompt() },
                    onLeaveClicked = {
                        PrivateBrowsingLocked.seeOtherTabsClicked.record()
                        closeFragment()
                    },
                    showNegativeButton = !isCustomPrivateTab,
                )
            }
        }
        maybeRequestPrompt()
    }

    override fun onBackPressed(): Boolean {
        closeFragment()
        return true
    }

    private fun maybeRequestPrompt() {
        // Delay the prompt until this fragment is resumed to ensure that `BiometricPromptFeature`
        // is wiring up the system `BiometricPrompt` to the right (currently active) fragment.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.withResumed {
                // This check handles an edge case related to the screen saver.
                // When a screen saver is used in combination with a screen lock, cancelling a screen saver
                // (in some cases) resumes the app briefly, although the screen lock is active. Which, in
                // this case, allows the app to trigger system windows above the screen lock.
                val manager = requireContext().getSystemService<KeyguardManager>()
                val screenLocked = manager?.isDeviceLocked ?: false
                if (!screenLocked) {
                    requestPrompt()
                }
            }
        }
    }

    private fun requestPrompt() {
        DefaultBiometricUtils.bindBiometricsCredentialsPromptOrShowWarning(
            titleRes = R.string.pbm_authentication_unlock_private_tabs,
            view = requireView(),
            onShowPinVerification = { intent -> startForResult.launch(intent) },
            onAuthSuccess = ::onAuthSuccess,
            onAuthFailure = ::onAuthFailure,
        )
    }

    /**
     * If the users decides to leave the fragment, we want to navigate them to normal tabs page.
     * If they don't have regular opened tabs, we navigate back to homepage as a fallback.
     */
    private fun closeFragment() {
        when (navigationOrigin) {
            // If we locked private mode while the user had a private tab opened or was on private
            // home page, then, when leaving without authentication, we want to navigate user back
            // to normal mode. If they have opened regular tabs, we open the tabs tray as well.
            NavigationOrigin.TAB, NavigationOrigin.HOME_PAGE -> {
                (activity as HomeActivity).browsingModeManager.mode = BrowsingMode.Normal
                findNavController().navigate(UnlockPrivateTabsFragmentDirections.actionGlobalHome())

                val hasNormalTabs = requireComponents.core.store.state.normalTabs.isNotEmpty()
                if (hasNormalTabs) {
                    findNavController().navigate(
                        HomeFragmentDirections.actionGlobalTabManagementFragment(
                            page = Page.NormalTabs,
                        ),
                    )
                }
            }
            // If we locked private mode while the user had the tabs tray private page opened, then
            // going to the previous state means just closing the fragment and defaulting to tabs
            // tray regular page
            NavigationOrigin.TABS_TRAY -> {
                findNavController().popBackStack()

                findNavController().navigate(
                    HomeFragmentDirections.actionGlobalTabManagementFragment(
                        page = Page.NormalTabs,
                    ),
                )
            }
            // If private mode is locked and the user opens a private custom tab,
            // close the activity when they leave without authenticating to return to the host app.
            NavigationOrigin.CUSTOM_TAB -> (activity as HomeActivity).finish()
        }
    }

    private fun onAuthSuccess() {
        PrivateBrowsingLocked.authSuccess.record()

        requireComponents.useCases.privateBrowsingLockUseCases.authenticatedUseCase()

        findNavController().popBackStack()

        if (navigationOrigin == NavigationOrigin.TABS_TRAY) {
            findNavController().navigate(
                HomeFragmentDirections.actionGlobalTabManagementFragment(
                    page = Page.PrivateTabs,
                ),
            )
        }
    }

    private fun onAuthFailure() {
        PrivateBrowsingLocked.authFailure.record()
    }
}

/**
 * The navigation entry point from which the fragment was triggered.
 *
 * It helps determine the correct navigation behaviour when the user is leaving the fragment.
 */
enum class NavigationOrigin {
    TABS_TRAY,
    HOME_PAGE,
    TAB,
    CUSTOM_TAB,
}
