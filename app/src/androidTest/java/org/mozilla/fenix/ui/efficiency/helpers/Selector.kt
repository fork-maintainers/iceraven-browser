/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.helpers

import org.mozilla.fenix.R

data class Selector(
    val strategy: SelectorStrategy,
    val value: String,
    val secondaryValue: String? = null,
    val description: String,
    val groups: List<String> = emptyList(),
    val name: String? = null,
) {
    fun toResourceId(): Int {
        return try {
            val rClass = R.id::class.java
            val field = rClass.getField(value)
            field.getInt(null)
        } catch (e: Exception) {
            throw IllegalArgumentException("Could not resolve resource ID for selector value: '$value' using R.id", e)
        }
    }
}

enum class SelectorStrategy {
    /**
     * Supported strategies for locating UI elements.
     */
    COMPOSE_BY_CONTENT_DESCRIPTION,
    COMPOSE_BY_CONTENT_DESCRIPTION_SUBSTRING,
    COMPOSE_BY_TAG,
    COMPOSE_BY_TAG_AND_TEXT,
    COMPOSE_EDITABLE_BY_ANCESTOR_TAG,
    COMPOSE_ON_ALL_NODES_BY_TAG_ON_FIRST,
    COMPOSE_ON_ALL_NODES_BY_TAG_WITH_CHILD_TEXT_ON_FIRST,
    COMPOSE_BY_TEXT,
    COMPOSE_BY_TEXT_MERGED,
    COMPOSE_BY_TEXT_SUBSTRING,
    ESPRESSO_BY_ID,
    ESPRESSO_BY_ID_WITH_SIBLING_TEXT,
    ESPRESSO_BY_TEXT,
    ESPRESSO_BY_TEXT_WITH_SIBLING_TEXT,
    ESPRESSO_BY_CONTENT_DESC,
    ESPRESSO_BY_RES_NAME,
    UIAUTOMATOR2_BY_RES,
    UIAUTOMATOR2_BY_CLASS,
    UIAUTOMATOR2_BY_TEXT,
    UIAUTOMATOR_WITH_TEXT_CONTAINS,
    UIAUTOMATOR_WITH_RES_ID,
    UIAUTOMATOR_WITH_COMPOSE_TAG,
    UIAUTOMATOR_WITH_TEXT,
    UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
    UIAUTOMATOR_WITH_RES_ID_AND_TEXT,

    // Package-prefixed app res-id ("$packageName:id/<value>") whose text merely CONTAINS
    // secondaryValue. Mirrors the legacy MatcherHelper.itemWithResIdContainingText — needed when an
    // app view (e.g. an autofill suggestion row) carries a longer label than the substring we key on.
    UIAUTOMATOR_WITH_RES_ID_CONTAINING_TEXT,

    // Raw (un-prefixed) res-id matched together with an exact text. The res-id is a web DOM id
    // (GeckoView content), not an app resource — so it is used verbatim, like UIAUTOMATOR_WITH_COMPOSE_TAG,
    // but additionally asserts the element's text equals secondaryValue. Mirrors the legacy
    // MatcherHelper.itemWithResIdAndText applied to a web form field (e.g. verifying an autofilled value).
    UIAUTOMATOR_WITH_WEB_ID_AND_TEXT,
}
