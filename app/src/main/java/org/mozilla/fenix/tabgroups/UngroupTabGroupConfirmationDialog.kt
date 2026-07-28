/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabgroups

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import mozilla.components.compose.base.annotation.FlexibleWindowPreview
import mozilla.components.compose.base.button.TextButton
import org.mozilla.fenix.R
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.PreviewThemeProvider
import org.mozilla.fenix.theme.Theme

/**
 * This dialog is used to prompt the user to confirm if they want to ungroup a tab group.
 * It includes an option to suppress this warning in the future.
 *
 * @param onConfirmUngroup Callback invoked when the user confirms ungrouping. Passes a boolean
 *                         indicating whether "Don't ask again" was checked.
 * @param onCancel Callback invoked when the user cancels the action.
 */
@Composable
fun UngroupTabGroupConfirmationDialog(
    onConfirmUngroup: (dontAskAgain: Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    var dontAskAgain by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = stringResource(R.string.ungroup_tab_group_confirmation_dialog_title),
                style = FirefoxTheme.typography.headline5,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.ungroup_tab_group_confirmation_dialog_body),
                    style = FirefoxTheme.typography.body2,
                )

                Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.static300))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = FirefoxTheme.layout.space.static100),
                ) {
                    Checkbox(
                        checked = dontAskAgain,
                        onCheckedChange = { dontAskAgain = it },
                        modifier = Modifier.testTag(TabGroupsTestTag.UNGROUP_DIALOG_DONT_ASK_AGAIN_CHECKBOX),
                    )
                    Text(
                        text = stringResource(R.string.ungroup_tab_group_confirmation_dialog_dont_ask_again),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = FirefoxTheme.typography.body2,
                        modifier = Modifier.padding(start = FirefoxTheme.layout.space.static50),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                text = stringResource(R.string.ungroup_tab_group_confirmation_dialog_confirm),
                onClick = { onConfirmUngroup(dontAskAgain) },
                modifier = Modifier.testTag(tag = TabGroupsTestTag.UNGROUP_DIALOG_CONFIRM_BUTTON),
            )
        },
        dismissButton = {
            TextButton(
                text = stringResource(R.string.ungroup_tab_group_confirmation_dialog_cancel),
                onClick = onCancel,
                modifier = Modifier.testTag(tag = TabGroupsTestTag.UNGROUP_DIALOG_CANCEL_BUTTON),
            )
        },
    )
}

@FlexibleWindowPreview
@Composable
private fun UngroupTabGroupConfirmationDialogPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme) {
        Surface {
            UngroupTabGroupConfirmationDialog(
                onConfirmUngroup = {},
                onCancel = {},
            )
        }
    }
}
