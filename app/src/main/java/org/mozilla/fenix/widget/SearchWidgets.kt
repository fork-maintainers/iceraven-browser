/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import org.mozilla.fenix.GleanMetrics.Metrics
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.utils.Settings
import org.mozilla.fenix.widget.glance.CompactSearchBarWidgetReceiver
import org.mozilla.fenix.widget.glance.SearchBarWidgetReceiver
import org.mozilla.fenix.widget.glance.SearchBarWithLensWidgetReceiver
import org.mozilla.gecko.search.SearchWidgetProvider

/** The home screen search widgets, across the legacy and the Glance widget frameworks. */
object SearchWidgets {
    /** Every search widget provider. Each one gets its own enabled and disabled callbacks from the system. */
    val providers: List<Class<out AppWidgetProvider>> =
        listOf(
            SearchWidgetProvider::class.java,
            SearchBarWidgetReceiver::class.java,
            SearchBarWithLensWidgetReceiver::class.java,
            CompactSearchBarWidgetReceiver::class.java,
        )

    /** Persists and records whether any search widget is on the home screen. */
    fun syncInstalledState(context: Context, settings: Settings = context.components.settings) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val installed = providers.any { appWidgetManager.getAppWidgetIds(ComponentName(context, it)).isNotEmpty() }
        if (settings.searchWidgetInstalled != installed) {
            settings.searchWidgetInstalled = installed
            Metrics.searchWidgetInstalled.set(installed)
        }
    }
}
