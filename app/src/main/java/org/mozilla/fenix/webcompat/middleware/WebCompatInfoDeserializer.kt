/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.webcompat.middleware

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import mozilla.components.support.base.log.logger.Logger

/**
 * Deserializer for [WebCompatInfoDto].
 */
class WebCompatInfoDeserializer(
    private val json: Json,
) {

    private val logger = Logger("WebCompatInfoDeserializer")

    internal fun decode(string: String): WebCompatInfoDto? {
        return try {
            json.decodeFromString<WebCompatInfoDto>(string)
        } catch (e: SerializationException) {
            logger.error("Missing field while retrieving web compat info. ${e.message}")
            null
        }
    }
}
