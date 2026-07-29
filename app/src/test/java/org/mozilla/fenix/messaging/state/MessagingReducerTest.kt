/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.messaging.state

import io.mockk.mockk
import mozilla.components.service.nimbus.messaging.Message
import mozilla.components.service.nimbus.messaging.MessageData
import mozilla.components.service.nimbus.messaging.MessageSurfaceId
import mozilla.components.service.nimbus.messaging.MicrosurveyAnswer
import mozilla.components.service.nimbus.messaging.MicrosurveyConfig
import mozilla.components.service.nimbus.messaging.StyleData
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.experiments.nimbus.NullVariables
import org.mozilla.experiments.nimbus.StringHolder
import org.mozilla.fenix.components.appstate.AppAction.MessagingAction.ConsumeMessageToShow
import org.mozilla.fenix.components.appstate.AppAction.MessagingAction.UpdateMessageToShow
import org.mozilla.fenix.components.appstate.AppAction.MessagingAction.UpdateMessages
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.components.appstate.AppStoreReducer
import org.mozilla.fenix.messaging.FenixMessageSurfaceId
import org.mozilla.fenix.messaging.MessagingState
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class MessagingReducerTest {

    @Before
    fun setup() {
        NullVariables.instance.setContext(testContext)
    }

    @Test
    fun `GIVEN a new value for messageToShow WHEN UpdateMessageToShow is called THEN update the current value`() {
        val initialState = AppState(
            messaging = MessagingState(
                messageToShow = mapOf(),
            ),
        )

        val m = createMessage("message1")

        var updatedState = MessagingReducer.reduce(
            initialState,
            UpdateMessageToShow(m),
        )

        assertNotNull(updatedState.messaging.messageToShow[m.surface])

        updatedState = AppStoreReducer.reduce(updatedState, ConsumeMessageToShow(m.surface))

        assertNull(updatedState.messaging.messageToShow[m.surface])
    }

    private fun createMessage(id: String, action: String = "action-1", surface: MessageSurfaceId = FenixMessageSurfaceId.HOMESCREEN): Message =
        Message(
            id = id,
            data = MessageData(surface = surface),
            action = action,
            style = StyleData(),
            triggerIfAll = listOf(),
            metadata = Message.Metadata(id = id),
        )

    @Test
    fun `GIVEN a new value for messages WHEN UpdateMessages is called THEN update the current value`() {
        val initialState = AppState(
            messaging = MessagingState(
                messages = emptyList(),
            ),
        )

        var updatedState = MessagingReducer.reduce(
            initialState,
            UpdateMessages(listOf(mockk())),
        )

        assertFalse(updatedState.messaging.messages.isEmpty())

        updatedState = AppStoreReducer.reduce(updatedState, UpdateMessages(emptyList()))

        assertTrue(updatedState.messaging.messages.isEmpty())
    }

    @Test
    fun `GIVEN a microsurvey message WHEN UpdateMessageToShow is called THEN AppState#microsurvey#current is derived from it`() {
        val message = createMicrosurveyMessage(id = "microsurvey1")

        val updatedState = MessagingReducer.reduce(AppState(), UpdateMessageToShow(message))

        assertNotNull(updatedState.microsurvey.current)
        assertTrue(updatedState.microsurvey.current.id == "microsurvey1")
    }

    @Test
    fun `GIVEN a non-microsurvey message WHEN UpdateMessageToShow is called THEN AppState#microsurvey is unchanged`() {
        val message = createMessage("homescreen1")
        val initialState = AppState()

        val updatedState = MessagingReducer.reduce(initialState, UpdateMessageToShow(message))

        assertSame(initialState.microsurvey, updatedState.microsurvey)
    }

    @Test
    fun `GIVEN the same microsurvey is re-evaluated WHEN UpdateMessageToShow is called again THEN AppState#microsurvey is not replaced`() {
        val message = createMicrosurveyMessage(id = "microsurvey1")
        val firstUpdate = MessagingReducer.reduce(AppState(), UpdateMessageToShow(message))

        val secondUpdate = MessagingReducer.reduce(firstUpdate, UpdateMessageToShow(message))

        assertSame(firstUpdate.microsurvey, secondUpdate.microsurvey)
    }

    @Test
    fun `GIVEN a microsurvey is shown WHEN ConsumeMessageToShow is called for MICROSURVEY THEN AppState#microsurvey is cleared`() {
        val message = createMicrosurveyMessage(id = "microsurvey1")
        val shownState = MessagingReducer.reduce(AppState(), UpdateMessageToShow(message))

        val updatedState = MessagingReducer.reduce(
            shownState,
            ConsumeMessageToShow(FenixMessageSurfaceId.MICROSURVEY),
        )

        assertNull(updatedState.microsurvey.current)
    }

    @Test
    fun `GIVEN a microsurvey message with an invalid config WHEN UpdateMessageToShow is called THEN messageToShow is updated but AppState#microsurvey#current is not`() {
        val message = createMicrosurveyMessage(id = "microsurvey2", hasValidConfig = false)
        val initialState = AppState()

        val updatedState = MessagingReducer.reduce(initialState, UpdateMessageToShow(message))

        assertSame(message, updatedState.messaging.messageToShow[FenixMessageSurfaceId.MICROSURVEY])
        assertNull(updatedState.microsurvey.current)
    }

    private fun createMicrosurveyMessage(id: String, hasValidConfig: Boolean = true) = Message(
        id = id,
        data = MessageData(
            surface = FenixMessageSurfaceId.MICROSURVEY,
            title = StringHolder(null, "test title"),
            text = StringHolder(null, "test question"),
            microsurveyConfig = MicrosurveyConfig(
                answers = if (hasValidConfig) {
                    listOf(MicrosurveyAnswer(text = StringHolder(null, "a"), ordering = 0))
                } else {
                    emptyList()
                },
            ),
        ),
        action = "action-1",
        style = StyleData(),
        triggerIfAll = listOf(),
        metadata = Message.Metadata(id = id),
    )
}
