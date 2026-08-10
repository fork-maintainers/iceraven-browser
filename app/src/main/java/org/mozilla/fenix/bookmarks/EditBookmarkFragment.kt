/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.flow.emptyFlow
import mozilla.components.lib.state.helpers.StoreProvider.Companion.fragmentStore
import org.mozilla.fenix.R
import org.mozilla.fenix.components.accounts.FenixFxAEntryPoint
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.share.ShareSheetChooserAction
import org.mozilla.fenix.components.share.ShareSource
import org.mozilla.fenix.e2e.SystemInsetsPaddedFragment
import org.mozilla.fenix.ext.bookmarkStorage
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * Menu to edit the name, URL, and location of a bookmark item.
 */
class EditBookmarkFragment : Fragment(), SystemInsetsPaddedFragment {

    private val args by navArgs<EditBookmarkFragmentArgs>()

    @Suppress("LongMethod")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val context = requireContext()
        val buildStore = { composeNavController: NavHostController ->
            val appStore = requireComponents.appStore
            val navController = findNavController()
            val isSignedIntoSync = requireComponents
                .backgroundServices.accountManager.authenticatedAccount() != null

            val store by fragmentStore(
                BookmarksState.default.copy(
                    isSignedIntoSync = isSignedIntoSync,
                ),
            ) {
                BookmarksStore(
                    initialState = it,
                    middleware = listOf(
                        BookmarksMiddleware(
                            bookmarksStorage = context.bookmarkStorage,
                            addNewTabUseCase = requireComponents.useCases.tabsUseCases.addTab,
                            fenixBrowserUseCases = requireComponents.useCases.fenixBrowserUseCases,
                            openBookmarksInNewTab = if (requireComponents.settings.enableHomepageAsNewTab) {
                                false
                            } else {
                                appStore.state.mode.isPrivate
                            },
                            getNavController = { composeNavController },
                            exitBookmarks = { navController.popBackStack() },
                            navigateToBrowser = {
                                navController.navigate(R.id.browserFragment)
                            },
                            navigateToSignIntoSync = {
                                navController
                                    .navigate(
                                        BookmarkFragmentDirections.actionGlobalTurnOnSync(
                                            entrypoint = FenixFxAEntryPoint.BookmarkView,
                                        ),
                                    )
                            },
                            navigateToImportDialog = {},
                            shareBookmarks = { bookmarks ->
                                val shareItems = bookmarks.asShareDataArray().toList()
                                requireComponents.useCases.shareUseCases.shareItems(
                                    items = shareItems,
                                    source = ShareSource.BOOKMARKS,
                                    chooserActions = if (shareItems.size == 1) {
                                        listOf(
                                            ShareSheetChooserAction.SEND_TO_DEVICES,
                                            ShareSheetChooserAction.QR_CODE,
                                        )
                                    } else {
                                        listOf(ShareSheetChooserAction.SEND_TO_DEVICES)
                                    },
                                    navigateToShareFragment = {
                                        navController.nav(
                                            R.id.bookmarkFragment,
                                            BookmarkFragmentDirections.actionGlobalShareFragment(
                                                data = bookmarks.asShareDataArray(),
                                            ),
                                        )
                                    },
                                )
                            },
                            showTabsTray = { },
                            resolveFolderTitle = {
                                friendlyRootTitle(
                                    context = context,
                                    node = it,
                                    rootTitles = composeRootTitles(context),
                                ) ?: ""
                            },
                            getBrowsingMode = {
                                appStore.state.mode
                            },
                            editBookmarkUseCase = requireComponents.useCases.bookmarksUseCases.editBookmark,
                            saveBookmarkSortOrder = {},
                            reportResultGlobally = {
                                requireComponents.appStore.dispatch(
                                    AppAction.BookmarkAction.BookmarkOperationResultReported(it),
                                )
                            },
                            importEvents = { emptyFlow() },
                            lifecycleScope = lifecycleScope,
                        ),
                    ),
                )
            }

            store
        }
        return content {
            FirefoxTheme {
                BookmarksScreen(
                    buildStore = buildStore,
                    startDestination = BookmarksDestinations.EDIT_BOOKMARK,
                    bookmarkToLoad = args.guidToEdit,
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.title = getString(R.string.app_name)
    }
}
