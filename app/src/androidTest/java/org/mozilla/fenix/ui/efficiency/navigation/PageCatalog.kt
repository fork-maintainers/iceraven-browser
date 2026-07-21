/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.navigation

import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.PageContext
import java.lang.reflect.Field

object PageCatalog {

    data class PageRef(
        val propertyName: String,
        val getter: PageContext.() -> BasePage,
    )

    fun discoverPages(): List<PageRef> {
        val refs = mutableListOf<PageRef>()

        for (field in PageContext::class.java.declaredFields) {
            if (!BasePage::class.java.isAssignableFrom(field.type)) continue

            refs += buildPageRef(field)
        }

        return refs.sortedBy { it.propertyName }
    }

    private fun buildPageRef(field: Field): PageRef {
        field.isAccessible = true

        return PageRef(
            propertyName = field.name,
            getter = {
                field.isAccessible = true
                val value = field.get(this)
                require(value is BasePage) {
                    "Expected BasePage for field '${field.name}', got ${value?.javaClass}"
                }
                value
            },
        )
    }
}
