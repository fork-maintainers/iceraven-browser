/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.settingssearch.secretsettings

import androidx.lifecycle.coroutineScope
import androidx.navigation.fragment.findNavController
import mozilla.components.lib.state.helpers.StoreProvider.Companion.storeProvider
import org.mozilla.fenix.e2e.SystemInsetsPaddedFragment
import org.mozilla.fenix.settings.settingssearch.DefaultFenixSettingsIndexer
import org.mozilla.fenix.settings.settingssearch.FenixRecentSettingsSearchesRepository
import org.mozilla.fenix.settings.settingssearch.PreferenceFileInformation
import org.mozilla.fenix.settings.settingssearch.SettingsSearchAction
import org.mozilla.fenix.settings.settingssearch.SettingsSearchFragment
import org.mozilla.fenix.settings.settingssearch.SettingsSearchItem
import org.mozilla.fenix.settings.settingssearch.SettingsSearchMiddleware
import org.mozilla.fenix.settings.settingssearch.SettingsSearchState
import org.mozilla.fenix.settings.settingssearch.SettingsSearchStore
import org.mozilla.fenix.settings.settingssearch.secretRecentSearchesDataStore

/**
 * Fragment for the secret settings search screen.
 */
class SecretSettingsSearchFragment : SettingsSearchFragment(), SystemInsetsPaddedFragment {

    override fun buildSettingsSearchStore(): SettingsSearchStore = storeProvider.get { restoredState ->
        val secretPreferenceFileInformationList = listOf(
            PreferenceFileInformation.SecretSettingsPreferences,
        )

        SettingsSearchStore(
            initialState = restoredState ?: SettingsSearchState.Default(emptyList()),
            middleware = listOf(
                SettingsSearchMiddleware(
                    fenixSettingsIndexer = DefaultFenixSettingsIndexer(
                        context = requireContext(),
                        preferenceFileInformationList = secretPreferenceFileInformationList,
                    ),
                    navController = findNavController(),
                    recentSettingsSearchesRepository = FenixRecentSettingsSearchesRepository(
                        dataStore = requireContext().secretRecentSearchesDataStore,
                        preferenceFileInformationList = secretPreferenceFileInformationList,
                    ),
                    scope = viewLifecycleOwner.lifecycle.coroutineScope,
                ),
            ),
        )
    }

    override fun onResultItemClick(item: SettingsSearchItem, isRecentSearch: Boolean) {
        settingsSearchStore.dispatch(SettingsSearchAction.ResultItemClicked(item))
    }
}
