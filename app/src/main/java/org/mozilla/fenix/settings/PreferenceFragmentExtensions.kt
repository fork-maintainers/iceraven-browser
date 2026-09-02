/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.fenix.settings

import android.text.Editable
import android.text.TextWatcher
import android.widget.FrameLayout
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import mozilla.components.support.ktx.android.content.pixelSizeFor
import org.mozilla.fenix.R

/**
 * Displays a custom [MaterialAlertDialogBuilder] for an [EditTextPreference].
 * This provides a consistent look and feel for text input dialogs across different settings screens.
 *
 * @param preference The [Preference] that was clicked.
 * @param onSuccess An optional lambda to execute custom actions after the value is successfully changed.
 * @param errorMessage A callback to provide an error message if the input is invalid.
 * @return `true` if the dialog was handled for an [EditTextPreference], `false` otherwise.
 */
fun PreferenceFragmentCompat.showCustomEditTextPreferenceDialog(
    preference: Preference,
    onSuccess: () -> Unit = {},
    errorMessage: (String) -> Int? = { null },
): Boolean {
    if (preference !is EditTextPreference) return false

    val context = requireContext()

    val textInputLayout = TextInputLayout(context).apply { isErrorEnabled = true }

    val editText = TextInputEditText(context).apply {
        setText(preference.text)
        addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    // No-op
                }
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    textInputLayout.error = null
                }
                override fun afterTextChanged(s: Editable?) {
                    // No-op
                }
            },
        )
    }

    textInputLayout.addView(editText)

    val container = FrameLayout(context).apply {
        val horizontalPadding = context.pixelSizeFor(R.dimen.dialog_edit_text_horizontal_padding)
        setPadding(horizontalPadding, 0, horizontalPadding, 0)
        addView(textInputLayout)
    }

    val dialog = MaterialAlertDialogBuilder(context)
        .setTitle(preference.dialogTitle ?: preference.title)
        .setView(container)
        .setPositiveButton(android.R.string.ok, null)
        .setNegativeButton(android.R.string.cancel, null)
        .create()

    dialog.show()

    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
        val newValue = editText.text.toString()
        val errorResId = errorMessage(newValue)

        if (errorResId != null) {
            textInputLayout.error = context.getString(errorResId)
        } else {
            if (preference.callChangeListener(newValue)) {
                preference.text = newValue
                onSuccess()
            }
            dialog.dismiss()
        }
    }

    return true
}
