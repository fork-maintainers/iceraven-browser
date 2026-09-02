/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.mozilla.fenix.home.ui.HomepageTestTag.HOMEPAGE_WORDMARK_LOGO
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class WordmarkTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `When the normal theme is rendered, then the fenix wordmark is found and displayed`() {
        composeTestRule.setContent {
            ComposableUnderTest(themeId = R.style.NormalTheme)
        }

        composeTestRule.onNodeWithTag(HOMEPAGE_WORDMARK_LOGO).assertIsDisplayed()
        assertEquals(
            expected = R.attr.fenixWordmarkLogo,
            actual = composeTestRule.onNodeWithTag(HOMEPAGE_WORDMARK_LOGO).semanticResourceId(),
        )
    }

    @Test
    fun `When the private theme is rendered, then the fenix wordmark is found and displayed`() {
        composeTestRule.setContent {
            ComposableUnderTest(themeId = R.style.PrivateTheme)
        }

        composeTestRule.onNodeWithTag(HOMEPAGE_WORDMARK_LOGO).assertIsDisplayed()
        assertEquals(
            expected = R.attr.fenixWordmarkLogo,
            actual = composeTestRule.onNodeWithTag(HOMEPAGE_WORDMARK_LOGO).semanticResourceId(),
        )
    }

    /**
     * [androidx.appcompat.view.ContextThemeWrapper] layers a theme resource on top of a base context.
     * For this case, we want to test if a resource is missing from the base theme, so we need to create a
     * theme from scratch with the appropriate style resource.
     */
    private fun Context.withIsolatedTheme(themeId: Int): Context =
        object : ContextWrapper(this) {
            override fun getTheme(): Resources.Theme? {
                return resources.newTheme().apply { applyStyle(themeId, true) }
            }
        }

    @Composable
    private fun ComposableUnderTest(themeId: Int) {
        val theme = LocalContext.current.withIsolatedTheme(themeId)
        CompositionLocalProvider(LocalContext provides theme) {
            WordmarkLogo()
        }
    }

    private fun SemanticsNodeInteraction.semanticResourceId(): Int {
        return this.fetchSemanticsNode().config[ResourceId]
    }
}
