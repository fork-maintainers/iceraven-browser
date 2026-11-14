/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import mozilla.components.feature.summarize.R as summarizeR
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.ui.efficiency.helpers.PageReadinessProfiles
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorContainer
import org.mozilla.fenix.ui.efficiency.helpers.SelectorGroup
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

object SettingsPageSummariesSelectors : SelectorContainer {
    enum class Group : SelectorGroup {
        PAGE_SUMMARIES_VIEW
    }

    val PAGE_SUMMARIES_TOOLBAR_TITLE =
        navigationToolbarTitle(
            title = getStringResource(R.string.preferences_page_summaries),
            description = "the Page summaries toolbar title",
        )

    val SUMMARIZE_PAGES_OPTION =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(summarizeR.string.mozac_summarize_settings_summarize_pages),
            description = "the Summarize pages option",
            readiness = PageReadinessProfiles.READY_CONTENT,
        )

    // The "Summarize pages" row is a toggleable Row (Role.Switch); its ToggleableState lives on the merged
    // node, not on the inner text node COMPOSE_BY_TEXT resolves, so the on/off checks must use the merged
    // tree. Toggling the switch is a click on this same row.
    val SUMMARIZE_PAGES_TOGGLE =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT_MERGED,
            value = getStringResource(summarizeR.string.mozac_summarize_settings_summarize_pages),
            description = "the Summarize pages toggle",
        )

    val LEARN_MORE_LINK =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(summarizeR.string.mozac_summarize_settings_learn_more),
            description = "the Learn more link",
            groups = setOf(Group.PAGE_SUMMARIES_VIEW),
        )

    val GESTURES_SUB_HEADER =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(summarizeR.string.mozac_summarize_settings_gestures),
            description = "the Gestures sub header",
            groups = setOf(Group.PAGE_SUMMARIES_VIEW),
        )

    val SHAKE_TO_SUMMARIZE_OPTION =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(summarizeR.string.mozac_summarize_settings_shake_to_summarize),
            description = "the Shake to summarize option",
            groups = setOf(Group.PAGE_SUMMARIES_VIEW),
        )
}
