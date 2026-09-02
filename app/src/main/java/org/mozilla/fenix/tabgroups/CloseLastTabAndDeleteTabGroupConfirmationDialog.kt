/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabgroups

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import mozilla.components.compose.base.annotation.FlexibleWindowPreview
import mozilla.components.compose.base.button.TextButton
import org.mozilla.fenix.R
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.PreviewThemeProvider
import org.mozilla.fenix.theme.Theme
import mozilla.components.ui.icons.R as iconsR

/**
 * This dialog is used to prompt the user to confirm if they want to close the current tab
 * and delete the associated tab group.
 *
 * @param onConfirmDelete Callback invoked when the user confirms closing the tab and deleting the group.
 * @param onCancel Callback invoked when the user cancels the action.
 */
@Composable
fun CloseLastTabAndDeleteTabGroupConfirmationDialog(
    onConfirmDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        icon = {
            Icon(
                painter = painterResource(iconsR.drawable.mozac_ic_delete_24),
                contentDescription = null,
            )
        },
        onDismissRequest = onCancel,
        title = {
            Text(
                text = stringResource(R.string.close_tab_and_delete_group_confirmation_dialog_title),
                style = FirefoxTheme.typography.headline5,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.close_tab_and_delete_group_confirmation_dialog_body),
                style = FirefoxTheme.typography.body2,
            )
        },
        confirmButton = {
            TextButton(
                text = stringResource(R.string.close_tab_and_delete_group_confirmation_dialog_confirm),
                onClick = onConfirmDelete,
                modifier = Modifier.testTag(tag = TabGroupsTestTag.CLOSE_LAST_TAB_AND_DELETE_DIALOG_CONFIRM_BUTTON),
            )
        },
        dismissButton = {
            TextButton(
                text = stringResource(R.string.close_tab_and_delete_group_confirmation_dialog_cancel),
                onClick = onCancel,
                modifier = Modifier.testTag(tag = TabGroupsTestTag.CLOSE_LAST_TAB_AND_DELETE_DIALOG_CANCEL_BUTTON),
            )
        },
    )
}

@FlexibleWindowPreview
@Composable
private fun CloseTabAndDeleteGroupConfirmationDialogPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme) {
        Surface {
            CloseLastTabAndDeleteTabGroupConfirmationDialog(
                onConfirmDelete = {},
                onCancel = {},
            )
        }
    }
}
