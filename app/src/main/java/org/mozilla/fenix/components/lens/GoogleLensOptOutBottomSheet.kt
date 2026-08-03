/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.lens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import mozilla.components.compose.base.BottomSheetHandle
import mozilla.components.compose.base.LinkText
import mozilla.components.compose.base.LinkTextState
import mozilla.components.compose.base.button.FilledButton
import mozilla.components.compose.base.button.OutlinedButton
import org.mozilla.fenix.R
import org.mozilla.fenix.theme.FirefoxTheme

private val HorizontalPadding = 24.dp
private val BottomPadding = 24.dp
private val HandleVerticalPadding = 12.dp
private val ContentSpacing = 12.dp
private val ButtonSpacing = 8.dp
private val ButtonsTopPadding = 12.dp

/**
 * First-run bottom sheet explaining Google Lens, shown before the camera is started so the user
 * can leave without any camera or permission activity taking place.
 *
 * Meant to be hosted directly in a `BottomSheetDialogFragment`, which supplies the slide-in
 * animation and drag-to-dismiss behaviour; the colors come from the surrounding [FirefoxTheme] so
 * the sheet follows the same light/dark/private theming as the rest of the browser.
 *
 * @param onRequestDismiss Invoked when accessibility services request dismissal via the handle.
 * @param onTryItNowClick Invoked when the user accepts and wants to continue to the camera.
 * @param onNotNowClick Invoked when the user declines and wants to leave Google Lens.
 * @param onSettingsClick Invoked when the user taps the Google Lens settings link.
 */
@Composable
fun GoogleLensOptOutBottomSheet(
    onRequestDismiss: () -> Unit,
    onTryItNowClick: () -> Unit,
    onNotNowClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val cornerShape = MaterialTheme.shapes.large.copy(
        bottomStart = CornerSize(0.dp),
        bottomEnd = CornerSize(0.dp),
    )

    Column(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.surface, shape = cornerShape)
            .navigationBarsPadding()
            .verticalScroll(scrollState),
    ) {
        BottomSheetHandle(
            onRequestDismiss = onRequestDismiss,
            contentDescription = stringResource(R.string.a11y_action_label_collapse),
            modifier = Modifier
                .padding(vertical = HandleVerticalPadding)
                .align(Alignment.CenterHorizontally),
        )

        Column(
            modifier = Modifier.padding(
                start = HorizontalPadding,
                end = HorizontalPadding,
                bottom = BottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(ContentSpacing),
        ) {
            Text(
                text = stringResource(R.string.lens_opt_out_title),
                style = FirefoxTheme.typography.headline6,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(R.string.lens_opt_out_description),
                style = FirefoxTheme.typography.body2,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            val settingsLinkText = stringResource(R.string.lens_opt_out_settings_link)
            LinkText(
                text = settingsLinkText,
                linkTextStates = listOf(
                    LinkTextState(
                        text = settingsLinkText,
                        // In-app navigation rather than a URL, so the click is handled entirely by
                        // onClick and the url is left empty.
                        url = "",
                        onClick = { onSettingsClick() },
                    ),
                ),
                style = FirefoxTheme.typography.body2.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                ),
                linkTextDecoration = TextDecoration.Underline,
                shouldApplyAccessibleSize = true,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Buttons(
                onTryItNowClick = onTryItNowClick,
                onNotNowClick = onNotNowClick,
            )
        }
    }
}

@Composable
private fun Buttons(
    onTryItNowClick: () -> Unit,
    onNotNowClick: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(top = ButtonsTopPadding),
        verticalArrangement = Arrangement.spacedBy(ButtonSpacing),
    ) {
        FilledButton(
            text = stringResource(R.string.lens_opt_out_try_it_now_button),
            modifier = Modifier.fillMaxWidth(),
            onClick = onTryItNowClick,
        )

        OutlinedButton(
            text = stringResource(R.string.lens_opt_out_not_now_button),
            modifier = Modifier.fillMaxWidth(),
            onClick = onNotNowClick,
        )
    }
}

@PreviewLightDark
@Composable
private fun GoogleLensOptOutBottomSheetPreview() {
    FirefoxTheme {
        GoogleLensOptOutBottomSheet(
            onRequestDismiss = {},
            onTryItNowClick = {},
            onNotNowClick = {},
            onSettingsClick = {},
        )
    }
}
