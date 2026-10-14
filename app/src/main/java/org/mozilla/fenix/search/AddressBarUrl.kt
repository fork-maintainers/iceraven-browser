/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.search

import mozilla.components.support.ktx.util.URLStringUtils

/** Whether [input] is [canonicalUrl], as stored or as the address bar shows it for editing. */
internal fun isCurrentUrl(input: String, canonicalUrl: String?): Boolean =
    canonicalUrl != null && (input == canonicalUrl || input == URLStringUtils.toLosslessEditableUrl(canonicalUrl))
