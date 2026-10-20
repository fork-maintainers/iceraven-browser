/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.ui.tabitems

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import kotlin.math.abs
import mozilla.components.compose.base.modifier.thenConditional
import mozilla.components.compose.base.theme.AcornCorners
import mozilla.components.compose.draggable.ItemInteractionState
import org.mozilla.fenix.theme.FirefoxTheme

/** Object holding alpha values for tab items */
object Alpha {
    const val TAB_ITEM_DRAGGED = 0.7f
    const val TAB_ITEM_NO_INTERACTION = 1f
    const val TAB_ITEM_MIN_SWIPE_FADE = 0.1f
}

/** Animates the tab item's alpha value to be slightly transparent when it is dragged. */
@Composable
private fun tabGridItemAnimatedAlpha(interactionState: ItemInteractionState): State<Float> {
    return animateFloatAsState(
        targetValue =
            if (interactionState.isDragged) {
                Alpha.TAB_ITEM_DRAGGED
            } else {
                Alpha.TAB_ITEM_NO_INTERACTION
            },
        label = "TabGridItemAlpha",
    )
}

/** Animates the tab item's alpha value to be slightly transparent when it is dragged, after being moved. */
@Composable
private fun tabListItemAnimatedAlpha(interactionState: ItemInteractionState): State<Float> {
    return animateFloatAsState(
        targetValue =
            if (interactionState.isDragged && !interactionState.isHeld) {
                Alpha.TAB_ITEM_DRAGGED
            } else {
                Alpha.TAB_ITEM_NO_INTERACTION
            },
        label = "TabListItemAlpha",
    )
}

/** Animates the tab item's size to be slightly reduced when it is dragged. */
@Composable
private fun tabGridItemAnimatedScale(interactionState: ItemInteractionState): State<Float> {
    val targetValue =
        when {
            interactionState.isDragged -> Scale.DRAG_ACTIVE
            interactionState.isHoveredByItem -> Scale.HOVER_ACTIVE
            else -> Scale.NO_INTERACTION
        }
    return animateFloatAsState(
        targetValue = targetValue,
        label = "TabGridItemScale",
    )
}

/** Animates the tab item's size to be slightly reduced when it is dragged, after being moved. */
@Composable
private fun tabListItemAnimatedScale(interactionState: ItemInteractionState): State<Float> {
    val targetValue =
        when {
            interactionState.isHeld -> Scale.NO_INTERACTION
            interactionState.isDragged -> Scale.DRAG_ACTIVE
            interactionState.isHoveredByItem -> Scale.HOVER_ACTIVE_LIST
            else -> Scale.NO_INTERACTION
        }
    return animateFloatAsState(
        targetValue = targetValue,
        label = "TabListItemScale",
    )
}

/**
 * Renders an animated scale and alpha transition for the tab grid item based on its interaction state. This happens at
 * the graphics layer to avoid recomposition of the item. The semantics properties are provided so that the state can be
 * evaluated, as evaluating the composable will not return the correct result, since these graphical animations occur at
 * draw time. The list and grid animations differ slightly in terms of scale and corner radius.
 *
 * @param interactionState: State holding the hovered and dragged statuses.
 */
@Composable
fun Modifier.tabItemGridInteractionAnimation(interactionState: ItemInteractionState): Modifier {
    return this.tabItemInteractionAnimation(
        tabItemScaleState = tabGridItemAnimatedScale(interactionState),
        tabItemAlphaState = tabGridItemAnimatedAlpha(interactionState),
        cornerSize = AcornCorners.large,
        interactionState = interactionState,
    )
}

/**
 * Renders an animated scale and alpha transition for the tab list group item based on its interaction state. This
 * happens at the graphics layer to avoid recomposition of the item. The semantics properties are provided so that the
 * state can be evaluated, as evaluating the composable will not return the correct result, since these graphical
 * animations occur at draw time. The list and grid animations differ slightly in terms of scale and corner radius.
 *
 * @param interactionState: State holding the hovered and dragged statuses.
 * @param key The item's key
 * @param onGroupEntranceAnimationPlayed Invoked when the group's entrance animation is played.
 */
@Composable
fun Modifier.tabItemGroupListInteractionAnimation(
    interactionState: ItemInteractionState,
    key: String? = null,
    onGroupEntranceAnimationPlayed: () -> Unit = {},
): Modifier {
    val interactionScale = tabListItemAnimatedScale(interactionState)
    val entranceScale =
        tabGroupAppearanceScale(
            interactionState = interactionState,
            key = key,
            onGroupEntranceAnimationPlayed = onGroupEntranceAnimationPlayed,
        )
    val combinedScale = remember { derivedStateOf { interactionScale.value * entranceScale.value } }
    return this.tabItemInteractionAnimation(
        tabItemScaleState = combinedScale,
        tabItemAlphaState = tabListItemAnimatedAlpha(interactionState),
        cornerSize = AcornCorners.medium,
        interactionState = interactionState,
    )
}

/**
 * Renders an animated scale and alpha transition for the tab list item based on its interaction state. This happens at
 * the graphics layer to avoid recomposition of the item. The semantics properties are provided so that the state can be
 * evaluated, as evaluating the composable will not return the correct result, since these graphical animations occur at
 * draw time. The list and grid animations differ slightly in terms of scale and corner radius.
 *
 * @param interactionState: State holding the hovered and dragged statuses.
 */
@Composable
fun Modifier.tabItemListInteractionAnimation(interactionState: ItemInteractionState): Modifier {
    return this.tabItemInteractionAnimation(
        tabItemScaleState = tabListItemAnimatedScale(interactionState),
        tabItemAlphaState = tabListItemAnimatedAlpha(interactionState),
        cornerSize = AcornCorners.medium,
        interactionState = interactionState,
    )
}

/**
 * Provides the alpha state for the tab group appearance animation.
 *
 * @param interactionState The tab interaction state
 * @param key The tab item's key
 */
@Composable
private fun tabGroupAppearanceAlpha(
    interactionState: ItemInteractionState,
    key: String?,
): State<Float> {
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(key, interactionState.isEntering) {
        if (interactionState.isEntering) {
            alpha.snapTo(targetValue = 0f)
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
            )
        }
    }
    return alpha.asState()
}

/**
 * Provides the scale state for the tab group appearance animation.
 *
 * @param interactionState The tab interaction state
 * @param key The tab item's key
 * @param onGroupEntranceAnimationPlayed Invoked when the animation is finished playing
 */
@Composable
private fun tabGroupAppearanceScale(
    interactionState: ItemInteractionState,
    key: String?,
    onGroupEntranceAnimationPlayed: () -> Unit,
): State<Float> {
    // This must be the default, else all group items will be incorrectly scaled
    val scale = remember { Animatable(Scale.NO_INTERACTION) }
    LaunchedEffect(key, interactionState.isEntering) {
        if (interactionState.isEntering) {
            scale.snapTo(targetValue = Scale.NEW_GROUP_ENTRANCE_START)
            scale.animateTo(
                targetValue = Scale.NEW_GROUP_ENTRANCE_PEAK,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
            )
            scale.animateTo(
                targetValue = Scale.NO_INTERACTION,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
            )
            onGroupEntranceAnimationPlayed()
        }
    }
    return scale.asState()
}

/**
 * Plays the entrance animation for a tab group.
 *
 * @param interactionState The tab item's interaction state
 * @param key The tab item's key
 * @param onGroupEntranceAnimationPlayed Invoked when the animation is finished playing.
 */
@Composable
internal fun Modifier.tabGroupEntranceAnimation(
    interactionState: ItemInteractionState,
    key: String?,
    onGroupEntranceAnimationPlayed: () -> Unit,
): Modifier {
    if (!interactionState.isEntering) return this
    val entranceScale =
        tabGroupAppearanceScale(
            interactionState = interactionState,
            key = key,
            onGroupEntranceAnimationPlayed = onGroupEntranceAnimationPlayed,
        )
    val entranceAlpha = tabGroupAppearanceAlpha(interactionState = interactionState, key = key)
    return this.graphicsLayer {
        scaleX = entranceScale.value
        scaleY = entranceScale.value
        alpha = entranceAlpha.value
    }
}

/**
 * Renders an animated scale and alpha transition for the tab item based on its interaction state. This happens at the
 * graphics layer to avoid recomposition of the item. The semantics properties are provided so that the state can be
 * evaluated, as evaluating the composable will not return the correct result, since these graphical animations occur at
 * draw time.
 */
@Composable
private fun Modifier.tabItemInteractionAnimation(
    tabItemAlphaState: State<Float>,
    tabItemScaleState: State<Float>,
    cornerSize: Dp,
    interactionState: ItemInteractionState,
): Modifier {
    val backdropColor = MaterialTheme.colorScheme.secondaryContainer
    val backdropBorder = MaterialTheme.colorScheme.tertiary
    val borderSize = FirefoxTheme.layout.border.heaviest
    return this.thenConditional(
            Modifier.drawBehind({
                // A Stroke rect is centered on the shape edge, which will spill outside the drawing area
                // To make the border match other tabs, we must inset by half the stroke width, and adjust the size
                val inset = borderSize.toPx() / 2
                val shapeOffset = Offset(x = inset, y = inset)
                val shapeSize = Size(this.size.width - shapeOffset.x * 2, this.size.height - shapeOffset.y * 2)
                drawRoundRect(
                    color = backdropColor,
                    topLeft = shapeOffset,
                    size = shapeSize,
                    cornerRadius = CornerRadius(cornerSize.toPx()),
                )
                drawRoundRect(
                    color = backdropBorder,
                    topLeft = shapeOffset,
                    size = shapeSize,
                    cornerRadius = CornerRadius(cornerSize.toPx()),
                    style = Stroke(width = borderSize.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }),
            { interactionState.isHoveredByItem },
        )
        .graphicsLayer {
            alpha = tabItemAlphaState.value
            scaleX = tabItemScaleState.value
            scaleY = tabItemScaleState.value
        }
        .semantics {
            scale = tabItemScaleState.value
            alpha = tabItemAlphaState.value
        }
}

/**
 * Creates a [SwipeToDismissBoxState] for the tab item identified by [tabId].
 *
 * Deliberately not [androidx.compose.material3.rememberSwipeToDismissBoxState], which saves its current value: a lazy
 * layout keeps an item's saved state around after the item leaves the list, so a tab restored through the undo snackbar
 * would return as swiped away and be dismissed again on its first composition.
 *
 * @param tabId The id of the tab the state belongs to.
 */
@Composable
fun rememberTabSwipeToDismissBoxState(tabId: String): SwipeToDismissBoxState {
    val positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold

    return remember(tabId) {
        SwipeToDismissBoxState(
            initialValue = SwipeToDismissBoxValue.Settled,
            positionalThreshold = positionalThreshold,
        )
    }
}

/**
 * Custom modifier that fades an item to transparent as it is swiped to dismiss. The minimum alpha is 10%, so the item
 * will at least be 10% visible.
 */
fun Modifier.fadeOnSwipeToDismiss(state: SwipeToDismissBoxState) = graphicsLayer {
    // state.progress is tied to targetValue which has a fixed threshold,
    // so we need to pull the offset to get a linear fade animation.
    val offset =
        try {
            if (state.dismissDirection == SwipeToDismissBoxValue.Settled) {
                0f
            } else {
                state.requireOffset()
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            // It should be safe to call requireOffset() here, but there's no
            // reason to risk a crash for a fade animation.
            0f
        }
    alpha = swipeFadeAlpha(offset = offset, width = size.width)
}

internal fun swipeFadeAlpha(offset: Float, width: Float): Float {
    return if (width <= 0f || offset.isNaN()) {
        Alpha.TAB_ITEM_NO_INTERACTION
    } else {
        maxOf(
            Alpha.TAB_ITEM_MIN_SWIPE_FADE,
            Alpha.TAB_ITEM_NO_INTERACTION - (abs(offset) / width).coerceIn(0f, 1f),
        )
    }
}

/**
 * Semantic property for accessing a Composable item's current graphical scale property. This is intended to be applied
 * evenly across X and Y and set and fetched as needed for verification.
 */
internal val ScaleKey = SemanticsPropertyKey<Float>("Scale")
internal var SemanticsPropertyReceiver.scale by ScaleKey

/**
 * Semantic property for accessing a Composable item's alpha property. This is intended to be set and fetched as needed
 * for verification.
 */
internal val AlphaKey = SemanticsPropertyKey<Float>("Alpha")
internal var SemanticsPropertyReceiver.alpha by AlphaKey

/** Semantic property for accessing a tab grid's column count. */
internal val TabGridColumnCountKey = SemanticsPropertyKey<Int>("TabGridColumnCount")
internal var SemanticsPropertyReceiver.tabGridColumnCount by TabGridColumnCountKey

/** Scale parameters for interactable tab items. */
object Scale {
    const val DRAG_ACTIVE = 0.75f
    const val HOVER_ACTIVE = 0.75f
    const val HOVER_ACTIVE_LIST = 0.90f
    const val NO_INTERACTION = 1f
    const val NEW_GROUP_ENTRANCE_START = 0.92f
    const val NEW_GROUP_ENTRANCE_PEAK = 1.06f
}
