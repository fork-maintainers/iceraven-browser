/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.debugsettings

import android.content.Intent
import mozilla.components.support.test.robolectric.testContext
import mozilla.telemetry.glean.Glean
import mozilla.telemetry.glean.debug.GleanDebugActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.debugsettings.gleandebugtools.DefaultGleanDebugToolsStorage
import org.mozilla.fenix.helpers.FenixGleanTestRule
import org.mozilla.fenix.utils.Settings
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GleanDebugToolsStorageTest {

    @get:Rule
    val gleanTestRule = FenixGleanTestRule(testContext)

    private lateinit var settings: Settings

    @Before
    fun setup() {
        settings = Settings(testContext)
    }

    // The intents below mirror what GleanDebugActivity forwards to HomeActivity from the adb command:
    //   adb shell am start -n <pkg>/mozilla.telemetry.glean.debug.GleanDebugActivity \
    //     --es debugViewTag <tag> --ez persistDebugViewTag true
    // (TAG_DEBUG_VIEW_EXTRA_KEY = --es debugViewTag, PERSIST_DEBUG_VIEW_TAG_EXTRA = --ez persistDebugViewTag)
    @Test
    fun `GIVEN a debug tag and the persist flag WHEN persistDebugViewTagIfRequested THEN the tag is saved`() {
        val intent = Intent().apply {
            putExtra(GleanDebugActivity.TAG_DEBUG_VIEW_EXTRA_KEY, "my-tag")
            putExtra(DefaultGleanDebugToolsStorage.PERSIST_DEBUG_VIEW_TAG_EXTRA, true)
        }

        DefaultGleanDebugToolsStorage.persistDebugViewTagIfRequested(intent, settings)

        assertEquals("my-tag", settings.gleanDebugViewTag)
    }

    @Test
    fun `GIVEN a debug tag but no persist flag WHEN persistDebugViewTagIfRequested THEN the persisted tag is cleared`() {
        settings.gleanDebugViewTag = "previously-persisted"
        val intent = Intent().apply {
            putExtra(GleanDebugActivity.TAG_DEBUG_VIEW_EXTRA_KEY, "new-session-tag")
        }

        DefaultGleanDebugToolsStorage.persistDebugViewTagIfRequested(intent, settings)

        assertEquals("", settings.gleanDebugViewTag)
    }

    @Test
    fun `GIVEN no special intent WHEN persistDebugViewTagIfRequested THEN no tag is persisted`() {
        val intent = Intent()

        DefaultGleanDebugToolsStorage.persistDebugViewTagIfRequested(intent, settings)

        assertEquals("", settings.gleanDebugViewTag)
    }

    @Test
    fun `GIVEN no debug tag in the intent WHEN persistDebugViewTagIfRequested THEN the persisted tag is unchanged`() {
        settings.gleanDebugViewTag = "previously-persisted"
        val intent = Intent()

        DefaultGleanDebugToolsStorage.persistDebugViewTagIfRequested(intent, settings)

        assertEquals("previously-persisted", settings.gleanDebugViewTag)
    }

    @Test
    fun `GIVEN the persist flag but no debug tag WHEN persistDebugViewTagIfRequested THEN the persisted tag is unchanged`() {
        settings.gleanDebugViewTag = "previously-persisted"
        val intent = Intent().apply {
            putExtra(DefaultGleanDebugToolsStorage.PERSIST_DEBUG_VIEW_TAG_EXTRA, true)
        }

        DefaultGleanDebugToolsStorage.persistDebugViewTagIfRequested(intent, settings)

        assertEquals("previously-persisted", settings.gleanDebugViewTag)
    }

    @Test
    fun `GIVEN the persist flag but an empty debug tag WHEN persistDebugViewTagIfRequested THEN the persisted tag is unchanged`() {
        settings.gleanDebugViewTag = "previously-persisted"
        val intent = Intent().apply {
            putExtra(GleanDebugActivity.TAG_DEBUG_VIEW_EXTRA_KEY, "")
            putExtra(DefaultGleanDebugToolsStorage.PERSIST_DEBUG_VIEW_TAG_EXTRA, true)
        }

        DefaultGleanDebugToolsStorage.persistDebugViewTagIfRequested(intent, settings)

        assertEquals("previously-persisted", settings.gleanDebugViewTag)
    }

    @Test
    fun `GIVEN a persisted debug view tag WHEN clearPersistedDebugViewTag THEN the persisted tag is cleared`() {
        settings.gleanDebugViewTag = "previously-persisted"

        DefaultGleanDebugToolsStorage(settings).clearPersistedDebugViewTag()

        assertEquals("", settings.gleanDebugViewTag)
    }

    @Test
    fun `GIVEN a persisted debug view tag WHEN restorePersistedDebugViewTag THEN it is applied to Glean`() {
        settings.gleanDebugViewTag = "restored-tag"

        DefaultGleanDebugToolsStorage.restorePersistedDebugViewTag(settings)

        assertEquals("restored-tag", Glean.getDebugViewTag())
    }

    @Test
    fun `GIVEN no persisted debug view tag WHEN restorePersistedDebugViewTag THEN Glean is not called`() {
        DefaultGleanDebugToolsStorage.restorePersistedDebugViewTag(settings)

        assertNull(Glean.getDebugViewTag())
    }
}
