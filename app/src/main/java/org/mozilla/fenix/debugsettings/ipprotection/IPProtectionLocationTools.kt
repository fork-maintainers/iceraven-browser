/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:OptIn(ExperimentalAndroidComponentsApi::class)

package org.mozilla.fenix.debugsettings.ipprotection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import java.text.Collator
import mozilla.components.ExperimentalAndroidComponentsApi
import mozilla.components.compose.base.Switch
import mozilla.components.compose.base.button.TextButton
import mozilla.components.compose.base.theme.PreviewThemeProvider
import mozilla.components.compose.base.theme.Theme
import mozilla.components.concept.engine.ipprotection.IPProtectionHandler
import mozilla.components.concept.engine.ipprotection.ServiceState
import mozilla.components.feature.ipprotection.store.IPProtectionAction
import mozilla.components.feature.ipprotection.store.IPProtectionStore
import mozilla.components.feature.ipprotection.store.state.Country
import mozilla.components.feature.ipprotection.store.state.Location
import mozilla.components.lib.state.ext.observeAsComposableState
import org.mozilla.fenix.R
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * Represents one country row of [IPProtectionLocationTools], holding the edits made to it.
 *
 * @property country The country to send back to [IPProtectionStore], carrying the availability set on this screen
 *   rather than the one the store currently holds.
 * @property shouldHide Whether to leave the country out of the list sent back to [IPProtectionStore].
 */
internal data class CountryCustomizationOption(
    val country: Country,
    val shouldHide: Boolean = false,
)

/**
 * Hiding a country from the debug menu removes it from [IPProtectionStore], so this repository keeps every country it
 * has seen in order to restore them.
 */
internal class KnownCountryRepository {
    private val knownAvailabilityByCode = linkedMapOf<String, Boolean>()

    /** Adds [countries] to what is known, keeping the entries they omit. */
    fun save(countries: List<Country>) {
        knownAvailabilityByCode.putAll(countries.associate { it.countryCode to it.available })
    }

    /** Returns every known country, marking the ones missing from [countries] as hidden. */
    fun customizationOptions(countries: List<Country>): List<CountryCustomizationOption> {
        val visibleCodes = countries.mapTo(mutableSetOf()) { it.countryCode }
        return knownAvailabilityByCode.map { (code, available) ->
            CountryCustomizationOption(
                country = Country(countryCode = code, available = available),
                shouldHide = code !in visibleCodes,
            )
        }
    }
}

// Outlives the composition so that hidden countries survive leaving the screen.
private val knownCountries = KnownCountryRepository()

/**
 * The part of the [IPProtectionStore] state that this screen renders.
 *
 * @property serviceState The [ServiceState] of the IP protection service.
 * @property selectedLocation The [Location] the proxy would connect to.
 * @property locations Every [Location] the store currently offers.
 */
private data class LocationToolsState(
    val serviceState: ServiceState,
    val selectedLocation: Location,
    val locations: List<Location>,
)

/**
 * Debug tools for editing the IP protection country list without waiting for a server list change.
 *
 * Edits reach [IPProtectionStore] as [IPProtectionAction.CountryListChanged], the same action the engine delegate
 * dispatches in production, so the persistence and fallback paths behave the same way.
 *
 * @param store The [IPProtectionStore] to read from and dispatch edits to.
 */
@Composable
fun IPProtectionLocationTools(store: IPProtectionStore) {
    val state by store.observeAsComposableState {
        LocationToolsState(
            serviceState = it.serviceStatus,
            selectedLocation = it.locationState.selectedLocation,
            locations = it.locationState.locations,
        )
    }
    val locations = state.locations
    val locale = LocalLocale.current.platformLocale
    var options by remember { mutableStateOf(emptyList<CountryCustomizationOption>()) }

    LaunchedEffect(locations, locale) {
        val countries = locations.filterIsInstance<Country>()
        knownCountries.save(countries)
        options =
            knownCountries
                .customizationOptions(countries)
                .sortedWith(compareBy(Collator.getInstance(locale)) { it.country.displayName(locale) })
    }

    IPProtectionLocationToolsContent(
        serviceState = state.serviceState,
        selectedLocation = state.selectedLocation,
        options = options,
        onOptionChange = { updatedOption ->
            // Applied locally before dispatching: the store echo is asynchronous, and a second edit arriving before it
            // would otherwise rebuild the list from a snapshot that still predates this one.
            options = options.map {
                if (it.country.countryCode == updatedOption.country.countryCode) updatedOption else it
            }

            store.dispatch(
                IPProtectionAction.CountryListChanged(
                    countries =
                        options
                            .filterNot { it.shouldHide }
                            .map { IPProtectionHandler.Country(it.country.countryCode, it.country.available) }
                )
            )
        },
    )
}

@Composable
private fun IPProtectionLocationToolsContent(
    serviceState: ServiceState,
    selectedLocation: Location,
    options: List<CountryCustomizationOption>,
    onOptionChange: (CountryCustomizationOption) -> Unit,
) {
    val unavailableReason =
        when (serviceState) {
            ServiceState.Uninitialized -> stringResource(R.string.debug_drawer_ip_protection_uninitialized)
            ServiceState.Unavailable -> stringResource(R.string.debug_drawer_ip_protection_unavailable)
            ServiceState.Unauthenticated -> stringResource(R.string.debug_drawer_ip_protection_signed_out)
            ServiceState.Ready -> null
        }

    Surface {
        Column(
            modifier =
                Modifier.padding(all = FirefoxTheme.layout.space.static200)
                    .verticalScroll(state = rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FirefoxTheme.layout.space.static100),
        ) {
            when {
                unavailableReason != null ->
                    Text(
                        text = unavailableReason,
                        style = FirefoxTheme.typography.body2,
                    )

                options.isEmpty() ->
                    Text(
                        text = stringResource(R.string.debug_drawer_ip_protection_empty_country_list),
                        style = FirefoxTheme.typography.body2,
                    )

                else -> {
                    Text(
                        text = stringResource(R.string.debug_drawer_ip_protection_location_tools_description),
                        style = FirefoxTheme.typography.headline8,
                    )

                    SelectedLocationRow(
                        location =
                            (selectedLocation as? Country)?.displayName(LocalLocale.current.platformLocale)
                                ?: stringResource(R.string.debug_drawer_ip_protection_recommended)
                    )

                    Text(
                        text = stringResource(R.string.debug_drawer_ip_protection_country_list),
                        style = FirefoxTheme.typography.headline8,
                        modifier = Modifier.padding(top = FirefoxTheme.layout.space.static100),
                    )

                    options.forEach { option ->
                        CountryCustomizationRow(
                            option = option,
                            onChange = onOptionChange,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedLocationRow(location: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.debug_drawer_ip_protection_selected_location),
            modifier = Modifier.weight(1f),
            style = FirefoxTheme.typography.body2,
        )

        Text(
            text = location,
            style = FirefoxTheme.typography.headline8,
        )
    }
}

@Composable
private fun CountryCustomizationRow(
    option: CountryCustomizationOption,
    onChange: (CountryCustomizationOption) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.country.displayName(LocalLocale.current.platformLocale),
                style = FirefoxTheme.typography.body2,
                textDecoration = if (option.shouldHide) TextDecoration.LineThrough else null,
            )

            Text(
                text = option.country.countryCode,
                style = FirefoxTheme.typography.caption,
            )
        }

        TextButton(
            text =
                stringResource(
                    if (option.shouldHide) {
                        R.string.debug_drawer_ip_protection_show_country
                    } else {
                        R.string.debug_drawer_ip_protection_hide_country
                    }
                ),
            onClick = { onChange(option.copy(shouldHide = !option.shouldHide)) },
        )

        Switch(
            checked = option.country.available,
            onCheckedChange = { onChange(option.copy(country = option.country.copy(available = it))) },
            enabled = !option.shouldHide,
        )
    }
}

@Preview
@Composable
private fun IPProtectionLocationToolsPreview(@PreviewParameter(PreviewThemeProvider::class) theme: Theme) {
    FirefoxTheme(theme) {
        IPProtectionLocationToolsContent(
            serviceState = ServiceState.Ready,
            selectedLocation = Country(countryCode = "DE", available = true),
            options =
                listOf(
                    CountryCustomizationOption(country = Country(countryCode = "AT", available = true)),
                    CountryCustomizationOption(country = Country(countryCode = "DE", available = true)),
                    CountryCustomizationOption(country = Country(countryCode = "US", available = false)),
                    CountryCustomizationOption(
                        country = Country(countryCode = "JP", available = true),
                        shouldHide = true,
                    ),
                ),
            onOptionChange = {},
        )
    }
}

@Preview
@Composable
private fun IPProtectionLocationToolsSignedOutPreview(@PreviewParameter(PreviewThemeProvider::class) theme: Theme) {
    FirefoxTheme(theme) {
        IPProtectionLocationToolsContent(
            serviceState = ServiceState.Unauthenticated,
            selectedLocation = Country(countryCode = "DE", available = true),
            options = emptyList(),
            onOptionChange = {},
        )
    }
}
