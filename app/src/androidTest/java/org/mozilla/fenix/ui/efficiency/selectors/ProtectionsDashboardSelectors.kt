/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import mozilla.components.feature.protection.dashboard.R as protectionDashboardR
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.ui.efficiency.helpers.PageReadinessProfiles
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorContainer
import org.mozilla.fenix.ui.efficiency.helpers.SelectorGroup
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

// The blocked-trackers protections dashboard (ProtectionsDashboardFragment): a bottom-sheet dialog opened from the
// tabs-tray trackers-blocked pill. Compose content, Android-view scrim/sheet.
object ProtectionsDashboardSelectors : SelectorContainer {
    enum class Group : SelectorGroup {
        PROTECTIONS_DASHBOARD_VIEW
    }

    // Swiping this view down dismisses the sheet; the drag is View-layer BottomSheetBehavior, so swipe via UIAutomator.
    val DASHBOARD_SHEET =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR2_BY_RES,
            value = "design_bottom_sheet",
            description = "Protections dashboard bottom sheet container",
            groups = setOf(Group.PROTECTIONS_DASHBOARD_VIEW),
        )

    // Drag handle; present whenever the dashboard is shown. Identity anchor.
    val DASHBOARD_CLOSE_HANDLE =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
            value = getStringResource(R.string.tracker_protections_dashboard_close_handlebar_content_description),
            description = "Protections dashboard close/drag handle",
            groups = setOf(Group.PROTECTIONS_DASHBOARD_VIEW),
            readiness = PageReadinessProfiles.IDENTITY_ANCHOR,
        )

    // Header title; merged with the shield icon and weekly count into one semantics node, so match by substring.
    val TRACKERS_BLOCKED_THIS_WEEK_TITLE =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT_SUBSTRING,
            value =
                getStringResource(
                    protectionDashboardR.string.mozac_protections_dashboard_trackers_blocked_this_week_title
                ),
            description = "Protections dashboard 'Trackers blocked this week' header",
            groups = setOf(Group.PROTECTIONS_DASHBOARD_VIEW),
        )

    // All four category rows always render, each labelled with a count-prefixed plural; match the count-free substring.
    val CROSS_SITE_COOKIES_CATEGORY =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT_SUBSTRING,
            value = "cross-site tracking cookie",
            description = "Protections dashboard cross-site tracking cookies category row",
            groups = setOf(Group.PROTECTIONS_DASHBOARD_VIEW),
        )

    val SOCIAL_MEDIA_TRACKERS_CATEGORY =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT_SUBSTRING,
            value = "social media tracker",
            description = "Protections dashboard social media trackers category row",
            groups = setOf(Group.PROTECTIONS_DASHBOARD_VIEW),
        )

    val FINGERPRINTERS_CATEGORY =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT_SUBSTRING,
            value = "fingerprinter",
            description = "Protections dashboard fingerprinters category row",
            groups = setOf(Group.PROTECTIONS_DASHBOARD_VIEW),
        )

    val TRACKING_CONTENT_CATEGORY =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT_SUBSTRING,
            value = "tracking content",
            description = "Protections dashboard tracking content category row",
            groups = setOf(Group.PROTECTIONS_DASHBOARD_VIEW),
        )

    // Footer "<N> trackers blocked since <date>"; count and date vary, so match the invariant substring.
    val TOTAL_TRACKERS_BLOCKED_SINCE_FOOTER =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT_SUBSTRING,
            value = "blocked since",
            description = "Protections dashboard 'trackers blocked since <date>' footer",
            groups = setOf(Group.PROTECTIONS_DASHBOARD_VIEW),
        )
}
