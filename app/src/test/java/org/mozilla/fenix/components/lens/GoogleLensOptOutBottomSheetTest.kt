/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.lens

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.Theme

@RunWith(AndroidJUnit4::class)
class GoogleLensOptOutBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val resources get() = ApplicationProvider.getApplicationContext<Context>().resources
    private val title get() = resources.getString(R.string.lens_opt_out_title)
    private val description get() = resources.getString(R.string.lens_opt_out_description)
    private val settingsLink get() = resources.getString(R.string.lens_opt_out_settings_link)
    private val tryItNow get() = resources.getString(R.string.lens_opt_out_try_it_now_button)
    private val notNow get() = resources.getString(R.string.lens_opt_out_not_now_button)

    private var tryItNowClicked = false
    private var notNowClicked = false
    private var settingsClicked = false

    @Test
    fun `WHEN the sheet content is shown THEN the title, description, link and both buttons are displayed`() {
        setContent()

        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithText(description).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(settingsLink, substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(tryItNow).assertIsDisplayed()
        composeTestRule.onNodeWithText(notNow).assertIsDisplayed()
    }

    @Test
    fun `WHEN the sheet is shown in the dark theme THEN its content is displayed`() {
        setContent(theme = Theme.Dark)

        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithText(tryItNow).assertIsDisplayed()
    }

    @Test
    fun `WHEN the sheet is shown in the private theme THEN its content is displayed`() {
        setContent(theme = Theme.Private)

        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithText(tryItNow).assertIsDisplayed()
    }

    @Test
    fun `GIVEN the sheet content is shown WHEN try it now is tapped THEN only the try it now callback is invoked`() {
        setContent()

        composeTestRule.onNodeWithText(tryItNow).performClick()

        assertTrue(tryItNowClicked)
        assertFalse(notNowClicked)
        assertFalse(settingsClicked)
    }

    @Test
    fun `GIVEN the sheet content is shown WHEN not now is tapped THEN only the not now callback is invoked`() {
        setContent()

        composeTestRule.onNodeWithText(notNow).performClick()

        assertTrue(notNowClicked)
        assertFalse(tryItNowClicked)
        assertFalse(settingsClicked)
    }

    @Test
    fun `GIVEN the sheet content is shown WHEN the settings link is tapped THEN only the settings callback is invoked`() {
        setContent()

        composeTestRule
            .onNodeWithContentDescription(settingsLink, substring = true)
            .performSemanticsAction(SemanticsActions.OnClick)

        assertTrue(settingsClicked)
        assertFalse(tryItNowClicked)
        assertFalse(notNowClicked)
    }

    private fun setContent(theme: Theme = Theme.Light) {
        composeTestRule.setContent {
            // Explicit theme: the default provider reads the AppStore, which is mocked here.
            FirefoxTheme(theme = theme) {
                GoogleLensOptOutBottomSheet(
                    onRequestDismiss = {},
                    onTryItNowClick = { tryItNowClicked = true },
                    onNotNowClick = { notNowClicked = true },
                    onSettingsClick = { settingsClicked = true },
                )
            }
        }
    }
}
