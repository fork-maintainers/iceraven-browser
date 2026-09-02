/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.generation.behavior

/**
 * Describes whether a context value is currently runnable or only tracked as future coverage.
 *
 * Keep this deliberately simple:
 * - IMPLEMENTED means the Behavior factory/runtime knows how to create/apply this value.
 * - TODO means it is part of the desired matrix, but should not produce executable cases yet.
 */
enum class BehaviorContextStatus {
    IMPLEMENTED,
    TODO,
}

/**
 * Describes where a context value would eventually be applied.
 *
 * This is documentation + planning metadata for now. It lets the manifest act as a roadmap.
 */
enum class BehaviorContextApplicationMode {
    IMPLICIT_DEFAULT,
    BASE_TEST_CONSTRUCTOR,
    RUNTIME_SETUP,
    CI_DEVICE_SELECTION,
    PRESEEDED_DATA,
}

/**
 * One allowed value for a context axis.
 *
 * Example:
 *   key=BrowserMode, value=Default
 *   key=BrowserMode, value=Private
 */
data class BehaviorContextValueSpec(
    val key: String,
    val value: String,
    val status: BehaviorContextStatus,
    val applicationMode: BehaviorContextApplicationMode,
    val reason: String,
) {
    val id: String get() = "$key=$value"
}

/**
 * Manifest of context values we care about.
 *
 * This is intentionally broader than what is runnable today.
 * It is both a roadmap and a source of explanations when generated cases are skipped.
 */
object BehaviorContextManifest {

    val all: List<BehaviorContextValueSpec> = listOf(
        BehaviorContextValueSpec(
            key = "BrowserMode",
            value = "Default",
            status = BehaviorContextStatus.IMPLEMENTED,
            applicationMode = BehaviorContextApplicationMode.IMPLICIT_DEFAULT,
            reason = "Default app/browser mode is created by the normal BaseTest launch path.",
        ),
        BehaviorContextValueSpec(
            key = "BrowserMode",
            value = "Private",
            status = BehaviorContextStatus.TODO,
            applicationMode = BehaviorContextApplicationMode.RUNTIME_SETUP,
            reason = "Needs a Behavior runtime setup capability that opens or verifies private browsing mode.",
        ),
        BehaviorContextValueSpec(
            key = "PocketEnabled",
            value = "false",
            status = BehaviorContextStatus.TODO,
            applicationMode = BehaviorContextApplicationMode.BASE_TEST_CONSTRUCTOR,
            reason = "BaseTest has a constructor flag, but Behavior shards do not yet derive BaseTest config from context.",
        ),
        BehaviorContextValueSpec(
            key = "RecentlyVisitedEnabled",
            value = "false",
            status = BehaviorContextStatus.TODO,
            applicationMode = BehaviorContextApplicationMode.BASE_TEST_CONSTRUCTOR,
            reason = "BaseTest has a constructor flag, but Behavior shards do not yet derive BaseTest config from context.",
        ),
        BehaviorContextValueSpec(
            key = "UnifiedTrustPanelEnabled",
            value = "false",
            status = BehaviorContextStatus.TODO,
            applicationMode = BehaviorContextApplicationMode.BASE_TEST_CONSTRUCTOR,
            reason = "BaseTest has a constructor flag, but Behavior shards do not yet derive BaseTest config from context.",
        ),
        BehaviorContextValueSpec(
            key = "Account",
            value = "SignedOut",
            status = BehaviorContextStatus.TODO,
            applicationMode = BehaviorContextApplicationMode.PRESEEDED_DATA,
            reason = "Signed-out is probably the default, but the Behavior context layer does not yet own account-state validation.",
        ),
        BehaviorContextValueSpec(
            key = "Account",
            value = "SignedIn",
            status = BehaviorContextStatus.TODO,
            applicationMode = BehaviorContextApplicationMode.PRESEEDED_DATA,
            reason = "Needs account setup or mock account/session support.",
        ),
        BehaviorContextValueSpec(
            key = "DeviceClass",
            value = "Phone",
            status = BehaviorContextStatus.TODO,
            applicationMode = BehaviorContextApplicationMode.CI_DEVICE_SELECTION,
            reason = "Device class should be selected by CI/device pool, not toggled inside the test.",
        ),
        BehaviorContextValueSpec(
            key = "DeviceClass",
            value = "Tablet",
            status = BehaviorContextStatus.TODO,
            applicationMode = BehaviorContextApplicationMode.CI_DEVICE_SELECTION,
            reason = "Device class should be selected by CI/device pool, not toggled inside the test.",
        ),
    )

    fun find(
        key: String,
        value: String,
    ): BehaviorContextValueSpec? {
        return all.firstOrNull {
            it.key.equals(key, ignoreCase = true) &&
                it.value.equals(value, ignoreCase = true)
        }
    }
}
