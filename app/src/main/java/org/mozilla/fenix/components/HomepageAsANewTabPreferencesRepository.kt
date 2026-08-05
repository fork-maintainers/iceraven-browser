/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import org.mozilla.fenix.utils.Settings

/**
 * The repository for managing the homepage as a new tab preference.
 */
interface HomepageAsANewTabPreferencesRepository {

    /**
     * Returns whether the homepage as a new tab preference is enabled.
     */
    fun getHomepageAsANewTabEnabled(): Boolean
}

/**
 * The default implementation of [HomepageAsANewTabPreferencesRepository].
 *
 * @param settings [Settings] used to check the application shared preferences.
 */
class DefaultHomepageAsANewTabPreferenceRepository(
    private val settings: Settings,
) : HomepageAsANewTabPreferencesRepository {

    override fun getHomepageAsANewTabEnabled(): Boolean = settings.enableHomepageAsNewTab
}
