/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:OptIn(ExperimentalAndroidComponentsApi::class)

package org.mozilla.fenix.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mozilla.components.ExperimentalAndroidComponentsApi
import mozilla.components.compose.base.LinkTextState
import mozilla.components.compose.base.PromoCard
import mozilla.components.compose.base.Switch
import mozilla.components.compose.base.annotation.FlexibleWindowPreview
import mozilla.components.compose.base.button.FilledButton
import mozilla.components.compose.base.button.IconButton
import mozilla.components.concept.engine.ipprotection.IPProtectionHandler
import mozilla.components.concept.engine.ipprotection.ServiceState
import mozilla.components.feature.ipprotection.store.state.Authorized
import mozilla.components.feature.ipprotection.store.state.BYTES_PER_GB
import mozilla.components.feature.ipprotection.store.state.Country
import mozilla.components.feature.ipprotection.store.state.EligibilityStatus
import mozilla.components.feature.ipprotection.store.state.IPProtectionState
import mozilla.components.feature.ipprotection.store.state.Location
import mozilla.components.feature.ipprotection.store.state.Recommended
import mozilla.components.feature.ipprotection.store.state.Uninitialized
import mozilla.components.feature.ipprotection.store.state.maxDataGb
import mozilla.components.feature.ipprotection.store.state.remainingDataGb
import mozilla.components.feature.ipprotection.store.state.usedDataGb
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.list.TextListItem
import org.mozilla.fenix.compose.settings.SettingsSectionHeader
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.PreviewThemeProvider
import org.mozilla.fenix.theme.Theme
import mozilla.components.ui.icons.R as iconsR

private val PROMO_ILLUSTRATION_SIZE = 60.dp

/**
 * The main VPN / IP Protection settings screen.
 *
 * @param state Current [IPProtectionHandler.StateInfo] to render.
 * @param snackbarHostState The [SnackbarHostState] used to display snackbars.
 * @param readyToUse Whether the user is entitled to use the service.
 * @param syncingData Whether the data sync is in progress.
 * @param promoDate Locale-formatted end date used by the promo copy when the user is on a metered
 * plan. `null` means the promo cannot be rendered (e.g. Nimbus shipped a malformed date) and the
 * card should fall back to the standard description.
 * @param onVpnToggle Called when the VPN switch is toggled.
 * @param onLearnMoreClick Called when any "Learn more" link is tapped.
 * @param onGetStartedClick Called when the "Get started" button is tapped.
 * @param showDebugAction Whether to show the debug menu action in the toolbar.
 * @param onDebugActionClick Called when the debug menu action is tapped.
 * @param onNavigateBack Called when the back navigation icon is tapped.
 * @param onLocationClicked Called when the VPN location row is tapped.
 * @param isLocationSelectionEnabled Whether the location row is interactive. When `false`, the row
 * is displayed without a click affordance.
 */
@Suppress("LongParameterList")
@Composable
fun IPProtectionScreen(
    state: IPProtectionState,
    snackbarHostState: SnackbarHostState,
    readyToUse: Boolean,
    syncingData: Boolean,
    promoDate: String?,
    onVpnToggle: (Boolean) -> Unit,
    onLearnMoreClick: () -> Unit,
    onGetStartedClick: () -> Unit,
    showDebugAction: Boolean = false,
    onDebugActionClick: () -> Unit = {},
    onNavigateBack: () -> Unit,
    onLocationClicked: () -> Unit,
    isLocationSelectionEnabled: Boolean = false,
) {
    val screenTitle = stringResource(R.string.ip_protection_title)

    Scaffold(
        modifier = Modifier.semantics { paneTitle = screenTitle },
        topBar = {
            IPProtectionTopAppBar(
                showDebugAction = showDebugAction,
                onNavigateBack = onNavigateBack,
                onDebugActionClick = onDebugActionClick,
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.static100))

                VpnPromoCard(
                    isActive = state.proxyStatus is Authorized.Active,
                    promoDate = promoDate.takeIf { state.maxDataGb <= 0F },
                    onLearnMoreClick = onLearnMoreClick,
                    modifier = Modifier.padding(horizontal = FirefoxTheme.layout.space.dynamic200),
                )

                Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.static200))

                VpnToggleRow(
                    checked = state.proxyStatus is Authorized.Active,
                    enabled = state.proxyStatus is Authorized && state.proxyStatus !is Authorized.DataLimitReached,
                    onToggle = onVpnToggle,
                )

                HorizontalDivider()

                if (readyToUse) {
                    if (state.maxDataBytes > 0) {
                        DataLimitSection(state = state, onLearnMoreClick = onLearnMoreClick)

                        HorizontalDivider()
                    }

                    VpnLocationSection(
                        selectedLocation = state.locationState.selectedLocation,
                        onLocationClicked = onLocationClicked,
                        enabled = isLocationSelectionEnabled,
                    )
                } else {
                    GetStartedSection(
                        syncingData = syncingData,
                        onGetStartedClick = onGetStartedClick,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IPProtectionTopAppBar(
    showDebugAction: Boolean,
    onNavigateBack: () -> Unit,
    onDebugActionClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.ip_protection_title),
                style = FirefoxTheme.typography.headline5,
                modifier = Modifier.semantics { heading() },
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onNavigateBack,
                contentDescription = stringResource(
                    R.string.ip_protection_navigate_back_button_content_description,
                ),
            ) {
                Icon(
                    painter = painterResource(iconsR.drawable.mozac_ic_back_24),
                    contentDescription = null,
                )
            }
        },
        actions = {
            if (showDebugAction) {
                IconButton(
                    onClick = onDebugActionClick,
                    contentDescription = stringResource(R.string.content_description_menu),
                ) {
                    Icon(
                        painter = painterResource(iconsR.drawable.mozac_ic_debug_drawer_24),
                        contentDescription = stringResource(R.string.debug_drawer_title),
                    )
                }
            }
        },
        windowInsets = WindowInsets(
            top = 0.dp,
            bottom = 0.dp,
        ),
    )
}

@OptIn(ExperimentalAndroidComponentsApi::class)
@Composable
private fun DataLimitSection(
    state: IPProtectionState,
    onLearnMoreClick: () -> Unit,
) {
    val isDataLimitReached = state.proxyStatus is Authorized.DataLimitReached

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = FirefoxTheme.layout.space.dynamic200,
                vertical = FirefoxTheme.layout.space.static150,
            ),
    ) {
        Text(
            text = stringResource(R.string.ip_protection_data_limit_label),
            style = FirefoxTheme.typography.subtitle1,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (!isDataLimitReached) {
            Text(
                text = stringResource(R.string.ip_protection_data_limit_value, state.remainingDataGb, state.maxDataGb),
                style = FirefoxTheme.typography.body2,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(R.string.ip_protection_data_limit_reached_description, state.maxDataGb.toInt()),
                style = FirefoxTheme.typography.body2,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    LinearProgressIndicator(
        progress = { if (isDataLimitReached) 1f else (state.usedDataGb / state.maxDataGb).coerceIn(0f, 1f) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FirefoxTheme.layout.space.dynamic200)
            .clip(CircleShape),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        drawStopIndicator = {},
    )

    Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.static100))

    val linkColor = MaterialTheme.colorScheme.tertiary
    Text(
        text = buildAnnotatedString {
            append(stringResource(R.string.ip_protection_data_reset_info, state.maxDataGb))
            append(" ")
            withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                append(stringResource(R.string.ip_protection_learn_more))
            }
        },
        style = FirefoxTheme.typography.body2,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLearnMoreClick() }
            .padding(
                horizontal = FirefoxTheme.layout.space.dynamic200,
                vertical = FirefoxTheme.layout.space.static150,
            ),
    )
}

@Composable
private fun ColumnScope.GetStartedSection(
    syncingData: Boolean,
    onGetStartedClick: () -> Unit,
) {
    Spacer(modifier = Modifier.weight(1f))

    val text = if (syncingData) {
        stringResource(R.string.ip_protection_connecting)
    } else {
        stringResource(R.string.ip_protection_get_started)
    }

    FilledButton(
        text = text,
        enabled = !syncingData,
        modifier = Modifier
            .padding(horizontal = FirefoxTheme.layout.space.static200)
            .fillMaxWidth(),
        onClick = onGetStartedClick,
    )

    Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.static400))
}

@Composable
private fun VpnLocationSection(
    selectedLocation: Location,
    onLocationClicked: () -> Unit,
    enabled: Boolean,
) {
    SettingsSectionHeader(
        text = stringResource(R.string.ip_protection_location_section),
        modifier = Modifier.padding(
            horizontal = FirefoxTheme.layout.space.dynamic200,
            vertical = FirefoxTheme.layout.space.static100,
        ),
    )

    when (selectedLocation) {
        is Recommended -> {
            TextListItem(
                label = stringResource(R.string.ip_protection_location_recommended_label),
                description = stringResource(
                    R.string.ip_protection_location_fastest_description,
                    stringResource(R.string.firefox),
                ),
                maxDescriptionLines = Int.MAX_VALUE,
                onClick = onLocationClicked.takeIf { enabled },
            )
        }
        is Country -> {
            TextListItem(
                label = selectedLocation.displayName,
                onClick = onLocationClicked.takeIf { enabled },
            )
        }
    }
}

@Composable
private fun VpnToggleRow(
    checked: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onToggle,
            )
            .padding(
                horizontal = FirefoxTheme.layout.space.dynamic200,
                vertical = FirefoxTheme.layout.space.static150,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FirefoxTheme.layout.space.static200),
    ) {
        Icon(
            painter = painterResource(mozilla.components.ui.icons.R.drawable.mozac_ic_globe_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.ip_protection_toggle_label),
            modifier = Modifier.weight(1f),
            style = FirefoxTheme.typography.subtitle1,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
        )
    }
}

@Composable
private fun VpnPromoCard(
    isActive: Boolean,
    promoDate: String?,
    onLearnMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val learnMoreText = stringResource(R.string.ip_protection_learn_more)
    val description = if (promoDate != null) {
        stringResource(R.string.ip_protection_onboarding_body_promo, promoDate, learnMoreText)
    } else {
        stringResource(R.string.ip_protection_promo_body_2, learnMoreText)
    }

    PromoCard(
        description = null,
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.ip_protection_promo_headline, stringResource(R.string.firefox)),
        footer = description to LinkTextState(
            text = learnMoreText,
            url = "",
            onClick = { onLearnMoreClick() },
        ),
        illustration = {
            Image(
                painter = painterResource(
                    if (isActive) {
                        R.drawable.ic_kit_shield_on_state
                    } else {
                        R.drawable.ic_kit_shield_off_state
                    },
                ),
                contentDescription = null,
                modifier = Modifier.size(PROMO_ILLUSTRATION_SIZE),
            )
        },
        verticalAlignment = Alignment.CenterVertically,
    )
}

@OptIn(ExperimentalAndroidComponentsApi::class)
@FlexibleWindowPreview
@Composable
private fun IPProtectionScreenActivePreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme = theme) {
        IPProtectionScreen(
            state = IPProtectionState(
                eligibilityStatus = EligibilityStatus.Eligible,
                proxyStatus = Authorized.Active,
                remainingDataBytes = 40 * BYTES_PER_GB.toLong(),
                maxDataBytes = 50 * BYTES_PER_GB.toLong(),
            ),
            snackbarHostState = SnackbarHostState(),
            readyToUse = true,
            syncingData = false,
            promoDate = null,
            onVpnToggle = {},
            onLearnMoreClick = {},
            onGetStartedClick = {},
            showDebugAction = false,
            onDebugActionClick = {},
            onNavigateBack = {},
            onLocationClicked = {},
        )
    }
}

@OptIn(ExperimentalAndroidComponentsApi::class)
@FlexibleWindowPreview
@Composable
private fun IPProtectionScreenNotEnrolledPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme = theme) {
        IPProtectionScreen(
            state = IPProtectionState(
                eligibilityStatus = EligibilityStatus.Eligible,
                serviceStatus = ServiceState.Unauthenticated,
                maxDataBytes = 0L,
            ),
            snackbarHostState = SnackbarHostState(),
            readyToUse = false,
            syncingData = true,
            promoDate = "January 31",
            onVpnToggle = {},
            onLearnMoreClick = {},
            onGetStartedClick = {},
            showDebugAction = false,
            onDebugActionClick = {},
            onNavigateBack = {},
            onLocationClicked = {},
        )
    }
}

@OptIn(ExperimentalAndroidComponentsApi::class)
@FlexibleWindowPreview
@Composable
private fun IPProtectionScreenPausedPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme = theme) {
        IPProtectionScreen(
            state = IPProtectionState(
                eligibilityStatus = EligibilityStatus.Eligible,
                proxyStatus = Authorized.DataLimitReached,
                maxDataBytes = 50 * BYTES_PER_GB.toLong(),
                remainingDataBytes = 0L,
            ),
            snackbarHostState = SnackbarHostState(),
            readyToUse = true,
            syncingData = false,
            promoDate = null,
            onVpnToggle = {},
            onLearnMoreClick = {},
            onGetStartedClick = {},
            showDebugAction = false,
            onDebugActionClick = {},
            onNavigateBack = {},
            onLocationClicked = {},
        )
    }
}

@OptIn(ExperimentalAndroidComponentsApi::class)
@FlexibleWindowPreview
@Composable
private fun IPProtectionScreenConnectingPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme = theme) {
        IPProtectionScreen(
            state = IPProtectionState(
                eligibilityStatus = EligibilityStatus.Eligible,
                proxyStatus = Uninitialized,
                remainingDataBytes = 40 * BYTES_PER_GB.toLong(),
                maxDataBytes = 50 * BYTES_PER_GB.toLong(),
            ),
            snackbarHostState = SnackbarHostState(),
            readyToUse = false,
            syncingData = false,
            promoDate = null,
            onVpnToggle = {},
            onLearnMoreClick = {},
            onGetStartedClick = {},
            showDebugAction = false,
            onDebugActionClick = {},
            onNavigateBack = {},
            onLocationClicked = {},
        )
    }
}
