/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import android.content.Context
import android.content.res.Configuration
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.mozilla.fenix.components.toolbar.ToolbarPosition
import org.mozilla.fenix.navigation.NavControllerProvider
import org.mozilla.fenix.utils.Settings
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class FragmentTest {

    private val navDirections: NavDirections = mockk(relaxed = true)
    private val mockDestination = spyk(NavDestination("hi"))
    private val mockId = 4
    private val navController: NavController = mockk(relaxed = true)
    private val mockOptions: NavOptions = mockk(relaxed = true)
    private val navControllerProvider: NavControllerProvider = mockk(relaxed = true)
    private val settings: Settings = spyk(Settings(testContext))
    private lateinit var mockContext: Context
    private lateinit var fragment: Fragment

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true) {
            every { components.settings } returns settings
        }
        fragment = spyk(Fragment()).apply {
            every { context } returns mockContext
        }

        every { navControllerProvider.getNavController(fragment) } returns navController
        every { navController.currentDestination } returns mockDestination
        every { mockDestination.id } returns mockId
        every { mockContext.resources.getDimensionPixelSize(R.dimen.tab_strip_height) } returns 56
        every { mockContext.resources.getDimensionPixelSize(R.dimen.browser_microsurvey_height) } returns 131
        every { mockContext.resources.getDimensionPixelSize(R.dimen.browser_navbar_height) } returns 60
        every { mockContext.resources.getDimensionPixelSize(R.dimen.browser_navbar_height_small) } returns 48
    }

    @Test
    fun `Test nav fun with ID and directions`() {
        every { navController.navigate(navDirections, null) } just Runs

        fragment.nav(mockId, navDirections, navControllerProvider = navControllerProvider)
        verify { navController.currentDestination }
        verify { navController.navigate(navDirections, null) }
        confirmVerified(fragment)
    }

    @Test
    fun `Test nav fun with ID, directions, and options`() {
        every { navController.navigate(navDirections, mockOptions) } just Runs

        fragment.nav(mockId, navDirections, mockOptions, navControllerProvider = navControllerProvider)
        verify { navController.currentDestination }
        verify { navController.navigate(navDirections, mockOptions) }
        confirmVerified(fragment)
    }

    @Test
    fun `GIVEN the composable toolbar and the microsurvey are shown at bottom WHEN getBottomToolbarHeight THEN returns the combined height`() {
        every { mockContext.resources } returns testContext.resources
        every { settings.shouldShowMicrosurveyPrompt } returns true
        every { settings.toolbarPosition } returns ToolbarPosition.BOTTOM

        val bottomToolbarHeight = fragment.getBottomToolbarHeight()

        assertEquals(195, bottomToolbarHeight)
    }

    @Test
    fun `GIVEN just the microsurvey is shown at bottom WHEN getBottomToolbarHeight THEN returns it's height`() {
        every { settings.shouldShowMicrosurveyPrompt } returns true
        every { settings.toolbarPosition } returns ToolbarPosition.TOP

        val bottomToolbarHeight = fragment.getBottomToolbarHeight()

        assertEquals(131, bottomToolbarHeight)
    }

    @Test
    @Config(qualifiers = "h481dp") // navbar is only shown on screens taller than 480dp
    fun `GIVEN just the composable toolbar shown at bottom WHEN getBottomToolbarHeight THEN returns it's height`() {
        every { mockContext.resources } returns testContext.resources
        every { settings.shouldShowMicrosurveyPrompt } returns false
        every { settings.toolbarPosition } returns ToolbarPosition.BOTTOM

        val bottomToolbarHeight = fragment.getBottomToolbarHeight()

        assertEquals(64, bottomToolbarHeight)
    }

    @Test
    @Config(qualifiers = "h481dp") // navbar is only shown on screens taller than 480dp
    fun `GIVEN the composable toolbar, navigation bar and the microsurvey are shown at bottom WHEN getBottomToolbarHeight with excluded navigation bar THEN returns the combined height minus navigation bar`() {
        val configuration = Configuration().apply {
            screenHeightDp = 481
        }
        every { mockContext.resources.configuration } returns configuration
        every { mockContext.resources } returns testContext.resources
        every { settings.shouldShowMicrosurveyPrompt } returns true
        every { settings.shouldUseExpandedToolbar } returns true
        every { settings.toolbarPosition } returns ToolbarPosition.BOTTOM

        val bottomToolbarHeight = fragment.getBottomToolbarHeight(includeNavBarIfEnabled = false)

        assertEquals(187, bottomToolbarHeight)
    }

    @Test
    @Config(qualifiers = "h481dp") // navbar is only shown on screens taller than 480dp
    fun `GIVEN the composable toolbar, navigation bar and the microsurvey are shown at bottom WHEN getBottomToolbarHeight THEN returns the combined height`() {
        val configuration = Configuration().apply {
            screenHeightDp = 481
        }
        every { mockContext.resources.configuration } returns configuration
        every { mockContext.resources } returns testContext.resources
        every { settings.shouldShowMicrosurveyPrompt } returns true
        every { settings.shouldUseExpandedToolbar } returns true
        every { settings.toolbarPosition } returns ToolbarPosition.BOTTOM

        val bottomToolbarHeight = fragment.getBottomToolbarHeight()

        assertEquals(235, bottomToolbarHeight)
    }

    @Test
    @Config(qualifiers = "h481dp") // navbar is only shown on screens taller than 480dp
    fun `GIVEN navigation bar and microsurvey is shown at bottom WHEN getBottomToolbarHeight THEN returns the combined height`() {
        val configuration = Configuration().apply {
            screenHeightDp = 481
        }
        every { mockContext.resources.configuration } returns configuration
        every { settings.shouldShowMicrosurveyPrompt } returns true
        every { settings.shouldUseExpandedToolbar } returns true
        every { settings.toolbarPosition } returns ToolbarPosition.TOP

        val bottomToolbarHeight = fragment.getBottomToolbarHeight()

        assertEquals(191, bottomToolbarHeight)
    }

    @Test
    fun `GIVEN navigation bar and microsurvey is shown at bottom WHEN getBottomToolbarHeight with excluded navigation bar THEN the microsurvey height`() {
        every { settings.shouldShowMicrosurveyPrompt } returns true
        every { settings.shouldUseExpandedToolbar } returns true
        every { settings.toolbarPosition } returns ToolbarPosition.TOP

        val bottomToolbarHeight = fragment.getBottomToolbarHeight(includeNavBarIfEnabled = false)

        assertEquals(131, bottomToolbarHeight)
    }

    @Test
    @Config(qualifiers = "h481dp") // navbar is only shown on screens taller than 480dp
    fun `GIVEN the composable toolbar and navigation bar are shown at bottom WHEN getBottomToolbarHeight THEN returns the combined height`() {
        val configuration = Configuration().apply {
            screenHeightDp = 481
        }
        every { mockContext.resources.configuration } returns configuration
        every { mockContext.resources } returns testContext.resources
        every { settings.shouldShowMicrosurveyPrompt } returns false
        every { settings.shouldUseExpandedToolbar } returns true
        every { settings.toolbarPosition } returns ToolbarPosition.BOTTOM

        val bottomToolbarHeight = fragment.getBottomToolbarHeight()

        assertEquals(104, bottomToolbarHeight)
    }

    @Test
    @Config(qualifiers = "h481dp") // navbar is only shown on screens taller than 480dp
    fun `GIVEN just the navigation bar shown at bottom WHEN getBottomToolbarHeight THEN returns the navbar height`() {
        val configuration = Configuration().apply {
            screenHeightDp = 481
        }
        every { mockContext.resources.configuration } returns configuration
        every { settings.shouldShowMicrosurveyPrompt } returns false
        every { settings.shouldUseExpandedToolbar } returns true
        every { settings.toolbarPosition } returns ToolbarPosition.TOP

        val bottomToolbarHeight = fragment.getBottomToolbarHeight()

        assertEquals(60, bottomToolbarHeight)
    }

    @Test
    @Config(qualifiers = "h480dp") // navbar is only shown on screens taller than 480dp
    fun `GIVEN a short screen with navigation bar enabled and address bar at top WHEN getBottomToolbarHeight THEN returns 0`() {
        val configuration = Configuration().apply {
            screenHeightDp = 480
        }
        every { mockContext.resources.configuration } returns configuration
        every { settings.shouldShowMicrosurveyPrompt } returns false
        every { settings.shouldUseExpandedToolbar } returns true
        every { settings.toolbarPosition } returns ToolbarPosition.TOP

        val bottomToolbarHeight = fragment.getBottomToolbarHeight()

        assertEquals(0, bottomToolbarHeight)
    }

    @Test
    fun `WHEN allowScreenCaptureInSecureScreens is true and a fragment is secured THEN FLAG_SECURE flag is added to the LayoutParams`() {
        every { settings.allowScreenCaptureInSecureScreens } returns false

        val testFragment = TestFragment(mockContext)

        startFragment(testFragment)

        testFragment.secure()

        val flags = testFragment.requireActivity().window.attributes.flags
        assertTrue(flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
    }

    @Test
    fun `WHEN allowScreenCaptureInSecureScreens is false and a fragment is secured THEN no window flags are added to the LayoutParams`() {
        every { settings.allowScreenCaptureInSecureScreens } returns true

        val testFragment = TestFragment(mockContext)

        startFragment(testFragment)

        testFragment.secure()

        val flags = testFragment.requireActivity().window.attributes.flags
        assertFalse(flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
    }

    class TestFragment(private val mockContext: Context) : Fragment() {
        override fun getContext(): Context? {
            return mockContext
        }
    }

    private fun startFragment(fragment: Fragment) {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java)
            .create()
            .start()
            .resume()
            .get()
        activity.supportFragmentManager.beginTransaction()
            .add(fragment, null)
            .commitNow()
    }
}
