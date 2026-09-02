/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.lens

import android.graphics.Matrix
import android.graphics.Point
import android.graphics.RectF
import android.util.Size
import android.view.Surface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LensPreviewTransformTest {

    private fun Matrix.values(): FloatArray = FloatArray(9).also { getValues(it) }

    private fun Matrix.mapViewRect(viewWidth: Int, viewHeight: Int): RectF =
        RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat()).also { mapRect(it) }

    // --- forRotation tests: steady state, where the view has been sized to the buffer aspect ---

    @Test
    fun `GIVEN a portrait view already matching the buffer aspect WHEN forRotation is called THEN the matrix is identity`() {
        // The TextureView base fill alone maps the buffer onto the view, so nothing to correct.
        val matrix = LensPreviewTransform.forRotation(
            viewWidth = 1080,
            viewHeight = 1920,
            bufferSize = Size(1920, 1080),
            rotation = Surface.ROTATION_0,
            swapped = true,
        )

        assertTrue(matrix.isIdentity)
    }

    @Test
    fun `GIVEN a landscape view already matching the buffer aspect WHEN forRotation is called THEN the preview fills the view`() {
        val matrix = LensPreviewTransform.forRotation(
            viewWidth = 1920,
            viewHeight = 1080,
            bufferSize = Size(1920, 1080),
            rotation = Surface.ROTATION_90,
            swapped = false,
        )

        // Pure rotation: the view rect maps onto itself, so the preview covers the view exactly
        // with no letterbox bands and no distortion.
        assertEquals(RectF(0f, 0f, 1920f, 1080f), matrix.mapViewRect(1920, 1080))
    }

    @Test
    fun `GIVEN an upright sensor in portrait WHEN forRotation is called THEN the buffer is not transposed`() {
        // areDimensionsSwapped is false for a SENSOR_ORIENTATION 0 device held upright, so the
        // buffer is already in display orientation and the view was sized landscape to match.
        val matrix = LensPreviewTransform.forRotation(
            viewWidth = 1920,
            viewHeight = 1080,
            bufferSize = Size(1920, 1080),
            rotation = Surface.ROTATION_0,
            swapped = false,
        )

        assertTrue(matrix.isIdentity)
    }

    @Test
    fun `GIVEN an upright sensor in landscape WHEN forRotation is called THEN the preview fills the view`() {
        // Regression guard: this configuration used to transpose the buffer unconditionally,
        // shrinking the preview to a 1080x607 band inside the view.
        val matrix = LensPreviewTransform.forRotation(
            viewWidth = 1080,
            viewHeight = 1920,
            bufferSize = Size(1920, 1080),
            rotation = Surface.ROTATION_90,
            swapped = true,
        )

        assertEquals(RectF(0f, 0f, 1080f, 1920f), matrix.mapViewRect(1080, 1920))
    }

    // --- forRotation tests: transient state, before the view has been resized ---

    @Test
    fun `GIVEN portrait rotation WHEN forRotation is called THEN matrix uses uniform letterbox scale`() {
        // Landscape camera buffer mapped into a portrait view that is taller than the buffer.
        val matrix = LensPreviewTransform.forRotation(
            viewWidth = 1080,
            viewHeight = 2400,
            bufferSize = Size(1920, 1080),
            rotation = Surface.ROTATION_0,
            swapped = true,
        )

        // The buffer fits inside the view (letterbox) with a single uniform net scale on both
        // axes, so the true aspect ratio is preserved. Effective scale = min(1.0, 1.25) = 1.0.
        // postScale args = (1.0 * 1080/1080, 1.0 * 1920/2400) = (1.0, 0.8); combined with the
        // TextureView base fill (1080/1080, 2400/1920) the net scale is (1.0, 1.0).
        val values = matrix.values()
        assertEquals(1.0f, values[Matrix.MSCALE_X], 0.001f)
        assertEquals(0.8f, values[Matrix.MSCALE_Y], 0.001f)
    }

    @Test
    fun `GIVEN upside-down rotation WHEN forRotation is called THEN letterbox scale is negated by the 180 degree rotation`() {
        val matrix = LensPreviewTransform.forRotation(
            viewWidth = 1080,
            viewHeight = 2400,
            bufferSize = Size(1920, 1080),
            rotation = Surface.ROTATION_180,
            swapped = true,
        )

        // Same scale magnitudes as ROTATION_0, negated on both axes by the postRotate(180).
        val values = matrix.values()
        assertEquals(-1.0f, values[Matrix.MSCALE_X], 0.001f)
        assertEquals(-0.8f, values[Matrix.MSCALE_Y], 0.001f)
    }

    @Test
    fun `GIVEN landscape rotation WHEN forRotation is called THEN the preview is letterboxed inside the view`() {
        val matrix = LensPreviewTransform.forRotation(
            viewWidth = 2400,
            viewHeight = 1080,
            bufferSize = Size(1920, 1080),
            rotation = Surface.ROTATION_90,
            swapped = false,
        )

        // The 1.7778 buffer is fitted into a wider 2.22 view, leaving 240px bands either side.
        assertEquals(RectF(240f, 0f, 2160f, 1080f), matrix.mapViewRect(2400, 1080))
    }

    @Test
    fun `GIVEN landscape rotation WHEN forRotation is called THEN the rotation direction is applied`() {
        val matrix = LensPreviewTransform.forRotation(
            viewWidth = 2400,
            viewHeight = 1080,
            bufferSize = Size(1920, 1080),
            rotation = Surface.ROTATION_90,
            swapped = false,
        )

        // setRectToRect(FILL) maps the view onto the centered, transposed buffer rect
        // (scale 0.45 x 1.7778), then the -90 rotation about the center moves those factors into
        // the skew terms and leaves the scale terms at ~0.
        val values = matrix.values()
        assertEquals(0f, values[Matrix.MSCALE_X], 0.001f)
        assertEquals(1.7778f, values[Matrix.MSKEW_X], 0.001f)
        assertEquals(-0.45f, values[Matrix.MSKEW_Y], 0.001f)
        assertEquals(0f, values[Matrix.MSCALE_Y], 0.001f)
    }

    @Test
    fun `GIVEN reverse landscape rotation WHEN forRotation is called THEN the skew terms mirror the landscape case`() {
        val matrix = LensPreviewTransform.forRotation(
            viewWidth = 2400,
            viewHeight = 1080,
            bufferSize = Size(1920, 1080),
            rotation = Surface.ROTATION_270,
            swapped = false,
        )

        // Same magnitudes as ROTATION_90 with the rotation applied in the opposite direction,
        // so both skew terms flip sign.
        val values = matrix.values()
        assertEquals(0f, values[Matrix.MSCALE_X], 0.001f)
        assertEquals(-1.7778f, values[Matrix.MSKEW_X], 0.001f)
        assertEquals(2160f, values[Matrix.MTRANS_X], 0.001f)
        assertEquals(0.45f, values[Matrix.MSKEW_Y], 0.001f)
        assertEquals(0f, values[Matrix.MSCALE_Y], 0.001f)
        assertEquals(0f, values[Matrix.MTRANS_Y], 0.001f)
    }

    @Test
    fun `GIVEN a view that has not been laid out WHEN forRotation is called THEN the matrix is identity`() {
        val matrix = LensPreviewTransform.forRotation(
            viewWidth = 0,
            viewHeight = 0,
            bufferSize = Size(1920, 1080),
            rotation = Surface.ROTATION_0,
            swapped = true,
        )

        assertTrue(matrix.isIdentity)
    }

    // --- areDimensionsSwapped tests ---

    @Test
    fun `GIVEN portrait display and a 90 degree sensor WHEN areDimensionsSwapped is called THEN dimensions are swapped`() {
        assertTrue(LensPreviewTransform.areDimensionsSwapped(Surface.ROTATION_0, ORIENTATION_90))
        assertTrue(LensPreviewTransform.areDimensionsSwapped(Surface.ROTATION_180, ORIENTATION_270))
    }

    @Test
    fun `GIVEN landscape display and an upright sensor WHEN areDimensionsSwapped is called THEN dimensions are swapped`() {
        assertTrue(LensPreviewTransform.areDimensionsSwapped(Surface.ROTATION_90, ORIENTATION_0))
        assertTrue(LensPreviewTransform.areDimensionsSwapped(Surface.ROTATION_270, ORIENTATION_180))
    }

    @Test
    fun `GIVEN display and sensor orientations agree WHEN areDimensionsSwapped is called THEN dimensions are not swapped`() {
        assertFalse(LensPreviewTransform.areDimensionsSwapped(Surface.ROTATION_0, ORIENTATION_0))
        assertFalse(LensPreviewTransform.areDimensionsSwapped(Surface.ROTATION_90, ORIENTATION_90))
    }

    // --- previewConstraints tests ---

    @Test
    fun `GIVEN dimensions are not swapped WHEN previewConstraints is called THEN view and display dimensions pass through`() {
        val constraints = LensPreviewTransform.previewConstraints(
            viewWidth = 1080,
            viewHeight = 2400,
            displaySize = Point(1080, 1000),
            swapped = false,
        )

        assertEquals(1080, constraints.rotatedWidth)
        assertEquals(2400, constraints.rotatedHeight)
        assertEquals(1080, constraints.maxWidth)
        assertEquals(1000, constraints.maxHeight)
    }

    @Test
    fun `GIVEN dimensions are swapped WHEN previewConstraints is called THEN view and display dimensions are transposed`() {
        val constraints = LensPreviewTransform.previewConstraints(
            viewWidth = 1080,
            viewHeight = 2400,
            displaySize = Point(1080, 1000),
            swapped = true,
        )

        assertEquals(2400, constraints.rotatedWidth)
        assertEquals(1080, constraints.rotatedHeight)
        assertEquals(1000, constraints.maxWidth)
        assertEquals(1080, constraints.maxHeight)
    }

    @Test
    fun `GIVEN a display larger than the preview maximums WHEN previewConstraints is called THEN the maximums are clamped`() {
        val constraints = LensPreviewTransform.previewConstraints(
            viewWidth = 1440,
            viewHeight = 3200,
            displaySize = Point(2400, 3200),
            swapped = false,
        )

        assertEquals(1920, constraints.maxWidth)
        assertEquals(1080, constraints.maxHeight)
    }

    // --- displayAspectRatio tests ---

    @Test
    fun `GIVEN dimensions are not swapped WHEN displayAspectRatio is called THEN the buffer ratio is returned`() {
        assertEquals(1.7778f, LensPreviewTransform.displayAspectRatio(Size(1920, 1080), swapped = false), 0.001f)
    }

    @Test
    fun `GIVEN dimensions are swapped WHEN displayAspectRatio is called THEN the inverted buffer ratio is returned`() {
        assertEquals(0.5625f, LensPreviewTransform.displayAspectRatio(Size(1920, 1080), swapped = true), 0.001f)
    }
}
