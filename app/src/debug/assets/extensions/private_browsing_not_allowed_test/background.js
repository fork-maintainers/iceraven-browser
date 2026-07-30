/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

/* globals browser */

"use strict";

// This extension declares incognito:not_allowed, so when private browsing mode is
// enabled browser.tabs.create() is expected to reject. Report the outcome back to
// ExtensionPrivateBrowsingTest.kt via native messaging.
(async () => {
  let message;
  try {
    await browser.tabs.create({ url: "about:blank", active: true });
    message = "tabs.create resolved";
  } catch (e) {
    message = "tabs.create rejected: " + e.message;
  }
  await browser.runtime.sendNativeMessage("testresult", message);
})();
