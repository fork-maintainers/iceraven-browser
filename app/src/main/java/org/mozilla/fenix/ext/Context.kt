/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import androidx.annotation.StringRes
import mozilla.components.compose.base.theme.layout.AcornWindowSize
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.locale.LocaleManager
import org.mozilla.fenix.FenixApplication
import org.mozilla.fenix.R
import org.mozilla.fenix.components.Components
import org.mozilla.fenix.components.toolbar.ToolbarPosition
import org.mozilla.fenix.settings.advanced.getSelectedLocale
import org.mozilla.fenix.utils.isLargeScreenSize
import java.lang.String.format
import java.util.Locale

/**
 * Get the BrowserApplication object from a context.
 */
val Context.application: FenixApplication
    get() = applicationContext as FenixApplication

/**
 * Get the requireComponents of this application.
 */
val Context.components: Components
    get() = application.components

fun Context.asActivity() = (this as? ContextThemeWrapper)?.baseContext as? Activity
    ?: this as? Activity

fun Context.getPreferenceKey(
    @StringRes resourceId: Int,
): String =
    resources.getString(resourceId)

/**
 * Gets the Root View with an activity context
 *
 * @return ViewGroup? if it is able to get a root view from the context
 */
fun Context.getRootView(): View? =
    asActivity()?.window?.decorView?.findViewById<View>(android.R.id.content) as? ViewGroup

/**
 * Used to catch IllegalArgumentException that is thrown when
 * a string's placeholder is incorrectly formatted in a translation
 *
 * @return the formatted string in locale language or English as a fallback
 */
fun Context.getStringWithArgSafe(
    @StringRes resId: Int,
    formatArg: String,
): String {
    return try {
        format(getString(resId), formatArg)
    } catch (e: IllegalArgumentException) {
        // fallback to <en> string
        Logger("L10n").debug(
            "String: " + resources.getResourceEntryName(resId) +
                " not properly formatted in: " + LocaleManager.getSelectedLocale(this).language,
            e,
        )
        val config = resources.configuration
        config.setLocale(Locale.Builder().setLanguage("en").build())
        val localizedContext: Context = this.createConfigurationContext(config)
        return format(localizedContext.getString(resId), formatArg)
    }
}

/**
 * Used to obtain a reference to an AccessibilityManager
 * @return accessibilityManager
 */
val Context.accessibilityManager: AccessibilityManager get() =
    getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

/**
 * Used to navigate to system notifications settings for app.
 *
 * @param onError Invoked when the activity described by the intent is not present on the device.
 */
fun Context.navigateToNotificationsSettings(
    onError: () -> Unit,
) {
    val intent = Intent()
    intent.let {
        it.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
        it.putExtra(Settings.EXTRA_APP_PACKAGE, this.packageName)
    }
    startExternalActivitySafe(intent, onError)
}

/**
 * Used to navigate to the system's "App Info" or "App Details" settings page for this application.
 *
 * @param onError Invoked when the activity described by the intent is not present on the device.
 */
fun Context.navigateToAppDetailsSettings(
    onError: () -> Unit,
) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    }

    startExternalActivitySafe(intent, onError)
}

/**
 * Checks for the presence of an activity before starting it. In case it's not present,
 * [onActivityNotPresent] is invoked, preventing ActivityNotFoundException from being thrown.
 * This is useful when navigating to external activities like device permission settings,
 * notification settings, default app settings, etc.
 *
 * @param intent The Intent of the activity to resolve and start.
 * @param onActivityNotPresent Invoked when the activity to handle the intent is not present.
 */
inline fun Context.startExternalActivitySafe(intent: Intent, onActivityNotPresent: () -> Unit) {
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    } else {
        onActivityNotPresent()
    }
}

/**
 * Helper function used to determine if the user's device is set to dark mode.
 *
 * @return true if the system is considered to be in dark theme.
 */
fun Context.isSystemInDarkTheme(): Boolean =
    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

/**
 * Returns the message to be shown when a tab is closed based on whether the tab was private or not.
 * @param private true if the tab was private, false otherwise.
 */
fun Context.tabClosedUndoMessage(private: Boolean): String =
    if (private) {
        getString(R.string.snackbar_private_tab_closed)
    } else {
        getString(R.string.snackbar_tab_closed)
    }

/**
 * Returns the message to be shown when multiple tabs are closed based on whether the tabs were all private or not.
 * @param count The number of tabs that were closed.
 */
fun Context.tabsClosedUndoMessage(count: Int): String =
    if (count > 1) {
        getString(R.string.snackbar_num_tabs_closed, count.toString())
    } else {
        getString(R.string.snackbar_tab_closed)
    }

/**
 * Helper function used to determine whether the app's total *window* size is at least that of a tablet.
 * This relies on the window size check from [AcornWindowSize]. To determine whether the device's
 * *physical* size is at least the size of a tablet, use [Context.isLargeScreenSize] instead.
 *
 * @return true if the app has a large window size akin to a tablet.
 */
fun Context.isLargeWindow(): Boolean = AcornWindowSize.isLargeWindow(this)

internal const val TALL_SCREEN_HEIGHT_DP = 480
internal const val WIDE_SCREEN_WIDTH_DP = 600

/**
 * Helper function to determine whether the app's current window height
 * is at least more than [TALL_SCREEN_HEIGHT_DP].
 *
 * This is useful when navigation bar should only be enabled on
 * taller screens (e.g., to avoid crowding content vertically).
 *
 * @return true if the window height size is more than [TALL_SCREEN_HEIGHT_DP].
 */
fun Context.isTallWindow(): Boolean {
    return resources.configuration.screenHeightDp > TALL_SCREEN_HEIGHT_DP
}

/**
 * Helper function to determine whether the app's current window width
 * is at least more than [WIDE_SCREEN_WIDTH_DP].
 *
 * This is useful when navigation bar should only be enabled on
 * wider screens (e.g., to avoid crowding content horizontally).
 *
 * @return true if the window width size is more than [WIDE_SCREEN_WIDTH_DP].
 */
fun Context.isWideWindow(): Boolean {
    return resources.configuration.screenWidthDp > WIDE_SCREEN_WIDTH_DP
}

/**
 *  This will record an event in the Nimbus internal event store. Used for behavioral targeting.
 */
fun Context.recordEventInNimbus(eventId: String) = components.nimbus.events.recordEvent(eventId)

/**
 * Returns true if the toolbar is position at the bottom.
 */
fun Context.isToolbarAtBottom() =
    components.settings.toolbarPosition == ToolbarPosition.BOTTOM
