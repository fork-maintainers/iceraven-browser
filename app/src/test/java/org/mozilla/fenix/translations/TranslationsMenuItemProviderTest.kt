/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.translations

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.ContentState
import mozilla.components.browser.state.state.ReaderState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.TranslationsBrowserState
import mozilla.components.browser.state.state.TranslationsState
import mozilla.components.browser.state.state.createCustomTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.MenuItemBadge
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.compose.menu.ui.MenuItemState
import mozilla.components.concept.engine.translate.Language
import mozilla.components.concept.engine.translate.TranslationEngineState
import mozilla.components.concept.engine.translate.TranslationPair
import mozilla.components.concept.engine.translate.TranslationSupport
import mozilla.components.ui.icons.R as iconsR
import org.junit.Test
import org.mozilla.fenix.R
import org.mozilla.fenix.components.menu.MenuFragmentDirections
import org.mozilla.fenix.components.menu.MenuTarget
import org.mozilla.fenix.components.menu.fake.FakeMenuHost
import org.mozilla.fenix.components.menu.fake.reachableEvents
import org.mozilla.fenix.components.menu.store.MenuAction

class TranslationsMenuItemProviderTest {
    @Test
    fun `GIVEN the current page is not translated WHEN providing the item THEN offer translating it`() = runTest {
        val provider = provider()

        assertEquals(translatePageItem(), provider.itemFlow.value)
    }

    @Test
    fun `GIVEN a custom tab WHEN providing the item THEN offer translating it`() = runTest {
        val customTab = createCustomTab(url = "https://mozilla.org", id = "customTabId")
        val store =
            BrowserStore(
                BrowserState(
                    customTabs = listOf(customTab),
                    translationEngine =
                        TranslationsBrowserState(
                            isEngineSupported = true,
                            supportedLanguages =
                                TranslationSupport(
                                    toLanguages =
                                        listOf(Language(code = LANGUAGE_CODE, localizedDisplayName = LANGUAGE_NAME))
                                ),
                        ),
                )
            )
        val provider = provider(browserStore = store, target = MenuTarget.CustomTab("customTabId"))

        assertEquals(translatePageItem(), provider.itemFlow.value)
    }

    @Test
    fun `GIVEN the current page is translated WHEN providing the item THEN show the language it was translated to`() =
        runTest {
            val provider = provider(browserStore = browserStore(translatedTo = LANGUAGE_CODE))

            assertEquals(
                StandardMenuItem(
                    title = Text.Resource(R.string.browser_menu_translated),
                    icon = MenuItemIconRes(iconsR.drawable.mozac_ic_translate_active_24),
                    onClickEvent = MenuAction.Navigate.Translate,
                    badge = MenuItemBadge(text = Text.String(LANGUAGE_NAME), state = MenuItemState.ACTIVE),
                    state = MenuItemState.ACTIVE,
                ),
                provider.itemFlow.value,
            )
        }

    @Test
    fun `GIVEN the language of a translated page is unknown WHEN providing the item THEN don't show a badge`() =
        runTest {
            val provider = provider(browserStore = browserStore(translatedTo = "unknown language code"))

            assertNull((provider.itemFlow.value as StandardMenuItem).badge)
        }

    @Test
    fun `GIVEN reader view is active WHEN providing the item THEN show translating as not available`() = runTest {
        val provider = provider(browserStore = browserStore(isReaderViewActive = true))

        assertEquals(translatePageItem(canTranslate = false), provider.itemFlow.value)
    }

    @Test
    fun `GIVEN the current page is a PDF WHEN providing the item THEN show translating as not available`() = runTest {
        val provider = provider(browserStore = browserStore(isPdf = true))

        assertEquals(translatePageItem(canTranslate = false), provider.itemFlow.value)
    }

    @Test
    fun `GIVEN the engine does not support translating WHEN providing the item THEN return null`() = runTest {
        val provider = provider(browserStore = browserStore(isEngineSupported = null))

        assertNull(provider.itemFlow.value)
    }

    @Test
    fun `GIVEN translating is not available to this user WHEN providing the item THEN return null`() = runTest {
        val provider = provider(isFeatureEnabled = false)

        assertNull(provider.itemFlow.value)
    }

    @Test
    fun `GIVEN there is no page shown WHEN providing the item THEN return null`() = runTest {
        val provider = provider(browserStore = BrowserStore())

        assertNull(provider.itemFlow.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `GIVEN the user turned translations off WHEN that is known THEN stop showing the item`() = runTest {
        // Translations are assumed to still be on until read, so the item is offered before that is known.
        val provider = provider(translationsSettings = TranslationsEnabledSettings.inMemory(isEnabledInitial = false))

        assertEquals(translatePageItem(), provider.itemFlow.value)

        runCurrent()

        assertNull(provider.itemFlow.value)
    }

    @Test
    fun `WHEN clicking the item THEN show the translations dialog for the current page instead of the menu`() =
        runTest {
            val menu = FakeMenuHost()

            provider().onEvent(MenuAction.Navigate.Translate, menu)

            assertEquals(
                MenuFragmentDirections.actionMenuFragmentToTranslationsDialogFragment(sessionId = TAB_ID),
                menu.directions,
            )
        }

    @Test
    fun `WHEN tapping translate with a custom tab THEN show the translations dialog for the custom tab`() = runTest {
        val customTab = createCustomTab(url = "https://mozilla.org", id = CUSTOM_TAB_ID)
        val store = BrowserStore(BrowserState(customTabs = listOf(customTab)))
        val menu = FakeMenuHost()

        provider(browserStore = store, target = MenuTarget.CustomTab(CUSTOM_TAB_ID))
            .onEvent(MenuAction.Navigate.Translate, menu)

        assertEquals(
            MenuFragmentDirections.actionMenuFragmentToTranslationsDialogFragment(sessionId = CUSTOM_TAB_ID),
            menu.directions,
        )
    }

    @Test
    fun `WHEN the page is translated or not THEN handle all events the item can dispatch and no others`() = runTest {
        listOf(browserStore(), browserStore(translatedTo = LANGUAGE_CODE)).forEach { store ->
            val provider = provider(browserStore = store)

            val events = requireNotNull(provider.itemFlow.value).reachableEvents()

            assertTrue(events.all { provider.handles(it) }, "Not all of $events are handled")
            assertFalse(provider.handles(MenuAction.Navigate.Summarizer))
        }
    }

    // The item is kept up to date on a scope that runTest cancels at the end of each test.
    private fun TestScope.provider(
        browserStore: BrowserStore = browserStore(),
        translationsSettings: TranslationsEnabledSettings = TranslationsEnabledSettings.inMemory(true),
        isFeatureEnabled: Boolean = true,
        target: MenuTarget = MenuTarget.BrowserTab,
    ) =
        TranslationsMenuItemProvider(
            browserStore = browserStore,
            target = target,
            translationsSettings = translationsSettings,
            scope = backgroundScope,
            isFeatureEnabled = isFeatureEnabled,
        )

    private fun browserStore(
        isEngineSupported: Boolean? = true,
        translatedTo: String? = null,
        isReaderViewActive: Boolean = false,
        isPdf: Boolean = false,
    ) =
        BrowserStore(
            BrowserState(
                tabs =
                    listOf(
                        TabSessionState(
                            id = TAB_ID,
                            content = ContentState(url = "https://mozilla.org", isPdf = isPdf),
                            readerState = ReaderState(active = isReaderViewActive),
                            translationsState =
                                TranslationsState(
                                    isTranslated = translatedTo != null,
                                    translationEngineState =
                                        TranslationEngineState(
                                            requestedTranslationPair = TranslationPair(toLanguage = translatedTo)
                                        ),
                                ),
                        )
                    ),
                selectedTabId = TAB_ID,
                translationEngine =
                    TranslationsBrowserState(
                        isEngineSupported = isEngineSupported,
                        supportedLanguages =
                            TranslationSupport(
                                toLanguages =
                                    listOf(Language(code = LANGUAGE_CODE, localizedDisplayName = LANGUAGE_NAME))
                            ),
                    ),
            )
        )

    private fun translatePageItem(canTranslate: Boolean = true) =
        StandardMenuItem(
            title = Text.Resource(R.string.browser_menu_translate_page_2),
            icon = MenuItemIconRes(iconsR.drawable.mozac_ic_translate_24),
            onClickEvent = MenuAction.Navigate.Translate,
            state =
                when (canTranslate) {
                    true -> MenuItemState.DEFAULT
                    else -> MenuItemState.DISABLED
                },
        )

    private companion object {
        const val TAB_ID = "tab1"
        const val CUSTOM_TAB_ID = "customTabId"
        const val LANGUAGE_CODE = "fr"
        const val LANGUAGE_NAME = "French"
    }
}
