/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.widget

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import mozilla.components.browser.state.state.selectedOrDefaultSearchEngine
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.IntentReceiverActivity
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.isGoogleSearchEngine
import org.mozilla.fenix.home.intent.StartSearchIntentProcessor
import org.mozilla.fenix.widget.VoiceSearchActivity.Companion.SPEECH_PROCESSING

/** Builds the intents launched by the device home screen Firefox search widgets. */
object SearchWidgetIntents {

    /** Intent that opens the browser and starts a new text search. */
    fun textSearch(context: Context): Intent =
        Intent(context, IntentReceiverActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(HomeActivity.OPEN_TO_SEARCH, StartSearchIntentProcessor.SEARCH_WIDGET)
        }

    /** Intent that starts a new voice search, or null if voice search is disabled or unavailable. */
    fun maybeVoiceSearch(context: Context): Intent? {
        if (!context.components.settings.shouldShowVoiceSearch) {
            return null
        }

        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).resolveActivity(context.packageManager) ?: return null

        return Intent(context, VoiceSearchActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(SPEECH_PROCESSING, true)
        }
    }

    /**
     * Intent that starts a Google Lens image search, or null when the Google Lens integration is disabled or the user's
     * selected search engine is not Google.
     */
    fun maybeLensSearch(context: Context): Intent? {
        val settings = context.components.settings
        if (!settings.googleLensIntegrationEnabled || !settings.googleLensIntegrationUserEnabled) {
            return null
        }

        val selectedSearchEngine =
            context.components.core.store.state.search.selectedOrDefaultSearchEngine(private = false)
        if (!selectedSearchEngine.isGoogleSearchEngine()) {
            return null
        }

        return Intent(context, LensSearchActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    }
}
