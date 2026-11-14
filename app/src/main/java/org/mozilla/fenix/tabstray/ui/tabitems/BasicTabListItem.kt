/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.ui.tabitems

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import mozilla.components.support.ktx.util.URLStringUtils
import mozilla.components.ui.icons.R as iconsR
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.Favicon
import org.mozilla.fenix.compose.list.FaviconListItem
import org.mozilla.fenix.theme.FirefoxTheme

private val FaviconShape = RoundedCornerShape(2.dp)

/**
 * Basic tab list item used to display a Tab with its title, URL, and [Favicon] with an optional close icon button at
 * the end. Use [TabListTabItem] for displaying a Tab with a thumbnail.
 *
 * @param title The title of the tab.
 * @param url The url of the tab.
 * @param modifier [Modifier] to be applied to the layout.
 * @param faviconShape The shape used to clip the favicon. Defaults to a slightly rounded rectangle.
 * @param faviconPainter Optional painter to use when fetching a new favicon is unnecessary.
 * @param onClick Invoked when the user clicks on the tab.
 * @param showCloseButton Whether or not to show the close button.
 * @param onCloseButtonClick Invoked when the user clicks on the close button.
 */
@Composable
internal fun BasicTabListItem(
    title: String,
    url: String,
    modifier: Modifier = Modifier,
    faviconShape: Shape = FaviconShape,
    faviconPainter: Painter? = null,
    onClick: () -> Unit,
    showCloseButton: Boolean = false,
    onCloseButtonClick: (() -> Unit)? = null,
) {
    FaviconListItem(
        label = title,
        url = url,
        modifier = modifier,
        faviconShape = faviconShape,
        description = URLStringUtils.toLosslessDisplayUrl(url),
        faviconPainter = faviconPainter,
        onClick = onClick,
        iconPainter =
            if (showCloseButton) {
                painterResource(iconsR.drawable.mozac_ic_cross_24)
            } else {
                null
            },
        iconDescription = stringResource(R.string.tab_manager_close_tabs),
        onIconClick = onCloseButtonClick,
    )
}

@PreviewLightDark
@Composable
private fun TabItemPreview() {
    FirefoxTheme {
        Card {
            BasicTabListItem(
                title = "Mozilla",
                url = "mozilla.org/credits",
                onClick = {},
                showCloseButton = true,
                onCloseButtonClick = {},
            )

            HorizontalDivider()

            BasicTabListItem(
                title = "Google",
                url = "google.com",
                onClick = {},
                showCloseButton = true,
                onCloseButtonClick = {},
            )

            HorizontalDivider()

            BasicTabListItem(
                title = "Example Domain",
                url = "example.com",
                onClick = {},
                showCloseButton = false,
            )

            HorizontalDivider()

            BasicTabListItem(
                title = "www.google.com",
                url = "www.google.com",
                onClick = {},
                showCloseButton = false,
            )
        }
    }
}
