/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.search

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.Dimension
import androidx.annotation.Dimension.Companion.DP
import androidx.annotation.VisibleForTesting
import mozilla.components.browser.state.state.selectedOrDefaultSearchEngine
import org.mozilla.fenix.GleanMetrics.Metrics
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.IntentReceiverActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.isGoogleSearchEngine
import org.mozilla.fenix.home.intent.StartSearchIntentProcessor
import org.mozilla.fenix.iconpicker.DefaultAppIconRepository
import org.mozilla.fenix.iconpicker.DefaultPackageManagerWrapper
import org.mozilla.fenix.utils.IntentUtils
import org.mozilla.fenix.utils.Settings
import org.mozilla.fenix.widget.LensSearchActivity
import org.mozilla.fenix.widget.VoiceSearchActivity
import org.mozilla.fenix.widget.VoiceSearchActivity.Companion.SPEECH_PROCESSING

class SearchWidgetProvider : AppWidgetProvider() {
    // Implementation note:
    // This class name (SearchWidgetProvider) and package name (org.mozilla.gecko.search) should
    // not be changed because otherwise this widget will disappear from the home screen of the user.
    // The existing name replicates the name and package we used in Fennec.

    override fun onEnabled(context: Context) {
        recordWidgetIsInstalled(context.components.settings)
    }

    override fun onDisabled(context: Context) {
        context.components.settings.searchWidgetInstalled = false
        Metrics.searchWidgetInstalled.set(false)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        recordWidgetIsInstalled(context.components.settings)

        val textSearchIntent = createTextSearchIntent(context)
        val voiceSearchIntent = createVoiceSearchIntent(context)
        val lensSearchIntent = createLensSearchIntent(context)

        appWidgetIds.forEach { appWidgetId ->
            val currentWidth = appWidgetManager.getAppWidgetOptions(appWidgetId).getInt(OPTION_APPWIDGET_MIN_WIDTH)
            val layoutSize = getLayoutSize(currentWidth)
            // It's not enough to just hide the microphone on the "small" sized widget due to its design.
            // The "small" widget needs a complete redesign, meaning it needs a new layout file.
            val showMic = (voiceSearchIntent != null)
            val layout = getLayout(layoutSize, showMic)
            val text = getText(layoutSize, context)

            val views =
                createRemoteViews(
                    context,
                    layout,
                    textSearchIntent,
                    voiceSearchIntent,
                    lensSearchIntent,
                    text,
                )
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?,
    ) {
        val textSearchIntent = createTextSearchIntent(context)
        val voiceSearchIntent = createVoiceSearchIntent(context)
        val lensSearchIntent = createLensSearchIntent(context)

        val currentWidth = appWidgetManager.getAppWidgetOptions(appWidgetId).getInt(OPTION_APPWIDGET_MIN_WIDTH)
        val layoutSize = getLayoutSize(currentWidth)
        val showMic = (voiceSearchIntent != null)
        val layout = getLayout(layoutSize, showMic)
        val text = getText(layoutSize, context)

        val views =
            createRemoteViews(
                context,
                layout,
                textSearchIntent,
                voiceSearchIntent,
                lensSearchIntent,
                text,
            )
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun recordWidgetIsInstalled(settings: Settings) {
        if (!settings.searchWidgetInstalled) {
            settings.searchWidgetInstalled = true
            Metrics.searchWidgetInstalled.set(true)
        }
    }

    /** Builds pending intent that opens the browser and starts a new text search. */
    private fun createTextSearchIntent(context: Context): PendingIntent {
        return Intent(context, IntentReceiverActivity::class.java).let { intent ->
            val createTextSearchIntentFlags =
                IntentUtils.DEFAULT_PENDING_INTENT_FLAGS or PendingIntent.FLAG_UPDATE_CURRENT
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.putExtra(HomeActivity.OPEN_TO_SEARCH, StartSearchIntentProcessor.SEARCH_WIDGET)
            PendingIntent.getActivity(
                context,
                REQUEST_CODE_NEW_TAB,
                intent,
                createTextSearchIntentFlags,
            )
        }
    }

    /** Builds pending intent that starts a new voice search. */
    @VisibleForTesting
    internal fun createVoiceSearchIntent(context: Context): PendingIntent? {
        if (!context.components.settings.shouldShowVoiceSearch) {
            return null
        }

        val voiceIntent =
            Intent(context, VoiceSearchActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(SPEECH_PROCESSING, true)
            }

        val intentSpeech = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

        return intentSpeech.resolveActivity(context.packageManager)?.let {
            PendingIntent.getActivity(
                context,
                REQUEST_CODE_VOICE,
                voiceIntent,
                IntentUtils.DEFAULT_PENDING_INTENT_FLAGS,
            )
        }
    }

    /**
     * Builds pending intent that opens the browser and starts a Google Lens image search. Returns null when the Google
     * Lens integration is disabled or the user's selected search engine is not Google, so the button can be hidden.
     */
    @VisibleForTesting
    internal fun createLensSearchIntent(context: Context): PendingIntent? {
        val settings = context.components.settings
        if (!settings.googleLensIntegrationEnabled || !settings.googleLensIntegrationUserEnabled) {
            return null
        }

        val selectedSearchEngine =
            context.components.core.store.state.search.selectedOrDefaultSearchEngine(private = false)
        if (!selectedSearchEngine.isGoogleSearchEngine()) {
            return null
        }

        val intent =
            Intent(context, LensSearchActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_LENS,
            intent,
            IntentUtils.DEFAULT_PENDING_INTENT_FLAGS or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    @VisibleForTesting
    internal fun createRemoteViews(
        context: Context,
        layout: Int,
        textSearchIntent: PendingIntent,
        voiceSearchIntent: PendingIntent?,
        lensSearchIntent: PendingIntent?,
        text: String?,
    ): RemoteViews {
        return RemoteViews(context.packageName, layout).apply {
            setIcon(context)
            when (layout) {
                R.layout.search_widget_extra_small_v1,
                R.layout.search_widget_extra_small_v2,
                R.layout.search_widget_small_no_mic -> {
                    setOnClickPendingIntent(R.id.button_search_widget_new_tab, textSearchIntent)
                }
                R.layout.search_widget_small -> {
                    setOnClickPendingIntent(R.id.button_search_widget_new_tab_icon, textSearchIntent)
                    setOnClickPendingIntent(R.id.button_search_widget_voice, voiceSearchIntent)
                }
                R.layout.search_widget_medium,
                R.layout.search_widget_large -> {
                    setOnClickPendingIntent(R.id.button_search_widget_new_tab, textSearchIntent)
                    setOnClickPendingIntent(R.id.button_search_widget_voice, voiceSearchIntent)
                    setOnClickPendingIntent(R.id.button_search_widget_new_tab_icon, textSearchIntent)
                    setOnClickPendingIntent(R.id.button_search_widget_lens, lensSearchIntent)
                    setTextViewText(R.id.button_search_widget_new_tab, text)
                    // Unlike "small" widget, "medium" and "large" sizes do not have separate layouts
                    // that exclude the microphone and lens icons, which is why we must hide them
                    // accordingly here.
                    setViewVisibility(
                        R.id.button_search_widget_voice,
                        if (voiceSearchIntent == null) View.GONE else View.VISIBLE,
                    )
                    setViewVisibility(
                        R.id.button_search_widget_lens,
                        if (lensSearchIntent == null) View.GONE else View.VISIBLE,
                    )
                }
            }
        }
    }

    private fun RemoteViews.setIcon(context: Context) {
        val repository =
            DefaultAppIconRepository(
                packageManager = DefaultPackageManagerWrapper(context.packageManager),
                packageName = context.packageName,
            )
        // gradient color available for android:fillColor only on SDK 24+
        setImageViewResource(
            R.id.button_search_widget_new_tab_icon,
            repository.selectedAppIcon.iconForegroundId,
        )

        val appName = context.getString(R.string.app_name)
        setContentDescription(
            R.id.button_search_widget_new_tab_icon,
            context.getString(R.string.search_widget_content_description_2, appName),
        )
    }

    // Cell sizes obtained from the actual dimensions listed in search widget specs
    companion object {
        private const val DP_EXTRA_SMALL = 64
        private const val DP_SMALL = 100
        private const val DP_MEDIUM = 192
        private const val DP_LARGE = 256
        private const val REQUEST_CODE_NEW_TAB = 0
        private const val REQUEST_CODE_VOICE = 1
        private const val REQUEST_CODE_LENS = 2

        /**
         * Updates all instances of the search widget.
         *
         * This function is used to refresh the widget when its appearance or behavior needs to be changed, for example,
         * when the voice search setting is toggled.
         *
         * @param context The application context.
         */
        fun updateAllWidgets(context: Context) {
            updateAllWidgets(context, AppWidgetManager.getInstance(context))
        }

        /**
         * Updates all instances of the search widget.
         *
         * @param context The application context.
         * @param appWidgetManager An instance of [AppWidgetManager].
         */
        fun updateAllWidgets(context: Context, appWidgetManager: AppWidgetManager) {
            val widgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, SearchWidgetProvider::class.java))

            if (widgetIds.isNotEmpty()) {
                context.sendBroadcast(
                    Intent(context, SearchWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                    }
                )
            }
        }

        @VisibleForTesting
        internal fun getLayoutSize(@Dimension(unit = DP) dp: Int) =
            when {
                dp >= DP_LARGE -> SearchWidgetProviderSize.LARGE
                dp >= DP_MEDIUM -> SearchWidgetProviderSize.MEDIUM
                dp >= DP_SMALL -> SearchWidgetProviderSize.SMALL
                dp >= DP_EXTRA_SMALL -> SearchWidgetProviderSize.EXTRA_SMALL_V2
                else -> SearchWidgetProviderSize.EXTRA_SMALL_V1
            }

        /** Get the layout resource to use for the search widget. */
        @VisibleForTesting
        internal fun getLayout(size: SearchWidgetProviderSize, showMic: Boolean) =
            when (size) {
                SearchWidgetProviderSize.LARGE -> R.layout.search_widget_large
                SearchWidgetProviderSize.MEDIUM -> R.layout.search_widget_medium
                SearchWidgetProviderSize.SMALL -> {
                    if (showMic) {
                        R.layout.search_widget_small
                    } else {
                        R.layout.search_widget_small_no_mic
                    }
                }
                SearchWidgetProviderSize.EXTRA_SMALL_V2 -> R.layout.search_widget_extra_small_v2
                SearchWidgetProviderSize.EXTRA_SMALL_V1 -> R.layout.search_widget_extra_small_v1
            }

        /** Get the text to place in the search widget */
        @VisibleForTesting
        internal fun getText(layout: SearchWidgetProviderSize, context: Context) =
            when (layout) {
                SearchWidgetProviderSize.MEDIUM -> context.getString(R.string.search_widget_text_short)
                SearchWidgetProviderSize.LARGE -> context.getString(R.string.search_widget_text_long)
                else -> null
            }
    }
}

enum class SearchWidgetProviderSize {
    EXTRA_SMALL_V1,
    EXTRA_SMALL_V2,
    SMALL,
    MEDIUM,
    LARGE,
}
