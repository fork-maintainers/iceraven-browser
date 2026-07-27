/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.support.remotesettings.RemoteSettingsServer
import mozilla.components.support.remotesettings.RemoteSettingsServerConfig
import mozilla.components.support.remotesettings.into
import org.mozilla.fenix.R
import org.mozilla.fenix.e2e.SystemInsetsPaddedFragment
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.utils.view.addToRadioGroup

/**
 * Lets the user choose which remote settings server to use.
 */
class RemoteSettingsServerFragment : PreferenceFragmentCompat(), SystemInsetsPaddedFragment {
    private lateinit var radioProduction: RadioButtonPreference
    private lateinit var radioStaging: RadioButtonPreference
    private lateinit var radioDevelopment: RadioButtonPreference
    private var syncingToast: Toast? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.remote_settings_server_preferences, rootKey)

        radioProduction = requirePreference(R.string.pref_key_remote_settings_server_prod)
        radioStaging = requirePreference(R.string.pref_key_remote_settings_server_stage)
        radioDevelopment = requirePreference(R.string.pref_key_remote_settings_server_dev)
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_debug_settings_remote_settings_server_label))

        setupPreferences()
    }

    private fun setupPreferences() {
        when (requireComponents.settings.remoteSettingsServer) {
            getString(R.string.remote_settings_server_prod) ->
                radioProduction.setCheckedWithoutClickListener(true)
            getString(R.string.remote_settings_server_stage) ->
                radioStaging.setCheckedWithoutClickListener(true)
            getString(R.string.remote_settings_server_dev) ->
                radioDevelopment.setCheckedWithoutClickListener(true)
        }

        radioProduction.onClickListener {
            updateRemoteSettingsServer(getString(R.string.remote_settings_server_prod))
        }

        radioStaging.onClickListener {
            updateRemoteSettingsServer(getString(R.string.remote_settings_server_stage))
        }

        radioDevelopment.onClickListener {
            updateRemoteSettingsServer(getString(R.string.remote_settings_server_dev))
        }

        addToRadioGroup(
            radioProduction,
            radioStaging,
            radioDevelopment,
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private fun updateRemoteSettingsServer(serverValue: String) {
        setRadioButtonsEnabled(false)

        requireComponents.settings.remoteSettingsServer = serverValue

        syncingToast = Toast.makeText(
            requireContext(),
            getString(R.string.preferences_remote_settings_syncing),
            Toast.LENGTH_SHORT,
        )
        syncingToast?.show()

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val service = requireContext().components.remoteSettingsService.value.remoteSettingsService
                    service.updateConfig(
                        RemoteSettingsServerConfig(
                            server = when (serverValue) {
                                getString(R.string.remote_settings_server_prod) ->
                                    RemoteSettingsServer.Prod.into()
                                getString(R.string.remote_settings_server_dev) ->
                                    RemoteSettingsServer.Dev.into()
                                getString(R.string.remote_settings_server_stage) ->
                                    RemoteSettingsServer.Stage.into()
                                else -> RemoteSettingsServer.Prod.into()
                            },
                        ).into(),
                    )
                    service.sync()
                }

                syncingToast?.cancel()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.preferences_remote_settings_synced),
                    Toast.LENGTH_SHORT,
                ).show()
            } catch (e: Exception) {
                syncingToast?.cancel()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.preferences_remote_settings_sync_failed, e.message),
                    Toast.LENGTH_LONG,
                ).show()
            } finally {
                setRadioButtonsEnabled(true)
            }
        }
    }

    private fun setRadioButtonsEnabled(enabled: Boolean) {
        radioProduction.isEnabled = enabled
        radioStaging.isEnabled = enabled
        radioDevelopment.isEnabled = enabled
    }
}
