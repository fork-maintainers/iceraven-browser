/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.termsofuse

import org.mozilla.fenix.termsofuse.store.TermsOfUsePromptRepository

class FakeTermsOfUsePromptRepository(
    private val canShowTermsOfUsePrompt: Boolean = true,
    private val userPostponedAndWithinCooldownPeriod: Boolean = false,
) : TermsOfUsePromptRepository {
    override var isShowingPrompt = false

    override fun canShowTermsOfUsePrompt() = canShowTermsOfUsePrompt

    override fun userPostponedAndWithinCooldownPeriod(currentTimeMillis: Long) =
        userPostponedAndWithinCooldownPeriod

    override fun updateHasAcceptedTermsOfUsePreference() {}
    override fun updateHasPostponedAcceptingTermsOfUsePreference() {}
    override fun updateLastTermsOfUsePromptTimeInMillis() {}
    override fun incrementTermsOfUsePromptDisplayedCount() {}
}
