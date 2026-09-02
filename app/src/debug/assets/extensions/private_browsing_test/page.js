/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

/* globals browser */

"use strict";

// Report result back via title to ExtensionPrivateBrowsingTest.kt
document.title = "page-pbm=" + browser.extension.inIncognitoContext;

// Help the user to see what has happened. This is not relied upon by the test
// runner (we monitor the title because that is easier than waiting for the tab
// to appear and load).
document.body.textContent = document.title;
