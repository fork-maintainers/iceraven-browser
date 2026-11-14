/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import android.content.Context
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
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.navigation.NavControllerProvider
import org.mozilla.fenix.utils.Settings
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

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
        mockContext =
            mockk(relaxed = true) {
                every { components.settings } returns settings
            }
        fragment =
            spyk(Fragment()).apply {
                every { context } returns mockContext
            }

        every { navControllerProvider.getNavController(fragment) } returns navController
        every { navController.currentDestination } returns mockDestination
        every { mockDestination.id } returns mockId
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
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().start().resume().get()
        activity.supportFragmentManager.beginTransaction().add(fragment, null).commitNow()
    }
}
