/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

/* globals browser */

"use strict";

// Report result back to ExtensionPrivateBrowsingTest.kt
document.body.textContent = "popup-pbm=" + browser.extension.inIncognitoContext;

// P.S. page.html reports results via the title, but we cannot use the same
// mechanism for extension popups because the title is not shown. There is a
// bar above the popup content, showing the extension name. The test can read
// the page's content, so we use that instead.
