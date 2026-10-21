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
import mozilla.components.feature.listentopage.facts.ListenToPageFacts
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.Store
import mozilla.components.support.base.Component
import mozilla.components.support.base.facts.Fact
import mozilla.components.support.base.facts.FactProcessor
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.fenix.GleanMetrics.ListenToPage

/**
 * [Middleware] that records profiler markers for how long the listen to page feature takes to respond to the user.
 *
 * It is also a [FactProcessor] for measurements the feature records that aren't surfaced as actions. The middleware has
 * to be registered for the facts to be processed correctly.
 *
 * @param profiler Records the markers.
 * @param scope The scope markers are added on. It has to be the main thread, see bug 1960730.
 * @param elapsedMs Gets current time.
 */
class ListenPerformanceMiddleware(
    private val profiler: Profiler,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val elapsedMs: () -> Long = SystemClock::elapsedRealtime,
) : Middleware<ListenState, ListenAction>, FactProcessor {

    private val logger = Logger("ListenPerformance")

    private val spans = mutableMapOf<String, Span>()

    // Track whether a seek or voice change has stopped audio until plays again. We want to avoid measuring this as part
    // of a gap between chunks - that should solely be synthesis time.
    private var seekOrVoiceChangePending = false

    override fun invoke(
        store: Store<ListenState, ListenAction>,
        next: (ListenAction) -> Unit,
        action: ListenAction,
    ) {
        val previous = store.state

        next(action)

        when (action) {
            // Reading starts as soon as the reader asks to listen, so that is when the wait for audio starts.
            is ListenAction.Session.ListenRequested -> {
                seekOrVoiceChangePending = false
                cancelAll(action)
                start(
                    markerName = TIME_TO_FIRST_AUDIO,
                    onEnd = { durationMs -> ListenToPage.timeToFirstAudio.accumulateSamples(listOf(durationMs)) },
                )
                start(EXTRACTION, onEnd = { ListenToPage.extractionDuration.accumulateSamples(listOf(it)) })
            }

            is ListenAction.Session.StopRequested -> {
                seekOrVoiceChangePending = false
                cancelAll(action)
            }

            is ListenAction.Content.ContentReady -> {
                end(EXTRACTION)
                if (TIME_TO_FIRST_AUDIO in spans) startPreparation()
            }

            ListenAction.Content.ContentUnavailable,
            ListenAction.Voices.NoOfflineVoicesAvailable,
            ListenAction.Synthesis.SynthesisFailed,
            ListenAction.Playback.PlaybackFailed -> failAll(action)

            // Reading starts without a press, so this pauses or resumes it.
            ListenAction.Controls.PlayPauseClicked -> cancel(CHUNK_GAP, action)

            is ListenAction.Voices.VoiceSelected,
            is ListenAction.Playback.SeekRequested,
            ListenAction.Controls.RewindClicked,
            ListenAction.Controls.ForwardClicked -> {
                seekOrVoiceChangePending = true
                cancel(CHUNK_GAP, action)
            }

            // Only a wait after audio has played is a gap. The waits for the opening and for a replay are not.
            ListenAction.Playback.PlaybackWaiting ->
                if (previous.playbackState.phase == PlaybackPhase.Playing) startChunkGap(ChunkGapCause.Waiting)

            is ListenAction.Playback.PlaybackStarted -> onPlaying()

            is ListenAction.Playback.StateChangeObserved -> action.handleStateChange(previous)

            is ListenAction.Voices.AvailableVoicesLoaded,
            ListenAction.Controls.VoicesClicked,
            ListenAction.Controls.VoicesDismissed,
            ListenAction.Controls.PlaybackSpeedClicked,
            ListenAction.Controls.PlaybackSpeedDismissed,
            is ListenAction.Controls.PlaybackSpeedSelected,
            is ListenAction.Playback.ArticleProgressReverted,
            ListenAction.Playback.PlaybackEnded,
            is ListenAction.ModeChanged,
            ListenAction.ErrorDismissed -> Unit

            // The session's own actions, which are internal to it.
            else -> Unit
        }
    }

    /** Starts the preparation, which runs from the article being ready until its opening starts to be synthesized. */
    private fun startPreparation() {
        start(PREPARATION, onEnd = { ListenToPage.preparationDuration.accumulateSamples(listOf(it)) })
    }

    private fun startChunkGap(cause: ChunkGapCause) {
        if (seekOrVoiceChangePending) return

        val started =
            start(
                markerName = CHUNK_GAP,
                text = "cause=${cause.name.lowercase()}",
                onEnd = { durationMs -> ListenToPage.chunkGapDuration.accumulateSamples(listOf(durationMs)) },
            )
        if (started && cause == ChunkGapCause.Waiting) {
            ListenToPage.audioRanOut.add()
        }
    }

    private fun onPlaying() {
        seekOrVoiceChangePending = false
        end(PLAYBACK_START)
        end(TIME_TO_FIRST_AUDIO)
        end(CHUNK_GAP)
    }

    // Returns whether this call started a new marker span
    private fun start(
        markerName: String,
        text: String? = null,
        onEnd: ((durationMs: Long) -> Unit)? = null,
    ): Boolean {
        if (markerName in spans) return false
        val profilerStartTime = profiler.takeIf { it.isProfilerActive() }?.getProfilerTime()
        spans[markerName] = Span(elapsedMs(), profilerStartTime, text, onEnd)
        logger.debug("$markerName started ($text)")
        return true
    }

    private fun end(markerName: String): Boolean {
        val span = spans.remove(markerName) ?: return false
        val durationMs = record(markerName, span, span.text)
        span.onEnd?.invoke(durationMs)
        return true
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

    private fun cancel(markerName: String, action: ListenAction) {
        spans.remove(markerName) ?: return
        logger.debug("$markerName cancelled by ${action::class.simpleName}")
    }

    private fun cancelAll(action: ListenAction) {
        spans.keys.forEach { logger.debug("$it cancelled by ${action::class.simpleName}") }
        spans.clear()
    }

    private fun ListenAction.Playback.StateChangeObserved.handleStateChange(prevState: ListenState) =
        when (playbackState.phase) {
            PlaybackPhase.Playing -> onPlaying()
            PlaybackPhase.Paused -> cancel(CHUNK_GAP, this)
            PlaybackPhase.Buffering if prevState.playbackState.phase == PlaybackPhase.Playing ->
                startChunkGap(ChunkGapCause.Buffering)
            else -> Unit
        }

    override fun process(fact: Fact) {
        if (fact.component != Component.FEATURE_LISTENTOPAGE) return

        when (fact.item) {
            ListenToPageFacts.Items.OPENING_SYNTHESIS_STARTED ->
                if (end(PREPARATION)) {
                    start(
                        OPENING_SYNTHESIS,
                        onEnd = { ListenToPage.openingSynthesisDuration.accumulateSamples(listOf(it)) },
                    )
                }

            ListenToPageFacts.Items.CHUNK_SYNTHESIZED -> fact.recordChunkSynthesized()
        }
    }

    private fun Fact.recordChunkSynthesized() {
        val chunk =
            metadata?.get(ListenToPageFacts.MetadataKeys.CHUNK_SYNTHESIS) as? ListenToPageFacts.ChunkSynthesis ?: return
        if (chunk.index == 0 && end(OPENING_SYNTHESIS)) {
            start(PLAYBACK_START, onEnd = { ListenToPage.playbackStartDuration.accumulateSamples(listOf(it)) })
        }

        val text = "index=${chunk.index}, length=${chunk.length}, audioMs=${chunk.audioMs}"

        logger.debug("$CHUNK_SYNTHESIS took ${chunk.synthesisMs}ms ($text)")

        // The opening is a single sentence and has its own measurement, so it is kept out of the other chunks' samples.
        if (chunk.index != 0) {
            ListenToPage.chunkSynthesisDuration.accumulateSamples(listOf(chunk.synthesisMs))
        }

        val endTime = profiler.getProfilerTime() ?: return
        scope.launch {
            profiler.addMarker(CHUNK_SYNTHESIS, endTime - chunk.synthesisMs, endTime, text)
        }
    }

    private enum class ChunkGapCause {
        // Waiting on synthesis
        Waiting,

        // The media player experienced an internal buffering state
        Buffering,
    }

    private class Span(
        val startTimeMs: Long,
        val profilerStartTime: Double?,
        val text: String?,
        val onEnd: ((durationMs: Long) -> Unit)?,
    )

    private companion object {
        const val CHUNK_SYNTHESIS = "ListenToPage.ChunkSynthesis"
        const val TIME_TO_FIRST_AUDIO = "ListenToPage.TimeToFirstAudio"
        const val EXTRACTION = "$TIME_TO_FIRST_AUDIO.Extraction"
        const val PREPARATION = "$TIME_TO_FIRST_AUDIO.Preparation"
        const val OPENING_SYNTHESIS = "$TIME_TO_FIRST_AUDIO.OpeningSynthesis"
        const val PLAYBACK_START = "$TIME_TO_FIRST_AUDIO.PlaybackStart"
        const val CHUNK_GAP = "ListenToPage.ChunkGap"
        const val FAILED_SUFFIX = ".Failed"
    }
}
