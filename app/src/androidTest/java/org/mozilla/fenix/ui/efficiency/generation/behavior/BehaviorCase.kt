/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.generation.behavior

import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.PageContext

/** Concrete generated behavior test case. */
data class BehaviorCase(
    val label: String,
    val testRailId: String,
    val automationId: String,
    val feature: String,
    val entity: String,
    val templateId: String,
    val capabilities: List<BehaviorCapability>,
    val assertion: ResolvedBehaviorAssertion,
    val data: BehaviorData,
    val context: BehaviorContextVariant,
    val state: String,
    val missingRequirements: List<String> = emptyList(),
) {
    val isRunnable: Boolean get() = missingRequirements.isEmpty()

    override fun toString(): String =
        "$label ($testRailId)${if (state.isNotBlank()) " — $state" else ""}"
}

data class ResolvedBehaviorAssertion(
    val pagePropertyName: String,
    val page: PageContext.() -> BasePage,
    val selectorTemplate: SelectorTemplate,
    val kind: AssertionKind,
) {
    fun verify(runtime: BehaviorRuntime) {
        val pageObj = page(runtime.on)
        pageObj.navigateToPage()

        val selector = selectorTemplate.build(runtime.data)

        when (kind) {
            AssertionKind.PRESENT -> pageObj.mozVerify(selector)
            AssertionKind.ABSENT -> pageObj.mozVerifyElementAbsent(selector)
        }
    }
}
