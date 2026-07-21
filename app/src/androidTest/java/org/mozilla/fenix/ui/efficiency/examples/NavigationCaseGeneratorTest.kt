/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.examples

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.ui.efficiency.generation.interaction.InteractionCaseGenerator
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.navigation.planning.NavigationCaseGenerator

@RunWith(AndroidJUnit4::class)
class NavigationCaseGeneratorTest : BaseTest() {

    @Test
    fun logPresenceCases() {
        NavigationCaseGenerator.logNavigationCaseBoilerplate(on)
    }

    @Test
    fun logInteractionCases() {
        InteractionCaseGenerator.logInteractionCaseBoilerplate()
    }
}
