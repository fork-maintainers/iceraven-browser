/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.helpers

import androidx.annotation.StringRes
import androidx.test.platform.app.InstrumentationRegistry
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.helpers.TestHelper.appContext
import org.mozilla.fenix.nimbus.FxNimbus
import org.mozilla.fenix.settings.deletebrowsingdata.DeleteBrowsingDataOnQuitType

object HarnessPreferenceState {
    data class OwnedPreference(val id: String, val key: String)

    fun descriptors(): List<OwnedPreference> = buildList {
        add("homepage.bookmarks", R.string.pref_key_customization_bookmarks)
        add("homepage.recentTabs", R.string.pref_key_recent_tabs)
        add("homepage.openAfterFourHours", R.string.pref_key_start_on_home_after_four_hours)
        add("homepage.openAlways", R.string.pref_key_start_on_home_always)
        add("homepage.openLastTab", R.string.pref_key_start_on_home_never)
        add("toolbar.bottom", R.string.pref_key_toolbar_bottom)
        add("toolbar.expanded", R.string.pref_key_toolbar_expanded)
        add("privateBrowsing.allowScreenshots", R.string.pref_key_allow_screenshots_in_private_mode)
        add("deleteOnQuit.enabled", R.string.pref_key_delete_browsing_data_on_quit)
        DeleteBrowsingDataOnQuitType.entries.forEach { type ->
            add(OwnedPreference("deleteOnQuit.${type.name.lowercase()}", type.getPreferenceKey(appContext)))
        }
        add("httpsOnly.enabled", R.string.pref_key_https_only)
        add("httpsOnly.allTabs", R.string.pref_key_https_only_in_all_tabs)
        add("httpsOnly.privateTabs", R.string.pref_key_https_only_in_private_tabs)
        add("trackingProtection.standard", R.string.pref_key_tracking_protection_standard_option)
        add("trackingProtection.strict", R.string.pref_key_tracking_protection_strict_default)
        add("trackingProtection.custom", R.string.pref_key_tracking_protection_custom_option)
        add("search.suggestions", R.string.pref_key_show_search_suggestions)
        add("search.privateSuggestions", R.string.pref_key_show_search_suggestions_in_private)
        add("search.cameraPermissionPrompt", R.string.pref_key_camera_permissions_needed)
        add("accessibility.autoFontSize", R.string.pref_key_accessibility_auto_size)
        add("accessibility.fontScale", R.string.pref_key_accessibility_font_scale)
        add("logins.savePrompt", R.string.pref_key_save_logins)
        add("externalApps.openLinks", R.string.pref_key_open_links_in_apps)
        add("tabGroups.onboarding", R.string.pref_key_tab_groups_onboarding)
        add("googleLens.enabled", R.string.pref_key_google_lens_integration)
        add("googleLens.userEnabled", R.string.pref_key_google_lens_integration_user_enabled)
        add("googleLens.firstRunAccepted", R.string.pref_key_has_accepted_google_lens_first_run)
        add("googleLens.lastCameraMode", R.string.pref_key_lens_camera_last_mode)
    }

    fun overrideIds(): List<String> {
        val preferences = appContext.components.settings.preferences
        val persistedDefaults =
            mapOf(
                "toolbar.bottom" to FxNimbus.features.defaultBottomToolbar.value().enabled,
                "toolbar.expanded" to FxNimbus.features.defaultExpandedToolbar.value().enabled,
            )
        return descriptors()
            .filter { preference ->
                val persistedDefault = persistedDefaults[preference.id]
                persistedDefault?.let { preferences.getBoolean(preference.key, it) != it }
                    ?: preferences.contains(preference.key)
            }
            .map { it.id }
            .sorted()
    }

    fun clear() {
        val components = appContext.components
        val settings = components.settings
        val editor = settings.preferences.edit()
        descriptors().forEach { editor.remove(it.key) }
        check(editor.commit()) { "Failed to synchronously clear harness-owned preferences" }

        val policy = components.core.trackingProtectionPolicyFactory.createTrackingProtectionPolicy()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            components.core.engine.settings.apply {
                val useAutoSize = settings.shouldUseAutoSize
                automaticFontSizeAdjustment = useAutoSize
                fontInflationEnabled = useAutoSize
                if (!useAutoSize) {
                    fontSizeFactor = settings.fontSizeFactor
                }
            }
            components.useCases.settingsUseCases.updateTrackingProtection(policy)
        }
    }

    private fun MutableList<OwnedPreference>.add(id: String, @StringRes resourceId: Int) {
        add(OwnedPreference(id, appContext.getPreferenceKey(resourceId)))
    }
}
