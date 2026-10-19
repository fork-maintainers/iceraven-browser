/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.search

import android.content.Context
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.settings.settingssearch.PreferenceFileInformation
import org.mozilla.fenix.settings.settingssearch.SettingsSearchItem
import org.mozilla.fenix.settings.settingssearch.SettingsSearchProvider

/** Provides the Google Lens toggle of the Search settings screen for use in settings search. */
object GoogleLensSettingsSearchProvider : SettingsSearchProvider {
    private val preferenceFileInformation = PreferenceFileInformation.SearchSettingsPreferences

    override fun getSearchItems(context: Context): List<SettingsSearchItem> {
        if (!context.components.settings.googleLensIntegrationEnabled) return emptyList()

        return buildList {
            add(
                SettingsSearchItem(
                    title = context.getString(R.string.search_settings_google_lens_title),
                    summary = context.getString(R.string.search_settings_google_lens_description),
                    preferenceKey = context.getString(R.string.pref_key_google_lens_integration_user_enabled),
                    categoryHeader = context.getString(preferenceFileInformation.categoryHeaderResourceId),
                    preferenceFileInformation = preferenceFileInformation,
                )
            )
        }
    }
}
