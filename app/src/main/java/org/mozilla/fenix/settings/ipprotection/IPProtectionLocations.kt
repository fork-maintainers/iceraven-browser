/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.ipprotection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import mozilla.components.ExperimentalAndroidComponentsApi
import mozilla.components.compose.base.annotation.FlexibleWindowPreview
import mozilla.components.compose.base.button.IconButton
import mozilla.components.compose.base.theme.AcornCorners
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.list.TextListItem
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
 * @param regions A list of regions.
 * @param snackbarHostState The [SnackbarHostState] used to display snackbars.
 * @param onNavigateBack Called when the back navigation icon is tapped.
 * @param onLocationSelected Called with the user taps on a location.
 */
@Composable
fun IPProtectionLocationsScreen(
    selectedRegion: String?,
    regions: List<String>,
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
                regions = regions,
                onLocationSelected = onLocationSelected,
            )
        }
    }
}

@Composable
private fun LocationList(
    selectedRegion: String?,
    regions: List<String>,
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
        LocationOption(
            label = stringResource(R.string.ip_protection_location_recommended_label),
            description = stringResource(R.string.ip_protection_location_fastest_description),
            isSelected = selectedRegion == null,
            cornerSize = AcornCorners.extraLarge,
            onClick = { onLocationSelected(null) }, // this should look nicer once we have real data objects
        )

        // regions could be empty, empty state design is not ready yet. Tracked in:
        // https://bugzilla.mozilla.org/show_bug.cgi?id=2054588
        if (regions.isNotEmpty()) {
            // There is rounded container around the whole list AND
            // individual elements have rounded (small) corners.
            RoundedContainer(AcornCorners.extraLarge) {
                regions.forEach { region ->
                    LocationOption(
                        label = regionDisplayName(region),
                        isSelected = region == selectedRegion,
                        cornerSize = AcornCorners.extraSmall,
                        onClick = { onLocationSelected(region) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RoundedContainer(
    cornerSize: Dp,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(cornerSize)),
        verticalArrangement = Arrangement.spacedBy(FirefoxTheme.layout.space.static25),
    ) {
        content()
    }
}

@Composable
private fun LocationOption(
    label: String,
    description: String? = null,
    isSelected: Boolean,
    cornerSize: Dp,
    onClick: () -> Unit,
) {
    RoundedContainer(cornerSize = cornerSize) {
        TextListItem(
            label = label,
            modifier = Modifier
                .semantics(mergeDescendants = true) {
                    selected = isSelected
                    role = Role.RadioButton
                }
                .background(color = MaterialTheme.colorScheme.surfaceBright),
            description = description,
            // next two lines should look nicer once we have real data objects
            iconPainter = if (isSelected) {
                painterResource(iconsR.drawable.mozac_ic_checkmark_24)
            } else {
                null
            },
            onClick = onClick,
        )
    }
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

@OptIn(ExperimentalAndroidComponentsApi::class)
@FlexibleWindowPreview
@Composable
private fun IPProtectionLocationsRecommendedPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme = theme) {
        IPProtectionLocationsScreen(
            selectedRegion = null,
            regions = listOf("dk", "fr", "gb", "us"),
            snackbarHostState = SnackbarHostState(),
            onNavigateBack = {},
            onLocationSelected = {},
        )
    }
}

@OptIn(ExperimentalAndroidComponentsApi::class)
@FlexibleWindowPreview
@Composable
private fun IPProtectionLocationsRegionSelectedPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme = theme) {
        IPProtectionLocationsScreen(
            selectedRegion = "fr",
            regions = listOf("dk", "fr", "gb", "us"),
            snackbarHostState = SnackbarHostState(),
            onNavigateBack = {},
            onLocationSelected = {},
        )
    }
}
