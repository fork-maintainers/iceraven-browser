/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ipprotection.ui

import android.os.SystemClock
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.semantics.Role

/**
 * Used when toggleable needs to be debounced to prevent rapid successive toggles
 * from calling the `onValueChange` function.
 *
 * @param value Whether the underlying element is toggled on or off.
 * @param enabled Whether the underlying element will handle input events.
 * @param role The type of user interface element (used by accessibility services).
 * @param debounceInterval The length of time to wait between toggle events in milliseconds
 * @param onValueChange Callback for when item this modifier effects is toggled
 */
fun Modifier.debouncedToggleable(
    value: Boolean,
    enabled: Boolean = true,
    role: Role? = null,
    debounceInterval: Long = 1000L,
    onValueChange: (Boolean) -> Unit,
) = composed {
    var lastToggleTime: Long by remember { mutableLongStateOf(0) }

    this.then(
        Modifier.toggleable(
            value = value,
            enabled = enabled,
            role = role,
            onValueChange = { newValue ->
                val currentSystemTime = SystemClock.elapsedRealtime()
                if (currentSystemTime - lastToggleTime > debounceInterval) {
                    onValueChange(newValue)
                    lastToggleTime = currentSystemTime
                }
            },
        ),
    )
}
