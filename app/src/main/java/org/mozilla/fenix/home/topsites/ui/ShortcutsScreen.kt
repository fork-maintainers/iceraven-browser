/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.topsites.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mozilla.components.compose.base.annotation.FlexibleWindowLightDarkPreview
import mozilla.components.compose.base.button.IconButton
import mozilla.components.compose.base.utils.BackInvokedHandler
import org.mozilla.fenix.R
import org.mozilla.fenix.home.fake.FakeHomepagePreview
import org.mozilla.fenix.home.topsites.AddShortcutSource
import org.mozilla.fenix.home.topsites.interactor.TopSiteInteractor
import org.mozilla.fenix.home.topsites.store.DialogState
import org.mozilla.fenix.home.topsites.store.PopularSite
import org.mozilla.fenix.home.topsites.store.ShortcutsAction
import org.mozilla.fenix.home.topsites.store.ShortcutsState
import org.mozilla.fenix.home.topsites.store.ShortcutsStore
import org.mozilla.fenix.theme.FirefoxTheme
import mozilla.components.ui.icons.R as iconsR

/**
 * The shortcuts screen.
 *
 * @param store The [ShortcutsStore] used to observe the screen state and dispatch actions.
 * @param interactor The [TopSiteInteractor] used to handle user interactions with shortcuts.
 * @param onNavigationIconClick Callback invoked when the navigation icon is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutsScreen(
    store: ShortcutsStore,
    interactor: TopSiteInteractor,
    onNavigationIconClick: () -> Unit,
) {
    val state by remember { store.stateFlow }.collectAsState(initial = store.state)

    LaunchedEffect(Unit) {
        interactor.onShortcutsLibraryViewed()
    }

    BackInvokedHandler {
        onNavigationIconClick()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.homepage_shortcuts_title),
                        style = FirefoxTheme.typography.headline5,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigationIconClick,
                        contentDescription = "",
                    ) {
                        Icon(
                            painter = painterResource(iconsR.drawable.mozac_ic_back_24),
                            contentDescription = null,
                        )
                    }
                },
                windowInsets = WindowInsets(
                    top = 0.dp,
                    bottom = 0.dp,
                ),
            )
        },
    ) { paddingValues ->
        ShortcutsScreenContent(
            state = state,
            paddingValues = paddingValues,
            interactor = interactor,
            onAddShortcutClicked = { store.dispatch(ShortcutsAction.ShowAddShortcutBottomSheet) },
        )
    }

    ShortcutsDialog(
        dialogState = state.dialogState,
        popularSites = state.popularSites,
        onDismiss = { store.dispatch(ShortcutsAction.CloseDialog) },
        onAddWebsiteClicked = { store.dispatch(ShortcutsAction.ShowAddShortcutDialog) },
        onSaveShortcut = { title, url ->
            store.dispatch(
                ShortcutsAction.SaveShortcut(
                    title = title,
                    url = url,
                    source = AddShortcutSource.MANUAL,
                ),
            )
        },
        onAddPopularSiteClick = { site ->
            store.dispatch(
                ShortcutsAction.SaveShortcut(
                    title = site.title,
                    url = site.url,
                    source = AddShortcutSource.POPULAR,
                ),
            )
        },
    )
}

@Composable
private fun ShortcutsScreenContent(
    state: ShortcutsState,
    paddingValues: PaddingValues,
    interactor: TopSiteInteractor,
    onAddShortcutClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(paddingValues)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Shortcuts(
            topSites = state.topSites,
            interactor = interactor,
            showAddShortcut = state.showAddShortcut,
            onAddShortcutClicked = onAddShortcutClicked,
        )
    }
}

@Composable
private fun ShortcutsDialog(
    dialogState: DialogState,
    popularSites: List<PopularSite>,
    onDismiss: () -> Unit,
    onAddWebsiteClicked: () -> Unit,
    onSaveShortcut: (title: String, url: String) -> Unit,
    onAddPopularSiteClick: (PopularSite) -> Unit,
) {
    when (dialogState) {
        DialogState.AddShortcutBottomSheet -> {
            AddShortcutBottomSheet(
                popularSites = popularSites,
                onDismiss = onDismiss,
                onAddWebsiteClicked = onAddWebsiteClicked,
                onAddPopularSiteClick = onAddPopularSiteClick,
            )
        }

        DialogState.AddShortcut -> {
            AddShortcutDialog(
                onDismiss = onDismiss,
                onConfirm = onSaveShortcut,
            )
        }

        DialogState.Closed -> Unit
    }
}

@Composable
@FlexibleWindowLightDarkPreview
private fun ShortcutsScreenPreviews(
    @PreviewParameter(ShortcutsScreenParameterProvider::class) state: ShortcutsState,
) {
    FirefoxTheme {
        ShortcutsScreen(
            store = ShortcutsStore(initialState = state),
            interactor = FakeHomepagePreview.topSitesInteractor,
            onNavigationIconClick = {},
        )
    }
}

private class ShortcutsScreenParameterProvider : PreviewParameterProvider<ShortcutsState> {
    override val values: Sequence<ShortcutsState> = sequenceOf(
        ShortcutsState(
            topSites = FakeHomepagePreview.topSites(),
            showAddShortcut = false,
        ),
        ShortcutsState(
            topSites = FakeHomepagePreview.topSites(),
            showAddShortcut = true,
        ),
    )
}
