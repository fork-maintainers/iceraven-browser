/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.menu.compose.header

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mozilla.components.compose.base.theme.ThemedValue
import mozilla.components.compose.base.theme.ThemedValueProvider
import mozilla.components.service.fxa.manager.AccountState
import mozilla.components.service.fxa.manager.AccountState.Authenticated
import mozilla.components.service.fxa.manager.AccountState.AuthenticationProblem
import mozilla.components.service.fxa.manager.AccountState.NotAuthenticated
import mozilla.components.service.fxa.manager.AccountState.Unknown
import mozilla.components.service.fxa.store.Account
import mozilla.components.ui.icons.R as iconsR
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.Image
import org.mozilla.fenix.theme.FirefoxTheme

private val BUTTON_HEIGHT = 56.dp
private val AVATAR_SIZE = 24.dp

@SuppressWarnings("LongMethod")
@Composable
internal fun MozillaAccountMenuItem(
    account: Account?,
    accountState: AccountState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label: String
    val description: String
    val contentDescription: String

    when (accountState) {
        NotAuthenticated,
        Unknown -> {
            label = stringResource(id = R.string.browser_menu_sign_in)
            description = stringResource(id = R.string.browser_menu_sign_in_caption_3)
        }

        AuthenticationProblem -> {
            label =
                account?.displayName
                    ?: account?.email
                    ?: stringResource(id = R.string.browser_menu_sign_back_in_to_sync)
            description = stringResource(id = R.string.browser_menu_syncing_paused_caption)
        }

        Authenticated -> {
            label =
                account?.displayName ?: account?.email ?: stringResource(id = R.string.browser_menu_account_settings)
            description = stringResource(id = R.string.browser_menu_signed_in_caption)
        }
    }

    contentDescription = "$label $description"

    Row(
        modifier =
            modifier
                .clearAndSetSemantics {
                    role = Role.Button
                    this.contentDescription = contentDescription
                }
                .wrapContentSize()
                .clip(MaterialTheme.shapes.extraSmall)
                .background(color = MaterialTheme.colorScheme.surfaceBright)
                .height(IntrinsicSize.Min)
                .defaultMinSize(minHeight = BUTTON_HEIGHT)
                .clickable { onClick() }
                .padding(
                    horizontal = FirefoxTheme.layout.space.dynamic200,
                    vertical = FirefoxTheme.layout.space.static100,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            AvatarIcon(
                account = account,
                accountState = accountState,
            )

            Spacer(modifier = Modifier.width(FirefoxTheme.layout.space.static200))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    overflow = TextOverflow.Ellipsis,
                    style = FirefoxTheme.typography.body1,
                    maxLines = 2,
                )

                Text(
                    text = description,
                    color =
                        if (accountState is AuthenticationProblem) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                    style = FirefoxTheme.typography.caption,
                )
            }
        }
    }
}

@Composable
private fun FallbackAvatarIcon() {
    Icon(
        painter = painterResource(id = iconsR.drawable.mozac_ic_avatar_circle_24),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun WarningAvatarIcon() {
    Icon(
        painter = painterResource(id = iconsR.drawable.mozac_ic_avatar_warning_circle_fill_multicolor_24),
        contentDescription = null,
        tint = Color.Unspecified,
    )
}

@Composable
private fun AvatarIcon(
    account: Account?,
    accountState: AccountState,
) {
    if (accountState is AuthenticationProblem) {
        WarningAvatarIcon()
    } else {
        val avatarUrl = account?.avatar?.url

        if (avatarUrl != null) {
            Image(
                url = avatarUrl,
                modifier = Modifier.clip(CircleShape).size(AVATAR_SIZE),
                targetSize = AVATAR_SIZE,
                placeholder = { FallbackAvatarIcon() },
                fallback = { FallbackAvatarIcon() },
            )
        } else {
            FallbackAvatarIcon()
        }
    }
}

private data class MozillaAccountMenuItemPreviewState(
    val account: Account?,
    val accountState: AccountState,
    val isPrivate: Boolean,
)

private class MozillaAccountMenuItemPreviewProvider :
    ThemedValueProvider<MozillaAccountMenuItemPreviewState>(
        sequenceOf(
            MozillaAccountMenuItemPreviewState(
                account = null,
                accountState = NotAuthenticated,
                isPrivate = false,
            ),
            MozillaAccountMenuItemPreviewState(
                account = null,
                accountState = AuthenticationProblem,
                isPrivate = true,
            ),
            MozillaAccountMenuItemPreviewState(
                account =
                    Account(
                        uid = "testUID",
                        email = "test@example.com",
                        avatar = null,
                        displayName = "test profile",
                    ),
                accountState = Authenticated,
                isPrivate = false,
            ),
            MozillaAccountMenuItemPreviewState(
                account =
                    Account(
                        uid = "testUID",
                        email = "test@example.com",
                        avatar = null,
                        displayName = null,
                    ),
                accountState = Authenticated,
                isPrivate = false,
            ),
            MozillaAccountMenuItemPreviewState(
                account =
                    Account(
                        uid = "testUID",
                        email = null,
                        avatar = null,
                        displayName = null,
                    ),
                accountState = Authenticated,
                isPrivate = false,
            ),
        ),
        displayNames =
            listOf(
                "Signed out",
                "Authentication problem (private)",
                "Authenticated with display name",
                "Authenticated with email",
                "Authenticated without details",
            ),
    )

@Preview
@Composable
private fun MozillaAccountMenuItemPreview(
    @PreviewParameter(MozillaAccountMenuItemPreviewProvider::class)
    state: ThemedValue<MozillaAccountMenuItemPreviewState>
) {
    FirefoxTheme(state.theme) {
        Surface {
            Column(modifier = Modifier.padding(all = FirefoxTheme.layout.space.static200)) {
                MozillaAccountMenuItem(
                    account = state.value.account,
                    accountState = state.value.accountState,
                    onClick = {},
                )
            }
        }
    }
}
