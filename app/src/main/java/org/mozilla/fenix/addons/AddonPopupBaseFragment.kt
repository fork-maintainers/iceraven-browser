/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.addons

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.action.CustomTabListAction
import mozilla.components.browser.state.state.CustomTabSessionState
import mozilla.components.browser.state.state.EngineState
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.browser.state.state.content.DownloadState.Status
import mozilla.components.browser.state.state.createCustomTab
import mozilla.components.concept.base.crash.Breadcrumb
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.prompt.PromptRequest
import mozilla.components.concept.engine.window.WindowRequest
import mozilla.components.concept.fetch.Response
import mozilla.components.feature.downloads.CurrentDownloadState
import mozilla.components.feature.downloads.DownloadsFeature
import mozilla.components.feature.downloads.NegativeActionCallback
import mozilla.components.feature.downloads.PositiveActionCallback
import mozilla.components.feature.downloads.manager.FetchDownloadManager
import mozilla.components.feature.prompts.PromptFeature
import mozilla.components.feature.prompts.file.AndroidPhotoPicker
import mozilla.components.support.base.feature.ActivityResultHandler
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import mozilla.components.support.utils.DefaultDownloadFileUtils
import mozilla.components.support.utils.DownloadFileUtils
import mozilla.components.support.utils.ext.pixelSizeFor
import org.mozilla.fenix.R
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.downloads.DownloadService
import org.mozilla.fenix.downloads.RenameAndChangeLocationDialogFragment
import org.mozilla.fenix.downloads.dialog.createDownloadAppDialog
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.nimbus.FxNimbus
import org.mozilla.fenix.settings.downloads.DownloadLocationManager
import org.mozilla.fenix.theme.ThemeManager
import mozilla.components.feature.downloads.R as downloadsR

/**
 * Provides shared functionality to our fragments for add-on settings and
 * browser/page action popups.
 */
abstract class AddonPopupBaseFragment :
    Fragment(),
    EngineSession.Observer,
    UserInteractionHandler,
    ActivityResultHandler {
    private val promptsFeature = ViewBoundFeatureWrapper<PromptFeature>()
    private val downloadsFeature = ViewBoundFeatureWrapper<DownloadsFeature>()

    protected var session: SessionState? = null
    protected var engineSession: EngineSession? = null
    private var canGoBack: Boolean = false
    private var downloadDialog: AlertDialog? = null

    // Registers a photo picker activity launcher in single-select mode.
    private val singleMediaPicker =
        AndroidPhotoPicker.singleMediaPicker(
            { this@AddonPopupBaseFragment },
            { promptsFeature.get() },
        )

    // Registers a photo picker activity launcher in multi-select mode.
    private val multipleMediaPicker =
        AndroidPhotoPicker.multipleMediaPicker(
            { this@AddonPopupBaseFragment },
            { promptsFeature.get() },
        )

    @Suppress("DEPRECATION", "LongMethod")
    // https://github.com/mozilla-mobile/fenix/issues/19920
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        session?.let {
            promptsFeature.set(
                feature = PromptFeature(
                    activity = requireActivity(),
                    store = requireComponents.core.store,
                    customTabId = it.id,
                    fragmentManager = parentFragmentManager,
                    fileUploadsDirCleaner = requireComponents.core.fileUploadsDirCleaner,
                    onNeedToRequestPermissions = { permissions ->
                        requestPermissions(permissions, REQUEST_CODE_PROMPT_PERMISSIONS)
                    },
                    isEmailMaskFeatureEnabled = { requireComponents.settings.isEmailMaskFeatureEnabled },
                    isSuggestEmailMaskEnabled = { requireComponents.settings.isEmailMaskSuggestionEnabled },
                    tabsUseCases = requireComponents.useCases.tabsUseCases,
                    androidPhotoPicker = AndroidPhotoPicker(
                        requireContext(),
                        singleMediaPicker,
                        multipleMediaPicker,
                    ),
                ),
                owner = this,
                view = view,
            )
            val downloadFileUtils: DownloadFileUtils = DefaultDownloadFileUtils(
                context = requireContext(),
                downloadLocation = {
                    DownloadLocationManager(
                        requireComponents.settings,
                        requireContext().contentResolver,
                    ).defaultLocation
                },
            )
            val downloadFeature = DownloadsFeature(
                requireContext().applicationContext,
                store = provideBrowserStore(),
                useCases = requireContext().components.useCases.downloadUseCases,
                downloadFileUtils = downloadFileUtils,
                fragmentManager = childFragmentManager,
                tabId = it.id,
                downloadManager = FetchDownloadManager(
                    requireContext().applicationContext,
                    provideBrowserStore(),
                    DownloadService::class,
                    notificationsDelegate = requireContext().components.notificationsDelegate,
                ),
                shouldForwardToThirdParties = {
                    PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean(
                        requireContext().getPreferenceKey(R.string.pref_key_external_download_manager),
                        false,
                    )
                },
                promptsStyling = DownloadsFeature.PromptsStyling(
                    gravity = Gravity.BOTTOM,
                    shouldWidthMatchParent = true,
                    positiveButtonBackgroundColor = ThemeManager.resolveAttribute(
                        R.attr.accent,
                        requireContext(),
                    ),
                    positiveButtonTextColor = ThemeManager.resolveAttribute(
                        R.attr.textOnColorPrimary,
                        requireContext(),
                    ),
                    positiveButtonRadius = pixelSizeFor(R.dimen.tab_corner_radius).toFloat(),
                ),
                onDownloadStartedListener = { downloadId ->
                    requireComponents.appStore.dispatch(
                        AppAction.DownloadAction.DownloadInProgress(
                             downloadId,
                        ),
                    )
                },
                onNeedToRequestPermissions = { permissions ->
                    requestPermissions(permissions, REQUEST_CODE_DOWNLOAD_PERMISSIONS)
                },
                dismissCustomFirstPartyDownloadDialog = {
                    dismissRenameDialog()
                    downloadDialog?.dismiss()
                },
                customFirstPartyDownloadDialog = { currentDownloadState, _, positiveAction, negativeAction, _ ->
                    run {
                        if (canShowDownloadDialog()) {
                            if (!FxNimbus.features.downloadsCustomLocation.value().enabled) {
                                showFirstPartyDownloadDialog(
                                    currentDownloadState = currentDownloadState,
                                    positiveAction = positiveAction,
                                    negativeAction = negativeAction,
                                )
                            } else {
                                currentDownloadState.value.fileName?.let { fileName ->
                                    showRenameDownloadDialog(
                                        fileName = fileName,
                                        currentDownloadState = currentDownloadState,
                                        positiveAction = positiveAction,
                                        negativeAction = negativeAction,
                                    )
                                }
                            }
                        }
                    }
                },
                customThirdPartyDownloadDialog = { downloaderApps, onAppSelected, negativeActionCallback ->
                    run {
                        if (canShowDownloadDialog()) {
                            requireContext().components.analytics.crashReporter.recordCrashBreadcrumb(
                                Breadcrumb("DownloaderAppDialog created"),
                            )
                            downloadDialog = createDownloadAppDialog(
                                context = requireContext(),
                                downloaderApps = downloaderApps.value,
                                onAppSelected = onAppSelected.value,
                                onDismiss = {
                                    downloadDialog = null
                                    requireContext().components.analytics.crashReporter.recordCrashBreadcrumb(
                                        Breadcrumb("DownloaderAppDialog onDismiss"),
                                    )
                                },
                            )
                            downloadDialog?.show()
                        }
                    }
                },
            )
            downloadsFeature.set(
                downloadFeature,
                owner = this,
                view = view,
            )
            downloadFeature.onDownloadStopped = { downloadState, _, downloadJobStatus ->
                handleOnDownloadFinished(downloadState, downloadJobStatus, downloadFileUtils)
            }
        }
    }

    override fun onExternalResource(
        url: String,
        fileName: String?,
        contentLength: Long?,
        contentType: String?,
        cookie: String?,
        userAgent: String?,
        isPrivate: Boolean,
        skipConfirmation: Boolean,
        openInApp: Boolean,
        response: Response?,
    ) {
        session?.let { session ->
            val fileSize = if (contentLength != null && contentLength < 0) null else contentLength
            val download = DownloadState(
                url,
                fileName,
                contentType,
                fileSize,
                0,
                Status.INITIATED,
                userAgent,
                directoryPath = DownloadLocationManager(
                    requireComponents.settings,
                    requireContext().contentResolver,
                ).defaultLocation,
                private = isPrivate,
                skipConfirmation = skipConfirmation,
                openInApp = openInApp,
                response = response,
            )

            provideBrowserStore().dispatch(
                ContentAction.UpdateDownloadAction(
                    session.id,
                    download,
                ),
            )
        }
    }

    private fun provideBrowserStore() = requireComponents.core.store

    /**
     * Provides a container for dynamic snackbars.
     *
     * @return A ConstraintLayout that will contain the dynamic snackbars.
     */
    abstract fun provideDynamicSnackbarContainer(): ConstraintLayout

    override fun onDestroyView() {
        engineSession?.close()
        session?.let {
            requireComponents.core.store.dispatch(CustomTabListAction.RemoveCustomTabAction(it.id))
        }
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        engineSession?.register(this)
    }

    override fun onStop() {
        super.onStop()
        engineSession?.unregister(this)
        downloadDialog?.dismiss()
    }

    override fun onPromptRequest(promptRequest: PromptRequest) {
        session?.let { session ->
            requireComponents.core.store.dispatch(
                ContentAction.UpdatePromptRequestAction(
                    session.id,
                    promptRequest,
                ),
            )
        }
    }

    override fun onWindowRequest(windowRequest: WindowRequest) {
        if (windowRequest.type == WindowRequest.Type.CLOSE) {
            findNavController().popBackStack()
        } else {
            engineSession?.loadUrl(windowRequest.url)
        }
    }

    override fun onNavigationStateChange(canGoBack: Boolean?, canGoForward: Boolean?) {
        canGoBack?.let { this.canGoBack = canGoBack }
    }

    override fun onBackPressed(): Boolean {
        return if (this.canGoBack) {
            engineSession?.goBack()
            true
        } else {
            false
        }
    }

    protected fun initializeSession(fromEngineSession: EngineSession? = null) {
        engineSession = fromEngineSession ?: requireComponents.core.engine.createSession()
        session = createCustomTab(
            url = "",
            source = SessionState.Source.Internal.CustomTab,
        ).copy(engineState = EngineState(engineSession))
        requireComponents.core.store.dispatch(CustomTabListAction.AddCustomTabAction(session as CustomTabSessionState))
    }

    @Suppress("OVERRIDE_DEPRECATION")
    final override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        when (requestCode) {
            REQUEST_CODE_PROMPT_PERMISSIONS -> promptsFeature.get()?.onPermissionsResult(permissions, grantResults)
            REQUEST_CODE_DOWNLOAD_PERMISSIONS -> promptsFeature.get()?.onPermissionsResult(permissions, grantResults)
        }
    }

    private fun shouldShowCompletedDownloadDialog(
        downloadState: DownloadState,
        status: Status,
    ): Boolean {
        val isValidStatus = status in listOf(Status.COMPLETED, Status.FAILED)
        val isSameTab = downloadState.sessionId == (session?.id ?: false)

        return isValidStatus && isSameTab
    }

    private fun handleOnDownloadFinished(
        downloadState: DownloadState,
        downloadJobStatus: Status,
        downloadFileUtils: DownloadFileUtils,
    ) {
        // If the download is just paused, don't show any in-app notification
        if (shouldShowCompletedDownloadDialog(downloadState, downloadJobStatus)) {
            if (downloadState.openInApp && downloadJobStatus == Status.COMPLETED) {
                val fileWasOpened = downloadFileUtils.openFile(
                    fileName = downloadState.fileName,
                    directoryPath = downloadState.directoryPath,
                    contentType = downloadState.contentType,
                )
                if (!fileWasOpened) {
                    requireComponents.appStore.dispatch(
                        AppAction.DownloadAction.CannotOpenFile(
                            downloadState = downloadState,
                        ),
                    )
                }
            } else {
                if (downloadJobStatus == Status.FAILED) {
                    requireComponents.appStore.dispatch(
                        AppAction.DownloadAction.DownloadFailed(
                            downloadState.fileName,
                        ),
                    )
                } else {
                    requireComponents.appStore.dispatch(
                        AppAction.DownloadAction.DownloadCompleted(
                            downloadState,
                        ),
                    )
                }
            }
        }
    }

    private fun canShowDownloadDialog(): Boolean {
        val isRenameFragmentShowing = childFragmentManager.findFragmentByTag(
            RenameAndChangeLocationDialogFragment.RENAME_AND_CHANGE_LOCATION_DIALOG_TAG,
        ) != null

        return downloadDialog == null && !isRenameFragmentShowing
    }

    private fun dismissRenameDialog() {
        val renameDialog = childFragmentManager.findFragmentByTag(
            RenameAndChangeLocationDialogFragment.RENAME_AND_CHANGE_LOCATION_DIALOG_TAG,
        ) as? RenameAndChangeLocationDialogFragment
        renameDialog?.dismissAllowingStateLoss()
    }

    /**
     * Forwards activity results to the [ActivityResultHandler] features.
     */
    override fun onActivityResult(requestCode: Int, data: Intent?, resultCode: Int): Boolean {
        return listOf(
            promptsFeature,
        ).any { it.onActivityResult(requestCode, data, resultCode) }
    }

    private fun showFirstPartyDownloadDialog(
        currentDownloadState: CurrentDownloadState,
        positiveAction: PositiveActionCallback,
        negativeAction: NegativeActionCallback,
    ) {
        val contentSize = currentDownloadState.value.contentLength ?: 0
        val title = if (contentSize > 0L) {
            val contentSizeInBytes = requireComponents.core.fileSizeFormatter.formatSizeInBytes(
                contentSize,
            )
            getString(
                downloadsR.string.mozac_feature_downloads_dialog_title_3,
                contentSizeInBytes,
            )
        } else {
            getString(
                downloadsR.string.mozac_feature_downloads_dialog_title_with_unknown_size,
            )
        }

        downloadDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(currentDownloadState.value.fileName)
            .setPositiveButton(
                downloadsR.string.mozac_feature_downloads_dialog_download,
            ) { dialog, _ ->
                positiveAction.value.invoke(currentDownloadState.value)
                dialog.dismiss()
            }
            .setNegativeButton(
                downloadsR.string.mozac_feature_downloads_dialog_cancel,
            ) { dialog, _ ->
                negativeAction.value.invoke()
                dialog.dismiss()
            }.setOnDismissListener {
                downloadDialog = null
                requireContext().components.analytics.crashReporter.recordCrashBreadcrumb(
                    Breadcrumb("FirstPartyDownloadDialog onDismiss"),
                )
            }.show()
    }

    private fun showRenameDownloadDialog(
        fileName: String,
        currentDownloadState: CurrentDownloadState,
        positiveAction: PositiveActionCallback,
        negativeAction: NegativeActionCallback,
    ) {
        val existingFragment = childFragmentManager.findFragmentByTag(
            RenameAndChangeLocationDialogFragment.RENAME_AND_CHANGE_LOCATION_DIALOG_TAG,
        )

        if (existingFragment == null && isAdded && !childFragmentManager.isStateSaved) {
            val renameDialog = RenameAndChangeLocationDialogFragment.newInstance(
                fileName = fileName,
                directoryPath = currentDownloadState.value.directoryPath,
                contentSize = currentDownloadState.value.contentLength ?: 0,
            )
            renameDialog.onConfirmSave = { newFileName: String, directoryPath: String ->
                val downloadState = currentDownloadState.value.copy(
                    fileName = newFileName,
                    directoryPath = directoryPath,
                )
                positiveAction.value.invoke(downloadState)
            }
            renameDialog.onCancel = {
                negativeAction.value.invoke()
            }
            renameDialog.show(
                childFragmentManager,
                RenameAndChangeLocationDialogFragment.RENAME_AND_CHANGE_LOCATION_DIALOG_TAG,
            )
        }
    }

    companion object {
        private const val REQUEST_CODE_PROMPT_PERMISSIONS = 1
        private const val REQUEST_CODE_DOWNLOAD_PERMISSIONS = 2
    }
}
