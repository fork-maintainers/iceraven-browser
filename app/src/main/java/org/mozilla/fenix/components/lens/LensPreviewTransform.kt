/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.lens

import android.graphics.Matrix
import android.graphics.Point
import android.graphics.RectF
import android.util.Size
import android.view.Surface
import kotlin.math.min

internal const val ORIENTATION_0 = 0
internal const val ORIENTATION_90 = 90
internal const val ORIENTATION_180 = 180
internal const val ORIENTATION_270 = 270

private const val DEGREES_180 = 180f
private const val ROTATION_LANDSCAPE_OFFSET = 2

private const val MAX_PREVIEW_WIDTH = 1920
private const val MAX_PREVIEW_HEIGHT = 1080

/**
 * Preview size and rotation dimensions for the camera preview.
 *
 * @property rotatedWidth View width in sensor orientation, used to pick the preview size.
 * @property rotatedHeight View height in sensor orientation, used to pick the preview size.
 * @property maxWidth Upper bound on the preview width.
 * @property maxHeight Upper bound on the preview height.
 */
internal data class PreviewConstraints(
    val rotatedWidth: Int,
    val rotatedHeight: Int,
    val maxWidth: Int,
    val maxHeight: Int,
)

/**
 * Geometry calculations for the Google Lens camera preview.
 *
 * Preview scaling is handled in two stages:
 *
 * 1. `LensCameraScreen` sizes the preview `TextureView` to match the camera buffer's aspect ratio,
 *    using the ratio from [displayAspectRatio].
 * 2. The matrix from [forRotation] corrects the sensor rotation and compensates for TextureView's
 *    default behaviour of stretching the buffer to fill its bounds.
 *
 * Once the aspect ratio is known and the view has been resized, the compensating scale drops out:
 * the matrix reduces to identity when upright and to the sensor rotation alone in landscape.
 * Before then, the matrix letterboxes the preview as a temporary fallback. Any uncovered area
 * shows the black background behind the TextureView.
 *
 * Both stages are driven by the same [areDimensionsSwapped] result, so they always agree on which
 * axis of the camera buffer is the long one on screen.
 */
internal object LensPreviewTransform {

    /**
     * Returns the preview transform for a [bufferSize] camera buffer displayed in a
     * [viewWidth] x [viewHeight] TextureView at the given screen [rotation], where [swapped] is
     * the [areDimensionsSwapped] result for that rotation.
     */
    fun forRotation(viewWidth: Int, viewHeight: Int, bufferSize: Size, rotation: Int, swapped: Boolean): Matrix {
        if (viewWidth <= 0 || viewHeight <= 0) return Matrix()

        val contentSize = displayOrientedSize(bufferSize, swapped)
        return if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            rotatedPreview(viewWidth, viewHeight, contentSize, rotation)
        } else {
            uprightPreview(viewWidth, viewHeight, contentSize, rotation)
        }
    }

    private fun rotatedPreview(viewWidth: Int, viewHeight: Int, contentSize: Size, rotation: Int): Matrix {
        // The postRotate below transposes the content, so the pre-rotation rect is the transpose
        // of the on-screen size.
        val bufferRect = RectF(0f, 0f, contentSize.height.toFloat(), contentSize.width.toFloat())
        val centerX = viewWidth / 2f
        val centerY = viewHeight / 2f
        bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())

        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())

        val matrix = Matrix()
        // FILL is intentional: its non-uniform scale cancels the TextureView's (also
        // non-uniform) base fill so the composite scale stays uniform. The uniform
        // postScale below then fits the rotated buffer inside the view.
        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
        val scale = fitScale(viewWidth, viewHeight, contentSize)
        matrix.postScale(scale, scale, centerX, centerY)
        matrix.postRotate(
            (ORIENTATION_90 * (rotation - ROTATION_LANDSCAPE_OFFSET)).toFloat(),
            centerX,
            centerY,
        )

        return matrix
    }

    private fun uprightPreview(viewWidth: Int, viewHeight: Int, contentSize: Size, rotation: Int): Matrix {
        val centerX = viewWidth / 2f
        val centerY = viewHeight / 2f

        val matrix = Matrix()
        // The postScale cancels the TextureView base fill, leaving a uniform scale.
        val scale = fitScale(viewWidth, viewHeight, contentSize)
        matrix.postScale(
            scale * contentSize.width / viewWidth,
            scale * contentSize.height / viewHeight,
            centerX,
            centerY,
        )
        if (rotation == Surface.ROTATION_180) {
            matrix.postRotate(DEGREES_180, centerX, centerY)
        }

        return matrix
    }

    private fun fitScale(viewWidth: Int, viewHeight: Int, contentSize: Size): Float =
        min(viewWidth.toFloat() / contentSize.width, viewHeight.toFloat() / contentSize.height)

    /**
     * Transposes [bufferSize] from sensor orientation into display orientation when the two
     * disagree about which axis is the long one.
     */
    private fun displayOrientedSize(bufferSize: Size, swapped: Boolean): Size =
        if (swapped) Size(bufferSize.height, bufferSize.width) else bufferSize

    /**
     * Whether the view and sensor dimensions are transposed relative to each other, which happens
     * when [displayRotation] and [sensorOrientation] disagree about which axis is the long one.
     */
    fun areDimensionsSwapped(displayRotation: Int, sensorOrientation: Int): Boolean =
        when (displayRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 ->
                sensorOrientation == ORIENTATION_90 || sensorOrientation == ORIENTATION_270
            Surface.ROTATION_90, Surface.ROTATION_270 ->
                sensorOrientation == ORIENTATION_0 || sensorOrientation == ORIENTATION_180
            else -> false
        }

    /**
     * Maps the view and display dimensions into sensor orientation, capped at the maximum preview
     * dimensions the camera pipeline supports. [swapped] is the [areDimensionsSwapped] result for
     * the current rotation.
     */
    fun previewConstraints(
        viewWidth: Int,
        viewHeight: Int,
        displaySize: Point,
        swapped: Boolean,
    ): PreviewConstraints = PreviewConstraints(
        rotatedWidth = if (swapped) viewHeight else viewWidth,
        rotatedHeight = if (swapped) viewWidth else viewHeight,
        maxWidth = min(if (swapped) displaySize.y else displaySize.x, MAX_PREVIEW_WIDTH),
        maxHeight = min(if (swapped) displaySize.x else displaySize.y, MAX_PREVIEW_HEIGHT),
    )

    /**
     * Display-oriented width/height ratio for a [previewSize] camera buffer, where [swapped] is
     * the [areDimensionsSwapped] result for the current rotation.
     */
    fun displayAspectRatio(previewSize: Size, swapped: Boolean): Float {
        val size = displayOrientedSize(previewSize, swapped)
        return size.width.toFloat() / size.height.toFloat()
    }
}
