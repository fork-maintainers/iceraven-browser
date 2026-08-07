package org.mozilla.fenix.tabstray.ui.tabpage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.TestCase.assertEquals
import mozilla.components.compose.base.utils.LocalUnderTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.tabstray.TabsTrayTestTag
import org.mozilla.fenix.tabstray.controller.TabInteractionHandler
import org.mozilla.fenix.tabstray.data.createTab
import org.mozilla.fenix.tabstray.redux.state.TabsTrayState
import org.mozilla.fenix.tabstray.ui.tabitems.TabGridColumnCountKey
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.Theme
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class TabLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val tabletLandscapeSize = DpSize(1280.dp, 800.dp)
    private val tabletPortraitSize = DpSize(800.dp, 1280.dp)

    @Test
    fun `WHEN the container for TabLayout is large on a large device in landscape THEN 5 columns are created`() {
        composeTestRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowSize(tabletLandscapeSize),
            ) {
                GridContainer(1280.dp)
            }
        }

        assertEquals(5, gridColumnCount)
    }

    @Test
    fun `WHEN the container for TabLayout is medium with a large device in landscape THEN 4 columns are created`() {
        composeTestRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowSize(tabletLandscapeSize),
            ) {
                GridContainer(800.dp)
            }
        }

        assertEquals(3, gridColumnCount)
    }

    @Test
    fun `WHEN the container for TabLayout is small with a large device in landscape THEN 3 columns are created`() {
        composeTestRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowSize(tabletLandscapeSize),
            ) {
                GridContainer(500.dp)
            }
        }

        assertEquals(3, gridColumnCount)
    }

    @Test
    fun `WHEN the container for TabLayout is large on a large device in portrait THEN 5 columns are created`() {
        composeTestRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowSize(tabletPortraitSize),
            ) {
                GridContainer(1280.dp)
            }
        }

        assertEquals(4, gridColumnCount)
    }

    @Test
    fun `WHEN the container for TabLayout is small with a large device in portrait THEN 3 columns are created`() {
        composeTestRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowSize(tabletPortraitSize),
            ) {
                GridContainer(400.dp)
            }
        }

        assertEquals(2, gridColumnCount)
    }

    @Test
    fun `WHEN screen width is large THEN columnCount is 4 in portrait`() {
        assertEquals(expected = 4, numberOfGridColumnsPortrait(screenWidthDp = 800f))
    }

    @Test
    fun `WHEN screen width is medium THEN columnCount is 3 in portrait`() {
        assertEquals(expected = 3, numberOfGridColumnsPortrait(screenWidthDp = 500f))
    }

    @Test
    fun `WHEN screen width is small THEN columnCount is 2 in portrait`() {
        assertEquals(expected = 2, numberOfGridColumnsPortrait(screenWidthDp = 200f))
    }

    @Test
    fun `WHEN screen width is large THEN columnCount is 5 in landscape`() {
        assertEquals(expected = 5, numberOfGridColumnsLandscape(screenWidthDp = 1280f))
    }

    @Test
    fun `WHEN screen width is medium THEN columnCount is 4 in landscape`() {
        assertEquals(expected = 4, numberOfGridColumnsLandscape(screenWidthDp = 1000f))
    }

    @Test
    fun `WHEN screen width is small THEN columnCount is 3 in landscape`() {
        assertEquals(expected = 3, numberOfGridColumnsLandscape(screenWidthDp = 200f))
    }

    @Test
    fun `GIVEN the tab group onboarding card is shown in grid view WHEN onboarding is no longer displayed THEN the card is removed`() {
        var displayTabGroupOnboarding by mutableStateOf(true)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalUnderTest provides true) {
                FirefoxTheme(theme = Theme.Light) {
                    Surface {
                        TabLayout(
                            tabs = List(10) { createTab(url = "www.mozilla.org") },
                            displayTabsInGrid = true,
                            dragAndDropEnabled = true,
                            displayTabGroupOnboarding = displayTabGroupOnboarding,
                            selectedItemIndex = 0,
                            selectionMode = TabsTrayState.Mode.Normal,
                            focusEnabled = true,
                            tabInteractionHandler = fakeTabInteractionHandler(),
                            onTabClose = { _ -> },
                            onItemClick = { _ -> },
                            onItemLongClick = { _ -> },
                            onDeleteTabGroupClick = { _ -> },
                            onEditTabGroupClick = { _ -> },
                            onCloseTabGroupClick = { _ -> },
                            onTabGroupOnboardingDismiss = { },
                            liveReorderEnabled = false,
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag(TabsTrayTestTag.TAB_GROUP_ONBOARDING_GRID_ITEM).assertExists()

        displayTabGroupOnboarding = false

        composeTestRule.onNodeWithTag(TabsTrayTestTag.TAB_GROUP_ONBOARDING_GRID_ITEM).assertDoesNotExist()
    }

    @Test
    fun `GIVEN the tab group onboarding card is shown in list view WHEN onboarding is no longer displayed THEN the card is removed`() {
        var displayTabGroupOnboarding by mutableStateOf(true)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalUnderTest provides true) {
                FirefoxTheme(theme = Theme.Light) {
                    Surface {
                        TabLayout(
                            tabs = List(10) { createTab(url = "www.mozilla.org") },
                            displayTabsInGrid = false,
                            dragAndDropEnabled = true,
                            displayTabGroupOnboarding = displayTabGroupOnboarding,
                            selectedItemIndex = 0,
                            selectionMode = TabsTrayState.Mode.Normal,
                            focusEnabled = true,
                            tabInteractionHandler = fakeTabInteractionHandler(),
                            onTabClose = { _ -> },
                            onItemClick = { _ -> },
                            onItemLongClick = { _ -> },
                            onDeleteTabGroupClick = { _ -> },
                            onEditTabGroupClick = { _ -> },
                            onCloseTabGroupClick = { _ -> },
                            onTabGroupOnboardingDismiss = { },
                            liveReorderEnabled = false,
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag(TabsTrayTestTag.TAB_GROUP_ONBOARDING_LIST_ITEM).assertExists()

        displayTabGroupOnboarding = false

        composeTestRule.onNodeWithTag(TabsTrayTestTag.TAB_GROUP_ONBOARDING_LIST_ITEM).assertDoesNotExist()
    }

    private val gridColumnCount: Int
        get() = composeTestRule.onNodeWithTag(TabsTrayTestTag.TAB_GRID).fetchSemanticsNode().config[TabGridColumnCountKey]

    @Composable
    private fun GridContainer(width: Dp) {
        CompositionLocalProvider(LocalUnderTest provides true) {
            FirefoxTheme(theme = Theme.Light) {
                Surface {
                    Box(Modifier.requiredWidth(width)) {
                        TabLayoutGrid()
                    }
                }
            }
        }
    }

    @Composable
    private fun TabLayoutGrid(modifier: Modifier = Modifier) {
        val tabs = List(10) {
            createTab(url = "www.mozilla.org")
        }
        TabLayout(
            tabs = tabs,
            displayTabsInGrid = true,
            dragAndDropEnabled = true,
            displayTabGroupOnboarding = true,
            selectedItemIndex = 0,
            selectionMode = TabsTrayState.Mode.Normal,
            focusEnabled = true,
            tabInteractionHandler = fakeTabInteractionHandler(),
            modifier = modifier,
            trackersBlockedCount = 0,
            onTabClose = { _ -> },
            onItemClick = { _ -> },
            onItemLongClick = { _ -> },
            onDeleteTabGroupClick = { _ -> },
            onEditTabGroupClick = { _ -> },
            onCloseTabGroupClick = { _ -> },
            onTabGroupOnboardingDismiss = { },
            onPrivacyReportTapped = {},
            liveReorderEnabled = false,
        )
    }

    private fun fakeTabInteractionHandler() = object : TabInteractionHandler {
        override fun onMove(sourceKey: String, targetKey: String?, placeAfter: Boolean) {
            // no op
        }

        override fun onDrop(sourceKey: String, targetKey: String) {
            // no op
        }

        override fun onDragCancel() {
            // no op
        }

        override fun onDragStart(sourceKey: String, preserveSelectMode: Boolean) {
            // no op
        }
    }
}
