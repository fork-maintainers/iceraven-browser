/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.devtools

import org.mozilla.fenix.ui.efficiency.generation.interaction.InteractionTestPlanner

object InteractionCaseGenerator {

    private const val TAG = "InteractionGenerator"

    /**
     * Builds the interaction boilerplate report without emitting it anywhere, so a future
     * consumer (a debug UI, an export, etc.) can reuse the same computation
     * [logInteractionCaseBoilerplate] uses.
     */
    fun buildInteractionCaseBoilerplateReport(): DevToolReport {
        val cases = InteractionTestPlanner.buildInteractionCases()

        return buildBoilerplateReport(
            header = "Generated ${cases.size} interaction case templates:",
            items = cases,
        ) { case ->
            """
            // pageName=${case.pageName}, property=${case.pagePropertyName}, paths=${case.pathCount}
            // interaction=${case.interactionSelectorName}
            // description=${case.interactionDescription}
            // expectedGroup=${case.expectedGroup}
            // expectedSelectors=${case.expectedSelectorNames}

            InteractionCase(
                label = "${case.pageName} - ${case.interactionSelectorName}",
                testRailId = "TBD",
                page = { ${case.pagePropertyName} },
                interactionSelectorName = "${case.interactionSelectorName}",
                expectedSelectorNames = listOf(
                    ${case.expectedSelectorNames.joinToString { "\"$it\"" }}
                ),
                state = runState.ifBlank { "Interaction Factory" },
            ),
            """.trimIndent()
        }
    }

    fun logInteractionCaseBoilerplate() {
        logReport(TAG, buildInteractionCaseBoilerplateReport())
    }
}
