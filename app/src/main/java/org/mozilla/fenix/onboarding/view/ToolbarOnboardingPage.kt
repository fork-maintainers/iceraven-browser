/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding.view

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mozilla.components.compose.base.annotation.FlexibleWindowLightDarkPreview
import mozilla.components.compose.base.button.FilledButton
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.ScrollIndicator
import org.mozilla.fenix.onboarding.store.OnboardingStore
import org.mozilla.fenix.theme.FirefoxTheme
import mozilla.components.ui.icons.R as iconsR

private val TOOLBAR_IMAGE_HEIGHT = 150.dp

private val buttonHeight = 40.dp

/**
 * A Composable for displaying toolbar placement onboarding page content.
 *
 * @param onboardingStore The [OnboardingStore] that holds the toolbar selection state.
 * @param pageState The page content that's displayed.
 * @param onToolbarSelectionClicked Callback for when a toolbar selection is clicked.
 */
@Composable
fun ToolbarOnboardingPage(
    onboardingStore: OnboardingStore,
    pageState: OnboardingPageState,
    onToolbarSelectionClicked: (ToolbarOptionType) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(if (!pageState.isSmallDevice) 6.dp else 0.dp),
    ) {
        val verticalPadding = if (pageState.isSmallDevice) 0.dp else FirefoxTheme.layout.space.static300
        Column(
            modifier = Modifier.padding(
                horizontal = FirefoxTheme.layout.space.static200,
                vertical = verticalPadding,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (pageState.isSmallDevice) {
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.weight(TITLE_TOP_SPACER_WEIGHT))
            }

            Box(
                modifier = Modifier
                    .weight(CONTENT_WEIGHT)
                    .fillMaxWidth(),
            ) {
                val scrollState = rememberScrollState()

                CompositionLocalProvider(
                    LocalOverscrollFactory provides null,
                ) {
                    val startPadding = 20.dp
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(start = startPadding, end = FirefoxTheme.layout.space.static400),
                        verticalArrangement = Arrangement.spacedBy(36.dp),
                    ) {
                        Text(
                            text = pageState.title,
                            textAlign = TextAlign.Start,
                            style = FirefoxTheme.typography.headline6,
                        )

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            ToolbarPositionOptions(
                                onboardingStore = onboardingStore,
                                pageState = pageState,
                                onToolbarSelectionClicked = onToolbarSelectionClicked,
                            )
                        }
                    }
                }

                ScrollIndicator(
                    scrollState = scrollState,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    enabled = pageState.isSmallDevice,
                )
            }

            Spacer(Modifier.height(buttonHeight))

            FilledButton(
                text = pageState.primaryButton.text,
                modifier = Modifier
                    .width(width = FirefoxTheme.layout.size.maxWidth.small)
                    .semantics {
                        testTag = pageState.title + "onboarding_card.positive_button"
                    },
                onClick = pageState.primaryButton.onClick,
            )
        }
    }

    LaunchedEffect(Unit) {
        pageState.onRecordImpressionEvent()
    }
}

@Composable
private fun ToolbarPositionOptions(
    onboardingStore: OnboardingStore,
    pageState: OnboardingPageState,
    onToolbarSelectionClicked: (ToolbarOptionType) -> Unit,
) {
    val state by onboardingStore.stateFlow.collectAsState()
    pageState.toolbarOptions?.let { options ->
        Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            options.forEachIndexed { index, option ->
                ToolbarPositionOption(
                    modifier = Modifier.weight(1f),
                    option = option,
                    isSelected = option.toolbarType == state.toolbarOptionSelected,
                    onClick = { onToolbarSelectionClicked(option.toolbarType) },
                    contentAlignment = if (index == 0) {
                        Alignment.CenterEnd
                    } else {
                        Alignment.CenterStart
                    },
                )
            }
        }
    }
}

@Composable
private fun ToolbarPositionOption(
    option: ToolbarOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    contentAlignment: Alignment,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = contentAlignment,
    ) {
        Column(
            modifier = Modifier.clickable(
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Prevents onClick press/ripple animation
                onClick = onClick,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))

            Image(
                painter = painterResource(option.toolbarType.imageRes(isSelected)),
                contentDescription = null, // Decorative only
                modifier = Modifier.height(TOOLBAR_IMAGE_HEIGHT),
            )

            Spacer(Modifier.height(26.dp))

            Text(
                text = option.label,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = FirefoxTheme.typography.headline7,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(12.dp))

            SelectedCheckmark(isSelected)
        }
    }
}

@DrawableRes
private fun ToolbarOptionType.imageRes(isSelected: Boolean): Int =
    when (this) {
        ToolbarOptionType.TOOLBAR_TOP ->
            if (isSelected) {
                R.drawable.nova_onboarding_toolbar_top_active
            } else {
                R.drawable.nova_onboarding_toolbar_top_inactive
            }

        ToolbarOptionType.TOOLBAR_BOTTOM ->
            if (isSelected) {
                R.drawable.nova_onboarding_toolbar_bottom_active
            } else {
                R.drawable.nova_onboarding_toolbar_bottom_inactive
            }
    }

@Composable
private fun SelectedCheckmark(selected: Boolean = false) {
    if (selected) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .padding(1.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = iconsR.drawable.mozac_ic_checkmark_24),
                contentDescription = null, // Decorative only.
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(24.dp)
                    .padding(2.dp),
            )
        }
    } else {
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) { }
    }
}

@FlexibleWindowLightDarkPreview
@Composable
private fun OnboardingPagePreview() {
    FirefoxTheme {
        ToolbarOnboardingPage(
            onboardingStore = OnboardingStore(),
            pageState = OnboardingPageState(
                imageRes = R.drawable.ic_onboarding_customize_toolbar,
                title = stringResource(id = R.string.nova_onboarding_toolbar_selection_title),
                description = "", // Unused
                primaryButton = Action(
                    text = stringResource(
                        id = R.string.nova_onboarding_continue_button,
                    ),
                    onClick = {},
                ),
                toolbarOptions = listOf(
                    ToolbarOption(
                        toolbarType = ToolbarOptionType.TOOLBAR_TOP,
                        imageRes = R.drawable.ic_onboarding_top_toolbar,
                        label = stringResource(R.string.nova_onboarding_toolbar_selection_top_label),
                    ),
                    ToolbarOption(
                        toolbarType = ToolbarOptionType.TOOLBAR_BOTTOM,
                        imageRes = R.drawable.ic_onboarding_bottom_toolbar,
                        label = stringResource(R.string.nova_onboarding_toolbar_selection_bottom_label),
                    ),
                ),
                onRecordImpressionEvent = {},
            ),
            onToolbarSelectionClicked = {},
        )
    }
}

@Preview(
    locale = "es",
    fontScale = 2f,
)
@Preview(
    locale = "es",
    fontScale = 2f,
    widthDp = 1000,
    device = Devices.PIXEL_TABLET,
)
@Composable
private fun SpanishOnboardingPagePreview() {
    FirefoxTheme {
        ToolbarOnboardingPage(
            onboardingStore = OnboardingStore(),
            pageState = OnboardingPageState(
                imageRes = R.drawable.ic_onboarding_customize_toolbar,
                title = stringResource(id = R.string.nova_onboarding_toolbar_selection_title),
                description = "", // Unused
                primaryButton = Action(
                    text = stringResource(
                        id = R.string.nova_onboarding_continue_button,
                    ),
                    onClick = {},
                ),
                toolbarOptions = listOf(
                    ToolbarOption(
                        toolbarType = ToolbarOptionType.TOOLBAR_TOP,
                        imageRes = R.drawable.ic_onboarding_top_toolbar,
                        label = stringResource(R.string.nova_onboarding_toolbar_selection_top_label),
                    ),
                    ToolbarOption(
                        toolbarType = ToolbarOptionType.TOOLBAR_BOTTOM,
                        imageRes = R.drawable.ic_onboarding_bottom_toolbar,
                        label = stringResource(R.string.nova_onboarding_toolbar_selection_bottom_label),
                    ),
                ),
                onRecordImpressionEvent = {},
            ),
            onToolbarSelectionClicked = {},
        )
    }
}
