/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.compose

import androidx.compose.animation.core.EaseInOut
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

private const val FADE_STEPS = 8

/**
 * Fades the content out towards its top and bottom edges, so that content clipped at those edges dissolves into
 * whatever is drawn behind it instead of ending on a hard line.
 *
 * @param fadeLength The distance over which the content fades out at each edge.
 */
fun Modifier.verticalFadingEdges(fadeLength: Dp): Modifier = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}
    .drawWithCache {
        val fadeHeight = fadeLength.toPx().coerceAtMost(size.height / 2)
        val fadeSize = Size(size.width, fadeHeight)
        // The alpha follows an ease-in-out curve across the fade, so it eases out of the solid content and tails off
        // gently towards transparent instead of changing at a constant rate.
        val fadeColors =
            (0..FADE_STEPS).map { step ->
                Color.Black.copy(alpha = EaseInOut.transform(step.toFloat() / FADE_STEPS))
            }
        val topMask = Brush.verticalGradient(fadeColors, startY = 0f, endY = fadeHeight)
        val bottomMask =
            Brush.verticalGradient(fadeColors.reversed(), startY = size.height - fadeHeight, endY = size.height)

        onDrawWithContent {
            drawContent()
            drawRect(brush = topMask, size = fadeSize, blendMode = BlendMode.DstIn)
            drawRect(
                brush = bottomMask,
                topLeft = Offset(0f, size.height - fadeHeight),
                size = fadeSize,
                blendMode = BlendMode.DstIn,
            )
        }
    }
