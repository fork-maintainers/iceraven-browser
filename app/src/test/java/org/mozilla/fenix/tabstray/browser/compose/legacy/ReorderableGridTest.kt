/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.browser.compose.legacy

import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toOffset
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import mozilla.components.compose.draggable.ItemInteractionHandler
import mozilla.components.compose.draggable.NoOpItemInteractionHandler
import org.junit.Test

class ReorderableGridTest {
    private val testDispatcher = StandardTestDispatcher()
    private val scope = TestScope(testDispatcher)

    @Test
    fun `GIVEN an item is dragged right onto another THEN onMove is called with placeAfter false`() {
        val handler = mockk<ItemInteractionHandler>(relaxed = true)
        val dragItemOffset = IntOffset(10, 0)
        val targetItemOffset = IntOffset(30, 0)
        val reorderState =
            fakeGridReorderState(
                mockGridState(
                    mockItems =
                        listOf(
                            mockk<LazyGridItemInfo> {
                                every { key } returns "key1"
                                every { index } returns 1
                                every { size } returns IntSize(10, 10)
                                every { offset } returns dragItemOffset
                            },
                            mockk<LazyGridItemInfo> {
                                every { key } returns "key2"
                                every { index } returns 1
                                every { size } returns IntSize(10, 10)
                                every { offset } returns targetItemOffset
                            },
                        )
                ),
                handler = handler,
            )

        reorderState.onTouchSlopPassed(dragItemOffset.toOffset(), false)
        reorderState.onDrag(offset = Offset(20f, 0f), preserveSelectMode = false) // 20 to the right

        verify { handler.onMove("key1", "key2", false) }
    }

    @Test
    fun `GIVEN an item is dragged left onto another THEN onMove is called with placeAfter true`() {
        val handler = mockk<ItemInteractionHandler>(relaxed = true)
        val targetItemOffset = IntOffset(10, 0)
        val draggedItemOffset = IntOffset(30, 0)
        val reorderState =
            fakeGridReorderState(
                mockGridState(
                    mockItems =
                        listOf(
                            mockk<LazyGridItemInfo> {
                                every { key } returns "key1"
                                every { index } returns 1
                                every { size } returns IntSize(10, 10)
                                every { offset } returns targetItemOffset
                            },
                            mockk<LazyGridItemInfo> {
                                every { key } returns "key2"
                                every { index } returns 1
                                every { size } returns IntSize(10, 10)
                                every { offset } returns draggedItemOffset
                            },
                        )
                ),
                handler = handler,
            )

        reorderState.onTouchSlopPassed(draggedItemOffset.toOffset(), false)
        reorderState.onDrag(offset = Offset(-20f, 0f), preserveSelectMode = false) // 20 to the left

        verify { handler.onMove("key2", "key1", false) }
    }

    @Test
    fun `GIVEN a drag is in progress and onDragInterrupted is called THEN the previous key is saved and the state is reset`() {
        val reorderState = fakeGridReorderState(gridState = mockGridState(mockItems = emptyList()))
        val draggingItemKey = reorderState.draggingItemKey

        reorderState.onTouchSlopPassed(Offset.Zero, false)
        reorderState.onDragInterrupted()

        assertEquals(draggingItemKey, reorderState.previousKeyOfDraggedItem)
        assertNull(reorderState.draggingItemKey)
    }

    @Test
    fun `GIVEN a drag is in progress, cursor has not moved and onDragInterrupted is called THEN onDragCancelled is not called`() {
        val handler = mockk<ItemInteractionHandler>(relaxed = true)
        val targetItemOffset = IntOffset(10, 0)
        val draggedItemOffset = IntOffset(30, 0)
        val reorderState =
            fakeGridReorderState(
                mockGridState(
                    mockItems =
                        listOf(
                            mockk<LazyGridItemInfo> {
                                every { key } returns "key1"
                                every { index } returns 1
                                every { size } returns IntSize(10, 10)
                                every { offset } returns targetItemOffset
                            },
                            mockk<LazyGridItemInfo> {
                                every { key } returns "key2"
                                every { index } returns 2
                                every { size } returns IntSize(10, 10)
                                every { offset } returns draggedItemOffset
                            },
                        )
                ),
                handler = handler,
            )

        reorderState.onTouchSlopPassed(draggedItemOffset.toOffset(), false)
        reorderState.onDragInterrupted()

        verify(exactly = 0) { handler.onDragCancel() }
    }

    @Test
    fun `GIVEN a drag is in progress, cursor has moved and onDragInterrupted is called THEN onDragCancelled is called`() {
        val handler = mockk<ItemInteractionHandler>(relaxed = true)
        val targetItemOffset = IntOffset(10, 0)
        val draggedItemOffset = IntOffset(30, 0)
        val reorderState =
            fakeGridReorderState(
                mockGridState(
                    mockItems =
                        listOf(
                            mockk<LazyGridItemInfo> {
                                every { key } returns "key1"
                                every { index } returns 1
                                every { size } returns IntSize(10, 10)
                                every { offset } returns targetItemOffset
                            },
                            mockk<LazyGridItemInfo> {
                                every { key } returns "key2"
                                every { index } returns 2
                                every { size } returns IntSize(10, 10)
                                every { offset } returns draggedItemOffset
                            },
                        )
                ),
                handler = handler,
            )

        reorderState.onTouchSlopPassed(draggedItemOffset.toOffset(), false)
        reorderState.onDrag(offset = Offset(20f, 0f), preserveSelectMode = false) // 20 to the right
        reorderState.onDragInterrupted()

        verify { handler.onDragCancel() }
    }

    @Test
    fun `WHEN an item is dragged GIVEN preserveSelectMode is true THEN onDragStart is called with the same flag`() {
        val handler = mockk<ItemInteractionHandler>(relaxed = true)
        val dragItemOffset = IntOffset(10, 0)
        val targetItemOffset = IntOffset(30, 0)
        val reorderState =
            fakeGridReorderState(
                mockGridState(
                    mockItems =
                        listOf(
                            mockk<LazyGridItemInfo> {
                                every { key } returns "key1"
                                every { index } returns 1
                                every { size } returns IntSize(10, 10)
                                every { offset } returns dragItemOffset
                            },
                            mockk<LazyGridItemInfo> {
                                every { key } returns "key2"
                                every { index } returns 2
                                every { size } returns IntSize(10, 10)
                                every { offset } returns targetItemOffset
                            },
                        )
                ),
                handler = handler,
            )

        reorderState.onTouchSlopPassed(dragItemOffset.toOffset(), true)
        reorderState.onDrag(offset = Offset(50f, 0f), preserveSelectMode = true) // 50 to the right

        verify { handler.onDragStart(sourceKey = "key1", preserveSelectMode = true) }
    }

    @Test
    fun `WHEN an item is dragged GIVEN preserveSelectMode is false THEN onDragStart is called with the same flag`() {
        val handler = mockk<ItemInteractionHandler>(relaxed = true)
        val dragItemOffset = IntOffset(10, 0)
        val targetItemOffset = IntOffset(30, 0)
        val reorderState =
            fakeGridReorderState(
                mockGridState(
                    mockItems =
                        listOf(
                            mockk<LazyGridItemInfo> {
                                every { key } returns "key1"
                                every { index } returns 1
                                every { size } returns IntSize(10, 10)
                                every { offset } returns dragItemOffset
                            },
                            mockk<LazyGridItemInfo> {
                                every { key } returns "key2"
                                every { index } returns 2
                                every { size } returns IntSize(10, 10)
                                every { offset } returns targetItemOffset
                            },
                        )
                ),
                handler = handler,
            )

        reorderState.onTouchSlopPassed(dragItemOffset.toOffset(), false)
        reorderState.onDrag(Offset(50f, 0f), preserveSelectMode = false) // 50 to the right

        verify { handler.onDragStart(sourceKey = "key1", preserveSelectMode = false) }
    }

    private fun mockGridState(
        mockItems: List<LazyGridItemInfo> = emptyList(),
        firstVisibleIndex: Int = 0,
        totalItems: Int = mockItems.size,
    ): LazyGridState {
        return mockk<LazyGridState> {
            every { firstVisibleItemIndex } returns firstVisibleIndex
            every { layoutInfo } returns
                mockk {
                    every { visibleItemsInfo } returns mockItems
                    every { viewportSize } returns IntSize(10, 10)
                    every { firstVisibleItemIndex } returns firstVisibleIndex
                    every { totalItemsCount } returns totalItems
                    every { mainAxisItemSpacing } returns 10
                    every { beforeContentPadding } returns 10
                }
            every { isScrollInProgress } returns false
        }
    }

    private fun fakeGridReorderState(
        gridState: LazyGridState,
        handler: ItemInteractionHandler = NoOpItemInteractionHandler,
    ): GridReorderState {
        return GridReorderState(
            gridState = gridState,
            itemInteractionHandler = handler,
            scope = scope,
            touchSlop = 0f,
            ignoredItems = emptyList(),
            onLongPress = { _ -> },
            hapticFeedback =
                mockk<HapticFeedback> {
                    every { performHapticFeedback(any()) } just Runs
                },
            onMove = { initialTab, newTab ->
                handler.onMove(
                    (initialTab.key as String),
                    (newTab.key as String),
                    initialTab.index < newTab.index,
                )
            },
        )
    }
}
