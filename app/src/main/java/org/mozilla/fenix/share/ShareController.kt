/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.share

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.EXTRA_SUBJECT
import android.content.Intent.EXTRA_TEXT
import android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import mozilla.components.concept.engine.prompt.ShareData
import mozilla.components.concept.sync.Device
import mozilla.components.concept.sync.FxAEntryPoint
import mozilla.components.concept.sync.TabData
import mozilla.components.concept.sync.TabPrivacy
import mozilla.components.feature.accounts.push.SendTabUseCases
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.share.RecentAppsStorage
import mozilla.components.support.ktx.kotlin.isExtensionUrl
import mozilla.telemetry.glean.private.NoExtras
import org.mozilla.fenix.GleanMetrics.Events
import org.mozilla.fenix.GleanMetrics.SyncAccount
import org.mozilla.fenix.R
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.accounts.FenixFxAEntryPoint
import org.mozilla.fenix.components.appstate.AppAction.ShareAction
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.share.listadapters.AppShareOption

/**
 * [ShareFragment] controller.
 *
 * Delegated by View Interactors, handles container business logic and operates changes on it.
 */
interface ShareController {
    fun handleReauth()

    fun handleShareClosed()

    fun handleShareToApp(app: AppShareOption)

    /** Handles when a save to PDF action was requested. */
    fun handleSaveToPDF(tabId: String?)

    fun handleAddNewDevice()

    fun handleShareToDevice(device: Device)

    fun handleShareToAllDevices(devices: List<Device>)

    fun handleSignIn()

    /** Handles when a print action was requested. */
    fun handlePrint(tabId: String?)

    enum class Result {
        DISMISSED,
        SHARE_ERROR,
        SUCCESS,
    }
}

/**
 * Default behavior of [ShareController]. Other implementations are possible.
 *
 * @param context [Context] used for various Android interactions.
 * @param appStore Instance of [AppStore] for interacting with application wide state.
 * @param shareSubject Desired message subject used when sharing through 3rd party apps, like email clients.
 * @param shareData The list of [ShareData]s that can be shared.
 * @param sendTabUseCases Instance of [SendTabUseCases] which allows sending tabs to account devices.
 * @param saveToPdfUseCase Instance of [SessionUseCases.SaveToPdfUseCase] to generate a PDF of a given tab.
 * @param printUseCase Instance of [SessionUseCases.PrintContentUseCase] to print content of a given tab.
 * @param sentFromFirefoxManager Instance of [SentFromFirefoxManager] to facilitate "Sent from" nimbus experiment.
 * @param navController [NavController] used for navigation.
 * @param recentAppsStorage Instance of [RecentAppsStorage] for storing and retrieving the most recent apps.
 * @param scope [CoroutineScope] used for retrieving the most recent apps in the background.
 * @param applicationScope [CoroutineScope] tied to the application lifetime, used for operations that must outlive the
 *   share fragment (e.g. sending tabs to devices after the fragment is dismissed).
 * @param fxaEntrypoint The entrypoint if we need to authenticate, it will be reported in telemetry.
 * @param dismiss Callback signalling sharing can be closed.
 */
@Suppress("TooManyFunctions", "LongParameterList")
class DefaultShareController(
    private val context: Context,
    private val appStore: AppStore,
    private val shareSubject: String?,
    private val shareData: List<ShareData>,
    private val sendTabUseCases: SendTabUseCases,
    private val saveToPdfUseCase: SessionUseCases.SaveToPdfUseCase,
    private val printUseCase: SessionUseCases.PrintContentUseCase,
    private val sentFromFirefoxManager: SentFromFirefoxManager,
    private val navController: NavController,
    private val recentAppsStorage: RecentAppsStorage,
    private val scope: CoroutineScope,
    private val applicationScope: CoroutineScope,
    private val fxaEntrypoint: FxAEntryPoint = FenixFxAEntryPoint.ShareMenu,
    private val dismiss: (ShareController.Result) -> Unit,
) : ShareController {

    override fun handleReauth() {
        val directions =
            ShareFragmentDirections.actionGlobalAccountProblemFragment(entrypoint = fxaEntrypoint as FenixFxAEntryPoint)
        navController.nav(R.id.shareFragment, directions)
        dismiss(ShareController.Result.DISMISSED)
    }

    override fun handleShareClosed() {
        dismiss(ShareController.Result.DISMISSED)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun handleShareToApp(app: AppShareOption) {
        Events.shareToApp.record(
            getShareToAppSafeExtra(
                appPackage = app.packageName,
                sentFromFirefoxEnabled = sentFromFirefoxManager.featureEnabled,
            )
        )
        if (app.packageName == ACTION_COPY_LINK_TO_CLIPBOARD) {
            copyClipboard()
            dismiss(ShareController.Result.SUCCESS)

            return
        }

        scope.launch {
            recentAppsStorage.updateRecentApp(app.activityName)
        }

        val intent =
            Intent(ACTION_SEND).apply {
                val sharedText =
                    sentFromFirefoxManager.maybeAppendShareText(
                        packageName = app.packageName,
                        shareText = getShareText(),
                    )
                putExtra(EXTRA_TEXT, sharedText)
                putExtra(EXTRA_SUBJECT, getShareSubject())
                type = "text/plain"
                flags = FLAG_ACTIVITY_NEW_DOCUMENT + FLAG_ACTIVITY_MULTIPLE_TASK
                setClassName(app.packageName, app.activityName)
            }

        @Suppress("TooGenericExceptionCaught")
        val result =
            try {
                context.startActivity(intent)
                ShareController.Result.SUCCESS
            } catch (e: Exception) {
                when (e) {
                    is SecurityException,
                    is ActivityNotFoundException -> {
                        appStore.dispatch(ShareAction.ShareToAppFailed)

                        ShareController.Result.SHARE_ERROR
                    }
                    else -> throw e
                }
            }
        dismiss(result)
    }

    override fun handleSaveToPDF(tabId: String?) {
        handleShareClosed()
        saveToPdfUseCase.invoke(tabId)
    }

    override fun handlePrint(tabId: String?) {
        Events.shareMenuAction.record(Events.ShareMenuActionExtra("print"))
        handleShareClosed()
        printUseCase.invoke(tabId)
    }

    override fun handleAddNewDevice() {
        val directions = ShareFragmentDirections.actionShareFragmentToAddNewDeviceFragment()
        navController.navigate(directions)
    }

    override fun handleShareToDevice(device: Device) {
        SyncAccount.sendTab.record(NoExtras())
        shareToDevicesWithRetry(listOf(device.id)) {
            sendTabUseCases.sendToDeviceAsync(device.id, shareData.toTabData())
        }
    }

    override fun handleShareToAllDevices(devices: List<Device>) {
        shareToDevicesWithRetry(devices.map { it.id }) { sendTabUseCases.sendToAllAsync(shareData.toTabData()) }
    }

    override fun handleSignIn() {
        SyncAccount.signInToSendTab.record(NoExtras())
        val directions =
            ShareFragmentDirections.actionGlobalTurnOnSync(entrypoint = fxaEntrypoint as FenixFxAEntryPoint)
        navController.nav(R.id.shareFragment, directions)
        dismiss(ShareController.Result.DISMISSED)
    }

    /**
     * Share tabs with other devices linked to user's account.
     *
     * @param destination List of device IDs to share tabs with.
     * @param shareOperation Operation to be executed for actually sharing tabs.
     */
    private fun shareToDevicesWithRetry(
        destination: List<String>,
        shareOperation: () -> Deferred<Boolean>,
    ) {
        applicationScope.launch {
            val result =
                if (shareOperation.invoke().await()) {
                    showSuccess(destination)
                    ShareController.Result.SUCCESS
                } else {
                    showFailureWithRetryOption(destination)
                    ShareController.Result.DISMISSED
                }
            if (navController.currentDestination?.id == R.id.shareFragment) {
                dismiss(result)
            }
        }
    }

    /**
     * Inform about the successful sharing of tabs.
     *
     * @param destination List of device IDs to which tabs were shared.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun showSuccess(destination: List<String>) {
        appStore.dispatch(ShareAction.SharedTabsSuccessfully(destination, shareData.toTabData()))
    }

    /**
     * Inform about the failure of sharing tabs.
     *
     * @param destination List of device IDs to which tabs were tried to be shared.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun showFailureWithRetryOption(destination: List<String>) {
        appStore.dispatch(ShareAction.ShareTabsFailed(destination, shareData.toTabData()))
    }

    @VisibleForTesting
    fun getShareText() =
        shareData.joinToString("\n\n") { data ->
            val url = data.url.orEmpty()
            val text = data.text.orEmpty()
            val shareUrl =
                if (url.isExtensionUrl()) {
                    // Sharing moz-extension:// URLs is not practical in general, as
                    // they will only work on the current device.

                    // We solve this for URLs from our reader extension as they contain
                    // the original URL as a query parameter. This is a workaround for
                    // now and needs a clean fix once we have a reader specific protocol
                    // e.g. ext+reader://
                    // https://github.com/mozilla-mobile/android-components/issues/2879
                    url.toUri().getQueryParameter("url") ?: url
                } else {
                    url
                }
            listOfNotNull(text, shareUrl).filter { it.isNotEmpty() }.joinToString("\n")
        }

    @VisibleForTesting
    internal fun getShareSubject() =
        shareSubject ?: shareData.filterNot { it.title.isNullOrEmpty() }.joinToString(", ") { it.title.toString() }

    @SuppressLint("InlinedApi")
    private fun copyClipboard() {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(getShareSubject(), getShareText())

        val isPrivate = shareData.any { it.private }

        if (isPrivate) {
            clipData.description.extras =
                PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
        }

        clipboardManager.setPrimaryClip(clipData)
    }

    companion object {
        const val ACTION_COPY_LINK_TO_CLIPBOARD = "org.mozilla.fenix.COPY_LINK_TO_CLIPBOARD"
    }
}

// Navigation between app fragments uses ShareTab as arguments. SendTabUseCases uses TabData.
@VisibleForTesting
internal fun List<ShareData>.toTabData() = map { data ->
    fun String.toDataUri(): String {
        return "data:,${Uri.encode(this)}"
    }

    TabData(
        title = data.title.orEmpty(),
        url = data.url ?: data.text?.toDataUri().orEmpty(),
        privacy = if (data.private) TabPrivacy.Private else TabPrivacy.Normal,
    )
}
