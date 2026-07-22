/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.webcompat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mozilla.components.compose.base.theme.AcornCorners
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.PreviewThemeProvider
import org.mozilla.fenix.theme.Theme
import org.mozilla.fenix.webcompat.BrokenSiteReporterTestTags.BROKEN_SITE_REPORTER_REASON_OPTION
import org.mozilla.fenix.webcompat.store.WebCompatReporterState.BrokenSiteReason

/**
 * A clickable list item used to display a single reason in the webcompat reporter.
 *
 * @param text The text to display inside the list item.
 * @param modifier [Modifier] applied to the list item content.
 * @param onClick Callback invoked when the user clicks on this item.
 * @param shape The shape applied to the list item container.
 * @param iconPainter Optional icon displayed at the end of the list item.
 * @param iconDescription Content description for the optional trailing icon.
 * @param onIconClick Callback invoked when the optional trailing icon is clicked.
 */
@Composable
fun BrokenSiteReasonListItem(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(AcornCorners.extraSmall),
    iconPainter: Painter? = null,
    iconDescription: String? = null,
    onIconClick: (() -> Unit)? = null,
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .semantics {
                    testTagsAsResourceId = true
                    testTag = "$BROKEN_SITE_REPORTER_REASON_OPTION-$text"
                }
                .defaultMinSize(minHeight = 56.dp)
                .padding(
                    horizontal = FirefoxTheme.layout.space.dynamic200,
                    vertical = FirefoxTheme.layout.space.static100,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = FirefoxTheme.typography.body1,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )

            if (iconPainter != null) {
                Icon(
                    painter = iconPainter,
                    contentDescription = iconDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = FirefoxTheme.layout.space.static100)
                        .clip(CircleShape)
                        .clickable(
                            enabled = onIconClick != null,
                            onClick = { onIconClick?.invoke() },
                        )
                        .padding(FirefoxTheme.layout.space.static100),
                )
            }
        }
    }
}

@Preview
@Composable
private fun WebCompatReporterBrokenSiteReasonListItemPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                verticalArrangement = Arrangement.spacedBy(FirefoxTheme.layout.space.static100),
            ) {
                BrokenSiteReason.entries.forEach { reason ->
                    BrokenSiteReasonListItem(
                        text = stringResource(reason.displayStringId),
                        onClick = {},
                    )
                }
            }
        }
    }
}
