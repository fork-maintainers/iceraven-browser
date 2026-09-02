/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.pageObjects

import android.content.Context
import android.os.SystemClock
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.selectors.ReaderViewSelectors

class ReaderViewPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "ReaderViewPage"

    // No navigation edge is registered on purpose. This page previously registered one with an empty
    // step list, which made navigateToPage() a silent no-op that reported success without going
    // anywhere. With no edge at all it fails loudly with "no navigation path found", which is the
    // honest answer: the appearance controls are only reachable on a reader-capable page (a specific
    // URL) via the reader-view toggle + Customize Reader View menu item, which the generic graph
    // cannot express. Tests drive that sequence explicitly and then act on this page's selectors.

    override fun mozGetSelectorsByGroup(group: String): List<Selector> {
        return ReaderViewSelectors.all.filter { it.groups.contains(group) }
    }

    /**
     * Clear the reader-view appearance SharedPreferences so the test starts from the feature defaults
     * (font size 3, no explicit font/color scheme). These preferences persist across test runs and across
     * the RetryTestRule's attempts, so without this a value left by an earlier run (e.g. font size 4) makes
     * the initial verifyFontSize(3) assertion fail. Mirrors the clean app state the legacy test relied on.
     */
    fun resetAppearancePrefsToDefault(): ReaderViewPage {
        InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences(READER_VIEW_PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        return this
    }

    /**
     * Assert the reader-view font size equals [expected], polling until it does (or [timeout] elapses).
     *
     * Font size is the one appearance control with no UI handle to assert against — the increase and
     * decrease buttons carry no state and the size is not surfaced on screen. It lives only in the
     * feature's SharedPreferences (as the legacy ReaderViewRobot.verifyAppearanceFontSize also read).
     *
     * A tap on the +/- button does NOT update the pref synchronously: it round-trips asynchronously
     * through the reader-view web extension before the new value is persisted, so an immediate single
     * read races ahead of the write and intermittently reads the pre-click value (seen on Firebase as
     * off-by-one flakes like expected:6 but was:5). Polling until the pref reaches [expected] both
     * closes that race and serializes each click with its effect, since the test asserts after every
     * click. A genuinely dropped click still fails correctly once the timeout elapses.
     */
    fun verifyFontSize(expected: Int, timeout: Long = 5_000, interval: Long = 100): ReaderViewPage {
        val prefs = InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences(READER_VIEW_PREFS, Context.MODE_PRIVATE)

        val deadline = SystemClock.uptimeMillis() + timeout
        var actual = prefs.getInt(FONT_SIZE_KEY, DEFAULT_FONT_SIZE)
        while (actual != expected && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(interval)
            actual = prefs.getInt(FONT_SIZE_KEY, DEFAULT_FONT_SIZE)
        }

        assertEquals(expected, actual)
        return this
    }

    private companion object {
        const val READER_VIEW_PREFS = "mozac_feature_reader_view"
        const val FONT_SIZE_KEY = "mozac-readerview-fontsize"
        const val DEFAULT_FONT_SIZE = 3
    }
}
