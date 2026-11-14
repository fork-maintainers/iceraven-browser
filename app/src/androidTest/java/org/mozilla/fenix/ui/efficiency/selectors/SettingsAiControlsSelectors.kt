/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import mozilla.components.feature.summarize.R as summarizeR
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorContainer
import org.mozilla.fenix.ui.efficiency.helpers.SelectorGroup
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy
import org.mozilla.fenix.ui.efficiency.helpers.SwipeDirection

object SettingsAiControlsSelectors : SelectorContainer {
    enum class Group : SelectorGroup {
        AI_CONTROLS_VIEW
    }

    val TOOLBAR_TITLE =
        navigationToolbarTitle(
            title = getStringResource(R.string.preferences_ai_controls),
            description = "the AI controls toolbar title",
            groups = setOf(Group.AI_CONTROLS_VIEW),
        )

    val CHOICE_BANNER_HEADLINE =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(R.string.ai_controls_banner_headline),
            description = "the AI choice banner headline",
            groups = setOf(Group.AI_CONTROLS_VIEW),
        )

    // Links (banner supporting text, "See what...", "More ... settings") expose their label as a content
    // description suffixed with "Links available", so match a content-description substring. The value is
    // built the same way the banner composes it: the supporting text formatted with the "Learn more" link.
    val CHOICE_BANNER_SUPPORTING_TEXT =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION_SUBSTRING,
            value =
                getStringResource(
                    R.string.ai_controls_banner_supporting_text_2,
                    getStringResource(R.string.ai_controls_learn_more),
                ),
            description = "the AI choice banner supporting text and Learn more link",
        )

    // SwitchListItem rows: the toggle state lives on the merged row (selected = checked), so presence and
    // on/off are both asserted through the MERGED selector (see TranslationsSelectors for the rationale).
    val BLOCK_AI_TOGGLE =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT_MERGED,
            value = getStringResource(R.string.ai_controls_block_ai_title),
            description = "the Block AI enhancements toggle",
        )

    // Shown when turning Block AI enhancements on.
    val BLOCK_AI_DIALOG_TITLE =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(R.string.ai_controls_block_dialog_title),
            description = "the Block AI enhancements dialog title",
        )

    val BLOCK_AI_DIALOG_BLOCK_BUTTON =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(R.string.ai_controls_block_dialog_block),
            description = "the Block button in the Block AI enhancements dialog",
        )

    val BLOCK_AI_DESCRIPTION =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(R.string.ai_controls_block_ai_description),
            description = "the Block AI enhancements description",
        )

    // Warning banner shown while AI enhancements are blocked.
    val BLOCKED_INFO_BANNER =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT_SUBSTRING,
            value = getStringResource(R.string.ai_controls_blocked_info_banner),
            description = "the blocked AI enhancements info banner",
        )

    val SEE_WHATS_INCLUDED_LINK =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION_SUBSTRING,
            value = getStringResource(R.string.ai_controls_see_whats_included),
            description = "the See what is and isn't included link",
            groups = setOf(Group.AI_CONTROLS_VIEW),
            scrollDirection = SwipeDirection.UP,
        )

    val AI_POWERED_FEATURES_HEADER =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(R.string.ai_controls_ai_powered_features),
            description = "the AI-powered features section header",
        )

    val TRANSLATIONS_TOGGLE =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT_MERGED,
            value = getStringResource(R.string.ai_controls_translations_title),
            description = "the Translations toggle",
        )

    val TRANSLATIONS_DESCRIPTION =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(R.string.ai_controls_translations_description),
            description = "the Translations description",
        )

    val MORE_TRANSLATIONS_SETTINGS_LINK =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION_SUBSTRING,
            value = getStringResource(R.string.ai_controls_more_translations_settings),
            description = "the More translations settings link",
        )

    val PAGE_SUMMARIES_TOGGLE =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT_MERGED,
            value = getStringResource(summarizeR.string.mozac_ai_controls_page_summary_title),
            description = "the Page summaries toggle",
        )

    val PAGE_SUMMARIES_DESCRIPTION =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(summarizeR.string.mozac_ai_controls_page_summary_description),
            description = "the Page summaries description",
        )

    val MORE_PAGE_SUMMARY_SETTINGS_LINK =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION_SUBSTRING,
            value = getStringResource(R.string.ai_controls_more_page_summary_settings),
            description = "the More page summary settings link",
        )

    val VOICE_SEARCH_TOGGLE =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT_MERGED,
            value = getStringResource(R.string.ai_controls_voice_search_title),
            description = "the Voice search toggle",
        )

    val VOICE_SEARCH_DESCRIPTION =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TEXT,
            value = getStringResource(R.string.ai_controls_voice_search_description),
            description = "the Voice search description",
        )
}
