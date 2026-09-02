/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.share

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import mozilla.components.concept.engine.prompt.ShareData
import org.mozilla.fenix.databinding.ShareCloseBinding
import org.mozilla.fenix.share.listadapters.ShareTabsAdapter

/**
 * Callbacks for possible user interactions on the [ShareCloseView]
 */
interface ShareCloseInteractor {
    fun onShareClosed()
}

class ShareCloseView(
    val containerView: ViewGroup,
    private val interactor: ShareCloseInteractor,
) {

    val adapter = ShareTabsAdapter()

    private val binding = ShareCloseBinding.inflate(
        LayoutInflater.from(containerView.context),
        containerView,
        true,
    )

    init {
        binding.closeButton.setOnClickListener { interactor.onShareClosed() }

        binding.sharedSiteList.layoutManager = LinearLayoutManager(containerView.context)
        binding.sharedSiteList.adapter = adapter
    }

    fun setTabs(tabs: List<ShareData>) {
        adapter.submitList(tabs)
    }

    /**
     * Renders the share preview as a tab group with the group name and theme.
     *
     * @param title The tab group name to display as the header, or null to keep the default header.
     * @param color The tab group theme color used to tint the dot.
     */
    fun setGroup(
        title: String?,
        @ColorInt color: Int,
    ) {
        title ?: return
        binding.title.text = title
        binding.groupDot.isVisible = true
        binding.groupDot.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }
}
