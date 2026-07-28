/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.share

import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.collectAsState
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import mozilla.components.concept.sync.AccountObserver
import mozilla.components.concept.sync.AuthType
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.concept.sync.TabData
import mozilla.components.concept.sync.TabPrivacy
import mozilla.components.feature.accounts.push.SendTabUseCases
import mozilla.components.feature.share.RecentAppsStorage
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.support.utils.ext.packageManagerCompatHelper
import mozilla.telemetry.glean.private.NoExtras
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.GleanMetrics.SyncAccount
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.share.DefaultShareController.Companion.ACTION_COPY_LINK_TO_CLIPBOARD
import org.mozilla.fenix.share.ShareViewModel
import org.mozilla.fenix.share.listadapters.AppShareOption
import org.mozilla.fenix.share.listadapters.SyncShareOption

/**
 * A [BottomSheetDialogFragment] that allows the user to send a tab to their other devices.
 */
class SendToDevicesDialogFragment : BottomSheetDialogFragment() {

    private val model: ShareViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = requireContext().applicationContext
                return ShareViewModel(
                    fxaAccountManager = requireComponents.backgroundServices.accountManager,
                    recentAppsStorage = RecentAppsStorage(app),
                    connectivityManager = app.getSystemService<ConnectivityManager>(),
                    packageManager = app.packageManager,
                    packageName = app.packageName,
                    getCopyApp = ::getCopyApp,
                    queryIntentActivitiesCompat = { intent ->
                        app.packageManagerCompatHelper.queryIntentActivitiesCompat(intent, 0)
                    },
                ) as T
            }
        }
    }

    private val sendTabUseCases by lazy {
        SendTabUseCases(requireComponents.backgroundServices.accountManager)
    }

    private var tabUrl: String? = null
    private var tabTitle: String? = null
    private var tabPrivacy: TabPrivacy = TabPrivacy.Normal
    private var hasNavigatedToSignIn = false

    private val accountObserver = object : AccountObserver {
        override fun onAuthenticated(account: OAuthAccount, authType: AuthType) {
            onAuthenticated()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        val uiState = model.uiState.collectAsState().value
        SendToDevicesContent(
            uiState = uiState,
            onDismiss = { dismiss() },
            onSendToDevice = { option: SyncShareOption.SingleDevice ->
                sendAndDismiss(
                    sendTabToDevices(option.device.id, tabUrl ?: "", tabTitle ?: "", tabPrivacy),
                )
            },
            onSendToAll = {
                sendAndDismiss(sendTabToAllDevices(tabUrl ?: "", tabTitle ?: "", tabPrivacy))
            },
        )
    }

    override fun onStart() {
        super.onStart()
        loadTabData(arguments)
        requireComponents.backgroundServices.accountManager.register(
            accountObserver,
            owner = this,
            autoPause = false,
        )
    }

    internal fun loadTabData(bundle: Bundle?) {
        tabUrl = bundle?.getString(EXTRA_URL)
        tabTitle = bundle?.getString(EXTRA_TITLE)
        tabPrivacy = if (bundle?.getString(EXTRA_PRIVACY) == PRIVACY_PRIVATE) {
            TabPrivacy.Private
        } else {
            TabPrivacy.Normal
        }
    }

    override fun onResume() {
        super.onResume()
        checkAuthAndNavigate(requireComponents.backgroundServices.accountManager)
    }

    internal fun onAuthenticated() {
        model.initDataLoad()
    }

    internal fun checkAuthAndNavigate(accountManager: FxaAccountManager) {
        if (accountManager.authenticatedAccount() != null) {
            onAuthenticated()
        } else if (!hasNavigatedToSignIn) {
            hasNavigatedToSignIn = true
            navigateToSignIn()
        }
    }

    internal fun navigateToSignIn() {
        SyncAccount.signInToSendTab.record(NoExtras())
        requireActivity().startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "${BuildConfig.DEEP_LINK_SCHEME}://turn_on_sync".toUri(),
            ).apply {
                setPackage(requireActivity().packageName)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
        )
    }

    private fun getCopyApp(): AppShareOption? {
        val copyIcon = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_share_clipboard)
        return copyIcon?.let {
            AppShareOption(
                requireContext().getString(R.string.share_copy_link_to_clipboard),
                it,
                ACTION_COPY_LINK_TO_CLIPBOARD,
                "",
            )
        }
    }

    private fun sendAndDismiss(deferred: Deferred<Boolean>) {
        lifecycleScope.launch {
            val success = deferred.await()
            val message = if (success) {
                R.string.sync_sent_tab_snackbar_2
            } else {
                R.string.sync_sent_tab_error_snackbar
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun sendTabToDevices(
        deviceId: String,
        url: String,
        title: String,
        privacy: TabPrivacy,
    ): Deferred<Boolean> {
        return sendTabUseCases.sendToDeviceAsync.invoke(
            deviceId = deviceId,
            tab = TabData(url = url, title = title, privacy = privacy),
        )
    }

    private fun sendTabToAllDevices(
        url: String,
        title: String,
        privacy: TabPrivacy,
    ): Deferred<Boolean> {
        return sendTabUseCases.sendToAllAsync.invoke(
            tab = TabData(url = url, title = title, privacy = privacy),
        )
    }

    companion object {
        const val TAG = "SendToDevicesDialogFragment"

        internal const val EXTRA_URL = "url"
        internal const val EXTRA_TITLE = "title"
        internal const val EXTRA_PRIVACY = "privacy"
        internal const val PRIVACY_PRIVATE = "PRIVATE"
        internal const val PRIVACY_NORMAL = "NORMAL"

        /**
         * Creates a new instance of [SendToDevicesDialogFragment] with the provided URL, title, and privacy status.
         * @param url The URL of the tab to be sent.
         * @param title The title of the tab to be sent (optional).
         * @param isPrivate Whether the tab is private or not.
         */
        fun newInstance(url: String, title: String?, isPrivate: Boolean) =
            SendToDevicesDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_URL, url)
                    putString(EXTRA_TITLE, title)
                    putString(EXTRA_PRIVACY, if (isPrivate) PRIVACY_PRIVATE else PRIVACY_NORMAL)
                }
            }
    }
}
