/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.browser.compose.legacy

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.unit.IntSize
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
import org.mozilla.fenix.tabstray.browser.compose.ListReorderState

class ReorderableListTest {
    private val testDispatcher = StandardTestDispatcher()
    private val scope = TestScope(testDispatcher)

    @Test
    fun `GIVEN an item is dragged down onto another THEN onMove is called with placeAfter true`() {
        val handler = mockk<ItemInteractionHandler>(relaxed = true)
        val dragItemOffset = 10
        val targetItemOffset = 30
        val reorderState =
            fakeListReorderState(
                mockListState(
                    mockItems =
                        listOf(
                            mockk<LazyListItemInfo> {
                                every { key } returns "key1"
                                every { index } returns 1
                                every { size } returns 10
                                every { offset } returns dragItemOffset
                            },
                            mockk<LazyListItemInfo> {
                                every { key } returns "key2"
                                every { index } returns 2
                                every { size } returns 10
                                every { offset } returns targetItemOffset
                            },
                        )
                ),
                handler = handler,
            )

        reorderState.onTouchSlopPassed(offset = dragItemOffset.toFloat(), shouldLongPress = true)
        reorderState.onDrag(offset = 20f) // 20 down

        verify { handler.onMove("key1", "key2", true) }
    }

    @Test
    fun `GIVEN an item is dragged up onto another THEN onMove is called with placeAfter false`() {
        val handler = mockk<ItemInteractionHandler>(relaxed = true)
        val dragItemOffset = 30
        val targetItemOffset = 10
        val reorderState =
            fakeListReorderState(
                mockListState(
                    mockItems =
                        listOf(
                            mockk<LazyListItemInfo> {
                                every { key } returns "key1"
                                every { index } returns 1
                                every { size } returns 10
                                every { offset } returns dragItemOffset
                            },
                            mockk<LazyListItemInfo> {
                                every { key } returns "key2"
                                every { index } returns 2
                                every { size } returns 10
                                every { offset } returns targetItemOffset
                            },
                        )
                ),
                handler = handler,
            )

        reorderState.onTouchSlopPassed(dragItemOffset.toFloat(), true)
        reorderState.onDrag(offset = -20f) // 20 up

        verify { handler.onMove("key1", "key2", true) }
    }

    @Test
    fun `GIVEN a drag is in progress and onDragInterrupted is called THEN the previous key is saved and the state is reset`() {
        val reorderState = fakeListReorderState(listState = mockListState(mockItems = emptyList()))
        val draggingItemKey = reorderState.draggingItemKey

        reorderState.onTouchSlopPassed(offset = 0f, shouldLongPress = false)
        reorderState.onDragInterrupted()

        assertEquals(expected = draggingItemKey, actual = reorderState.previousKeyOfDraggedItem)
        assertNull(reorderState.draggingItemKey)
    }

    private fun mockListState(
        mockItems: List<LazyListItemInfo> = emptyList(),
        firstVisibleIndex: Int = 0,
        totalItems: Int = mockItems.size,
    ): LazyListState {
        return mockk<LazyListState> {
            every { firstVisibleItemIndex } returns firstVisibleIndex
            every { layoutInfo } returns
                mockk {
                    every { visibleItemsInfo } returns mockItems
                    every { viewportSize } returns IntSize(10, 10)
                    every { firstVisibleItemIndex } returns firstVisibleIndex
                    every { totalItemsCount } returns totalItems
                    every { mainAxisItemSpacing } returns 10
                    every { beforeContentPadding } returns 10
                    every { viewportStartOffset } returns 0
                    every { viewportEndOffset } returns 10
                }
            every { isScrollInProgress } returns false
        }
    }

    private fun fakeListReorderState(
        listState: LazyListState,
        handler: ItemInteractionHandler = NoOpItemInteractionHandler,
    ): ListReorderState {
        return ListReorderState(
            listState = listState,
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
                    sourceKey = initialTab.key as String,
                    targetKey = newTab.key as String,
                    placeAfter = initialTab.index < newTab.index,
                )
            },
            onExitLongPress = { sourceKey ->
                handler.onDragStart(sourceKey = sourceKey as String, preserveSelectMode = true)
            },
        )
    }
}
