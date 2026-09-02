/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.webcompat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import mozilla.components.compose.base.annotation.FlexibleWindowPreview
import mozilla.components.compose.base.button.TextButton
import mozilla.components.compose.base.textfield.TextField
import org.mozilla.fenix.R
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.PreviewThemeProvider
import org.mozilla.fenix.theme.Theme
import org.mozilla.fenix.webcompat.BrokenSiteReporterTestTags

/**
 * This dialog is used to prompt the user to edit the website URL in the broken site reporter.
 *
 * @param url The current url being edited.
 * @param onUrlChange Callback invoked when the user types in the text field.
 * @param isError Whether the current [url] is invalid.
 * @param onSave Callback invoked when the user saves their edited url.
 * @param onDismiss Callback invoked when the dialog is dismissed.
 */
@Composable
fun EditUrlConfirmationDialog(
    url: String,
    onUrlChange: (String) -> Unit,
    isError: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.webcompat_reporter_edit_url_dialog_title),
                style = FirefoxTheme.typography.headline5,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            TextField(
                value = url,
                onValueChange = onUrlChange,
                placeholder = "",
                errorText = if (isError) stringResource(id = R.string.webcompat_reporter_url_error_invalid) else "",
                label = stringResource(id = R.string.webcompat_reporter_label_url),
                isError = isError,
                singleLine = true,
            )
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(
                    text = stringResource(id = R.string.webcompat_reporter_edit_url_dialog_dismiss),
                    onClick = onDismiss,
                    modifier = Modifier.testTag(
                        tag = BrokenSiteReporterTestTags.BROKEN_SITE_REPORTER_EDIT_URL_DIALOG_DISMISS_BUTTON,
                    ),
                )

                Spacer(modifier = Modifier.width(FirefoxTheme.layout.space.static100))

                TextButton(
                    text = stringResource(id = R.string.webcompat_reporter_edit_url_dialog_save),
                    onClick = onSave,
                    enabled = !isError,
                    modifier = Modifier.testTag(
                        tag = BrokenSiteReporterTestTags.BROKEN_SITE_REPORTER_EDIT_URL_DIALOG_SAVE_BUTTON,
                    ),
                )
            }
        },
    )
}

@FlexibleWindowPreview
@Composable
private fun EditUrlConfirmationDialogPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme) {
        Surface {
            EditUrlConfirmationDialog(
                url = "https://www.example.com",
                onUrlChange = {},
                isError = false,
                onSave = {},
                onDismiss = {},
            )
        }
    }
}
