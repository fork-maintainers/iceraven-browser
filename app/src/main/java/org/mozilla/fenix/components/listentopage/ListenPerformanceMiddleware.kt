/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.listentopage

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.concept.base.profiler.Profiler
import mozilla.components.feature.listentopage.ListenAction
import mozilla.components.feature.listentopage.ListenState
import mozilla.components.feature.listentopage.PlaybackPhase
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.Store
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.fenix.GleanMetrics.ListenToPage

/**
 * [Middleware] that records profiler markers for how long the listen to page feature takes to respond to the user.
 *
 * @param profiler Records the markers.
 * @param scope The scope markers are added on. It has to be the main thread, see bug 1960730.
 * @param elapsedMs Reads a monotonic clock, so that durations can be measured under a test's virtual time.
 */
class ListenPerformanceMiddleware(
    private val profiler: Profiler,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val elapsedMs: () -> Long = SystemClock::elapsedRealtime,
) : Middleware<ListenState, ListenAction> {

    private val logger = Logger("ListenPerformance")

    private val spans = mutableMapOf<String, Span>()

    override fun invoke(
        store: Store<ListenState, ListenAction>,
        next: (ListenAction) -> Unit,
        action: ListenAction,
    ) {
        next(action)

        when (action) {
            // Reading starts as soon as the reader asks to listen, so that is when the wait for audio starts.
            is ListenAction.Session.ListenRequested -> {
                cancelAll(action)
                start(
                    markerName = TIME_TO_FIRST_AUDIO,
                    onEnd = { durationMs -> ListenToPage.timeToFirstAudio.accumulateSamples(listOf(durationMs)) },
                )
            }

            is ListenAction.Session.StopRequested -> cancelAll(action)

            ListenAction.Content.ContentUnavailable,
            ListenAction.Voices.NoOfflineVoicesAvailable,
            ListenAction.Synthesis.SynthesisFailed,
            ListenAction.Playback.PlaybackFailed -> failAll(action)

            is ListenAction.Playback.PlaybackStarted -> end(TIME_TO_FIRST_AUDIO)

            is ListenAction.Playback.StateChangeObserved ->
                if (action.playbackState.phase == PlaybackPhase.Playing) {
                    end(TIME_TO_FIRST_AUDIO)
                }

            is ListenAction.Content.ContentReady,
            is ListenAction.Voices.VoiceSelected,
            is ListenAction.Voices.AvailableVoicesLoaded,
            ListenAction.Controls.PlayPauseClicked,
            ListenAction.Controls.RewindClicked,
            ListenAction.Controls.ForwardClicked,
            ListenAction.Controls.VoicesClicked,
            ListenAction.Controls.VoicesDismissed,
            ListenAction.Controls.PlaybackSpeedClicked,
            ListenAction.Controls.PlaybackSpeedDismissed,
            is ListenAction.Controls.PlaybackSpeedSelected,
            is ListenAction.Playback.ArticleProgressReverted,
            is ListenAction.Playback.SeekRequested,
            ListenAction.Playback.PlaybackWaiting,
            ListenAction.Playback.PlaybackEnded,
            is ListenAction.ModeChanged,
            ListenAction.ErrorDismissed -> Unit

            // The session's own actions, which are internal to it.
            else -> Unit
        }
    }

    private fun start(markerName: String, text: String? = null, onEnd: ((durationMs: Long) -> Unit)? = null) {
        if (markerName in spans) return
        val profilerStartTime = profiler.takeIf { it.isProfilerActive() }?.getProfilerTime()
        spans[markerName] = Span(elapsedMs(), profilerStartTime, text, onEnd)
        logger.debug("$markerName started ($text)")
    }

    private fun end(markerName: String) {
        val span = spans.remove(markerName) ?: return
        val durationMs = record(markerName, span, span.text)
        span.onEnd?.invoke(durationMs)
    }

    private fun failAll(action: ListenAction) {
        val reason = "failed=${action::class.simpleName}"
        spans.forEach { (markerName, span) ->
            record("$markerName$FAILED_SUFFIX", span, listOfNotNull(reason, span.text).joinToString())
        }
        spans.clear()
    }

    private fun record(markerName: String, span: Span, text: String?): Long {
        val durationMs = elapsedMs() - span.startTimeMs
        logger.debug("$markerName took ${durationMs}ms ($text)")

        val profilerStartTime = span.profilerStartTime
        val profilerEndTime = profiler.getProfilerTime()
        if (profilerStartTime != null && profilerEndTime != null) {
            scope.launch {
                profiler.addMarker(markerName, profilerStartTime, profilerEndTime, text)
            }
        }
        return durationMs
    }

    private fun cancelAll(action: ListenAction) {
        spans.keys.forEach { logger.debug("$it cancelled by ${action::class.simpleName}") }
        spans.clear()
    }

    private class Span(
        val startTimeMs: Long,
        val profilerStartTime: Double?,
        val text: String?,
        val onEnd: ((durationMs: Long) -> Unit)?,
    )

    private companion object {
        const val TIME_TO_FIRST_AUDIO = "ListenToPage.TimeToFirstAudio"
        const val FAILED_SUFFIX = ".Failed"
    }
}
