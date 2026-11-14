/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.widget

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AlertDialog
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.assertNotNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.GleanMetrics.GoogleLens
import org.mozilla.fenix.GleanMetrics.SearchWidget
import org.mozilla.fenix.HomeActivity.Companion.LENS_RESULT_URL
import org.mozilla.fenix.HomeActivity.Companion.OPEN_TO_BROWSER_AND_LOAD
import org.mozilla.fenix.IntentReceiverActivity
import org.mozilla.fenix.components.LensImageUploader
import org.mozilla.fenix.components.lens.LensCameraActivity
import org.mozilla.fenix.helpers.FenixGleanTestRule
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowActivity
import org.robolectric.shadows.ShadowDialog

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LensSearchActivityTest {

    @get:Rule val gleanTestRule = FenixGleanTestRule(testContext)

    private lateinit var controller: ActivityController<LensSearchActivity>
    private lateinit var activity: LensSearchActivity
    private lateinit var shadow: ShadowActivity

    private val uploader: LensImageUploader = mockk()

    @Before
    fun setup() {
        controller = Robolectric.buildActivity(LensSearchActivity::class.java, Intent())
        activity = controller.get()
        shadow = shadowOf(activity)
    }

    @Test
    fun `WHEN created THEN it launches the lens camera and records telemetry`() {
        controller.create()

        val cameraIntent = shadow.peekNextStartedActivityForResult()
        assertEquals(
            ComponentName(activity, LensCameraActivity::class.java),
            cameraIntent.intent.component,
        )
        assertNotNull(SearchWidget.lensButton.testGetValue())
    }

    @Test
    fun `WHEN created THEN the lens camera is opened in Google Lens mode`() {
        controller.create()

        val cameraIntent = shadow.peekNextStartedActivityForResult()
        assertEquals("LENS", cameraIntent.intent.getStringExtra(LensCameraActivity.EXTRA_INITIAL_MODE))
    }

    @Test
    fun `GIVEN saved instance state WHEN created THEN the camera is not relaunched`() {
        controller.create(Bundle())

        assertNull(shadow.peekNextStartedActivityForResult())
    }

    @Test
    fun `GIVEN a successful image upload WHEN handling the camera result THEN it forwards the result to the browser and finishes`() {
        controller.create()
        activity.uploadDispatcher = UnconfinedTestDispatcher()
        activity.uploader = uploader
        coEvery { uploader.upload(any(), any()) } returns
            LensImageUploader.UploadResult(resultUrl = "https://lens.google.com/results", httpStatusCode = 200)

        val resultIntent = Intent().apply { data = Uri.parse("content://test/image.jpg") }
        activity.handleCameraResult(ActivityResult(RESULT_OK, resultIntent))

        val browserIntent = shadow.peekNextStartedActivity()
        assertEquals(
            ComponentName(activity, IntentReceiverActivity::class.java),
            browserIntent.component,
        )
        assertTrue(browserIntent.getBooleanExtra(OPEN_TO_BROWSER_AND_LOAD, false))
        assertEquals("https://lens.google.com/results", browserIntent.getStringExtra(LENS_RESULT_URL))
        assertTrue(activity.isFinishing)

        val events = GoogleLens.searchCompleted.testGetValue()
        assertNotNull(events)
        assertEquals("true", events.last().extra?.get("succeeded"))
        assertEquals("200", events.last().extra?.get("http_status_code"))
    }

    @Test
    fun `GIVEN an image the camera screen already uploaded WHEN handling the camera result THEN it forwards the result without uploading`() {
        controller.create()
        activity.uploader = uploader

        val resultIntent =
            Intent().apply {
                putExtra(LensCameraActivity.EXTRA_LENS_RESULT_URL, "https://lens.google.com/results")
                putExtra(LensCameraActivity.EXTRA_IS_PRIVATE, false)
            }
        activity.handleCameraResult(ActivityResult(RESULT_OK, resultIntent))

        val browserIntent = shadow.peekNextStartedActivity()
        assertEquals(
            ComponentName(activity, IntentReceiverActivity::class.java),
            browserIntent.component,
        )
        assertEquals("https://lens.google.com/results", browserIntent.getStringExtra(LENS_RESULT_URL))
        assertTrue(activity.isFinishing)
        coVerify(exactly = 0) { uploader.upload(any(), any()) }
    }

    @Test
    fun `GIVEN the upload fails WHEN handling the camera result THEN it finishes without forwarding`() {
        controller.create()
        activity.uploadDispatcher = UnconfinedTestDispatcher()
        activity.uploader = uploader
        coEvery { uploader.upload(any(), any()) } returns LensImageUploader.UploadResult(resultUrl = null)
        shadow.clearNextStartedActivities()

        val resultIntent = Intent().apply { data = Uri.parse("content://test/image.jpg") }
        activity.handleCameraResult(ActivityResult(RESULT_OK, resultIntent))

        assertNull(shadow.peekNextStartedActivity())
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `GIVEN a cancelled camera result WHEN handling it THEN it finishes without forwarding`() {
        controller.create()
        shadow.clearNextStartedActivities()

        activity.handleCameraResult(ActivityResult(RESULT_CANCELED, Intent()))

        assertNull(shadow.peekNextStartedActivity())
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `GIVEN a result with no image uri WHEN handling it THEN it finishes without forwarding`() {
        controller.create()
        shadow.clearNextStartedActivities()

        activity.handleCameraResult(ActivityResult(RESULT_OK, Intent()))

        assertNull(shadow.peekNextStartedActivity())
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `GIVEN a QR scan result WHEN the user allows the confirmation dialog THEN it forwards the scanned value to the browser`() {
        controller.create()
        shadow.clearNextStartedActivities()

        val resultIntent =
            Intent().apply {
                putExtra(LensCameraActivity.EXTRA_SCAN_RESULT_DATA, "https://qr.example.com")
            }
        activity.handleCameraResult(ActivityResult(RESULT_OK, resultIntent))

        assertNull(shadow.peekNextStartedActivity())
        val dialog = ShadowDialog.getLatestDialog() as AlertDialog
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()

        val browserIntent = shadow.peekNextStartedActivity()
        assertEquals(
            ComponentName(activity, IntentReceiverActivity::class.java),
            browserIntent.component,
        )
        assertTrue(browserIntent.getBooleanExtra(OPEN_TO_BROWSER_AND_LOAD, false))
        assertEquals("https://qr.example.com", browserIntent.getStringExtra(LENS_RESULT_URL))
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `GIVEN a QR scan result WHEN the user denies the confirmation dialog THEN it finishes without forwarding`() {
        controller.create()
        shadow.clearNextStartedActivities()

        val resultIntent =
            Intent().apply {
                putExtra(LensCameraActivity.EXTRA_SCAN_RESULT_DATA, "https://qr.example.com")
            }
        activity.handleCameraResult(ActivityResult(RESULT_OK, resultIntent))

        val dialog = ShadowDialog.getLatestDialog() as AlertDialog
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()

        assertNull(shadow.peekNextStartedActivity())
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `GIVEN a non-http QR scan result WHEN handling it THEN it shows the invalid url dialog and finishes without forwarding`() {
        controller.create()
        shadow.clearNextStartedActivities()

        val resultIntent =
            Intent().apply {
                putExtra(LensCameraActivity.EXTRA_SCAN_RESULT_DATA, "javascript:alert(document.cookie)")
            }
        activity.handleCameraResult(ActivityResult(RESULT_OK, resultIntent))

        val dialog = ShadowDialog.getLatestDialog() as AlertDialog
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()

        assertNull(shadow.peekNextStartedActivity())
        assertTrue(activity.isFinishing)
    }
}
