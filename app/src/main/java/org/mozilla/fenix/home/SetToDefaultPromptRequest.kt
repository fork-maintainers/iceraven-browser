/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home

import android.app.Activity
import android.app.Activity.RESULT_CANCELED
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import mozilla.components.support.utils.DateTimeProvider
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.openSetDefaultBrowserOption
import org.mozilla.fenix.utils.Settings
import java.lang.ref.WeakReference

/**
 * Delta value used to determine the speed at which the "set to default" system prompt was dismissed.
 *
 * 300ms is for distinguishing between an automated system response (rejection) and a deliberate user action.
 *
 * A threshold of 300ms is chosen because it is unlikely for a user to perceive, process, and dismiss
 * a system dialog within this timeframe.
 *
 * Values lower than 200ms might be triggered by system lag, while values higher than 500ms could be
 * triggered by users who are very fast at canceling.
 */
private const val MINIMUM_INTERACTION_DURATION_MS = 300

/**
 * @param resultCode The [ActivityResult.resultCode] returned by the calling activity result launcher.
 * @param settings The [Settings] instance used to access and reset the prompt timestamp.
 * @param dateTimeProvider A provider used to retrieve the current system time, allowing for consistent
 * time calculations and easier unit testing.
 * @param isChecklistTask A boolean flag indicating whether it's triggered from a checklist flow.
 * When true and the browser is already set as default, we avoid redirecting the user to device settings.
 * @param navigationAction A lambda function that executes the navigation to the system's default
 * app settings screen.
 */
fun maybeNavigateToSystemSetToDefaultAction(
    resultCode: Int,
    settings: Settings,
    dateTimeProvider: DateTimeProvider,
    isChecklistTask: Boolean = false,
    navigationAction: () -> Unit,
) {
    val promptBlockedBySystem = wasPromptBlockedBySystem(
        resultCode,
        settings.setToDefaultPromptRequested,
        dateTimeProvider,
    )
    val isEligibleForAppSettingsNavigation = settings.isDefaultBrowser && !isChecklistTask
    val shouldNavigateToAppSettingsLauncher = promptBlockedBySystem || isEligibleForAppSettingsNavigation

    if (shouldNavigateToAppSettingsLauncher) {
        navigationAction()
    }

    settings.setToDefaultPromptRequested = 0L
}

/**
 * Creates an [Intent] to prompt the user to set the default browser role via [RoleManager].
 * @return An [Intent] to be used with the role request flow, or null if the role manager or browser
 * role is unavailable.
 */
@RequiresApi(Build.VERSION_CODES.Q)
private fun getBrowserRoleRequestIntent(context: Context): Intent? {
    val roleManager = context.getSystemService(RoleManager::class.java)
    return roleManager?.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
}

/**
 * Manages the process of setting the application as the default handler, adapting the execution
 * flow based on the device's Android version.
 *
 * For Android 10 (API 29) and above, it utilizes the [RoleManager] system prompt, while falling
 * back to navigate to the system 'default browser' settings on older versions.
 *
 * @param activityRef A [WeakReference] to the [HomeActivity], used to launch the system prompt and
 * provide the necessary window context.
 * @param setToDefaultPromptRequest The [ActivityResultLauncher] responsible for launching the system
 * intent and handling the result.
 * @param now The current time in milliseconds.
 */
fun maybeRequestDefaultBrowserPrompt(
    activityRef: WeakReference<HomeActivity>,
    setToDefaultPromptRequest: ActivityResultLauncher<Intent>,
    now: Long = System.currentTimeMillis(),
) {
    val activity = activityRef.get() ?: return

    activity.components.settings.setToDefaultPromptRequested = now

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        activity.openSetDefaultBrowserOption()
        return
    }

    val intent = getBrowserRoleRequestIntent(activity)
    if (intent != null) {
        setToDefaultPromptRequest.launch(intent)
    } else {
        activity.openSetDefaultBrowserOption()
    }
}

/**
 * @param resultCode The result returned by the system activity (e.g., [Activity.RESULT_OK] or
 * [Activity.RESULT_CANCELED]).
 * @param promptRequestTimestamp The timestamp (in milliseconds) when the system prompt was first
 * displayed, used to calculate the interaction duration.
 * @param dateTimeProvider A provider used to retrieve the current system time, allowing for consistent
 * time calculations and easier unit testing.
 * @return True if the prompt was likely blocked by system.
 */
private fun wasPromptBlockedBySystem(
    resultCode: Int,
    promptRequestTimestamp: Long,
    dateTimeProvider: DateTimeProvider,
): Boolean {
    // The time it took for the prompt to be interacted with either by the user or the system.
    val interactionDuration = dateTimeProvider.currentTimeMillis() - promptRequestTimestamp

    return (resultCode == RESULT_CANCELED) && (interactionDuration < MINIMUM_INTERACTION_DURATION_MS)
}
