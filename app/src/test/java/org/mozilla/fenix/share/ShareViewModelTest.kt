/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.share

import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mozilla.components.feature.share.RecentApp
import mozilla.components.feature.share.RecentAppsStorage
import mozilla.components.service.fxa.manager.FxaAccountManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.share.DefaultShareController.Companion.ACTION_COPY_LINK_TO_CLIPBOARD
import org.mozilla.fenix.share.ShareViewModel.Companion.RECENT_APPS_LIMIT
import org.mozilla.fenix.share.listadapters.AppShareOption
import org.mozilla.fenix.share.listadapters.SyncShareOption
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShareViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val packageName = "org.packageName"
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var fxaAccountManager: FxaAccountManager
    private lateinit var viewModel: ShareViewModel
    private lateinit var storage: RecentAppsStorage

    @Before
    fun setup() {
        connectivityManager = mockk(relaxed = true)
        fxaAccountManager = mockk(relaxed = true)
        storage = mockk(relaxUnitFun = true)

        val mockCopyApp =
            AppShareOption(
                "Copy",
                mockk(),
                ACTION_COPY_LINK_TO_CLIPBOARD,
                "",
            )

        viewModel =
            spyk(
                ShareViewModel(
                    fxaAccountManager,
                    storage,
                    connectivityManager,
                    testDispatcher,
                    mockk(),
                    packageName,
                    { mockCopyApp },
                )
            )
    }

    @Test
    fun `uiState should be initialized with loading true and empty lists`() {
        val state = viewModel.uiState.value
        assertTrue(state.isLoading)
        assertTrue(state.devices.isEmpty())
        assertTrue(state.recentApps.isEmpty())
        assertTrue(state.otherApps.isEmpty())
    }

    @Test
    fun `initDataLoad updates state with apps and devices`() =
        runTest(testDispatcher) {
            val appOptions = listOf(AppShareOption("Label", mockk(), "Package", "Activity"))

            val appEntity = mockk<RecentApp>()
            every { appEntity.activityName } returns "Activity"
            val recentAppOptions = listOf(appEntity)

            coEvery { storage.getRecentAppsUpTo(RECENT_APPS_LIMIT) } returns recentAppOptions
            coEvery { viewModel.buildAppsList(any()) } returns appOptions
            coEvery { viewModel.buildDeviceList(any()) } returns listOf(SyncShareOption.Offline)

            viewModel.initDataLoad()
            testDispatcher.scheduler.advanceUntilIdle()

            verify {
                connectivityManager.registerNetworkCallback(
                    any(),
                    any<ConnectivityManager.NetworkCallback>(),
                )
            }

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(1, state.recentApps.size)
            assertEquals(1, state.otherApps.size)
            assertEquals(listOf(SyncShareOption.Offline), state.devices)
        }

    @Test
    fun `buildAppsList transforms ResolveInfo list`() =
        runTest(testDispatcher) {
            val icon1: Drawable = mockk()
            val icon2: Drawable = mockk()

            val info =
                listOf(
                    createResolveInfo("App 0", icon1, "package 0", "activity 0"),
                    createResolveInfo("Self", mockk(), packageName, "activity self"),
                    createResolveInfo("App 1", icon2, "package 1", "activity 1"),
                )
            val expected =
                listOf(
                    AppShareOption("App 0", icon1, "package 0", "activity 0"),
                    AppShareOption("App 1", icon2, "package 1", "activity 1"),
                )

            val result = viewModel.buildAppsList(info)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(expected, result)
        }

    @Test
    fun `buildDeviceList returns offline option`() =
        runTest(testDispatcher) {
            every { viewModel.isOffline(any()) } returns true
            assertEquals(listOf(SyncShareOption.Offline), viewModel.buildDeviceList())

            // every { connectivityManager.isOnline(any()) } returns false
            assertEquals(listOf(SyncShareOption.Offline), viewModel.buildDeviceList())
        }

    @Test
    fun `GIVEN only one app THEN show copy to clipboard before the app`() =
        runTest(testDispatcher) {
            val appOptions = listOf(AppShareOption("Label", mockk(), "Package", "Activity"))

            coEvery { storage.getRecentAppsUpTo(RECENT_APPS_LIMIT) } returns emptyList()
            coEvery { viewModel.buildAppsList(any()) } returns appOptions

            viewModel.initDataLoad()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(0, state.recentApps.size)
            // 1 app + 1 copy action = 2
            assertEquals(2, state.otherApps.size)
            assertEquals(ACTION_COPY_LINK_TO_CLIPBOARD, state.otherApps[0].packageName)
        }

    @Test
    fun `WHEN no apps found THEN at least have copy to clipboard available`() =
        runTest(testDispatcher) {
            coEvery { storage.getRecentAppsUpTo(RECENT_APPS_LIMIT) } returns emptyList()
            coEvery { viewModel.getIntentActivities(any()) } returns emptyList()
            coEvery { viewModel.buildAppsList(any()) } returns emptyList()

            viewModel.initDataLoad()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(0, state.recentApps.size)
            assertEquals(1, state.otherApps.size)
            assertEquals(ACTION_COPY_LINK_TO_CLIPBOARD, state.otherApps[0].packageName)
        }

    private fun createResolveInfo(
        label: String,
        icon: Drawable,
        packageName: String,
        name: String,
    ): ResolveInfo {
        val info =
            ResolveInfo().apply {
                activityInfo = ActivityInfo()
                activityInfo.packageName = packageName
                activityInfo.name = name
            }
        val spy = spyk(info)
        every { spy.loadLabel(any()) } returns label
        every { spy.loadIcon(any()) } returns icon
        return spy
    }
}
