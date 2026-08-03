/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.lens

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.isVisible
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.mockk.every
import io.mockk.verify
import mozilla.components.feature.qr.QrScanActivity
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.R
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.utils.Settings
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowToast
import java.io.File
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class LensCameraActivityTest {

    private lateinit var settings: Settings

    @Before
    fun setUp() {
        settings = testContext.components.settings
        // Most tests exercise the camera flow, which is only reached once the opt-out sheet has
        // been acknowledged; the opt-out tests below override this.
        every { settings.hasAcceptedGoogleLensFirstRun } returns true
        // FirefoxTheme, used by the opt-out sheet, resolves the theme through FenixApplication,
        // which reads the AppStore's browsing mode.
        every { testContext.components.appStore } returns AppStore()
    }

    @Test
    fun `GIVEN a context WHEN newIntent is called THEN the returned intent targets LensCameraActivity`() {
        val intent = LensCameraActivity.newIntent(testContext)
        assertEquals(
            LensCameraActivity::class.java.name,
            intent.component?.className,
        )
    }

    @Test
    fun `GIVEN cached lens images exist WHEN clearLensImageCache is called THEN cached files are deleted and the directory remains`() {
        val activity = Robolectric.buildActivity(LensCameraActivity::class.java).get()
        val imageDir = File(activity.cacheDir, "lens_images")
        imageDir.mkdirs()
        val testFile = File(imageDir, "test_image.jpg")
        testFile.writeText("test")
        assertTrue(testFile.exists())

        activity.clearLensImageCache()

        assertFalse(testFile.exists())
        assertTrue(imageDir.exists())
    }

    @Test
    fun `GIVEN the lens image cache directory does not exist WHEN clearLensImageCache is called THEN no exception is thrown`() {
        val activity = Robolectric.buildActivity(LensCameraActivity::class.java).get()
        val imageDir = File(activity.cacheDir, "lens_images")
        assertFalse(imageDir.exists())

        activity.clearLensImageCache()
    }

    @Test
    fun `WHEN handlePermissionResult is called with false THEN a permission-denied Toast is shown, RESULT_CANCELED is set, and the activity finishes`() {
        val activity = Robolectric.buildActivity(LensCameraActivity::class.java).create().get()

        activity.handlePermissionResult(isGranted = false)

        assertEquals("Permission denied", ShadowToast.getTextOfLatestToast())
        assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity).resultCode)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `WHEN handlePermissionResult is called with true THEN a LensCameraFragment is added to the container`() {
        val activity = Robolectric.buildActivity(LensCameraActivity::class.java).create().get()

        activity.handlePermissionResult(isGranted = true)
        activity.supportFragmentManager.executePendingTransactions()

        val fragment = activity.supportFragmentManager
            .findFragmentById(R.id.lens_fragment_container_view)
        assertNotNull(fragment)
        assertIs<LensCameraFragment>(fragment)
    }

    @Test
    fun `GIVEN a LensCameraFragment is already in the container WHEN handlePermissionResult is called with true THEN no duplicate fragment is added`() {
        val activity = Robolectric.buildActivity(LensCameraActivity::class.java).create().get()
        activity.handlePermissionResult(isGranted = true)
        activity.supportFragmentManager.executePendingTransactions()
        val firstFragment = activity.supportFragmentManager
            .findFragmentById(R.id.lens_fragment_container_view)

        activity.handlePermissionResult(isGranted = true)
        activity.supportFragmentManager.executePendingTransactions()

        val fragments = activity.supportFragmentManager.fragments
        assertEquals(1, fragments.size)
        assertSame(firstFragment, fragments[0])
    }

    @Test
    fun `GIVEN camera permission is granted WHEN the activity reaches onResume THEN a LensCameraFragment is added`() {
        val shadowApp = Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>())
        shadowApp.grantPermissions(Manifest.permission.CAMERA)

        val controller = Robolectric.buildActivity(LensCameraActivity::class.java).setup()
        val activity = controller.get()
        activity.supportFragmentManager.executePendingTransactions()

        val fragment = activity.supportFragmentManager
            .findFragmentById(R.id.lens_fragment_container_view)
        assertNotNull(fragment)
        assertIs<LensCameraFragment>(fragment)
    }

    @Test
    fun `WHEN the fragment result bundle carries RESULT_QR_STRING THEN the activity returns RESULT_OK with EXTRA_SCAN_RESULT_DATA and finishes`() {
        val controller = Robolectric.buildActivity(LensCameraActivity::class.java).setup()
        val activity = controller.get()
        val qrString = "https://example.com"

        activity.supportFragmentManager.setFragmentResult(
            LensCameraFragment.RESULT_REQUEST_KEY,
            Bundle().apply { putString(LensCameraFragment.RESULT_QR_STRING, qrString) },
        )
        activity.supportFragmentManager.executePendingTransactions()

        val shadow = Shadows.shadowOf(activity)
        assertEquals(Activity.RESULT_OK, shadow.resultCode)
        assertEquals(qrString, shadow.resultIntent.getStringExtra(QrScanActivity.EXTRA_SCAN_RESULT_DATA))
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `GIVEN both QR string and image URI are present WHEN the fragment result fires THEN the QR string takes priority`() {
        val controller = Robolectric.buildActivity(LensCameraActivity::class.java).setup()
        val activity = controller.get()
        val qrString = "https://example.com"

        activity.supportFragmentManager.setFragmentResult(
            LensCameraFragment.RESULT_REQUEST_KEY,
            Bundle().apply {
                putString(LensCameraFragment.RESULT_QR_STRING, qrString)
                putBoolean(LensCameraFragment.RESULT_GALLERY_REQUEST, true)
            },
        )
        activity.supportFragmentManager.executePendingTransactions()

        val shadow = Shadows.shadowOf(activity)
        assertEquals(Activity.RESULT_OK, shadow.resultCode)
        assertEquals(qrString, shadow.resultIntent.getStringExtra(QrScanActivity.EXTRA_SCAN_RESULT_DATA))
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `GIVEN a decoded QR string WHEN handleQrDecodeResult is called THEN the activity finishes with EXTRA_SCAN_RESULT_DATA`() {
        val controller = Robolectric.buildActivity(LensCameraActivity::class.java).setup()
        val activity = controller.get()
        val qrString = "https://example.com/from-gallery"

        activity.handleQrDecodeResult(qrString)

        val shadow = Shadows.shadowOf(activity)
        assertEquals(Activity.RESULT_OK, shadow.resultCode)
        assertEquals(qrString, shadow.resultIntent.getStringExtra(QrScanActivity.EXTRA_SCAN_RESULT_DATA))
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `GIVEN no QR decoded from the picked image WHEN handleQrDecodeResult is called with null THEN a toast is shown and the activity stays open`() {
        val controller = Robolectric.buildActivity(LensCameraActivity::class.java).setup()
        val activity = controller.get()

        activity.handleQrDecodeResult(null)

        assertEquals(
            activity.getString(R.string.lens_camera_qr_no_code_found),
            ShadowToast.getTextOfLatestToast(),
        )
        assertFalse(activity.isFinishing)
    }

    @Test
    fun `GIVEN an empty QR result WHEN handleQrDecodeResult is called THEN a toast is shown and the activity stays open`() {
        val controller = Robolectric.buildActivity(LensCameraActivity::class.java).setup()
        val activity = controller.get()

        activity.handleQrDecodeResult("")

        assertEquals(
            activity.getString(R.string.lens_camera_qr_no_code_found),
            ShadowToast.getTextOfLatestToast(),
        )
        assertFalse(activity.isFinishing)
    }

    @Test
    fun `WHEN the fragment result bundle has an empty QR string THEN the QR branch is ignored`() {
        val controller = Robolectric.buildActivity(LensCameraActivity::class.java).setup()
        val activity = controller.get()

        activity.supportFragmentManager.setFragmentResult(
            LensCameraFragment.RESULT_REQUEST_KEY,
            Bundle().apply { putString(LensCameraFragment.RESULT_QR_STRING, "") },
        )
        activity.supportFragmentManager.executePendingTransactions()

        // No QR result means we fall through; empty bundle (no image URI, no gallery flag)
        // sets RESULT_CANCELED and finishes.
        val shadow = Shadows.shadowOf(activity)
        assertEquals(Activity.RESULT_CANCELED, shadow.resultCode)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `WHEN the fragment result carries a captured image URI THEN the activity returns RESULT_OK tagged as the camera source`() {
        val controller = Robolectric.buildActivity(LensCameraActivity::class.java).setup()
        val activity = controller.get()
        val imageUri = Uri.parse("content://test/lens_capture.jpg")

        activity.supportFragmentManager.setFragmentResult(
            LensCameraFragment.RESULT_REQUEST_KEY,
            Bundle().apply { putParcelable(LensCameraFragment.RESULT_IMAGE_URI, imageUri) },
        )
        activity.supportFragmentManager.executePendingTransactions()

        val shadow = Shadows.shadowOf(activity)
        assertEquals(Activity.RESULT_OK, shadow.resultCode)
        assertEquals(imageUri, shadow.resultIntent.data)
        assertEquals(
            LensCameraActivity.IMAGE_SOURCE_CAMERA,
            shadow.resultIntent.getStringExtra(LensCameraActivity.EXTRA_IMAGE_SOURCE),
        )
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `WHEN a photo picker image is returned THEN the activity returns RESULT_OK tagged as the photo_picker source`() {
        val controller = Robolectric.buildActivity(LensCameraActivity::class.java).setup()
        val activity = controller.get()
        val imageUri = Uri.parse("content://test/picked_image.jpg")

        activity.handleGalleryResult(imageUri)

        val shadow = Shadows.shadowOf(activity)
        assertEquals(Activity.RESULT_OK, shadow.resultCode)
        assertEquals(imageUri, shadow.resultIntent.data)
        assertEquals(
            LensCameraActivity.IMAGE_SOURCE_PHOTO_PICKER,
            shadow.resultIntent.getStringExtra(LensCameraActivity.EXTRA_IMAGE_SOURCE),
        )
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `GIVEN the opt-out sheet has not been acknowledged WHEN the activity reaches onResume THEN the sheet is shown and no camera fragment is added`() {
        every { settings.hasAcceptedGoogleLensFirstRun } returns false
        val shadowApp = Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>())
        shadowApp.grantPermissions(Manifest.permission.CAMERA)

        val activity = Robolectric.buildActivity(LensCameraActivity::class.java).setup().get()
        activity.supportFragmentManager.executePendingTransactions()

        assertNotNull(
            activity.supportFragmentManager
                .findFragmentByTag(GoogleLensOptOutBottomSheetFragment.TAG),
        )
        assertNull(
            activity.supportFragmentManager
                .findFragmentById(R.id.lens_fragment_container_view),
        )
    }

    @Test
    fun `GIVEN the opt-out sheet has not been acknowledged WHEN requestOptOutBottomSheet is called THEN the sheet is not added before the first frame is drawn`() {
        every { settings.hasAcceptedGoogleLensFirstRun } returns false
        val activity = Robolectric.buildActivity(LensCameraActivity::class.java).create().get()

        activity.requestOptOutBottomSheet()
        activity.supportFragmentManager.executePendingTransactions()

        assertNull(
            activity.supportFragmentManager
                .findFragmentByTag(GoogleLensOptOutBottomSheetFragment.TAG),
        )
    }

    @Test
    fun `GIVEN the opt-out sheet is already shown WHEN requestOptOutBottomSheet is called again THEN only one sheet is added`() {
        every { settings.hasAcceptedGoogleLensFirstRun } returns false
        val activity = Robolectric.buildActivity(LensCameraActivity::class.java).setup().get()

        activity.requestOptOutBottomSheet()
        activity.supportFragmentManager.executePendingTransactions()

        assertEquals(
            1,
            activity.supportFragmentManager.fragments
                .count { it is GoogleLensOptOutBottomSheetFragment },
        )
    }

    @Test
    fun `GIVEN the opt-out sheet is already shown WHEN showOptOutBottomSheet is called again THEN no duplicate sheet is added`() {
        every { settings.hasAcceptedGoogleLensFirstRun } returns false
        val activity = Robolectric.buildActivity(LensCameraActivity::class.java).setup().get()
        activity.supportFragmentManager.executePendingTransactions()
        val sheet = activity.supportFragmentManager
            .findFragmentByTag(GoogleLensOptOutBottomSheetFragment.TAG)

        activity.showOptOutBottomSheet()
        activity.supportFragmentManager.executePendingTransactions()

        assertSame(
            sheet,
            activity.supportFragmentManager
                .findFragmentByTag(GoogleLensOptOutBottomSheetFragment.TAG),
        )
    }

    @Test
    fun `GIVEN the opt-out sheet is shown WHEN its dialog is created THEN it is expanded and sized to its content`() {
        every { settings.hasAcceptedGoogleLensFirstRun } returns false
        val activity = Robolectric.buildActivity(LensCameraActivity::class.java).setup().get()
        activity.supportFragmentManager.executePendingTransactions()
        val sheet = activity.supportFragmentManager
            .findFragmentByTag(GoogleLensOptOutBottomSheetFragment.TAG)
        assertIs<GoogleLensOptOutBottomSheetFragment>(sheet)

        val behavior = assertIs<BottomSheetDialog>(sheet.dialog).behavior

        assertEquals(BottomSheetBehavior.STATE_EXPANDED, behavior.state)
        assertTrue(behavior.skipCollapsed)
        assertEquals(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            sheet.requireView().layoutParams.height,
        )
    }

    @Test
    fun `GIVEN the opt-out sheet is shown WHEN it is cancelled without a choice THEN the activity returns RESULT_CANCELED and finishes`() {
        every { settings.hasAcceptedGoogleLensFirstRun } returns false
        val activity = Robolectric.buildActivity(LensCameraActivity::class.java).setup().get()
        activity.supportFragmentManager.executePendingTransactions()
        val sheet = activity.supportFragmentManager
            .findFragmentByTag(GoogleLensOptOutBottomSheetFragment.TAG)
        assertIs<GoogleLensOptOutBottomSheetFragment>(sheet)

        // A swipe, back press or tap outside all route through the dialog's cancel, which posts
        // its listener to the main looper rather than invoking it inline.
        sheet.dialog?.cancel()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        activity.supportFragmentManager.executePendingTransactions()

        assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity).resultCode)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `GIVEN the opt-out sheet is shown WHEN the activity is recreated THEN the sheet survives and no choice is reported`() {
        every { settings.hasAcceptedGoogleLensFirstRun } returns false
        val controller = Robolectric.buildActivity(LensCameraActivity::class.java).setup()
        controller.get().supportFragmentManager.executePendingTransactions()

        val activity = controller.recreate().get()
        activity.supportFragmentManager.executePendingTransactions()

        // onDismiss also fires from onDestroyView on a configuration change; only onCancel means
        // the user actually declined, so a recreate must not cancel the activity.
        assertNotNull(
            activity.supportFragmentManager
                .findFragmentByTag(GoogleLensOptOutBottomSheetFragment.TAG),
        )
        assertFalse(activity.isFinishing)
    }

    @Test
    fun `GIVEN the camera permission was denied WHEN the activity resumes THEN the camera is not started behind the denial`() {
        // No grantPermissions here, so the resume inside setup requests the permission.
        val controller = Robolectric.buildActivity(LensCameraActivity::class.java).setup()
        val activity = controller.get()
        assertTrue(activity.permissionRequested)

        // Activity results are delivered before onResume, so the denial lands first and finishes us.
        activity.handlePermissionResult(isGranted = false)
        assertTrue(activity.isFinishing)

        // Were onResume to re-enter checkCameraPermission it would now find the permission granted
        // and start the camera the user just refused; a denial that stuck would re-prompt instead.
        Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .grantPermissions(Manifest.permission.CAMERA)
        controller.pause().resume()

        assertNull(
            activity.supportFragmentManager
                .findFragmentById(R.id.lens_fragment_container_view),
        )
        assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity).resultCode)
    }

    @Test
    fun `GIVEN the opt-out sheet is shown WHEN try it now leads to the camera THEN the backdrop is hidden`() {
        every { settings.hasAcceptedGoogleLensFirstRun } returns false
        val shadowApp = Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>())
        shadowApp.grantPermissions(Manifest.permission.CAMERA)
        val activity = Robolectric.buildActivity(LensCameraActivity::class.java).setup().get()
        val backdrop = activity.findViewById<ComposeView>(R.id.lens_opt_out_backdrop)
        assertTrue(backdrop.isVisible)

        activity.handleOptOutResult(GoogleLensOptOutBottomSheetFragment.ACTION_TRY_IT_NOW)
        activity.supportFragmentManager.executePendingTransactions()

        // Only covered by the fragment container, so it has to be gone rather than obscured or its
        // close button stays reachable by accessibility services.
        assertFalse(backdrop.isVisible)
    }

    @Test
    fun `GIVEN the opt-out sheet has been acknowledged WHEN the activity starts THEN the backdrop is never shown`() {
        val shadowApp = Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>())
        shadowApp.grantPermissions(Manifest.permission.CAMERA)

        val activity = Robolectric.buildActivity(LensCameraActivity::class.java).setup().get()

        assertFalse(activity.findViewById<ComposeView>(R.id.lens_opt_out_backdrop).isVisible)
    }

    @Test
    fun `WHEN the opt-out sheet reports try it now THEN the acknowledgement is recorded and the camera fragment is added`() {
        val shadowApp = Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>())
        shadowApp.grantPermissions(Manifest.permission.CAMERA)
        val activity = Robolectric.buildActivity(LensCameraActivity::class.java).create().get()

        activity.handleOptOutResult(GoogleLensOptOutBottomSheetFragment.ACTION_TRY_IT_NOW)
        activity.supportFragmentManager.executePendingTransactions()

        verify { settings.hasAcceptedGoogleLensFirstRun = true }
        assertIs<LensCameraFragment>(
            activity.supportFragmentManager
                .findFragmentById(R.id.lens_fragment_container_view),
        )
        assertFalse(activity.isFinishing)
    }

    @Test
    fun `WHEN the opt-out sheet reports not now THEN the activity returns RESULT_CANCELED and finishes`() {
        val activity = Robolectric.buildActivity(LensCameraActivity::class.java).create().get()

        activity.handleOptOutResult(GoogleLensOptOutBottomSheetFragment.ACTION_NOT_NOW)

        verify(exactly = 0) { settings.hasAcceptedGoogleLensFirstRun = true }
        assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity).resultCode)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `WHEN the opt-out sheet is dismissed without an action THEN the activity returns RESULT_CANCELED and finishes`() {
        val activity = Robolectric.buildActivity(LensCameraActivity::class.java).create().get()

        activity.handleOptOutResult(null)

        assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity).resultCode)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `WHEN the opt-out sheet reports the settings link THEN the Search settings deep link is started and the activity finishes`() {
        val activity = Robolectric.buildActivity(LensCameraActivity::class.java).create().get()

        activity.handleOptOutResult(GoogleLensOptOutBottomSheetFragment.ACTION_SETTINGS)

        val startedIntent = Shadows.shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, startedIntent.action)
        assertEquals(
            "${BuildConfig.DEEP_LINK_SCHEME}://settings_search_engine",
            startedIntent.data.toString(),
        )
        assertEquals(activity.packageName, startedIntent.`package`)
        verify(exactly = 0) { settings.hasAcceptedGoogleLensFirstRun = true }
        assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity).resultCode)
        assertTrue(activity.isFinishing)
    }
}
