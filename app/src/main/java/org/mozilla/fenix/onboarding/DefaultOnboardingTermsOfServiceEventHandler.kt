/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding

import mozilla.components.support.ktx.kotlin.ifNullOrEmpty
import org.mozilla.fenix.onboarding.view.OnboardingTermsOfServiceEventHandler
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.termsofuse.TOU_VERSION
import org.mozilla.fenix.utils.Settings

/**
 * Default implementation for [OnboardingTermsOfServiceEventHandler].
 */
class DefaultOnboardingTermsOfServiceEventHandler(
    private val telemetryRecorder: OnboardingTelemetryRecorder,
    private val openLink: (String) -> Unit,
    private val showManagePrivacyPreferencesDialog: () -> Unit,
    private val settings: Settings,
    private val startGlean: () -> Unit,
    private val currentTimeMillis: () -> Long = { System.currentTimeMillis() },
) : OnboardingTermsOfServiceEventHandler {

    override fun onTermsOfServiceLinkClicked(url: String) {
        telemetryRecorder.onTermsOfServiceLinkClick()
        openLink(
            url.trim().ifNullOrEmpty {
                SupportUtils.getMozillaPageUrl(SupportUtils.MozillaPage.TERMS_OF_SERVICE)
            },
        )
    }

    override fun onPrivacyNoticeLinkClicked(url: String) {
        telemetryRecorder.onTermsOfServicePrivacyNoticeLinkClick()
        openLink(
            url.trim().ifNullOrEmpty {
                SupportUtils.getMozillaPageUrl(SupportUtils.MozillaPage.PRIVACY_NOTICE)
            },
        )
    }

    override fun onManagePrivacyPreferencesLinkClicked() {
        telemetryRecorder.onTermsOfServiceManagePrivacyPreferencesLinkClick()
        showManagePrivacyPreferencesDialog()
    }

    override fun onAcceptTermsButtonClicked() {
        telemetryRecorder.onTermsOfServiceManagerAcceptTermsButtonClick()
        settings.hasAcceptedTermsOfService = true
        settings.termsOfUseAcceptedVersion = TOU_VERSION
        settings.termsOfUseAcceptedTimeInMillis = currentTimeMillis()
        startGlean()
    }
}
