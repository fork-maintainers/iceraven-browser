/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.benchmark.utils

import androidx.core.net.toUri

const val TARGET_PACKAGE = "org.mozilla.fenix"
const val DEFAULT_ITERATIONS = 5
// Extra to enable or disable TabsTray animation
const val EXTRA_TAB_TRAY_ANIMATION = "EXTRA_TAB_TRAY_ANIMATION"
// Intent extra to enable or disable TabTray enhancements setting for testing
const val EXTRA_TAB_TRAY_ENHANCEMENTS = "EXTRA_TAB_MANAGER_ENHANCEMENTS"
// Intent extra to force onboarding to show in the benchmark build, where it is otherwise suppressed
const val EXTRA_FORCE_ONBOARDING = "EXTRA_FORCE_ONBOARDING"
val FENIX_HOME_DEEP_LINK = "fenix-nightly://home".toUri()
