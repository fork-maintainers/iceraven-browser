/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.browser.compose.legacy

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mozilla.components.compose.draggable.ItemInteractionHandler
import mozilla.components.compose.draggable.ItemInteractionState

/**
 * Remember the reordering state for reordering grid items.
 *
 * @param gridState State of the grid.
 * @param onMove Callback to be invoked when switching between two items.
 * @param ignoredItems List of keys for non-draggable items.
 * @param itemInteractionHandler The item interaction handler for moves, drops, and drag events.
 * @param onLongPress Optional callback to be invoked when long pressing an item.
 */
@Composable
fun createGridReorderState(
    gridState: LazyGridState,
    onMove: (LazyGridItemInfo, LazyGridItemInfo) -> Unit,
    ignoredItems: List<Any>,
    itemInteractionHandler: ItemInteractionHandler,
    onLongPress: (LazyGridItemInfo) -> Unit = {},
): GridReorderState {
    val scope = rememberCoroutineScope()
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val hapticFeedback = LocalHapticFeedback.current
    val state =
        remember(gridState) {
            GridReorderState(
                gridState = gridState,
                onMove = onMove,
                scope = scope,
                touchSlop = touchSlop,
                ignoredItems = ignoredItems,
                onLongPress = onLongPress,
                hapticFeedback = hapticFeedback,
                itemInteractionHandler = itemInteractionHandler,
            )
        }
    return state
}

/**
 * Class containing details about the current state of dragging in grid.
 *
 * @param gridState State of the grid.
 * @param scope [CoroutineScope] used for scrolling to the target item.
 * @param hapticFeedback [HapticFeedback] used for performing haptic feedback on item long press.
 * @param touchSlop Distance in pixels the user can wander until we consider they started dragging.
 * @param onMove Callback to be invoked when switching between two items.
 * @param onLongPress Optional callback to be invoked when long pressing an item.
 * @param ignoredItems List of keys for non-draggable items.
 * @param itemInteractionHandler The item interaction handler for moves, drops, and drag events.
 */
class GridReorderState
internal constructor(
    private val gridState: LazyGridState,
    private val scope: CoroutineScope,
    private val hapticFeedback: HapticFeedback,
    private val touchSlop: Float,
    private val onMove: (LazyGridItemInfo, LazyGridItemInfo) -> Unit,
    private val onLongPress: (LazyGridItemInfo) -> Unit = {},
    private val ignoredItems: List<Any> = emptyList(),
    private val itemInteractionHandler: ItemInteractionHandler,
) {
    internal var draggingItemKey by mutableStateOf<GridItemKey?>(null)
        private set

    internal var hoveredItemKey by mutableStateOf<GridItemKey?>(null)
        private set

    private var draggingItemCumulatedOffset by mutableStateOf(Offset.Zero)
    private var draggingItemInitialOffset by mutableStateOf(Offset.Zero)
    internal var moved by mutableStateOf(false)
    private val draggingItemOffset: Offset
        get() =
            draggingItemLayoutInfo?.let { item ->
                draggingItemInitialOffset + draggingItemCumulatedOffset - item.offset.toOffset()
            } ?: Offset.Zero

    internal fun computeItemOffset(index: Int): Offset {
        val itemAtIndex =
            gridState.layoutInfo.visibleItemsInfo.firstOrNull { info -> info.index == index } ?: return Offset.Zero
        return draggingItemInitialOffset + draggingItemCumulatedOffset - itemAtIndex.offset.toOffset()
    }

    private val draggingItemLayoutInfo: LazyGridItemInfo?
        get() = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == draggingItemKey }

    internal var previousKeyOfDraggedItem by mutableStateOf<GridItemKey?>(null)
        private set

    internal var previousItemOffset = Animatable(Offset.Zero, Offset.VectorConverter)
        private set

    internal fun onTouchSlopPassed(offset: Offset, shouldLongPress: Boolean) {
        gridState.findItem(offset)?.also {
            draggingItemKey = it.key
            if (shouldLongPress) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongPress(it)
            }
            draggingItemInitialOffset = it.offset.toOffset()
        }
    }

    internal fun onDragInterrupted() {
        if (draggingItemKey != null) {
            previousKeyOfDraggedItem = draggingItemKey
            val startOffset = draggingItemOffset
            scope.launch {
                previousItemOffset.snapTo(startOffset)
                previousItemOffset.animateTo(
                    Offset.Zero,
                    spring(
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = Offset.VisibilityThreshold,
                    ),
                )
                previousKeyOfDraggedItem = null
            }
        }
        draggingItemCumulatedOffset = Offset.Zero
        draggingItemKey = null
        draggingItemInitialOffset = Offset.Zero
        if (moved) {
            itemInteractionHandler.onDragCancel()
        }
    }

    internal fun onDrag(offset: Offset, preserveSelectMode: Boolean) {
        draggingItemCumulatedOffset += offset

        if (draggingItemLayoutInfo == null) {
            moved = false
        }
        val draggingItem = draggingItemLayoutInfo ?: return

        if (!moved && draggingItemCumulatedOffset.getDistance() > touchSlop) {
            (draggingItemKey as? String)?.let { key ->
                itemInteractionHandler.onDragStart(sourceKey = key, preserveSelectMode = preserveSelectMode)
            }
            moved = true
        }
        val startOffset = draggingItem.offset.toOffset() + draggingItemOffset
        val endOffset =
            Offset(
                startOffset.x + draggingItem.size.toSize().width,
                startOffset.y + draggingItem.size.toSize().height,
            )
        val middleOffset = startOffset + (endOffset - startOffset) / 2f

        val targetItem =
            gridState.layoutInfo.visibleItemsInfo.find { item ->
                middleOffset.x.toInt() in item.offset.x..item.endOffset.x &&
                    middleOffset.y.toInt() in item.offset.y..item.endOffset.y &&
                    draggingItemKey != item.key
            }
        if (targetItem != null && targetItem.key !in ignoredItems) {
            if (draggingItem.index == gridState.firstVisibleItemIndex) {
                scope.launch {
                    gridState.scrollBy(-draggingItem.size.height.toFloat())
                }
            }
            onMove.invoke(draggingItem, targetItem)
        } else {
            val overscroll =
                when {
                    draggingItemCumulatedOffset.y > 0 ->
                        (endOffset.y - gridState.layoutInfo.viewportEndOffset).coerceAtLeast(0f)

                    draggingItemCumulatedOffset.y < 0 ->
                        (startOffset.y - gridState.layoutInfo.viewportStartOffset).coerceAtMost(0f)

                    else -> 0f
                }
            if (overscroll != 0f) {
                scope.launch {
                    gridState.scrollBy(overscroll)
                }
            }
        }
    }
}

/**
 * Container for draggable grid item.
 *
 * @param state State of the lazy grid.
 * @param key Key of the item to be displayed.
 * @param position Position in the grid of the item to be displayed.
 * @param swipingActive Whether the container is being swiped.
 * @param content Content of the item to be displayed.
 */
@Composable
@Suppress("MagicNumber")
fun LazyGridItemScope.ReorderableDragItemContainer(
    state: GridReorderState,
    key: GridItemKey,
    position: Int,
    swipingActive: Boolean,
    content: @Composable (interactionState: ItemInteractionState) -> Unit,
) {
    val modifier =
        Modifier.zIndex(
                if (swipingActive) {
                    10f
                } else if (key == state.draggingItemKey || key == state.previousKeyOfDraggedItem) {
                    1f
                } else {
                    0f
                }
            )
            .then(
                when (key) {
                    state.draggingItemKey -> {
                        Modifier.graphicsLayer {
                            translationX = state.computeItemOffset(position).x
                            translationY = state.computeItemOffset(position).y
                        }
                    }

                    state.previousKeyOfDraggedItem -> {
                        Modifier.graphicsLayer {
                            translationX = state.previousItemOffset.value.x
                            translationY = state.previousItemOffset.value.y
                        }
                    }

                    else -> {
                        Modifier.animateItem(tween())
                    }
                }
            )

    Box(modifier = modifier, propagateMinConstraints = true) {
        content(
            ItemInteractionState(
                isHoveredByItem = key == state.hoveredItemKey,
                isDragged = key == state.draggingItemKey,
            )
        )
    }
}

/** Calculate the offset of an item taking its width and height into account. */
private val LazyGridItemInfo.endOffset: IntOffset
    get() = IntOffset(offset.x + size.width, offset.y + size.height)

/**
 * Find item based on position on screen.
 *
 * @param offset Position on screen used to find the item.
 */
private fun LazyGridState.findItem(offset: Offset) =
    layoutInfo.visibleItemsInfo.firstOrNull { item ->
        offset.x.toInt() in item.offset.x..item.endOffset.x && offset.y.toInt() in item.offset.y..item.endOffset.y
    }

/**
 * Detects press, long press and drag gestures.
 *
 * @param gridState State of the grid.
 * @param reorderState Grid reordering state used for dragging callbacks.
 * @param isInMultiSelectMode Whether or not multi-select mode is active for the grid being reordered.
 */
fun Modifier.detectGridPressAndDragGestures(
    gridState: LazyGridState,
    reorderState: GridReorderState,
    isInMultiSelectMode: Boolean,
): Modifier =
    pointerInput(gridState, isInMultiSelectMode) {
        // In multi-select mode, drag gestures will be detected without a long press and the reorder state
        // will attempt to preserve the select mode state.
        if (isInMultiSelectMode) {
            detectDragGestures(
                onDragStart = { offset -> reorderState.onTouchSlopPassed(offset, false) },
                onDrag = { change, dragAmount ->
                    change.consume()
                    reorderState.onDrag(offset = dragAmount, preserveSelectMode = true)
                },
                onDragEnd = reorderState::onDragInterrupted,
                onDragCancel = reorderState::onDragInterrupted,
            )
        } else {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset -> reorderState.onTouchSlopPassed(offset, true) },
                onDrag = { change, dragAmount ->
                    change.consume()
                    reorderState.onDrag(offset = dragAmount, preserveSelectMode = false)
                },
                onDragEnd = reorderState::onDragInterrupted,
                onDragCancel = reorderState::onDragInterrupted,
            )
        }
    }

typealias GridItemKey = Any
