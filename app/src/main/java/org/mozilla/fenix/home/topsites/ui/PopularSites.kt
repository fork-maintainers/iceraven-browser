/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.topsites.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mozilla.components.compose.base.annotation.FlexibleWindowLightDarkPreview
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.Favicon
import org.mozilla.fenix.home.topsites.TOP_SITES_FAVICON_CARD_SIZE
import org.mozilla.fenix.home.topsites.TOP_SITES_FAVICON_SIZE
import org.mozilla.fenix.home.topsites.TOP_SITES_ITEM_SIZE
import org.mozilla.fenix.home.topsites.TOP_SITES_PER_ROW
import org.mozilla.fenix.home.topsites.store.PopularSite
import org.mozilla.fenix.theme.FirefoxTheme
import mozilla.components.ui.icons.R as iconsR

private const val BADGE_SIZE = 16
private const val PREVIEW_SITE_COUNT = 8

// Fixed height of a single popular site cell:
// Favicon card (60dp) + Spacing (static100, 8dp) + Title (2 lines, ~36dp).
private val POPULAR_SITES_ITEM_HEIGHT = 104.dp

@Composable
internal fun PopularSites(
    sites: List<PopularSite>,
    onClick: (PopularSite) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Column(modifier = Modifier.padding(FirefoxTheme.layout.space.static200)) {
            Text(
                text = stringResource(R.string.homepage_shortcuts_popular_sites),
                style = FirefoxTheme.typography.headline8,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.static200))

            val rowCount = (sites.size + TOP_SITES_PER_ROW - 1) / TOP_SITES_PER_ROW
            val rowSpacing = FirefoxTheme.layout.space.static100
            val gridHeight = POPULAR_SITES_ITEM_HEIGHT * rowCount +
                rowSpacing * (rowCount - 1).coerceAtLeast(0)

            LazyVerticalGrid(
                columns = GridCells.Fixed(TOP_SITES_PER_ROW),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight),
                verticalArrangement = Arrangement.spacedBy(rowSpacing),
                horizontalArrangement = Arrangement.Center,
                userScrollEnabled = false,
            ) {
                items(sites) { site ->
                    PopularSiteItem(
                        site = site,
                        onClick = { onClick(site) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PopularSiteItem(
    site: PopularSite,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .width(TOP_SITES_ITEM_SIZE.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            FaviconCard(
                url = site.url,
                imageUrl = site.iconUrl,
            )

            AddBadge()
        }

        Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.static100))

        Text(
            text = site.title,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
            style = FirefoxTheme.typography.caption.copy(fontWeight = FontWeight.W700),
        )
    }
}

@Composable
private fun FaviconCard(
    url: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.size(TOP_SITES_FAVICON_CARD_SIZE.dp),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(
            width = FirefoxTheme.layout.border.default,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.size(TOP_SITES_FAVICON_SIZE.dp),
                color = MaterialTheme.colorScheme.surfaceDim,
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                Favicon(
                    url = url,
                    size = TOP_SITES_FAVICON_SIZE.dp,
                    imageUrl = imageUrl,
                )
            }
        }
    }
}

@Composable
private fun AddBadge() {
    Card(
        modifier = Modifier.size(BADGE_SIZE.dp),
        shape = CircleShape,
        border = BorderStroke(
            width = FirefoxTheme.layout.border.heavy,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(FirefoxTheme.layout.size.static150),
                painter = painterResource(iconsR.drawable.mozac_ic_plus_24),
                contentDescription = null,
            )
        }
    }
}

@Composable
@FlexibleWindowLightDarkPreview
private fun PopularSitesPreview() {
    FirefoxTheme {
        Surface {
            PopularSites(
                sites = List(PREVIEW_SITE_COUNT) {
                    PopularSite(title = "Mozilla", url = "https://mozilla.com", iconUrl = null)
                },
                onClick = {},
            )
        }
    }
}
