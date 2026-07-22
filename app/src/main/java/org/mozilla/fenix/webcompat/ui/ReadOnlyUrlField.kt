/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.webcompat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import org.mozilla.fenix.theme.FirefoxTheme
import mozilla.components.ui.icons.R as iconsR

/**
 * A custom list item that displays a URL with its base domain bolded.
 * The entire component acts as a clickable surface.
 *
 * @param url The full URL string to display.
 * @param baseDomain The exact base domain (e.g., "example.com") to be bolded within the [url].
 * @param label The text displayed above the URL container.
 * @param onClick Callback invoked when the URL container is clicked.
 * @param modifier [Modifier] to be applied to the layout.
 */
@Composable
fun ReadOnlyUrlField(
    url: String,
    baseDomain: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val annotatedUrl = remember(url, baseDomain) {
        buildAnnotatedString {
            append(url)

            val startIndex = url.indexOf(baseDomain)

            if (startIndex != -1) {
                addStyle(
                    style = SpanStyle(fontWeight = FontWeight.Bold),
                    start = startIndex,
                    end = startIndex + baseDomain.length,
                )
            }
        }
    }

    Column(
        modifier = modifier,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = FirefoxTheme.layout.space.static100),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            style = FirefoxTheme.typography.body2,
        )

        Surface(
            shape = RoundedCornerShape(FirefoxTheme.layout.space.static100),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = FirefoxTheme.layout.space.static50)
                .clip(RoundedCornerShape(FirefoxTheme.layout.space.static100))
                .clickable(
                    onClick = onClick,
                    role = Role.Button,
                ),
        ) {
            Row(
                modifier = Modifier.padding(
                    vertical = FirefoxTheme.layout.space.static100,
                    horizontal = FirefoxTheme.layout.space.static200,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = annotatedUrl,
                    modifier = Modifier.weight(1f),
                    style = FirefoxTheme.typography.body1,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.width(FirefoxTheme.layout.space.static200))

                Icon(
                    painter = painterResource(id = iconsR.drawable.mozac_ic_edit_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
@PreviewLightDark
private fun ReadOnlyUrlFieldPreview() {
    FirefoxTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(FirefoxTheme.layout.space.static150),
        ) {
            Column {
                ReadOnlyUrlField(
                    url = "https://www.houseandhome.com/recipe/croque-monsieur/",
                    baseDomain = "houseandhome.com",
                    label = "URL",
                    onClick = {},
                )

                Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.static200))

                ReadOnlyUrlField(
                    url = "example.com/missing-scheme",
                    baseDomain = "example.com",
                    label = "URL Without Scheme",
                    onClick = {},
                )
            }
        }
    }
}
