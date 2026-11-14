/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.menu.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.mozilla.fenix.R
import org.mozilla.fenix.theme.FirefoxTheme

@Composable
internal fun CustomizeMenuPopup(
    popupOffset: Offset?,
    onDismissRequest: () -> Unit,
    onCustomizeMenuClick: () -> Unit,
) {
    val density = LocalDensity.current
    val defaultY = with(density) { 100.dp.toPx() }
    val offsetAdjustment = with(density) { 40.dp.toPx() }

    Popup(
        alignment = Alignment.TopCenter,
        offset =
            IntOffset(
                x = 0,
                y = ((popupOffset?.y ?: defaultY) - offsetAdjustment).toInt(),
            ),
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true, dismissOnClickOutside = true),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceBright,
            shadowElevation = 6.dp,
            modifier = Modifier.clickable(onClick = onCustomizeMenuClick),
        ) {
            Text(
                text = stringResource(R.string.customize_menu),
                style = FirefoxTheme.typography.body1,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(FirefoxTheme.layout.space.static150),
            )
        }
    }
}
