/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.webcompat.store

import android.webkit.URLUtil
import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import mozilla.components.lib.state.Action
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.State
import mozilla.components.lib.state.Store
import org.mozilla.fenix.R

private const val MIN_PROBLEM_DESCRIPTION_LENGTH = 10

/**
 * Value type that represents the state of the WebCompat Reporter.
 *
 * @property tabUrl The URL of the current tab when the reporter was opened.
 * @property enteredUrl The URL that is being reported as broken.
 * @property reason Specifies the reason that [enteredUrl] is broken.
 * @property showEditUrlDialog Whether or not the Edit Url Confirmation Dialog is shown.
 * @property editedUrl The temporary Url currently being edited in the dialog.
 * @property problemDescription Description of the encountered problem.
 * @property includeEtpBlockedUrls Checks if the user wants to include ETP-blocked URLs in the report.
 * @property previewReporterItems The list of items to be displayed in the WebCompat report preview.
 */
data class WebCompatReporterState(
    val tabUrl: String = "",
    val enteredUrl: String = "",
    val reason: BrokenSiteReason? = null,
    val showEditUrlDialog: Boolean = false,
    val editedUrl: String = "",
    val problemDescription: String = "",
    val includeEtpBlockedUrls: Boolean = false,
    val previewReporterItems: List<PreviewReporterItem> = emptyList(),
) : State {

    /**
     * An enum for the different reasons that a website is broken.
     *
     * @property displayStringId The string ID corresponding to the reason.
     */
    enum class BrokenSiteReason(
        @param:StringRes val displayStringId: Int,
    ) {
        NotSupported(
            displayStringId = R.string.webcompat_reporter_reason_notsupported_2,
        ),
        Load(
            displayStringId = R.string.webcompat_reporter_reason_load,
        ),
        Media(
            displayStringId = R.string.webcompat_reporter_reason_media2,
        ),
        DeceptiveSite(
            displayStringId = R.string.webcompat_reporter_reason_site_is_deceptive,
        ),
        Content(
            displayStringId = R.string.webcompat_reporter_reason_content2,
        ),
        Slow(
            displayStringId = R.string.webcompat_reporter_reason_slow2,
        ),
        Checkout(
            displayStringId = R.string.webcompat_reporter_reason_checkout,
        ),
        Account(
            displayStringId = R.string.webcompat_reporter_reason_account2,
        ),
        AdBlocker(
            displayStringId = R.string.webcompat_reporter_reason_turn_off_adblocker,
        ),
        Other(
            displayStringId = R.string.webcompat_reporter_reason_other,
        ),
    }

    /**
     * Helper function to strictly validate the URL.
     */
    private fun isValidUrl(urlToValidate: String): Boolean {
        if (urlToValidate.contains(" ")) return false

        return URLUtil.isNetworkUrl(urlToValidate)
    }

    /**
     * Whether the URL text field has an error.
     */
    val hasUrlTextError: Boolean
        get() = !isValidUrl(enteredUrl)

    /**
     * Whether the edited Url in the edit dialog has an error.
     */
    val hasEditedUrlError: Boolean
        get() = !isValidUrl(editedUrl)

    /**
     * Whether the reason dropdown has an error.
     */
    val hasReasonDropdownError: Boolean
        get() = reason == null

    /**
     * Whether the problem description label should be mandatory.
     */
    val problemDescriptionRequiredLabel: Boolean
        get() = reason == BrokenSiteReason.Other

    /**
     * Whether the problem description has an error.
     */
    val hasDescriptionError: Boolean
        get() = reason == BrokenSiteReason.Other && problemDescription.trim().length < MIN_PROBLEM_DESCRIPTION_LENGTH

    /**
     * Whether the submit button is enabled.
     */
    val isSubmitEnabled: Boolean
        get() = !hasUrlTextError && !hasReasonDropdownError && !hasDescriptionError
}

/**
 * Represents a single category of data to be displayed in the WebCompat report preview.
 *
 * @property title The header or category name for this group of items.
 * @property data A map of key-value pairs representing the specific data points in this category.
 */
data class PreviewReporterItem(
    val title: String,
    val data: Map<String, String>,
)

/**
 * [Action] implementation related to [WebCompatReporterStore].
 */
sealed class WebCompatReporterAction : Action {

    /**
     * A sealed type representing [WebCompatReporterAction]s which have storage side effects.
     */
    sealed interface WebCompatReporterStorageAction

    /**
     * Dispatched when [WebCompatReporterStore] has been initialized.
     */
    data object Initialized : WebCompatReporterAction(), WebCompatReporterStorageAction

    /**
     * Dispatched when the URL field is updated.
     *
     * @property newUrl The updated URL field.
     */
    data class BrokenSiteChanged(val newUrl: String) : WebCompatReporterAction()

    /**
     * Dispatched when the broken site reason is updated.
     *
     * @property newReason The updated broken site reason.
     */
    data class ReasonChanged(val newReason: WebCompatReporterState.BrokenSiteReason) : WebCompatReporterAction()

    /**
     * Dispatched when the user deselects their chosen reason to return to the list.
     */
    data object ReasonCleared : WebCompatReporterAction()

    /**
     * Dispatched when the ETP checkbox is toggled.
     *
     * @property include The value of the checkbox being toggled or not.
     */
    data class IncludeEtpBlockedUrlsChanged(val include: Boolean) : WebCompatReporterAction()

    /**
     * Dispatched when the problem description is updated.
     *
     * @property newProblemDescription The updated problem description.
     */
    data class ProblemDescriptionChanged(val newProblemDescription: String) : WebCompatReporterAction()

    /**
     * Dispatched when the user edits the Url in the dialog
     *
     * @property newUrl The updated Url that the user enters.
     */
    data class EditUrlChanged(val newUrl: String) : WebCompatReporterAction()

    /**
     * [Action] fired when the user navigates within the WebCompat Reporter.
     */
    sealed interface NavigationAction

    /**
     * Dispatched when the "Learn more" button is clicked.
     */
    data object LearnMoreClicked : WebCompatReporterAction(), WebCompatReporterStorageAction, NavigationAction

    /**
     * Dispatched when the user requests to send the WebCompat report.
     */
    data object SendReportClicked : WebCompatReporterAction()

    /**
     * Dispatched when the user selects the "Deceptive site" reason.
     */
    data object DeceptiveSiteReportSelected : WebCompatReporterAction(), NavigationAction

    /**
     * Dispatched when the WebCompat report has been submitted.
     */
    data object ReportSubmitted : WebCompatReporterAction(), NavigationAction

    /**
     * Dispatched when the WebCompat report "Preview Report" button is clicked.
     */
    data object OpenPreviewClicked : WebCompatReporterAction()

    /**
    * Dispatched when the preview of the report is opened up.
    *
    * @property previewReporterItems The list of items to be displayed in the WebCompat Report preview.
    */
    data class PreviewItemsUpdated(
        val previewReporterItems: List<PreviewReporterItem>,
    ) : WebCompatReporterAction()

    /**
     * Dispatched when the user requests to cancel the report.
     */
    data object CancelClicked : WebCompatReporterAction(), WebCompatReporterStorageAction, NavigationAction

    /**
     * Dispatched when the user clicks the field to open the Edit Url Dialog.
     */
    data object EditUrlClicked : WebCompatReporterAction()

    /**
     * Dispatched when the user saves their edits in the Edit Url Dialog.
     */
    data object SaveEditedUrlClicked : WebCompatReporterAction()

    /**
     * Dispatched when the user dismisses the Edit Url Dialog.
     */
    data object DismissEditUrlDialog : WebCompatReporterAction()

    /**
     * Dispatched when a previous [WebCompatReporterState] has been restored.
     */
    data class StateRestored(val restoredState: WebCompatReporterState) : WebCompatReporterAction()
}

private fun reduce(
    state: WebCompatReporterState,
    action: WebCompatReporterAction,
): WebCompatReporterState = when (action) {
    is WebCompatReporterAction.BrokenSiteChanged -> state.copy(enteredUrl = action.newUrl)
    is WebCompatReporterAction.ProblemDescriptionChanged -> state.copy(
        problemDescription = action.newProblemDescription,
    )
    is WebCompatReporterAction.ReasonChanged -> state.copy(reason = action.newReason)
    WebCompatReporterAction.Initialized -> state
    is WebCompatReporterAction.StateRestored -> action.restoredState
    is WebCompatReporterAction.OpenPreviewClicked -> state
    is WebCompatReporterAction.PreviewItemsUpdated -> state.copy(
        previewReporterItems = action.previewReporterItems,
    )
    is WebCompatReporterAction.DeceptiveSiteReportSelected -> state
    is WebCompatReporterAction.NavigationAction -> state
    WebCompatReporterAction.SendReportClicked -> state
    WebCompatReporterAction.LearnMoreClicked -> state
    is WebCompatReporterAction.IncludeEtpBlockedUrlsChanged -> state.copy(includeEtpBlockedUrls = action.include)
    is WebCompatReporterAction.EditUrlChanged -> state.copy(
        editedUrl = action.newUrl,
    )
    WebCompatReporterAction.EditUrlClicked -> state.copy(
        showEditUrlDialog = true,
        editedUrl = state.enteredUrl,
    )
    WebCompatReporterAction.DismissEditUrlDialog -> state.copy(
        showEditUrlDialog = false,
    )
    WebCompatReporterAction.SaveEditedUrlClicked -> state.copy(
        showEditUrlDialog = false,
        enteredUrl = state.editedUrl,
    )
    WebCompatReporterAction.ReasonCleared -> state.copy(reason = null)
}

/**
 * A [Store] that holds the [WebCompatReporterState] for the WebCompat Reporter and reduces
 * [WebCompatReporterAction]s dispatched to the store.
 */
class WebCompatReporterStore(
    initialState: WebCompatReporterState = WebCompatReporterState(),
    middleware: List<Middleware<WebCompatReporterState, WebCompatReporterAction>> = listOf(),
) : Store<WebCompatReporterState, WebCompatReporterAction>(
    initialState,
    ::reduce,
    middleware,
) {
    init {
        dispatch(WebCompatReporterAction.Initialized)
    }

    private val _navEvents = MutableSharedFlow<WebCompatReporterAction.NavigationAction>(
        extraBufferCapacity = 1,
    )

    val navEvents: SharedFlow<WebCompatReporterAction.NavigationAction>
        get() = _navEvents.asSharedFlow()

    internal fun emitNavAction(
        action: WebCompatReporterAction.NavigationAction,
    ) = _navEvents.tryEmit(action)
}
