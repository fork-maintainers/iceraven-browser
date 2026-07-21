/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.generation.behavior

import org.mozilla.fenix.ui.efficiency.navigation.NavigationRegistry
import org.mozilla.fenix.ui.efficiency.navigation.PageCatalog

object BehaviorTestPlanner {
    fun buildBehaviorCasePlans(
        templates: List<BehaviorTemplate> = BehaviorTemplateCatalog.all,
        capabilities: List<BehaviorCapability> = BehaviorCapabilityCatalog.all,
        selectorTemplates: List<SelectorTemplate> = SelectorTemplateCatalog.all,
        contexts: List<BehaviorContextVariant> = BehaviorContextMatrix.variants(),
    ): List<BehaviorCasePlan> {
        val capabilitiesByFeatureEntity = capabilities.groupBy { it.feature to it.entity }
        val pageRefsByProperty = PageCatalog.discoverPages().associateBy { it.propertyName }

        return buildList {
            capabilitiesByFeatureEntity.keys
                .sortedWith(compareBy({ it.first }, { it.second }))
                .forEach { (feature, entity) ->
                    templates.forEach { template ->
                        contexts.forEach { context ->
                            add(
                                buildPlan(
                                    feature = feature,
                                    entity = entity,
                                    template = template,
                                    capabilities = capabilitiesByFeatureEntity.getValue(feature to entity),
                                    selectorTemplates = selectorTemplates,
                                    pageRefsByProperty = pageRefsByProperty,
                                    context = context,
                                ),
                            )
                        }
                    }
                }
        }.sortedWith(compareBy({ it.feature }, { it.entity }, { it.templateId }, { it.context.toString() }))
    }

    private fun buildPlan(
        feature: String,
        entity: String,
        template: BehaviorTemplate,
        capabilities: List<BehaviorCapability>,
        selectorTemplates: List<SelectorTemplate>,
        pageRefsByProperty: Map<String, PageCatalog.PageRef>,
        context: BehaviorContextVariant,
    ): BehaviorCasePlan {
        val missing = mutableListOf<String>()

        val selectedCapabilities = template.requiredOperations.mapNotNull { operation ->
            val match = capabilities.firstOrNull { it.operation == operation }
            if (match == null) {
                missing += "capability:$feature.$entity.$operation"
            }
            match
        }

        selectedCapabilities.forEach { capability ->
            if (pageRefsByProperty[capability.pagePropertyName] == null) {
                missing += "page:${capability.pagePropertyName}"
            }
        }

        val assertionTemplate = selectorTemplates.firstOrNull {
            it.feature == feature &&
                it.entity == entity &&
                it.target == template.assertionTarget
        }

        if (assertionTemplate == null) {
            missing += "selectorTemplate:$feature.$entity.${template.assertionTarget}"
        } else if (pageRefsByProperty[assertionTemplate.pagePropertyName] == null) {
            missing += "page:${assertionTemplate.pagePropertyName}"
        }

        val requiredDataKeys = buildSet {
            selectedCapabilities.forEach { addAll(it.requiredDataKeys) }
            assertionTemplate?.let { addAll(it.requiredDataKeys) }
        }

        val data = BehaviorDataCatalog.buildData(
            feature = feature,
            entity = entity,
            templateId = template.id,
            requiredKeys = requiredDataKeys,
        )

        val pageSequence = buildList {
            selectedCapabilities.forEach { add(it.pagePropertyName.toPageName()) }
            assertionTemplate?.let { add(it.pagePropertyName.toPageName()) }
        }

        val transitions = buildNavigationTransitions(pageSequence)

        transitions.forEach { transition ->
            val parts = transition.split(" -> ")
            val from = parts[0]
            val to = parts[1]
            if (from != to && NavigationRegistry.findAllPaths(from, to).isEmpty()) {
                missing += "navigationPath:$transition"
            }
        }

        val contextDecision = BehaviorContextSupportRegistry.evaluate((context))

        return BehaviorCasePlan(
            label = template.title(feature, entity),
            testRailId = "TBD",
            automationId = template.automationId(feature, entity),
            feature = feature,
            entity = entity,
            templateId = template.id,
            requiredOperations = template.requiredOperations,
            capabilityIds = selectedCapabilities.map { it.id },
            assertionKind = template.assertionKind,
            assertionSelectorTemplateId = assertionTemplate?.id,
            assertionPagePropertyName = assertionTemplate?.pagePropertyName,
            data = data,
            context = context,
            navigationTransitions = transitions,
            missingRequirements = missing.distinct().sorted(),
            skippedReason = contextDecision.skippedReason,
        )
    }

    private fun buildNavigationTransitions(pageSequence: List<String>): List<String> {
        if (pageSequence.isEmpty()) return emptyList()

        val transitions = mutableListOf<String>()
        transitions += "AppEntry -> ${pageSequence.first()}"

        pageSequence.zipWithNext()
            .filter { (from, to) -> from != to }
            .forEach { (from, to) -> transitions += "$from -> $to" }

        return transitions.distinct()
    }

    private fun String.toPageName(): String {
        val name = replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }

        return if (name.endsWith("Page") || name.endsWith("Component")) {
            name
        } else {
            "${name}Page"
        }
    }
}
