/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.e2e

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.WindowCompat.enableEdgeToEdge
import androidx.core.view.doOnAttach
import androidx.core.view.isEmpty
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.navigation.fragment.NavHostFragment
import mozilla.components.feature.qr.QrFragment
import mozilla.components.support.ktx.android.view.clearPersistentInsets
import mozilla.components.support.ktx.android.view.setupPersistentInsets

/**
 * [FragmentLifecycleCallbacks] delegate for configuring the container activity
 * as edge-to-edge or not to match how the new fragment navigated to is wants to be displayed.
 */
class EdgeToEdgeFragmentLifecycleCallbacks : FragmentLifecycleCallbacks() {

    override fun onFragmentViewCreated(
        fm: FragmentManager,
        f: Fragment,
        v: View,
        savedInstanceState: Bundle?,
    ) {
        // Dialog fragments have their own edge-to-edge behavior, separate from Fenix's main activity.
        // DialogFragments which cycle through inner fragments should not change the edge-to-edge strategy for
        // Fenix's main activity. One such example is the calendar picker handled in a MaterialDatePicker dialog
        // which then uses a normal fragment for the actual picker functionality.
        if (f is DialogFragment || f.parentFragment is DialogFragment) return

        // QRFragment is a generic Android Components fragment that is nested in Fenix.
        // As such the edge-to-edge behavior is to be controlled only through its Fenix container.
        if (f is QrFragment) return

        // NavHostFragment loads fragments into it, and when we set the edge to edge strategy for a
        // NavHostFragment there ends up a race condition between the competing fragments.
        if (f is NavHostFragment) return

        setEdgeToEdgeStrategy(f)
    }

    private fun setEdgeToEdgeStrategy(fragment: Fragment) {
        fragment.requireActivity().window.apply {
            // Change the edge-to-edge behavior immediately after the new fragment is attached
            // to ensure an immediate change.
            fragment.view?.doOnAttach {
                when (fragment is SystemInsetsPaddedFragment) {
                    true -> setupPersistentInsets()
                    else -> clearPersistentInsets()
                }
            }
        }
    }

    companion object {
        /**
         * Register this functionality to observe all fragment navigations in [supportFragmentManager]
         * and configure the parent activity with a new edge-to-edge behavior depending on the current fragment.
         * This only works on if API33+.
         *
         * Must be called before [android.app.Activity.setContentView] so the callbacks are registered
         * before the content view and its fragments are created. This ensures the edge-to-edge strategy
         * is applied to every fragment through [onFragmentViewCreated].
         *
         * @param supportFragmentManager [FragmentManager] hosting all screens for which to set
         * a different edge-to-edge behavior.
         * @param window [Window] which will be shown as edge-to-edge or not depending on
         * the current fragment being shown.
         */
        fun register(supportFragmentManager: FragmentManager, window: Window) {
            // Matching the same API guard used for the methods controlling the edge-to-edge insets.
            if (SDK_INT < VERSION_CODES.TIRAMISU) return
            enableEdgeToEdge(window)

            val contentView = window.peekDecorView()?.findViewById<ViewGroup>(android.R.id.content)
            check(contentView == null || contentView.isEmpty()) {
                "register() must be called before Activity.setContentView()"
            }

            // Applying this recursively is needed because
            // NavHostFragment adds additional fragments as subfragments.
            supportFragmentManager.registerFragmentLifecycleCallbacks(
                EdgeToEdgeFragmentLifecycleCallbacks(),
                true,
            )
        }
    }
}
