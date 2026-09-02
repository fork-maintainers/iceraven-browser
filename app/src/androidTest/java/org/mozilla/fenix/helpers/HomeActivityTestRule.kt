/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("DEPRECATION")

package org.mozilla.fenix.helpers

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.util.Log
import android.view.ViewConfiguration.getLongPressTimeout
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.rule.ActivityTestRule
import mozilla.components.feature.sitepermissions.SitePermissionsRules
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.components.initializeGlean
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.Constants.TAG
import org.mozilla.fenix.helpers.FeatureSettingsHelper.Companion.settings
import org.mozilla.fenix.helpers.TestHelper.appContext
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.onboarding.FenixOnboarding
import org.mozilla.fenix.settings.PhoneFeature

/**
 * A [org.junit.Rule] to handle shared test set up for tests on [HomeActivity].
 *
 * @param initialTouchMode See [ActivityTestRule]
 * @param launchActivity See [ActivityTestRule]
 */

class HomeActivityTestRule(
    initialTouchMode: Boolean = false,
    launchActivity: Boolean = true,
    private val skipOnboarding: Boolean = true,
) : ActivityTestRule<HomeActivity>(HomeActivity::class.java, initialTouchMode, launchActivity),
    FeatureSettingsHelper by FeatureSettingsHelperDelegate() {

    // Using a secondary constructor allows us to easily delegate the settings to FeatureSettingsHelperDelegate.
    // Otherwise if wanting to use the same names we would have to override these settings in the primary
    // constructor and in that elide the FeatureSettingsHelperDelegate.
    constructor(
        initialTouchMode: Boolean = false,
        launchActivity: Boolean = true,
        skipOnboarding: Boolean = true,
        isPocketEnabled: Boolean = settings.showPocketRecommendationsFeature,
        isRecentTabsFeatureEnabled: Boolean = settings.showRecentTabsFeature,
        isRecentlyVisitedFeatureEnabled: Boolean = settings.historyMetadataUIFeature,
        isWallpaperOnboardingEnabled: Boolean = settings.showWallpaperOnboarding,
        isDeleteSitePermissionsEnabled: Boolean = settings.deleteSitePermissions,
        isOpenInAppBannerEnabled: Boolean = settings.shouldShowOpenInAppBanner,
        etpPolicy: ETPPolicy = getETPPolicy(settings),
        isLocationPermissionEnabled: SitePermissionsRules.Action = getFeaturePermission(PhoneFeature.LOCATION, settings),
        isPageLoadTranslationsPromptEnabled: Boolean = false,
        isMicrosurveyEnabled: Boolean = settings.microsurveyFeatureEnabled,
        shouldUseBottomToolbar: Boolean = settings.shouldUseBottomToolbar,
        isIPProtectionEnabled: Boolean = false,
        isTabSwipeCFREnabled: Boolean = false,
        isTermsOfServiceAccepted: Boolean = true,
        openLinksInExternalApp: OpenLinksInApp = getOpenLinksInApp(settings),
        hasSeenShakeToSummarizeToolbarCfr: Boolean = true,
        shakeToSummarizeFeatureFlagEnabled: Boolean = settings.shakeToSummarizeFeatureFlagEnabled,
        isPrivateModeAndStoriesEntryPointEnabled: Boolean = false,
        shouldUseExpandedToolbar: Boolean = false,
        isTabStripEnabled: Boolean = false,
        nativeShareSheetEnabled: Boolean = false,
        showVoiceSearchInDisplayToolbar: Boolean = false,
        isHomepageTrendingRecentSearchEnabled: Boolean = false,
    ) : this(initialTouchMode, launchActivity, skipOnboarding) {
        this.isPocketEnabled = isPocketEnabled
        this.isRecentTabsFeatureEnabled = isRecentTabsFeatureEnabled
        this.isRecentlyVisitedFeatureEnabled = isRecentlyVisitedFeatureEnabled
        this.isWallpaperOnboardingEnabled = isWallpaperOnboardingEnabled
        this.isDeleteSitePermissionsEnabled = isDeleteSitePermissionsEnabled
        this.isOpenInAppBannerEnabled = isOpenInAppBannerEnabled
        this.etpPolicy = etpPolicy
        this.isLocationPermissionEnabled = isLocationPermissionEnabled
        this.enableOrDisablePageLoadTranslationsPrompt(isPageLoadTranslationsPromptEnabled)
        this.isMicrosurveyEnabled = isMicrosurveyEnabled
        this.shouldUseBottomToolbar = shouldUseBottomToolbar
        this.enableOrDisableIPProtection(isIPProtectionEnabled)
        this.isTabSwipeCFREnabled = isTabSwipeCFREnabled
        this.isTermsOfServiceAccepted = isTermsOfServiceAccepted
        this.openLinksInExternalApp = openLinksInExternalApp
        this.hasSeenShakeToSummarizeToolbarCfr = hasSeenShakeToSummarizeToolbarCfr
        this.shakeToSummarizeFeatureFlagEnabled = shakeToSummarizeFeatureFlagEnabled
        this.isPrivateModeAndStoriesEntryPointEnabled = isPrivateModeAndStoriesEntryPointEnabled
        this.shouldUseExpandedToolbar = shouldUseExpandedToolbar
        this.isTabStripEnabled = isTabStripEnabled
        this.nativeShareSheetEnabled = nativeShareSheetEnabled
        this.showVoiceSearchInDisplayToolbar = showVoiceSearchInDisplayToolbar
        this.isHomepageTrendingRecentSearchEnabled = isHomepageTrendingRecentSearchEnabled
    }

    /**
     * Update settings after the activity was created.
     */
    fun applySettingsExceptions(settings: (FeatureSettingsHelper) -> Unit) {
        Log.i(TAG, "applySettingsExceptions: Trying to update the settings after the activity was created")
        FeatureSettingsHelperDelegate().also {
            settings(this)
            applyFlagUpdates()
        }
        Log.i(TAG, "applySettingsExceptions: Updated the settings after the activity was created")
    }

    private val longTapUserPreference = getLongPressTimeout()

    override fun beforeActivityLaunched() {
        super.beforeActivityLaunched()
        setLongTapTimeout(3000)
        Log.i(TAG, "beforeActivityLaunched: Trying to apply the feature flags updates")
        applyFlagUpdates()
        Log.i(TAG, "beforeActivityLaunched: Successfully applied the feature flag updates")
        if (skipOnboarding) { skipOnboardingBeforeLaunch() }
    }

    override fun afterActivityFinished() {
        super.afterActivityFinished()
        setLongTapTimeout(longTapUserPreference)
        Log.i(TAG, "afterActivityFinished: Trying to reset all feature flags")
        resetAllFeatureFlags()
        Log.i(TAG, "afterActivityFinished: Successfully performed the reset of all feature flags")
    }

    companion object {
        /**
         * Create a new instance of [HomeActivityTestRule] which by default will disable specific
         * app features that would otherwise negatively impact most tests.
         *
         * The disabled features are:
         *  - the PWA prompt dialog,
         *  - the wallpaper onboarding.
         */
        fun withDefaultSettingsOverrides(
            initialTouchMode: Boolean = false,
            launchActivity: Boolean = true,
            skipOnboarding: Boolean = true,
            useNewCrashReporterFlow: Boolean = false,
        ) = HomeActivityTestRule(
            initialTouchMode = initialTouchMode,
            launchActivity = launchActivity,
            skipOnboarding = skipOnboarding,
            isWallpaperOnboardingEnabled = false,
            isOpenInAppBannerEnabled = false,
            isMicrosurveyEnabled = false,
            // workaround for toolbar at top position by default
            // remove with https://bugzilla.mozilla.org/show_bug.cgi?id=1917640
            shouldUseBottomToolbar = true,
            isPageLoadTranslationsPromptEnabled = false,
            isIPProtectionEnabled = false,
            isTabSwipeCFREnabled = true,
            hasSeenShakeToSummarizeToolbarCfr = true,
            isTermsOfServiceAccepted = true,
            isPrivateModeAndStoriesEntryPointEnabled = false,
            isTabStripEnabled = false,
        )
    }
}

/**
 * A [org.junit.Rule] to handle shared test set up for tests on [HomeActivity]. This adds
 * functionality for using the Espresso-intents api, and extends from ActivityTestRule.
 *
 * @param initialTouchMode See [IntentsTestRule]
 * @param launchActivity See [IntentsTestRule]
 */

class HomeActivityIntentTestRule internal constructor(
    initialTouchMode: Boolean = false,
    launchActivity: Boolean = true,
    private val skipOnboarding: Boolean = true,
) : IntentsTestRule<HomeActivity>(HomeActivity::class.java, initialTouchMode, launchActivity),
    FeatureSettingsHelper by FeatureSettingsHelperDelegate() {
    // Using a secondary constructor allows us to easily delegate the settings to FeatureSettingsHelperDelegate.
    // Otherwise if wanting to use the same names we would have to override these settings in the primary
    // constructor and in that elide the FeatureSettingsHelperDelegate.
    constructor(
        initialTouchMode: Boolean = false,
        launchActivity: Boolean = true,
        skipOnboarding: Boolean = true,
        isPocketEnabled: Boolean = settings.showPocketRecommendationsFeature,
        isRecentTabsFeatureEnabled: Boolean = settings.showRecentTabsFeature,
        isRecentlyVisitedFeatureEnabled: Boolean = settings.historyMetadataUIFeature,
        isWallpaperOnboardingEnabled: Boolean = settings.showWallpaperOnboarding,
        isDeleteSitePermissionsEnabled: Boolean = settings.deleteSitePermissions,
        isOpenInAppBannerEnabled: Boolean = settings.shouldShowOpenInAppBanner,
        etpPolicy: ETPPolicy = getETPPolicy(settings),
        isLocationPermissionEnabled: SitePermissionsRules.Action = getFeaturePermission(PhoneFeature.LOCATION, settings),
        isPageLoadTranslationsPromptEnabled: Boolean = false,
        isMicrosurveyEnabled: Boolean = settings.microsurveyFeatureEnabled,
        shouldUseBottomToolbar: Boolean = settings.shouldUseBottomToolbar,
        onboardingFeatureEnabled: Boolean = true,
        isIPProtectionEnabled: Boolean = false,
        isTabSwipeCFREnabled: Boolean = false,
        isTermsOfServiceAccepted: Boolean = true,
        openLinksInExternalApp: OpenLinksInApp = getOpenLinksInApp(settings),
        tabManagerOpeningAnimationEnabled: Boolean = false,
        hasSeenShakeToSummarizeToolbarCfr: Boolean = true,
        shakeToSummarizeFeatureFlagEnabled: Boolean = settings.shakeToSummarizeFeatureFlagEnabled,
        isPrivateModeAndStoriesEntryPointEnabled: Boolean = false,
        shouldUseExpandedToolbar: Boolean = false,
        isTabStripEnabled: Boolean = false,
        nativeShareSheetEnabled: Boolean = false,
        showVoiceSearchInDisplayToolbar: Boolean = false,
        isHomepageTrendingRecentSearchEnabled: Boolean = false,
    ) : this(initialTouchMode, launchActivity, skipOnboarding) {
        this.isPocketEnabled = isPocketEnabled
        this.isRecentTabsFeatureEnabled = isRecentTabsFeatureEnabled
        this.isRecentlyVisitedFeatureEnabled = isRecentlyVisitedFeatureEnabled
        this.isWallpaperOnboardingEnabled = isWallpaperOnboardingEnabled
        this.isDeleteSitePermissionsEnabled = isDeleteSitePermissionsEnabled
        this.isOpenInAppBannerEnabled = isOpenInAppBannerEnabled
        this.etpPolicy = etpPolicy
        this.isLocationPermissionEnabled = isLocationPermissionEnabled
        this.enableOrDisablePageLoadTranslationsPrompt(isPageLoadTranslationsPromptEnabled)
        this.isMicrosurveyEnabled = isMicrosurveyEnabled
        this.shouldUseBottomToolbar = shouldUseBottomToolbar
        this.onboardingFeatureEnabled = onboardingFeatureEnabled
        this.enableOrDisableIPProtection(isIPProtectionEnabled)
        this.isTabSwipeCFREnabled = isTabSwipeCFREnabled
        this.isTermsOfServiceAccepted = isTermsOfServiceAccepted
        this.openLinksInExternalApp = openLinksInExternalApp
        this.tabManagerOpeningAnimationEnabled = tabManagerOpeningAnimationEnabled
        this.hasSeenShakeToSummarizeToolbarCfr = hasSeenShakeToSummarizeToolbarCfr
        this.shakeToSummarizeFeatureFlagEnabled = shakeToSummarizeFeatureFlagEnabled
        this.isPrivateModeAndStoriesEntryPointEnabled = isPrivateModeAndStoriesEntryPointEnabled
        this.shouldUseExpandedToolbar = shouldUseExpandedToolbar
        this.isTabStripEnabled = isTabStripEnabled
        this.nativeShareSheetEnabled = nativeShareSheetEnabled
        this.showVoiceSearchInDisplayToolbar = showVoiceSearchInDisplayToolbar
        this.isHomepageTrendingRecentSearchEnabled = isHomepageTrendingRecentSearchEnabled
    }

    private val longTapUserPreference = getLongPressTimeout()

    private lateinit var intent: Intent

    /**
     * Update settings after the activity was created.
     */
    fun applySettingsExceptions(settings: (FeatureSettingsHelper) -> Unit) {
        Log.i(TAG, "applySettingsExceptions: Trying to update the settings after the activity was created")
        FeatureSettingsHelperDelegate().apply {
            settings(this)
            applyFlagUpdates()
        }
        Log.i(TAG, "applySettingsExceptions: Updated the settings after the activity was created")
    }

    override fun getActivityIntent(): Intent? {
        return if (this::intent.isInitialized) {
            this.intent
        } else {
            super.getActivityIntent()
        }
    }

    fun withIntent(intent: Intent): HomeActivityIntentTestRule {
        this.intent = intent
        return this
    }

    override fun beforeActivityLaunched() {
        super.beforeActivityLaunched()
        setLongTapTimeout(3000)
        Log.i(TAG, "beforeActivityLaunched: Trying to apply the feature flag updates")
        applyFlagUpdates()
        Log.i(TAG, "beforeActivityLaunched: Successfully applied the feature flag updates")
        if (skipOnboarding) { skipOnboardingBeforeLaunch() }
    }

    override fun afterActivityFinished() {
        super.afterActivityFinished()
        setLongTapTimeout(longTapUserPreference)
        Log.i(TAG, "afterActivityFinished: Trying to reset all feature flags")
        resetAllFeatureFlags()
        Log.i(TAG, "afterActivityFinished: Successfully performed the reset of all feature flags")
    }

    /**
     * Update the settings values from when this rule was first instantiated to account for any changes
     * done while running the tests.
     * Useful in the scenario about the activity being restarted which would otherwise set the initial
     * settings and override any changes made in the meantime.
     */
    fun updateCachedSettings() {
        isPocketEnabled = settings.showPocketRecommendationsFeature
        isRecentTabsFeatureEnabled = settings.showRecentTabsFeature
        isRecentlyVisitedFeatureEnabled = settings.historyMetadataUIFeature
        isWallpaperOnboardingEnabled = settings.showWallpaperOnboarding
        isDeleteSitePermissionsEnabled = settings.deleteSitePermissions
        isOpenInAppBannerEnabled = settings.shouldShowOpenInAppBanner
        etpPolicy = getETPPolicy(settings)
        isLocationPermissionEnabled = getFeaturePermission(PhoneFeature.LOCATION, settings)
        isMicrosurveyEnabled = settings.microsurveyFeatureEnabled
        shouldUseBottomToolbar = settings.shouldUseBottomToolbar
        isTabSwipeCFREnabled = !settings.hasShownTabSwipeCFR
        isTermsOfServiceAccepted = settings.hasAcceptedTermsOfService
        openLinksInExternalApp = getOpenLinksInApp(settings)
        tabManagerOpeningAnimationEnabled = settings.tabManagerOpeningAnimationEnabled
        hasSeenShakeToSummarizeToolbarCfr = settings.shakeToSummarizeToolbarCfrShown
        isPrivateModeAndStoriesEntryPointEnabled = settings.privateModeAndStoriesEntryPointEnabled
        nativeShareSheetEnabled = settings.nativeShareSheetEnabled
        isHomepageTrendingRecentSearchEnabled = settings.enableHomepageTrendingRecentSearch
        isTabStripEnabled = settings.isTabStripEnabled
    }

    companion object {
        /**
         * Create a new instance of [HomeActivityIntentTestRule] which by default will disable specific
         * app features that would otherwise negatively impact most tests.
         *
         * The disabled features are:
         *  - the PWA prompt dialog,
         *  - the wallpaper onboarding.
         */
        fun withDefaultSettingsOverrides(
            initialTouchMode: Boolean = false,
            launchActivity: Boolean = true,
            skipOnboarding: Boolean = true,
        ) = HomeActivityIntentTestRule(
            initialTouchMode = initialTouchMode,
            launchActivity = launchActivity,
            skipOnboarding = skipOnboarding,
            isWallpaperOnboardingEnabled = false,
            isOpenInAppBannerEnabled = false,
            isMicrosurveyEnabled = false,
            // workaround for toolbar at top position by default
            // remove with https://bugzilla.mozilla.org/show_bug.cgi?id=1917640
            shouldUseBottomToolbar = true,
            isPageLoadTranslationsPromptEnabled = false,
            isIPProtectionEnabled = false,
            isTabSwipeCFREnabled = true,
            hasSeenShakeToSummarizeToolbarCfr = true,
            isTermsOfServiceAccepted = true,
            tabManagerOpeningAnimationEnabled = false,
            isPrivateModeAndStoriesEntryPointEnabled = false,
            isTabStripEnabled = false,
        )
    }
}

// changing the device preference for Touch and Hold delay, to avoid long-clicks instead of a single-click
fun setLongTapTimeout(delay: Int) {
    // Issue: https://github.com/mozilla-mobile/fenix/issues/25132
    var attempts = 0
    while (attempts++ < 3) {
        try {
            Log.i(TAG, "setLongTapTimeout: Trying to set the \"Touch and hold delay\" to: $delay ms")
            mDevice.executeShellCommand("settings put secure long_press_timeout $delay")
            Log.i(TAG, "setLongTapTimeout: Executed command \"settings put secure long_press_timeout $delay\"")
            break
        } catch (e: RuntimeException) {
            Log.i(TAG, "setLongTapTimeout: RuntimeException caught, executing fallback methods")
            e.printStackTrace()
        }
    }
}

private fun skipOnboardingBeforeLaunch() {
    // The production code isn't aware that we're using
    // this API so it can be fragile.
    Log.i(TAG, "skipOnboardingBeforeLaunch: Trying to skip the onboarding before launching the app")
    FenixOnboarding(appContext).finish()
    // As we are disabling the onboarding we need to initialize glean manually,
    // as it runs after the onboarding finishes
    Handler(Looper.getMainLooper()).post {
        appContext.components.strictMode.allowViolation(StrictMode::allowThreadDiskReads) {
            initializeGlean(
                applicationContext = appContext,
                logger = Logger(),
                isTelemetryUploadEnabled = appContext.components.settings.isTelemetryEnabled,
                client = appContext.components.core.client,
            )
        }
    }
    Log.i(TAG, "skipOnboardingBeforeLaunch: Successfully skipped the onboarding before launching the app")
}
