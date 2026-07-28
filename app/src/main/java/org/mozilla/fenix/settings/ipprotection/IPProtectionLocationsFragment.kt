/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:OptIn(ExperimentalAndroidComponentsApi::class)

package org.mozilla.fenix.settings.ipprotection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.navigation.fragment.findNavController
import mozilla.components.ExperimentalAndroidComponentsApi
import mozilla.components.lib.state.ext.observeAsComposableState
import org.mozilla.fenix.components.components
import org.mozilla.fenix.e2e.SystemInsetsPaddedFragment
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * A [Fragment] that hosts the [IPProtectionLocationsScreen] for choosing the VPN location.
 */
class IPProtectionLocationsFragment : Fragment(), SystemInsetsPaddedFragment {

    private val snackbarHostState = SnackbarHostState()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        var selectedRegion by rememberSaveable { mutableStateOf<String?>(null) }
        val countries = components.ipProtection.store.observeAsComposableState { it.countries }.value

        FirefoxTheme {
            IPProtectionLocationsScreen(
                selectedRegion = selectedRegion,
                countries = countries,
                snackbarHostState = snackbarHostState,
                onNavigateBack = { findNavController().popBackStack() },
                onLocationSelected = { selectedRegion = it },
            )
        }
    }
}
