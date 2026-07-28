/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.benchmark.utils

import android.content.Intent
import android.net.Uri
import androidx.benchmark.macro.MacrobenchmarkScope

fun MacrobenchmarkScope.browserPageScrollJourney(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, FENIX_HOME_DEEP_LINK)
    intent.setPackage(packageName)

    startActivityAndWait(intent = intent)

    if (device.isWallpaperOnboardingShown()) {
        device.dismissWallpaperOnboarding()
    }

    device.enterSearchMode()
    device.loadSite(url = url)

    device.flingToEnd(
        scrollableId = "$packageName:id/engineView",
        maxSwipes = 1,
    )

    device.flingToBeginning(
        scrollableId = "$packageName:id/engineView",
        maxSwipes = 1,
    )
}

fun MacrobenchmarkScope.homepageScrollJourney() {
    val intent = Intent(Intent.ACTION_VIEW, FENIX_HOME_DEEP_LINK)

    startActivityAndWait(intent = intent)

    if (device.isWallpaperOnboardingShown()) {
        device.dismissWallpaperOnboarding()
    }

    device.flingToEnd(
        scrollableId = "$packageName:id/rootContainer",
        maxSwipes = Int.MAX_VALUE,
    )
}

fun MacrobenchmarkScope.launchIntentJourney(intentData: Uri) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = intentData
    intent.setPackage(packageName)

    startActivityAndWait(intent = intent)
}

fun MacrobenchmarkScope.normalBrowsingJourney(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, FENIX_HOME_DEEP_LINK)
    startActivityAndWait(intent = intent)

    if (device.isWallpaperOnboardingShown()) {
        device.dismissWallpaperOnboarding()
    }

    device.enterSearchMode()
    device.loadSite(url = url)
}

fun MacrobenchmarkScope.onboardingJourney() {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        putExtra(EXTRA_FORCE_ONBOARDING, true)
    }
    startActivityAndWait(intent = intent)
    device.completeOnboarding()
    device.waitForHomepage()
}

fun MacrobenchmarkScope.privateBrowsingJourney(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, FENIX_HOME_DEEP_LINK)

    startActivityAndWait(intent = intent)

    if (device.isWallpaperOnboardingShown()) {
        device.dismissWallpaperOnboarding()
    }

    device.openTabsTray()
    device.openNewPrivateTabOnTabsTray()
    device.enterSearchMode()
    device.loadSite(url = url)

    device.openTabsTray()
    device.closeTab(siteName = HtmlAsset.SIMPLE.title, siteUrl = url)
}

fun MacrobenchmarkScope.startupOnlyJourney() {
    startActivityAndWait()
}

fun MacrobenchmarkScope.switchTabsJourney(simpleHtmlUrl: String, longHtmlUrl: String) {
    val intent = Intent(Intent.ACTION_VIEW, FENIX_HOME_DEEP_LINK)
    intent.setPackage(packageName)

    startActivityAndWait(intent = intent)

    device.enterSearchMode()
    device.loadSite(url = simpleHtmlUrl)

    device.openTabsTray()
    device.openNewTabOnTabsTray()
    device.enterSearchMode()
    device.loadSite(url = longHtmlUrl)

    device.openTabsTray()
    device.switchTabs(siteName = HtmlAsset.SIMPLE.title, newTabUrl = simpleHtmlUrl)

    device.openTabsTray()
    device.closeAllTabs()
}
