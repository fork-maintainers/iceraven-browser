/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.share

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlin.test.assertNotNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mozilla.components.concept.engine.prompt.ShareData
import mozilla.components.concept.sync.Device
import mozilla.components.concept.sync.DeviceType
import mozilla.components.concept.sync.TabData
import mozilla.components.concept.sync.TabPrivacy
import mozilla.components.feature.accounts.push.SendTabUseCases
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.share.RecentAppsStorage
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.GleanMetrics.Events
import org.mozilla.fenix.GleanMetrics.SyncAccount
import org.mozilla.fenix.R
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.accounts.FenixFxAEntryPoint
import org.mozilla.fenix.components.appstate.AppAction.ShareAction
import org.mozilla.fenix.helpers.FenixGleanTestRule
import org.mozilla.fenix.share.listadapters.AppShareOption
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShareControllerTest {
    private val appStore: AppStore = mockk(relaxed = true)
    private val shareSubject = "shareSubject"
    private val shareData =
        listOf(
            ShareData(url = "url0", title = "title0", private = false),
            ShareData(url = "url1", title = "title1", private = false),
        )

    // Navigation between app fragments uses ShareTab as arguments. SendTabUseCases uses TabData.
    private val tabsData =
        listOf(
            TabData("title0", "url0", TabPrivacy.Normal),
            TabData("title1", "url1", TabPrivacy.Normal),
        )
    private val textToShare = "${shareData[0].url}\n\n${shareData[1].url}"
    private val sendTabUseCases = mockk<SendTabUseCases>(relaxed = true)
    private val saveToPdfUseCase = mockk<SessionUseCases.SaveToPdfUseCase>(relaxed = true)
    private val printUseCase = mockk<SessionUseCases.PrintContentUseCase>(relaxed = true)
    private val sentFromFirefoxManager = mockk<SentFromFirefoxManager>(relaxed = true)
    private val navController =
        mockk<NavController>(relaxed = true) {
            every { navigate(any<NavDirections>(), any<NavOptions>()) } just runs
            every { currentDestination?.id } returns R.id.shareFragment
        }

    private val dismissSink: DismissSink = mockk(relaxed = true)
    private val dismiss: (ShareController.Result) -> Unit = { dismissSink.onDismiss(it) }
    private val recentAppStorage = mockk<RecentAppsStorage>(relaxed = true)

    @get:Rule val gleanTestRule = FenixGleanTestRule(testContext)

    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun `handleShareClosed should call a passed in delegate to close this`() =
        runTest(testDispatcher) {
            val controller =
                DefaultShareController(
                    testContext,
                    appStore,
                    shareSubject,
                    shareData,
                    sendTabUseCases,
                    saveToPdfUseCase,
                    printUseCase,
                    sentFromFirefoxManager,
                    navController,
                    recentAppStorage,
                    this,
                    this,
                    dismiss = dismiss,
                )

            controller.handleShareClosed()

            verify { dismissSink.onDismiss(ShareController.Result.DISMISSED) }
        }

    @Test
    fun `handleShareToApp should start a new sharing activity and close this`() =
        runTest(testDispatcher) {
            assertNull(Events.shareToApp.testGetValue())

            val appPackageName = "package"
            val appClassName = "activity"
            val appShareOption = AppShareOption("app", mockk(), appPackageName, appClassName)
            val shareIntent = slot<Intent>()
            // Our share Intent uses `FLAG_ACTIVITY_NEW_TASK` but when resolving the startActivity call
            // needed for capturing the actual Intent used the `slot` one doesn't have this flag so we
            // need to use an Activity Context.
            val activityContext: Context = mockk<Activity>()
            val testController =
                DefaultShareController(
                    activityContext,
                    appStore,
                    shareSubject,
                    shareData,
                    mockk(),
                    mockk(),
                    mockk(),
                    sentFromFirefoxManager,
                    mockk(),
                    recentAppStorage,
                    this,
                    this,
                    FenixFxAEntryPoint.ShareMenu,
                    dismiss,
                )
            every { activityContext.startActivity(capture(shareIntent)) } just Runs
            coEvery { recentAppStorage.updateRecentApp(appShareOption.activityName) } just Runs
            every { sentFromFirefoxManager.maybeAppendShareText(any(), any()) } returns textToShare

            testController.handleShareToApp(appShareOption)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                "shareToApp event only called once",
                1,
                Events.shareToApp.testGetValue()?.size,
            )
            assertEquals(
                "other",
                Events.shareToApp.testGetValue()?.last()?.extra?.getValue("app_package"),
            )

            // Check that the Intent used for querying apps has the expected structure
            assertTrue(shareIntent.isCaptured)
            assertEquals(Intent.ACTION_SEND, shareIntent.captured.action)
            @Suppress("DEPRECATION") assertEquals(shareSubject, shareIntent.captured.extras!![Intent.EXTRA_SUBJECT])
            @Suppress("DEPRECATION") assertEquals(textToShare, shareIntent.captured.extras!![Intent.EXTRA_TEXT])
            assertEquals("text/plain", shareIntent.captured.type)
            assertEquals(
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT + Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
                shareIntent.captured.flags,
            )
            assertEquals(appPackageName, shareIntent.captured.component!!.packageName)
            assertEquals(appClassName, shareIntent.captured.component!!.className)

            coVerify { recentAppStorage.updateRecentApp(appShareOption.activityName) }
            verifyOrder {
                activityContext.startActivity(shareIntent.captured)
                dismissSink.onDismiss(ShareController.Result.SUCCESS)
            }
        }

    @Test
    fun `handleShareToApp should record to telemetry packages which are in allowed list`() =
        runTest(testDispatcher) {
            assertNull(Events.shareToApp.testGetValue())

            val appPackageName = "com.android.bluetooth"
            val appClassName = "activity"
            val appShareOption = AppShareOption("app", mockk(), appPackageName, appClassName)
            val shareIntent = slot<Intent>()
            // Our share Intent uses `FLAG_ACTIVITY_NEW_TASK` but when resolving the startActivity call
            // needed for capturing the actual Intent used the `slot` one doesn't have this flag so we
            // need to use an Activity Context.
            val activityContext: Context = mockk<Activity>()
            val testController =
                DefaultShareController(
                    activityContext,
                    appStore,
                    shareSubject,
                    shareData,
                    mockk(),
                    mockk(),
                    mockk(),
                    sentFromFirefoxManager,
                    mockk(),
                    recentAppStorage,
                    this,
                    this,
                    FenixFxAEntryPoint.ShareMenu,
                    dismiss,
                )

            every { activityContext.startActivity(capture(shareIntent)) } just Runs
            coEvery { recentAppStorage.updateRecentApp(appShareOption.activityName) } just Runs

            testController.handleShareToApp(appShareOption)

            assertEquals(
                "shareToApp event only called once",
                1,
                Events.shareToApp.testGetValue()?.size,
            )
            assertEquals(
                "com.android.bluetooth",
                Events.shareToApp.testGetValue()?.last()?.extra?.getValue("app_package"),
            )
        }

    @Test
    fun `handleShareToApp should record to telemetry as other when app package not in allowed list`() =
        runTest(testDispatcher) {
            assertNull(Events.shareToApp.testGetValue())

            val appPackageName = "com.package.record.not.allowed"
            val appClassName = "activity"
            val appShareOption = AppShareOption("app", mockk(), appPackageName, appClassName)
            val shareIntent = slot<Intent>()
            // Our share Intent uses `FLAG_ACTIVITY_NEW_TASK` but when resolving the startActivity call
            // needed for capturing the actual Intent used the `slot` one doesn't have this flag so we
            // need to use an Activity Context.
            val activityContext: Context = mockk<Activity>()
            val testController =
                DefaultShareController(
                    activityContext,
                    appStore,
                    shareSubject,
                    shareData,
                    mockk(),
                    mockk(),
                    mockk(),
                    sentFromFirefoxManager,
                    mockk(),
                    recentAppStorage,
                    this,
                    this,
                    FenixFxAEntryPoint.ShareMenu,
                    dismiss,
                )

            every { activityContext.startActivity(capture(shareIntent)) } just Runs
            coEvery { recentAppStorage.updateRecentApp(appShareOption.activityName) } just Runs

            testController.handleShareToApp(appShareOption)

            // Only called once and package is not in the allowed telemetry list so this should record "other"
            assertEquals(
                "shareToApp event only called once",
                1,
                Events.shareToApp.testGetValue()?.size,
            )
            assertEquals(
                "other",
                Events.shareToApp.testGetValue()?.last()?.extra?.getValue("app_package"),
            )
        }

    @Test
    fun `handleShareToApp should dismiss with an error start when a security exception occurs`() =
        runTest(testDispatcher) {
            val appPackageName = "package"
            val appClassName = "activity"
            val appShareOption = AppShareOption("app", mockk(), appPackageName, appClassName)
            val shareIntent = slot<Intent>()
            // Our share Intent uses `FLAG_ACTIVITY_NEW_TASK` but when resolving the startActivity call
            // needed for capturing the actual Intent used the `slot` one doesn't have this flag so we
            // need to use an Activity Context.
            val activityContext: Context = mockk<Activity>()
            val testController =
                DefaultShareController(
                    context = activityContext,
                    appStore = appStore,
                    shareSubject = shareSubject,
                    shareData = shareData,
                    sendTabUseCases = mockk(),
                    saveToPdfUseCase = mockk(),
                    printUseCase = mockk(),
                    sentFromFirefoxManager = sentFromFirefoxManager,
                    navController = mockk(),
                    recentAppsStorage = recentAppStorage,
                    scope = this,
                    applicationScope = this,
                    dismiss = dismiss,
                )
            coEvery { recentAppStorage.updateRecentApp(appShareOption.activityName) } just Runs
            every { activityContext.startActivity(capture(shareIntent)) } throws SecurityException()
            every { activityContext.getString(R.string.share_error_snackbar) } returns "Cannot share to this app"

            testController.handleShareToApp(appShareOption)

            verifyOrder {
                activityContext.startActivity(shareIntent.captured)
                appStore.dispatch(ShareAction.ShareToAppFailed)
                dismissSink.onDismiss(ShareController.Result.SHARE_ERROR)
            }
        }

    @Test
    fun `handleShareToApp should dismiss with an error start when a ActivityNotFoundException occurs`() =
        runTest(testDispatcher) {
            val appPackageName = "package"
            val appClassName = "activity"
            val appShareOption = AppShareOption("app", mockk(), appPackageName, appClassName)
            val shareIntent = slot<Intent>()
            // Our share Intent uses `FLAG_ACTIVITY_NEW_TASK` but when resolving the startActivity call
            // needed for capturing the actual Intent used the `slot` one doesn't have this flag so we
            // need to use an Activity Context.
            val activityContext: Context = mockk<Activity>()
            val testController =
                DefaultShareController(
                    context = activityContext,
                    appStore = appStore,
                    shareSubject = shareSubject,
                    shareData = shareData,
                    sendTabUseCases = mockk(),
                    saveToPdfUseCase = mockk(),
                    printUseCase = mockk(),
                    sentFromFirefoxManager = sentFromFirefoxManager,
                    navController = mockk(),
                    recentAppsStorage = recentAppStorage,
                    scope = this,
                    applicationScope = this,
                    dismiss = dismiss,
                )
            coEvery { recentAppStorage.updateRecentApp(appShareOption.activityName) } just Runs
            every { activityContext.startActivity(capture(shareIntent)) } throws ActivityNotFoundException()
            every { activityContext.getString(R.string.share_error_snackbar) } returns "Cannot share to this app"

            testController.handleShareToApp(appShareOption)

            verifyOrder {
                activityContext.startActivity(shareIntent.captured)
                appStore.dispatch(ShareAction.ShareToAppFailed)
                dismissSink.onDismiss(ShareController.Result.SHARE_ERROR)
            }
        }

    @Test
    fun `WHEN handleSaveToPDF close the dialog and save the page to pdf`() =
        runTest(testDispatcher) {
            val testController =
                DefaultShareController(
                    context = mockk(),
                    appStore = appStore,
                    shareSubject = shareSubject,
                    shareData = shareData,
                    sendTabUseCases = mockk(),
                    saveToPdfUseCase = saveToPdfUseCase,
                    printUseCase = mockk(),
                    sentFromFirefoxManager = sentFromFirefoxManager,
                    navController = mockk(),
                    recentAppsStorage = recentAppStorage,
                    scope = this,
                    applicationScope = this,
                    dismiss = dismiss,
                )

            testController.handleSaveToPDF("tabID")

            verify {
                saveToPdfUseCase.invoke("tabID")
                dismissSink.onDismiss(ShareController.Result.DISMISSED)
            }
        }

    @Test
    fun `WHEN handlePrint close the dialog and print the page AND send tapped telemetry`() =
        runTest(testDispatcher) {
            val testController =
                DefaultShareController(
                    context = mockk(),
                    appStore = appStore,
                    shareSubject = shareSubject,
                    shareData = shareData,
                    sendTabUseCases = mockk(),
                    saveToPdfUseCase = mockk(),
                    printUseCase = printUseCase,
                    sentFromFirefoxManager = sentFromFirefoxManager,
                    navController = mockk(),
                    recentAppsStorage = recentAppStorage,
                    scope = this,
                    applicationScope = this,
                    dismiss = dismiss,
                )

            testController.handlePrint("tabID")

            verify {
                printUseCase.invoke("tabID")
                dismissSink.onDismiss(ShareController.Result.DISMISSED)
            }

            assertNotNull(Events.shareMenuAction.testGetValue())
            val printTapped = Events.shareMenuAction.testGetValue()!!
            assertEquals(1, printTapped.size)
            assertEquals("print", printTapped.single().extra?.getValue("item"))
        }

    @Test
    fun `getShareSubject should return the shareSubject when shareSubject is not null`() =
        runTest(testDispatcher) {
            val activityContext: Context = mockk<Activity>()
            val testController =
                DefaultShareController(
                    context = activityContext,
                    appStore = appStore,
                    shareSubject = shareSubject,
                    shareData = shareData,
                    sendTabUseCases = mockk(),
                    saveToPdfUseCase = mockk(),
                    printUseCase = mockk(),
                    sentFromFirefoxManager = sentFromFirefoxManager,
                    navController = mockk(),
                    recentAppsStorage = recentAppStorage,
                    scope = this,
                    applicationScope = this,
                    dismiss = dismiss,
                )

            assertEquals(shareSubject, testController.getShareSubject())
        }

    @Test
    fun `getShareSubject should return a combination of non-null titles when shareSubject is null`() =
        runTest(testDispatcher) {
            val activityContext: Context = mockk<Activity>()
            val testController =
                DefaultShareController(
                    context = activityContext,
                    appStore = appStore,
                    shareSubject = null,
                    shareData = shareData,
                    sendTabUseCases = mockk(),
                    saveToPdfUseCase = mockk(),
                    printUseCase = mockk(),
                    sentFromFirefoxManager = sentFromFirefoxManager,
                    navController = mockk(),
                    recentAppsStorage = recentAppStorage,
                    scope = this,
                    applicationScope = this,
                    dismiss = dismiss,
                )

            assertEquals("title0, title1", testController.getShareSubject())
        }

    @Test
    fun `getShareSubject should return just the not null titles string when shareSubject is  null`() =
        runTest(testDispatcher) {
            val activityContext: Context = mockk<Activity>()
            val partialTitlesShareData =
                listOf(
                    ShareData(url = "url0", title = null, private = false),
                    ShareData(url = "url1", title = "title1", private = false),
                )
            val testController =
                DefaultShareController(
                    context = activityContext,
                    appStore = appStore,
                    shareSubject = null,
                    shareData = partialTitlesShareData,
                    sendTabUseCases = mockk(),
                    saveToPdfUseCase = mockk(),
                    printUseCase = mockk(),
                    sentFromFirefoxManager = sentFromFirefoxManager,
                    navController = mockk(),
                    recentAppsStorage = recentAppStorage,
                    scope = this,
                    applicationScope = this,
                    dismiss = dismiss,
                )

            assertEquals("title1", testController.getShareSubject())
        }

    @Test
    fun `getShareSubject should return empty string when shareSubject and all titles are null`() =
        runTest(testDispatcher) {
            val activityContext: Context = mockk<Activity>()
            val noTitleShareData =
                listOf(
                    ShareData(url = "url0", title = null, private = false),
                    ShareData(url = "url1", title = null, private = false),
                )
            val testController =
                DefaultShareController(
                    context = activityContext,
                    appStore = appStore,
                    shareSubject = null,
                    shareData = noTitleShareData,
                    sendTabUseCases = mockk(),
                    saveToPdfUseCase = mockk(),
                    printUseCase = mockk(),
                    sentFromFirefoxManager = sentFromFirefoxManager,
                    navController = mockk(),
                    recentAppsStorage = recentAppStorage,
                    scope = this,
                    applicationScope = this,
                    dismiss = dismiss,
                )

            assertEquals("", testController.getShareSubject())
        }

    @Test
    fun `getShareSubject should return empty string when shareSubject is null and and all titles are empty`() =
        runTest(testDispatcher) {
            val activityContext: Context = mockk<Activity>()
            val noTitleShareData =
                listOf(
                    ShareData(url = "url0", title = "", private = false),
                    ShareData(url = "url1", title = "", private = false),
                )
            val testController =
                DefaultShareController(
                    appStore = appStore,
                    context = activityContext,
                    shareSubject = null,
                    shareData = noTitleShareData,
                    sendTabUseCases = mockk(),
                    saveToPdfUseCase = mockk(),
                    printUseCase = mockk(),
                    sentFromFirefoxManager = sentFromFirefoxManager,
                    navController = mockk(),
                    recentAppsStorage = recentAppStorage,
                    scope = this,
                    applicationScope = this,
                    dismiss = dismiss,
                )

            assertEquals("", testController.getShareSubject())
        }

    @Test
    fun `handleShareToDevice should share to account device, inform callbacks and dismiss`() =
        runTest(testDispatcher) {
            val controller =
                DefaultShareController(
                    testContext,
                    appStore,
                    shareSubject,
                    shareData,
                    sendTabUseCases,
                    saveToPdfUseCase,
                    printUseCase,
                    sentFromFirefoxManager,
                    navController,
                    recentAppStorage,
                    this,
                    this,
                    FenixFxAEntryPoint.ShareMenu,
                    dismiss,
                )

            val deviceToShareTo =
                Device(
                    "deviceId",
                    "deviceName",
                    DeviceType.UNKNOWN,
                    false,
                    0L,
                    emptyList(),
                    false,
                    null,
                )
            val deviceId = slot<String>()
            val tabsShared = slot<List<TabData>>()

            every {
                sendTabUseCases.sendToDeviceAsync(
                    any<String>(),
                    any<List<TabData>>(),
                )
            } returns CompletableDeferred(true)
            every { navController.currentDestination?.id } returns R.id.shareFragment

            controller.handleShareToDevice(deviceToShareTo)
            testDispatcher.scheduler.advanceUntilIdle()

            assertNotNull(SyncAccount.sendTab.testGetValue())
            assertEquals(1, SyncAccount.sendTab.testGetValue()!!.size)
            assertNull(SyncAccount.sendTab.testGetValue()!!.single().extra)

            verifyOrder {
                sendTabUseCases.sendToDeviceAsync(capture(deviceId), capture(tabsShared))
                dismissSink.onDismiss(ShareController.Result.SUCCESS)
            }

            assertTrue(deviceId.isCaptured)
            assertEquals(deviceToShareTo.id, deviceId.captured)
            assertTrue(tabsShared.isCaptured)
            assertEquals(tabsData, tabsShared.captured)
        }

    @Test
    fun `handleShareToAllDevices calls handleShareToDevice multiple times`() =
        runTest(testDispatcher) {
            val controller =
                DefaultShareController(
                    testContext,
                    appStore,
                    shareSubject,
                    shareData,
                    sendTabUseCases,
                    saveToPdfUseCase,
                    printUseCase,
                    sentFromFirefoxManager,
                    navController,
                    recentAppStorage,
                    this,
                    this,
                    FenixFxAEntryPoint.ShareMenu,
                    dismiss,
                )

            every { sendTabUseCases.sendToAllAsync(any<List<TabData>>()) } returns CompletableDeferred(true)
            every { navController.currentDestination?.id } returns R.id.shareFragment

            val devicesToShareTo =
                listOf(
                    Device(
                        "deviceId0",
                        "deviceName0",
                        DeviceType.UNKNOWN,
                        false,
                        0L,
                        emptyList(),
                        false,
                        null,
                    ),
                    Device(
                        "deviceId1",
                        "deviceName1",
                        DeviceType.UNKNOWN,
                        true,
                        1L,
                        emptyList(),
                        false,
                        null,
                    ),
                )
            val tabsShared = slot<List<TabData>>()

            controller.handleShareToAllDevices(devicesToShareTo)
            testDispatcher.scheduler.advanceUntilIdle()

            verifyOrder {
                sendTabUseCases.sendToAllAsync(capture(tabsShared))
                dismissSink.onDismiss(ShareController.Result.SUCCESS)
            }

            // SendTabUseCases should send a the `shareTabs` mapped to tabData
            assertTrue(tabsShared.isCaptured)
            assertEquals(tabsData, tabsShared.captured)
        }

    @Test
    fun `handleSignIn should navigate to the Sync Fragment and dismiss this one`() =
        runTest(testDispatcher) {
            val controller =
                DefaultShareController(
                    testContext,
                    appStore,
                    shareSubject,
                    shareData,
                    sendTabUseCases,
                    saveToPdfUseCase,
                    printUseCase,
                    sentFromFirefoxManager,
                    navController,
                    recentAppStorage,
                    this,
                    this,
                    FenixFxAEntryPoint.ShareMenu,
                    dismiss,
                )

            controller.handleSignIn()

            assertNotNull(SyncAccount.signInToSendTab.testGetValue())
            assertEquals(1, SyncAccount.signInToSendTab.testGetValue()!!.size)
            assertNull(SyncAccount.signInToSendTab.testGetValue()!!.single().extra)

            verifyOrder {
                navController.navigate(
                    ShareFragmentDirections.actionGlobalTurnOnSync(entrypoint = FenixFxAEntryPoint.ShareMenu),
                    null,
                )
                dismissSink.onDismiss(ShareController.Result.DISMISSED)
            }
        }

    @Test
    fun `handleReauth should navigate to the Account Problem Fragment and dismiss this one`() =
        runTest(testDispatcher) {
            val controller =
                DefaultShareController(
                    testContext,
                    appStore,
                    shareSubject,
                    shareData,
                    sendTabUseCases,
                    saveToPdfUseCase,
                    printUseCase,
                    sentFromFirefoxManager,
                    navController,
                    recentAppStorage,
                    this,
                    this,
                    FenixFxAEntryPoint.ShareMenu,
                    dismiss,
                )

            controller.handleReauth()

            verifyOrder {
                navController.navigate(
                    ShareFragmentDirections.actionGlobalAccountProblemFragment(
                        entrypoint = FenixFxAEntryPoint.ShareMenu
                    ),
                    null,
                )
                dismissSink.onDismiss(ShareController.Result.DISMISSED)
            }
        }

    @Test
    fun `showSuccess should update AppStore with a success action`() =
        runTest(testDispatcher) {
            val controller =
                DefaultShareController(
                    testContext,
                    appStore,
                    shareSubject,
                    shareData,
                    sendTabUseCases,
                    saveToPdfUseCase,
                    printUseCase,
                    sentFromFirefoxManager,
                    navController,
                    recentAppStorage,
                    this,
                    this,
                    FenixFxAEntryPoint.ShareMenu,
                    dismiss,
                )

            val destinations = listOf("a", "b")
            val expectedTabsShared = with(controller) { shareData.toTabData() }

            controller.showSuccess(destinations)

            verify {
                appStore.dispatch(
                    ShareAction.SharedTabsSuccessfully(
                        destinations,
                        expectedTabsShared,
                    )
                )
            }
        }

    @Test
    fun `showFailureWithRetryOption should update AppStore with a failure action`() =
        runTest(testDispatcher) {
            val controller =
                DefaultShareController(
                    testContext,
                    appStore,
                    shareSubject,
                    shareData,
                    sendTabUseCases,
                    saveToPdfUseCase,
                    printUseCase,
                    sentFromFirefoxManager,
                    navController,
                    recentAppStorage,
                    this,
                    this,
                    FenixFxAEntryPoint.ShareMenu,
                    dismiss,
                )

            val destinations = listOf("a", "b")
            val expectedTabsShared = with(controller) { shareData.toTabData() }

            controller.showFailureWithRetryOption(destinations)

            verify {
                appStore.dispatch(
                    ShareAction.ShareTabsFailed(
                        destinations,
                        expectedTabsShared,
                    )
                )
            }
        }

    @Test
    fun `getShareText should respect concatenate shared tabs urls`() =
        runTest(testDispatcher) {
            val controller =
                DefaultShareController(
                    testContext,
                    appStore,
                    shareSubject,
                    shareData,
                    sendTabUseCases,
                    saveToPdfUseCase,
                    printUseCase,
                    sentFromFirefoxManager,
                    navController,
                    recentAppStorage,
                    this,
                    this,
                    FenixFxAEntryPoint.ShareMenu,
                    dismiss,
                )

            assertEquals(textToShare, controller.getShareText())
        }

    @Test
    fun `getShareText attempts to use original URL for reader pages`() =
        runTest(testDispatcher) {
            val shareData =
                listOf(
                    ShareData(url = "moz-extension://eb8df45a-895b-4f3a-896a-c0c71ae4/page.html", private = false),
                    ShareData(
                        url = "moz-extension://eb8df45a-895b-4f3a-896a-c0c71ae5/page.html?url=url0",
                        private = false,
                    ),
                    ShareData(url = "url1", private = false),
                )
            val controller =
                DefaultShareController(
                    context = testContext,
                    appStore = appStore,
                    shareSubject = shareSubject,
                    shareData = shareData,
                    sendTabUseCases = sendTabUseCases,
                    saveToPdfUseCase = mockk(),
                    printUseCase = mockk(),
                    sentFromFirefoxManager = sentFromFirefoxManager,
                    navController = navController,
                    recentAppsStorage = recentAppStorage,
                    scope = this,
                    applicationScope = this,
                    dismiss = dismiss,
                )

            val expectedShareText = "${shareData[0].url}\n\nurl0\n\n${shareData[2].url}"
            assertEquals(expectedShareText, controller.getShareText())
        }

    @Test
    fun `getShareSubject will return 'shareSubject' if that is non null`() =
        runTest(testDispatcher) {
            val controller =
                DefaultShareController(
                    testContext,
                    appStore,
                    shareSubject,
                    shareData,
                    sendTabUseCases,
                    saveToPdfUseCase,
                    printUseCase,
                    sentFromFirefoxManager,
                    navController,
                    recentAppStorage,
                    this,
                    this,
                    FenixFxAEntryPoint.ShareMenu,
                    dismiss,
                )

            assertEquals(shareSubject, controller.getShareSubject())
        }

    @Test
    fun `getShareSubject will return a concatenation of tab titles if 'shareSubject' is null`() =
        runTest(testDispatcher) {
            val controller =
                DefaultShareController(
                    context = testContext,
                    appStore = appStore,
                    shareSubject = null,
                    shareData = shareData,
                    sendTabUseCases = sendTabUseCases,
                    saveToPdfUseCase = mockk(),
                    printUseCase = mockk(),
                    sentFromFirefoxManager = sentFromFirefoxManager,
                    navController = navController,
                    recentAppsStorage = recentAppStorage,
                    scope = this,
                    applicationScope = this,
                    dismiss = dismiss,
                )

            assertEquals("title0, title1", controller.getShareSubject())
        }

    @Test
    fun `ShareTab#toTabData maps a list of ShareTab to a TabData list`() =
        runTest(testDispatcher) {
            val tabData: List<TabData> = shareData.toTabData()

            assertEquals(tabsData, tabData)
        }

    @Test
    fun `ShareTab#toTabData creates a data url from text if no url is specified`() {
        val expected =
            listOf(
                TabData(title = "title0", url = "", privacy = TabPrivacy.Normal),
                TabData(title = "title1", url = "data:,Hello%2C%20World!", privacy = TabPrivacy.Normal),
            )

        val tabData: List<TabData> =
            listOf(
                    ShareData(title = "title0", private = false),
                    ShareData(title = "title1", text = "Hello, World!", private = false),
                )
                .toTabData()

        assertEquals(expected, tabData)
    }

    @Test
    fun `ShareTab#toTabData respects private browsing mode`() {
        val privateShareData =
            listOf(
                ShareData(url = "url0", title = "title0", private = true),
                ShareData(url = "url1", title = "title1", private = true),
            )
        val expected =
            listOf(
                TabData(title = "title0", url = "url0", privacy = TabPrivacy.Private),
                TabData(title = "title1", url = "url1", privacy = TabPrivacy.Private),
            )

        val tabData: List<TabData> = privateShareData.toTabData()
        assertEquals(expected, tabData)
    }

    // SAM routed through mockk so verifyOrder can still span dismiss calls and other mocked
    // interactions. A user-defined interface keeps ByteBuddy out of the kotlin.jvm.functions
    // TypeCache bucket that Robolectric's shared classloader can race on.
    private fun interface DismissSink {
        fun onDismiss(result: ShareController.Result)
    }
}
