/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import android.content.Context
import android.content.res.Configuration
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import junit.framework.TestCase.assertEquals
import mozilla.components.support.test.robolectric.testContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.BaseBrowserFragment
import org.mozilla.fenix.browser.BrowserFragment
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.components.toolbar.ToolbarPosition
import org.mozilla.fenix.microsurvey.MicrosurveyState
import org.mozilla.fenix.microsurvey.ui.ext.MicrosurveyUIData
import org.mozilla.fenix.utils.Settings
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class BaseBrowserFragmentTest {

    private val settings: Settings = spyk(Settings(testContext))
    private lateinit var mockContext: Context
    private lateinit var fragment: BaseBrowserFragment
    private val microsurvey =
        MicrosurveyUIData(
            id = "microsurvey-id",
            promptTitle = "Prompt title",
            icon = 0,
            question = "Question?",
            answers = listOf("Yes", "No"),
            maxNumberLines = 3,
        )

    @Before
    fun setup() {
        mockContext =
            mockk(relaxed = true) {
                every { components.settings } returns settings
            }
        // `BaseBrowserFragment` is abstract, so test against the concrete `BrowserFragment`.
        fragment =
            spyk(BrowserFragment()).apply {
                every { context } returns mockContext
            }

        every { mockContext.resources.getDimensionPixelSize(R.dimen.tab_strip_height) } returns 56
        every { mockContext.resources.getDimensionPixelSize(R.dimen.browser_microsurvey_height) } returns 131
        every { mockContext.resources.getDimensionPixelSize(R.dimen.browser_navbar_height) } returns 60
        every { mockContext.resources.getDimensionPixelSize(R.dimen.browser_navbar_height_small) } returns 48
        every { settings.tabGroupsStripEnabled } returns false
        setMicrosurveyPromptVisible(false)
    }

    private fun setMicrosurveyPromptVisible(visible: Boolean) {
        every { settings.microsurveyFeatureEnabled } returns visible
        every { mockContext.components.appStore.state } returns
            AppState(microsurvey = MicrosurveyState(current = if (visible) microsurvey else null))
    }

    @Test
    fun `GIVEN the toolbar and the microsurvey are shown at bottom WHEN getBottomToolbarHeight THEN returns the combined height`() {
        every { mockContext.resources } returns testContext.resources
        setMicrosurveyPromptVisible(true)
        every { settings.toolbarPosition } returns ToolbarPosition.BOTTOM

        val bottomToolbarHeight = fragment.getBottomToolbarHeight()

        assertEquals(195, bottomToolbarHeight)
    }

    @Test
    fun `GIVEN just the microsurvey is shown at bottom WHEN getBottomToolbarHeight THEN returns it's height`() {
        setMicrosurveyPromptVisible(true)
        every { settings.toolbarPosition } returns ToolbarPosition.TOP

        val bottomToolbarHeight = fragment.getBottomToolbarHeight()

        assertEquals(131, bottomToolbarHeight)
    }

    @Test
    @Config(qualifiers = "h481dp") // navbar is only shown on screens taller than 480dp
    fun `GIVEN just the toolbar shown at bottom WHEN getBottomToolbarHeight THEN returns it's height`() {
        every { mockContext.resources } returns testContext.resources
        setMicrosurveyPromptVisible(false)
        every { settings.toolbarPosition } returns ToolbarPosition.BOTTOM

        val bottomToolbarHeight = fragment.getBottomToolbarHeight()

        assertEquals(64, bottomToolbarHeight)
    }

    @Test
    @Config(qualifiers = "h481dp") // navbar is only shown on screens taller than 480dp
    fun `GIVEN the toolbar, navigation bar and the microsurvey are shown at bottom WHEN getBottomToolbarHeight with excluded navigation bar THEN returns the combined height minus navigation bar`() {
        val configuration =
            Configuration().apply {
                screenHeightDp = 481
            }
        every { mockContext.resources.configuration } returns configuration
        every { mockContext.resources } returns testContext.resources
        setMicrosurveyPromptVisible(true)
        every { settings.shouldUseExpandedToolbar } returns true
        every { settings.toolbarPosition } returns ToolbarPosition.BOTTOM

        val bottomToolbarHeight = fragment.getBottomToolbarHeight(includeNavBarIfEnabled = false)

        assertEquals(187, bottomToolbarHeight)
    }

    @Test
    @Config(qualifiers = "h481dp") // navbar is only shown on screens taller than 480dp
    fun `GIVEN the toolbar, navigation bar and the microsurvey are shown at bottom WHEN getBottomToolbarHeight THEN returns the combined height`() {
        val configuration =
            Configuration().apply {
                screenHeightDp = 481
            }
        every { mockContext.resources.configuration } returns configuration
        every { mockContext.resources } returns testContext.resources
        setMicrosurveyPromptVisible(true)
        every { settings.shouldUseExpandedToolbar } returns true
        every { settings.toolbarPosition } returns ToolbarPosition.BOTTOM

        val bottomToolbarHeight = fragment.getBottomToolbarHeight()

        assertEquals(235, bottomToolbarHeight)
    }

    @Test
    fun `GIVEN just the toolbar at the bottom WHEN getBottomToolbarHeight THEN returns the toolbar height`() {
        every { mockContext.resources } returns testContext.resources
        every { settings.toolbarPosition } returns ToolbarPosition.BOTTOM
        every { settings.shouldShowTabStripAtBottom } returns false

        val bottomToolbarHeight = fragment.getBottomToolbarHeight()

        assertEquals(64, bottomToolbarHeight)
    }

    @Test
    fun `GIVEN the composable toolbar and tab groups strip are shown at bottom WHEN getBottomToolbarHeight THEN returns the combined height`() {
        every { mockContext.resources } returns testContext.resources
        every { settings.toolbarPosition } returns ToolbarPosition.BOTTOM
        every { settings.isTabStripEnabled } returns false
        every { settings.tabGroupsStripEnabled } returns true

        val bottomToolbarHeight = fragment.getBottomToolbarHeight()

        assertEquals(112, bottomToolbarHeight)
    }

    @Test
    @Config(qualifiers = "h481dp") // navbar is only shown on screens taller than 480dp
    fun `GIVEN the tabstrip, toolbar, navigation bar and the microsurvey are shown at bottom WHEN getBottomToolbarHeight THEN returns the combined height`() {
        val configuration =
            Configuration().apply {
                screenHeightDp = 481
            }
        every { mockContext.resources.configuration } returns configuration
        every { mockContext.resources } returns testContext.resources
        setMicrosurveyPromptVisible(true)
        every { settings.shouldUseExpandedToolbar } returns true
        every { settings.toolbarPosition } returns ToolbarPosition.BOTTOM
        every { settings.shouldShowTabStripAtBottom } returns true

        val bottomToolbarHeight = fragment.getBottomToolbarHeight()

        assertEquals(291, bottomToolbarHeight)
    }

    @Test
    fun `GIVEN the tabstrip and toolbar are shown at bottom WHEN getBottomToolbarHeight excludes the tabstrip THEN returns the toolbar height`() {
        every { mockContext.resources } returns testContext.resources
        every { settings.toolbarPosition } returns ToolbarPosition.BOTTOM
        every { settings.shouldShowTabStripAtBottom } returns true

        val bottomToolbarHeight = fragment.getBottomToolbarHeight(includeTabStripIfAvailable = false)

        assertEquals(64, bottomToolbarHeight)
    }

    @Test
    fun `GIVEN the tabstrip and toolbar are shown at top WHEN getTopToolbarHeight THEN returns their combined height`() {
        every { mockContext.resources } returns testContext.resources
        every { settings.toolbarPosition } returns ToolbarPosition.TOP
        every { settings.shouldShowTabStripAtTop } returns true

        val topToolbarHeight = fragment.getTopToolbarHeight()

        assertEquals(120, topToolbarHeight)
    }

    @Test
    fun `GIVEN just the tabstrip is shown at top WHEN getTopToolbarHeight THEN returns the tabstrip height`() {
        every { settings.toolbarPosition } returns ToolbarPosition.BOTTOM
        every { settings.shouldShowTabStripAtTop } returns true

        val topToolbarHeight = fragment.getTopToolbarHeight()

        assertEquals(56, topToolbarHeight)
    }

    @Test
    fun `GIVEN just the toolbar is shown at top WHEN getTopToolbarHeight THEN returns the toolbar height`() {
        every { mockContext.resources } returns testContext.resources
        every { settings.toolbarPosition } returns ToolbarPosition.TOP
        every { settings.shouldShowTabStripAtTop } returns false

        val topToolbarHeight = fragment.getTopToolbarHeight()

        assertEquals(64, topToolbarHeight)
    }

    @Test
    fun `GIVEN the tabstrip and toolbar are shown at top WHEN getTopToolbarHeight excludes the tabstrip THEN returns the toolbar height`() {
        every { mockContext.resources } returns testContext.resources
        every { settings.toolbarPosition } returns ToolbarPosition.TOP
        every { settings.shouldShowTabStripAtTop } returns true

        val topToolbarHeight = fragment.getTopToolbarHeight(includeTabStripIfAvailable = false)

        assertEquals(64, topToolbarHeight)
    }

    @Test
    @Config(qualifiers = "h481dp") // navbar is only shown on screens taller than 480dp
    fun `GIVEN navigation bar and microsurvey is shown at bottom WHEN getBottomToolbarHeight THEN returns the combined height`() {
        val configuration =
            Configuration().apply {
                screenHeightDp = 481
            }
        every { mockContext.resources.configuration } returns configuration
        setMicrosurveyPromptVisible(true)
        every { settings.shouldUseExpandedToolbar } returns true
        every { settings.toolbarPosition } returns ToolbarPosition.TOP

        val bottomToolbarHeight = fragment.getBottomToolbarHeight()

        assertEquals(191, bottomToolbarHeight)
    }

    @Test
    fun `GIVEN navigation bar and microsurvey is shown at bottom WHEN getBottomToolbarHeight with excluded navigation bar THEN the microsurvey height`() {
        setMicrosurveyPromptVisible(true)
        every { settings.shouldUseExpandedToolbar } returns true
        every { settings.toolbarPosition } returns ToolbarPosition.TOP

        val bottomToolbarHeight = fragment.getBottomToolbarHeight(includeNavBarIfEnabled = false)

        assertEquals(131, bottomToolbarHeight)
    }

    @Test
    @Config(qualifiers = "h481dp") // navbar is only shown on screens taller than 480dp
    fun `GIVEN the toolbar and navigation bar are shown at bottom WHEN getBottomToolbarHeight THEN returns the combined height`() {
        val configuration =
            Configuration().apply {
                screenHeightDp = 481
            }
        every { mockContext.resources.configuration } returns configuration
        every { mockContext.resources } returns testContext.resources
        setMicrosurveyPromptVisible(false)
        every { settings.shouldUseExpandedToolbar } returns true
        every { settings.toolbarPosition } returns ToolbarPosition.BOTTOM

        val bottomToolbarHeight = fragment.getBottomToolbarHeight()

        assertEquals(104, bottomToolbarHeight)
    }

    @Test
    @Config(qualifiers = "h481dp") // navbar is only shown on screens taller than 480dp
    fun `GIVEN just the navigation bar shown at bottom WHEN getBottomToolbarHeight THEN returns the navbar height`() {
        val configuration =
            Configuration().apply {
                screenHeightDp = 481
            }
        every { mockContext.resources.configuration } returns configuration
        setMicrosurveyPromptVisible(false)
        every { settings.shouldUseExpandedToolbar } returns true
        every { settings.toolbarPosition } returns ToolbarPosition.TOP

        val bottomToolbarHeight = fragment.getBottomToolbarHeight()

        assertEquals(60, bottomToolbarHeight)
    }

    @Test
    @Config(qualifiers = "h480dp") // navbar is only shown on screens taller than 480dp
    fun `GIVEN a short screen with navigation bar enabled and address bar at top WHEN getBottomToolbarHeight THEN returns 0`() {
        val configuration =
            Configuration().apply {
                screenHeightDp = 480
            }
        every { mockContext.resources.configuration } returns configuration
        setMicrosurveyPromptVisible(false)
        every { settings.shouldUseExpandedToolbar } returns true
        every { settings.toolbarPosition } returns ToolbarPosition.TOP

        val bottomToolbarHeight = fragment.getBottomToolbarHeight()

        assertEquals(0, bottomToolbarHeight)
    }
}
