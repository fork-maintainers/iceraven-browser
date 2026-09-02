/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import androidx.core.text.HtmlCompat
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.helpers.TestHelper.appName
import org.mozilla.fenix.helpers.TestHelper.shortAppName
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

object UnifiedTrustPanelSelectors {

    val CLEAR_COOKIES_AND_SITE_DATA_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.clear_site_data),
        description = "Unified trust panel clear cookies and site button",
        groups = listOf("requiredForPage"),
    )

    val CLEAR_COOKIES_AND_SITE_DATA_DIALOG_TITLE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.clear_site_data),
        description = "Unified trust panel clear site data dialog title",
        groups = listOf("clearCookiesAndSiteDataDialog"),
    )

    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun CLEAR_COOKIES_AND_SITE_DATA_DIALOG_DESCRIPTION(webSite: String = "") = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = HtmlCompat.fromHtml(
            getStringResource(R.string.clear_site_data_dialog_description, argument = webSite),
            HtmlCompat.FROM_HTML_MODE_LEGACY,
        ).toString(),
        description = "Unified trust panel clear cookies and site data dialog description",
        groups = listOf("clearCookiesAndSiteDataDialog"),
    )

    val CLEAR_COOKIES_AND_SITE_DATA_DIALOG_CLEAR_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.clear_site_data_dialog_positive_button_text),
        description = "Unified trust panel clear site data dialog clear button",
        groups = listOf("clearCookiesAndSiteDataDialog"),
    )

    val CLEAR_COOKIES_AND_SITE_DATA_DIALOG_CANCEL_BUTTON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.clear_site_data_dialog_negative_button_text),
        description = "Unified trust panel clear site data dialog cancel button",
        groups = listOf("clearCookiesAndSiteDataDialog"),
    )

    // ── Site identity ───────────────────────────────────────────────────────
    // Match the unique testTag AND the text. Text alone is ambiguous — the host also renders in the
    // address bar (gotcha A7) — but tag alone would stop asserting which site the panel is describing.
    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun WEBSITE_TITLE(webSite: String = "") = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG_AND_TEXT,
        value = "unified.trust.panel.website",
        secondaryValue = webSite,
        description = "Unified trust panel website title: $webSite",
        groups = listOf(),
    )

    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun WEBSITE_URL(webSiteURL: String = "") = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TAG_AND_TEXT,
        value = "unified.trust.panel.website.url",
        secondaryValue = webSiteURL,
        description = "Unified trust panel website url: $webSiteURL",
        groups = listOf(),
    )

    // ── Enhanced Tracking Protection banner (content-description, substring) ──
    val ETP_BANNER_PROTECTED_TITLE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION_SUBSTRING,
        value = getStringResource(R.string.protection_panel_banner_protected_title, argument = shortAppName),
        description = "ETP banner: protected title",
        groups = listOf(),
    )

    val ETP_BANNER_NOT_SECURE_TITLE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION_SUBSTRING,
        value = getStringResource(R.string.protection_panel_banner_not_secure_title, argument = appName),
        description = "ETP banner: not-secure title",
        groups = listOf(),
    )

    val ETP_BANNER_NOT_SECURE_DESCRIPTION = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION_SUBSTRING,
        value = getStringResource(R.string.protection_panel_banner_not_secure_description),
        description = "ETP banner: not-secure description",
        groups = listOf(),
    )

    val ETP_BANNER_NOT_PROTECTED_TITLE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION_SUBSTRING,
        value = getStringResource(R.string.protection_panel_banner_not_protected_title),
        description = "ETP banner: not-protected title",
        groups = listOf(),
    )

    val ETP_BANNER_NOT_PROTECTED_DESCRIPTION = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION_SUBSTRING,
        value = getStringResource(R.string.protection_panel_banner_not_protected_description, argument = shortAppName),
        description = "ETP banner: not-protected description",
        groups = listOf(),
    )

    // ── ETP toggle row ───────────────────────────────────────────────────────
    val ETP_TOGGLE_LABEL = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.protection_panel_etp_toggle_label),
        description = "ETP toggle label",
        groups = listOf(),
    )

    val ETP_TOGGLE_ENABLED_DESCRIPTION = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.protection_panel_etp_toggle_enabled_description_2),
        description = "ETP toggle enabled description",
        groups = listOf(),
    )

    val ETP_TOGGLE_DISABLED_DESCRIPTION = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.protection_panel_etp_toggle_disabled_description_2),
        description = "ETP toggle disabled description",
        groups = listOf(),
    )

    val ETP_TOGGLE_ON = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.protection_panel_etp_toggle_on),
        description = "ETP toggle ON state",
        groups = listOf(),
    )

    val ETP_TOGGLE_OFF = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.protection_panel_etp_toggle_off),
        description = "ETP toggle OFF state",
        groups = listOf(),
    )

    // ── Trackers-blocked option ──────────────────────────────────────────────
    val TRACKERS_DISABLED_NONE_BLOCKED = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.protection_panel_etp_disabled_no_trackers_blocked),
        description = "Trackers: ETP disabled, none blocked",
        groups = listOf(),
    )

    val TRACKERS_BLOCKED_ON_SITE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION_SUBSTRING,
        value = "blocked on this site",
        description = "Trackers: blocked on this site",
        groups = listOf(),
    )

    val TRACKERS_PROTECTED_NONE_BLOCKED = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION_SUBSTRING,
        value = getStringResource(R.string.protection_panel_banner_protected_no_blocked_trackers_description),
        description = "Trackers: protected, none blocked",
        groups = listOf(),
    )

    // ── Connection / site security ───────────────────────────────────────────
    val CONNECTION_SECURE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.connection_security_panel_secure),
        description = "Connection: secure",
        groups = listOf(),
    )

    val CONNECTION_VERIFIED_BY = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT_SUBSTRING,
        value = "Verified by",
        description = "Connection: verified-by line",
        groups = listOf(),
    )

    val CONNECTION_NOT_SECURE = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_TEXT,
        value = getStringResource(R.string.connection_security_panel_not_secure),
        description = "Connection: not secure",
        groups = listOf(),
    )

    // ── Panel chrome ─────────────────────────────────────────────────────────
    val PRIVACY_SETTINGS_LINK = Selector(
        strategy = SelectorStrategy.COMPOSE_BY_CONTENT_DESCRIPTION,
        value = "Privacy Settings Links available",
        description = "Privacy settings hyperlink",
        groups = listOf(),
    )

    val all = listOf(
        CLEAR_COOKIES_AND_SITE_DATA_BUTTON,
        CLEAR_COOKIES_AND_SITE_DATA_DIALOG_TITLE,
        CLEAR_COOKIES_AND_SITE_DATA_DIALOG_DESCRIPTION(),
        CLEAR_COOKIES_AND_SITE_DATA_DIALOG_CLEAR_BUTTON,
        CLEAR_COOKIES_AND_SITE_DATA_DIALOG_CANCEL_BUTTON,
        WEBSITE_TITLE(),
        WEBSITE_URL(),
        ETP_BANNER_PROTECTED_TITLE,
        ETP_BANNER_NOT_SECURE_TITLE,
        ETP_BANNER_NOT_SECURE_DESCRIPTION,
        ETP_BANNER_NOT_PROTECTED_TITLE,
        ETP_BANNER_NOT_PROTECTED_DESCRIPTION,
        ETP_TOGGLE_LABEL,
        ETP_TOGGLE_ENABLED_DESCRIPTION,
        ETP_TOGGLE_DISABLED_DESCRIPTION,
        ETP_TOGGLE_ON,
        ETP_TOGGLE_OFF,
        TRACKERS_DISABLED_NONE_BLOCKED,
        TRACKERS_BLOCKED_ON_SITE,
        TRACKERS_PROTECTED_NONE_BLOCKED,
        CONNECTION_SECURE,
        CONNECTION_VERIFIED_BY,
        CONNECTION_NOT_SECURE,
        PRIVACY_SETTINGS_LINK,
    )
}
