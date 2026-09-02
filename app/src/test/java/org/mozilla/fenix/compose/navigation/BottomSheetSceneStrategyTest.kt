/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.compose.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.ui.NavDisplay
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.Theme
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

private const val ROOT = "Root"
private const val FIRST_SHEET = "FirstSheet"
private const val SECOND_SHEET = "SecondSheet"

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
class BottomSheetSceneStrategyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val root = NavEntry(key = ROOT) {}
    private val firstSheet = NavEntry(key = FIRST_SHEET, metadata = sheetMetadata()) {}
    private val secondSheet = NavEntry(key = SECOND_SHEET, metadata = sheetMetadata()) {}

    @Test
    fun `WHEN a sheet entry is pushed onto another sheet entry THEN the scene key changes`() {
        val single = requireNotNull(calculateScene(root, firstSheet))
        val stacked = requireNotNull(calculateScene(root, firstSheet, secondSheet))

        assertNotEquals(single.key, stacked.key)
    }

    @Test
    fun `WHEN sheet entries are stacked THEN the scene renders the last entry`() {
        val scene = requireNotNull(calculateScene(root, firstSheet, secondSheet))

        assertEquals(listOf(secondSheet), scene.entries)
        assertEquals(secondSheet.contentKey, scene.key)
    }

    @Test
    fun `WHEN two scenes are calculated from the same back stack THEN they are equal`() {
        val first = requireNotNull(calculateScene(root, firstSheet, secondSheet))
        val second = requireNotNull(calculateScene(root, firstSheet, secondSheet))

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `WHEN two scenes are calculated from different back stacks THEN they are not equal`() {
        val single = requireNotNull(calculateScene(root, firstSheet))
        val stacked = requireNotNull(calculateScene(root, firstSheet, secondSheet))

        assertNotEquals(single, stacked)
    }

    @Test
    fun `WHEN a second sheet is pushed THEN the second sheet content is displayed`() {
        val backStack = mutableStateListOf(ROOT, FIRST_SHEET)

        composeTestRule.setContent {
            FirefoxTheme(theme = Theme.Light) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    sceneStrategies = listOf(BottomSheetSceneStrategy()),
                    entryProvider = entryProvider {
                        entry(ROOT) { Text(text = ROOT) }
                        entry(FIRST_SHEET, metadata = sheetMetadata()) { Text(text = FIRST_SHEET) }
                        entry(SECOND_SHEET, metadata = sheetMetadata()) { Text(text = SECOND_SHEET) }
                    },
                )
            }
        }

        composeTestRule.onNodeWithText(FIRST_SHEET).assertIsDisplayed()

        composeTestRule.runOnUiThread { backStack.add(SECOND_SHEET) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(SECOND_SHEET).assertIsDisplayed()
        composeTestRule.onNodeWithText(FIRST_SHEET).assertDoesNotExist()
    }

    private fun sheetMetadata(): Map<String, Any> =
        BottomSheetSceneStrategy.bottomSheet(handleContentDescription = "")

    private fun calculateScene(vararg entries: NavEntry<String>) =
        with(BottomSheetSceneStrategy<String>()) {
            SceneStrategyScope<String>().calculateScene(entries.toList())
        }
}
