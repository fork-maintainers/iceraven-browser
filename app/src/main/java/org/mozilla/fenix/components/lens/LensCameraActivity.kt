/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.lens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.os.BundleCompat
import androidx.core.view.OneShotPreDrawListener
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.feature.qr.QrAnalyzer
import mozilla.components.feature.qr.QrScanActivity
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import java.io.IOException

internal const val LENS_IMAGES_DIR = "lens_images"

/**
 * Activity that hosts [LensCameraFragment] for capturing images for Google Lens.
 * Handles camera permission and gallery picking, returning the selected image URI
 * as the activity result.
 */
class LensCameraActivity : AppCompatActivity() {

    private val logger = Logger("LensCameraActivity")

    private var optOutSheetPreDrawListener: OneShotPreDrawListener? = null

    // The camera permission is requested at most once per activity instance: the result is
    // delivered before onResume, so without this onResume would re-request it after a denial.
    @VisibleForTesting
    internal var permissionRequested = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted -> handlePermissionResult(isGranted) }

    @VisibleForTesting
    internal fun handlePermissionResult(isGranted: Boolean) {
        if (isGranted) {
            launchCameraFragment()
        } else {
            Toast.makeText(this, R.string.lens_camera_permission_denied, Toast.LENGTH_SHORT).show()
            cancelAndFinish()
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> handleGalleryResult(uri) }

    @VisibleForTesting
    internal fun handleGalleryResult(uri: Uri?) {
        if (uri != null) {
            val resultIntent = Intent().apply {
                data = uri
                putExtra(EXTRA_IMAGE_SOURCE, IMAGE_SOURCE_PHOTO_PICKER)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    private val qrGalleryLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            decodeQrFromUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lens_camera)
        permissionRequested = savedInstanceState?.getBoolean(STATE_PERMISSION_REQUESTED, false) == true
        if (savedInstanceState == null) {
            lifecycleScope.launch(Dispatchers.IO) { clearLensImageCache() }
        }

        supportFragmentManager.setFragmentResultListener(
            GoogleLensOptOutBottomSheetFragment.RESULT_REQUEST_KEY,
            this,
        ) { _, bundle ->
            handleOptOutResult(bundle.getString(GoogleLensOptOutBottomSheetFragment.RESULT_ACTION))
        }

        supportFragmentManager.setFragmentResultListener(
            LensCameraFragment.RESULT_REQUEST_KEY,
            this,
        ) { _, bundle ->
            // RESULT_QR_STRING must never be set to an empty string by the producer
            // (LensCameraFragment.handleQrResult). An empty value falls through to the
            // gallery/image branch below; downstream callers treat an empty extra as no scan.
            val qrString = bundle.getString(LensCameraFragment.RESULT_QR_STRING)
            if (!qrString.isNullOrEmpty()) {
                val resultIntent = Intent().apply {
                    putExtra(QrScanActivity.EXTRA_SCAN_RESULT_DATA, qrString)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
                return@setFragmentResultListener
            }

            if (bundle.getBoolean(LensCameraFragment.RESULT_QR_GALLERY_REQUEST, false)) {
                launchQrGalleryPicker()
                return@setFragmentResultListener
            }

            if (bundle.getBoolean(LensCameraFragment.RESULT_GALLERY_REQUEST, false)) {
                launchGalleryPicker()
                return@setFragmentResultListener
            }

            val imageUri: Uri? = BundleCompat.getParcelable(
                bundle,
                LensCameraFragment.RESULT_IMAGE_URI,
                Uri::class.java,
            )
            if (imageUri != null) {
                val resultIntent = Intent().apply {
                    data = imageUri
                    putExtra(EXTRA_IMAGE_SOURCE, IMAGE_SOURCE_CAMERA)
                }
                setResult(RESULT_OK, resultIntent)
            } else {
                setResult(RESULT_CANCELED)
            }
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_PERMISSION_REQUESTED, permissionRequested)
    }

    override fun onResume() {
        super.onResume()
        // handlePermissionResult may already have finished us: activity results are delivered
        // before onResume, and finishing does not stop the lifecycle from reaching it.
        if (isFinishing) return

        // The opt-out sheet gates everything else: while it is up neither the camera permission nor
        // LensCameraFragment - and therefore the camera itself - is touched.
        if (components.settings.hasAcceptedGoogleLensFirstRun) {
            if (!permissionRequested) {
                checkCameraPermission()
            }
        } else {
            showOptOutBackdrop()
            requestOptOutBottomSheet()
        }
    }

    override fun onStop() {
        super.onStop()
        // OneShotPreDrawListener only unregisters itself once it runs or once the view is detached,
        // and the decor view stays attached across a stop. Removing it here keeps a stop/start
        // before the first frame from leaving a stale listener that adds a second sheet.
        optOutSheetPreDrawListener?.removeListener()
        optOutSheetPreDrawListener = null
    }

    /**
     * Shows the camera-less backdrop the opt-out sheet sits on. Composed on demand so the common
     * post-acknowledgement path, which goes straight to the camera, never pays for it.
     */
    private fun showOptOutBackdrop() {
        val backdrop = findViewById<ComposeView>(R.id.lens_opt_out_backdrop)
        if (backdrop.isVisible) return
        // Not wrapped in FirefoxTheme, matching how LensCameraFragment hosts LensCameraScreen: the
        // camera surface is always dark and uses fixed colors.
        backdrop.setContent {
            LensOptOutBackdrop(onClose = ::cancelAndFinish)
        }
        backdrop.isVisible = true
    }

    /**
     * Hides the backdrop once the camera takes over. It is only covered by the fragment container,
     * so leaving it in place would keep its close button reachable by accessibility services.
     */
    private fun hideOptOutBackdrop() {
        val backdrop = findViewById<ComposeView>(R.id.lens_opt_out_backdrop)
        if (!backdrop.isVisible) return
        backdrop.isVisible = false
        backdrop.disposeComposition()
    }

    /**
     * Schedules the opt-out sheet for after the activity's first frame. Showing it any earlier adds
     * the dialog window while this window still has non-final metrics, which lays the sheet out at
     * the top of the screen for a frame before its behavior offsets it into place.
     */
    @VisibleForTesting
    internal fun requestOptOutBottomSheet() {
        if (optOutSheetPreDrawListener != null ||
            supportFragmentManager.findFragmentByTag(GoogleLensOptOutBottomSheetFragment.TAG) != null
        ) {
            return
        }
        optOutSheetPreDrawListener = OneShotPreDrawListener.add(window.decorView) {
            // Posted so the sheet is added after the first frame is drawn, not merely after layout.
            // The listener handle is cleared by onStop rather than here, so a request stays "in
            // flight" for the rest of this start and cannot be issued twice.
            window.decorView.post {
                if (!isFinishing && !isDestroyed &&
                    lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                ) {
                    showOptOutBottomSheet()
                }
            }
        }
    }

    @VisibleForTesting
    internal fun showOptOutBottomSheet() {
        // DialogFragment.show commits asynchronously, so an already-pending add would be invisible
        // to findFragmentByTag and a second caller would queue a duplicate sheet behind it.
        supportFragmentManager.executePendingTransactions()
        if (supportFragmentManager.findFragmentByTag(GoogleLensOptOutBottomSheetFragment.TAG) != null) {
            return
        }
        GoogleLensOptOutBottomSheetFragment().show(
            supportFragmentManager,
            GoogleLensOptOutBottomSheetFragment.TAG,
        )
    }

    @VisibleForTesting
    internal fun handleOptOutResult(action: String?) {
        when (action) {
            GoogleLensOptOutBottomSheetFragment.ACTION_TRY_IT_NOW -> {
                // Recorded even if the permission is subsequently denied: the user did opt in.
                components.settings.hasAcceptedGoogleLensFirstRun = true
                checkCameraPermission()
            }
            GoogleLensOptOutBottomSheetFragment.ACTION_SETTINGS -> {
                openSearchSettings()
                cancelAndFinish()
            }
            else -> cancelAndFinish()
        }
    }

    @VisibleForTesting
    internal fun openSearchSettings() {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "${BuildConfig.DEEP_LINK_SCHEME}://settings_search_engine".toUri(),
            ).apply {
                setPackage(packageName)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
        )
    }

    @VisibleForTesting
    internal fun cancelAndFinish() {
        setResult(RESULT_CANCELED)
        finish()
    }

    @VisibleForTesting
    internal fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            launchCameraFragment()
        } else {
            permissionRequested = true
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCameraFragment() {
        hideOptOutBackdrop()
        if (supportFragmentManager.findFragmentById(R.id.lens_fragment_container_view) != null) {
            return
        }
        supportFragmentManager.commit {
            add(R.id.lens_fragment_container_view, LensCameraFragment::class.java, null)
        }
    }

    private fun launchGalleryPicker() {
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    private fun launchQrGalleryPicker() {
        qrGalleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    @VisibleForTesting
    internal fun decodeQrFromUri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val qrString = try {
                val bitmap = loadSoftwareBitmap(uri)
                QrAnalyzer().analyze(bitmap)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                logger.error("Failed to decode QR from picked image", e)
                null
            } catch (e: IllegalArgumentException) {
                logger.error("Failed to decode QR from picked image", e)
                null
            } catch (e: IllegalStateException) {
                logger.error("Failed to decode QR from picked image", e)
                null
            }
            withContext(Dispatchers.Main) {
                if (!isFinishing && !isDestroyed) handleQrDecodeResult(qrString)
            }
        }
    }

    @VisibleForTesting
    internal fun handleQrDecodeResult(qrString: String?) {
        if (qrString.isNullOrEmpty()) {
            Toast.makeText(this, R.string.lens_camera_qr_no_code_found, Toast.LENGTH_SHORT).show()
            return
        }
        val resultIntent = Intent().apply {
            putExtra(QrScanActivity.EXTRA_SCAN_RESULT_DATA, qrString)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun loadSoftwareBitmap(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                // QrAnalyzer.analyze(bitmap) calls Bitmap.getPixels, which throws on
                // hardware-backed bitmaps — force the software allocator.
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longEdge = maxOf(info.size.width, info.size.height)
                if (longEdge > QR_DECODE_MAX_DIMENSION) {
                    val scale = QR_DECODE_MAX_DIMENSION.toFloat() / longEdge
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt().coerceAtLeast(1),
                        (info.size.height * scale).toInt().coerceAtLeast(1),
                    )
                }
            }
        } else {
            decodeDownsampledStream(uri)
        }
    }

    private fun decodeDownsampledStream(uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        } ?: throw IOException("Unable to open $uri")

        val longEdge = maxOf(bounds.outWidth, bounds.outHeight)
        var sampleSize = 1
        while (longEdge / sampleSize > QR_DECODE_MAX_DIMENSION) sampleSize *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: throw IOException("Unable to open $uri")
    }

    @VisibleForTesting
    internal fun clearLensImageCache() {
        val imageDir = java.io.File(cacheDir, LENS_IMAGES_DIR)
        if (imageDir.exists()) {
            imageDir.listFiles()?.forEach { it.delete() }
        }
    }

    companion object {
        // Cap the long edge of decoded gallery images before QR analysis. Modern phone photos
        // are 12 MP+ which would allocate ~50 MB as ARGB_8888 plus another ~50 MB for the
        // IntArray pixel copy inside QrAnalyzer — enough to OOM low-RAM devices. ZXing
        // detects QR codes reliably well below this resolution.
        private const val QR_DECODE_MAX_DIMENSION = 2048

        private const val STATE_PERMISSION_REQUESTED = "permission_requested"

        /**
         * Result intent extra naming the upload method that produced the image, read by
         * `LensFeature` to attribute the Google Lens search telemetry.
         */
        internal const val EXTRA_IMAGE_SOURCE = "lens_image_source"

        @VisibleForTesting internal const val IMAGE_SOURCE_CAMERA = "camera"

        @VisibleForTesting internal const val IMAGE_SOURCE_PHOTO_PICKER = "photo_picker"

        /**
         * Creates an intent to launch [LensCameraActivity].
         */
        fun newIntent(context: Context): Intent {
            return Intent(context, LensCameraActivity::class.java)
        }
    }
}
