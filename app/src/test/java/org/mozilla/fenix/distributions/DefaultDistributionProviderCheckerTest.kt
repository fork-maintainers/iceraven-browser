/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.distributions

import android.content.ComponentName
import android.content.ContentProvider
import android.content.ContentValues
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.runBlocking
import mozilla.components.support.test.robolectric.testContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class DefaultDistributionProviderCheckerTest {
    private val subject = DefaultDistributionProviderChecker(testContext)

    @After
    fun tearDown() {
        TestContentProvider.shouldThrowOnQuery = false
    }

    @Test
    fun `WHEN a content provider exists THEN the provider name is returned`() = runBlocking {
        createFakeContentProviderForAdjust(
            otherAppsPackageName = "some.package",
            providerName = "myProvider",
        )

        val provider = subject.queryProvider()

        assertEquals("myProvider", provider)
    }

    @Test
    fun `WHEN a content provider does not exists THEN null is returned`() = runBlocking {
        val provider = subject.queryProvider()

        assertEquals(null, provider)
    }

    @Test
    fun `WHEN a content provider exists but does not have the data THEN null is returned`() =
        runBlocking {
            createFakeContentProviderForAdjust(
                otherAppsPackageName = "some.package",
                columns = listOf(),
            )

            val provider = subject.queryProvider()

            assertEquals(null, provider)
        }

    @Test
    fun `WHEN a content provider exists but does not have the correct package_name THEN null is returned`() =
        runBlocking {
            createFakeContentProviderForAdjust(
                otherAppsPackageName = "some.package",
                providerName = "myProvider",
                columns = listOf(
                    Pair("com.test", Pair("encrypted_data", "{\"provider\": \"provider\"}")),
                ),
            )

            val provider = subject.queryProvider()

            assertEquals(null, provider)
        }

    @Test
    fun `WHEN the encrypted_data column is not json THEN null is returned`() = runBlocking {
        createFakeContentProviderForAdjust(
            otherAppsPackageName = "some.package",
            columns = listOf(
                Pair("org.mozilla.fenix.debug", Pair("encrypted_data", "not json")),
            ),
        )

        val provider = subject.queryProvider()

        assertEquals(null, provider)
    }

    @Test
    fun `WHEN the content provider throws an exception THEN null is returned without crashing`() =
        runBlocking {
            createFakeContentProviderForAdjust(
                otherAppsPackageName = "some.package",
                providerName = "myProvider",
            )
            TestContentProvider.shouldThrowOnQuery = true

            val provider = subject.queryProvider()

            assertEquals(null, provider)
        }

    @Test
    fun `WHEN the encrypted_data column does not have a provider string THEN null is returned`() =
        runBlocking {
            createFakeContentProviderForAdjust(
                otherAppsPackageName = "some.package",
                columns = listOf(
                    Pair("org.mozilla.fenix.debug", Pair("encrypted_data", "{\"test\": \"test\"}")),
                ),
            )

            val provider = subject.queryProvider()

            assertEquals(null, provider)
        }

    @Suppress("SameParameterValue")
    private fun createFakeContentProviderForAdjust(
        otherAppsPackageName: String,
        providerName: String = "provider",
        columns: List<Pair<String, Pair<String, String>>> = listOf(
            Pair("org.mozilla.fenix.debug", Pair("encrypted_data", "{\"provider\": \"$providerName\"}")),
        ),
    ) {
        val shadowPackageManager = shadowOf(testContext.packageManager)

        // Register a fake app with a fake content provider
        val providerInfo = ProviderInfo().apply {
            authority = otherAppsPackageName
            name = TestContentProvider::class.qualifiedName
            this.packageName = otherAppsPackageName
            applicationInfo = ApplicationInfo().apply {
                this.packageName = otherAppsPackageName
                flags = ApplicationInfo.FLAG_INSTALLED
            }
        }
        shadowPackageManager.addOrUpdateProvider(providerInfo)

        // Insert test data into a fake content provider
        val contentProvider = Robolectric.buildContentProvider(TestContentProvider::class.java)
            .create(providerInfo)
            .get()
        val uri = "content://$otherAppsPackageName/trackers".toUri()
        columns.forEach {
            val values = ContentValues().apply {
                put(it.second.first, it.second.second)
            }
            contentProvider.insert(uri, values, it.first)
        }

        // Make the content provider discoverable via an intent action
        shadowPackageManager.addIntentFilterForProvider(
            ComponentName(providerInfo.packageName, providerInfo.name),
            IntentFilter("com.attribution.REFERRAL_PROVIDER"),
        )
    }

    class TestContentProvider : ContentProvider() {
        private val database = mutableListOf<Pair<String, ContentValues>>()

        companion object {
            var shouldThrowOnQuery = false
        }

        override fun onCreate(): Boolean = true

        override fun insert(uri: Uri, values: ContentValues?): Uri {
            return uri
        }

        fun insert(uri: Uri, values: ContentValues?, packageName: String): Uri {
            values?.let { database.add(Pair(packageName, it)) }
            return uri
        }

        override fun query(
            uri: Uri,
            projection: Array<String>?,
            selection: String?,
            selectionArgs: Array<String>?,
            sortOrder: String?,
        ): Cursor {
            if (shouldThrowOnQuery) {
                throw IllegalStateException("Simulated third-party provider failure")
            }

            val cursor = MatrixCursor(projection ?: emptyArray())

            val selectionKey = if (selection == "package_name=?") "package_name" else null
            val selectionValue = selectionArgs?.firstOrNull()

            for (values in database) {
                val matches = selectionKey == null || values.first == selectionValue
                if (!matches) continue

                val row = projection?.map { values.second.getAsString(it) }?.toTypedArray() ?: emptyArray()
                cursor.addRow(row)
            }

            return cursor
        }

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<String>?,
        ): Int = 1

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 1

        override fun getType(uri: Uri): String = ""
    }
}
