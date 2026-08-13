/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser.infobanner

import android.content.Context
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import org.mozilla.fenix.databinding.InfoBannerBinding
import org.mozilla.fenix.utils.Settings

/**
 * Displays an Info Banner in the specified container with a message and an optional action.
 * The container can be a placeholder layout inserted in the original screen, or an existing layout.
 *
 * @param context A [Context] for accessing system resources.
 * @param settings [Settings] used to update lastCfrShownTimeInMillis.
 * @param container The layout where the banner will be shown.
 * @param message The message displayed in the banner.
 * @param dismissText The text on the dismiss button.
 * @param actionText The text on the action to perform button.
 * @param dismissByHiding Whether or not to hide the banner when dismissed.
 * @property dismissAction  Optional callback invoked when the user dismisses the banner.
 * @param actionToPerform The action to be performed on action button press.
 * @param currentTimeMillis provider for the current time in milliseconds, injectable for testing.
 */
open class InfoBanner(
    private val context: Context,
    private val settings: Settings,
    private val container: ViewGroup,
    private val message: String,
    private val dismissText: String,
    private val actionText: String? = null,
    private val dismissByHiding: Boolean = false,
    internal val dismissAction: (() -> Unit)? = null,
    private val actionToPerform: (() -> Unit)? = null,
    private val currentTimeMillis: () -> Long = { System.currentTimeMillis() },
) {
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    internal val binding = InfoBannerBinding.inflate(LayoutInflater.from(context), container, false)

    internal open fun showBanner() {
        binding.bannerInfoMessage.text = message
        binding.dismiss.text = dismissText

        if (actionText.isNullOrEmpty()) {
            binding.action.visibility = GONE
        } else {
            binding.action.text = actionText
        }

        container.addView(binding.root)

        binding.dismiss.setOnClickListener {
            dismissAction?.invoke()
            if (dismissByHiding) { binding.root.visibility = GONE } else { dismiss() }
        }

        binding.action.setOnClickListener {
            actionToPerform?.invoke()
        }

        settings.lastCfrShownTimeInMillis = currentTimeMillis()
    }

    internal fun dismiss() {
        container.removeView(binding.root)
    }
}
