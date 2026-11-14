/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabgroups.strip.redux

import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mozilla.components.feature.tabdata.coordinator.TabDataCoordinator
import mozilla.components.feature.tabdata.coordinator.data.Tab as CoordinatorTab
import mozilla.components.feature.tabdata.coordinator.data.TabGroup
import mozilla.components.feature.tabdata.coordinator.data.TabsSnapshot
import org.junit.Test

class TabGroupsStripMiddlewareTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun `GIVEN a selected tab in an open group WHEN a snapshot emits THEN the strip shows the group's tabs in order`() =
        runTest(testDispatcher) {
            val tab1 = createTab(id = "1", tabGroupId = "group")
            val tab2 = createTab(id = "2", tabGroupId = "group")
            val tab3 = createTab(id = "3", tabGroupId = "group")
            val store =
                buildStore(
                    TabsSnapshot(
                        selectedTab = tab2,
                        normalTabs = listOf(tab1, tab2, tab3),
                        openTabGroups = listOf(createGroup("group", "Group", tabIds = listOf("3", "1", "2"))),
                    )
                )

            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                expected =
                    TabGroupsStripState.TabGroupStripVisible(
                        groupId = "group",
                        groupTitle = "Group",
                        tabs = listOf(stripTab("3"), stripTab("1"), stripTab("2", isFocused = true)),
                    ),
                actual = store.state,
            )
        }

    @Test
    fun `GIVEN there is no selected tab WHEN a snapshot emits THEN the strip is empty`() =
        runTest(testDispatcher) {
            val store = buildStore(TabsSnapshot())

            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(expected = TabGroupsStripState.TabGroupStripHidden, actual = store.state)
        }

    @Test
    fun `GIVEN the selected tab is not in a group WHEN a snapshot emits THEN the strip is empty`() =
        runTest(testDispatcher) {
            val tab1 = createTab(id = "1", tabGroupId = null)
            val store =
                buildStore(TabsSnapshot(selectedTab = tab1, normalTabs = listOf(tab1), openTabGroups = emptyList()))

            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(expected = TabGroupsStripState.TabGroupStripHidden, actual = store.state)
        }

    @Test
    fun `GIVEN several open groups WHEN a snapshot emits THEN the strip shows the selected tab's group`() =
        runTest(testDispatcher) {
            val tab1 = createTab(id = "1", tabGroupId = "group-1")
            val tab2 = createTab(id = "2", tabGroupId = "group-2")
            val tab3 = createTab(id = "3", tabGroupId = "group-2")
            val store =
                buildStore(
                    TabsSnapshot(
                        selectedTab = tab2,
                        normalTabs = listOf(tab1, tab2, tab3),
                        openTabGroups =
                            listOf(
                                createGroup("group-1", "Group 1", tabIds = listOf("1")),
                                createGroup("group-2", "Group 2", tabIds = listOf("2", "3")),
                            ),
                    )
                )

            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                expected =
                    TabGroupsStripState.TabGroupStripVisible(
                        groupId = "group-2",
                        groupTitle = "Group 2",
                        tabs = listOf(stripTab("2", isFocused = true), stripTab("3")),
                    ),
                actual = store.state,
            )
        }

    @Test
    fun `WHEN a later snapshot emits THEN the strip keeps updating`() =
        runTest(testDispatcher) {
            val tabsSnapshot = MutableStateFlow<TabsSnapshot?>(null)
            val store = buildStore(coordinator = FakeTabDataCoordinator(tabsSnapshot))
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(expected = TabGroupsStripState.TabGroupStripHidden, actual = store.state)

            val tab1 = createTab(id = "1", tabGroupId = "group")
            tabsSnapshot.value =
                TabsSnapshot(
                    selectedTab = tab1,
                    normalTabs = listOf(tab1),
                    openTabGroups = listOf(createGroup("group", "Group", tabIds = listOf("1"))),
                )
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                expected =
                    TabGroupsStripState.TabGroupStripVisible(
                        groupId = "group",
                        groupTitle = "Group",
                        tabs = listOf(stripTab("1", isFocused = true)),
                    ),
                actual = store.state,
            )
        }

    @Test
    fun `GIVEN InitializeDataObservation is not dispatched WHEN a snapshot is available THEN it is not observed`() =
        runTest(testDispatcher) {
            val tab1 = createTab(id = "1", tabGroupId = "group")
            val coordinator =
                FakeTabDataCoordinator(
                    MutableStateFlow(
                        TabsSnapshot(
                            selectedTab = tab1,
                            normalTabs = listOf(tab1),
                            openTabGroups = listOf(createGroup("group", "Group", tabIds = listOf("1"))),
                        )
                    )
                )
            val store =
                TabGroupsStripStore(
                    middleware = listOf(TabGroupsStripMiddleware(coordinator, testScope, testDispatcher))
                )

            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(expected = TabGroupsStripState.TabGroupStripHidden, actual = store.state)

            store.dispatch(TabGroupsStripAction.InitializeDataObservation)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                expected =
                    TabGroupsStripState.TabGroupStripVisible(
                        groupId = "group",
                        groupTitle = "Group",
                        tabs = listOf(stripTab("1", isFocused = true)),
                    ),
                actual = store.state,
            )
        }

    private fun buildStore(snapshot: TabsSnapshot?): TabGroupsStripStore =
        buildStore(FakeTabDataCoordinator(MutableStateFlow(snapshot)))

    private fun buildStore(coordinator: TabDataCoordinator): TabGroupsStripStore =
        TabGroupsStripStore(middleware = listOf(TabGroupsStripMiddleware(coordinator, testScope, testDispatcher)))
            .also { it.dispatch(TabGroupsStripAction.InitializeDataObservation) }

    private fun createTab(
        id: String,
        tabGroupId: String?,
    ) =
        CoordinatorTab(
            id = id,
            url = "https://example.com/$id",
            title = "Tab $id",
            private = false,
            inactive = false,
            tabGroupId = tabGroupId,
        )

    private fun createGroup(
        id: String,
        title: String,
        tabIds: List<String>,
    ) = TabGroup(id = id, title = title, theme = "", closed = false, lastModified = 0L, tabIds = tabIds)

    private fun stripTab(
        id: String,
        isFocused: Boolean = false,
    ) = TabGroupsStripState.Tab(id = id, url = "https://example.com/$id", title = "Tab $id", isFocused = isFocused)
}

private class FakeTabDataCoordinator(override val tabsSnapshot: StateFlow<TabsSnapshot?>) : TabDataCoordinator {
    override fun initialize() = Unit
}
