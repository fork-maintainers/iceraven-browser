/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.collections

import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import mozilla.components.compose.base.LinkTextState
import mozilla.components.compose.base.PromoCard
import mozilla.components.compose.base.PromoCardColors
import org.mozilla.fenix.R
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * Collections to tab groups migration card.
 *
 * @param onClick Invoked when the user clicks on the "View my tab groups" link.
 * @param modifier The [Modifier] to be applied to the [PromoCard].
 */
@Composable
fun CollectionsMigrationPromoCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkText = stringResource(R.string.collections_migration_homepage_card_link)

    PromoCard(
        description = stringResource(R.string.collections_migration_homepage_card_message),
        modifier = modifier,
        title = stringResource(R.string.collections_migration_homepage_banner_title),
        footer = linkText to LinkTextState(
            text = linkText,
            url = "",
            onClick = { onClick() },
        ),
        illustration = {
            Image(
                painter = painterResource(R.drawable.mozac_ic_kit_tab_groups),
                contentDescription = null,
            )
        },
        verticalAlignment = Alignment.CenterVertically,
        colors = PromoCardColors.promoCardColors(
            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
            actionsTextColor = MaterialTheme.colorScheme.tertiary,
        ),
    )
}

@Composable
@PreviewLightDark
private fun CollectionsMigrationPromoCardPreview() {
    FirefoxTheme {
        CollectionsMigrationPromoCard(
            onClick = {},
        )
    }
}
