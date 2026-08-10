/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.collections

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckedTextView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.updatePaddingRelative
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import mozilla.components.support.ktx.android.view.pixelSizeFor
import mozilla.components.support.ktx.android.view.putCompoundDrawablesRelativeWithIntrinsicBounds
import org.mozilla.fenix.R
import com.google.android.material.R as materialR
import mozilla.components.ui.icons.R as iconsR

/**
 * An adapter for displaying an option to create a new collection and the list of existing
 * collections.
 */
class CollectionsListAdapter(
    private val collections: Array<String>,
    private val onNewCollectionClicked: () -> Unit,
) : RecyclerView.Adapter<CollectionsListAdapter.CollectionItemViewHolder>() {

    @VisibleForTesting
    internal var checkedPosition = 1

    class CollectionItemViewHolder(val textView: CheckedTextView) :
        RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): CollectionItemViewHolder {
        val textView = LayoutInflater.from(parent.context)
            .inflate(R.layout.collection_dialog_list_item, parent, false) as CheckedTextView
        return CollectionItemViewHolder(textView)
    }

    override fun onBindViewHolder(holder: CollectionItemViewHolder, position: Int) {
        if (position == 0) {
            holder.textView.updatePaddingRelative(
                start = holder.textView.pixelSizeFor(R.dimen.tab_tray_new_collection_padding_start),
            )
            holder.textView.compoundDrawablePadding =
                holder.textView.pixelSizeFor(R.dimen.tab_tray_new_collection_drawable_padding)
            val drawable = AppCompatResources.getDrawable(
                holder.textView.context,
                iconsR.drawable.mozac_ic_plus_24,
            )?.mutate()
            drawable?.let {
                DrawableCompat.setTint(
                    it,
                    MaterialColors.getColor(
                        holder.textView.context,
                        materialR.attr.colorOnSurface,
                        "Could not resolve themed color",
                    ),
                )
            }
            holder.textView.putCompoundDrawablesRelativeWithIntrinsicBounds(
                start = drawable,
            )
        } else {
            holder.textView.isChecked = checkedPosition == position
        }

        holder.textView.setOnClickListener {
            if (position == 0) {
                onNewCollectionClicked()
            } else if (checkedPosition != position) {
                notifyItemChanged(position)
                notifyItemChanged(checkedPosition)
                checkedPosition = position
            }
        }
        holder.textView.text = collections[position]
    }

    override fun getItemCount() = collections.size

    fun getSelectedCollection() = checkedPosition - 1
}
