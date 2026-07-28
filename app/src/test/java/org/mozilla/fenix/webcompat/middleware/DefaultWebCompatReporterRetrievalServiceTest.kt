/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.webcompat.middleware

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineSession
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.webcompat.fake.FakeEngineSession
import org.mozilla.fenix.webcompat.testdata.WebCompatTestData

@RunWith(AndroidJUnit4::class)
class DefaultWebCompatReporterRetrievalServiceTest {
    @Test
    fun `WHEN WebCompatInfo is retrieved successfully THEN all corresponding fields in the JSON are submitted`() = runTest {
        val engineSession = FakeEngineSession(WebCompatTestData.basicDataJson)
        val service = createService(engineSession = engineSession)
        val actual = service.retrieveInfo()
        val expected = WebCompatTestData.createTestObject()
        assertEquals(expected.toString(), actual.toString())
    }

    private fun createService(engineSession: EngineSession): WebCompatReporterRetrievalService {
        val tab = createTab(
            url = "https://www.mozilla.org",
            id = "test-tab",
            engineSession = engineSession,
        )
        val browserStore = BrowserStore(
            initialState = BrowserState(
                tabs = listOf(tab),
                selectedTabId = tab.id,
            ),
        )

        return DefaultWebCompatReporterRetrievalService(
            browserStore = browserStore,
        )
    }
}
