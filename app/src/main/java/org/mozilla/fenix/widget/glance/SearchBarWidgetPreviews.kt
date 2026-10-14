/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.widget.glance

import android.content.Context
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidgetManager
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.ext.components

/** Publishes the generated widget picker previews of the search bar widgets. */
object SearchBarWidgetPreviews {
    private val receivers =
        listOf(
            SearchBarWidgetReceiver::class,
            SearchBarWithLensWidgetReceiver::class,
            CompactSearchBarWidgetReceiver::class,
        )

    /**
     * Publishes the previews once per app version. Generated previews need Android 15, and the system rate limits
     * publishing, so a failed attempt is retried on a later launch.
     */
    suspend fun publishIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return
        }

        val settings = context.components.settings
        if (settings.searchBarWidgetPreviewsVersion == BuildConfig.VERSION_CODE) {
            return
        }

        val manager = GlanceAppWidgetManager(context)
        val allPublished =
            receivers
                .map { manager.setWidgetPreviews(it) }
                .all { it == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS }

        if (allPublished) {
            settings.searchBarWidgetPreviewsVersion = BuildConfig.VERSION_CODE
        }
    }
}
