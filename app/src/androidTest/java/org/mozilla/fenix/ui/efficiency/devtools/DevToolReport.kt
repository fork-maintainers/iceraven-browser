/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.devtools

import android.util.Log

/**
 * The computed output of a dev-tool, kept separate from how/where it gets displayed.
 *
 * Dev-tools build one of these and hand it to [logReport] today. Because computing [lines] never
 * calls Log.i/println directly, the same report can be handed to a different consumer later (an
 * in-app debug screen, a file export, etc.) without changing the code that computed it.
 *
 * Introduced as part of P2a-2 (2026-07-20) to split each dev-tool's data-building /
 * report-formatting / emitting responsibilities apart, in preparation for a future dev-tools UI.
 */
data class DevToolReport(
    val lines: List<String>,
)

/** Shared separator used by dev-tools that wrap their output in a header/footer band. */
const val DEV_TOOL_SEPARATOR = "--------------------------------------------------"

/**
 * Builds a [DevToolReport] for the "header, separator, one formatted block per item, separator"
 * shape shared by every boilerplate-printing dev-tool (Navigation, Interaction, and Behavior's
 * case-boilerplate generators all follow this exact shape).
 */
fun <T> buildBoilerplateReport(
    header: String,
    items: List<T>,
    formatItem: (T) -> String,
): DevToolReport = DevToolReport(
    lines = buildList {
        add(header)
        add(DEV_TOOL_SEPARATOR)
        items.forEach { add(formatItem(it)) }
        add(DEV_TOOL_SEPARATOR)
    },
)

/** Emits a [DevToolReport] to both the Android log and stdout. */
fun logReport(tag: String, report: DevToolReport) {
    report.lines.forEach { line ->
        Log.i(tag, line)
        println(line)
    }
}
