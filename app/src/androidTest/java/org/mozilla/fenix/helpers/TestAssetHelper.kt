/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.helpers

import android.net.Uri
import androidx.core.net.toUri
import java.util.concurrent.TimeUnit
import mockwebserver3.MockWebServer

/** Helper for hosting web pages locally for testing purposes. */
object TestAssetHelper {
    val waitingTime: Long = TimeUnit.SECONDS.toMillis(15)
    val waitingTimeLong = TimeUnit.SECONDS.toMillis(25)
    val waitingTimeShort: Long = TimeUnit.SECONDS.toMillis(3)
    val waitingTimeVeryShort: Long = TimeUnit.SECONDS.toMillis(1)

    data class TestAsset(val url: Uri, val content: String, val title: String)

    /**
     * Hosts 3 simple websites, found at androidTest/assets/pages/generic[1|2|3].html Returns a list of TestAsset, which
     * can be used to navigate to each and assert that the correct information is being displayed.
     *
     * Content for these pages all follow the same pattern. See [generic1.html] for content implementation details.
     */
    val MockWebServer.genericAssets
        get() = (1..4).map { getGenericAsset(it) }

    fun MockWebServer.getGenericAsset(pageNum: Int) =
        createTestAsset(
            path = "pages/generic$pageNum.html",
            content = "Page content: $pageNum",
            title = "Test_Page_$pageNum",
        )

    val MockWebServer.loremIpsumAsset
        get() =
            createTestAsset(
                path = "pages/lorem-ipsum.html",
                content = "Page content: lorem ipsum",
                title =
                    "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt",
            )

    val MockWebServer.tallPageAsset
        get() =
            createTestAsset(
                path = "pages/tallPage.html",
                content = "Page content: tall page",
                title = "Tall page",
            )

    val MockWebServer.refreshAsset
        get() =
            createTestAsset(
                path = "pages/refresh.html",
                content = "Page content: refresh",
            )

    val MockWebServer.uuidPage
        get() =
            createTestAsset(
                path = "pages/basic_nav_uuid.html",
                content = "Page content: basic_nav_uuid",
            )

    val MockWebServer.enhancedTrackingProtectionAsset
        get() =
            createTestAsset(
                path = "pages/trackingPage.html",
                content = "Level 1 (Basic) List",
            )

    val MockWebServer.imageAsset
        get() = createTestAsset("resources/rabbit.jpg")

    val MockWebServer.pdfFormAsset
        get() = createTestAsset("resources/pdfForm.pdf")

    val MockWebServer.downloadPageAsset
        get() =
            createTestAsset(
                path = "pages/downloads.html",
                title = "Download_Test_Page",
            )

    val MockWebServer.saveLoginAsset
        get() = createTestAsset("pages/password.html")

    val MockWebServer.signupFormAsset
        get() = createTestAsset("pages/signupForm.html")

    val MockWebServer.addressFormAsset
        get() = createTestAsset("pages/addressForm.html")

    val MockWebServer.appLinksRedirectAsset
        get() = createTestAsset("pages/appLinksLinks.html")

    val MockWebServer.creditCardFormAsset
        get() = createTestAsset("pages/creditCardForm.html")

    val MockWebServer.htmlControlsFormAsset
        get() = createTestAsset("pages/htmlControls.html")

    val MockWebServer.externalLinksAsset
        get() = createTestAsset("pages/externalLinks.html")

    val MockWebServer.audioPageAsset
        get() =
            createTestAsset(
                path = "pages/audioMediaPage.html",
                title = "Audio_Test_Page",
                content = "Page content: audio player",
            )

    val MockWebServer.videoPageAsset
        get() =
            createTestAsset(
                path = "pages/videoMediaPage.html",
                title = "Video_Test_Page",
                content = "Page content: video player",
            )

    val MockWebServer.mutedVideoPageAsset
        get() =
            createTestAsset(
                path = "pages/mutedVideoPage.html",
                title = "Muted_Video_Test_Page",
                content = "Page content: muted video player",
            )

    val MockWebServer.gcpTestAsset
        get() = createTestAsset("pages/global_privacy_control.html")

    val MockWebServer.textFragmentAsset
        get() =
            createTestAsset(
                path = "pages/textFragment.html",
                title = "Text_Fragment",
            )

    val MockWebServer.promptAsset
        get() =
            createTestAsset(
                path = "pages/beforeUnload.html",
                title = "BeforeUnload_Test_Page",
            )

    val MockWebServer.firstForeignWebPageAsset
        get() =
            createTestAsset(
                path = "pages/firstForeignWebPage.html",
                title = "Page_de_test_FR_1",
                content = "Article du jour",
            )

    val MockWebServer.secondForeignWebPageAsset
        get() =
            createTestAsset(
                path = "pages/secondForeignWebPage.html",
                title = "Page_de_test_FR_2",
                content = "Mot du jour",
            )

    val MockWebServer.storageCheckPageAsset
        get() = createTestAsset("pages/storage_check.html")

    val MockWebServer.storageWritePageAsset
        get() = createTestAsset("pages/storage_write.html")

    val MockWebServer.navigablePageStartAsset
        get() = createTestAsset("pages/linkNavigationStart.html")

    val MockWebServer.navigablePageTargetAsset
        get() = createTestAsset("pages/linkNavigationTarget.html")

    private fun MockWebServer.createTestAsset(
        path: String,
        content: String = "",
        title: String = "",
    ) =
        TestAsset(
            url(path).toString().toUri(),
            content,
            title,
        )

    val MockWebServer.articleSummaryAsset
        get() =
            createTestAsset(
                path = "pages/article-summary.html",
                content = "Page content: article summary",
                title = "Article for Testing Summarization Feature",
            )
}
