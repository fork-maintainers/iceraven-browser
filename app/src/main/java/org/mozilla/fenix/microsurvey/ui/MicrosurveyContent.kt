/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.microsurvey.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mozilla.components.compose.base.annotation.FlexibleWindowPreview
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.list.RadioButtonListItem
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.PreviewThemeProvider
import org.mozilla.fenix.theme.Theme
import mozilla.components.ui.icons.R as iconsR

/**
 * The microsurvey content UI to hold question and answer data.
 *
 * @param question The survey question text.
 * @param answers The survey answer text options available for the question.
 * @param icon The survey icon, this will represent the feature the survey is for.
 * @param iconTint The optional tint color to apply to the survey [icon].
 * @param backgroundColor The view background color.
 * @param selectedAnswer The current selected answer. Will be null until user selects an option.
 * @param maxLabelLines The maximum number of lines allowed for each answer text layout to prevent truncation.
 * @param onSelectionChange An event that updates the [selectedAnswer].
 */
@Composable
fun MicrosurveyContent(
    question: String,
    answers: List<String>,
    @DrawableRes icon: Int = iconsR.drawable.mozac_ic_print_24,
    iconTint: Color? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceBright,
    selectedAnswer: String? = null,
    maxLabelLines: Int = 2,
    onSelectionChange: (String) -> Unit,
) {
    Card(
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column {
            Header(icon, iconTint, question)

            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .selectableGroup()
                    .nestedScroll(rememberNestedScrollInteropConnection())
                    .verticalScroll(rememberScrollState()),
            ) {
                answers.forEach {
                    RadioButtonListItem(
                        label = it,
                        selected = selectedAnswer == it,
                        maxLabelLines = maxLabelLines,
                        onClick = {
                            onSelectionChange.invoke(it)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(icon: Int, iconTint: Color?, question: String) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(id = R.string.microsurvey_feature_icon_content_description),
            modifier = Modifier.size(24.dp),
            tint = iconTint ?: Color.Unspecified,
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = question,
            style = FirefoxTheme.typography.headline7,
        )
    }
}

@FlexibleWindowPreview
@Composable
private fun MicrosurveyContentPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme) {
        MicrosurveyContent(
            question = "How satisfied are you with printing in Firefox?",
            icon = iconsR.drawable.mozac_ic_print_24,
            answers = listOf(
                stringResource(id = R.string.likert_scale_option_1),
                stringResource(id = R.string.likert_scale_option_2),
                stringResource(id = R.string.likert_scale_option_3),
                stringResource(id = R.string.likert_scale_option_4),
                stringResource(id = R.string.likert_scale_option_5),
            ),
            onSelectionChange = {},
        )
    }
}
