/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

object SettingsSiteSettingsPermissionsSelectors {

    val ASK_TO_ALLOW_RADIO_BUTTON = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_ID,
        value = "ask_to_allow_radio",
        description = "Ask to allow radio button",
        // requiredForPage: the permission detail screen always shows the "Ask to allow" radio, so it is
        // what proves we arrived. The page registers a real nav edge, so without an anchor here
        // navigateToPage() would report success for whatever screen happened to be in front of it.
        groups = listOf("requiredForPage", "askToAllow"),
    )

    val all = listOf(
        ASK_TO_ALLOW_RADIO_BUTTON,
    )
}
