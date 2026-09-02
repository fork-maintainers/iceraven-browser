package org.mozilla.fenix.tabstray.ui.tabitems

import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.tabstray.TabsTrayTestTag
import org.mozilla.fenix.tabstray.browser.compose.TabItemInteractionState
import org.mozilla.fenix.tabstray.data.createTab

/**
 * Note - this test runs in the androidTest directory due to difficulties handling the Bitmaps coming from
 * [org.mozilla.fenix.compose.ThumbnailImage] in Robolectric
 */
@RunWith(AndroidJUnit4::class)
class TabGridTabItemTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun verifyDraggedItemScale() {
        composeTestRule.setContent {
            ComposableUnderTest(interactionState = TabItemInteractionState(isDragged = true))
        }
        composeTestRule.waitUntil("Dragged item is scaled at 75%") {
            composeTestRule.onNodeWithTag(TabsTrayTestTag.TAB_ITEM_ROOT).fetchSemanticsNode().config[ScaleKey] == 0.75f
        }
    }

    @Test
    fun verifyUndraggedItemScale() {
        composeTestRule.setContent {
            ComposableUnderTest(interactionState = TabItemInteractionState(isDragged = false))
        }
        composeTestRule.waitUntil("Undragged item is scaled at 100%") {
            composeTestRule.onNodeWithTag(TabsTrayTestTag.TAB_ITEM_ROOT).fetchSemanticsNode().config[ScaleKey] == 1f
        }
    }

    @Test
    fun verifyDraggedItemAlpha() {
        composeTestRule.setContent {
            ComposableUnderTest(interactionState = TabItemInteractionState(isDragged = true))
        }
        composeTestRule.waitUntil("Dragged item opacity is 70%") {
            composeTestRule.onNodeWithTag(TabsTrayTestTag.TAB_ITEM_ROOT).fetchSemanticsNode().config[AlphaKey] == 0.7f
        }
    }

    @Test
    fun verifyUndraggedItemAlpha() {
        composeTestRule.setContent {
            ComposableUnderTest(interactionState = TabItemInteractionState(isDragged = false))
        }
        composeTestRule.waitUntil("Undragged item opacity is 100%") {
            composeTestRule.onNodeWithTag(TabsTrayTestTag.TAB_ITEM_ROOT).fetchSemanticsNode().config[AlphaKey] == 1f
        }
    }

    @Test
    fun verifyHeldUndraggedItemAlpha() {
        composeTestRule.setContent {
            ComposableUnderTest(interactionState = TabItemInteractionState(isDragged = false, isHeld = true))
        }
        composeTestRule.waitUntil("Held item opacity is 100%") {
            composeTestRule.onNodeWithTag(TabsTrayTestTag.TAB_ITEM_ROOT).fetchSemanticsNode().config[AlphaKey] == 1f
        }
    }

    @Test
    fun verifyHeldUndraggedItemScale() {
        composeTestRule.setContent {
            ComposableUnderTest(interactionState = TabItemInteractionState(isDragged = false, isHeld = true))
        }
        composeTestRule.waitUntil("Held item scale is 100%") {
            composeTestRule.onNodeWithTag(TabsTrayTestTag.TAB_ITEM_ROOT).fetchSemanticsNode().config[ScaleKey] == 1f
        }
    }

    @Test
    fun verifyHeldItemAlpha() {
        composeTestRule.setContent {
            ComposableUnderTest(interactionState = TabItemInteractionState(isDragged = true, isHeld = true))
        }
        composeTestRule.waitUntil("Held item opacity is 70%") {
            composeTestRule.onNodeWithTag(TabsTrayTestTag.TAB_ITEM_ROOT).fetchSemanticsNode().config[AlphaKey] == 0.7f
        }
    }

    @Test
    fun verifyHeldItemScale() {
        composeTestRule.setContent {
            ComposableUnderTest(interactionState = TabItemInteractionState(isDragged = true, isHeld = true))
        }
        composeTestRule.waitUntil("Held item scale is 75%") {
            composeTestRule.onNodeWithTag(TabsTrayTestTag.TAB_ITEM_ROOT).fetchSemanticsNode().config[ScaleKey] == 0.75f
        }
    }

    @Composable
    private fun ComposableUnderTest(
        interactionState: TabItemInteractionState = TabItemInteractionState(),
    ) {
        TabGridTabItem(
            tab = createTab(url = "mozilla.org"),
            swipeToDismissBoxState = rememberSwipeToDismissBoxState(),
            swipingEnabled = true,
            interactionState = interactionState,
            onCloseClick = { _ -> },
            onClick = { _ -> },
        )
    }
}
