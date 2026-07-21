/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.lens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Size
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import kotlinx.coroutines.flow.MutableStateFlow
import mozilla.components.feature.qr.QrAnalyzer
import mozilla.components.feature.qr.isLowLightBoostSupported
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.android.content.hasCamera
import mozilla.components.support.utils.ext.handleBackEvents
import java.io.File
import java.io.IOException
import java.util.Collections
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min

/**
 * A [Fragment] that displays a camera preview with shutter and gallery buttons
 * for capturing images for Google Lens.
 */
@Suppress("LargeClass", "TooManyFunctions")
class LensCameraFragment : Fragment() {
    private val logger = Logger("LensCameraFragment")

    @VisibleForTesting
    internal var textureView: AutoFitTextureView? = null

    @VisibleForTesting
    internal val showCameraError = mutableStateOf(false)

    @VisibleForTesting
    internal var cameraId: String? = null
    private var isLowLightBoostSupported: Boolean = false

    @VisibleForTesting
    internal var captureSession: CameraCaptureSession? = null

    @VisibleForTesting
    internal var cameraDevice: CameraDevice? = null

    @VisibleForTesting
    internal var previewSize: Size? = null

    @VisibleForTesting
    internal val previewAspectRatio = mutableStateOf<Float?>(null)
    private var sensorOrientation: Int = 0

    @VisibleForTesting
    internal var surface: Surface? = null

    @VisibleForTesting
    internal var backgroundThread: HandlerThread? = null

    @VisibleForTesting
    internal var backgroundHandler: Handler? = null

    @VisibleForTesting
    internal var backgroundExecutor: ExecutorService? = null

    @VisibleForTesting
    internal var imageReader: ImageReader? = null

    @VisibleForTesting
    internal var qrImageReader: ImageReader? = null

    @VisibleForTesting
    internal var qrAnalyzer = QrAnalyzer()

    // Source of truth for the current camera mode. MutableStateFlow rather than mutableStateOf
    // because qrImageAvailableListener reads .value on the camera background thread.
    @VisibleForTesting
    internal val cameraMode = MutableStateFlow(CameraMode.LENS)

    @VisibleForTesting
    internal val qrInFlight = AtomicBoolean(false)

    @VisibleForTesting
    internal var qrResultSent = false

    @VisibleForTesting
    internal var getUriForFile: (Context, String, File) -> Uri = { ctx, authority, file ->
        FileProvider.getUriForFile(ctx, authority, file)
    }
    private var isCapturing = false
    private val cameraOpenCloseLock = Semaphore(1)
    private val mainHandler = Handler(Looper.getMainLooper())

    @VisibleForTesting
    internal val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            tryOpenCamera(width, height)
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        @Suppress("EmptyFunctionBlock")
        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) { }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean = true
    }

    @VisibleForTesting
    internal val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice = camera
            createCameraPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
            handleResult(null)
        }
    }

    private val stillCaptureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: android.hardware.camera2.CaptureFailure,
        ) {
            logger.error("Still capture failed with reason: ${failure.reason}")
            mainHandler.post {
                isCapturing = false
                handleResult(null)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoreFromState(savedInstanceState)
    }

    @VisibleForTesting
    internal fun restoreFromState(savedInstanceState: Bundle?) {
        savedInstanceState?.let { state ->
            state.getString(STATE_CAMERA_MODE)?.let { name ->
                cameraMode.value = CameraMode.valueOf(name)
            }
            qrResultSent = state.getBoolean(STATE_QR_RESULT_SENT, false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_CAMERA_MODE, cameraMode.value.name)
        outState.putBoolean(STATE_QR_RESULT_SENT, qrResultSent)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val mode by cameraMode.collectAsState()
                LensCameraScreen(
                    state = LensCameraState(
                        showError = showCameraError.value,
                        mode = mode,
                        previewAspectRatio = previewAspectRatio.value,
                    ),
                    onModeChange = ::handleModeChanged,
                    onClose = { handleResult(null) },
                    onShutter = { captureStillImage() },
                    onGallery = { requestGalleryPick() },
                    textureViewProvider = { ctx ->
                        AutoFitTextureView(ctx).also { view ->
                            textureView = view
                            if (isResumed) {
                                startCamera()
                            }
                        }
                    },
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.isFocusableInTouchMode = true
        view.requestFocus()
        view.handleBackEvents { handleResult(null) }
    }

    override fun onResume() {
        super.onResume()
        qrAnalyzer.reset()
        // qrResultSent is intentionally not reset here. After a successful scan it stays true
        // through process death + restore, so a re-entry to the activity before it finishes
        // can't fire a duplicate result. handleModeChanged(QR) clears it on a fresh mode entry.
        startCamera()
    }

    override fun onPause() {
        stopServices()
        super.onPause()
    }

    @VisibleForTesting
    internal fun startCamera() {
        val view = textureView ?: return
        maybeStartBackgroundThread()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            maybeStartExecutorService()
        }
        if (view.isAvailable) {
            tryOpenCamera(view.width, view.height)
        } else {
            view.surfaceTextureListener = surfaceTextureListener
        }
    }

    @VisibleForTesting
    internal fun stopServices() {
        closeCamera()
        stopBackgroundThread()
        stopExecutorService()
    }

    @Suppress("TooGenericExceptionCaught")
    @VisibleForTesting
    internal fun tryOpenCamera(width: Int, height: Int) {
        try {
            if (deviceHasCamera()) {
                openCamera(width, height)
                showCameraError.value = false
            } else {
                showCameraError.value = true
            }
        } catch (e: Exception) {
            showCameraError.value = true
        }
    }

    @VisibleForTesting
    internal fun deviceHasCamera(): Boolean = context?.hasCamera() == true

    @SuppressLint("MissingPermission")
    @Suppress("ThrowsCount")
    @VisibleForTesting
    internal fun openCamera(width: Int, height: Int) {
        try {
            setUpCameraOutputs(width, height)
            val id = cameraId ?: throw IllegalStateException("No camera found on device")
            configureTransform(width, height)

            val manager = activity?.getSystemService(Context.CAMERA_SERVICE) as CameraManager?
            if (!cameraOpenCloseLock.tryAcquire(CAMERA_CLOSE_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                throw IllegalStateException("Time out waiting to lock camera opening.")
            }
            manager?.openCamera(id, stateCallback, backgroundHandler)
        } catch (e: InterruptedException) {
            throw IllegalStateException("Interrupted while trying to lock camera opening.", e)
        } catch (e: CameraAccessException) {
            logger.error("Failed to open camera", e)
        }
    }

    @VisibleForTesting
    internal fun setUpCameraOutputs(width: Int, height: Int) {
        val displayRotation = getScreenRotation() ?: Surface.ROTATION_0
        val manager = activity?.getSystemService(Context.CAMERA_SERVICE) as CameraManager? ?: return

        for (id in manager.cameraIdList) {
            val characteristics = manager.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_FRONT) continue

            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: continue

            val jpegSizes = map.getOutputSizes(ImageFormat.JPEG)
            val captureSize = chooseCaptureSizeFromList(jpegSizes)

            imageReader = ImageReader.newInstance(
                captureSize.width,
                captureSize.height,
                ImageFormat.JPEG,
                1,
            ).apply {
                setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
            }

            qrImageReader = ImageReader.newInstance(
                QrAnalyzer.YUV_WIDTH,
                QrAnalyzer.YUV_HEIGHT,
                ImageFormat.YUV_420_888,
                QrAnalyzer.YUV_MAX_IMAGES,
            ).apply {
                setOnImageAvailableListener(qrImageAvailableListener, backgroundHandler)
            }

            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) as Int

            val swappedDimensions = areDimensionsSwapped(displayRotation, sensorOrientation)

            val displaySize = activity?.windowManager?.let { getDisplaySize(it) } ?: Point()
            var rotatedPreviewWidth = width
            var rotatedPreviewHeight = height
            var maxPreviewWidth = displaySize.x
            var maxPreviewHeight = displaySize.y

            if (swappedDimensions) {
                rotatedPreviewWidth = height
                rotatedPreviewHeight = width
                maxPreviewWidth = displaySize.y
                maxPreviewHeight = displaySize.x
            }

            maxPreviewWidth = min(maxPreviewWidth, MAX_PREVIEW_WIDTH)
            maxPreviewHeight = min(maxPreviewHeight, MAX_PREVIEW_HEIGHT)

            val optimalSize = chooseOptimalSize(
                map.getOutputSizes(SurfaceTexture::class.java),
                rotatedPreviewWidth,
                rotatedPreviewHeight,
                maxPreviewWidth,
                maxPreviewHeight,
                captureSize,
            )

            previewSize = optimalSize
            val displayWidth = if (swappedDimensions) optimalSize.height else optimalSize.width
            val displayHeight = if (swappedDimensions) optimalSize.width else optimalSize.height
            textureView?.setAspectRatio(displayWidth, displayHeight)
            previewAspectRatio.value = displayWidth.toFloat() / displayHeight.toFloat()
            this.cameraId = id
            this.isLowLightBoostSupported = manager.isLowLightBoostSupported(id)
            return
        }
    }

    private fun areDimensionsSwapped(displayRotation: Int, sensorOrientation: Int): Boolean =
        when (displayRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 ->
                sensorOrientation == ORIENTATION_90 || sensorOrientation == ORIENTATION_270
            Surface.ROTATION_90, Surface.ROTATION_270 ->
                sensorOrientation == ORIENTATION_0 || sensorOrientation == ORIENTATION_180
            else -> false
        }

    @VisibleForTesting
    internal fun createCameraPreviewSession() {
        val texture = textureView?.surfaceTexture
        val size = previewSize ?: return
        texture?.setDefaultBufferSize(size.width, size.height)

        val previewSurface = Surface(texture).also { surface = it }
        val imageSurface = imageReader?.surface ?: return
        val qrSurface = qrImageReader?.surface ?: return

        handleCaptureException("Failed to create camera preview session") {
            cameraDevice?.let { camera ->
                val previewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                    addTarget(previewSurface)
                    // qrSurface is added as a target in both LENS and QR modes. In LENS mode
                    // qrImageAvailableListener drains frames without invoking the analyzer. The
                    // alternative — rebuilding the repeating request on each mode toggle — costs
                    // a brief preview stall and adds complexity, so we accept the steady-state
                    // YUV throughput in exchange for an instantaneous mode switch.
                    addTarget(qrSurface)
                }

                previewRequestBuilder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
                    isLowLightBoostSupported
                ) {
                    previewRequestBuilder.set(
                        CaptureRequest.CONTROL_AE_MODE,
                        CameraMetadata.CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY,
                    )
                }

                val previewRequest = previewRequestBuilder.build()
                val captureCallback = object : CameraCaptureSession.CaptureCallback() {}
                val sessionStateCallback = object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        onSessionConfigured(session, previewRequest, captureCallback)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        logger.error("Failed to configure CameraCaptureSession")
                    }
                }
                createCaptureSessionCompat(camera, imageSurface, qrSurface, previewSurface, sessionStateCallback)
            }
        }
    }

    @VisibleForTesting
    internal fun onSessionConfigured(
        session: CameraCaptureSession,
        previewRequest: CaptureRequest,
        captureCallback: CameraCaptureSession.CaptureCallback,
    ) {
        // closeCamera() runs on the main thread while this callback runs on the background
        // thread. Gate the check-and-assign on cameraOpenCloseLock (the lock closeCamera() also
        // holds) so the session is atomically either stored for a live camera or closed here,
        // and never assigned after a concurrent closeCamera() has finished tearing down - which
        // would leave the session unreferenced and never closed. The lock is released before
        // setRepeatingRequest so a camera call is never made while holding it.
        try {
            cameraOpenCloseLock.acquire()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            session.close()
            return
        }
        try {
            if (cameraDevice == null) {
                session.close()
                return
            }
            captureSession = session
        } finally {
            cameraOpenCloseLock.release()
        }

        // Even under the lock the surfaces can be released between here and setRepeatingRequest,
        // so the IllegalArgumentException catch remains the safety net for that request targeting
        // a surface released by closeCamera().
        handleCaptureException("Failed to request capture") {
            try {
                session.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler)
            } catch (e: IllegalArgumentException) {
                logger.error("Failed to request capture", e)
            }
        }
    }

    private fun createCaptureSessionCompat(
        camera: CameraDevice,
        imageSurface: Surface,
        qrSurface: Surface,
        previewSurface: Surface,
        stateCallback: CameraCaptureSession.StateCallback,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val executor = backgroundExecutor ?: run {
                maybeStartExecutorService()
                backgroundExecutor
            } ?: return
            val sessionConfig = SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                listOf(
                    OutputConfiguration(imageSurface),
                    OutputConfiguration(qrSurface),
                    OutputConfiguration(previewSurface),
                ),
                executor,
                stateCallback,
            )
            camera.createCaptureSession(sessionConfig)
        } else {
            @Suppress("DEPRECATION")
            camera.createCaptureSession(listOf(imageSurface, qrSurface, previewSurface), stateCallback, null)
        }
    }

    private fun captureStillImage() {
        if (isCapturing) return
        isCapturing = true
        val camera = cameraDevice ?: run { isCapturing = false; return }
        val reader = imageReader ?: run { isCapturing = false; return }

        handleCaptureException("Failed to capture still image") {
            val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(reader.surface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation())
            }
            captureSession?.capture(captureBuilder.build(), stillCaptureCallback, backgroundHandler)
        }
    }

    @VisibleForTesting
    internal val onImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        processImage(reader)
    }

    @VisibleForTesting
    internal val qrImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        if (cameraMode.value != CameraMode.QR) {
            // Drain to keep the YUV buffer queue from filling up and stalling the camera pipeline.
            reader.acquireLatestImage()?.close()
            return@OnImageAvailableListener
        }
        if (!qrInFlight.compareAndSet(false, true)) return@OnImageAvailableListener
        val image = reader.acquireLatestImage()
        if (image == null) {
            qrInFlight.set(false)
            return@OnImageAvailableListener
        }
        try {
            val result = qrAnalyzer.analyze(image)
            if (!result.isNullOrEmpty()) {
                mainHandler.post { handleQrResult(result) }
            }
        } finally {
            image.close()
            qrInFlight.set(false)
        }
    }

    @VisibleForTesting
    internal fun handleModeChanged(newMode: CameraMode) {
        if (cameraMode.value == newMode) return
        cameraMode.value = newMode
        if (newMode == CameraMode.QR) {
            qrAnalyzer.reset()
            qrResultSent = false
        }
    }

    @VisibleForTesting
    internal fun handleQrResult(qrString: String) {
        if (qrResultSent || cameraMode.value != CameraMode.QR) return
        qrResultSent = true
        val bundle = Bundle().apply { putString(RESULT_QR_STRING, qrString) }
        if (isAdded) {
            setFragmentResult(RESULT_REQUEST_KEY, bundle)
        }
    }

    @VisibleForTesting
    internal fun processImage(reader: ImageReader) {
        val image = reader.acquireLatestImage() ?: return
        try {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            val ctx = context?.applicationContext ?: return
            val imageDir = File(ctx.cacheDir, LENS_IMAGES_DIR)
            imageDir.mkdirs()
            val imageFile = File(imageDir, "lens_capture_${System.currentTimeMillis()}.jpg")
            imageFile.writeBytes(bytes)

            val uri = getUriForFile(
                ctx,
                "${ctx.packageName}.lens.fileprovider",
                imageFile,
            )

            mainHandler.post {
                isCapturing = false
                handleResult(uri)
            }
        } catch (e: IOException) {
            logger.error("Failed to save lens capture image", e)
            mainHandler.post {
                isCapturing = false
                handleResult(null)
            }
        } catch (e: SecurityException) {
            logger.error("Failed to get URI for lens capture image", e)
            mainHandler.post {
                isCapturing = false
                handleResult(null)
            }
        } finally {
            image.close()
        }
    }

    private fun getJpegOrientation(): Int {
        val deviceRotation = getScreenRotation() ?: Surface.ROTATION_0
        val degrees = ORIENTATIONS[deviceRotation] ?: 0
        return (sensorOrientation + degrees) % DEGREES_FULL_ROTATION
    }

    @VisibleForTesting
    internal fun requestGalleryPick() {
        if (isAdded) {
            setFragmentResult(RESULT_REQUEST_KEY, buildGalleryRequestBundle())
        }
    }

    @VisibleForTesting
    internal fun buildGalleryRequestBundle(): Bundle {
        val key = if (cameraMode.value == CameraMode.QR) {
            RESULT_QR_GALLERY_REQUEST
        } else {
            RESULT_GALLERY_REQUEST
        }
        return Bundle().apply { putBoolean(key, true) }
    }

    @VisibleForTesting
    internal fun handleResult(uri: Uri?) {
        val bundle = Bundle().apply {
            putParcelable(RESULT_IMAGE_URI, uri)
        }
        if (isAdded) {
            setFragmentResult(RESULT_REQUEST_KEY, bundle)
        }
    }

    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
            qrImageReader?.close()
            qrImageReader = null
            surface?.release()
            surface = null
        } catch (e: InterruptedException) {
            throw IllegalStateException("Interrupted while trying to lock camera closing.", e)
        } catch (e: RejectedExecutionException) {
            logger.error("backgroundExecutor terminated", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    @VisibleForTesting
    internal fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val size = previewSize ?: return
        val rotation = getScreenRotation() ?: Surface.ROTATION_0
        val matrix = Matrix()
        val centerX = viewWidth / 2f
        val centerY = viewHeight / 2f

        // LENS mode letterboxes so the user sees the full capture area (matters for framing
        // a still image); QR mode center-crops so detection focuses on the centered viewfinder.
        // With Compose's aspectRatio modifier the view aspect matches the buffer, so in steady
        // state both reduce to identity — the choice only takes effect during init or if the
        // view's effective bounds diverge from the buffer aspect.
        val combine: (Float, Float) -> Float =
            if (cameraMode.value == CameraMode.QR) ::max else ::min

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            val scale = combine(viewWidth.toFloat() / size.width, viewHeight.toFloat() / size.height)
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate(
                (ORIENTATION_90 * (rotation - ROTATION_LANDSCAPE_OFFSET)).toFloat(),
                centerX,
                centerY,
            )
        } else {
            // Portrait (0 or 180): the camera buffer is landscape (e.g. 1920x1080) but
            // the TextureView implicitly rotates it, so effective dimensions are swapped.
            val effectiveBufferWidth = size.height.toFloat()
            val effectiveBufferHeight = size.width.toFloat()
            val scaleX = viewWidth / effectiveBufferWidth
            val scaleY = viewHeight / effectiveBufferHeight
            val scale = combine(scaleX, scaleY)
            matrix.postScale(
                scale * effectiveBufferWidth / viewWidth,
                scale * effectiveBufferHeight / viewHeight,
                centerX,
                centerY,
            )
            if (Surface.ROTATION_180 == rotation) {
                matrix.postRotate(DEGREES_180, centerX, centerY)
            }
        }
        textureView?.setTransform(matrix)
    }

    @VisibleForTesting
    internal fun maybeStartBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("LensCameraBackground")
        }
        backgroundThread?.let {
            if (!it.isAlive) {
                it.start()
                backgroundHandler = Handler(it.looper)
            }
        }
    }

    @VisibleForTesting
    internal fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
        } catch (e: InterruptedException) {
            logger.debug("Interrupted while stopping background thread", e)
        } finally {
            backgroundThread = null
            backgroundHandler = null
        }
    }

    @VisibleForTesting
    internal fun maybeStartExecutorService() {
        if (backgroundExecutor == null) {
            backgroundExecutor = Executors.newSingleThreadExecutor()
        }
    }

    private fun stopExecutorService() {
        backgroundExecutor?.shutdownNow()
        backgroundExecutor = null
    }

    @Suppress("TooGenericExceptionCaught")
    private fun handleCaptureException(msg: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            when (e) {
                is CameraAccessException, is IllegalStateException -> logger.error(msg, e)
                else -> throw e
            }
        }
    }

    @VisibleForTesting
    internal fun getScreenRotation(): Int? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context?.display?.rotation
        } else {
            @Suppress("DEPRECATION")
            activity?.windowManager?.defaultDisplay?.rotation
        }
    }

    companion object {
        const val RESULT_REQUEST_KEY = "lens_camera_fragment_result_key"
        const val RESULT_IMAGE_URI = "lens_camera_image_uri"
        const val RESULT_GALLERY_REQUEST = "lens_camera_gallery_request"
        const val RESULT_QR_GALLERY_REQUEST = "lens_camera_qr_gallery_request"
        const val RESULT_QR_STRING = "lens_camera_qr_string"

        private const val STATE_CAMERA_MODE = "camera_mode"
        private const val STATE_QR_RESULT_SENT = "qr_result_sent"

        private const val MAX_PREVIEW_WIDTH = 1920
        private const val MAX_PREVIEW_HEIGHT = 1080
        private const val MAX_CAPTURE_DIMENSION = 4096
        private const val CAMERA_CLOSE_LOCK_TIMEOUT_MS = 2500L

        private const val ORIENTATION_0 = 0
        private const val ORIENTATION_90 = 90
        private const val ORIENTATION_180 = 180
        private const val ORIENTATION_270 = 270

        private const val DEGREES_180 = 180f
        private const val DEGREES_FULL_ROTATION = 360
        private const val ROTATION_LANDSCAPE_OFFSET = 2

        private val ORIENTATIONS = mapOf(
            Surface.ROTATION_0 to ORIENTATION_0,
            Surface.ROTATION_90 to ORIENTATION_90,
            Surface.ROTATION_180 to ORIENTATION_180,
            Surface.ROTATION_270 to ORIENTATION_270,
        )

        @VisibleForTesting
        internal fun chooseCaptureSizeFromList(sizes: Array<Size>): Size {
            require(sizes.isNotEmpty()) { "No capture sizes available from camera" }
            val filtered = sizes.filter {
                it.width <= MAX_CAPTURE_DIMENSION && it.height <= MAX_CAPTURE_DIMENSION
            }
            return if (filtered.isNotEmpty()) {
                Collections.max(filtered, compareBy { it.width.toLong() * it.height })
            } else {
                sizes[0]
            }
        }

        @VisibleForTesting
        internal fun chooseOptimalSize(
            choices: Array<Size>,
            textureViewWidth: Int,
            textureViewHeight: Int,
            maxWidth: Int,
            maxHeight: Int,
            aspectRatio: Size,
        ): Size {
            require(choices.isNotEmpty()) { "No preview sizes available from camera" }
            val bigEnough = ArrayList<Size>()
            val notBigEnough = ArrayList<Size>()
            val w = aspectRatio.width
            val h = aspectRatio.height
            for (option in choices) {
                if (option.width <= maxWidth && option.height <= maxHeight &&
                    option.height == option.width * h / w
                ) {
                    if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                        bigEnough.add(option)
                    } else {
                        notBigEnough.add(option)
                    }
                }
            }
            return when {
                bigEnough.size > 0 -> Collections.min(bigEnough, compareBy { it.width.toLong() * it.height })
                notBigEnough.size > 0 -> Collections.max(notBigEnough, compareBy { it.width.toLong() * it.height })
                else -> choices[0]
            }
        }

        @VisibleForTesting
        internal fun getDisplaySize(windowManager: android.view.WindowManager): Point {
            val size = Point()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics = windowManager.currentWindowMetrics
                val windowInsets = WindowInsetsCompat.toWindowInsetsCompat(windowMetrics.windowInsets)
                val insets = windowInsets.getInsetsIgnoringVisibility(
                    WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.displayCutout(),
                )
                val bounds: Rect = windowMetrics.bounds
                size.set(bounds.width() - insets.right - insets.left, bounds.height() - insets.top - insets.bottom)
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getSize(size)
            }
            return size
        }
    }
}
