/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceFragmentCompat
import kotlin.system.exitProcess
import kotlinx.coroutines.launch
import mozilla.components.concept.sync.FxAEntryPoint
import mozilla.components.service.fxa.FxaService
import mozilla.components.service.fxa.manager.SCOPE_PROFILE
import mozilla.components.service.fxa.manager.SCOPE_SESSION
import mozilla.components.service.fxa.manager.SCOPE_SYNC
import org.mozilla.fenix.R
import org.mozilla.fenix.e2e.SystemInsetsPaddedFragment
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.showToolbar

/** Lets the user customize Private browsing options. */
class SyncDebugFragment : PreferenceFragmentCompat(), SystemInsetsPaddedFragment {
    private var hasChanges = false

    private val preferenceUpdater =
        object : StringSharedPreferenceUpdater() {
            override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                return super.onPreferenceChange(preference, newValue).also {
                    hasChanges = true
                    updateMenu()
                }
            }
        }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_sync_debug))
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        val handled =
            showCustomEditTextPreferenceDialog(
                preference = preference,
                onSuccess = {
                    hasChanges = true
                    updateMenu()
                },
            )

        if (!handled) {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.sync_debug_preferences, rootKey)
        requirePreference<EditTextPreference>(R.string.pref_key_override_fxa_server).let { pref ->
            pref.setOnBindEditTextListener { it.setSingleLine() }
            pref.onPreferenceChangeListener = preferenceUpdater
        }
        requirePreference<EditTextPreference>(R.string.pref_key_override_sync_tokenserver).let { pref ->
            pref.setOnBindEditTextListener { it.setSingleLine() }
            pref.onPreferenceChangeListener = preferenceUpdater
        }
        requirePreference<EditTextPreference>(R.string.pref_key_override_push_server).let { pref ->
            pref.setOnBindEditTextListener { it.setSingleLine() }
            pref.onPreferenceChangeListener = preferenceUpdater
        }
        requirePreference<Preference>(R.string.pref_key_sync_debug_quit).let { pref ->
            pref.onPreferenceClickListener = OnPreferenceClickListener {
                // Copied from StudiesView. This feels like a dramatic way to
                // quit, is there a better way?
                exitProcess(0)
            }
        }
        requirePreference<CheckBoxPreference>(R.string.pref_key_use_react_fxa).apply {
            onPreferenceChangeListener = SharedPreferenceUpdater()
        }
        setupSimulateErrorPreferences()
        setupScopeAuthorizationPreferences()
        setupOauthTokenFetchPreferences()
        setupPairingPreferences()
        updateMenu()
    }

    private fun setupSimulateErrorPreferences() {
        requirePreference<Preference>(R.string.pref_key_sync_debug_network_error).let { pref ->
            pref.onPreferenceClickListener = OnPreferenceClickListener {
                requireComponents.backgroundServices.accountManager.simulateNetworkError()
                true
            }
        }
        requirePreference<Preference>(R.string.pref_key_sync_debug_temporary_auth_error).let { pref ->
            pref.onPreferenceClickListener = OnPreferenceClickListener {
                requireComponents.backgroundServices.accountManager.simulateTemporaryAuthTokenIssue()
                true
            }
        }
        requirePreference<Preference>(R.string.pref_key_sync_debug_permanent_auth_error).let { pref ->
            pref.onPreferenceClickListener = OnPreferenceClickListener {
                requireComponents.backgroundServices.accountManager.simulatePermanentAuthTokenIssue()
                true
            }
        }
    }

    private fun setupScopeAuthorizationPreferences() {
        requirePreference<EditTextPreference>(R.string.pref_key_sync_debug_scope_authorization_scopes).let { pref ->
            pref.setOnBindEditTextListener { it.setSingleLine() }
            pref.onPreferenceChangeListener = preferenceUpdater
        }
        requirePreference<EditTextPreference>(R.string.pref_key_sync_debug_scope_authorization_entrypoint).let { pref ->
            pref.setOnBindEditTextListener { it.setSingleLine() }
            pref.onPreferenceChangeListener = preferenceUpdater
        }
        requirePreference<Preference>(R.string.pref_key_sync_debug_scope_authorization_start).let { pref ->
            pref.onPreferenceClickListener = OnPreferenceClickListener {
                val accountManager = requireComponents.backgroundServices.accountManager
                if (accountManager.authenticatedAccount() == null) {
                    Toast.makeText(requireContext(), "No authenticated account", Toast.LENGTH_SHORT).show()
                    return@OnPreferenceClickListener true
                }
                val scopesPref =
                    requirePreference<EditTextPreference>(R.string.pref_key_sync_debug_scope_authorization_scopes)
                val scopes = (scopesPref.text ?: "profile").split(" ").filter { it.isNotEmpty() }.toSet()
                val entrypointPref =
                    requirePreference<EditTextPreference>(R.string.pref_key_sync_debug_scope_authorization_entrypoint)
                val entrypoint = (entrypointPref.text ?: "sync-debug-menu").trim()
                val fxaEntryPoint =
                    object : FxAEntryPoint {
                        override val entryName = entrypoint
                    }
                lifecycleScope.launch {
                    val url =
                        accountManager.beginAuthentication(
                            pairingUrl = null,
                            service = FxaService.Sync.identifier,
                            entrypoint = fxaEntryPoint,
                            authScopes = scopes,
                        )
                    if (url != null) {
                        val intent = SupportUtils.createAuthCustomTabIntent(requireContext(), url)
                        startActivity(intent)
                    } else {
                        Toast.makeText(
                                requireContext(),
                                "Failed to begin scope authorization",
                                Toast.LENGTH_SHORT,
                            )
                            .show()
                    }
                }
                true
            }
        }
    }

    // Drives the pairing supplicant flow from a pasted pairing URL, standing in for
    // the QR scanner so pairing can be exercised on an emulator with no camera.
    private fun setupPairingPreferences() {
        requirePreference<EditTextPreference>(R.string.pref_key_sync_debug_pairing_url).let { pref ->
            pref.setOnBindEditTextListener { it.setSingleLine() }
        }
        requirePreference<Preference>(R.string.pref_key_sync_debug_pairing_start).let { pref ->
            pref.onPreferenceClickListener = OnPreferenceClickListener {
                val pairingUrl =
                    requirePreference<EditTextPreference>(R.string.pref_key_sync_debug_pairing_url)
                        .text
                        ?.trim()
                        .orEmpty()
                if (pairingUrl.isEmpty()) {
                    Toast.makeText(requireContext(), "Enter a pairing URL", Toast.LENGTH_SHORT).show()
                    return@OnPreferenceClickListener true
                }
                val fxaEntryPoint =
                    object : FxAEntryPoint {
                        override val entryName = "sync-debug-pairing"
                    }
                requireComponents.services.accountsAuthFeature.beginPairingAuthentication(
                    context = requireContext(),
                    pairingUrl = pairingUrl,
                    entrypoint = fxaEntryPoint,
                    service = FxaService.Sync.identifier,
                    scopes = setOf(SCOPE_SYNC, SCOPE_PROFILE, SCOPE_SESSION),
                )
                true
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun setupOauthTokenFetchPreferences() {
        requirePreference<EditTextPreference>(R.string.pref_key_sync_debug_oauth_token_scopes).let { pref ->
            pref.setOnBindEditTextListener { it.setSingleLine() }
        }
        requirePreference<Preference>(R.string.pref_key_sync_debug_oauth_token_fetch).let { pref ->
            pref.onPreferenceClickListener = OnPreferenceClickListener {
                val account = requireComponents.backgroundServices.accountManager.authenticatedAccount()
                if (account == null) {
                    Toast.makeText(requireContext(), "No authenticated account", Toast.LENGTH_SHORT).show()
                    return@OnPreferenceClickListener true
                }
                val scope =
                    requirePreference<EditTextPreference>(R.string.pref_key_sync_debug_oauth_token_scopes).text?.trim()
                        ?: ""
                val resultPref = requirePreference<Preference>(R.string.pref_key_sync_debug_oauth_token_result)
                lifecycleScope.launch {
                    try {
                        val tokenInfo = account.getAccessToken(scope)
                        resultPref.summary =
                            if (tokenInfo != null) {
                                "Token: ${tokenInfo.token}"
                            } else {
                                "Error: null"
                            }
                        // XXX - `getAccessToken()` catches all FxA exceptions, so there's no way to know
                        // whether we don't have the scope, or if there was a network error or similar.
                        // We should fix that and stop catching plain `Exception` - this remains as a
                        // reminder to do that!
                    } catch (e: Exception) {
                        resultPref.summary = "Error: $e"
                    }
                }
                true
            }
        }
    }

    private fun updateMenu() {
        val settings = requireComponents.settings
        requirePreference<EditTextPreference>(R.string.pref_key_override_fxa_server).let {
            it.summary = settings.overrideFxAServer.ifEmpty { null }
        }
        requirePreference<EditTextPreference>(R.string.pref_key_override_sync_tokenserver).let {
            it.summary = settings.overrideSyncTokenServer.ifEmpty { null }
        }
        requirePreference<EditTextPreference>(R.string.pref_key_override_push_server).let {
            it.summary = settings.overridePushServer.ifEmpty { null }
        }
        requirePreference<Preference>(R.string.pref_key_sync_debug_quit).let { pref ->
            pref.isVisible = hasChanges
        }
        val isConnected = requireComponents.backgroundServices.accountManager.authenticatedAccount() != null
        requirePreference<EditTextPreference>(R.string.pref_key_sync_debug_scope_authorization_scopes).isEnabled =
            isConnected
        requirePreference<EditTextPreference>(R.string.pref_key_sync_debug_scope_authorization_entrypoint).isEnabled =
            isConnected
        requirePreference<Preference>(R.string.pref_key_sync_debug_scope_authorization_start).isEnabled = isConnected
        requirePreference<EditTextPreference>(R.string.pref_key_sync_debug_oauth_token_scopes).isEnabled = isConnected
        requirePreference<Preference>(R.string.pref_key_sync_debug_oauth_token_fetch).isEnabled = isConnected
    }
}
