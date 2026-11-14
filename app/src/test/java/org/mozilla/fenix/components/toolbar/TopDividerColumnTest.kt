/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_TAG = "COLUMN"
private const val STRIP_ANIMATION_MS = 150

@RunWith(AndroidJUnit4::class)
class TopDividerColumnTest {

    @get:Rule val composeTestRule = createComposeRule()

    private var showDivider by mutableStateOf(true)
    private var isStripVisible by mutableStateOf(true)

    @Test
    fun `WHEN the divider is turned off THEN it is no longer drawn`() {
        setContent()

        composeTestRule.onNodeWithTag(BOTTOM_CHROME_DIVIDER_TEST_TAG).assertExists()

        showDivider = false
        composeTestRule.onNodeWithTag(BOTTOM_CHROME_DIVIDER_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `WHEN the divider is shown THEN it is on the top edge and spans the whole width`() {
        setContent()

        val columnBounds = columnBounds()
        val dividerBounds = dividerBounds()
        assertEquals(columnBounds.top, dividerBounds.top)
        assertEquals(columnBounds.left, dividerBounds.left)
        assertEquals(columnBounds.right, dividerBounds.right)
    }

    @Test
    fun `WHEN the divider is turned off THEN the height stays the sum of the content heights`() {
        setContent()

        composeTestRule.onNodeWithTag(TEST_TAG).assertHeightIsEqualTo(96.dp)

        showDivider = false
        composeTestRule.onNodeWithTag(TEST_TAG).assertHeightIsEqualTo(96.dp)
    }

    @Test
    fun `WHEN the content has no height THEN nothing is drawn`() {
        composeTestRule.setContent {
            TopDividerColumn(showDivider = true, modifier = Modifier.fillMaxWidth().testTag(TEST_TAG)) {}
        }

        composeTestRule.onNodeWithTag(TEST_TAG).assertHeightIsEqualTo(0.dp)
        composeTestRule.onNodeWithTag(BOTTOM_CHROME_DIVIDER_TEST_TAG).assertHeightIsEqualTo(0.dp)
    }

    @Test
    fun `WHEN the top content collapses THEN the divider stays on the top edge`() {
        setContent()
        val initialTop = columnBounds().top
        composeTestRule.mainClock.autoAdvance = false

        isStripVisible = false
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(STRIP_ANIMATION_MS / 2L)

        assertNotEquals(initialTop, columnBounds().top)
        assertEquals(columnBounds().top, dividerBounds().top)

        composeTestRule.mainClock.advanceTimeBy(STRIP_ANIMATION_MS.toLong())

        composeTestRule.onNodeWithTag(TEST_TAG).assertHeightIsEqualTo(56.dp)
        assertEquals(columnBounds().top, dividerBounds().top)
    }

    private fun setContent() {
        composeTestRule.setContent {
            Box(modifier = Modifier.size(width = 300.dp, height = 300.dp)) {
                TopDividerColumn(
                    showDivider = showDivider,
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).testTag(TEST_TAG),
                ) {
                    AnimatedVisibility(
                        visible = isStripVisible,
                        exit = shrinkVertically(animationSpec = tween(durationMillis = STRIP_ANIMATION_MS)),
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(40.dp))
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(56.dp))
                }
            }
        }
    }

    private fun columnBounds() = composeTestRule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot()

    private fun dividerBounds() =
        composeTestRule.onNodeWithTag(BOTTOM_CHROME_DIVIDER_TEST_TAG).getUnclippedBoundsInRoot()
}
