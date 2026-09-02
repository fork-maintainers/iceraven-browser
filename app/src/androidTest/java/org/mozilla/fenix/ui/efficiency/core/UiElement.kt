/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.core

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
 * This is the harness's interaction facade: once a selector has been resolved to one of these, the
 * interaction verbs (mozClick, later mozVerify/mozEnterText/…) act through this interface and never
 * switch on Compose (SemanticsNodeInteraction) vs Espresso (ViewInteraction) vs UiAutomator
 * (UiObject/UiObject2) types themselves. That keeps the per-backend quirks in one place (here) and
 * the verbs thin. Grow this interface (longClick, enterText, …) as verbs migrate onto it.
 *
 * Lives in the `core` package (split out of BasePage) so it can be shared and evolved independently.
 */
interface UiElement {
    fun exists(): Boolean
    fun isDisplayed(): Boolean
    fun click()
}

class ComposeUiElement(private val node: SemanticsNodeInteraction) : UiElement {
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
}

class EspressoUiElement(private val interaction: ViewInteraction) : UiElement {
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

    override fun click() { interaction.perform(ViewActions.click()) }
}

class UiObjectUiElement(private val obj: UiObject) : UiElement {
    override fun exists(): Boolean = obj.exists()
    override fun isDisplayed(): Boolean = obj.exists()
    override fun click() {
        if (!obj.exists()) throw AssertionError("UiObject does not exist")
        if (!obj.click()) throw AssertionError("Failed to click UiObject")
    }
}

class UiObject2UiElement(private val obj: UiObject2) : UiElement {
    override fun exists(): Boolean = true
    override fun isDisplayed(): Boolean = true
    override fun click() { obj.click() }
}
