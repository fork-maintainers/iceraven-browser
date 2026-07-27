/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.debugsettings.gleandebugtools

import android.content.Intent
import mozilla.telemetry.glean.Glean
import mozilla.telemetry.glean.debug.GleanDebugActivity
import org.mozilla.fenix.Config
import org.mozilla.fenix.utils.Settings

/**
 * A storage used to access the Glean APIs.
 */
interface GleanDebugToolsStorage {

    /**
     * Toggle whether to log pings to console.
     */
    fun setLogPings(enabled: Boolean)

    /**
     * Send a ping.
     *
     * @param pingType The type of ping to submit.
     * @param debugViewTag The debug tag to use for the ping.
     */
    fun sendPing(pingType: String, debugViewTag: String)

    /**
     * Clear any debug view tag persisted across app restarts.
     */
    fun clearPersistedDebugViewTag()
}

/**
 * The default storage, used by the [GleanDebugToolsMiddleware] to access the Glean APIs.
 *
 * @param settings Where the debug view tag is persisted.
 */
class DefaultGleanDebugToolsStorage(
    private val settings: Settings,
) : GleanDebugToolsStorage {
    override fun setLogPings(enabled: Boolean) {
        Glean.setLogPings(enabled)
    }

    override fun sendPing(pingType: String, debugViewTag: String) {
        Glean.setDebugViewTag(debugViewTag)
        Glean.submitPingByName(pingType)
    }

    override fun clearPersistedDebugViewTag() {
        if (settings.gleanDebugViewTag.isNotEmpty()) {
            settings.gleanDebugViewTag = ""
        }
    }

    /**
     * @see [DefaultGleanDebugToolsStorage].
     */
    companion object {
        /**
         * Intent extra for opting in to keep the debug view tag across app restarts.
         */
        const val PERSIST_DEBUG_VIEW_TAG_EXTRA = "persistDebugViewTag"

        /**
         * Get all the types of pings that can be submitted.
         */
        fun getPingTypes(): Set<String> {
            return Glean.getRegisteredPingNames()
        }

        /**
         * Persist the debug view tag from a Glean debug [intent] so it survives a restart, but only
         * when the caller opts in with [PERSIST_DEBUG_VIEW_TAG_EXTRA].
         * - A debug tag set without the flag clears the persisted tag.
         * - A launch with no debug tag (a normal restart, or the persist flag without a tag)
         * leaves any persisted tag as-is.
         *
         * Only runs on Nightly or Debug builds.
         *
         * @param intent The launch intent, with debug extras forwarded from GleanDebugActivity.
         * @param settings Where the debug view tag is persisted.
         */
        fun persistDebugViewTagIfRequested(intent: Intent, settings: Settings) {
            if (!Config.channel.isNightlyOrDebug) {
                return
            }
            val debugViewTag = intent.getStringExtra(GleanDebugActivity.TAG_DEBUG_VIEW_EXTRA_KEY).orEmpty()
            if (debugViewTag.isBlank()) {
                return
            }
            val shouldPersist = intent.getBooleanExtra(PERSIST_DEBUG_VIEW_TAG_EXTRA, false)
            // Persist the tag when specifically requested, otherwise clear any previously persisted tag.
            settings.gleanDebugViewTag = if (shouldPersist) debugViewTag else ""
        }

        /**
         * Re-apply the persisted debug view tag at startup so it survives a restart, since Glean
         * only keeps its debug view tag in memory.
         *
         * Only runs on Nightly or Debug builds.
         *
         * @param settings Where the debug view tag is persisted.
         */
        fun restorePersistedDebugViewTag(settings: Settings) {
            if (!Config.channel.isNightlyOrDebug) {
                return
            }
            settings.gleanDebugViewTag
                .takeIf { it.isNotEmpty() }
                ?.let { Glean.setDebugViewTag(it) }
        }
    }
}
