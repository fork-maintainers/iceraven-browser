/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.R
import org.mozilla.fenix.compose.snackbar.SNACKBAR_BUTTON_TEST_TAG
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.helpers.TestHelper.shortAppName
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

object BrowserPageSelectors {
    val ENGINE_VIEW = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "engineView",
        description = "Engine view",
        groups = listOf("requiredForPage"),
    )

    val PAGE_CONTENT = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "mozilla",
        description = "Page content",
        groups = listOf(""),
    )

    val SNACKBAR_EDIT_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG,
        value = SNACKBAR_BUTTON_TEST_TAG,
        description = "Snackbar Edit button",
        groups = listOf("snackbar"),
    )

    val MAIN_MENU_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_DESCRIPTION_CONTAINS,
        value = getStringResource(R.string.content_description_menu),
        description = "Three Dot Menu",
        // NOT an arrival anchor: the menu button lives on the top toolbar in the default layout but moves to
        // the bottom navigation bar when shouldUseExpandedToolbar is on — so it's layout-dependent (gotcha B7).
        // ENGINE_VIEW (the web content) is the layout-invariant "browser is loaded" signal for requiredForPage.
        groups = listOf(),
    )

    val TAB_CRASH_REPORTER_IMAGE = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "crash_tab_image",
        description = "Tab crash reporter image",
        groups = listOf("tabCrashReporter"),
    )

    val TAB_CRASH_REPORTER_TITLE = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = getStringResource(R.string.tab_crash_title_2),
        description = "Tab crash reporter title",
        groups = listOf("tabCrashReporter"),
    )

    val TAB_CRASH_REPORTER_MESSAGE = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = getStringResource(R.string.tab_crash_send_report),
        description = "Tab crash reporter send crash message",
        groups = listOf("tabCrashReporter"),
    )

    val TAB_CRASH_REPORTER_RESTORE_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "restoreTabButton",
        description = "Tab crash reporter restore button",
        groups = listOf("tabCrashReporter"),
    )

    val TAB_CRASH_REPORTER_CLOSE_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "closeTabButton",
        description = "Tab crash reporter close button",
        groups = listOf("tabCrashReporter"),
    )

    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun PAGE_CONTENT(text: String = "") = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = text,
        description = "Page content '$text'",
        groups = listOf(),
    )

    val TRANSLATION_SHEET_TITLE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.translations_bottom_sheet_title_first_time, argument = shortAppName),
        description = "Translation bottom sheet title",
        groups = listOf("notTranslatedPageTranslationSheet"),
    )

    val TRANSLATION_SHEET_TRANSLATE_FROM = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.translations_bottom_sheet_translate_from),
        description = "Translation bottom sheet translate from dropdown",
        groups = listOf("notTranslatedPageTranslationSheet"),
    )

    val TRANSLATION_SHEET_TRANSLATE_TO = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.translations_bottom_sheet_translate_to),
        description = "Translation bottom sheet translate to dropdown",
        groups = listOf("notTranslatedPageTranslationSheet"),
    )

    val TRANSLATION_SHEET_TRANSLATE_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.translations_bottom_sheet_positive_button),
        description = "Translation bottom sheet translate button",
        groups = listOf("notTranslatedPageTranslationSheet"),
    )

    val TRANSLATION_SHEET_SHOW_ORIGINAL_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.translations_bottom_sheet_negative_button_restore),
        description = "Translation bottom sheet show original button",
        groups = listOf("translatedPageTranslationSheet"),
    )

    val ADDED_TO_SHORTCUTS_SNACKBAR_TEXT = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = getStringResource(R.string.snackbar_added_to_shortcuts),
        description = "Added to shortcuts snackbar text",
        groups = listOf("addedToShortcutsSnackbar"),
    )

    // Web form submit button. The value is a raw web DOM id, NOT a Compose tag — but GeckoView exposes web
    // element ids unprefixed in the accessibility tree, exactly like Compose's testTagsAsResourceId, so
    // UIAUTOMATOR_WITH_COMPOSE_TAG's un-namespaced resourceId lookup is the mechanism that matches it.
    val SUBMIT_LOGIN_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_COMPOSE_TAG,
        value = "submit",
        description = "Web form submit/login button",
        groups = listOf(),
    )

    // Save-login prompt is an app View (package-prefixed res-id).
    val SAVE_LOGIN_PROMPT = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "feature_prompt_login_fragment",
        description = "Save-login prompt",
        groups = listOf(),
    )

    // --- Address autofill on a web form (GeckoView content + the app's autofill prompt) ---

    // Web DOM ids on the address form page. Not Compose tags — GeckoView exposes web element ids
    // unprefixed in the accessibility tree, so UIAUTOMATOR_WITH_COMPOSE_TAG's un-namespaced resourceId
    // lookup is what matches them (same mechanism as SUBMIT_LOGIN_BUTTON above).
    val ADDRESS_STREET_WEB_FIELD = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_COMPOSE_TAG,
        value = "streetAddress",
        description = "Web address form: street address field",
        groups = listOf(),
    )

    val ADDRESS_CITY_WEB_FIELD = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_COMPOSE_TAG,
        value = "city",
        description = "Web address form: city field",
        groups = listOf(),
    )

    val ADDRESS_COUNTRY_WEB_FIELD = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_COMPOSE_TAG,
        value = "country",
        description = "Web address form: country field",
        groups = listOf(),
    )

    // NOTE: the Android stylus-handwriting prompt that can cover this page is handled centrally via
    // OverlayRegistry + BasePage.dismissKnownOverlaysIfPresent(), not with a per-page selector here.

    // The "Select address" header of the autofill prompt (an app View, package-prefixed res-id).
    val SELECT_ADDRESS_HEADER = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "select_address_header",
        description = "Autofill prompt: 'Select address' header",
        groups = listOf(),
    )

    // A saved-address suggestion row in the autofill prompt, keyed by the substring shown in its
    // name/title (e.g. the street address). App View: package-prefixed res-id + textContains.
    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun ADDRESS_SUGGESTION(text: String = "") = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID_CONTAINING_TEXT,
        value = "address_name",
        secondaryValue = text,
        description = "Autofill suggestion containing '$text'",
        groups = listOf(),
    )

    // Assertion helper: the web street-address field is populated with the expected value.
    // Raw web DOM id + exact text.
    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun AUTOFILLED_STREET_ADDRESS(text: String = "") = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_WEB_ID_AND_TEXT,
        value = "streetAddress",
        secondaryValue = text,
        description = "Web address form: street address autofilled with '$text'",
        groups = listOf(),
    )

    val all = listOf(
        SUBMIT_LOGIN_BUTTON,
        SAVE_LOGIN_PROMPT,
        ADDRESS_STREET_WEB_FIELD,
        ADDRESS_CITY_WEB_FIELD,
        ADDRESS_COUNTRY_WEB_FIELD,
        SELECT_ADDRESS_HEADER,
        ADDRESS_SUGGESTION(),
        AUTOFILLED_STREET_ADDRESS(),
        ENGINE_VIEW,
        PAGE_CONTENT,
        SNACKBAR_EDIT_BUTTON,
        MAIN_MENU_BUTTON,
        TAB_CRASH_REPORTER_IMAGE,
        TAB_CRASH_REPORTER_TITLE,
        TAB_CRASH_REPORTER_MESSAGE,
        TAB_CRASH_REPORTER_RESTORE_BUTTON,
        TAB_CRASH_REPORTER_CLOSE_BUTTON,
        PAGE_CONTENT(),
        TRANSLATION_SHEET_TITLE,
        TRANSLATION_SHEET_TRANSLATE_FROM,
        TRANSLATION_SHEET_TRANSLATE_TO,
        TRANSLATION_SHEET_TRANSLATE_BUTTON,
        TRANSLATION_SHEET_SHOW_ORIGINAL_BUTTON,
        ADDED_TO_SHORTCUTS_SNACKBAR_TEXT,
    )
}
