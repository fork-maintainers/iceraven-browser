/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.listentopage

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.listentopage.ListenMiddleware
import mozilla.components.feature.listentopage.ListenState
import mozilla.components.feature.listentopage.ListenStore
import mozilla.components.feature.listentopage.content.ContentProvider
import mozilla.components.feature.listentopage.listenReducer
import mozilla.components.feature.listentopage.playback.DirectoryAudioFileCache
import mozilla.components.feature.listentopage.playback.ListenPlaybackController
import mozilla.components.feature.listentopage.settings.ListenSettings
import mozilla.components.feature.listentopage.synthesis.SpeechSynthesizer

/**
 * Provides access to the components that read a page out loud.
 *
 * @param browserStore Used to find the engine session of the tab an article is extracted from.
 * @param context Used to reach the speech engine, the audio cache and the playback service.
 * @param scope The application scope. The [store] outlives any screen, because the audio keeps playing when the app is
 *   in the background, so its work cannot be tied to a fragment or a view model.
 */
class ListenToPage(
    private val browserStore: BrowserStore,
    private val context: Context,
    private val scope: CoroutineScope,
) {

    val store by lazy {
        ListenStore(
            initialState = ListenState(),
            reducer = ::listenReducer,
            middleware =
                listOf(
                    ListenMiddleware(
                        browserStore = browserStore,
                        contentProvider =
                            ContentProvider.fromPage(
                                pageContentExtractor = pageExtractor,
                                pageMetadataExtractor = pageExtractor,
                            ),
                        // A provider, not an instance: building one binds the speech engine over IPC, and the
                        // middleware closes it when a session stops, so it needs a way to get another.
                        synthesizerProvider = {
                            SpeechSynthesizer.android(context = context, audioCache = audioCache)
                        },
                        audioCache = audioCache,
                        playbackController = ListenPlaybackController(context, scope),
                        settings = ListenSettings.dataStore(context),
                        scope = scope,
                    ),
                    ListenToPageTelemetryMiddleware(),
                ),
        )
    }

    // One cache for the whole feature: the synthesizer writes the audio into it and the middleware empties it when the
    // session stops. Two instances would each hold their own directory and neither would clean up after the other.
    private val audioCache by lazy { DirectoryAudioFileCache(context) }

    private val pageExtractor by lazy {
        FenixListenPageExtractor { tabId -> browserStore.state.findTab(tabId)?.engineState?.engineSession }
    }
}
