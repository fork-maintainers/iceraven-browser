/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.reviewprompt

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class CustomReviewPromptNavigationMiddlewareTest {

    val store = CustomReviewPromptStore(CustomReviewPromptState.PrePrompt)

    @Test
    fun `WHEN rate button is clicked THEN play store prompt is opened AND prompt is dismissed`() = runTest {
        val middleware = CustomReviewPromptNavigationMiddleware(this)

        val navigationEvents = mutableListOf<CustomReviewPromptNavigationEvent>()
        backgroundScope.launch {
            store.navigationEvents.toList(navigationEvents)
        }
        testScheduler.runCurrent()

        middleware.invoke(
            store = store,
            next = {},
            action = CustomReviewPromptAction.RateButtonClicked,
        )
        testScheduler.runCurrent()

        assertEquals(
            listOf(
                CustomReviewPromptNavigationEvent.OpenPlayStoreReviewPrompt,
                CustomReviewPromptNavigationEvent.Dismiss,
            ),
            navigationEvents,
        )
    }

    @Test
    fun `WHEN feedback button is clicked THEN sumo forum is opened in a new tab AND prompt is dismissed`() = runTest {
        val middleware = CustomReviewPromptNavigationMiddleware(this)

        val navigationEvents = mutableListOf<CustomReviewPromptNavigationEvent>()
        backgroundScope.launch {
            store.navigationEvents.toList(navigationEvents)
        }
        testScheduler.runCurrent()

        middleware.invoke(
            store = store,
            next = {},
            action = CustomReviewPromptAction.LeaveFeedbackButtonClicked,
        )
        testScheduler.runCurrent()

        assertEquals(
            listOf(
                CustomReviewPromptNavigationEvent.OpenInNewTab("mzl.la/AndroidSupport"),
                CustomReviewPromptNavigationEvent.Dismiss,
            ),
            navigationEvents,
        )
    }

    @Test
    fun `WHEN other actions are dispatched THEN no navigation events are emitted`() = runTest {
        val middleware = CustomReviewPromptNavigationMiddleware(this)

        val navigationEvents = mutableListOf<CustomReviewPromptNavigationEvent>()
        backgroundScope.launch {
            store.navigationEvents.toList(navigationEvents)
        }
        testScheduler.runCurrent()

        middleware.invoke(
            store = store,
            next = {},
            action = CustomReviewPromptAction.PositivePrePromptButtonClicked,
        )
        middleware.invoke(
            store = store,
            next = {},
            action = CustomReviewPromptAction.NegativePrePromptButtonClicked,
        )
        middleware.invoke(
            store = store,
            next = {},
            action = CustomReviewPromptAction.Displayed,
        )
        middleware.invoke(
            store = store,
            next = {},
            action = CustomReviewPromptAction.Dismissed,
        )
        testScheduler.runCurrent()

        assertTrue(navigationEvents.isEmpty())
    }
}
