/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

object NotificationSelectors {

    const val NOTIFICATION_STACK_SCROLLER_RES_ID = "com.android.systemui:id/notification_stack_scroller"

    // Proof that the shade is open, which is what page presence means for this page. Deliberately not
    // keyed on the app's own notification header (text == appName): that only exists once Firefox has
    // posted a notification and rendered it, so it made "is the shade open" lose a race against the
    // notification it was really waiting for. Tests assert their own notification by text instead.
    val NOTIFICATION_SHADE = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RAW_RES_ID,
        value = NOTIFICATION_STACK_SCROLLER_RES_ID,
        description = "System notification shade",
        groups = listOf("requiredForPage"),
    )

    // Everything below matches system UI rather than app UI, so the res-ids are raw "android:id/..."
    // values, not packageName-prefixed ones.

    // A notification's action button (PAUSE / RESUME / CANCEL on a download). Every action button on a
    // notification shares the res-id android:id/action0, so the label is what distinguishes them.
    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun NOTIFICATION_ACTION_BUTTON(action: String = "") = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RAW_RES_ID_CONTAINING_TEXT,
        value = "android:id/action0",
        secondaryValue = action,
        description = "Notification action button: $action",
        groups = listOf(),
    )

    // Matched by text anywhere in the shade — a notification's title or body.
    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun SYSTEM_NOTIFICATION(text: String = "") = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = text,
        description = "System notification containing text: $text",
        groups = listOf(),
    )

    // The collapsed notification's top line, used as the swipe handle to expand it.
    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun NOTIFICATION_TOP_LINE(text: String = "") = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RAW_RES_ID_CONTAINING_TEXT,
        value = "android:id/notification_top_line",
        secondaryValue = text,
        description = "Collapsed notification top line for: $text",
        groups = listOf(),
    )

    val all = listOf(
        NOTIFICATION_SHADE,
        NOTIFICATION_ACTION_BUTTON(),
        SYSTEM_NOTIFICATION(),
        NOTIFICATION_TOP_LINE(),
    )
}
