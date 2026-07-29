/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:OptIn(ExperimentalAndroidComponentsApi::class)

package org.mozilla.fenix.settings.ipprotection

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewParameter
import mozilla.components.ExperimentalAndroidComponentsApi
import mozilla.components.compose.base.annotation.FlexibleWindowPreview
import mozilla.components.compose.base.button.IconButton
import mozilla.components.concept.engine.ipprotection.IPProtectionHandler
import org.mozilla.fenix.R
import org.mozilla.fenix.components.menu.compose.MenuGroup
import org.mozilla.fenix.components.menu.compose.MenuTextItem
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.PreviewThemeProvider
import org.mozilla.fenix.theme.Theme
import java.util.IllformedLocaleException
import java.util.Locale
import mozilla.components.ui.icons.R as iconsR

/**
 * The IP Protection location selection screen.
 *
 * @param selectedRegion The currently selected region, or `null` for recommended location.
 * @param countries A list of countries available in the proxy server-list.
 * @param snackbarHostState The [SnackbarHostState] used to display snackbars.
 * @param onNavigateBack Called when the back navigation icon is tapped.
 * @param onLocationSelected Called with the user taps on a location.
 */
@Composable
fun IPProtectionLocationsScreen(
    selectedRegion: String?,
    countries: List<IPProtectionHandler.Country>,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onLocationSelected: (String?) -> Unit,
) {
    val screenTitle = stringResource(R.string.ip_protection_locations_title)

    Scaffold(
        modifier = Modifier
            .semantics { paneTitle = screenTitle },
        topBar = {
            IPProtectionLocationsTopAppBar(
                onNavigateBack = onNavigateBack,
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
            )
        },
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.surface,
        ) {
            LocationList(
                selectedRegion = selectedRegion,
                countries = countries,
                onLocationSelected = onLocationSelected,
            )
        }
    }
}

@Composable
private fun LocationList(
    selectedRegion: String?,
    countries: List<IPProtectionHandler.Country>,
    onLocationSelected: (String?) -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = FirefoxTheme.layout.space.static200,
                vertical = FirefoxTheme.layout.space.static150,
            ),
        verticalArrangement = Arrangement.spacedBy(FirefoxTheme.layout.space.static200),
    ) {
        MenuGroup {
            LocationOption(
                label = stringResource(R.string.ip_protection_location_recommended_label),
                description = stringResource(R.string.ip_protection_location_fastest_description),
                isSelected = selectedRegion == null,
                onClick = { onLocationSelected(null) },
            )
        }

        if (countries.isNotEmpty()) {
            MenuGroup {
                countries.forEach { country ->
                    LocationOption(
                        label = regionDisplayName(country.code),
                        isSelected = country.code == selectedRegion,
                        enabled = country.available,
                        onClick = { onLocationSelected(country.code) },
                    )
                }
            }
        } else {
            LocationsEmptyState()
        }
    }
}

@Composable
private fun LocationsEmptyState() {
    MenuGroup {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.surfaceBright)
                .padding(
                    paddingValues = PaddingValues(
                        horizontal = FirefoxTheme.layout.space.dynamic200,
                        vertical = FirefoxTheme.layout.space.static150,
                    ),
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.kit_search_error),
                contentDescription = null, // Decorative only
            )

            Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.static200))

            Column(
                verticalArrangement = Arrangement.spacedBy(FirefoxTheme.layout.space.static50),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.ip_protection_locations_unavailable_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = FirefoxTheme.typography.headline7,
                )

                Text(
                    text = stringResource(R.string.ip_protection_locations_unavailable_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = FirefoxTheme.typography.caption,
                )
            }
        }
    }
}

@Composable
private fun LocationOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    description: String? = null,
    enabled: Boolean = true,
) {
    MenuTextItem(
        label = label,
        modifier = Modifier.semantics(mergeDescendants = true) {
            selected = isSelected
            role = Role.RadioButton
        },
        description = description,
        // We should have alternative design for unavailable items,
        // tracked in https://bugzilla.mozilla.org/show_bug.cgi?id=2056379
        enabled = enabled,
        iconPainter = if (isSelected) {
            painterResource(iconsR.drawable.mozac_ic_checkmark_24)
        } else {
            null
        },
        onClick = onClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IPProtectionLocationsTopAppBar(
    onNavigateBack: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.ip_protection_locations_title),
                style = FirefoxTheme.typography.headline5,
                modifier = Modifier.semantics { heading() },
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onNavigateBack,
                contentDescription = stringResource(
                    R.string.ip_protection_locations_navigate_back_button_content_description,
                ),
            ) {
                Icon(
                    painter = painterResource(iconsR.drawable.mozac_ic_back_24),
                    contentDescription = null,
                )
            }
        },
        windowInsets = WindowInsets(),
    )
}

// This will probably go into the data layer, with some validation logic.
// We might want to filter out broken input (if `getDisplayCountry` returns an empty string)
private fun regionDisplayName(regionCode: String): String {
    val normalizedCode = regionCode.uppercase()
    val displayName = try {
        Locale.Builder()
            .setRegion(normalizedCode)
            .build()
            .getDisplayCountry(Locale.getDefault())
    } catch (_: IllformedLocaleException) {
        ""
    }

    return displayName.ifBlank { normalizedCode }
}

@FlexibleWindowPreview
@Composable
private fun IPProtectionLocationsRecommendedPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme = theme) {
        IPProtectionLocationsScreen(
            selectedRegion = null,
            countries = SAMPLE_COUNTRIES,
            snackbarHostState = SnackbarHostState(),
            onNavigateBack = {},
            onLocationSelected = {},
        )
    }
}

@FlexibleWindowPreview
@Composable
private fun IPProtectionLocationsCountrySelectedPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme = theme) {
        IPProtectionLocationsScreen(
            selectedRegion = "fr",
            countries = SAMPLE_COUNTRIES,
            snackbarHostState = SnackbarHostState(),
            onNavigateBack = {},
            onLocationSelected = {},
        )
    }
}

@FlexibleWindowPreview
@Composable
private fun IPProtectionLocationsEmptyPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme = theme) {
        IPProtectionLocationsScreen(
            selectedRegion = null,
            countries = emptyList(),
            snackbarHostState = SnackbarHostState(),
            onNavigateBack = {},
            onLocationSelected = {},
        )
    }
}

private val SAMPLE_COUNTRIES = listOf(
    IPProtectionHandler.Country(code = "dk", available = true),
    IPProtectionHandler.Country(code = "fr", available = true),
    IPProtectionHandler.Country(code = "gb", available = false),
    IPProtectionHandler.Country(code = "us", available = true),
)
