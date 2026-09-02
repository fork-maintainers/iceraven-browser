/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.downloads

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import mozilla.components.compose.base.button.FilledButton
import mozilla.components.compose.base.button.TextButton
import mozilla.components.support.utils.ext.withExtension
import org.mozilla.fenix.compose.list.IconListItem
import org.mozilla.fenix.downloads.listscreen.DownloadRenameDialogTextField
import org.mozilla.fenix.downloads.listscreen.store.RenameFileError
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.PreviewThemeProvider
import org.mozilla.fenix.theme.Theme
import java.io.File
import mozilla.components.feature.downloads.R as downloadsR
import mozilla.components.ui.icons.R as iconsR

/**
 * Composable content for the rename and change location dialog.
 *
 * @param dialogState The current state of the dialog.
 * @param friendlyPath A user-friendly, shortened path string to be displayed in the UI.
 * @param title The title text displayed at the top of the dialog.
 * @param onFileNameChange Callback when file name is changed.
 * @param onDirectorySelect Callback when directory selection is triggered.
 * @param onConfirm Callback when user confirms the dialog.
 * @param onCancel Callback when user cancels the dialog.
 */
@Composable
fun RenameAndChangeLocationDialogContent(
    dialogState: RenameAndChangeLocationDialogState,
    friendlyPath: String,
    title: String,
    onFileNameChange: (String) -> Unit,
    onDirectorySelect: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val (initialBaseName, extension) = remember {
        // We use a keyless remember to ensure the base name and extension are only calculated
        // once from the initial state, preventing them from being recalculated on every edit.
        val file = File(dialogState.fileName)
        file.nameWithoutExtension to file.extension
    }

    var fileNameState by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialBaseName,
                selection = TextRange(0, initialBaseName.length),
            ),
        )
    }

    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            val text = fileNameState.text
            val end = File(text).nameWithoutExtension.length
            fileNameState = fileNameState.copy(
                selection = TextRange(0, end),
            )
        }
    }

    val currentError: RenameFileError? = when {
        fileNameState.text.contains("/") -> RenameFileError.InvalidFileName
        else -> null
    }

    val trimmedBaseName = fileNameState.text.trim()
    val fullName = trimmedBaseName.withExtension(extension)

    val isConfirmEnabled = enableConfirmButton(fileName = fullName)

    Column(
        modifier = Modifier.padding(all = FirefoxTheme.layout.space.static300),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DialogHeader(
                title = title,
            )

            Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.static300))

            DownloadRenameDialogTextField(
                fileNameState = fileNameState,
                onFileNameChange = { newFileNameState ->
                    fileNameState = newFileNameState
                    val newFullName = newFileNameState.text.withExtension(extension)
                    onFileNameChange(newFullName)
                },
                currentError = currentError,
                focusRequester = focusRequester,
                modifier = Modifier.onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
                extension = extension.ifEmpty { null },
            )

            DirectorySelectionItem(
                friendlyPath = friendlyPath,
                onDirectorySelect = onDirectorySelect,
            )
        }

        DialogActionButtons(
            onConfirm = onConfirm,
            onCancel = onCancel,
            isConfirmEnabled = isConfirmEnabled,
        )
    }
}

@Composable
private fun DialogHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(id = iconsR.drawable.mozac_ic_download_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
        )

        Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.static200))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@VisibleForTesting
internal fun enableConfirmButton(
    fileName: String,
): Boolean {
    val isInvalidRename =
        fileName.isEmpty() ||
            '/' in fileName ||
            '\u0000' in fileName
    if (isInvalidRename) return false

    val base = File(fileName).nameWithoutExtension
    return base.isNotBlank()
}

@Composable
private fun DirectorySelectionItem(
    modifier: Modifier = Modifier,
    friendlyPath: String,
    onDirectorySelect: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        IconListItem(
            label = friendlyPath,
            onClick = onDirectorySelect,
            modifier = Modifier.fillMaxWidth(),
            contentPaddingListItem = PaddingValues(vertical = FirefoxTheme.layout.space.static150),
            beforeIconPainter = painterResource(iconsR.drawable.mozac_ic_folder_24),
        )
    }
}

@Composable
private fun DialogActionButtons(
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    isConfirmEnabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = FirefoxTheme.layout.space.static300),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(
            text = stringResource(id = downloadsR.string.mozac_feature_downloads_dialog_cancel),
            onClick = onCancel,
        )

        Spacer(modifier = Modifier.width(FirefoxTheme.layout.space.dynamic100))

        FilledButton(
            text = stringResource(id = downloadsR.string.mozac_feature_downloads_dialog_download),
            enabled = isConfirmEnabled,
            onClick = onConfirm,
        )
    }
}

/**
 * State for the rename and change location dialog.
 *
 * @property fileName The name of the file being downloaded.
 * @property directoryPath The path to the directory where the file will be saved.
 */
data class RenameAndChangeLocationDialogState(
    val fileName: String = "",
    val directoryPath: String = "",
)

@Composable
@Preview
private fun RenameAndChangeLocationDialogContentPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            RenameAndChangeLocationDialogContent(
                dialogState = RenameAndChangeLocationDialogState(
                    fileName = "filename.pdf",
                    directoryPath = "/storage/emulated/0/Download",
                ),
                friendlyPath = "~/Downloads",
                title = "Download file",
                onFileNameChange = {},
                onDirectorySelect = {},
                onConfirm = {},
                onCancel = {},
            )
        }
    }
}
