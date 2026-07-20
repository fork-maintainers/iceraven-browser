/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.content.Context
import android.util.AttributeSet
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageView
import androidx.preference.PreferenceViewHolder
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.nimbus.FxNimbus

const val EXPANDED_TOOLBAR_TYPE = "expanded"

internal class ToolbarExpandedShortcutPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ToolbarShortcutPreference(context, attrs) {
    var isTranslationsFeatureEnabled: Boolean = false

    override val options: List<ShortcutOption>
        get() = expandedShortcutOptions.filterNot {
            it.key == ShortcutType.TRANSLATE && !isTranslationsFeatureAvailable
        }

    override fun readSelectedKey(): String = context.components.settings.toolbarExpandedShortcutKey

    override fun writeSelectedKey(key: String) {
        context.components.settings.toolbarExpandedShortcutKey = key
    }

    override fun getToolbarType(): String = EXPANDED_TOOLBAR_TYPE

    override fun getSelectedIconImageView(holder: PreferenceViewHolder): ImageView {
        val simplePreview = holder.findViewById(R.id.toolbar_simple_shortcut_preview)
        val simpleNoShortcutPreview = holder.findViewById(R.id.toolbar_simple_no_shortcut_preview)
        val expandedPreview = holder.findViewById(R.id.toolbar_expanded_shortcut_preview)

        simplePreview.visibility = GONE
        simpleNoShortcutPreview.visibility = GONE
        expandedPreview.visibility = VISIBLE

        return expandedPreview.findViewById(R.id.selected_expanded_shortcut_icon)
    }

    private val isTranslationsFeatureAvailable: Boolean
        get() {
            val browserStore = this.context.components.core.store
            val isTranslationEngineSupported = browserStore.state.translationEngine.isEngineSupported ?: false

            return isTranslationEngineSupported &&
                isTranslationsFeatureEnabled &&
                FxNimbus.features.translations.value().mainFlowToolbarEnabled
        }
}
