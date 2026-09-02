/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.debugsettings.crashtools

import android.content.Intent
import android.os.Process
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import mozilla.components.compose.base.annotation.FlexibleWindowPreview
import mozilla.components.compose.base.button.FilledButton
import org.mozilla.fenix.R
import org.mozilla.fenix.components.components
import org.mozilla.fenix.startupCrash.StartupCrashActivity
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.PreviewThemeProvider
import org.mozilla.fenix.theme.Theme
import org.mozilla.fenix.utils.Settings

private const val SECOND_IN_MILLISECOND = 1000L

@Composable
internal fun CrashTools(
    settings: Settings = components.settings,
    currentTimeMillis: () -> Long = { System.currentTimeMillis() },
) {
    val appContext = LocalContext.current.applicationContext

    var now = currentTimeMillis()
    var genericDeferPeriod by remember { mutableLongStateOf(settings.crashReportDeferredUntil - now) }
    LaunchedEffect(Unit) {
        while (true) {
            now = currentTimeMillis()
            genericDeferPeriod = settings.crashReportDeferredUntil - now
            delay(SECOND_IN_MILLISECOND)
        }
    }

    Surface {
        Column(
            modifier = Modifier
                .padding(all = 16.dp)
                .verticalScroll(state = rememberScrollState()),
        ) {
            Text(
                text = stringResource(
                    R.string.crash_debug_deferral_timer,
                    convertMillisToDHMS(maxOf(genericDeferPeriod, 0)),
                ),
                style = FirefoxTheme.typography.body2,
            )
            FilledButton(
                text = stringResource(R.string.crash_debug_deferral_button),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    settings.crashReportDeferredUntil = 0L
                    genericDeferPeriod = 0L
                },
            )
            Text(
                text = stringResource(R.string.crash_debug_crash_app_warning),
                color = MaterialTheme.colorScheme.error,
                style = FirefoxTheme.typography.subtitle2,
            )
            FilledButton(
                text = stringResource(R.string.crash_debug_generic_crash_trigger),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    throw ArithmeticException("Debug drawer triggered exception.")
                },
            )
            Text(
                text = stringResource(R.string.crash_debug_startup_crash_warning),
                color = MaterialTheme.colorScheme.error,
                style = FirefoxTheme.typography.subtitle2,
            )
            FilledButton(
                text = stringResource(R.string.crash_debug_show_startup_crash_screen),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val intent = Intent(appContext, StartupCrashActivity::class.java)

                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    appContext.startActivity(intent)
                    Process.killProcess(Process.myPid())
                },
            )
        }
    }
}

internal fun convertMillisToDHMS(milliseconds: Long): String {
    return DateUtils.formatElapsedTime(milliseconds / SECOND_IN_MILLISECOND)
}

@FlexibleWindowPreview
@Composable
private fun CrashToolsPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme) {
        CrashTools(Settings(LocalContext.current))
    }
}
