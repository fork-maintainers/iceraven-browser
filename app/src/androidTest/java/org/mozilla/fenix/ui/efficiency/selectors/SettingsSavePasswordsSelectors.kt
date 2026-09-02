/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

object SettingsSavePasswordsSelectors {

    val ASK_TO_SAVE_OPTION = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_TEXT,
        value = "Ask to save",
        description = "Ask to save option",
        groups = listOf("requiredForPage"),
    )

    val NEVER_SAVE_OPTION = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_TEXT,
        value = "Never save",
        description = "Never save option",
        groups = listOf(),
    )

    val all = listOf(
        ASK_TO_SAVE_OPTION,
        NEVER_SAVE_OPTION,
    )
}
