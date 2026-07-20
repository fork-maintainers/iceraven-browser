/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.home.ui.HomepageTestTag.PRIVATE_BROWSING_HOMEPAGE_BUTTON
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.PreviewThemeProvider
import org.mozilla.fenix.theme.Theme
import org.mozilla.fenix.wallpapers.WallpaperTheme
import mozilla.components.ui.icons.R as iconsR

/**
 * Header for the homepage.
 */
@Composable
fun HomepageHeader(
    browsingMode: BrowsingMode,
    browsingModeChanged: (BrowsingMode) -> Unit,
) {
    // In normal mode the wordmark and private-browsing button follow the wallpaper text color; in
    // private mode there is no wallpaper, so the wordmark is untinted and the button uses its
    // dedicated private-mode icon color.
    val wordmarkTextColor = if (browsingMode.isPrivate) {
        null
    } else {
        WallpaperTheme.onWallpaper
    }
    val privateBrowsingButtonColor = if (browsingMode.isPrivate) {
        colorResource(getAttr(iconsR.attr.mozac_ic_private_mode_circle_fill_icon_color))
    } else {
        WallpaperTheme.onWallpaper
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WordmarkLogo()

        WordmarkText(wordmarkTextColor)

        Spacer(modifier = Modifier.weight(1f))

        PrivateBrowsingButton(
            color = privateBrowsingButtonColor,
            browsingMode = browsingMode,
            browsingModeChanged = browsingModeChanged,
        )
    }
}

@Composable
private fun PrivateBrowsingButton(
    color: Color,
    browsingMode: BrowsingMode,
    browsingModeChanged: (BrowsingMode) -> Unit,
) {
    IconToggleButton(
        modifier = Modifier
            .background(
                color = colorResource(getAttr(iconsR.attr.mozac_ic_private_mode_circle_fill_background_color)),
                shape = CircleShape,
            )
            .size(40.dp)
            .semantics {
                role = Role.Switch
                testTagsAsResourceId = true
                testTag = PRIVATE_BROWSING_HOMEPAGE_BUTTON
            },
        checked = browsingMode.isPrivate,
        onCheckedChange = {
            browsingModeChanged(BrowsingMode.fromBoolean(!browsingMode.isPrivate))
        },
    ) {
        Icon(
            tint = color,
            painter = painterResource(iconsR.drawable.mozac_ic_private_mode_fill_24),
            contentDescription = stringResource(R.string.content_description_private_browsing),
        )
    }
}

@Composable
internal fun getAttr(resId: Int): Int {
    val typedArray = LocalContext.current.obtainStyledAttributes(intArrayOf(resId))
    val newResId = typedArray.getResourceId(0, 0)
    typedArray.recycle()

    return newResId
}

@Preview
@Composable
private fun HomepageHeaderPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme) {
        Surface {
            HomepageHeader(
                browsingMode = BrowsingMode.Normal,
                browsingModeChanged = {},
            )
        }
    }
}
