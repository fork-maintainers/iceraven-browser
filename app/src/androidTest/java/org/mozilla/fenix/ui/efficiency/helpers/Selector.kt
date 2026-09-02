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

    // Compose testTag combined with a content-description that merely CONTAINS secondaryValue. Mirrors
    // the legacy onNode(hasTestTag(tag)).assert(hasContentDescription(text, substring = true)) — needed
    // for a tagged row whose identifying label lives in its content description (e.g. an installed
    // webExtension.item advertising the addon name).
    COMPOSE_BY_TAG_AND_CONTENT_DESCRIPTION_SUBSTRING,
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

    // UiObject2 content-description-contains. Prefer this over UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS
    // for a control whose reaction is SLOW (opens a dialog, starts a download): UiObject (the
    // UiSelector strategies) clicks via clickAndSync, which reports failure when no window update
    // lands inside its ~5.5s budget — a slow-but-successful click is then indistinguishable from a
    // missed one. UiObject2.click() just injects the gesture and lets the caller do the waiting.
    UIAUTOMATOR2_BY_DESCRIPTION_CONTAINS,
    UIAUTOMATOR2_BY_TEXT_CONTAINS,
    UIAUTOMATOR_WITH_TEXT_CONTAINS,
    UIAUTOMATOR_WITH_RES_ID,
    UIAUTOMATOR_WITH_COMPOSE_TAG,

    // A res-id matched verbatim (no packageName prefix) together with a content-description that merely
    // CONTAINS secondaryValue: UiSelector().resourceId(value).descriptionContains(secondaryValue).
    // Mirrors the legacy itemWithResIdAndDescription(resId, description) — needed for a row (e.g. the
    // collapsed Extensions menu row, whose res-id is the "mainMenu.extensions" Compose testTag surfaced
    // via testTagsAsResourceId) whose description advertises the installed addon name.
    UIAUTOMATOR_WITH_RES_ID_AND_DESCRIPTION_CONTAINS,
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

    // Raw (un-prefixed) res-id whose text merely CONTAINS secondaryValue. Needed for system-UI ids that
    // are not ours to prefix — e.g. a notification action button is "android:id/action0", so the
    // packageName-prefixing done by UIAUTOMATOR_WITH_RES_ID_CONTAINING_TEXT would look for
    // "org.mozilla.fenix.debug:id/action0" and never match. Mirrors the legacy
    // NotificationRobot.downloadSystemNotificationButton.
    UIAUTOMATOR_WITH_RAW_RES_ID_CONTAINING_TEXT,

    // Raw (un-prefixed) res-id on its own, for a system-UI container that carries no text of its own —
    // e.g. the notification shade's scroller. UIAUTOMATOR_WITH_RAW_RES_ID_CONTAINING_TEXT cannot stand in
    // for this: its text criterion is not optional at the UiSelector level.
    UIAUTOMATOR_WITH_RAW_RES_ID,
}
