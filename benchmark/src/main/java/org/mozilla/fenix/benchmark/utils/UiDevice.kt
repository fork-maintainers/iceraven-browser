/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.benchmark.utils

import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector

const val WAITING_TIME_MS = 1000L

fun UiDevice.clearPackageData(packageName: String) {
    executeShellCommand("pm clear $packageName")
}

fun UiDevice.revokeNotificationPermission(packageName: String) {
    executeShellCommand("pm revoke $packageName android.permission.POST_NOTIFICATIONS")
}

fun UiDevice.isWallpaperOnboardingShown() : Boolean {
    val wallpaperFragmentText = findObject(
        UiSelector().text("Choose a wallpaper that speaks to you.")
    )
    return wallpaperFragmentText.exists()
}

fun UiDevice.dismissWallpaperOnboarding() {
    val closeButton = findObject(
        UiSelector().description("Close tab")
    )
    closeButton.click()
}

fun UiDevice.dismissCFR() {
    val cfrDismiss = findObject(
        UiSelector().resourceId("cfr.dismiss")
    )
    if (cfrDismiss.exists()) {
        cfrDismiss.click()
    }
}

fun UiDevice.completeOnboarding() {
    // Step 1: Terms of use
    clickIfExistsWithText("Continue")

    // Step 2: Set as default browser
    clickIfExistsWithResourceId("android:id/button2")

    // Step3: Add Firefox Widget
    clickIfExistsWithText("Not now")

    // Step 4: Sync
    clickIfExistsWithText("Not now")

    // Step 5: Notifications
    clickIfExistsWithText("Not now")

    // Step 6: Notifications
    clickIfExistsWithText("Turn on notifications")
    clickIfExistsWithText("Allow")

    // Stp 7: Address bar position
    clickIfExistsWithText("Continue")
}

fun UiDevice.waitForHomepage() {
    findObject(UiSelector().resourceId("$TARGET_PACKAGE:id/toolbar_text")).run {
        waitForExists(WAITING_TIME_MS)
    }
}

fun UiDevice.clickIfExistsWithText(text: String) {
    findObject(UiSelector().text(text)).run {
        if (waitForExists(WAITING_TIME_MS)) {
            click()
        }
    }
}

fun UiDevice.clickIfExistsWithResourceId(resourceId: String) {
    findObject(UiSelector().resourceId(resourceId)).run {
        if (waitForExists(WAITING_TIME_MS)) {
            click()
        }
    }
}

fun UiDevice.waitUntilPageLoaded() {
    // Refresh icon toggles between refresh and stop; refresh is only shown after page has loaded
    val refresh = findObject(
        UiSelector().description("Refresh"),
    )
    refresh.waitForExists(WAITING_TIME_MS)
}

fun UiDevice.openTabsTray() {
    val tabsTrayButton = findObject(
        UiSelector().resourceId("TabCounterTestTags.tabCounter")
    )
    tabsTrayButton.waitForExists(WAITING_TIME_MS)
    tabsTrayButton.click()
}

fun UiDevice.openNewPrivateTabOnTabsTray() {
    val pbmButton = findObject(
        UiSelector().descriptionStartsWith("Private Tabs Open:")
    )
    pbmButton.waitForExists(WAITING_TIME_MS)
    pbmButton.click()

    val newTabFab = findObject(
        UiSelector().description("Add private tab"),
    )
    newTabFab.waitForExists(WAITING_TIME_MS)
    newTabFab.click()
}

fun UiDevice.openNewTabOnTabsTray() {
    val newTabFab = findObject(
        UiSelector().description("Add tab"),
    )
    newTabFab.waitForExists(WAITING_TIME_MS)
    newTabFab.click()

    if (isWallpaperOnboardingShown()) {
        dismissWallpaperOnboarding()
    }
}

fun UiDevice.switchTabs(siteName: String, newTabUrl: String) {
    var newTabGridItem = findObject(
        UiSelector().textContains(siteName)
    )

    if (newTabGridItem.waitForExists(WAITING_TIME_MS)) {
        newTabGridItem.click()
    } else {
        newTabGridItem = findObject(
            UiSelector().textContains(newTabUrl)
        )
        newTabGridItem.waitForExists(WAITING_TIME_MS)
        newTabGridItem.click()
    }

    waitUntilPageLoaded()

    dismissCFR()
}

fun UiDevice.closeTab(siteName: String, siteUrl: String) {
    val closeTabButtonSiteName = findObject(
        UiSelector()
            .descriptionContains("Close tab")
            .descriptionContains(siteName)
    )

    if (closeTabButtonSiteName.waitForExists(WAITING_TIME_MS)) {
        closeTabButtonSiteName.click()
    } else {
        val closeTabButtonTabUrl = findObject(
            UiSelector()
                .descriptionContains("Close tab")
                .descriptionContains(siteUrl)
        )

        closeTabButtonTabUrl.waitForExists(WAITING_TIME_MS)
        closeTabButtonTabUrl.click()
    }
}

fun UiDevice.closeAllTabs() {
    val contextualMenu = findObject(
        UiSelector().description("Open tabs menu")
    )
    contextualMenu.waitForExists(WAITING_TIME_MS)
    contextualMenu.click()

    val closeAllTabsButton = findObject(
        UiSelector().text("Close all tabs")
    )
    closeAllTabsButton.waitForExists(WAITING_TIME_MS)
    closeAllTabsButton.click()
}

fun UiDevice.enterSearchMode() {
    val urlBar = findObject(
        UiSelector().resourceId("ADDRESSBAR_URL_BOX")
    )
    urlBar.waitForExists(WAITING_TIME_MS)
    urlBar.click()
}

fun UiDevice.loadSite(url: String) {
    val urlBarEditField = findObject(
        UiSelector().resourceId("ADDRESSBAR_SEARCH_BOX")
    )
    urlBarEditField.setText(url)
    pressEnter()

    waitUntilPageLoaded()

    dismissCFR()
}

fun UiDevice.flingToEnd(scrollableId: String, maxSwipes: Int) {
    val scrollable = UiScrollable(UiSelector().resourceId(scrollableId))
    scrollable.waitForExists(WAITING_TIME_MS)
    scrollable.flingToEnd(maxSwipes)
}

fun UiDevice.flingToBeginning(scrollableId: String, maxSwipes: Int) {
    val scrollable = UiScrollable(UiSelector().resourceId(scrollableId))
    scrollable.waitForExists(WAITING_TIME_MS)
    scrollable.flingToBeginning(maxSwipes)
}
