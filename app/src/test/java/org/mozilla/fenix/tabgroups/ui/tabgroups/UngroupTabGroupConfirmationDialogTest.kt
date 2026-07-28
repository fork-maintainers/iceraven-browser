/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabgroups.ui.tabgroups

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.tabgroups.TabGroupsTestTag
import org.mozilla.fenix.tabgroups.UngroupTabGroupConfirmationDialog

@RunWith(AndroidJUnit4::class)
class UngroupTabGroupConfirmationDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun confirmUngroupDialogClicked_dontAskAgainFalseTest() {
        var onConfirmInvoked = false
        var dontAskAgainResult = true

        composeTestRule.setContent {
            UngroupTabGroupConfirmationDialog(
                onConfirmUngroup = { dontAskAgain ->
                    onConfirmInvoked = true
                    dontAskAgainResult = dontAskAgain
                },
                onCancel = { },
            )
        }

        composeTestRule.onNodeWithTag(TabGroupsTestTag.UNGROUP_DIALOG_CONFIRM_BUTTON)
            .performClick()

        Assert.assertTrue(onConfirmInvoked)
        Assert.assertFalse(dontAskAgainResult)
    }

    @Test
    fun confirmUngroupDialogClicked_dontAskAgainTrueTest() {
        var onConfirmInvoked = false
        var dontAskAgainResult = false

        composeTestRule.setContent {
            UngroupTabGroupConfirmationDialog(
                onConfirmUngroup = { dontAskAgain ->
                    onConfirmInvoked = true
                    dontAskAgainResult = dontAskAgain
                },
                onCancel = { },
            )
        }

        composeTestRule.onNodeWithTag(TabGroupsTestTag.UNGROUP_DIALOG_DONT_ASK_AGAIN_CHECKBOX)
            .performClick()

        composeTestRule.onNodeWithTag(TabGroupsTestTag.UNGROUP_DIALOG_CONFIRM_BUTTON)
            .performClick()

        Assert.assertTrue(onConfirmInvoked)
        Assert.assertTrue(dontAskAgainResult)
    }

    @Test
    fun cancelUngroupDialogClickedTest() {
        var onCancelInvoked = false

        composeTestRule.setContent {
            UngroupTabGroupConfirmationDialog(
                onConfirmUngroup = { },
                onCancel = { onCancelInvoked = true },
            )
        }

        composeTestRule.onNodeWithTag(TabGroupsTestTag.UNGROUP_DIALOG_CANCEL_BUTTON)
            .performClick()

        Assert.assertTrue(onCancelInvoked)
    }
}
