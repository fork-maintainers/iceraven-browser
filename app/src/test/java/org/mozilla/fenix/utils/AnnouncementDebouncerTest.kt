/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class AnnouncementDebouncerTest {

    @Test
    fun `WHEN actions fire within the delay window THEN only the first and post-window ones run`() {
        var now = 10_000L
        val debouncer = AnnouncementDebouncer(currentTimeMillis = { now })
        var runCount = 0
        val action: () -> Unit = { runCount++ }

        debouncer.debounce(delay = 2000, action = action)
        assertEquals(1, runCount)

        now = 11_999L
        debouncer.debounce(delay = 2000, action = action)
        assertEquals(1, runCount)

        // 2000ms >= 2000ms: fires again at the exact boundary.
        now = 12_000L
        debouncer.debounce(delay = 2000, action = action)
        assertEquals(2, runCount)
    }
}
