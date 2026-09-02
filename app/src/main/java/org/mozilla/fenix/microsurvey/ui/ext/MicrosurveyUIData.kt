/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.microsurvey.ui.ext

import androidx.annotation.DrawableRes
import mozilla.components.service.nimbus.messaging.Message
import mozilla.components.service.nimbus.messaging.MicrosurveyAnswer
import mozilla.components.service.nimbus.messaging.MicrosurveyConfig
import mozilla.components.service.nimbus.messaging.MicrosurveyOrdering
import mozilla.components.ui.icons.R as iconsR

/**
 * UI model for [MicrosurveyConfig].
 *
 * @property id Unique identifier of the microsurvey.
 * @property promptTitle The title to display on the 'prompt'.
 * @property icon The survey icon.
 * @property question The survey question.
 * @property answers The list of survey answers in randomized or pinned randomized order.
 * @property utmContent Optional utm content parameter to specify the surveyed feature in a URL.
 * @property maxNumberLines The maximum number of lines allowed for the answer text layout.
 */
data class MicrosurveyUIData(
    val id: String,
    val promptTitle: String,
    @param:DrawableRes val icon: Int,
    val question: String,
    val answers: List<String>,
    val utmContent: String? = null,
    val maxNumberLines: Int,
)

/**
 * @returns a [MicrosurveyUIData] derived from the given [Message].
 * [MicrosurveyUIData.answers] are in randomized or pinned randomized order,
 * specified by [MicrosurveyConfig.answerOrderingType].
 */
fun Message.toMicrosurveyUIData() = if (hasValidMicrosurveyConfig()) {
    MicrosurveyUIData(
        id = id,
        // title null checked in hasValidMicrosurveyConfig
        promptTitle = title!!,
        // microsurvey null checked in hasValidMicrosurveyConfig
        icon = microsurvey!!.icon?.resourceId ?: iconsR.drawable.mozac_ic_lightbulb_24,
        question = text,
        // microsurvey null checked in hasValidMicrosurveyConfig
        answers = microsurvey!!.toOrderedAnswers(),
        utmContent = microsurvey?.utmContent,
        maxNumberLines = microsurvey!!.maxNumberLines,
    )
} else {
    null
}

private fun Message.hasValidMicrosurveyConfig() =
    title != null && microsurvey != null && microsurvey!!.answers.isNotEmpty()

/**
 * @return a list of text answers derived from the given [MicrosurveyConfig.answers] in
 * randomized order.
 */
private fun MicrosurveyConfig.toRandomizedAnswers() = answers.shuffled().map { it.text }

/**
 * @return a list of text answers derived from the given [MicrosurveyConfig.answers], where
 * answers with a [MicrosurveyAnswer.ordering] greater than 0 are pinned to the top in ascending
 * order and the remaining answers are randomized. E.g. 1 will be the first/top item, 2 will be next,
 * followed by randomized 0s.
 */
private fun MicrosurveyConfig.toPinnedRandomizedAnswers(): List<String> {
    val (pinned, unpinned) = answers.partition { it.ordering > 0 }
    return (pinned.sortedBy { it.ordering } + unpinned.shuffled()).map { it.text }
}

private fun MicrosurveyConfig.toOrderedAnswers() = when (answerOrderingType) {
    MicrosurveyOrdering.PINNED_RANDOMIZED -> toPinnedRandomizedAnswers()
    MicrosurveyOrdering.RANDOMIZED -> toRandomizedAnswers()
}
