/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag

internal const val BOTTOM_CHROME_DIVIDER_TEST_TAG = "bottom_chrome_divider"

/**
 * A [Column] that can show a divider over its top edge without adding to its height.
 *
 * @param showDivider Whether to show the divider.
 * @param modifier [Modifier] to be applied to the layout.
 * @param dividerColor Color of the divider.
 * @param content The content of the [Column].
 */
@Composable
fun TopDividerColumn(
    showDivider: Boolean,
    modifier: Modifier = Modifier,
    dividerColor: Color = DividerDefaults.color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier, propagateMinConstraints = true) {
        Column(content = content)

        if (showDivider) {
            // This inner box helps avoid a divider when the entire column is empty - as when the keyboard is up
            // and the navigation bar is hidden.
            Box(modifier = Modifier.matchParentSize().clipToBounds()) {
                HorizontalDivider(
                    modifier = Modifier.align(Alignment.TopCenter).testTag(BOTTOM_CHROME_DIVIDER_TEST_TAG),
                    color = dividerColor,
                )
            }
        }
    }
}
