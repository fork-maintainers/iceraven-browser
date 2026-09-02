/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

// The reader-view appearance controls are the legacy Android View bar from
// mozilla.components.feature.readerview (layout mozac_feature_readerview_view.xml). They are matched by
// their res-id through UIAutomator, NOT Espresso: the bar is shown over an active GeckoView (the reader
// article keeps rendering), so Espresso's onView never reaches an idle main thread and its assertions
// time out even though the views are present and displayed — a UIAutomator res-id lookup reads the
// device accessibility tree directly and is unaffected. UIAUTOMATOR2_BY_RES (UiObject2) rather than
// UIAUTOMATOR_WITH_RES_ID (UiObject): UiObject.click() intermittently returned false ("Failed to click")
// on the font-size buttons after a preceding tap re-laid-out the bar, whereas UiObject2's gesture-based
// click is reliable. The font/color buttons are AppCompatRadioButtons, so their active state is asserted
// with mozVerifyElementIsChecked (UiObject2.isChecked). Font size has no UI state at all — it lives only
// in SharedPreferences — so it is asserted via ReaderViewPage.verifyFontSize.
object ReaderViewSelectors {

    val APPEARANCE_FONT_GROUP = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR2_BY_RES,
        value = "mozac_feature_readerview_font_group",
        description = "Reader view font group",
        groups = listOf("requiredForPage", "readerViewControls"),
    )

    val APPEARANCE_FONT_SANS_SERIF = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR2_BY_RES,
        value = "mozac_feature_readerview_font_sans_serif",
        description = "Reader view sans serif font button",
        groups = listOf("readerViewControls"),
    )

    val APPEARANCE_FONT_SERIF = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR2_BY_RES,
        value = "mozac_feature_readerview_font_serif",
        description = "Reader view serif font button",
        groups = listOf("readerViewControls"),
    )

    val APPEARANCE_FONT_SIZE_INCREASE = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR2_BY_RES,
        value = "mozac_feature_readerview_font_size_increase",
        description = "Reader view increase font size button",
        groups = listOf("readerViewControls"),
    )

    val APPEARANCE_FONT_SIZE_DECREASE = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR2_BY_RES,
        value = "mozac_feature_readerview_font_size_decrease",
        description = "Reader view decrease font size button",
        groups = listOf("readerViewControls"),
    )

    val APPEARANCE_COLOR_GROUP = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR2_BY_RES,
        value = "mozac_feature_readerview_color_scheme_group",
        description = "Reader view color scheme group",
        groups = listOf("readerViewControls"),
    )

    val APPEARANCE_COLOR_DARK = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR2_BY_RES,
        value = "mozac_feature_readerview_color_dark",
        description = "Reader view dark color scheme button",
        groups = listOf("readerViewControls"),
    )

    val APPEARANCE_COLOR_LIGHT = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR2_BY_RES,
        value = "mozac_feature_readerview_color_light",
        description = "Reader view light color scheme button",
        groups = listOf("readerViewControls"),
    )

    val APPEARANCE_COLOR_SEPIA = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR2_BY_RES,
        value = "mozac_feature_readerview_color_sepia",
        description = "Reader view sepia color scheme button",
        groups = listOf("readerViewControls"),
    )

    val all = listOf(
        APPEARANCE_FONT_GROUP,
        APPEARANCE_FONT_SANS_SERIF,
        APPEARANCE_FONT_SERIF,
        APPEARANCE_FONT_SIZE_INCREASE,
        APPEARANCE_FONT_SIZE_DECREASE,
        APPEARANCE_COLOR_GROUP,
        APPEARANCE_COLOR_DARK,
        APPEARANCE_COLOR_LIGHT,
        APPEARANCE_COLOR_SEPIA,
    )
}
