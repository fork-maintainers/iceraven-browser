/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.core

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiObject2

/**
 * A backend-agnostic handle to a single located UI element.
 *
 * Once a selector resolves to one of these, every single-element verb stays on this boundary. Backend-specific actions
 * and state queries unwrap it inside the core rather than leaking Compose, Espresso, or UiAutomator objects to callers.
 *
 * Lives in the `core` package (split out of BasePage) so it can be shared and evolved independently.
 */
sealed interface UiElement {
    fun exists(): Boolean

    fun isDisplayed(): Boolean

    fun click()

    /** The element's visible text, or null if the backend exposes none. */
    fun text(): String?

    companion object {
        /** Wrap a raw located node - whatever backend produced it - into the facade. */
        fun wrap(any: Any?): UiElement? =
            when (any) {
                is UiElement -> any
                is SemanticsNodeInteraction -> ComposeUiElement(any)
                is ViewInteraction -> EspressoUiElement(any)
                is UiObject -> UiObjectUiElement(any)
                is UiObject2 -> UiObject2UiElement(any)
                else -> null
            }
    }
}

internal class ComposeUiElement(val node: SemanticsNodeInteraction) : UiElement {
    override fun exists(): Boolean =
        try {
            node.assertExists()
            true
        } catch (_: AssertionError) {
            false
        }

    override fun isDisplayed(): Boolean =
        try {
            node.assertExists()
            node.assertIsDisplayed()
            true
        } catch (_: AssertionError) {
            false
        }

    override fun click() {
        node.assertExists()
        node.assertIsDisplayed()
        node.performClick()
    }

    override fun text(): String? = runCatching {
        val config = node.fetchSemanticsNode().config
        config.getOrNull(SemanticsProperties.EditableText)?.text
            ?: config.getOrNull(SemanticsProperties.Text)?.joinToString(separator = "") { it.text }
    }
        .getOrNull()
}

internal class EspressoUiElement(val interaction: ViewInteraction) : UiElement {
    override fun isDisplayed(): Boolean =
        try {
            interaction.check(matches(ViewMatchers.isDisplayed()))
            true
        } catch (_: Throwable) {
            false
        }

    // Espresso has no cheap "exists but not displayed" check; approximate exists as displayed
    // (matches the previous BasePage behavior for ViewInteraction).
    override fun exists(): Boolean = isDisplayed()

    override fun click() {
        interaction.perform(ViewActions.click())
    }

    // No caller reads text through Espresso.
    override fun text(): String? = null
}

internal class UiObjectUiElement(val obj: UiObject) : UiElement {
    override fun exists(): Boolean = obj.exists()

    override fun isDisplayed(): Boolean = obj.exists()

    override fun click() {
        if (!obj.exists()) throw AssertionError("UiObject does not exist")
        if (!obj.click()) throw AssertionError("Failed to click UiObject")
    }

    override fun text(): String? = runCatching { obj.text }.getOrNull()
}

internal class UiObject2UiElement(val obj: UiObject2) : UiElement {
    override fun exists(): Boolean = runCatching { !obj.visibleBounds.isEmpty }.getOrDefault(false)

    override fun isDisplayed(): Boolean = exists()

    override fun click() {
        obj.click()
    }

    override fun text(): String? = runCatching { obj.text }.getOrNull()
}

internal fun UiElement.backend(): Any =
    when (this) {
        is ComposeUiElement -> node
        is EspressoUiElement -> interaction
        is UiObjectUiElement -> obj
        is UiObject2UiElement -> obj
    }
