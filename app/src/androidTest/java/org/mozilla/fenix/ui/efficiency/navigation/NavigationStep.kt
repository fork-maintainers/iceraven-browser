/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.navigation

import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SwipeDirection

sealed class NavigationStep {
    data class Click(val selector: Selector) : NavigationStep()
    data class LongClick(val selector: Selector) : NavigationStep()
    data class ClickIfPresent(val selector: Selector) : NavigationStep()
    data class Swipe(val selector: Selector, val direction: SwipeDirection = SwipeDirection.UP) : NavigationStep()

    // No parameters: BasePage always calls mozOpenNotificationsTray(). It previously took an
    // openNotificationsTrayAction lambda that was never invoked — dead weight, and a trap for anyone
    // who passed a real action expecting it to run.
    object OpenNotificationsTray : NavigationStep()
    data class Action(val action: () -> Unit) : NavigationStep()
    data class EnterText(val selector: Selector) : NavigationStep()
    data class EnterTextValue(val selector: Selector, val text: String) : NavigationStep()
    data class PressEnter(val selector: Selector) : NavigationStep()
    object PressBack : NavigationStep()
    object WaitForIdle : NavigationStep()

    /**
     * Presses back until [selector] is no longer present, bounded by [maxPresses]. Use for
     * multi-screen back-out flows (e.g. nested Settings) where a fixed count of [PressBack] is
     * unreliable because a press issued before the previous transition settles gets swallowed.
     */
    data class PressBackUntilGone(val selector: Selector, val maxPresses: Int = 5) : NavigationStep()
}
