/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.ipprotection

import mozilla.components.feature.ipprotection.store.IPProtectionAction
import mozilla.components.feature.ipprotection.store.state.IPProtectionState
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.Store
import org.mozilla.fenix.utils.Settings

/**
 * Interface providing access to IP Protection related [android.content.SharedPreferences].
 */
interface IPProtectionRepository {
    var hasAlreadyUsedVpn: Boolean
}

/**
 * The default implementation of [IPProtectionRepository] proxying calls to global Fenix [Settings].
 */
class DefaultIPProtectionRepository(
    private val settings: Settings,
) : IPProtectionRepository {
    override var hasAlreadyUsedVpn: Boolean
        get() = settings.hasAlreadyUsedVpn
        set(value) {
            settings.hasAlreadyUsedVpn = value
        }
}

/**
 * [Middleware] responsible for intercepting a toggle action and persistently recording the initial VPN interaction.
 */
@OptIn(mozilla.components.ExperimentalAndroidComponentsApi::class)
internal class IPProtectionPreferencesMiddleware(
    private val preferences: IPProtectionRepository,
) : Middleware<IPProtectionState, IPProtectionAction> {

    override fun invoke(
        store: Store<IPProtectionState, IPProtectionAction>,
        next: (IPProtectionAction) -> Unit,
        action: IPProtectionAction,
    ) {
        next(action)

        if (action is IPProtectionAction.Toggle && !preferences.hasAlreadyUsedVpn) {
            preferences.hasAlreadyUsedVpn = true
        }
    }
}
