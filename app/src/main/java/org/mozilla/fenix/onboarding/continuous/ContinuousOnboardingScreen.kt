/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding.continuous

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mozilla.components.compose.base.button.FilledButton
import mozilla.components.compose.base.button.IconButton
import mozilla.components.compose.base.button.OutlinedButton
import org.mozilla.fenix.R
import org.mozilla.fenix.onboarding.view.Action
import org.mozilla.fenix.onboarding.view.OnboardingPageState
import org.mozilla.fenix.theme.FirefoxTheme
import mozilla.components.ui.icons.R as iconsR

val maxCardWidth = 360.dp

/**
 * A screen for displaying continuous onboarding.
 */
@Composable
fun ContinuousOnboardingScreen(
    pageState: OnboardingPageState,
    onDismissRequest: () -> Unit,
    onCloseButtonClicked: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = true,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            CardContent(
                pageState = pageState,
                onDismissRequest = onDismissRequest,
                onCloseButtonClicked = onCloseButtonClicked,
            )
        }

        LaunchedEffect(Unit) {
            pageState.onRecordImpressionEvent()
        }
    }
}

@Composable
private fun CardContent(
    pageState: OnboardingPageState,
    onDismissRequest: () -> Unit,
    onCloseButtonClicked: () -> Unit,
) {
    @Composable
    fun Content() {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(36.dp),
        ) {
            Text(
                text = pageState.title,
                style = MaterialTheme.typography.headlineSmall,
            )

            Box(
                modifier = Modifier
                    .height(150.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(pageState.imageRes),
                    contentDescription = null, // Decorative only
                )
            }

            Text(
                text = pageState.description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = FirefoxTheme.typography.subtitle1,
            )
        }
    }

    Card(
        modifier = Modifier
            .widthIn(max = maxCardWidth)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
        ),
    ) {
        IconButton(
            onClick = {
                onCloseButtonClicked()
                onDismissRequest()
            },
            contentDescription = stringResource(R.string.onboarding_home_content_description_close_button),
            modifier = Modifier.align(Alignment.End),
        ) {
            Icon(
                painter = painterResource(id = iconsR.drawable.mozac_ic_cross_24),
                contentDescription = null, // Uses the parent content description
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Content()

            Spacer(Modifier.height(36.dp))

            FilledButton(
                modifier = Modifier.width(FirefoxTheme.layout.size.maxWidth.small),
                text = pageState.primaryButton.text,
                onClick = {
                    pageState.primaryButton.onClick()
                    onDismissRequest()
                },
            )

            pageState.secondaryButton?.let {
                OutlinedButton(
                    modifier = Modifier.width(FirefoxTheme.layout.size.maxWidth.small),
                    text = it.text,
                    onClick = {
                        it.onClick()
                        onDismissRequest()
                    },
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ContinuousOnboardingScreenNotificationPreview() {
    FirefoxTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            CardContent(
                pageState = OnboardingPageState(
                    imageRes = R.drawable.nova_onboarding_notifications,
                    title = stringResource(R.string.nova_onboarding_notifications_title),
                    description = stringResource(R.string.nova_onboarding_notifications_subtitle),
                    primaryButton = Action(
                        text = stringResource(R.string.nova_onboarding_notifications_button),
                        onClick = { },
                    ),
                    secondaryButton = Action(
                        text = stringResource(R.string.nova_onboarding_negative_button),
                        onClick = { },
                    ),
                    onRecordImpressionEvent = { },
                ),
                onDismissRequest = { },
                onCloseButtonClicked = { },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ContinuousOnboardingScreenSyncPreview() {
    FirefoxTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            CardContent(
                pageState = OnboardingPageState(
                    imageRes = R.drawable.nova_onboarding_sync,
                    title = stringResource(R.string.nova_onboarding_sync_title),
                    description = stringResource(R.string.nova_onboarding_sync_subtitle),
                    primaryButton = Action(
                        text = stringResource(R.string.nova_onboarding_sync_button),
                        onClick = { },
                    ),
                    secondaryButton = Action(
                        text = stringResource(R.string.nova_onboarding_continue_button),
                        onClick = { },
                    ),
                    onRecordImpressionEvent = { },
                ),
                onDismissRequest = { },
                onCloseButtonClicked = { },
            )
        }
    }
}
