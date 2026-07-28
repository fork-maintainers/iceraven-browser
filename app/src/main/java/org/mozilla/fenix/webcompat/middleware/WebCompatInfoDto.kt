/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.webcompat.middleware

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * WebCompat data wrapper class.
 *
 * @property antitracking Key anti-tracking data.
 * @property app Key build information for Firefox.
 * @property browserInfo Profile info such as addons and experiments.
 * @property frameworks Which website frameworks were seen on the current tab.
 * @property graphics Key data about Firefox's graphical capabilities.
 * @property prefs Values of key about:config preferences.
 * @property system Key information about the user's device or operating system.
 * @property tabInfo Key information about the tab being reported.
 */
@Serializable
data class WebCompatInfoDto(
    val antitracking: WebCompatAntiTrackingDto,
    val app: WebCompatAppDto,
    val browserInfo: WebCompatBrowserInfoDto,
    val frameworks: WebCompatFrameworksDto,
    val graphics: WebCompatGraphicsDto,
    val prefs: WebCompatPrefsDto,
    val system: WebCompatSystemDto,
    val tabInfo: WebCompatTabInfoDto,
) {
    /**
     * Common wrapper/format for values from getBrokenSiteReport
     *
     * @property value The actual value.
     * @property glean Which glean object to send this value as in reports.
     * @property doNotPreview Whether to include this datum on the report preview.
     * @property isTabSpecific Whether this datum is specific to the tab being reported.
     */
    @Serializable
    data class WebCompatObjectDto<T>(
      val value: T,
      val glean: String? = null,
      val doNotPreview: Boolean? = null,
      val isTabSpecific: Boolean? = null,
    )

    /**
     * WebCompat anti-tracking data associated with the current tab.
     *
     * @property isTabSpecific Whether this group of data is specific to the tab's origin.
     * @property blockList The blocklist string.
     * @property blockedOrigins The list of URLs currently blocked by ETP.
     * @property btpHasPurgedSite Whether the current tab has recently been purged by Bounce Tracking Protection.
     * @property etpCategory The current ETP category.
     * @property hasMixedActiveContentBlocked Whether the current tab has mixed active content blocked.
     * @property hasMixedDisplayContentBlocked Whether the current tab has mixed display content blocked.
     * @property hasTrackingContentBlocked Whether the current tab has tracking content blocked.
     * @property isPrivateBrowsing Whether the current tab is in private browsing mode.
     */
    @Serializable
    data class WebCompatAntiTrackingDto(
        val isTabSpecific: Boolean? = null,
        val blockList: WebCompatObjectDto<String>,
        val blockedOrigins: WebCompatObjectDto<List<String>>,
        val btpHasPurgedSite: WebCompatObjectDto<Boolean>,
        val etpCategory: WebCompatObjectDto<String>,
        val hasMixedActiveContentBlocked: WebCompatObjectDto<Boolean>,
        val hasMixedDisplayContentBlocked: WebCompatObjectDto<Boolean>,
        val hasTrackingContentBlocked: WebCompatObjectDto<Boolean>,
        val isPrivateBrowsing: WebCompatObjectDto<Boolean>,
    )

    /**
     * WebCompat app data.
     *
     * @property isTabSpecific Whether this group of data is specific to the tab's origin.
     * @property applicationName The name of this browser.
     * @property buildId The Firefox build ID.
     * @property defaultLocales The locales data.
     * @property defaultUseragentString The default useragent string used by this browser.
     * @property fissionEnabled Whether Fission is enabled.
     * @property version The Firefox version.
     */
    @Serializable
    data class WebCompatAppDto(
        val isTabSpecific: Boolean? = null,
        val applicationName: WebCompatObjectDto<String>,
        val buildId: WebCompatObjectDto<String>,
        val defaultLocales: WebCompatObjectDto<List<String>>,
        val defaultUseragentString: WebCompatObjectDto<String>,
        val fissionEnabled: WebCompatObjectDto<Boolean>,
        val version: WebCompatObjectDto<String>,
    )

    /**
     * WebCompat browser data.
     * Note: experiment info contained in this JSON is ignored; we must pull it in from elsewhere.
     *
     * @property isTabSpecific Whether this group of data is specific to the tab's origin.
     * @property addons Info about active add-ons.
     */
    @Serializable
    data class WebCompatBrowserInfoDto(
        val isTabSpecific: Boolean? = null,
        val addons: WebCompatObjectDto<List<AddonDto>>,
    ) {
        /**
         * WebCompat addon data.
         *
         * @property id The addon's id.
         * @property name The addon's name.
         * @property temporary Boolean, whether the add-on is temporarily installed.
         * @property version The addon's version.
         */
        @Serializable
        data class AddonDto(
            val id: String,
            val name: String,
            val temporary: Boolean,
            val version: String,
        )
    }

    /**
     * WebCompat frameworks data.
     *
     * @property isTabSpecific Whether this group of data is specific to the tab's origin.
     * @property fastclick Whether the FastClick web library was detected on the current tab.
     * @property marfeel Whether the Marfeel web framework was detected on the current tab.
     * @property mobify Whether the Mobify web framework was detected on the original tab.
     */
    @Serializable
    data class WebCompatFrameworksDto(
        val isTabSpecific: Boolean? = null,
        val fastclick: WebCompatObjectDto<Boolean>,
        val marfeel: WebCompatObjectDto<Boolean>,
        val mobify: WebCompatObjectDto<Boolean>,
    )

    /**
     * WebCompat graphics data.
     *
     * @property isTabSpecific Whether this group of data is specific to the tab's origin.
     * @property devices The devices data, with vendorID and deviceID.
     * @property devicePixelRatio The value of devicePixelRatio on the top browser window.
     * @property drivers The drivers data, with renderer and version.
     * @property features The features data.
     * @property hasTouchScreen Whether the device has a touch screen.
     * @property monitors The monitors data, with screenWidth, screenHeight, and scale.
     */
    @Serializable
    data class WebCompatGraphicsDto(
        val isTabSpecific: Boolean? = null,
        val devices: WebCompatObjectDto<JsonArray?>,
        val devicePixelRatio: WebCompatObjectDto<Double>,
        val drivers: WebCompatObjectDto<JsonArray>,
        val features: WebCompatObjectDto<JsonObject?>,
        val hasTouchScreen: WebCompatObjectDto<Boolean?>,
        val monitors: WebCompatObjectDto<JsonArray?>,
    )

    /**
     * WebCompat prefs data.
     *
     * @property isTabSpecific Whether this group of data is specific to the tab's origin.
     * @property cookieBehavior Value of network.cookie.cookieBehavior.
     * @property forcedAcceleratedLayers Value of layers.acceleration_force.enabled.
     * @property globalPrivacyControlEnabled Value of privacy.globalprivacycontrol.enabled.
     * @property installtriggerEnabled Value of extensions.InstallTrigger.enabled.
     * @property opaqueResponseBlocking Value of browser.opaqueResponseBlocking.
     * @property resistFingerprintingEnabled Value of privacy.resistFingerprinting.
     * @property softwareWebrender Value of gfx.webrender.software.
     * @property thirdPartyCookieBlockingEnabled Value of network.cookie.cookieBehavior.optInPartitioning.
     * @property thirdPartyCookieBlockingEnabledInPbm Value of network.cookie.cookieBehavior.optInPartitioning.pbmode.
     */
    @Serializable
    data class WebCompatPrefsDto(
        val isTabSpecific: Boolean? = null,
        val cookieBehavior: WebCompatObjectDto<Long>,
        val forcedAcceleratedLayers: WebCompatObjectDto<Boolean>,
        val globalPrivacyControlEnabled: WebCompatObjectDto<Boolean>,
        val installtriggerEnabled: WebCompatObjectDto<Boolean>,
        val opaqueResponseBlocking: WebCompatObjectDto<Boolean>,
        val resistFingerprintingEnabled: WebCompatObjectDto<Boolean>,
        val softwareWebrender: WebCompatObjectDto<Boolean>,
        val thirdPartyCookieBlockingEnabled: WebCompatObjectDto<Boolean>,
        val thirdPartyCookieBlockingEnabledInPbm: WebCompatObjectDto<Boolean>,
    )

    /**
     * WebCompat system data.
     *
     * @property isTabSpecific Whether this group of data is specific to the tab's origin.
     * @property isTablet Whether the device is a tablet.
     * @property memory The amount of RAM the device has.
     * @property osArchitecture The operating system's architecture.
     * @property osName The operating system's name.
     * @property osVersion The operating system's version.
     */
    @Serializable
    data class WebCompatSystemDto(
        val isTabSpecific: Boolean? = null,
        val isTablet: WebCompatObjectDto<Boolean>,
        val memory: WebCompatObjectDto<Long>,
        val osArchitecture: WebCompatObjectDto<String>,
        val osName: WebCompatObjectDto<String>,
        val osVersion: WebCompatObjectDto<String>,
    )

    /**
     * WebCompat tab data.
     *
     * @property isTabSpecific Whether this group of data is specific to the tab's origin.
     * @property languages navigator.languages presented to the site.
     * @property screenshot A screenshot of the tab.
     * @property url The document.location of the top frame on the tab.
     * @property useragentString The UA string actually presented to the site.
     */
    @Serializable
    data class WebCompatTabInfoDto(
        val isTabSpecific: Boolean? = null,
        val languages: WebCompatObjectDto<List<String>>,
        val screenshot: WebCompatObjectDto<String?>,
        val url: WebCompatObjectDto<String>,
        val useragentString: WebCompatObjectDto<String>,
    )

    companion object {

        private const val TAB_SPECIFIC_KEY = "isTabSpecific"

        private val JsonObject.isTabSpecific: Boolean
            get() = this.boolean(TAB_SPECIFIC_KEY)

        private fun JsonObject.boolean(key: String): Boolean = this[key]?.jsonPrimitive?.booleanOrNull == true

        /**
         * Adds a WebCompatInfoDto's contents to a JSON object meant for Report Broken Site preview feature.
         *
         * @param webCompatInfoDto the WebCompatInfoDto to add
         * @param noTabSpecificData whether or not to avoid tab-specific data
         */
        fun JsonObject.addWebCompatInfo(
            webCompatInfoDto: WebCompatInfoDto,
            noTabSpecificData: Boolean = false,
        ): JsonObject {
            // Filter out the screenshot and other not-to-be-previewed data, as well as
            // any data which is tab-specific if the user has changed the URL enough to
            // make it unlikely to be the URL for the tab we recorded.
            val webCompatJSON = Json.encodeToJsonElement(serializer(), webCompatInfoDto).jsonObject

            val webCompatPreview = webCompatJSON.mapNotNull { (groupName, groupElement) ->
                    val items = groupElement.jsonObject
                    if (noTabSpecificData && items.isTabSpecific) {
                        return@mapNotNull null
                    }

                    val values = items
                        .filterKeys { it != TAB_SPECIFIC_KEY }
                        .filterNot { (_, itemElement) ->
                            val item = itemElement.jsonObject
                            val doNotPreview = item.boolean("doNotPreview")
                            doNotPreview || (noTabSpecificData && item.isTabSpecific)
                        }
                        .mapValues { (_, itemElement) ->
                            itemElement.jsonObject["value"] ?: JsonNull
                        }

                    groupName to JsonObject(values)
                }.toMap()

            return JsonObject(this + webCompatPreview)
         }
    }
}
