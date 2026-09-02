/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.downloads.listscreen

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import mozilla.components.compose.base.button.TextButton
import org.mozilla.fenix.R
import org.mozilla.fenix.downloads.listscreen.store.FileItem
import org.mozilla.fenix.downloads.listscreen.store.RenameFileError
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.PreviewThemeProvider
import org.mozilla.fenix.theme.Theme
import org.mozilla.fenix.theme.ThemedValue
import org.mozilla.fenix.theme.ThemedValueProvider
import java.io.File
import mozilla.components.ui.icons.R as iconsR

/**
 * This encapsulates the flow for the renaming of a downloaded file.
 *
 * @param fileToRename The original download file to be renamed.
 * @param renameFileError The [RenameFileError] shown if there is a renaming error.
 * @param isChangeFileExtensionDialogVisible Indicates whether or not the dialog to change file extension is visible.
 * @param onRenameFileConfirmed Callback invoked when the user confirms the rename.
 * @param onRenameFileDismissed Callback invoked when the user dismisses the rename.
 * @param onRenameFileFailureDismissed Callback invoked when the user dismisses Cannot Rename failure.
 * @param onFileExtensionChangedByUser Callback invoked when the user changes the file extension during renaming.
 * @param onCloseChangeFileExtensionDialog Callback invoked when the file extension change dialog is closed.
 */
@SuppressWarnings("LongParameterList")
@Composable
fun DownloadRenameFlow(
    fileToRename: FileItem?,
    renameFileError: RenameFileError?,
    isChangeFileExtensionDialogVisible: Boolean,
    onRenameFileConfirmed: (FileItem, String) -> Unit,
    onRenameFileDismissed: () -> Unit,
    onRenameFileFailureDismissed: () -> Unit,
    onFileExtensionChangedByUser: (FileItem, String) -> Unit,
    onCloseChangeFileExtensionDialog: () -> Unit,
) {
    val fileToRename = fileToRename ?: return
    val originalName = fileToRename.fileName ?: File(fileToRename.filePath).name
    var fileNameState by remember(originalName) {
        val end = File(originalName).nameWithoutExtension.length
        mutableStateOf(
            TextFieldValue(
                text = originalName,
                selection = TextRange(0, end),
            ),
        )
    }

    val proposedName = fileNameState.text.trim()
    val proposedExtension = File(proposedName).extension

    if (isChangeFileExtensionDialogVisible) {
        ChangeFileExtensionDialog(
            fileExtension = proposedExtension,
            onConfirm = {
                onRenameFileConfirmed(fileToRename, proposedName)
                onCloseChangeFileExtensionDialog()
            },
            onDismiss = onCloseChangeFileExtensionDialog,
        )
    } else {
        DownloadRenameDialog(
            originalFileName = originalName,
            error = renameFileError,
            fileNameState = fileNameState,
            onFileNameChange = { fileNameState = it },
            onConfirmSave = {
                onFileExtensionChangedByUser(fileToRename, proposedName)
            },
            onCancel = onRenameFileDismissed,
            onCannotRenameDismiss = onRenameFileFailureDismissed,
        )
    }
}

/**
* This dialog is used to prompt the user to rename the downloaded file.
* It provides options to confirm or cancel the rename.
*
* @param originalFileName The original download file name to be renamed.
* @param error The [RenameFileError] shown if there is a renaming error.
* @param fileNameState The [TextFieldValue] for the dialog text field.
* @param onFileNameChange Callback invoked when the file name is changed.
* @param onConfirmSave Callback invoked when the user confirms the rename.
* @param onCancel Callback invoked when the user cancels.
* @param onCannotRenameDismiss Callback invoked when the user dismisses Cannot Rename failure.
*/
@Composable
fun DownloadRenameDialog(
    originalFileName: String,
    error: RenameFileError? = null,
    fileNameState: TextFieldValue,
    onFileNameChange: (TextFieldValue) -> Unit,
    onConfirmSave: (String) -> Unit,
    onCancel: () -> Unit,
    onCannotRenameDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val currentError: RenameFileError? = when {
        fileNameState.text.contains("/") -> RenameFileError.InvalidFileName
        error is RenameFileError.NameAlreadyExists &&
                error.proposedFileName == fileNameState.text -> error
        error is RenameFileError.CaseOnlyNameChange &&
            error.proposedFileName == fileNameState.text -> error
        else -> null
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = stringResource(R.string.download_rename_dialog_title),
                style = FirefoxTheme.typography.headline5,
            )
        },
        text = {
            DownloadRenameDialogTextField(
                fileNameState = fileNameState,
                onFileNameChange = onFileNameChange,
                currentError = currentError,
                focusRequester = focusRequester,
            )
        },
        confirmButton = {
            val newName = fileNameState.text.trim()
            TextButton(
                text = stringResource(id = R.string.download_rename_dialog_confirm_button),
                enabled = enableConfirmButton(originalFileName, newName, currentError),
                onClick = { onConfirmSave(newName) },
                modifier = Modifier.testTag(DownloadsListTestTag.RENAME_DIALOG_CONFIRM_BUTTON),
            )
        },
        dismissButton = {
            TextButton(
                text = stringResource(id = R.string.download_rename_dialog_cancel_button),
                onClick = onCancel,
                modifier = Modifier.testTag(DownloadsListTestTag.RENAME_DIALOG_CANCEL_BUTTON),
            )
        },
    )

    if (error == RenameFileError.CannotRename) {
        DownloadCannotRenameDialog(onDismiss = onCannotRenameDismiss)
    }
}

/**
 * A text field component used within the [DownloadRenameDialog].
 * It handles displaying the current file name, validation errors,
 * and supporting text for character limits.
 *
 * @param fileNameState The current state of the text input including selection.
 * @param focusRequester The [FocusRequester] used to focus this text field.
 * @param onFileNameChange Callback for when the user edits the text.
 * @param currentError The specific [RenameFileError] to display, if any.
 * @param modifier Modifier for layout adjustments.
 * @param extension The non-editable file extension to display as a suffix.
 */
@Composable
fun DownloadRenameDialogTextField(
    fileNameState: TextFieldValue,
    focusRequester: FocusRequester,
    onFileNameChange: (TextFieldValue) -> Unit,
    currentError: RenameFileError?,
    modifier: Modifier = Modifier,
    extension: String? = null,
) {
    val errorTextResource = when (currentError) {
        is RenameFileError.InvalidFileName ->
            stringResource(R.string.download_rename_error_invalid_name_error)
        is RenameFileError.NameAlreadyExists ->
            stringResource(
                R.string.download_rename_error_exists_error,
                currentError.proposedFileName,
            )
        is RenameFileError.CaseOnlyNameChange ->
            stringResource(R.string.download_rename_error_case_only_error)
        else -> null
    }

    OutlinedTextField(
        value = fileNameState,
        onValueChange = onFileNameChange,
        label = { Text(stringResource(R.string.download_rename_dialog_label)) },
        isError = currentError != null,
        supportingText = errorTextResource?.let {
            {
                Text(
                    text = it,
                    style = FirefoxTheme.typography.caption,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        suffix = extension?.let {
            {
                Text(
                    text = ".$it",
                    style = FirefoxTheme.typography.body1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .testTag(DownloadsListTestTag.RENAME_DIALOG_TEXT_FIELD),
    )
}

/**
 * This determines whether to enable the confirmation button, based on file
 * name validation such as if the new file name differs or if the new base
 * file name is not blank.
 *
 * @param originalFileName The original download file to be renamed.
 * @param newFileName The proposed new file name.
 * @param currentError The current error in the text field.
 */
@VisibleForTesting
internal fun enableConfirmButton(
    originalFileName: String,
    newFileName: String,
    currentError: RenameFileError? = null,
): Boolean {
    val trimmed = newFileName.trim()

    val isInvalidRename =
        currentError != null ||
        trimmed.isEmpty() ||
        trimmed.equals(originalFileName, ignoreCase = false) ||
        '/' in trimmed ||
        '\u0000' in trimmed
    if (isInvalidRename) return false

    val base = File(trimmed).nameWithoutExtension
    return base.isNotBlank()
}

@Composable
private fun DownloadCannotRenameDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        icon = {
            Icon(
                painter = painterResource(iconsR.drawable.mozac_ic_critical_24),
                contentDescription = null,
            )
        },
        title = {
            Text(
                text = stringResource(R.string.download_rename_error_cannot_rename_title),
                style = FirefoxTheme.typography.headline5,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        text = { Text(stringResource(R.string.download_rename_error_cannot_rename_description)) },
        confirmButton = {
            TextButton(
                text = stringResource(R.string.download_rename_error_dismiss_button),
                onClick = onDismiss,
                modifier = Modifier.testTag(
                    DownloadsListTestTag.RENAME_DIALOG_FAILURE_DISMISS_BUTTON,
                ),
            )
        },
        onDismissRequest = onDismiss,
    )
}

/**
 * This dialog informs the user they are requesting to change the file type.
 *
 * @param fileExtension The new file extension to change to.
 * @param onConfirm Callback invoked when the user confirms the file extension change.
 * @param onDismiss Callback invoked when the user cancels.
 */
@Composable
fun ChangeFileExtensionDialog(
    fileExtension: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.change_file_extension_title, ".$fileExtension"),
                style = FirefoxTheme.typography.headline5,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        text = { Text(stringResource(R.string.change_file_extension_description)) },
        confirmButton = {
            TextButton(
                text = stringResource(R.string.change_file_extension_confirm_button),
                onClick = onConfirm,
                modifier = Modifier.testTag(
                    DownloadsListTestTag.CHANGE_FILE_EXTENSION_CONFIRM_BUTTON,
                ),
            )
        },
        dismissButton = {
            TextButton(
                text = stringResource(R.string.change_file_extension_cancel_button),
                onClick = onDismiss,
                modifier = Modifier.testTag(
                    DownloadsListTestTag.CHANGE_FILE_EXTENSION_CANCEL_BUTTON,
                ),
            )
        },
    )
}

private data class RenameDialogPreviewState(
    val originalFileName: String,
    val error: RenameFileError? = null,
)

private class RenameDialogPreviewProvider : ThemedValueProvider<RenameDialogPreviewState>(
    sequenceOf(
        RenameDialogPreviewState(
            originalFileName = "README.md",
        ),
        RenameDialogPreviewState(
            originalFileName = "original.test.name.jpg",
        ),
        RenameDialogPreviewState(
            originalFileName = "file_with_no_extension",
        ),
        RenameDialogPreviewState(
            originalFileName = "README(2).md",
            error = RenameFileError.NameAlreadyExists(proposedFileName = "README(2).md"),
        ),
        RenameDialogPreviewState(
            originalFileName = "README.md",
            error = RenameFileError.CannotRename,
        ),
    ),
)

@Preview
@Composable
private fun RenameDownloadFileDialogPreview(
    @PreviewParameter(RenameDialogPreviewProvider::class) state: ThemedValue<RenameDialogPreviewState>,
) {
    var fileNameState by remember(state.value.originalFileName) {
        val fileNameLength = File(state.value.originalFileName).nameWithoutExtension.length
        mutableStateOf(
            TextFieldValue(
                text = state.value.originalFileName,
                selection = TextRange(0, fileNameLength),
            ),
        )
    }

    FirefoxTheme(state.theme) {
        DownloadRenameDialog(
            originalFileName = state.value.originalFileName,
            error = state.value.error,
            fileNameState = fileNameState,
            onFileNameChange = {},
            onConfirmSave = {},
            onCancel = {},
            onCannotRenameDismiss = {},
        )
    }
}

@Preview
@Composable
private fun ChangeFileExtensionDialogPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme) {
        ChangeFileExtensionDialog(
            fileExtension = "pdf",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
