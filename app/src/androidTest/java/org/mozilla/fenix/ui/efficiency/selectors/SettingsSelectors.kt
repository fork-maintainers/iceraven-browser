/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.helpers.TestHelper.appName
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

object SettingsSelectors {
    // Present on Settings and its sub-screens; absent once back on Home/Browser. Used as the
    // anchor for backing out of nested Settings via PressBackUntilGone.
    val NAVIGATION_TOOLBAR = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "navigationToolbar",
        description = "the settings navigation toolbar",
        groups = listOf(),
    )

    val GO_BACK_BUTTON = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_CONTENT_DESC,
        value = "Navigate up",
        description = "the Back Arrow button",
        groups = listOf("requiredForPage", "generalSettingsSection"),
    )

    val GENERAL_HEADING = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_TEXT,
        value = "General",
        description = "the General heading",
        groups = listOf("generalSettingsSection", "settingsView"),
    )

    // The "Privacy and security" preference category heading. requiresScroll: it sits below the fold
    // on a phone. Mirrors the legacy verifySettingsView (scrollToElementByText + onView(withText(...))).
    val PRIVACY_AND_SECURITY_HEADING = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = getStringResource(R.string.preferences_category_privacy_security),
        description = "the Privacy and security heading",
        groups = listOf("settingsView", "requiresScroll"),
    )

    // The "Extensions" settings entry, far down the settings list. requiresScroll for the same reason.
    // Mirrors the legacy verifySettingsView (RecyclerView scrollTo preferences_extensions).
    val EXTENSIONS_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = getStringResource(R.string.preferences_extensions),
        description = "the Extensions button",
        groups = listOf("settingsView", "requiresScroll"),
    )

    val SETTINGS_TITLE = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_TEXT,
        value = getStringResource(R.string.settings),
        description = "Settings screen title",
        groups = listOf("requiredForPage"),
    )

    val SEARCH_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Search",
        description = "the Search button",
        groups = listOf("generalSettingsSection"),
    )

    // Espresso variant of the Search row, used to assert its summary (the default engine name) via a
    // sibling-text check - the UiAutomator SEARCH_BUTTON cannot express hasSibling.
    val SEARCH_SETTING_ROW = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_TEXT,
        value = "Search",
        description = "the Search settings row",
        groups = listOf(),
    )

    val TABS_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Tabs",
        description = "the Tabs button",
        groups = listOf("generalSettingsSection"),
    )

    val ACCESSIBILITY_BUTTON = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_TEXT,
        value = "Accessibility",
        description = "the Accessibility button",
        groups = listOf("generalSettingsSection"),
    )

    val AUTOFILL_BUTTON = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_TEXT,
        value = "Autofill",
        description = "the Autofill button",
        groups = listOf("generalSettingsSection"),
    )

    val CUSTOMIZE_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Customize",
        description = "the Customize button",
        groups = listOf("generalSettingsSection"),
    )

    val HOMEPAGE_BUTTON = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_TEXT,
        value = "Homepage",
        description = "the Homepage button",
        groups = listOf("generalSettingsSection"),
    )

    val PASSWORDS_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Passwords",
        description = "the Passwords button",
        groups = listOf("generalSettingsSection"),
    )

    val ABOUT_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "About $appName",
        description = "the About button",
        groups = listOf("aboutSettingsSection"),
    )

    val DATA_COLLECTION_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Data collection",
        description = "the Data Collection button",
        groups = listOf("privacyAndSecuritySettingsSection"),
    )

    val DELETE_BROWSING_DATA_ON_QUIT_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Delete browsing data on quit",
        description = "the Delete browsing data on quit button",
        groups = listOf("privacyAndSecuritySettingsSection"),
    )

    val DELETE_BROWSING_DATA_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Delete browsing data",
        description = "the Delete browsing data button",
        groups = listOf("privacyAndSecuritySettingsSection"),
    )

    val ENHANCED_TRACKING_PROTECTION_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Enhanced Tracking Protection",
        description = "the Enhanced tracking protection button",
        groups = listOf("privacyAndSecuritySettingsSection"),
    )

    val HTTPS_ONLY_MODE_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = getStringResource(R.string.preferences_https_only_title),
        description = "the HTTPS only mode button",
        groups = listOf("privacyAndSecuritySettingsSection"),
    )

    val LANGUAGE_BUTTON = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_TEXT,
        value = getStringResource(R.string.preferences_language),
        description = "the Language button",
        groups = listOf("generalSettingsSection"),
    )

    val OPEN_LINKS_IN_APPS_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Open links in apps",
        description = "the Open links in apps button",
        groups = listOf("advancedSettingsSection"),
    )

    val PRIVATE_BROWSING_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Private browsing",
        description = "the Private browsing button",
        groups = listOf("privacyAndSecuritySettingsSection"),
    )

    val TRANSLATIONS_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Translations",
        description = "the Translations button",
        groups = listOf("generalSettingsSection"),
    )

    val SIGN_IN_BUTTON = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_TEXT,
        value = "Sign in",
        description = "the Sign in button",
        groups = listOf("sync"),
    )

    val NOTIFICATIONS_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Notifications",
        description = "the Notifications button",
        groups = listOf("privacyAndSecuritySettingsSection"),
    )

    val EXPERIMENTS_BUTTON = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_TEXT,
        value = getStringResource(R.string.preferences_nimbus_experiments),
        description = "the Experiments button",
        groups = listOf("experiments"),
    )

    val SITE_SETTINGS_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Site settings",
        description = "the Site settings button",
        groups = listOf("privacyAndSecuritySettingsSection"),
    )

    val ABOUT_SECTION_TITLE = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "About",
        description = "The About Section Title",
        groups = listOf("aboutSection", "requiresScroll"),
    )

    val RATE_ON_GOOGLE_PLAY_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = "Google Play",
        description = "The Rate on Google Play Button",
        groups = listOf("aboutSection", "requiresScroll", "googlePlay"),
    )

    val ABOUT_FIREFOX_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = "About Firefox",
        description = "The About Firefox Title",
        groups = listOf("aboutSection", "aboutFirefox", "requiresScroll"),
    )

    val SEARCH_DEFAULT_SUMMARY = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Google",
        description = "the Search default summary",
        groups = listOf("defaultValues"),
    )

    val TABS_DEFAULT_SUMMARY = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Close manually",
        description = "the Tabs default summary",
        groups = listOf("defaultValues"),
    )

    val ETP_DEFAULT_SUMMARY = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Standard",
        description = "the Enhanced Tracking Protection default summary",
        groups = listOf("defaultValues"),
    )

    val NOTIFICATIONS_DEFAULT_SUMMARY = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Allowed",
        description = "the Notifications default summary",
        groups = listOf("defaultValues"),
    )

    val SET_AS_DEFAULT_BROWSER_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Set as default browser",
        description = "the Set as default browser button",
        groups = listOf("defaultValues"),
    )

    val DOWNLOADS_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Downloads",
        description = "the Downloads button",
        groups = listOf("advancedSettingsSection", "defaultValues"),
    )

    val OPEN_LINKS_IN_APPS_DEFAULT_SUMMARY = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Ask before opening",
        description = "the Open links in apps default summary",
        groups = listOf("defaultValues"),
    )

    val HTTPS_ONLY_MODE_ON_ALL_TABS_SUMMARY = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "On in all tabs",
        description = "HTTPS-Only Mode summary - On in all tabs",
        groups = listOf(),
    )

    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun SETTING_OPTION_SUMMARY(settingName: String = "", settingSummary: String = "") = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_TEXT_WITH_SIBLING_TEXT,
        value = settingName,
        secondaryValue = settingSummary,
        description = "Setting: $settingName with summary: $settingSummary",
        groups = listOf("settingsOptionSummary"),
    )

    val all = listOf(
        NAVIGATION_TOOLBAR,
        GO_BACK_BUTTON,
        GENERAL_HEADING,
        PRIVACY_AND_SECURITY_HEADING,
        EXTENSIONS_BUTTON,
        SETTINGS_TITLE,
        SEARCH_BUTTON,
        SEARCH_SETTING_ROW,
        TABS_BUTTON,
        ACCESSIBILITY_BUTTON,
        AUTOFILL_BUTTON,
        CUSTOMIZE_BUTTON,
        HOMEPAGE_BUTTON,
        PASSWORDS_BUTTON,
        ABOUT_BUTTON,
        DATA_COLLECTION_BUTTON,
        DELETE_BROWSING_DATA_ON_QUIT_BUTTON,
        DELETE_BROWSING_DATA_BUTTON,
        ENHANCED_TRACKING_PROTECTION_BUTTON,
        HTTPS_ONLY_MODE_BUTTON,
        LANGUAGE_BUTTON,
        OPEN_LINKS_IN_APPS_BUTTON,
        PRIVATE_BROWSING_BUTTON,
        TRANSLATIONS_BUTTON,
        SIGN_IN_BUTTON,
        NOTIFICATIONS_BUTTON,
        EXPERIMENTS_BUTTON,
        SITE_SETTINGS_BUTTON,
        ABOUT_FIREFOX_BUTTON,
        ABOUT_SECTION_TITLE,
        RATE_ON_GOOGLE_PLAY_BUTTON,
        SEARCH_DEFAULT_SUMMARY,
        TABS_DEFAULT_SUMMARY,
        ETP_DEFAULT_SUMMARY,
        NOTIFICATIONS_DEFAULT_SUMMARY,
        SET_AS_DEFAULT_BROWSER_BUTTON,
        DOWNLOADS_BUTTON,
        OPEN_LINKS_IN_APPS_DEFAULT_SUMMARY,
        HTTPS_ONLY_MODE_ON_ALL_TABS_SUMMARY,
        SETTING_OPTION_SUMMARY(),
    )
}
