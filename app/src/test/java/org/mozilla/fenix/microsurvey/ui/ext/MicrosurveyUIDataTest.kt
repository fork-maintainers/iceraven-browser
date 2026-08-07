/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.microsurvey.ui.ext

import mozilla.components.service.nimbus.messaging.Message
import mozilla.components.service.nimbus.messaging.MessageData
import mozilla.components.service.nimbus.messaging.MicrosurveyAnswer
import mozilla.components.service.nimbus.messaging.MicrosurveyConfig
import mozilla.components.service.nimbus.messaging.MicrosurveyOrdering
import mozilla.components.service.nimbus.messaging.StyleData
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.experiments.nimbus.NullVariables
import org.mozilla.experiments.nimbus.StringHolder
import org.mozilla.fenix.R
import org.robolectric.RobolectricTestRunner
import mozilla.components.ui.icons.R as iconsR

@RunWith(RobolectricTestRunner::class)
class MicrosurveyUIDataTest {
    private val answer1 = MicrosurveyAnswer(text = StringHolder(null, "a"), ordering = 0)
    private val answer2 = MicrosurveyAnswer(text = StringHolder(null, "b"), ordering = 1)
    private val answer3 = MicrosurveyAnswer(text = StringHolder(null, "c"), ordering = 2)
    private val answer4 = MicrosurveyAnswer(text = StringHolder(null, "d"), ordering = 3)
    private val answers = listOf(answer1, answer2, answer3, answer4)

    private val answersText = listOf("a", "b", "c", "d")

    @Before
    fun setup() {
        NullVariables.instance.setContext(testContext)
    }

    @Test
    fun `WHEN message has a valid microsurvey configuration THEN toMicrosurveyUIData returns the UI data from the raw data`() {
        val microsurveyConfig = MicrosurveyConfig(
            utmContent = "test utm content",
            icon = R.drawable.ic_print,
            answers = answers,
        )
        val messageData = MessageData(
            title = StringHolder(null, "test title"),
            text = StringHolder(null, "test question"),
            microsurveyConfig = microsurveyConfig,
        )
        val message = createTestMessage(messageData)

        val expected = MicrosurveyUIData(
            id = "test ID",
            promptTitle = "test title",
            icon = R.drawable.ic_print,
            question = "test question",
            answers = answersText,
            utmContent = "test utm content",
            maxNumberLines = 2,
        )
        val actual = message.toMicrosurveyUIData()
        assertEquals(expected.copy(answers = emptyList()), actual?.copy(answers = emptyList()))
        assertEquals(answersText.sorted(), actual?.answers?.sorted())
    }

    @Test
    fun `WHEN message has no title THEN toMicrosurveyUIData returns null`() {
        val microsurveyConfig = MicrosurveyConfig(
            utmContent = "test utm content",
            icon = R.drawable.ic_print,
            answers = answers,
        )
        val messageData = MessageData(
            text = StringHolder(null, "test question"),
            microsurveyConfig = microsurveyConfig,
        )
        val message = createTestMessage(messageData)

        val actual = message.toMicrosurveyUIData()
        assertEquals(null, actual)
    }

    @Test
    fun `WHEN message has no microsurvey THEN toMicrosurveyUIData returns null`() {
        val messageData = MessageData(
            title = StringHolder(null, "test title"),
            text = StringHolder(null, "test question"),
        )
        val message = createTestMessage(messageData)

        val actual = message.toMicrosurveyUIData()
        assertEquals(null, actual)
    }

    @Test
    fun `WHEN microsurvey answers is empty THEN toMicrosurveyUIData returns null`() {
        val microsurveyConfig = MicrosurveyConfig(
            utmContent = "test utm content",
            icon = R.drawable.ic_print,
            answers = emptyList(),
        )
        val messageData = MessageData(
            title = StringHolder(null, "test title"),
            text = StringHolder(null, "test question"),
            microsurveyConfig = microsurveyConfig,
        )
        val message = createTestMessage(messageData)

        val actual = message.toMicrosurveyUIData()
        assertEquals(null, actual)
    }

    @Test
    fun `WHEN microsurvey has no icon THEN toMicrosurveyUIData returns the UI data from the raw data with the default icon`() {
        val microsurveyConfig = MicrosurveyConfig(
            utmContent = "test utm content",
            answers = answers,
        )
        val messageData = MessageData(
            title = StringHolder(null, "test title"),
            text = StringHolder(null, "test question"),
            microsurveyConfig = microsurveyConfig,
        )
        val message = createTestMessage(messageData)

        val expected = MicrosurveyUIData(
            id = "test ID",
            promptTitle = "test title",
            icon = iconsR.drawable.mozac_ic_lightbulb_24,
            question = "test question",
            answers = answersText,
            utmContent = "test utm content",
            maxNumberLines = 2,
        )
        val actual = message.toMicrosurveyUIData()
        assertEquals(expected.copy(answers = emptyList()), actual?.copy(answers = emptyList()))
        assertEquals(answersText.sorted(), actual?.answers?.sorted())
    }

    @Test
    fun `WHEN microsurvey has no utm content THEN toMicrosurveyUIData returns the UI data from the raw data`() {
        val microsurveyConfig = MicrosurveyConfig(
            icon = R.drawable.ic_print,
            answers = answers,
        )
        val messageData = MessageData(
            title = StringHolder(null, "test title"),
            text = StringHolder(null, "test question"),
            microsurveyConfig = microsurveyConfig,
        )
        val message = createTestMessage(messageData)

        val expected = MicrosurveyUIData(
            id = "test ID",
            promptTitle = "test title",
            icon = R.drawable.ic_print,
            question = "test question",
            answers = answersText,
            utmContent = null,
            maxNumberLines = 2,
        )
        val actual = message.toMicrosurveyUIData()
        assertEquals(expected.copy(answers = emptyList()), actual?.copy(answers = emptyList()))
        assertEquals(answersText.sorted(), actual?.answers?.sorted())
    }

    @Test
    fun `WHEN microsurvey has pinned randomized ordering THEN answers with ordering greater than 0 are sorted in ascending order before shuffled answers`() {
        val answer1 = MicrosurveyAnswer(text = StringHolder(null, "b"), ordering = 2)
        val answer2 = MicrosurveyAnswer(text = StringHolder(null, "c"), ordering = 3)
        val answer3 = MicrosurveyAnswer(text = StringHolder(null, "a"), ordering = 1)
        val answer4 = MicrosurveyAnswer(text = StringHolder(null, "d"), ordering = 0)
        val answer5 = MicrosurveyAnswer(text = StringHolder(null, "e"), ordering = 0)
        val answers = listOf(answer1, answer2, answer3, answer4, answer5)

        val microsurveyConfig = MicrosurveyConfig(
            icon = R.drawable.ic_print,
            answers = answers,
            answerOrderingType = MicrosurveyOrdering.PINNED_RANDOMIZED,
        )
        val messageData = MessageData(
            title = StringHolder(null, "test title"),
            text = StringHolder(null, "test question"),
            microsurveyConfig = microsurveyConfig,
        )
        val message = createTestMessage(messageData)

        val actual = message.toMicrosurveyUIData()

        assertEquals(5, actual?.answers?.size)
        assertEquals(listOf("a", "b", "c"), actual?.answers?.subList(0, 3))
        assertEquals(setOf("d", "e"), actual?.answers?.subList(3, 5)?.toSet())
    }

    private fun createTestMessage(messageData: MessageData) = Message(
        id = "test ID",
        data = messageData,
        action = "action",
        style = StyleData(),
        triggerIfAll = emptyList(),
        excludeIfAny = emptyList(),
        metadata = Message.Metadata(id = "test ID"),
    )
}
