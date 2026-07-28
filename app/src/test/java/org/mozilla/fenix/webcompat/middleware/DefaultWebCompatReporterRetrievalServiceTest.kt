/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.webcompat.middleware

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.webcompat.di.WebCompatReporterMiddlewareProvider
import org.mozilla.fenix.webcompat.fake.FakeEngineSession
import org.mozilla.fenix.webcompat.testdata.WebCompatTestData

@RunWith(AndroidJUnit4::class)
class DefaultWebCompatReporterRetrievalServiceTest {
    private val webCompatInfoDeserializer = WebCompatReporterMiddlewareProvider.provideWebCompatInfoDeserializer()

    @Test
    fun `WHEN WebCompatInfo is retrieved successfully THEN all corresponding fields in the DTO are submitted`() = runTest {
        val engineSession = FakeEngineSession(WebCompatTestData.basicDataJson)
        val service = createService(engineSession = engineSession)
        val actual = service.retrieveInfo()
        val expected = WebCompatTestData.createTestObject()
        assertEquals(expected, actual)
    }

    @Test
    fun `WHEN a required WebCompatInfo field is missing THEN null is returned`() = runTest {
        val engineSession = FakeEngineSession(WebCompatTestData.missingDataJson)
        val service = createService(engineSession = engineSession)

        val info = service.retrieveInfo()

        assertNull(info)
    }

    @Test
    fun `GIVEN the json has irrelevant fields WHEN calling decode THEN the relevant fields are parsed and the irrelevant fields are ignored`() = runTest {
        val engineSession = FakeEngineSession(WebCompatTestData.extraDataJson)
        val service = createService(engineSession = engineSession)
        val actual = service.retrieveInfo()
        val expected = WebCompatTestData.createTestObject(blockedOrigins = listOf("https://blockedUrlExample.com", "https://blockedUrlExample2.com"))
        assertEquals(expected, actual)
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
            webCompatInfoDeserializer = webCompatInfoDeserializer,
        )
    }
}
