/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.webcompat.testdata

import org.json.JSONArray
import org.json.JSONObject

object WebCompatTestData {

    val basicDataJson = """
        {
          "antitracking": {
            "blockList": {
              "value": "basic"
            },
            "blockedOrigins": {
              "isTabSpecific": true,
              "value": ["https://blockedUrlExample.com"]
            },
            "btpHasPurgedSite": {
              "isTabSpecific": true,
              "value": false
            },
            "etpCategory": {
              "value": "standard"
            },
            "hasMixedActiveContentBlocked": {
              "isTabSpecific": true,
              "value": false
            },
            "hasMixedDisplayContentBlocked": {
              "isTabSpecific": true,
              "value": false
            },
            "hasTrackingContentBlocked": {
              "isTabSpecific": true,
              "value": false
            },
            "isPrivateBrowsing": {
              "isTabSpecific": true,
              "value": false
            }
          },
          "app": {
            "applicationName": {
              "value": "testApplicationName"
            },
            "buildId": {
              "value": "testBuildId"
            },
            "defaultLocales": {
              "value": ["en-CA", "en-US"]
            },
            "defaultUseragentString": {
              "value": "testDefaultUserAgent"
            },
            "fissionEnabled": {
              "value": false
            },
            "version": {
              "value": "testVersion"
            }
          },
          "browserInfo": {
            "addons": {
              "value": [
                {
                  "id": "id.temp",
                  "name": "name1",
                  "temporary": true,
                  "version": "version1"
                }, {
                  "id": "id.perm",
                  "name": "name2",
                  "temporary": false,
                  "version": "version2"
                }
              ]
            },
            "experiments": {
              "value": []
            }
          },
          "frameworks": {
            "isTabSpecific": true,
            "fastclick": {
              "value": true
            },
            "marfeel": {
              "value": true
            },
            "mobify": {
              "value": true
            }
          },
          "graphics": {
            "devices": {
              "value": [
                { "id": "device1" },
                { "id": "device2" },
                { "id": "device3" }
              ]
            },
            "devicePixelRatio": {
              "value": 1.5
            },
            "drivers": {
              "value": [
                { "id": "driver1" },
                { "id": "driver2" },
                { "id": "driver3" }
              ]
            },
            "features": {
              "value": {
                "id": "feature1"
              }
            },
            "hasTouchScreen": {
              "value": true
            },
            "monitors": {
              "value": [
                { "id": "monitor1" },
                { "id": "monitor2" },
                { "id": "monitor3" }
              ]
            }
          },
          "prefs": {
            "cookieBehavior": {
              "value": 1
            },
            "forcedAcceleratedLayers": {
              "value": false
            },
            "globalPrivacyControlEnabled": {
              "value": false
            },
            "installtriggerEnabled": {
              "value": false
            },
            "opaqueResponseBlocking": {
              "value": false
            },
            "resistFingerprintingEnabled": {
              "value": false
            },
            "softwareWebrender": {
              "value": false
            },
            "thirdPartyCookieBlockingEnabled": {
              "value": false
            },
            "thirdPartyCookieBlockingEnabledInPbm": {
              "value": false
            }
          },
          "system": {
            "isTablet": {
              "value": false
            },
            "memory": {
              "value": 1
            },
            "osArchitecture": {
              "value": "testOSArchitecture"
            },
            "osName": {
              "value": "testOSName"
            },
            "osVersion": {
              "value": "testOSVersion"
            }
          },
          "tabInfo": {
            "isTabSpecific": true,
            "languages": {
              "value": ["en-CA", "en-US"]
            },
            "screenshot": {
              "value": "testScreenshot"
            },
            "url": {
              "value": "https://www.mozilla.org"
            },
            "useragentString": {
              "value": "testUserAgent"
            }
          }
        }
    """.trimIndent()

    /**
     * Creates a pre-fabbed JSONObject which we use in multiple places in the tests.
     *
     * @param blockedOrigins an override for blockedOrigins
     * @return the JSONObject to use in the tests
     */
    fun createTestObject(blockedOrigins: List<String> = listOf("https://blockedUrlExample.com")): JSONObject? =
        JSONObject(basicDataJson).apply {
            put(
                "antitracking",
                optJSONObject("antitracking")?.apply {
                    put(
                        "blockedOrigins",
                        optJSONObject("blockedOrigins")?.apply {
                            put("value", JSONArray(blockedOrigins))
                        },
                    )
                },
            )
        }
}
