/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

/* globals browser */

"use strict";

// Note: This is a MV2 extension with a persistent background page.
// Do not make this a MV3 extension, to avoid the possibility of the background
// suspending and resuming, which would cause this logic to be run twice.

browser.tabs.create({
  url: browser.runtime.getURL("page.html"),
  active: true,
});
