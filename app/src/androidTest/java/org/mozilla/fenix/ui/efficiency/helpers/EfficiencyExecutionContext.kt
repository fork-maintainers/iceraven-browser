/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.helpers

import org.mozilla.fenix.ui.efficiency.navigation.LaunchConfig

enum class RequiredState {
    ENABLED,
    DISABLED,
    PRESERVE,
}

enum class RequiredOrientation {
    PORTRAIT,
    LANDSCAPE,
    PRESERVE,
}

enum class MockWebServerRequirement {
    AVAILABLE,
    NOT_NEEDED,
}

data class EfficiencyExecutionRequirements(
    val orientation: RequiredOrientation = RequiredOrientation.PORTRAIT,
    val backGestureNavigation: RequiredState = RequiredState.ENABLED,
    val dataSaver: RequiredState = RequiredState.DISABLED,
    val systemUiClipboardAccess: RequiredState = RequiredState.DISABLED,
    val postNotificationPermission: RequiredState = RequiredState.ENABLED,
    val clearNotifications: Boolean = true,
    val clearSharedDownloads: Boolean = true,
    val mockWebServer: MockWebServerRequirement = MockWebServerRequirement.AVAILABLE,
    /** UiAutomator's wait-for-idle timeout for the test, or null to keep UiAutomator's default. */
    val uiAutomatorIdleTimeoutMillis: Long? = null,
) {
    fun asMeta(): Map<String, Any?> =
        mapOf(
            "requiredOrientation" to orientation.name,
            "requiredBackGestureNavigation" to backGestureNavigation.name,
            "requiredDataSaver" to dataSaver.name,
            "requiredSystemUiClipboardAccess" to systemUiClipboardAccess.name,
            "requiredPostNotificationPermission" to postNotificationPermission.name,
            "clearNotifications" to clearNotifications,
            "clearSharedDownloads" to clearSharedDownloads,
            "mockWebServer" to mockWebServer.name,
        )
}

data class EfficiencyExecutionContext(
    val testId: String,
    val launchConfig: LaunchConfig,
    val requirements: EfficiencyExecutionRequirements,
) {
    fun asMeta(): Map<String, Any?> = launchConfig.asMeta() + requirements.asMeta()
}
