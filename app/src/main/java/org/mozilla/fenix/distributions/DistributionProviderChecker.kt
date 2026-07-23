/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.distributions

import android.content.Context
import android.content.Intent
import android.database.Cursor
import androidx.core.net.toUri
import mozilla.components.support.base.log.logger.Logger
import org.json.JSONException
import org.json.JSONObject

private const val ADJUST_CONTENT_PROVIDER_INTENT_ACTION = "com.attribution.REFERRAL_PROVIDER"

private const val ENCRYPTED_DATA_COLUMN = "encrypted_data"

/**
 * A tool for trying to get a provider from a content resolver meant for adjust.
 */
interface DistributionProviderChecker {
    /**
     * Looks for the provider value
     */
    suspend fun queryProvider(): String?
}

private val logger = Logger(DistributionProviderChecker::class.simpleName)

/**
 * Default implementation for DistributionProviderChecker
 *
 * @param context application context used to get the packageManager and contentResolver
 */
class DefaultDistributionProviderChecker(private val context: Context) : DistributionProviderChecker {
    private val classVersion = "Default"

    override suspend fun queryProvider(): String? {
        logger.info("$classVersion - Starting check...")
        val adjustProviderIntent = Intent(ADJUST_CONTENT_PROVIDER_INTENT_ACTION)
        val contentProviders = context.packageManager.queryIntentContentProviders(
            adjustProviderIntent,
            0,
        )
        val contentResolver = context.contentResolver

        for (resolveInfo in contentProviders) {
            val authority = resolveInfo.providerInfo.authority
            val uri = "content://$authority/trackers".toUri()

            val projection = arrayOf(ENCRYPTED_DATA_COLUMN)

            @Suppress("TooGenericExceptionCaught")
            val contentResolverCursor = try {
                contentResolver.query(
                    uri,
                    projection,
                    "package_name=?",
                    arrayOf(context.packageName),
                    null,
                )
            } catch (e: Exception) {
                // Third-party referral providers can throw arbitrary exceptions across the Binder.
                // Skip the misbehaving provider instead of crashing at startup.
                logger.error("$classVersion - Failed to query provider $authority", e)
                continue
            }

            contentResolverCursor?.use { cursor ->
                cursor.getProvider()?.let { return it }
            }
        }

        return null
    }

    private fun Cursor.getProvider(): String? {
        logger.info("$classVersion - Cursor available")
        while (moveToNext()) {
            val dataColumnIndex = getColumnIndex(ENCRYPTED_DATA_COLUMN)

            // Check if columns exist
            if (dataColumnIndex == -1) {
                break
            }

            val data = getString(dataColumnIndex) ?: break
            try {
                val jsonObject = JSONObject(data)
                val provider = jsonObject.getString("provider")
                logger.info("$classVersion - Provider found: $provider")
                return provider
            } catch (e: JSONException) {
                logger.info("$classVersion - JSON expection: $e")
                break
            }
        }
        return null
    }
}
