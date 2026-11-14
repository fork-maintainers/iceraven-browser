/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.search

import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.support.ktx.kotlin.toNormalizedUrl
import mozilla.components.support.ktx.util.URLStringUtils.toLosslessEditableUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddressBarUrlTest {
    @Test
    fun `current page identity accepts readable prefill but not changed data`() {
        val source = "https://example.com/a%20b?token=secret%20value"
        assertTrue(isCurrentUrl(toLosslessEditableUrl(source), source))
        assertTrue(isCurrentUrl(source, source))
        assertFalse(isCurrentUrl("ordinary search", source))
        assertFalse(isCurrentUrl(source.replace("token", "changed"), source))
        assertFalse(isCurrentUrl(source, null))
    }

    @Test
    fun `readable text loads the same URL and is not decoded a second time`() {
        val source = "https://example.com/%2520/a%2Fb?q=a+b%2Bc%20d"
        val readable = toLosslessEditableUrl(source)
        assertEquals(source, readable.toNormalizedUrl())
        assertEquals(readable, toLosslessEditableUrl(readable))
    }
}
