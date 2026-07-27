/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.termsofuse.store

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.utils.Settings
import org.mozilla.fenix.utils.Settings.Companion.FIVE_DAYS_MS
import org.mozilla.fenix.utils.Settings.Companion.THIRTY_SECONDS_MS
import org.robolectric.RobolectricTestRunner

private const val TIME_IN_MILLIS = 1759926358L

/**
 * This copies the default set in `nimbus.fml.yaml` `terms-of-use-prompt` `max-display-count`.
 */
private const val MAX_DISPLAY_COUNT = 2

@RunWith(RobolectricTestRunner::class)
class TermsOfUsePromptRepositoryTest {
    private lateinit var settings: Settings

    private lateinit var repository: DefaultTermsOfUsePromptRepository

    @Before
    fun setup() {
        settings = Settings(testContext)
        repository = DefaultTermsOfUsePromptRepository(settings, currentTimeMillisProvider = { TIME_IN_MILLIS })
    }

    @Test
    fun `WHEN all conditions satisfied THEN show the prompt`() {
        settings.hasAcceptedTermsOfService = false
        settings.isTermsOfUsePromptEnabled = true
        settings.termsOfUsePromptDisplayedCount = MAX_DISPLAY_COUNT - 1
        repository.isShowingPrompt = false

        assertFalse(settings.hasAcceptedTermsOfService)
        assertTrue(settings.isTermsOfUsePromptEnabled)
        assertEquals(MAX_DISPLAY_COUNT - 1, settings.termsOfUsePromptDisplayedCount)

        assertTrue(repository.canShowTermsOfUsePrompt())
    }

    @Test
    fun `WHEN the prompt is already showing THEN do not show the prompt`() {
        settings.hasAcceptedTermsOfService = false
        settings.isTermsOfUsePromptEnabled = true
        settings.termsOfUsePromptDisplayedCount = MAX_DISPLAY_COUNT - 1
        repository.isShowingPrompt = true

        assertFalse(settings.hasAcceptedTermsOfService)
        assertTrue(settings.isTermsOfUsePromptEnabled)
        assertEquals(MAX_DISPLAY_COUNT - 1, settings.termsOfUsePromptDisplayedCount)

        assertFalse(repository.canShowTermsOfUsePrompt())
    }

    @Test
    fun `WHEN user has already accepted the ToU THEN don't show the prompt`() {
        settings.hasAcceptedTermsOfService = true
        settings.isTermsOfUsePromptEnabled = true
        settings.termsOfUsePromptDisplayedCount = MAX_DISPLAY_COUNT - 1
        repository.isShowingPrompt = false

        assertTrue(settings.hasAcceptedTermsOfService)
        assertTrue(settings.isTermsOfUsePromptEnabled)
        assertEquals(MAX_DISPLAY_COUNT - 1, settings.termsOfUsePromptDisplayedCount)

        assertFalse(repository.canShowTermsOfUsePrompt())
    }

    @Test
    fun `WHEN the prompt feature is disabled THEN don't show the prompt`() {
        settings.hasAcceptedTermsOfService = false
        settings.isTermsOfUsePromptEnabled = false
        settings.termsOfUsePromptDisplayedCount = MAX_DISPLAY_COUNT - 1
        repository.isShowingPrompt = false

        assertFalse(settings.hasAcceptedTermsOfService)
        assertFalse(settings.isTermsOfUsePromptEnabled)
        assertEquals(MAX_DISPLAY_COUNT - 1, settings.termsOfUsePromptDisplayedCount)

        assertFalse(repository.canShowTermsOfUsePrompt())
    }

    @Test
    fun `WHEN user has already seen the maximum number of prompts THEN don't show the prompt`() {
        val excessiveDisplayCount = MAX_DISPLAY_COUNT + 1
        settings.hasAcceptedTermsOfService = false
        settings.isTermsOfUsePromptEnabled = true
        settings.termsOfUsePromptDisplayedCount = excessiveDisplayCount

        assertFalse(settings.hasAcceptedTermsOfService)
        assertTrue(settings.isTermsOfUsePromptEnabled)
        assertEquals(excessiveDisplayCount, settings.termsOfUsePromptDisplayedCount)

        assertFalse(repository.canShowTermsOfUsePrompt())
    }

    @Test
    fun `WHEN user was last prompted less than 5 days ago and postponed accepting ToU THEN userPostponedAndWithinCooldownPeriod returns true`() {
        val lastPromptTimeMs = TIME_IN_MILLIS - (FIVE_DAYS_MS - 1)
        settings.lastTermsOfUsePromptTimeInMillis = lastPromptTimeMs
        settings.hasPostponedAcceptingTermsOfUse = true

        assertEquals(lastPromptTimeMs, settings.lastTermsOfUsePromptTimeInMillis)
        assertTrue(settings.hasPostponedAcceptingTermsOfUse)

        assertTrue(repository.userPostponedAndWithinCooldownPeriod(TIME_IN_MILLIS))
    }

    @Test
    fun `WHEN user was last prompted 5 days ago and postponed accepting ToU THEN userPostponedAndWithinCooldownPeriod returns false`() {
        val lastPromptTimeMs = TIME_IN_MILLIS - FIVE_DAYS_MS
        settings.lastTermsOfUsePromptTimeInMillis = lastPromptTimeMs
        settings.hasPostponedAcceptingTermsOfUse = true

        assertEquals(lastPromptTimeMs, settings.lastTermsOfUsePromptTimeInMillis)
        assertTrue(settings.hasPostponedAcceptingTermsOfUse)

        assertFalse(repository.userPostponedAndWithinCooldownPeriod(TIME_IN_MILLIS))
    }

    @Test
    fun `WHEN user was last prompted over 5 days ago and postponed accepting ToU THEN userPostponedAndWithinCooldownPeriod returns false`() {
        val lastPromptTimeMs = TIME_IN_MILLIS - (FIVE_DAYS_MS + 1)
        settings.lastTermsOfUsePromptTimeInMillis = lastPromptTimeMs
        settings.hasPostponedAcceptingTermsOfUse = true

        assertEquals(lastPromptTimeMs, settings.lastTermsOfUsePromptTimeInMillis)
        assertTrue(settings.hasPostponedAcceptingTermsOfUse)

        assertFalse(repository.userPostponedAndWithinCooldownPeriod(TIME_IN_MILLIS))
    }

    @Test
    fun `GIVEN debug mode WHEN user was last prompted less than 30 seconds ago and postponed accepting ToU THEN userPostponedAndWithinCooldownPeriod returns true`() {
        val lastPromptTimeMs = TIME_IN_MILLIS - (THIRTY_SECONDS_MS - 1)
        settings.lastTermsOfUsePromptTimeInMillis = lastPromptTimeMs
        settings.isDebugTermsOfServiceTriggerTimeEnabled = true
        settings.hasPostponedAcceptingTermsOfUse = true

        assertEquals(lastPromptTimeMs, settings.lastTermsOfUsePromptTimeInMillis)
        assertTrue(settings.isDebugTermsOfServiceTriggerTimeEnabled)
        assertTrue(settings.hasPostponedAcceptingTermsOfUse)

        assertTrue(repository.userPostponedAndWithinCooldownPeriod(TIME_IN_MILLIS))
    }

    @Test
    fun `GIVEN debug mode WHEN user was last prompted 30 seconds ago and postponed accepting ToU THEN userPostponedAndWithinCooldownPeriod returns false`() {
        val lastPromptTimeMs = TIME_IN_MILLIS - THIRTY_SECONDS_MS
        settings.lastTermsOfUsePromptTimeInMillis = lastPromptTimeMs
        settings.isDebugTermsOfServiceTriggerTimeEnabled = true
        settings.hasPostponedAcceptingTermsOfUse = true

        assertEquals(lastPromptTimeMs, settings.lastTermsOfUsePromptTimeInMillis)
        assertTrue(settings.isDebugTermsOfServiceTriggerTimeEnabled)
        assertTrue(settings.hasPostponedAcceptingTermsOfUse)

        assertFalse(repository.userPostponedAndWithinCooldownPeriod(TIME_IN_MILLIS))
    }

    @Test
    fun `GIVEN debug mode WHEN user was last prompted over 30 seconds ago and postponed accepting ToU THEN userPostponedAndWithinCooldownPeriod returns false`() {
        val lastPromptTimeMs = TIME_IN_MILLIS - (THIRTY_SECONDS_MS + 1)
        settings.lastTermsOfUsePromptTimeInMillis = lastPromptTimeMs
        settings.isDebugTermsOfServiceTriggerTimeEnabled = true
        settings.hasPostponedAcceptingTermsOfUse = true

        assertEquals(lastPromptTimeMs, settings.lastTermsOfUsePromptTimeInMillis)
        assertTrue(settings.isDebugTermsOfServiceTriggerTimeEnabled)
        assertTrue(settings.hasPostponedAcceptingTermsOfUse)

        assertFalse(repository.userPostponedAndWithinCooldownPeriod(TIME_IN_MILLIS))
    }

    @Test
    fun `WHEN updateHasAcceptedTermsOfUsePreference is called THEN the related ToU preferences are updated`() {
        assertFalse(settings.hasAcceptedTermsOfService)
        assertEquals(0, settings.termsOfUseAcceptedVersion)
        assertEquals(0L, settings.termsOfUseAcceptedTimeInMillis)

        repository.updateHasAcceptedTermsOfUsePreference()

        assertTrue(settings.hasAcceptedTermsOfService)
        assertEquals(5, settings.termsOfUseAcceptedVersion)
        assertEquals(TIME_IN_MILLIS, settings.termsOfUseAcceptedTimeInMillis)
    }

    @Test
    fun `WHEN updateHasPostponedAcceptingTermsOfUsePreference is called THEN the preference is updated`() {
        assertFalse(settings.hasPostponedAcceptingTermsOfUse)
        repository.updateHasPostponedAcceptingTermsOfUsePreference()
        assertTrue(settings.hasPostponedAcceptingTermsOfUse)
    }

    @Test
    fun `WHEN updateLastTermsOfUsePromptTimeInMillis is called THEN the preference is updated`() {
        assertEquals(settings.lastTermsOfUsePromptTimeInMillis, 0)
        repository.updateLastTermsOfUsePromptTimeInMillis()
        assertTrue(settings.lastTermsOfUsePromptTimeInMillis > 0)
    }

    @Test
    fun `WHEN incrementTermsOfUsePromptDisplayedCount is called THEN the preference is updated`() {
        assertEquals(0, settings.termsOfUsePromptDisplayedCount)
        repository.incrementTermsOfUsePromptDisplayedCount()
        assertEquals(1, settings.termsOfUsePromptDisplayedCount)
    }
}
