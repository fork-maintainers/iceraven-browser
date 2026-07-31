/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.theme

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TabStripStatusBarViewTest {

    @Test
    fun `WHEN show is called THEN gradient overlay is added to the decor view`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).create().get()
        val decorView = activity.window.decorView as ViewGroup
        val initialChildCount = decorView.childCount

        TabStripStatusBarView(decorView, activity.lifecycle).show()

        assertEquals(initialChildCount + 1, decorView.childCount)
    }

    @Test
    fun `WHEN show is called twice THEN only one gradient overlay is added`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).create().get()
        val decorView = activity.window.decorView as ViewGroup
        val initialChildCount = decorView.childCount
        val view = TabStripStatusBarView(decorView, activity.lifecycle)

        view.show()
        view.show()

        assertEquals(initialChildCount + 1, decorView.childCount)
    }

    @Test
    fun `WHEN hide is called THEN the gradient overlay is removed from the decor view`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).create().get()
        val decorView = activity.window.decorView as ViewGroup
        val initialChildCount = decorView.childCount
        val view = TabStripStatusBarView(decorView, activity.lifecycle)

        view.show()
        assertEquals(initialChildCount + 1, decorView.childCount)

        view.hide()
        assertEquals(initialChildCount, decorView.childCount)
    }
}
