/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.addons

import AddonInstallIntentProcessor
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mozilla.components.concept.engine.webextension.WebExtensionRuntime
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream

@RunWith(AndroidJUnit4::class)
class AddonInstallIntentProcessorTest {

    private val uri = Uri.parse("content://com.example.provider/files/addon.xpi")
    private lateinit var resolver: ContentResolver
    private lateinit var runtime: WebExtensionRuntime
    private lateinit var processor: AddonInstallIntentProcessor

    @Before
    fun setup() {
        resolver = mockk(relaxed = true)
        runtime = mockk(relaxed = true)
        val context = object : ContextWrapper(testContext) {
            override fun getContentResolver(): ContentResolver = resolver
        }
        processor = AddonInstallIntentProcessor(context as Context, runtime)
    }

    private fun intent() = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/x-xpinstall")

    @Test
    fun `GIVEN a provider returning no stream WHEN processing THEN the intent is handled without installing`() {
        every { resolver.openInputStream(uri) } returns null

        assertTrue(processor.process(intent()))

        verify(exactly = 0) { runtime.installWebExtension(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN a provider throwing SecurityException WHEN processing THEN the intent is handled without installing`() {
        every { resolver.openInputStream(uri) } throws SecurityException("denied")

        assertTrue(processor.process(intent()))

        verify(exactly = 0) { runtime.installWebExtension(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN a readable stream WHEN copying the add-on THEN the bytes end up in the cache directory`() {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        every { resolver.openInputStream(uri) } returns ByteArrayInputStream(bytes)

        val file = processor.fromUri(uri)

        assertTrue(file.exists())
        assertTrue(file.absolutePath.startsWith((testContext.externalCacheDir ?: testContext.cacheDir).absolutePath))
        assertArrayEquals(bytes, file.readBytes())
    }
}
