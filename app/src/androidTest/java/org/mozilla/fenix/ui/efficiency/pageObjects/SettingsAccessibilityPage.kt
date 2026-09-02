/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.endsWith
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestHelper.hasCousin
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.navigation.NavigationRegistry
import org.mozilla.fenix.ui.efficiency.navigation.NavigationStep
import org.mozilla.fenix.ui.efficiency.selectors.SettingsAccessibilitySelectors
import org.mozilla.fenix.ui.efficiency.selectors.SettingsSelectors

class SettingsAccessibilityPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "SettingsAccessibilityPage"

    init {
        NavigationRegistry.register(
            from = pageName,
            to = "SettingsPage",
            steps = listOf(NavigationStep.Click(SettingsSelectors.GO_BACK_BUTTON)),
        )
    }

    override fun mozGetSelectorsByGroup(group: String): List<Selector> {
        return SettingsAccessibilitySelectors.all.filter { it.groups.contains(group) }
    }

    override fun navigateToPage(url: String, forceNavigation: Boolean): SettingsAccessibilityPage {
        super.navigateToPage(url, forceNavigation)
        return this
    }

    /**
     * Assert the full font-sizing section against a given state, mirroring the legacy
     * SettingsSubMenuAccessibilityRobot.verifyFontSizingMenuItems: titles/summaries present, both
     * switch toggles in the expected checked state, the slider present at 100% and enabled or not.
     * The toggle checks stay on Espresso's hasCousin(Switch) matcher because each toggle is a
     * SwitchPreferenceCompat whose Switch is a cousin of the title text, not the title itself.
     */
    fun verifyFontSizingMenuItems(
        isTheAutomaticFontSizingToggleChecked: Boolean,
        isTheFontSizingSliderEnabled: Boolean,
        isTheZoomOnAllWebsitesToggleChecked: Boolean,
    ): SettingsAccessibilityPage {
        mozVerify(SettingsAccessibilitySelectors.AUTOMATIC_FONT_SIZING_TITLE)
        mozVerify(SettingsAccessibilitySelectors.AUTOMATIC_FONT_SIZING_SUMMARY)
        verifyToggleChecked(R.string.preference_accessibility_auto_size_2, isTheAutomaticFontSizingToggleChecked)

        mozVerify(SettingsAccessibilitySelectors.FONT_SIZE_TITLE)
        mozVerify(SettingsAccessibilitySelectors.FONT_SIZE_SUBTITLE)
        mozVerify(SettingsAccessibilitySelectors.FONT_SIZE_SLIDER)
        mozVerifyAnyContainsText(SettingsAccessibilitySelectors.FONT_SIZE_SLIDER_VALUE, "100 %")
        if (isTheFontSizingSliderEnabled) {
            mozVerifyElementIsEnabled(SettingsAccessibilitySelectors.FONT_SIZE_SLIDER)
        } else {
            mozVerifyElementIsNotEnabled(SettingsAccessibilitySelectors.FONT_SIZE_SLIDER)
        }

        mozVerify(SettingsAccessibilitySelectors.ZOOM_ON_ALL_WEBSITES_TITLE)
        mozVerify(SettingsAccessibilitySelectors.ZOOM_ON_ALL_WEBSITES_SUMMARY)
        verifyToggleChecked(R.string.preference_accessibility_force_enable_zoom, isTheZoomOnAllWebsitesToggleChecked)
        return this
    }

    fun clickAutomaticFontSizingToggle(): SettingsAccessibilityPage {
        mozClick(SettingsAccessibilitySelectors.AUTOMATIC_FONT_SIZING_TITLE)
        return this
    }

    fun changeTextSizeSlider(seekBarPercentage: Int): SettingsAccessibilityPage {
        mozSetSliderValue(SettingsAccessibilitySelectors.FONT_SIZE_SLIDER, seekBarPercentage.toFloat())
        return this
    }

    fun verifyTextSizePercentage(textSize: Int): SettingsAccessibilityPage {
        mozVerifyAnyContainsText(SettingsAccessibilitySelectors.FONT_SIZE_SLIDER_VALUE, "$textSize %")
        return this
    }

    private fun verifyToggleChecked(titleResId: Int, isChecked: Boolean) {
        onView(withText(titleResId)).check(
            matches(
                hasCousin(
                    allOf(
                        withClassName(endsWith("Switch")),
                        if (isChecked) isChecked() else isNotChecked(),
                    ),
                ),
            ),
        )
    }
}
