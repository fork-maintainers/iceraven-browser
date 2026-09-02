package org.mozilla.fenix.ui.efficiency.tests

import org.junit.Test
import org.mozilla.fenix.R
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.helpers.TestAssetHelper.loremIpsumAsset
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.util.FRENCH_FOLLOW_DEVICE_LANGUAGE_OPTION
import org.mozilla.fenix.ui.util.FRENCH_LANGUAGE_HEADER
import org.mozilla.fenix.ui.util.ROMANIAN_LANGUAGE_HEADER

class SettingsGeneralTest : BaseTest() {
    private val mockWebServer get() = fenixTestRule.mockWebServer

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/344213
    @SmokeTest
    @Test
    fun verifyFontSizingChangeTest() {
        val webpage = mockWebServer.loremIpsumAsset.url
        val textSizePercentage = 180

        on.settingsAccessibility.navigateToPage()
            .verifyFontSizingMenuItems(
                isTheAutomaticFontSizingToggleChecked = true,
                isTheFontSizingSliderEnabled = false,
                isTheZoomOnAllWebsitesToggleChecked = false,
            )
            .clickAutomaticFontSizingToggle()
            .verifyFontSizingMenuItems(
                isTheAutomaticFontSizingToggleChecked = false,
                isTheFontSizingSliderEnabled = true,
                isTheZoomOnAllWebsitesToggleChecked = false,
            )
            .changeTextSizeSlider(textSizePercentage)
            .verifyTextSizePercentage(textSizePercentage)

        on.browserPage.navigateToPage(webpage.toString())
            .verifyTextSizeOnWebsite(textSizePercentage)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/243583
    @SmokeTest
    @Test
    fun changeDefaultBrowserSetting() {
        on.settings.navigateToPage()
            .verifyDefaultBrowserToggle(false)
            .clickDefaultBrowserSwitch()
            .verifyAndroidDefaultAppsMenuAppears()

        // Dismiss the system default-apps request.
        mDevice.pressBack()
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/516079
    @SmokeTest
    @Test
    fun setAppLanguageDifferentThanSystemLanguageTest() {
        val enLanguageHeaderText = getStringResource(R.string.preferences_language)

        on.settingsLanguage.navigateToPage()
        on.settingsLanguage
            .selectLanguage("Romanian")
            .verifyLanguageSettingHeaderIsTranslated(ROMANIAN_LANGUAGE_HEADER)
        on.settingsLanguage
            .selectLanguage("Français")
            .verifyLanguageSettingHeaderIsTranslated(FRENCH_LANGUAGE_HEADER)
        on.settingsLanguage
            .selectLanguage(FRENCH_FOLLOW_DEVICE_LANGUAGE_OPTION)
            .verifyLanguageSettingHeaderIsTranslated(enLanguageHeaderText)
    }
}
