/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.menu.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mozilla.components.compose.base.annotation.FlexibleWindowLightDarkPreview
import org.mozilla.fenix.R
import org.mozilla.fenix.components.menu.store.IPProtectionMenuState
import org.mozilla.fenix.components.menu.store.IPProtectionMenuStatus
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.PreviewThemeProvider
import org.mozilla.fenix.theme.Theme
import mozilla.components.ui.icons.R as iconsR

private val MENU_ITEM_MIN_HEIGHT = 52.dp

/**
 * A menu item showing the current IP Protection status.
 *
 * State is driven externally via [IPProtectionMenuState] so that the caller (menu fragment)
 * can subscribe to the IP protection feature store and dispatch updates.
 *
 * @param state The current [IPProtectionMenuState] to display.
 * @param onToggle Called when the label row is tapped to toggle IP Protection on or off.
 * @param onNavigate Called when the chevron is tapped to open the IP Protection settings screen.
 */
@Composable
internal fun IPProtectionMenuItem(
    state: IPProtectionMenuState,
    onToggle: () -> Unit,
    onNavigate: () -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Row(
            modifier = Modifier
                .wrapContentSize()
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceBright)
                .height(IntrinsicSize.Min)
                .defaultMinSize(minHeight = MENU_ITEM_MIN_HEIGHT),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IPProtectionToggle(
                state = state,
                onToggle = onToggle,
                modifier = Modifier.weight(1f),
            )

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = FirefoxTheme.layout.space.static100)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable(role = Role.Button, onClick = onNavigate)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconsR.drawable.mozac_ic_chevron_right_24),
                    contentDescription = stringResource(R.string.ip_protection_navigate_settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun IPProtectionToggle(
    state: IPProtectionMenuState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusDescription = badgeText(state.status)

    Row(
        modifier = modifier
            .fillMaxHeight()
            .clickable(role = Role.Button) { onToggle() }
            .semantics {
                stateDescription = statusDescription
                liveRegion = LiveRegionMode.Polite
            }
            .padding(horizontal = FirefoxTheme.layout.space.dynamic200),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FirefoxTheme.layout.space.static200),
    ) {
        Icon(
            painter = painterResource(iconsR.drawable.mozac_ic_globe_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        IPProtectionLabel(
            state = state,
            modifier = Modifier.weight(1f),
        )

        Badge(
            badgeText = statusDescription,
            state = badgeState(state.status),
        )
    }
}

@Composable
private fun IPProtectionLabel(
    state: IPProtectionMenuState,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.ip_protection_toggle_label)
    val labelStyle = FirefoxTheme.typography.subtitle1
    val textMeasurer = rememberTextMeasurer()
    val textLayout = remember(label, labelStyle) {
        textMeasurer.measure(text = label, style = labelStyle).size.width
    }

    Column(
        modifier = modifier.layout { measurable, constraints ->
            val placeable = if (constraints.maxWidth >= textLayout) {
                measurable.measure(constraints)
            } else {
                null
            }

            layout(width = placeable?.width ?: 0, height = placeable?.height ?: 0) {
                placeable?.placeRelative(0, 0)
            }
        },
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            style = labelStyle,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )

        if (state.status == IPProtectionMenuStatus.DataLimitReached && state.dataLimitGb > 0) {
            Text(
                text = stringResource(R.string.ip_protection_menu_limit_reached, state.dataLimitGb),
                color = MaterialTheme.colorScheme.error,
                style = FirefoxTheme.typography.caption,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun badgeText(status: IPProtectionMenuStatus): String = when (status) {
    IPProtectionMenuStatus.Disabled -> stringResource(R.string.preferences_ip_protection_off)
    IPProtectionMenuStatus.Enabled -> stringResource(R.string.preferences_ip_protection_on)
    IPProtectionMenuStatus.Activating -> stringResource(R.string.ip_protection_menu_connecting)
    IPProtectionMenuStatus.DataLimitReached -> stringResource(R.string.ip_protection_menu_paused)
    IPProtectionMenuStatus.ConnectionError -> stringResource(R.string.ip_protection_menu_error)
    IPProtectionMenuStatus.AuthRequired -> stringResource(R.string.ip_protection_menu_auth_required)
}

private fun badgeState(status: IPProtectionMenuStatus): MenuItemState = when (status) {
    IPProtectionMenuStatus.Enabled,
    IPProtectionMenuStatus.Activating,
    -> MenuItemState.ACTIVE
    IPProtectionMenuStatus.ConnectionError -> MenuItemState.WARNING
    IPProtectionMenuStatus.DataLimitReached -> MenuItemState.DISABLED
    IPProtectionMenuStatus.Disabled,
    IPProtectionMenuStatus.AuthRequired,
    -> MenuItemState.ENABLED
}

@FlexibleWindowLightDarkPreview
@Composable
private fun IPProtectionMenuItemOffPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme = theme) {
        MenuGroup {
            IPProtectionMenuItem(
                state = IPProtectionMenuState(status = IPProtectionMenuStatus.Disabled),
                onToggle = {},
                onNavigate = {},
            )
        }
    }
}

@FlexibleWindowLightDarkPreview
@Composable
private fun IPProtectionMenuItemOnPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme = theme) {
        MenuGroup {
            IPProtectionMenuItem(
                state = IPProtectionMenuState(status = IPProtectionMenuStatus.Enabled),
                onToggle = {},
                onNavigate = {},
            )
        }
    }
}

@FlexibleWindowLightDarkPreview
@Composable
private fun IPProtectionMenuItemConnectingPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme = theme) {
        MenuGroup {
            IPProtectionMenuItem(
                state = IPProtectionMenuState(status = IPProtectionMenuStatus.Activating),
                onToggle = {},
                onNavigate = {},
            )
        }
    }
}

@FlexibleWindowLightDarkPreview
@Composable
private fun IPProtectionMenuItemPausedPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme = theme) {
        MenuGroup {
            IPProtectionMenuItem(
                state = IPProtectionMenuState(
                    status = IPProtectionMenuStatus.DataLimitReached,
                    dataLimitGb = 50,
                ),
                onToggle = {},
                onNavigate = {},
            )
        }
    }
}
