/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.helpers.TestHelper.appName
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy
import mozilla.components.compose.base.R as composeBaseR

object WebCompatReporterSelectors {

    val URL_LABEL = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.webcompat_reporter_label_url),
        description = "Report broken site URL label",
        groups = listOf("requiredForPage", "reporterViewItems"),
    )

    val WHATS_BROKEN_LABEL = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.webcompat_reporter_label_whats_broken_3),
        description = "Report broken site \"What's not working?\" label",
        groups = listOf("reporterViewItems"),
    )

    // The description is a single content-description node built from the body copy, the inlined
    // "Learn more" link text, and the compose-base link affordance suffix — mirroring how the legacy
    // BrowserRobot composed it. Splitting it would not match the node.
    val DESCRIPTION = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = getStringResource(
            R.string.webcompat_reporter_description_3,
            appName,
            getStringResource(R.string.webcompat_reporter_learn_more),
        ) + " " + getStringResource(composeBaseR.string.mozac_compose_base_link_text_links_available),
        description = "Report broken site description",
        groups = listOf("reporterViewItems"),
    )

    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun REPORTED_SITE_URL(url: String = "") = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = url,
        description = "Report broken site reported URL: $url",
        groups = listOf(),
    )

    val all = listOf(
        URL_LABEL,
        WHATS_BROKEN_LABEL,
        DESCRIPTION,
        REPORTED_SITE_URL(),
    )
}
